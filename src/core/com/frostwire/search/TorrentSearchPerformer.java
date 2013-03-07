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

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.torrent.TOTorrentFile;
import org.gudy.azureus2.core3.util.TorrentUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author gubatron
 * @author aldenml
 *
 */
public abstract class TorrentSearchPerformer extends PagedWebSearchPerformer {

    private static final Logger LOG = LoggerFactory.getLogger(TorrentSearchPerformer.class);

    private static final int DEFAULT_NUM_TORRENT_DOWNLOADS = 1;
    private static final int TORRENT_DOWNLOAD_TIMEOUT = 20000; // 10 seconds

    private int numTorrentDownloads;

    public TorrentSearchPerformer(long token, String keywords, int timeout, int pages, int numTorrentDownloads) {
        super(token, keywords, timeout, pages);
        this.numTorrentDownloads = numTorrentDownloads;
    }

    public TorrentSearchPerformer(long token, String keywords, int timeout, int pages) {
        this(token, keywords, timeout, pages, DEFAULT_NUM_TORRENT_DOWNLOADS);
    }

    @Override
    public void crawl(CrawlableSearchResult sr) {
        if (numTorrentDownloads > 0) {
            numTorrentDownloads--;

            if (sr instanceof TorrentWebSearchResult) {
                crawlTorrent((TorrentWebSearchResult) sr);
            } else {
                LOG.warn("Something wrong with the logic, need to pass a TorrentWebSearchResult instead of " + sr.getClass());
            }
        }
    }

    private void crawlTorrent(TorrentWebSearchResult sr) {
        // this check will disappear once we update the vuze core and keep the magnet handler
        if (sr.getTorrentURI().startsWith("http")) {
            TOTorrent torrent = downloadTorrent(sr.getTorrentURI(), sr.getDetailsUrl());

            if (torrent != null) {
                List<String> keywordTokens = tokenize(keywords);

                TOTorrentFile[] files = torrent.getFiles();

                for (int i = 0; !isStopped() && i < files.length; i++) {
                    TOTorrentFile file = files[i];
                    String fileStr = sr.getFileName() + " " + file.getRelativePath();
                    if (match(keywordTokens, fileStr)) {
                        onResults(this, Arrays.asList(new TorrentDeepSearchResult(sr, file)));
                    }
                }
            }
        }
    }

    protected TOTorrent downloadTorrent(String url, String referrer) {
        TOTorrent torrent = null;
        try {
            byte[] data = fetchBytes(url, referrer, TORRENT_DOWNLOAD_TIMEOUT);
            torrent = TorrentUtils.readFromBEncodedInputStream(new ByteArrayInputStream(data));
        } catch (TOTorrentException e) {
            LOG.warn("Failed to download torrent: " + url, e);
        }
        return torrent;
    }

    private List<String> tokenize(String keywords) {
        // TODO: clean keywords

        List<String> tokens = new LinkedList<String>();

        for (String s : keywords.split(" ")) {
            tokens.add(s.toLowerCase(Locale.US));
        }

        return tokens;
    }

    private boolean match(List<String> keywordTokens, String fileStr) {
        // TODO: normalize fileStr

        fileStr = fileStr.toLowerCase(Locale.US);

        for (String t : keywordTokens) {
            if (!fileStr.contains(t)) {
                return false;
            }
        }

        return true;
    }
}
