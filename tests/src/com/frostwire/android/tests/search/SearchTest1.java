package com.frostwire.android.tests.search;

import junit.framework.TestCase;

import com.frostwire.search.SearchManagerImpl;

public class SearchTest1 extends TestCase {

    private SearchManagerImpl manager;

    @Override
    protected void setUp() throws Exception {
        manager = new SearchManagerImpl();
    }

    public void testCallWithPerfomerNull() {
        manager.perform(null);
    }
}
