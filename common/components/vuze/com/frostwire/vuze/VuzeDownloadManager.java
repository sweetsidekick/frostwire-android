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

package com.frostwire.vuze;

import java.io.File;
import java.util.Date;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.minicastle.util.Arrays;

import com.frostwire.logging.Logger;

/**
 * 
 * @author gubatron
 * @author aldenml
 *
 */
public final class VuzeDownloadManager {

    private static final Logger LOG = Logger.getLogger(VuzeDownloadManager.class);

    // states from azureus download manager
    public static final int STATE_WAITING = 0;
    public static final int STATE_INITIALIZING = 1;
    public static final int STATE_INITIALIZED = 2;
    public static final int STATE_ALLOCATING = 3;
    public static final int STATE_CHECKING = 4;
    public static final int STATE_READY = 5;
    public static final int STATE_DOWNLOADING = 6;
    public static final int STATE_FINISHING = 7;
    public static final int STATE_SEEDING = 8;
    public static final int STATE_STOPPING = 9;
    public static final int STATE_STOPPED = 10;
    public static final int STATE_CLOSED = 11;
    public static final int STATE_QUEUED = 12;
    public static final int STATE_ERROR = 13;

    private static final byte[] EMPTY_HASH = {};

    //private final DownloadManager dm;

    private final byte[] hash = null;
    private final File savePath = null;
    private final Date creationDate = null;

    // the only fields that can be changed due to a partial download change
    private String displayName;
    private long size;
    private long changedTime;

    VuzeDownloadManager() {
        // TODO:BITTORRENT
        /*
        this.dm = dm;

        dm.setUserData(VuzeKeys.VUZE_DOWNLOAD_MANAGER_OBJECT_KEY, this);

        this.hash = calculateHash(dm);
        this.savePath = dm.getSaveLocation();
        this.creationDate = new Date(dm.getCreationTime());

        refreshData(dm);
        */
    }

    public String getDisplayName() {
        return displayName;
    }

    public long getSize() {
        return size;
    }

    public long getChangedTime() {
        return changedTime;
    }

    /**
     * The client should be aware that if the array is modified, the inner state of
     * the object is changed (due to array mutability in java).
     * 
     * @return
     */
    public byte[] getHash() {
        return hash;
    }

    public File getSavePath() {
        return savePath;
    }

    /**
     * This method should be used with care, since Date is mutable, the user
     * of this class could mess with the inner state of the object.
     * 
     * @return
     */
    public Date getCreationDate() {
        return creationDate;
    }

    public String getStatus() {
        return "";
    }

    public int getDownloadCompleted() {
        return 0;//dm.getStats().getDownloadCompleted(true) / 10;
    }

    public boolean isResumable() {
        return false;
    }

    public boolean isPausable() {
        return false;
    }

    public boolean isComplete() {
        return false;
    }

    public boolean isDownloading() {
        return false;
    }

    public boolean isSeeding() {
        return false;
    }

    public long getBytesReceived() {
        return 0;
    }

    public long getBytesSent() {
        return 0;
    }

    public long getDownloadSpeed() {
        return 0;
    }

    public long getUploadSpeed() {
        return 0;
    }

    public long getETA() {
        return 0;
    }

    public int getShareRatio() {
        return 0;
    }

    public int getPeers() {
        return 0;
    }

    public int getSeeds() {
        return 0;
    }

    public int getConnectedPeers() {
        return 0;
    }

    public int getConnectedSeeds() {
        return 0;
    }

    public boolean hasStarted() {
        return false;
    }

    public boolean hasScrape() {
        return false;
    }

    public void start() {
        //ManagerUtils.start(dm);
    }

    public void stop() {
        //ManagerUtils.stop(dm);
    }
    
    /** Like stop() but transfer can be started automatically, like when switching to Wi-Fi for example.*/
    public void enqueue() {
        //ManagerUtils.stop(dm, DownloadManager.STATE_QUEUED);
    }

    public void setSkipped(Set<String> paths, boolean skipped) {
        // TODO:BITTORRENT
        /*
        DiskManagerFileInfo[] infs = dm.getDiskManagerFileInfoSet().getFiles();

        try {
            dm.getDownloadState().suppressStateSave(true);

            if (paths == null || paths.isEmpty()) {
                for (DiskManagerFileInfo inf : infs) {
                    inf.setSkipped(false);
                }
            } else {
                String savePath = dm.getSaveLocation().getPath();
                for (DiskManagerFileInfo inf : infs) {
                    String path = inf.getFile(false).getPath();
                    path = removePrefixPath(savePath, path);
                    if (skipped && !inf.isSkipped()) {
                        inf.setSkipped(paths.contains(path));
                    } else if (!skipped && inf.isSkipped()) {
                        inf.setSkipped(!paths.contains(path));
                    }
                }
            }
        } finally {
            dm.getDownloadState().suppressStateSave(false);
        }

        refreshData(dm);
        */
    }

    @Override
    public boolean equals(Object o) {
        // TODO:BITTORRENT

        boolean equals = false;

        /*if (o instanceof VuzeDownloadManager) {
            VuzeDownloadManager other = (VuzeDownloadManager) o;
            if (dm.equals(other.dm) || Arrays.areEqual(getHash(), other.getHash())) {
                equals = true;
            }
        }*/

        return equals;
    }
    
    static String removePrefixPath(String prefix, String path) {
        path = path.replace(prefix, "");
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        return path;
    }

    // TODO:BITTORRENT
    /*
    private void refreshData(DownloadManager dm) {
        Set<DiskManagerFileInfo> noSkippedSet = VuzeUtils.getFileInfoSet(dm, InfoSetQuery.NO_SKIPPED);

        this.displayName = calculateDisplayName(dm, noSkippedSet);
        this.size = calculateSize(dm, noSkippedSet);
        this.changedTime = System.currentTimeMillis();
    }

    private static String calculateDisplayName(DownloadManager dm, Set<DiskManagerFileInfo> noSkippedSet) {
        String displayName = null;

        if (noSkippedSet.size() == 1) {
            displayName = FilenameUtils.getBaseName(noSkippedSet.iterator().next().getFile(false).getName());
        } else {
            displayName = dm.getDisplayName();
        }

        return displayName;
    }

    private static long calculateSize(DownloadManager dm, Set<DiskManagerFileInfo> noSkippedSet) {
        long size = 0;

        boolean partial = noSkippedSet.size() != dm.getDiskManagerFileInfoSet().nbFiles();

        if (partial) {
            for (DiskManagerFileInfo fileInfo : noSkippedSet) {
                size += fileInfo.getLength();
            }
        } else {
            size = dm.getSize();
        }

        return size;
    }

    private static byte[] calculateHash(DownloadManager dm) {
        try {
            return dm.getTorrent().getHash();
        } catch (Throwable e) {
            LOG.error("Torrent download in bad state");
            return EMPTY_HASH;
        }
    }*/
}
