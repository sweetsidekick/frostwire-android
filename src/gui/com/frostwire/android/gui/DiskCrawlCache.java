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

package com.frostwire.android.gui;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.frostwire.android.BuildConfig;
import com.frostwire.android.gui.util.SystemUtils;
import com.frostwire.android.util.ByteUtils;
import com.frostwire.search.CrawlCache;
import com.frostwire.util.FileUtils;
import com.jakewharton.disklrucache.DiskLruCache;
import com.jakewharton.disklrucache.DiskLruCache.Editor;
import com.jakewharton.disklrucache.DiskLruCache.Snapshot;

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
        createCache();
    }

    @Override
    public byte[] get(String key) {
        byte[] data = null;

        if (cache != null) {
            Snapshot snapshot = null;

            try {
                snapshot = cache.get(encodeKey(key));

                if (snapshot != null) {
                    data = decode(snapshot);
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

        return data;
    }

    @Override
    public void put(String key, byte[] data) {
        if (cache != null) {
            DiskLruCache.Editor editor = null;
            try {

                editor = cache.edit(encodeKey(key));
                if (editor == null) {
                    return;
                }

                encode(editor, data);
                cache.flush();
                editor.commit();
                if (BuildConfig.DEBUG) {
                    LOG.debug("value put on disk cache " + key);
                }
            } catch (Throwable e) {
                LOG.warn("Error putting value to crawl cache, will force a cache rebuild: " + e.getMessage());
                try {
                    if (editor != null) {
                        editor.abort();
                    }
                } catch (IOException ignored) {
                    
                }
                
                if (e.getMessage().contains("failed to delete")) {
                    forceCacheRecovery();
                }
            }
        } else {
            LOG.warn("Crawl cache is null");
        }
    }

    @Override
    public void remove(String key) {
        if (cache != null) {
            try {
                cache.remove(encodeKey(key));
            } catch (Throwable e) {
                LOG.warn("Error deleting value from crawl cache: " + e.getMessage());
            }
        }
    }

    @Override
    public void clear() {
        if (cache != null) {
            try {
                cache.delete();
                createCache();
            } catch (Throwable e) {
                LOG.warn("Error deleting crawl cache: " + e.getMessage());
                forceCacheRecovery();
            }
        }
    }

    private void forceCacheRecovery() {
        if (cache != null) {
            try {
                cache.close();
            } catch (Exception e) { }
            
            FileUtils.deleteFolderRecursively(cache.getDirectory());
            
            createCache();
        }
    }

    @Override
    public long size() {
        long size = 0;

        if (cache != null) {
            size = cache.size();
        }

        return size;
    }

    private void createCache() {
        try {
            this.cache = DiskLruCache.open(SystemUtils.getDeepScanTorrentsDirectory(), APP_VERSION, VALUE_COUNT, DISK_CACHE_SIZE);
        } catch (Throwable e) {
            LOG.warn("Unable to create crawl cache", e);
        }
    }

    private byte[] decode(Snapshot snapshot) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        InputStream in = snapshot.getInputStream(0);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        copy(in, out);
        return out.toByteArray();
    }

    private void encode(Editor editor, byte[] data) throws IOException, FileNotFoundException {
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        OutputStream out = null;
        try {
            out = new BufferedOutputStream(editor.newOutputStream(0), IO_BUFFER_SIZE);
            copy(in, out);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    private String encodeKey(String key) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] md5hash = new byte[32];
            byte[] bytes = key.toString().getBytes("utf-8");
            md.update(bytes, 0, bytes.length);
            md5hash = md.digest();
            return ByteUtils.encodeHex(md5hash);
        } catch (Throwable e) {
            LOG.error("Error encoding cache key", e);
        }
        return key;
    }

    private void copy(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[IO_BUFFER_SIZE];
        int n = 0;
        while ((n = input.read(buffer)) != -1) {
            output.write(buffer, 0, n);
        }
    }
}