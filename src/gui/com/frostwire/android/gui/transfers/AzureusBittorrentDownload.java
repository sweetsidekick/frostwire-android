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

package com.frostwire.android.gui.transfers;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.tracker.client.TRTrackerScraperResponse;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.DisplayFormatters;

import com.frostwire.vuze.VuzeDownloadManager;
import com.frostwire.vuze.VuzeFileInfo;
import com.frostwire.vuze.VuzeFormatter;
import com.frostwire.vuze.VuzeUtils;

import android.util.Log;

/**
 * @author gubatron
 * @author aldenml
 *
 */
final class AzureusBittorrentDownload implements BittorrentDownload {

    private static final String TAG = "FW.AzureusBittorrentDownload";

    private final TransferManager manager;
    private VuzeDownloadManager downloadManager;

    private List<BittorrentDownloadItem> items;
    private String hash;
    private boolean partialDownload;
    private Set<DiskManagerFileInfo> fileInfoSet;
    private long size;
    private String displayName;

    public AzureusBittorrentDownload(TransferManager manager, VuzeDownloadManager downloadManager) {
        this.manager = manager;
        this.downloadManager = downloadManager;
        this.hash = VuzeFormatter.formatHash(downloadManager.getHash());

        refreshData(); // super mutable
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getStatus() {
        return downloadManager.getStatus();
    }

    public int getProgress() {
        if (isComplete()) {
            return 100;
        }

        if (partialDownload) {
            long downloaded = 0;
            for (DiskManagerFileInfo fileInfo : fileInfoSet) {
                downloaded += fileInfo.getDownloaded();
            }
            return (int) ((downloaded * 100) / size);
        } else {
            return downloadManager.getDownloadCompleted();
        }
    }

    public long getSize() {
        return size;
    }

    public boolean isResumable() {
        return downloadManager.isResumable();
    }

    public boolean isPausable() {
        return downloadManager.isPausable();
    }

    public boolean isComplete() {
        return downloadManager.isComplete();
    }

    public boolean isDownloading() {
        return downloadManager.isDownloading();
    }

    public boolean isSeeding() {
        return downloadManager.isSeeding();
    }

    public List<? extends BittorrentDownloadItem> getItems() {
        if (items.size() == 1) {
            return Collections.emptyList();
        }
        return items;
    }

    public void pause() {
        if (isPausable()) {
            downloadManager.stop();
        }
    }

    public void resume() {
        if (isResumable()) {
            downloadManager.start();
        }
    }

    public File getSavePath() {
        return downloadManager.getSavePath();
    }

    public long getBytesReceived() {
        return downloadManager.getBytesReceived();
    }

    public long getBytesSent() {
        return downloadManager.getBytesSent();
    }

    public long getDownloadSpeed() {
        return downloadManager.getDownloadSpeed();
    }

    public long getUploadSpeed() {
        return downloadManager.getUploadSpeed();
    }

    public long getETA() {
        return downloadManager.getETA();
    }

    public Date getDateCreated() {
        return downloadManager.getCreationDate();
    }

    public String getPeers() {
        return VuzeFormatter.formatPeers(downloadManager.getPeers(), downloadManager.getConnectedPeers(), downloadManager.hasStarted(), downloadManager.hasScrape());
    }

    public String getSeeds() {
        return VuzeFormatter.formatSeeds(downloadManager.getSeeds(), downloadManager.getConnectedSeeds(), downloadManager.hasStarted(), downloadManager.hasScrape());
    }

    public String getHash() {
        return hash;
    }

    public String getSeedToPeerRatio() {
        return VuzeFormatter.formatSeedToPeerRatio(downloadManager.getConnectedSeeds(), downloadManager.getConnectedPeers());
    }

    public String getShareRatio() {
        return VuzeFormatter.formatShareRatio(downloadManager.getShareRatio());
    }

    @Override
    public void cancel() {
        cancel(false);
    }

    public void cancel(boolean deleteData) {
        cancel(deleteData, true);
    }

    public void cancel(boolean deleteData, boolean async) {
        manager.remove(this);
        TorrentUtil.removeDownload(downloadManager.getDM(), deleteData, deleteData, async);
    }

    DownloadManager getDownloadManager() {
        return downloadManager.getDM();
    }

    @Override
    public List<? extends BittorrentDownloadItem> getBittorrentItems() {
        return items;
    }

    @Override
    public String getDetailsUrl() {
        return null;
    }

    public void refreshData() {
        fileInfoSet = TorrentUtil.getNoSkippedFileInfoSet(downloadManager);
        partialDownload = !TorrentUtil.getSkippedFiles(downloadManager).isEmpty();

        if (partialDownload) {
            if (fileInfoSet.isEmpty()) {
                size = downloadManager.getSize();
            } else {
                size = 0;
                for (DiskManagerFileInfo fileInfo : fileInfoSet) {
                    size += fileInfo.getLength();
                }
            }
        } else {
            size = downloadManager.getSize();
        }

        if (fileInfoSet.size() == 1) {
            displayName = FilenameUtils.getBaseName(fileInfoSet.toArray(new DiskManagerFileInfo[0])[0].getFile(false).getName());
        } else {
            displayName = downloadManager.getDisplayName();
        }

        items = new ArrayList<BittorrentDownloadItem>(fileInfoSet.size());
        for (DiskManagerFileInfo fileInfo : fileInfoSet) {
            items.add(new AzureusBittorrentDownloadItem(new VuzeFileInfo(fileInfo)));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BittorrentDownload)) {
            return false;
        }

        return getHash().equals(((BittorrentDownload) o).getHash());
    }
}
