package com.frostwire.android.gui.search;

public abstract class AbstractBittorrentIntentResult implements BittorrentSearchResult {

    @Override
    public String getFileName() {
        return null;
    }
    
    @Override
    public String getTorrentURI() {
        return null;
    }

    @Override
    public String getTitle() {
        return null;
    }

    @Override
    public long getSize() {
        return 0;
    }
    
    @Override
    public int getRank() {
        return 0;
    }

    @Override
    public long getCreationTime() {
        return 0;
    }

    @Override
    public String getHash() {
        return null;
    }

    @Override
    public int getSearchEngineId() {
        return 0;
    }

    @Override
    public String getDetailsUrl() {
        return null;
    }

    @Override
    public String getSource() {
        return null;
    }
}