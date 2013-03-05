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

package com.frostwire.search.archive;

import com.frostwire.websearch.TorrentWebSearchResult;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public class ArchiveSearchResult implements TorrentWebSearchResult {

    private final ArchiveItem item;

    public ArchiveSearchResult(ArchiveItem item) {
        this.item = item;
    }

    public long getCreationTime() {
        return -1;
    }

    public String getFileName() {
        return null;
    }

    public String getHash() {
        return null;
    }

    public String getTorrentURI() {
        return null;
    }

    public long getSize() {
        return -1;
    }

    public int getRank() {
        return -1;
    }

    public String getTorrentDetailsURL() {
        return null;
    }

    @Override
    public String getDisplayName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getSource() {
        return "Archive.org";
    }

    @Override
    public String getDetailsUrl() {
        // TODO Auto-generated method stub
        return null;
    }
}
