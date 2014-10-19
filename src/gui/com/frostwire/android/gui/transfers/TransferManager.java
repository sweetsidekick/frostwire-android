/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2013, FrostWire(R). All rights reserved.
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

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;

import com.frostwire.android.R;
import com.frostwire.android.core.ConfigurationManager;
import com.frostwire.android.core.Constants;
import com.frostwire.android.core.FileDescriptor;
import com.frostwire.android.gui.Librarian;
import com.frostwire.android.gui.NetworkManager;
import com.frostwire.android.gui.Peer;
import com.frostwire.android.gui.services.Engine;
import com.frostwire.android.gui.util.SystemUtils;
import com.frostwire.logging.Logger;
import com.frostwire.search.HttpSearchResult;
import com.frostwire.search.SearchResult;
import com.frostwire.search.soundcloud.SoundcloudSearchResult;
import com.frostwire.search.torrent.TorrentCrawledSearchResult;
import com.frostwire.search.torrent.TorrentSearchResult;
import com.frostwire.search.youtube.YouTubeCrawledSearchResult;
import com.frostwire.util.ByteUtils;
import com.frostwire.util.StringUtils;
import com.frostwire.uxstats.UXAction;
import com.frostwire.uxstats.UXStats;
import com.frostwire.vuze.VuzeDownloadFactory;
import com.frostwire.vuze.VuzeDownloadListener;
import com.frostwire.vuze.VuzeDownloadManager;
import com.frostwire.vuze.VuzeKeys;
import com.frostwire.vuze.VuzeManager;
import com.frostwire.vuze.VuzeUtils;
import com.frostwire.vuze.VuzeManager.LoadTorrentsListener;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public final class TransferManager implements VuzeKeys {

    private static final Logger LOG = Logger.getLogger(TransferManager.class);

    private final List<DownloadTransfer> downloads;
    private final List<UploadTransfer> uploads;
    private final List<BittorrentDownload> bittorrentDownloads;

    private int downloadsToReview;

    private final Object alreadyDownloadingMonitor = new Object();

    private volatile static TransferManager instance;

    private OnSharedPreferenceChangeListener preferenceListener;

    public static TransferManager instance() {
        if (instance == null) {
            instance = new TransferManager();
        }
        return instance;
    }

    private TransferManager() {
        registerPreferencesChangeListener();

        this.downloads = new CopyOnWriteArrayList<DownloadTransfer>();
        this.uploads = new CopyOnWriteArrayList<UploadTransfer>();
        this.bittorrentDownloads = new CopyOnWriteArrayList<BittorrentDownload>();

        this.downloadsToReview = 0;

        loadTorrents();
    }

    public List<Transfer> getTransfers() {
        List<Transfer> transfers = new ArrayList<Transfer>();

        if (downloads != null) {
            transfers.addAll(downloads);
        }

        if (uploads != null) {
            transfers.addAll(uploads);
        }

        if (bittorrentDownloads != null) {
            transfers.addAll(bittorrentDownloads);
        }

        return transfers;
    }

    private boolean alreadyDownloading(String detailsUrl) {
        synchronized (alreadyDownloadingMonitor) {
            for (DownloadTransfer dt : downloads) {
                if (dt.isDownloading()) {
                    if (dt.getDetailsUrl() != null && dt.getDetailsUrl().equals(detailsUrl)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean alreadyDownloadingByInfoHash(String infohash) {
        synchronized (alreadyDownloadingMonitor) {
            for (BittorrentDownload bt : bittorrentDownloads) {
                if (bt.getHash().equalsIgnoreCase(infohash)) {
                    return true;
                }
            }
        }
        return false;
    }

    
    public DownloadTransfer download(SearchResult sr) {
        DownloadTransfer transfer = new InvalidDownload();
        
        if (alreadyDownloading(sr.getDetailsUrl())) {
            transfer = new ExistingDownload();
        }

        if (sr instanceof TorrentSearchResult) {
            transfer = newBittorrentDownload((TorrentSearchResult) sr);
        } else if (sr instanceof HttpSlideSearchResult) {
            transfer = newHttpDownload((HttpSlideSearchResult) sr);
        } else if (sr instanceof YouTubeCrawledSearchResult) {
            transfer = newYouTubeDownload((YouTubeCrawledSearchResult) sr);
        } else if (sr instanceof SoundcloudSearchResult) {
            transfer = newSoundcloudDownload((SoundcloudSearchResult) sr);
        } else if (sr instanceof HttpSearchResult) {
            transfer = newHttpDownload((HttpSearchResult) sr);
        }
        
        if (isBittorrentDownloadAndMobileDataSavingsOn(transfer)) {
            //give it time to get to a pausable state.
            try { Thread.sleep(5000);  } catch (Throwable t) { /*meh*/ }
            enqueueTorrentTransfer(transfer);
            //give it time to stop before onPostExecute
            try { Thread.sleep(5000);  } catch (Throwable t) { /*meh*/ }
        }
        
        return transfer;
    }
    
    private void enqueueTorrentTransfer(DownloadTransfer transfer) {
        if (transfer instanceof AzureusBittorrentDownload) {
            AzureusBittorrentDownload btDownload = (AzureusBittorrentDownload) transfer;
            btDownload.enqueue();
        } else if (transfer instanceof TorrentFetcherDownload){
            TorrentFetcherDownload btDownload = (TorrentFetcherDownload) transfer;
            btDownload.enqueue();
        }
    }


    public DownloadTransfer download(Peer peer, FileDescriptor fd) {
        PeerHttpDownload download = new PeerHttpDownload(this, peer, fd);

        if (alreadyDownloading(download.getDetailsUrl())) {
            return new ExistingDownload();
        }

        downloads.add(download);
        download.start();

        UXStats.instance().log(UXAction.WIFI_SHARING_DOWNLOAD);

        return download;
    }

    public PeerHttpUpload upload(FileDescriptor fd) {
        PeerHttpUpload upload = new PeerHttpUpload(this, fd);
        uploads.add(upload);
        return upload;
    }

    public void clearComplete() {
        List<Transfer> transfers = getTransfers();

        for (Transfer transfer : transfers) {
            if (transfer != null && transfer.isComplete()) {
                if (transfer instanceof BittorrentDownload) {
                    BittorrentDownload bd = (BittorrentDownload) transfer;
                    if (bd != null && bd.isResumable()) {
                        bd.cancel();
                    }
                } else {
                    transfer.cancel();
                }
            }
        }
    }

    public int getActiveDownloads() {
        int count = 0;

        for (BittorrentDownload d : bittorrentDownloads) {
            if (!d.isComplete() && d.isDownloading()) {
                count++;
            }
        }

        for (DownloadTransfer d : downloads) {
            if (!d.isComplete() && d.isDownloading()) {
                count++;
            }
        }

        return count;
    }

    public int getActiveUploads() {
        int count = 0;

        for (BittorrentDownload d : bittorrentDownloads) {
            if (!d.isComplete() && d.isSeeding()) {
                count++;
            }
        }

        for (UploadTransfer u : uploads) {
            if (!u.isComplete() && u.isUploading()) {
                count++;
            }
        }

        return count;
    }

    public long getDownloadsBandwidth() {
        long torrenDownloadsBandwidth = VuzeManager.getInstance().getDataReceiveRate();

        long peerDownloadsBandwidth = 0;
        for (DownloadTransfer d : downloads) {
            peerDownloadsBandwidth += d.getDownloadSpeed() / 1000;
        }

        return torrenDownloadsBandwidth + peerDownloadsBandwidth;
    }

    public double getUploadsBandwidth() {
        long torrenUploadsBandwidth = VuzeManager.getInstance().getDataSendRate();

        long peerUploadsBandwidth = 0;
        for (UploadTransfer u : uploads) {
            peerUploadsBandwidth += u.getUploadSpeed() / 1000;
        }

        return torrenUploadsBandwidth + peerUploadsBandwidth;
    }

    public int getDownloadsToReview() {
        return downloadsToReview;
    }

    public void incrementDownloadsToReview() {
        downloadsToReview++;
    }

    public void clearDownloadsToReview() {
        downloadsToReview = 0;
    }

    public void stopSeedingTorrents() {
        for (BittorrentDownload d : bittorrentDownloads) {
            if (d.isSeeding() || d.isComplete()) {
                d.pause();
            }
        }
    }

    public void loadTorrents() {
        bittorrentDownloads.clear();

        boolean stop = false;
        if (!ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_TORRENT_SEED_FINISHED_TORRENTS)) {
            stop = true;
        } else {
            if (!NetworkManager.instance().isDataWIFIUp() && ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_TORRENT_SEED_FINISHED_TORRENTS_WIFI_ONLY)) {
                stop = true;
            }
        }

        VuzeManager.getInstance().loadTorrents(stop, new LoadTorrentsListener() {

            @Override
            public void onLoad(List<VuzeDownloadManager> dms) {
                for (VuzeDownloadManager dm : dms) {
                    bittorrentDownloads.add(new AzureusBittorrentDownload(TransferManager.this, dm));
                }
            }
        }, new DownloadListener());
    }

    boolean remove(Transfer transfer) {
        if (transfer instanceof BittorrentDownload) {
            return bittorrentDownloads.remove(transfer);
        } else if (transfer instanceof DownloadTransfer) {
            return downloads.remove(transfer);
        } else if (transfer instanceof UploadTransfer) {
            return uploads.remove(transfer);
        }

        return false;
    }

    public void pauseTorrents() {
        for (BittorrentDownload d : bittorrentDownloads) {
            d.pause();
        }
    }

    public BittorrentDownload downloadTorrent(String uri) {
        try {
            URI u = URI.create(uri);

            BittorrentDownload download = null;

            if (u.getScheme().equalsIgnoreCase("file")) {
                download = new AzureusBittorrentDownload(this, createVDM(u.getPath(), null));
            } else if (u.getScheme().equalsIgnoreCase("http") || u.getScheme().equalsIgnoreCase("magnet")) {
                download = new TorrentFetcherDownload(this, new TorrentUrlInfo(uri.toString()));
            } else {
                download = new InvalidBittorrentDownload(R.string.torrent_scheme_download_not_supported);
            }
            if (!(download instanceof InvalidBittorrentDownload)) {
                if ((download instanceof AzureusBittorrentDownload && !alreadyDownloadingByInfoHash(download.getHash())) ||
                    (download instanceof TorrentFetcherDownload && !alreadyDownloading(uri.toString()))) {
                    if (!bittorrentDownloads.contains(download)) {
                        bittorrentDownloads.add(download);
                        
                        if (isBittorrentDownloadAndMobileDataSavingsOn(download)) {
                            //give it time to get to a pausable state.
                            try { Thread.sleep(5000);  } catch (Throwable t) { /*meh*/ }
                            enqueueTorrentTransfer(download);
                            //give it time to stop before onPostExecute
                            try { Thread.sleep(5000);  } catch (Throwable t) { /*meh*/ }
                        }
                    }
                }
            }

            return download;
        } catch (Throwable e) {
            LOG.warn("Error creating download from uri: " + uri);
            return new InvalidBittorrentDownload(R.string.empty_string);
        }
    }

    private static BittorrentDownload createBittorrentDownload(TransferManager manager, TorrentSearchResult sr) {
        if (StringUtils.isNullOrEmpty(sr.getHash())) {
            return new TorrentFetcherDownload(manager, new TorrentSearchResultInfo(sr));
        } else {
            VuzeDownloadManager dm = VuzeManager.getInstance().find(ByteUtils.decodeHex(sr.getHash()));
            if (dm == null) {// new download, I need to download the torrent
                return new TorrentFetcherDownload(manager, new TorrentSearchResultInfo(sr));
            } else {
                if (sr instanceof TorrentCrawledSearchResult) {
                    Set<String> paths = new HashSet<String>();
                    paths.add(sr.getFilename());
                    dm.setSkipped(paths, false);
                } else {
                    dm.setSkipped(null, false);
                }
            }
            return new AzureusBittorrentDownload(manager, dm);
        }
    }

    private BittorrentDownload newBittorrentDownload(TorrentSearchResult sr) {
        try {
            BittorrentDownload dl = createBittorrentDownload(this, sr);

            if (!bittorrentDownloads.contains(dl)) {
                bittorrentDownloads.add(dl);
            }

            return dl;
        } catch (Throwable e) {
            LOG.warn("Error creating download from search result: " + sr);
            return new InvalidBittorrentDownload(R.string.empty_string);
        }
    }

    private HttpDownload newHttpDownload(HttpSlideSearchResult sr) {
        HttpDownload download = new HttpDownload(this, sr.getDownloadLink());

        downloads.add(download);
        download.start();

        return download;
    }

    private DownloadTransfer newYouTubeDownload(YouTubeCrawledSearchResult sr) {
        YouTubeDownload download = new YouTubeDownload(this, sr);

        downloads.add(download);
        download.start();

        return download;
    }

    private DownloadTransfer newSoundcloudDownload(SoundcloudSearchResult sr) {
        SoundcloudDownload download = new SoundcloudDownload(this, sr);

        downloads.add(download);
        download.start();

        return download;
    }

    private DownloadTransfer newHttpDownload(HttpSearchResult sr) {
        HttpDownload download = new HttpDownload(this, new HttpSearchResultDownloadLink(sr));

        downloads.add(download);
        download.start();

        return download;
    }
    
    private boolean isBittorrentDownload(DownloadTransfer transfer) {
        return transfer instanceof AzureusBittorrentDownload || transfer instanceof TorrentFetcherDownload;
    }

    public boolean isBittorrentDownloadAndMobileDataSavingsOn(DownloadTransfer transfer) {
        return isBittorrentDownload(transfer) && 
                NetworkManager.instance().isDataMobileUp() && 
                !ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_NETWORK_USE_MOBILE_DATA);
    }
    
    public boolean isBittorrentDownloadAndMobileDataSavingsOff(DownloadTransfer transfer) {
        return isBittorrentDownload(transfer) && 
               NetworkManager.instance().isDataMobileUp() && 
               ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_NETWORK_USE_MOBILE_DATA);
    }
    
    public boolean isBittorrentDisconnected(){
       return Engine.instance().isStopped() || Engine.instance().isStopping() || Engine.instance().isDisconnected();
    }
    
    public void resumeResumableTransfers() {
        List<Transfer> transfers = getTransfers();

        for (Transfer t : transfers) {
            if (t instanceof BittorrentDownload) {
                BittorrentDownload bt = (BittorrentDownload) t;
                if (bt.isResumable()) {
                    bt.resume();
                }
            } 
        }        
    }

    /** Stops all HttpDownloads (Cloud and Wi-Fi) */
    public void stopHttpTransfers() {
        List<Transfer> transfers = new ArrayList<Transfer>();
        transfers.addAll(downloads);
        transfers.addAll(uploads);

        for (Transfer t : transfers) {
            if (t instanceof DownloadTransfer) {
                DownloadTransfer d = (DownloadTransfer) t;
                if (!d.isComplete() && d.isDownloading()) {
                    d.cancel();
                }
            } else if (t instanceof UploadTransfer) {
                UploadTransfer u = (UploadTransfer) t;

                if (!u.isComplete() && u.isUploading()) {
                    u.cancel();
                }
            }
        }
    }

    private void registerPreferencesChangeListener() {
        preferenceListener = new OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (key.equals(Constants.PREF_KEY_TORRENT_MAX_DOWNLOAD_SPEED)) {
                    setAzureusParameter(MAX_DOWNLOAD_SPEED);
                } else if (key.equals(Constants.PREF_KEY_TORRENT_MAX_UPLOAD_SPEED)) {
                    setAzureusParameter(MAX_UPLOAD_SPEED);
                } else if (key.equals(Constants.PREF_KEY_TORRENT_MAX_DOWNLOADS)) {
                    setAzureusParameter(MAX_DOWNLOADS);
                } else if (key.equals(Constants.PREF_KEY_TORRENT_MAX_UPLOADS)) {
                    setAzureusParameter(MAX_UPLOADS);
                } else if (key.equals(Constants.PREF_KEY_TORRENT_MAX_TOTAL_CONNECTIONS)) {
                    setAzureusParameter(MAX_TOTAL_CONNECTIONS);
                } else if (key.equals(Constants.PREF_KEY_TORRENT_MAX_TORRENT_CONNECTIONS)) {
                    setAzureusParameter(MAX_TORRENT_CONNECTIONS);
                }
            }
        };
        ConfigurationManager.instance().registerOnPreferenceChange(preferenceListener);
    }

    private void setAzureusParameter(String key) {
        VuzeManager.getInstance().setParameter(key, ConfigurationManager.instance().getLong(key));
    }

    VuzeDownloadManager createVDM(String path, Set<String> selection) throws IOException {
        VuzeDownloadManager dm = VuzeDownloadFactory.create(path, selection, SystemUtils.getTorrentDataDirectory().getAbsolutePath(), new DownloadListener());

        return dm;
    }

    private static class DownloadListener implements VuzeDownloadListener {

        @Override
        public void stateChanged(VuzeDownloadManager dm, int state) {
            if (state == VuzeDownloadManager.STATE_SEEDING) {
                stopSeedingIfNecessary(dm);
            }
        }

        @Override
        public void downloadComplete(VuzeDownloadManager dm) {
            stopSeedingIfNecessary(dm);
            TransferManager.instance().incrementDownloadsToReview();
            // TODO:BITTORRENT
            //VuzeUtils.finalCleanup(dm.getDM()); //make sure it cleans unnecessary files (android has handpicked seeding off by default)
            Engine.instance().notifyDownloadFinished(dm.getDisplayName(), dm.getSavePath().getAbsoluteFile());
            Librarian.instance().scan(dm.getSavePath().getAbsoluteFile());
        }

        private void stopSeedingIfNecessary(VuzeDownloadManager dm) {
            boolean seedFinishedTorrents = ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_TORRENT_SEED_FINISHED_TORRENTS);
            boolean seedFinishedTorrentsOnWifiOnly = ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_TORRENT_SEED_FINISHED_TORRENTS_WIFI_ONLY);
            boolean isDataWIFIUp = NetworkManager.instance().isDataWIFIUp();
            if (!seedFinishedTorrents || (!isDataWIFIUp && seedFinishedTorrentsOnWifiOnly)) {
                dm.stop();
            }
        }
    }
}
