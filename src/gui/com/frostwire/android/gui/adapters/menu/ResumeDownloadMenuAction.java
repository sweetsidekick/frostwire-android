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

package com.frostwire.android.gui.adapters.menu;

import android.content.Context;

import com.frostwire.android.R;
import com.frostwire.android.core.ConfigurationManager;
import com.frostwire.android.core.Constants;
import com.frostwire.android.gui.NetworkManager;
import com.frostwire.android.gui.transfers.BittorrentDownload;
import com.frostwire.android.gui.transfers.TransferManager;
import com.frostwire.android.gui.util.UIUtils;
import com.frostwire.android.gui.views.MenuAction;
import com.frostwire.logging.Logger;
import com.frostwire.torrent.TorrentUtils;
import com.frostwire.uxstats.UXAction;
import com.frostwire.uxstats.UXStats;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public final class ResumeDownloadMenuAction extends MenuAction {
    private static final Logger LOG = Logger.getLogger(ResumeDownloadMenuAction.class);
    private final BittorrentDownload download;

    public ResumeDownloadMenuAction(Context context, BittorrentDownload download, int stringId) {
        super(context, R.drawable.contextmenu_icon_play_transfer, stringId);
        this.download = download;
    }
    
    public static int getMenuStringResId(BittorrentDownload download) {
        if (!download.isComplete()) {
            return R.string.resume_torrent_menu_action;
        }
        
        boolean seedTorrents = ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_TORRENT_SEED_FINISHED_TORRENTS);
        boolean seedTorrentsOnWifiOnly = ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_TORRENT_SEED_FINISHED_TORRENTS_WIFI_ONLY);
        
        if (!seedTorrents || (!NetworkManager.instance().isDataWIFIUp() && !seedTorrentsOnWifiOnly)) {
            
            return R.string.resume_torrent_menu_action;
        }
        
        return R.string.seed;
    }

    @Override
    protected void onClick(Context context) {
        if (NetworkManager.instance().isDataUp()) {
            if (download.isResumable()) {
                download.resume();
                UXStats.instance().log(UXAction.DOWNLOAD_RESUME);
            }
        } else {
            UIUtils.showShortMessage(context, R.string.please_check_connection_status_before_resuming_download);
        }
    }
}
