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

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloader;
import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloaderCallBackInterface;
import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloaderFactory;
import org.gudy.azureus2.core3.util.TorrentUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.frostwire.android.gui.util.SystemUtils;
import com.frostwire.websearch.TorrentWebSearchResult;

/**
 * 
 * @author gubatron
 * @author aldenml
 *
 */
public abstract class TorrentSearchPerformer extends PagedWebSearchPerformer {

    private static final Logger LOG = LoggerFactory.getLogger(TorrentSearchPerformer.class);

    private static final int TORRENT_DOWNLOAD_TIMEOUT = 10000; // 10 seconds

    public TorrentSearchPerformer(long token, String keywords, int timeout, int pages) {
        super(token, keywords, timeout, pages);
    }

    @Override
    public void crawl(CrawlableSearchResult sr) {
        if (sr instanceof TorrentWebSearchResult) {
            crawlTorrent((TorrentWebSearchResult) sr);
        }
    }

    private void crawlTorrent(TorrentWebSearchResult sr) {
        downloadTorrent(sr.getTorrentURI(), sr.getDetailsUrl());
    }

    /**
     * This method should not be asynchronous since it is already called in a background thread context
     * 
     * @param url
     * @param referrer
     */
    protected TOTorrent downloadTorrent(String url, String referrer) {
        String saveDir = SystemUtils.getDeepScanTorrentsDirectory().getAbsolutePath();

        LOG.debug(String.format("About to download: %s, details %s", url, referrer));

        CountDownLatch finishSignal = new CountDownLatch(1);

        TorrentDownloadListener listener = new TorrentDownloadListener(url, finishSignal);
        TorrentDownloader downloader = TorrentDownloaderFactory.create(listener, url, referrer, saveDir);
        downloader.setDownloadPath(saveDir, null);
        downloader.start();

        await(downloader, finishSignal);

        return listener.getTorrent();
    }

    private void await(TorrentDownloader downloader, CountDownLatch finishSignal) {
        try {
            if (!finishSignal.await(TORRENT_DOWNLOAD_TIMEOUT, TimeUnit.MILLISECONDS)) {
                LOG.warn("Download didn't finish in time: " + downloader.getURL());
                downloader.cancel();
            }
        } catch (InterruptedException e) {
            // ignore
        }
    }

    private static final class TorrentDownloadListener implements TorrentDownloaderCallBackInterface {

        private final String url;
        private final CountDownLatch finishSignal;

        private final AtomicBoolean finished = new AtomicBoolean(false);

        private TOTorrent torrent;

        public TorrentDownloadListener(String url, CountDownLatch finishSignal) {
            this.url = url;
            this.finishSignal = finishSignal;
        }

        public TOTorrent getTorrent() {
            return torrent;
        }

        @Override
        public void TorrentDownloaderEvent(int state, TorrentDownloader inf) {
            if (state == TorrentDownloader.STATE_FINISHED && finished.compareAndSet(false, true)) {
                try {
                    File file = inf.getFile();
                    LOG.debug("Downloaded torrent file to: " + file.getName());
                    torrent = TorrentUtils.readFromFile(file, false);
                    file.delete();
                } catch (Throwable e) {
                    LOG.warn("Error downloading torrent: " + url, e);
                }

                finishSignal.countDown();
            }

            switch (state) {
            case TorrentDownloader.STATE_ERROR:
            case TorrentDownloader.STATE_DUPLICATE:
            case TorrentDownloader.STATE_CANCELLED:
                LOG.warn("Torrent download error: state=" + state + ", url=" + url);
                finishSignal.countDown();
                break;
            }
        }
    }
}
