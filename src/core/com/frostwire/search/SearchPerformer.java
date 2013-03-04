package com.frostwire.search;

public interface SearchPerformer {

    public void perform();

    public void registerListener(SearchResultListener listener);
}
