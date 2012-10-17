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

import com.frostwire.android.gui.search.SearchResult;
import com.frostwire.android.gui.transfers.BittorrentPromotionSearchResult;
import com.frostwire.android.gui.transfers.HttpSlideSearchResult;

/**
 * 
 * @author gubatron
 * @author aldenml
 * 
 */
public class PromotionsHandler {

    public PromotionsHandler() {
    }

    /**
     * This is to create a sort of "non real" search result.
     * @return
     */
    public SearchResult buildSearchResult(Slide slide) {
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
