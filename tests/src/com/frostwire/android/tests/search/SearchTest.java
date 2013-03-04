package com.frostwire.android.tests.search;

import com.frostwire.search.SearchManager;
import com.frostwire.search.SearchManagerImpl;

import junit.framework.TestCase;

public class SearchTest extends TestCase {

    private SearchManager manager;

    @Override
    protected void setUp() throws Exception {
        manager = new SearchManagerImpl();
    }

    public void testCallWithPerfomerNull() {
        manager.perform(null);
    }
}
