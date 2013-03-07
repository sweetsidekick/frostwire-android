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

import com.frostwire.android.util.FilenameUtils;
import com.frostwire.search.TorrentSearchResult;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public class BittorrentWebSearchResult implements BittorrentSearchResult {

    private final TorrentSearchResult webResult;

    public BittorrentWebSearchResult(TorrentSearchResult webResult) {
        this.webResult = webResult;
    }

    public String getDisplayName() {
        return FilenameUtils.getName(webResult.getFilename());
    }

    public String getFileName() {
        return webResult.getFilename();
    }

    public int getRank() {
        return webResult.getSeeds();
    }

    public long getSize() {
        return webResult.getSize();
    }

    public long getCreationTime() {
        return webResult.getCreationTime();
    }

    public String getHash() {
        return webResult.getHash();
    }

    public String getDetailsUrl() {
        return webResult.getDetailsUrl();
    }

    public String getTorrentURI() {
        return webResult.getTorrentURI();
    }

    public String getSource() {
        return webResult.getSource();
    }

    @Override
    public String toString() {
        return "(" + "torrent:" + getTorrentURI() + ")";
    }
}
