package com.frostwire.search;

import java.util.List;

public interface SearchResultListener {

    public void onResults(SearchPerformer performer, List<?> results);

    public void onFinished(SearchPerformer performer);
}
