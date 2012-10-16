package com.frostwire.android.gui.search;

import com.frostwire.websearch.soundcloud.SoundcloudTrackSearchResult;

public class SoundcloudEngineSearchResult implements SearchResult {

    private final SoundcloudTrackSearchResult sr;

    public SoundcloudEngineSearchResult(SoundcloudTrackSearchResult sr) {
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
        return sr.getSize();
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

    public String getStreamUrl() {
        return sr.getStreamUrl();
    }
}
