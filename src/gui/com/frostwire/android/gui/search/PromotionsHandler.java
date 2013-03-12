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

package com.frostwire.android.gui.search;

import com.frostwire.android.gui.views.PromotionsView.Slide;
import com.frostwire.search.SearchResult;

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
            return new TorrentPromotionSearchResult(slide);
        case Slide.DOWNLOAD_METHOD_HTTP:
            return new HttpSlideSearchResult(slide);
        default:
            return null;
        }
    }

    
}
