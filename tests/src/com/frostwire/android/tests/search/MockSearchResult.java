package com.frostwire.android.tests.search;

import com.frostwire.search.CompleteSearchResult;

public class MockSearchResult implements CompleteSearchResult{

    @Override
    public String getSource() {
        return "Tests";
    }
}
