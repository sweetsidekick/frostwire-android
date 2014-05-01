package com.frostwire.vuze;

import java.io.File;
import java.util.Date;
import java.util.Set;

public class VuzeDownloadManager {

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

    public byte[] getHash() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getDisplayName() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getStatus() {
        // TODO Auto-generated method stub
        return null;
    }

    public int getDownloadCompleted() {
        // TODO Auto-generated method stub
        return 0;
    }

    public long getSize() {
        // TODO Auto-generated method stub
        return 0;
    }

    public boolean isResumable() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isPausable() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isComplete() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isDownloading() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isSeeding() {
        // TODO Auto-generated method stub
        return false;
    }

    public void stop() {
        // TODO Auto-generated method stub
        
    }

    public void start() {
        // TODO Auto-generated method stub
        
    }

    public File getSavePath() {
        // TODO Auto-generated method stub
        return null;
    }

    public long getBytesReceived() {
        // TODO Auto-generated method stub
        return 0;
    }

    public long getBytesSent() {
        // TODO Auto-generated method stub
        return 0;
    }

    public long getDownloadSpeed() {
        // TODO Auto-generated method stub
        return 0;
    }

    public long getUploadSpeed() {
        // TODO Auto-generated method stub
        return 0;
    }

    public long getETA() {
        // TODO Auto-generated method stub
        return 0;
    }

    public Date getCreationDate() {
        // TODO Auto-generated method stub
        return null;
    }

    public Object getConnectedPeers() {
        // TODO Auto-generated method stub
        return null;
    }

    public Object getPeers() {
        // TODO Auto-generated method stub
        return null;
    }

    public Object getConnectedSeeds() {
        // TODO Auto-generated method stub
        return null;
    }

    public Object getSeeds() {
        // TODO Auto-generated method stub
        return null;
    }

    public Object getShareRatio() {
        // TODO Auto-generated method stub
        return null;
    }

    public Object hasScrape() {
        // TODO Auto-generated method stub
        return null;
    }

    public Object hasStarted() {
        // TODO Auto-generated method stub
        return null;
    }

    public long getChangedTime() {
        // TODO Auto-generated method stub
        return 0;
    }

    public void setSkipped(Set<String> paths, boolean b) {
        // TODO Auto-generated method stub
        
    }

}
