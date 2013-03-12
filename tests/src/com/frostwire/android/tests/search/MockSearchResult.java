package com.frostwire.android.tests.search;

import com.frostwire.search.CompleteSearchResult;
import com.frostwire.search.SearchResultLicence;

public class MockSearchResult implements CompleteSearchResult {

    @Override
    public String getSource() {
        return "Tests";
    }

    @Override
    public String getDetailsUrl() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return null;
    }
    
    @Override
    public SearchResultLicence getLicence() {
        return SearchResultLicence.UNKNOWN;
    }
}
