package com.frostwire.search.appia;

import com.frostwire.search.AbstractFileSearchResult;
import com.frostwire.search.torrent.ComparableTorrentJsonItem;

public class AppiaSearchResult extends AbstractFileSearchResult implements ComparableTorrentJsonItem {

    public String clickProxyURL;
    public String impressionTrackingURL;
    public String displayName;
    public String description;
    public String thumbnailURL;
    public String appId;
    public String categoryName;
    
    public AppiaSearchResult(AppiaServletResponseItem item) {
        clickProxyURL = item.clickProxyURL;
        impressionTrackingURL = item.impressionTrackingURL;
        displayName = item.displayName;
        description = item.description;
        thumbnailURL = item.thumbnailURL;
        appId = item.appId;
        categoryName = item.categoryName;
    }
    
    public String getImpressionTrackingURL() {
        return impressionTrackingURL;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getThumbnailURL() {
        return thumbnailURL;
    }
    
    public String getAppId() {
        return appId;
    }
    
    public String getCategoryName() {
        return categoryName;
    }
    
    @Override
    public int getSeeds() {
        return 5000;
    }

    @Override
    public String getFilename() {
        return displayName;
    }

    @Override
    public long getSize() {
        return -1;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String getDetailsUrl() {
        return clickProxyURL;
    }

    @Override
    public String getSource() {
        return "Appia";
    }
}