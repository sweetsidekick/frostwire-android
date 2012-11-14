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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.global.GlobalManager;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.frostwire.android.core.ConfigurationManager;
import com.frostwire.android.core.Constants;
import com.frostwire.android.core.DesktopUploadRequest;
import com.frostwire.android.core.FileDescriptor;
import com.frostwire.android.gui.NetworkManager;
import com.frostwire.android.gui.Peer;
import com.frostwire.android.gui.search.BittorrentIntentFileResult;
import com.frostwire.android.gui.search.BittorrentIntentHttpResult;
import com.frostwire.android.gui.search.BittorrentSearchResult;
import com.frostwire.android.gui.search.SearchResult;
import com.frostwire.android.gui.search.SoundcloudEngineSearchResult;
import com.frostwire.android.gui.search.YouTubeEngineSearchResult;
import com.frostwire.android.util.ByteUtils;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public final class TransferManager {

    private static final String TAG = "FW.TransferManager";

    private final List<DownloadTransfer> downloads;
    private final List<UploadTransfer> uploads;
    private final List<BittorrentDownload> bittorrenDownloads;

    private int downloadsToReview;

    private static TransferManager instance;

    public static TransferManager instance() {
        if (instance == null) {
            instance = new TransferManager();
        }
        return instance;
    }

    private TransferManager() {
        this.downloads = new LinkedList<DownloadTransfer>();
        this.uploads = new LinkedList<UploadTransfer>();
        this.bittorrenDownloads = new LinkedList<BittorrentDownload>();

        this.downloadsToReview = 0;
    }

    public List<Transfer> getTransfers() {
        List<Transfer> transfers = new ArrayList<Transfer>();

        transfers.addAll(downloads);
        transfers.addAll(uploads);
        transfers.addAll(bittorrenDownloads);

        return transfers;
    }

    private boolean alreadyDownloading(SearchResult sr) {
        for (DownloadTransfer dt : downloads) {
            if (dt.isDownloading()) {
                if (dt instanceof TaggableTransfer<?>) {
                    if (sr.equals(((TaggableTransfer<?>) dt).getTag())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    public DownloadTransfer download(SearchResult sr) throws Exception {

        if (alreadyDownloading(sr)) {
            return new ExistingDownload();
        }
        
        if (sr instanceof BittorrentSearchResult) {
            return newBittorrentDownload((BittorrentSearchResult) sr);
        } else if (sr instanceof HttpSlideSearchResult) {
            return newHttpDownload((HttpSlideSearchResult) sr);
        } else if (sr instanceof YouTubeEngineSearchResult) {
            return newYouTubeDownload((YouTubeEngineSearchResult) sr);
        } else if (sr instanceof SoundcloudEngineSearchResult) {
            return newSoundcloudDownload((SoundcloudEngineSearchResult) sr);
        } else {
            return new InvalidDownload();
        }
    }

    public void download(Peer peer, FileDescriptor fd) {
        PeerHttpDownload download = new PeerHttpDownload(this, peer, fd);

        downloads.add(download);

        download.start();
    }

    public PeerHttpUpload upload(FileDescriptor fd) {
        PeerHttpUpload upload = new PeerHttpUpload(this, fd);
        uploads.add(upload);
        return upload;
    }

    public DesktopTransfer desktopTransfer(DesktopUploadRequest dur, FileDescriptor fd) {
        DesktopTransfer transfer = null;

        for (DownloadTransfer downloadTransfer : downloads) {
            if (downloadTransfer instanceof DesktopTransfer) {
                DesktopTransfer desktopTransfer = (DesktopTransfer) downloadTransfer;
                if (desktopTransfer.getDUR().equals(dur)) {
                    transfer = desktopTransfer;
                    break;
                }
            }
        }

        if (transfer == null) {
            transfer = new DesktopTransfer(this, dur, fd);
            downloads.add(transfer);
        } else {
            transfer.addFileDescriptor(fd);
        }

        return transfer;
    }

    public void clearComplete() {
        List<Transfer> transfers = getTransfers();

        for (Transfer transfer : transfers) {
            if (transfer.isComplete()) {
                if (transfer instanceof BittorrentDownload) {
                    BittorrentDownload bd = (BittorrentDownload) transfer;
                    if (bd.isResumable()) {
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

        for (BittorrentDownload d : bittorrenDownloads) {
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

        for (BittorrentDownload d : bittorrenDownloads) {
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
        long torrenDownloadsBandwidth = AzureusManager.isCreated() ? AzureusManager.instance().getGlobalManager().getStats().getDataReceiveRate() / 1000 : 0;

        long peerDownloadsBandwidth = 0;
        for (DownloadTransfer d : downloads) {
            peerDownloadsBandwidth += d.getDownloadSpeed() / 1000;
        }

        return torrenDownloadsBandwidth + peerDownloadsBandwidth;
    }

    public double getUploadsBandwidth() {
        long torrenUploadsBandwidth = AzureusManager.isCreated() ? AzureusManager.instance().getGlobalManager().getStats().getDataSendRate() / 1000 : 0;

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
        for (BittorrentDownload d : bittorrenDownloads) {
            if (d.isSeeding() || d.isComplete()) {
                d.pause();
            }
        }
    }

    public void loadTorrents() {
        bittorrenDownloads.clear();

        if (!AzureusManager.isCreated()) {
            return;
        }

        GlobalManager globalManager = AzureusManager.instance().getAzureusCore().getGlobalManager();
        List<?> downloadManagers = globalManager.getDownloadManagers();

        List<DownloadManager> downloads = new ArrayList<DownloadManager>();
        for (Object obj : downloadManagers) {
            if (obj instanceof DownloadManager) {
                try {
                    if (((DownloadManager) obj).getTorrent() != null && ((DownloadManager) obj).getTorrent().getHash() != null) {
                        Log.d(TAG, "Loading torrent with hash: " + ByteUtils.encodeHex(((DownloadManager) obj).getTorrent().getHash()));
                        downloads.add((DownloadManager) obj);
                    }
                } catch (Throwable e) {
                    // ignore
                    Log.d(TAG, "error loading torrent (not the end of the world, keep going)");
                }
            }
        }

        boolean stop = false;
        if (!ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_TORRENT_SEED_FINISHED_TORRENTS)) {
            stop = true;
        } else {
            if (!NetworkManager.instance().isDataWIFIUp() && ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_TORRENT_SEED_FINISHED_TORRENTS_WIFI_ONLY)) {
                stop = true;
            }
        }

        for (DownloadManager dm : downloads) {
            if (stop && TorrentUtil.isComplete(dm)) {
                TorrentUtil.stop(dm);
            }

            bittorrenDownloads.add(BittorrentDownloadCreator.create(this, dm));
        }
    }

    List<BittorrentDownload> getBittorrentDownloads() {
        return new LinkedList<BittorrentDownload>(bittorrenDownloads);
    }

    boolean remove(Transfer transfer) {
        if (transfer instanceof BittorrentDownload) {
            return bittorrenDownloads.remove(transfer);
        } else if (transfer instanceof DownloadTransfer) {
            return downloads.remove(transfer);
        } else if (transfer instanceof UploadTransfer) {
            return uploads.remove(transfer);
        }

        return false;
    }

    public void pauseTorrents() {
        for (BittorrentDownload d : bittorrenDownloads) {
            d.pause();
        }
    }

    /**
     * Start a torrent download from an intent.
     */
    public void download(Intent intent) {
        Uri torrentURI = intent.getData();

        boolean isFile = torrentURI.getScheme().equalsIgnoreCase("file");

        try {
            TransferManager.instance().download(isFile ? new BittorrentIntentFileResult(intent) : new BittorrentIntentHttpResult(intent));
        } catch (Throwable e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    private BittorrentDownload newBittorrentDownload(BittorrentSearchResult sr) throws Exception {
        BittorrentDownload download = BittorrentDownloadCreator.create(this, sr);

        if (!(download instanceof InvalidBittorrentDownload)) {
            bittorrenDownloads.add(download);
        }

        return download;
    }

    private HttpDownload newHttpDownload(HttpSlideSearchResult sr) throws Exception {
        HttpDownload download = new HttpDownload(this, sr.getDownloadLink());

        downloads.add(download);
        download.start();

        return download;
    }

    private DownloadTransfer newYouTubeDownload(YouTubeEngineSearchResult sr) {
        YouTubeDownload download = new YouTubeDownload(this, sr);

        downloads.add(download);
        download.start();

        return download;
    }

    private DownloadTransfer newSoundcloudDownload(SoundcloudEngineSearchResult sr) {
        SoundcloudDownload download = new SoundcloudDownload(this, sr);

        downloads.add(download);
        download.start();

        return download;
    }
    
    
}
