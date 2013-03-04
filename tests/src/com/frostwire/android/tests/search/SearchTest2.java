package com.frostwire.android.tests.search;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

import com.frostwire.search.SearchManagerImpl;
import com.frostwire.search.SearchPerformer;
import com.frostwire.search.SearchResult;
import com.frostwire.search.SearchResultListener;

public class SearchTest2 extends TestCase {

    private SearchManagerImpl manager;
    private List<SearchResult<Integer>> results;

    @Override
    protected void setUp() throws Exception {
        manager = new SearchManagerImpl();

        results = new ArrayList<SearchResult<Integer>>();
        results.add(new SearchResult<Integer>(1));
        results.add(new SearchResult<Integer>(2));
        results.add(new SearchResult<Integer>(3));
        results.add(new SearchResult<Integer>(4));
        results.add(new SearchResult<Integer>(5));
    }

    public void testFixedNumberResults() {
        MockSearchResultListener l = new MockSearchResultListener();

        manager.registerListener(l);
        manager.perform(new SearchPerformer() {

            private SearchResultListener listener;

            @Override
            public void registerListener(SearchResultListener listener) {
                this.listener = listener;
            }

            @Override
            public void perform() {
                listener.onResults(this, results);
            }
        });

        assertTrue("Did not finish or took too much time", manager.shutdown(5, TimeUnit.SECONDS));

        assertEquals(results.size(), l.getNumResults());
    }

    public void testPerformFinished() {
        MockSearchResultListener l = new MockSearchResultListener();

        manager.registerListener(l);
        manager.perform(new SearchPerformer() {

            @Override
            public void registerListener(SearchResultListener listener) {
            }

            @Override
            public void perform() {
                // not calling finished here
            }
        });

        assertTrue("Did not finish or took too much time", manager.shutdown(5, TimeUnit.SECONDS));

        assertTrue(l.isFinished());
    }
}
