package com.frostwire.search;

import java.util.concurrent.TimeUnit;

public interface SearchManager {

    public void registerListener(SearchResultListener listener);

    public void perform(SearchPerformer performer);

    public boolean shutdown(long timeout, TimeUnit unit);
}
