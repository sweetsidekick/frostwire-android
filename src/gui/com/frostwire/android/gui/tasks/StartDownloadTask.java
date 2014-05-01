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

package com.frostwire.android.gui.tasks;

import android.content.Context;

import com.frostwire.android.R;
import com.frostwire.android.core.ConfigurationManager;
import com.frostwire.android.core.Constants;
import com.frostwire.android.gui.NetworkManager;
import com.frostwire.android.gui.transfers.AzureusBittorrentDownload;
import com.frostwire.android.gui.transfers.DownloadTransfer;
import com.frostwire.android.gui.transfers.ExistingDownload;
import com.frostwire.android.gui.transfers.InvalidTransfer;
import com.frostwire.android.gui.transfers.TorrentFetcherDownload;
import com.frostwire.android.gui.transfers.TransferManager;
import com.frostwire.android.gui.util.UIUtils;
import com.frostwire.android.gui.views.ContextTask;
import com.frostwire.logging.Logger;
import com.frostwire.search.SearchResult;

/**
 * 
 * @author gubatron
 * @author aldenml
 * 
 */
public class StartDownloadTask extends ContextTask<DownloadTransfer> {

    private static final Logger LOG = Logger.getLogger(StartDownloadTask.class);

    private final SearchResult sr;
    private final String message;

    public StartDownloadTask(Context ctx, SearchResult sr, String message) {
        super(ctx);
        this.sr = sr;
        this.message = message;
    }

    private boolean isBittorrentDownload(DownloadTransfer transfer) {
        return transfer instanceof AzureusBittorrentDownload || transfer instanceof TorrentFetcherDownload;
    }

    private boolean isBittorrentDownloadAndMobileDataSavingsOn(DownloadTransfer transfer) {
        return isBittorrentDownload(transfer) && 
                NetworkManager.instance().isDataMobileUp() && 
                !ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_NETWORK_USE_MOBILE_DATA);
    }
    
    private boolean isBittorrentDownloadAndMobileDataSavingsOff(DownloadTransfer transfer) {
        return isBittorrentDownload(transfer) && 
               NetworkManager.instance().isDataMobileUp() && 
               ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_NETWORK_USE_MOBILE_DATA);
    }

    @Override
    protected DownloadTransfer doInBackground() {
        DownloadTransfer transfer = null;
        try {
            transfer = TransferManager.instance().download(sr);
            
            if (transfer != null && isBittorrentDownloadAndMobileDataSavingsOn(transfer)) {
                Thread.sleep(5000); //let it get to a pausable state.
                enqueueTorrentTransfer(transfer);
                Thread.sleep(1000); //let it stop. before onPostExecute
            }
        } catch (Throwable e) {
            LOG.warn("Error adding new download from result: " + sr, e);
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

    @Override
    protected void onPostExecute(Context ctx, DownloadTransfer transfer) {
        if (transfer != null) {
            if (!(transfer instanceof InvalidTransfer)) {
                if (isBittorrentDownloadAndMobileDataSavingsOn(transfer)) {
                    UIUtils.showLongMessage(ctx, R.string.torrent_transfer_enqueued_on_mobile_data);
                } else {
                    if (isBittorrentDownloadAndMobileDataSavingsOff(transfer)) {
                        UIUtils.showLongMessage(ctx, R.string.torrent_transfer_consuming_mobile_data);
                    }
                    UIUtils.showShortMessage(ctx, message);
                }
            } else {
                if (transfer instanceof ExistingDownload) {
                    //nothing happens here, the user should just see the transfer
                    //manager and we avoid adding the same transfer twice.
                } else {
                    UIUtils.showShortMessage(ctx, ((InvalidTransfer) transfer).getReasonResId());
                }
            }
        }
    }

}
