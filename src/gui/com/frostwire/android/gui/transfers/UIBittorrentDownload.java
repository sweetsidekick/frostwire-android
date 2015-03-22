package com.frostwire.android.gui.transfers;

import com.frostwire.android.core.ConfigurationManager;
import com.frostwire.android.core.Constants;
import com.frostwire.android.gui.Librarian;
import com.frostwire.android.gui.NetworkManager;
import com.frostwire.android.gui.services.Engine;
import com.frostwire.android.util.SystemUtils;
import com.frostwire.bittorrent.BTDownload;
import com.frostwire.bittorrent.BTDownloadListener;
import com.frostwire.bittorrent.PaymentOptions;
import com.frostwire.logging.Logger;
import com.frostwire.transfers.TransferItem;
import com.frostwire.transfers.TransferState;
import com.frostwire.util.DirectoryUtils;

import java.io.File;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author gubatron
 * @author aldenml
 */
public final class UIBittorrentDownload implements BittorrentDownload {

    private static final Logger LOG = Logger.getLogger(UIBittorrentDownload.class);

    private final TransferManager manager;
    private final BTDownload dl;

    private String displayName;
    private long size;
    private List<TransferItem> items;

    private boolean noSpaceAvailableInCurrentMount;

    public UIBittorrentDownload(TransferManager manager, BTDownload dl) {
        this.manager = manager;
        this.dl = dl;
        this.dl.setListener(new StatusListener());

        this.displayName = dl.getDisplayName();
        this.size = calculateSize(dl);
        this.items = calculateItems(dl);

        if (!dl.wasPaused()) {
            dl.resume();
        }

        try {
            noSpaceAvailableInCurrentMount = SystemUtils.getCurrentMountAvailableBytes() < size;
        } catch (Throwable t) {

        }
    }

    public BTDownload getDl() {
        return dl;
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

    public boolean hasPaymentOptions() {
        return this.dl.getPaymentOptions() != null;
    }

    public PaymentOptions getPaymentOptions() {
        return this.dl.getPaymentOptions();
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
    public File getSavePath() {
        return dl.getSavePath();
    }

    @Override
    public boolean isDownloading() {
        return dl.isDownloading();
    }

    @Override
    public void cancel(boolean deleteData) {
        manager.remove(this);
        dl.remove(deleteData);
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String getStatus() {
        if (noSpaceAvailableInCurrentMount) {
            return TransferState.ERROR_DISK_FULL.toString();
        }
        return dl.getState().toString();
    }

    @Override
    public int getProgress() {
        return dl.getProgress();
    }

    @Override
    public long getSize() {
        return size;
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
    public List<TransferItem> getItems() {
        return items;
    }

    @Override
    public void cancel() {
        cancel(false);
    }

    @Override
    public String getDetailsUrl() {
        return null;
    }

    private class StatusListener implements BTDownloadListener {

        @Override
        public void update(BTDownload dl) {
            displayName = dl.getDisplayName();
            size = calculateSize(dl);
            items = calculateItems(dl);
        }

        @Override
        public void finished(BTDownload dl) {
            pauseSeedingIfNecessary(dl);
            TransferManager.instance().incrementDownloadsToReview();
            File saveLocation = getSavePath().getAbsoluteFile();
            Engine.instance().notifyDownloadFinished(getDisplayName(), saveLocation, dl.getInfoHash());
            Librarian.instance().scan(saveLocation);
        }

        private void pauseSeedingIfNecessary(BTDownload dl) {
            boolean seedFinishedTorrents = ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_TORRENT_SEED_FINISHED_TORRENTS);
            boolean seedFinishedTorrentsOnWifiOnly = ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_TORRENT_SEED_FINISHED_TORRENTS_WIFI_ONLY);
            boolean isDataWIFIUp = NetworkManager.instance().isDataWIFIUp();
            if (!seedFinishedTorrents || (!isDataWIFIUp && seedFinishedTorrentsOnWifiOnly)) {
                dl.pause();
            }
        }

        @Override
        public void removed(BTDownload dl, Set<File> incompleteFiles) {
            finalCleanup(incompleteFiles);
        }
    }

    private void finalCleanup(Set<File> incompleteFiles) {
        for (File f : incompleteFiles) {
            try {
                if (f.exists() && !f.delete()) {
                    LOG.info("Can't delete file: " + f);
                }
            } catch (Throwable e) {
                LOG.info("Can't delete file: " + f);
            }
        }

        DirectoryUtils.deleteEmptyDirectoryRecursive(dl.getSavePath());
    }

    private long calculateSize(BTDownload dl) {
        long size = dl.getSize();

        boolean partial = dl.isPartial();
        if (partial) {
            List<com.frostwire.transfers.TransferItem> items = dl.getItems();

            long totalSize = 0;
            for (com.frostwire.transfers.TransferItem item : items) {
                if (!item.isSkipped()) {
                    totalSize += item.getSize();
                }
            }

            if (totalSize > 0) {
                size = totalSize;
            }
        }

        return size;
    }

    private List<TransferItem> calculateItems(BTDownload dl) {
        List<TransferItem> l = new LinkedList<TransferItem>();

        for (TransferItem item : dl.getItems()) {
            if (!item.isSkipped()) {
                l.add(item);
            }
        }

        return l;
    }
}
