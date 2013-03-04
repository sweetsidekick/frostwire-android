package com.frostwire.android.tests.search;

import java.util.List;

import com.frostwire.search.SearchPerformer;
import com.frostwire.search.SearchResult;
import com.frostwire.search.SearchResultListener;

public class MockSearchResultListener implements SearchResultListener {

    private int numResults;
    private boolean finished;

    public int getNumResults() {
        return numResults;
    }

    public boolean isFinished() {
        return finished;
    }

    @Override
    public void onResults(SearchPerformer performer, List<? extends SearchResult<?>> results) {
        this.numResults += results.size();
    }

    @Override
    public void onFinished(SearchPerformer performer) {
        this.finished = true;
    }
}
