package com.frostwire.android.gui.search;

import com.frostwire.websearch.youtube.YouTubeSearchResult;
import com.frostwire.websearch.youtube.YouTubeSearchResult.ResultType;

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
