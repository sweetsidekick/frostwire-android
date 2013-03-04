package com.frostwire.search;

import java.util.List;

public interface SearchResultListener {

    public void onResults(SearchPerformer performer, List<? extends SearchResult<?>> results);

    public void onFinished(SearchPerformer performer);
}
