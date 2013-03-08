/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011, 2012, FrostWire(R). All rights reserved.
 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.frostwire.search;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.frostwire.android.BuildConfig;
import com.frostwire.android.gui.util.SystemUtils;
import com.jakewharton.DiskLruCache;
import com.jakewharton.DiskLruCache.Snapshot;

/**
 * 
 * @author gubatron
 * @author aldenml
 *
 */
public class DiskCrawlCache implements CrawlCache {

    private static final Logger LOG = LoggerFactory.getLogger(DiskCrawlCache.class);

    private static final int APP_VERSION = 1;
    private static final int VALUE_COUNT = 1;
    private static final int DISK_CACHE_SIZE = 1024 * 1024 * 1; // 4MB
    private static final int IO_BUFFER_SIZE = 4 * 1024;

    private DiskLruCache cache;

    public DiskCrawlCache() {
        try {
            this.cache = DiskLruCache.open(SystemUtils.getDeepScanTorrentsDirectory(), APP_VERSION, VALUE_COUNT, DISK_CACHE_SIZE);
        } catch (Throwable e) {
            LOG.warn("Unable to create crawl cache", e);
        }
    }

    public CrawlableSearchResult get(String key) {
        CrawlableSearchResult sr = null;

        if (cache != null) {
            Snapshot snapshot = null;

            try {
                snapshot = cache.get(key);

                if (snapshot != null) {
                    String codec = snapshot.getString(0);
                    sr = decode(codec, snapshot.getInputStream(1));
                }
            } catch (Throwable e) {
                LOG.warn("Error getting value from crawl cache", e);
            } finally {
                if (snapshot != null) {
                    snapshot.close();
                }
            }
        } else {
            LOG.warn("Crawl cache is null");
        }

        return sr;
    }

    public void put(CrawlableSearchResult sr) {
        if (cache != null) {
            DiskLruCache.Editor editor = null;
            try {
                String key = sr.getCacheKey();

                editor = cache.edit(key);
                if (editor == null) {
                    return;
                }

                editor.set(0, sr.getCodec().getClass().getName());
                encode(sr, editor);
                cache.flush();
                editor.commit();
                if (BuildConfig.DEBUG) {
                    LOG.warn("value put on disk cache " + key);
                }
            } catch (Throwable e) {
                LOG.warn("Error putting value to crawl cache", e);
                try {
                    if (editor != null) {
                        editor.abort();
                    }
                } catch (IOException ignored) {
                }
            }
        } else {
            LOG.warn("Crawl cache is null");
        }
    }

    private CrawlableSearchResult decode(String codec, InputStream is) throws Exception {
        Class<?> clazz = Class.forName(codec);

        SearchResultCodec searchResultCodec = (SearchResultCodec) clazz.newInstance();

        return (CrawlableSearchResult) searchResultCodec.decode(is);
    }

    private void encode(CrawlableSearchResult sr, DiskLruCache.Editor editor) throws IOException, FileNotFoundException {
        OutputStream out = null;
        try {
            out = new BufferedOutputStream(editor.newOutputStream(1), IO_BUFFER_SIZE);
            copy(sr.getCodec().encode(sr), out);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    private void copy(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[IO_BUFFER_SIZE];
        int n = 0;
        while ((n = input.read(buffer)) != -1) {
            output.write(buffer, 0, n);
        }
    }
}
