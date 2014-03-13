package com.frostwire.search.appia;

import com.frostwire.search.AbstractFileSearchResult;
import com.frostwire.search.torrent.ComparableTorrentJsonItem;

public class AppiaSearchResult extends AbstractFileSearchResult implements ComparableTorrentJsonItem {

    public String clickProxyUrl;
    public String impressionTrackingURL;
    public String productName;
    public String productDescription;
    public String productThumbnail;
    
    
    public AppiaSearchResult() {
        
    }
    
    @Override
    public int getSeeds() {
        return 5000;
    }

    @Override
    public String getFilename() {
        return null;
    }

    @Override
    public long getSize() {
        return -1;
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public String getDetailsUrl() {
        return null;
    }

    @Override
    public String getSource() {
        return "Appia";
    }
}