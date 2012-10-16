package com.frostwire.websearch.youtube;

import java.util.ArrayList;
import java.util.List;

import com.frostwire.android.util.JsonUtils;
import com.frostwire.android.util.UrlUtils;
import com.frostwire.websearch.HttpClient;
import com.frostwire.websearch.WebSearchPerformer;
import com.frostwire.websearch.WebSearchResult;
import com.frostwire.websearch.youtube.YouTubeSearchResult.ResultType;

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
