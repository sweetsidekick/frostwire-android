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

package com.frostwire.android.gui;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Looper;
import android.util.Log;

import com.frostwire.android.R;
import com.frostwire.android.core.ConfigurationManager;
import com.frostwire.android.core.Constants;
import com.frostwire.android.gui.activities.MainActivity;
import com.frostwire.android.gui.search.SearchResult;
import com.frostwire.android.gui.services.Engine;
import com.frostwire.android.gui.transfers.BittorrentPromotionSearchResult;
import com.frostwire.android.gui.transfers.DownloadTransfer;
import com.frostwire.android.gui.transfers.HttpSlideSearchResult;
import com.frostwire.android.gui.transfers.InvalidTransfer;
import com.frostwire.android.gui.transfers.TransferManager;
import com.frostwire.android.gui.util.UIUtils;
import com.frostwire.android.gui.views.NewTransferDialog;
import com.frostwire.android.gui.views.NewTransferDialog.OnYesNoListener;
import com.frostwire.android.util.JsonUtils;
import com.frostwire.android.util.StringUtils;

/**
 * 
 * @author gubatron
 * @author aldenml
 * 
 */
public class PromotionsHandler {

    private static final String TAG = "FW.PromotionHandler";

    private final Context context;

    public PromotionsHandler(Context context) {
        this.context = context;
    }

    public void handleSelection(String json) {
        try {
            Slide slide = JsonUtils.toObject(StringUtils.decodeUrl(json), Slide.class);
            startTransfer(slide);
        } catch (Throwable e) {
            Log.e(TAG, "Error processing promotion", e);
        }
    }

    public void startTransfer(final Slide slide) {
        final SearchResult sr = buildSearchResult(slide);
        if (sr == null) {

            //check if there is a URL available to open a web browser.
            if (slide.url != null) {
                Intent i = new Intent("android.intent.action.VIEW", Uri.parse(slide.url));
                context.startActivity(i);
            }
            
            return;
        }

        NewTransferDialog dlg = new NewTransferDialog(context, sr, false, new OnYesNoListener() {
            public void onYes(NewTransferDialog dialog) {
                // putting this logic in a thread to avoid ANR errors. Needs refactor to avoid context leaks
                Engine.instance().getThreadPool().execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            DownloadTransfer download = TransferManager.instance().download(sr);
                            if (!(download instanceof InvalidTransfer)) {
                                Looper.prepare();
                                UIUtils.showShortMessage(context, R.string.downloading_promotion, download.getDisplayName());
                                if (ConfigurationManager.instance().showTransfersOnDownloadStart()) {
                                    Intent i = new Intent(Constants.ACTION_SHOW_TRANSFERS);
                                    context.startActivity(i.setClass(context, MainActivity.class));
                                }
                            }
                        } catch (Throwable e) {
                            Log.e(TAG, "Error processing promotion", e);
                        }
                    }
                });
            }

            public void onNo(NewTransferDialog dialog) {
            }
        });

        dlg.show(); // this dialog will leak if the context is destroyed. Find a solution.
    }

    /**
     * This is to create a sort of "non real" search result.
     * @return
     */
    private SearchResult buildSearchResult(Slide slide) {
        switch (slide.method) {
        case Slide.DOWNLOAD_METHOD_TORRENT:
            return new BittorrentPromotionSearchResult(slide);
        case Slide.DOWNLOAD_METHOD_HTTP:
            return new HttpSlideSearchResult(slide);
        default:
            return null;
        }
    }

    public static class SlideList {
        public List<Slide> slides;
    }

    public static class Slide {

        /** Download the torrent file */
        public static final int DOWNLOAD_METHOD_TORRENT = 0;
        
        /** Download the file via HTTP */
        public static final int DOWNLOAD_METHOD_HTTP = 1;
        
        /**
         * http address where to go if user clicks on this slide
         */
        public String url;

        /**
         * Download method
         * 0 - Torrent
         * 1 - HTTP
         */
        public int method;

        /**
         * url of torrent file that should be opened if user clicks on this slide
         */
        public String torrent;

        public String httpUrl;

        public boolean uncompress;

        /**
         * url of image that will be displayed on this slide
         */
        public String imageSrc;

        /**
         * length of time this slide will be shown
         */
        public long duration;

        /**
         * language (optional filter) = Can be given in the forms of:
         * *
         * en
         * en_US
         * 
         */
        public String language;

        /**
         * os (optional filter) = Can be given in the forms of:
         * windows
         * mac
         * linux
         */
        public String os;

        /** Title of the promotion */
        public String title;

        /** Total size in bytes */
        public long size;
    }
}
