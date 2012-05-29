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

import java.util.ArrayList;
import java.util.List;

import android.util.Log;

import com.frostwire.android.bittorrent.websearch.WebSearchResult;
import com.frostwire.android.core.SearchEngine;

/**
 * @author gubatron
 * @author aldenml
 * 
 */
class EngineSearchTask extends SearchTask {

    private static final String TAG = "FW.EngineSearchTask";

    private final SearchEngine se;
    private final SearchResultDisplayer srd;
    private final String query;

    public EngineSearchTask(SearchEngine se, SearchResultDisplayer srd, String query) {
        super("EngineSearchTask - " + se.getName());
        this.se = se;
        this.srd = srd;
        this.query = query;
    }

    public void run() {
        try {
            List<WebSearchResult> webResults = se.getPerformer().search(query);
            List<SearchResult> results = normalizeWebResults(se, webResults);
            Log.d(TAG, "SearchEngine " + se.getName() + " with " + results.size() + " results");
            srd.addResults(results);
        } catch (Throwable e) {
            Log.e(TAG, String.format("Error getting data from search engine %s", se.getName()), e);
        }
    }

    private static List<SearchResult> normalizeWebResults(SearchEngine se, List<WebSearchResult> webResults) {
        List<SearchResult> result = new ArrayList<SearchResult>(webResults.size());
        for (WebSearchResult webResult : webResults) {
            SearchResult sr = new BittorrentWebSearchResult(se, webResult);
            result.add(sr);
        }
        return result;
    }
}
