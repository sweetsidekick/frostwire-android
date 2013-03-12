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

package com.frostwire.frostclick;

import com.frostwire.android.gui.transfers.HttpDownloadLink;
import com.frostwire.android.util.FilenameUtils;
import com.frostwire.search.WebSearchResult;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public class HttpSlideSearchResult implements WebSearchResult {

    private final Slide slide;

    public HttpSlideSearchResult(Slide slide) {
        this.slide = slide;
    }

    @Override
    public String getDisplayName() {
        return slide.title;
    }

    @Override
    public long getSize() {
        return slide.size;
    }

    public String getHttpUrl() {
        return slide.httpUrl;
    }

    public boolean isCompressed() {
        return slide.uncompress;
    }

    @Override
    public String getFilename() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getSource() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getDetailsUrl() {
        // TODO Auto-generated method stub
        return null;
    }

    public HttpDownloadLink getDownloadLink() {
        HttpDownloadLink link = new HttpDownloadLink(slide.httpUrl);
        link.setDisplayName(slide.title);
        link.setSize(slide.size);
        link.setCompressed(slide.uncompress);
        link.setFileName(FilenameUtils.getName(slide.httpUrl));
        return link;
    }
    
    @Override
    public long getCreationTime() {
        // TODO Auto-generated method stub
        return 0;
    }
}
