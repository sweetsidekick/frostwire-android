/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011, 2012, FrostWire(R). All rights reserved.
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

import com.frostwire.websearch.youtube.YouTubeSearchResult;
import com.frostwire.websearch.youtube.YouTubeSearchResult.ResultType;

/**
 * @author gubatron
 * @author aldenml
 * 
 */
public class YouTubeEngineSearchResult implements SearchResult {

    private final YouTubeSearchResult sr;

    public YouTubeEngineSearchResult(YouTubeSearchResult sr) {
        this.sr = sr;
    }

    @Override
    public String getDisplayName() {
        return sr.getDisplayName();
    }

    @Override
    public String getFileName() {
        return sr.getFileName();
    }

    @Override
    public long getSize() {
        return 0;
    }

    @Override
    public int getRank() {
        return sr.getRank();
    }

    @Override
    public String getSource() {
        return sr.getSource();
    }

    @Override
    public String getDetailsUrl() {
        return sr.getDetailsUrl();
    }

    public ResultType getResultType() {
        return sr.getResultType();
    }
}
