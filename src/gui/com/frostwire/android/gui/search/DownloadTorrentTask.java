/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011, 2012, FrostWire(TM). All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.frostwire.android.gui.search;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloader;
import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloaderFactory;

import android.util.Log;

import com.frostwire.android.gui.util.SystemUtils;

/**
 * @author gubatron
 * @author aldenml
 * 
 */
class DownloadTorrentTask extends SearchTask {

    private static final String TAG = "FW.DownloadTorrentTask";

    private static final int TORRENT_DOWNLOAD_INDEX_TIMEOUT = 60000; // 60 seconds

    private final String query;
    private final BittorrentWebSearchResult sr;
    private final SearchTask task;

    private TorrentDownloader torrentDownloader;

    public DownloadTorrentTask(String query, BittorrentWebSearchResult sr, SearchTask task) {
        super("DownloadTorrentTask: " + sr.getTorrentURI());
        this.query = query;
        this.sr = sr;
        this.task = task;
    }

    @Override
    public void cancel() {
        super.cancel();
        
        LocalSearchEngine.instance().forgetInfoHash(sr.getHash());

        try {
            if (torrentDownloader != null) {
                torrentDownloader.cancel();
            }
        } catch (Throwable e) {
            Log.e(TAG, "Error canceling TorrentDonloader for: " + sr.getTorrentURI(), e);
        }
    }

    @Override
    public void runTask() {
        if (isCancelled()) {
            return;
        }

        try {
            String saveDir = SystemUtils.getDeepScanTorrentsDirectory().getAbsolutePath();

            CountDownLatch finishSignal = new CountDownLatch(1);

            Log.d(TAG, String.format("About to download: %s, details %s", sr.getTorrentURI(), sr.getDetailsUrl()));
            torrentDownloader = TorrentDownloaderFactory.create(new LocalSearchTorrentDownloaderListener(query, sr, task, finishSignal), sr.getTorrentURI(), sr.getDetailsUrl(), saveDir);
            torrentDownloader.start();

            if (finishSignal.await(TORRENT_DOWNLOAD_INDEX_TIMEOUT, TimeUnit.MILLISECONDS)) {
                //Log.d(TAG, "Torrent downloaded finish  and indexed: " + sr.getTorrentURI());
            } else {
                Log.w(TAG, "Download didn't finish in time: " + sr.getTorrentURI());
            }
        } catch (Throwable e) {
            Log.e(TAG, "Error in DownloadTorrentTask", e);
        }
    }
}
