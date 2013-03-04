package com.frostwire.search;

public interface SearchManager {

    public void perform(SearchPerformer performer);

    public void registerListener(SearchResultListener listener);
}
