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

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentFile;
import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloader;
import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloaderCallBackInterface;
import org.gudy.azureus2.core3.util.TorrentUtils;

import android.util.Log;

/**
 * @author gubatron
 * @author aldenml
 * 
 */
class LocalSearchTorrentDownloaderListener implements TorrentDownloaderCallBackInterface {

    private static final String TAG = "FW.LocalSearchTorrentDownloaderListener";

    private final Set<String> tokens;
    private final BittorrentWebSearchResult sr;
    private final SearchTask task;
    private final CountDownLatch finishSignal;

    private final AtomicBoolean finished = new AtomicBoolean(false);

    public LocalSearchTorrentDownloaderListener(String query, BittorrentWebSearchResult sr, SearchTask task, CountDownLatch finishSignal) {
        this.tokens = new HashSet<String>(Arrays.asList(query.toLowerCase().split(" ")));
        this.sr = sr;
        this.task = task;
        this.finishSignal = finishSignal;
    }

    @Override
    public void TorrentDownloaderEvent(int state, TorrentDownloader inf) {
        // index the torrent (insert it's structure in the local DB)
        if (state == TorrentDownloader.STATE_FINISHED && finished.compareAndSet(false, true)) {
            try {
                File file = inf.getFile();
                TOTorrent torrent = TorrentUtils.readFromFile(file, false);

                // search right away on this torrent.
                if (!task.isCancelled() && tokens.size() > 0) {
                    matchResults(torrent);
                }

                LocalSearchEngine.instance().indexTorrent(sr, torrent);

                file.delete();
            } catch (Throwable e) {
                Log.e(TAG, "Error indexing a torrent: " + sr.getTorrentURI(), e);
            }

            finishSignal.countDown();
        }

        switch (state) {
        case TorrentDownloader.STATE_ERROR:
        case TorrentDownloader.STATE_DUPLICATE:
            Log.e(TAG, "Error downloading torrent: " + sr.getTorrentURI());
        case TorrentDownloader.STATE_CANCELLED:
            Log.d(TAG, "Torrent download cancelled: " + sr.getTorrentURI());
            finishSignal.countDown();
            break;
        }
    }

    private void matchResults(TOTorrent torrent) {
        TOTorrentFile[] files = torrent.getFiles();
        for (int i = 0; i < files.length && !task.isCancelled(); i++) {
            try {
                String keywords = LocalSearchEngine.sanitize(sr.getFileName() + " " + files[i].getRelativePath()).toLowerCase();
                //Log.d(TAG, "Keywords for on the fly match: " + keywords);

                boolean foundMatch = true;

                for (String token : tokens) {
                    //Log.d(TAG, "Token: " + token);
                    if (!keywords.contains(token)) {
                        //Log.d(TAG, "Not match for token: " + token);
                        foundMatch = false;
                        break;
                    }
                }

                if (foundMatch) {
                    LocalSearchEngine.instance().addResult(new BittorrentDeepSearchResult(sr, files[i]));
                }
            } catch (Throwable e) {
                Log.e(TAG, "Error testing match for inner file of torrent", e);
            }
        }
    }
}
