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

import com.frostwire.android.core.ConfigurationManager;
import com.frostwire.android.core.Constants;
import com.frostwire.android.core.SearchEngine;
import com.frostwire.websearch.TorrentWebSearchResult;
import com.frostwire.websearch.WebSearchResult;
import com.frostwire.websearch.soundcloud.SoundcloudTrackSearchResult;
import com.frostwire.websearch.youtube.YouTubeSearchResult;

/**
 * @author gubatron
 * @author aldenml
 * 
 */
class EngineSearchTask extends SearchTask {

    private static final String TAG = "FW.EngineSearchTask";

    private final SearchEngine engine;
    private final String query;

    // filter constants
    private final int MIN_SEEDS_TORRENT_RESULT;

    public EngineSearchTask(SearchEngine engine, String query) {
        super("EngineSearchTask - " + engine.getName());
        this.engine = engine;
        this.query = query;

        MIN_SEEDS_TORRENT_RESULT = ConfigurationManager.instance().getInt(Constants.PREF_KEY_SEARCH_MIN_SEEDS_FOR_TORRENT_RESULT);
    }

    public void runTask() {
        if (isCancelled()) {
            return;
        }

        try {
            List<WebSearchResult> webResults = engine.getPerformer().search(query);

            if (!isCancelled()) {
                List<SearchResult> results = normalizeWebResults(webResults);
                LocalSearchEngine.instance().addResults(results);
            }
        } catch (Throwable e) {
            Log.e(TAG, "Error getting data from search engine " + engine.getName(), e);
        }
    }

    private List<SearchResult> normalizeWebResults(List<WebSearchResult> webResults) {
        List<SearchResult> result = new ArrayList<SearchResult>(webResults.size());
        for (WebSearchResult webResult : webResults) {
            if (webResult instanceof TorrentWebSearchResult) {
                TorrentWebSearchResult tsr = (TorrentWebSearchResult) webResult;
                if (filter(tsr)) {
                    BittorrentSearchResult sr = new BittorrentWebSearchResult(engine, tsr);
                    result.add(sr);
                }
            } else if (webResult instanceof YouTubeSearchResult) {
                YouTubeEngineSearchResult sr = new YouTubeEngineSearchResult((YouTubeSearchResult) webResult);
                result.add(sr);
            } else if (webResult instanceof SoundcloudTrackSearchResult) {
                SoundcloudEngineSearchResult sr = new SoundcloudEngineSearchResult((SoundcloudTrackSearchResult) webResult);
                result.add(sr);
            } else {
                //result.add(new WebEngineSearchResult(webResult));
            }
        }
        return result;
    }

    // this is a preliminary filter, since we need to provide the best user experience
    // we will remove "low quality" torrents, for example: low seeds, with bad names, etc.
    private boolean filter(TorrentWebSearchResult sr) {
        if (sr.getRank() < MIN_SEEDS_TORRENT_RESULT) {
            return false;
        }

        // more filter conditions here

        return true;
    }
}
