package com.frostwire.android.gui.transfers;

import java.io.File;
import java.util.Date;
import java.util.List;

public class YouTubeDownload implements DownloadTransfer {

    @Override
    public String getDisplayName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getStatus() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getProgress() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getSize() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Date getDateCreated() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getBytesReceived() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getBytesSent() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getDownloadSpeed() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getUploadSpeed() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getETA() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean isComplete() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public List<? extends TransferItem> getItems() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void cancel() {
        // TODO Auto-generated method stub

    }

    @Override
    public File getSavePath() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isDownloading() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void cancel(boolean deleteData) {
        // TODO Auto-generated method stub

    }
}
