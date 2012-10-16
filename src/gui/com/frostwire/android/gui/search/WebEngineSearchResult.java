package com.frostwire.android.gui.search;

import com.frostwire.websearch.WebSearchResult;

public class WebEngineSearchResult implements SearchResult {

    private final WebSearchResult sr;

    public WebEngineSearchResult(WebSearchResult sr) {
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
}
