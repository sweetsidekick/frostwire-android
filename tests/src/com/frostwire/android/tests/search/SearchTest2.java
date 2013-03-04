package com.frostwire.android.tests.search;

import java.util.ArrayList;
import java.util.LinkedList;
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

            @Override
            public void stop() {
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

            @Override
            public void stop() {
            }
        });

        assertTrue("Did not finish or took too much time", manager.shutdown(5, TimeUnit.SECONDS));

        assertTrue(l.isFinished());
    }

    public void testStopWithFastPerformers() {
        runStopWithPerformers(10, 0);
    }

    public void testStopWithMediumFastPerformers() {
        runStopWithPerformers(10, 2000);
    }

    public void testStopWithSlowPerformers() {
        runStopWithPerformers(10, 20000);
    }

    public void runStopWithPerformers(int n, long timeMillis) {
        List<SearchPerformer> performers = createTimedPerformers(10, 2000);

        MockSearchResultListener l = new MockSearchResultListener();

        manager.registerListener(l);

        for (SearchPerformer performer : performers) {
            manager.perform(performer);
        }

        assertTrue("Did not finish or took too much time", manager.shutdown(5, TimeUnit.SECONDS));

        assertTrue(l.isFinished());
    }

    private List<SearchPerformer> createTimedPerformers(int n, final long timeMillis) {
        List<SearchPerformer> performers = new LinkedList<SearchPerformer>();

        for (int i = 0; i < n; i++) {
            performers.add(new SearchPerformer() {

                private Object sync = new Object();

                @Override
                public void registerListener(SearchResultListener listener) {
                }

                @Override
                public void perform() {
                    try {
                        sync.wait(timeMillis);
                    } catch (InterruptedException e) {
                        // ignore
                    }
                }

                @Override
                public void stop() {
                    synchronized (sync) {
                        sync.notifyAll();
                    }
                }
            });
        }

        return performers;
    }
}
