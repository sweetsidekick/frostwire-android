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

package com.frostwire.websearch.youtube;

import java.util.ArrayList;
import java.util.List;

import com.frostwire.android.util.JsonUtils;
import com.frostwire.android.util.UrlUtils;
import com.frostwire.websearch.HttpClient;
import com.frostwire.websearch.WebSearchPerformer;
import com.frostwire.websearch.WebSearchResult;
import com.frostwire.websearch.youtube.YouTubeSearchResult.ResultType;

/**
 * 
 * @author gubatron
 * @author aldenml
 *
 */
public class YouTubeSearchPerformer implements WebSearchPerformer {

    private static final int YOUTUBE_MAX_RESULTS = 5;

    @Override
    public List<WebSearchResult> search(String keywords) {
        List<WebSearchResult> result = new ArrayList<WebSearchResult>();

        YouTubeResponse response = searchYouTube(keywords);

        if (response != null && response.feed != null && response.feed.entry != null)
            for (YouTubeEntry entry : response.feed.entry) {

                WebSearchResult vsr = new YouTubeSearchResult(entry, ResultType.VIDEO);
                result.add(vsr);
                WebSearchResult asr = new YouTubeSearchResult(entry, ResultType.AUDIO);
                result.add(asr);
            }

        return result;
    }

    private YouTubeResponse searchYouTube(String keywords) {
        String q = UrlUtils.encode(keywords);
        int maxResults = YOUTUBE_MAX_RESULTS;

        String url = String.format("https://gdata.youtube.com/feeds/api/videos?q=%s&orderby=relevance&start-index=1&max-results=%d&alt=json&prettyprint=true&v=2", q, maxResults);

        HttpClient c = new HttpClient(url);

        String json = c.get();

        json = fixJson(json);

        YouTubeResponse response = JsonUtils.toObject(json, YouTubeResponse.class);

        return response;
    }

    private String fixJson(String json) {
        return json.replace("\"$t\"", "\"title\"").replace("\"yt$userId\"", "\"ytuserId\"");
    }
}
