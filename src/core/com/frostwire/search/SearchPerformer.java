package com.frostwire.search;

public interface SearchPerformer {

    public void registerListener(SearchResultListener listener);

    public void perform();

    public void stop();
}
