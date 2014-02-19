/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2014, FrostWire(R). All rights reserved.
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

package com.frostwire.android.gui.transfers;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import com.frostwire.android.R;
import com.frostwire.android.gui.Librarian;
import com.frostwire.android.gui.services.Engine;
import com.frostwire.android.gui.util.SystemUtils;
import com.frostwire.logging.Logger;
import com.frostwire.vuze.VuzeDownloadFactory;
import com.frostwire.vuze.VuzeDownloadListener;
import com.frostwire.vuze.VuzeDownloadManager;
import com.frostwire.vuze.VuzeTorrentDownloadListener;
import com.frostwire.vuze.VuzeTorrentDownloader;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public class TorrentFetcherDownload implements BittorrentDownload {

    private static final Logger LOG = Logger.getLogger(TorrentFetcherDownload.class);

    private final TransferManager manager;
    private final TorrentDownloadInfo info;
    private final Date dateCreated;

    private int statusResId;
    private final VuzeTorrentDownloader torrentDownloader;

    private BittorrentDownload delegate;

    private boolean removed;

    public TorrentFetcherDownload(TransferManager manager, TorrentDownloadInfo info) {
        this.manager = manager;
        this.info = info;
        this.dateCreated = new Date();

        this.statusResId = R.string.torrent_fetcher_download_status_downloading_torrent;

        this.torrentDownloader = new VuzeTorrentDownloader(info.getTorrentUrl(), info.getDetailsUrl());
        this.torrentDownloader.setListener(new TorrentDownloaderListener());
        this.torrentDownloader.start();
    }

    public BittorrentDownload getDelegate() {
        return delegate;
    }

    public String getDisplayName() {
        return delegate != null ? delegate.getDisplayName() : info.getDisplayName();
    }

    public String getStatus() {
        return delegate != null ? delegate.getStatus() : String.valueOf(statusResId);
    }

    public int getProgress() {
        return delegate != null ? delegate.getProgress() : 0;
    }

    public long getSize() {
        return delegate != null ? delegate.getSize() : info.getSize();
    }

    public Date getDateCreated() {
        return delegate != null ? delegate.getDateCreated() : dateCreated;
    }

    public List<? extends TransferItem> getItems() {
        return delegate != null ? delegate.getItems() : new ArrayList<TransferItem>();
    }

    public File getSavePath() {
        return delegate != null ? delegate.getSavePath() : null;
    }

    public long getBytesReceived() {
        return delegate != null ? delegate.getBytesReceived() : 0;
    }

    public long getBytesSent() {
        return delegate != null ? delegate.getBytesSent() : 0;
    }

    public long getDownloadSpeed() {
        return delegate != null ? delegate.getDownloadSpeed() : 0;
    }

    public long getUploadSpeed() {
        return delegate != null ? delegate.getUploadSpeed() : 0;
    }

    public long getETA() {
        return delegate != null ? delegate.getETA() : 0;
    }

    public String getHash() {
        return delegate != null ? delegate.getHash() : info.getHash();
    }

    public String getPeers() {
        return delegate != null ? delegate.getPeers() : "";
    }

    public String getSeeds() {
        return delegate != null ? delegate.getSeeds() : "";
    }

    public String getSeedToPeerRatio() {
        return delegate != null ? delegate.getSeedToPeerRatio() : "";
    }

    public String getShareRatio() {
        return delegate != null ? delegate.getShareRatio() : "";
    }

    public boolean isResumable() {
        return delegate != null ? delegate.isResumable() : false;
    }

    public boolean isPausable() {
        return delegate != null ? delegate.isPausable() : false;
    }

    public boolean isComplete() {
        return delegate != null ? delegate.isComplete() : false;
    }

    @Override
    public boolean isDownloading() {
        return delegate != null ? delegate.isDownloading() : true;
    }

    @Override
    public boolean isSeeding() {
        return delegate != null ? delegate.isSeeding() : false;
    }

    @Override
    public void cancel() {
        cancel(false);
    }

    @Override
    public void cancel(boolean deleteData) {
        cancel(false, true);
    }

    @Override
    public void cancel(boolean deleteData, boolean async) {
        statusResId = R.string.torrent_fetcher_download_status_canceled;

        if (delegate != null) {
            delegate.cancel(deleteData, async);
        } else {
            removed = true;
            try {
                torrentDownloader.cancel();
            } catch (Throwable e) {
                // ignore, I can't do anything
                LOG.error("Error canceling torrent downloader", e);
            }
            try {
                torrentDownloader.getFile().delete();
            } catch (Throwable e) {
                // ignore, I can't do anything
                LOG.error("Error deleting file of torrent downloader", e);
            }
        }
        manager.remove(this);
    }

    public void pause() {
        if (delegate != null) {
            delegate.pause();
        }
    }

    public void resume() {
        if (delegate != null) {
            delegate.resume();
        }
    }

    private final class TorrentDownloaderListener implements VuzeTorrentDownloadListener {

        private AtomicBoolean finished = new AtomicBoolean(false);

        @Override
        public void onFinished(VuzeTorrentDownloader dl) {
            if (removed) {
                return;
            }
            if (finished.compareAndSet(false, true)) {
                try {

                    Set<String> selection = new HashSet<String>();
                    if (info.getRelativePath() != null) {
                        selection.add(info.getRelativePath());
                    }
                    VuzeDownloadManager dm = VuzeDownloadFactory.create(dl.getFile().getAbsolutePath(), selection, SystemUtils.getTorrentDataDirectory().getAbsolutePath(), new VuzeDownloadListener() {

                        @Override
                        public void stateChanged(VuzeDownloadManager dm, int state) {
                            // TODO Auto-generated method stub

                        }

                        @Override
                        public void downloadComplete(VuzeDownloadManager dm) {
                            TransferManager.instance().incrementDownloadsToReview();
                            Engine.instance().notifyDownloadFinished(dm.getDisplayName(), dm.getSavePath().getAbsoluteFile());
                            Librarian.instance().scan(dm.getSavePath().getAbsoluteFile());
                        }
                    });

                    delegate = new AzureusBittorrentDownload(manager, dm);

                } catch (Throwable e) {
                    statusResId = R.string.torrent_fetcher_download_status_error;
                    LOG.error("Error creating the actual torrent download", e);
                }
            }
        }

        @Override
        public void onError(VuzeTorrentDownloader dl) {
            if (removed) {
                return;
            }
            statusResId = R.string.torrent_fetcher_download_status_error;
        }
    }

    @Override
    public List<? extends BittorrentDownloadItem> getBittorrentItems() {
        return (delegate != null) ? delegate.getBittorrentItems() : new ArrayList<BittorrentDownloadItem>(0);
    }

    @Override
    public String getDetailsUrl() {
        return info.getDetailsUrl();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TorrentFetcherDownload)) {
            return false;
        }

        String u1 = info.getTorrentUrl();
        String u2 = ((TorrentFetcherDownload) o).info.getTorrentUrl();

        return u1.equalsIgnoreCase(u2);
    }

}