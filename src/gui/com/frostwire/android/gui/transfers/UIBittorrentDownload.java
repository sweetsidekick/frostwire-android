package com.frostwire.android.gui.transfers;

import com.frostwire.bittorrent.BTDownload;

import java.io.File;
import java.util.Date;
import java.util.List;

/**
 * @author gubatron
 * @author aldenml
 */
public final class UIBittorrentDownload implements BittorrentDownload {

    private final BTDownload dl;

    public UIBittorrentDownload(BTDownload dl) {
        this.dl = dl;
    }

    @Override
    public String getHash() {
        return dl.getInfoHash();
    }

    @Override
    public String getPeers() {
        int connectedPeers = dl.getConnectedPeers();
        int peers = dl.getTotalPeers();

        String tmp = connectedPeers > peers ? "%1" : "%1 " + "/" + " %2";

        tmp = tmp.replaceAll("%1", String.valueOf(connectedPeers));
        tmp = tmp.replaceAll("%2", String.valueOf(peers));

        return tmp;
    }

    @Override
    public String getSeeds() {
        int connectedSeeds = dl.getConnectedSeeds();
        int seeds = dl.getTotalSeeds();

        String tmp = connectedSeeds > seeds ? "%1" : "%1 " + "/" + " %2";

        tmp = tmp.replaceAll("%1", String.valueOf(connectedSeeds));
        String param2 = "?";
        if (seeds != -1) {
            param2 = String.valueOf(seeds);
        }
        tmp = tmp.replaceAll("%2", param2);

        return tmp;
    }

    @Override
    public boolean isResumable() {
        return dl.isPaused();
    }

    @Override
    public boolean isPausable() {
        return !dl.isPaused();
    }

    @Override
    public boolean isSeeding() {
        return dl.isSeeding();
    }

    @Override
    public void enqueue() {

    }

    @Override
    public void pause() {
        dl.pause();
    }

    @Override
    public void resume() {
        dl.resume();
    }

    @Override
    public List<? extends BittorrentDownloadItem> getBittorrentItems() {
        // TODO:BITTORRENT
        return null;
    }

    @Override
    public File getSavePath() {
        return dl.getSavePath();
    }

    @Override
    public boolean isDownloading() {
        return dl.isDownloading();
    }

    @Override
    public void cancel(boolean deleteData) {
        // TODO:BITTORRENT
    }

    @Override
    public String getDisplayName() {
        return dl.getDisplayName();
    }

    @Override
    public String getStatus() {
        return dl.getState().toString();
    }

    @Override
    public int getProgress() {
        return dl.getProgress();
    }

    @Override
    public long getSize() {
        return dl.getSize();
    }

    @Override
    public Date getDateCreated() {
        return dl.getCreated();
    }

    @Override
    public long getBytesReceived() {
        return dl.getBytesReceived();
    }

    @Override
    public long getBytesSent() {
        return dl.getBytesSent();
    }

    @Override
    public long getDownloadSpeed() {
        return dl.getDownloadSpeed();
    }

    @Override
    public long getUploadSpeed() {
        return dl.getUploadSpeed();
    }

    @Override
    public long getETA() {
        return dl.getETA();
    }

    @Override
    public boolean isComplete() {
        return dl.isComplete();
    }

    @Override
    public List<? extends TransferItem> getItems() {
        // TODO:BITTORRENT
        return null;
    }

    @Override
    public void cancel() {
        // TODO:BITTORRENT
    }

    @Override
    public String getDetailsUrl() {
        return null;
    }
}
