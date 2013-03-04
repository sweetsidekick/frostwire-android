package com.frostwire.search;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SearchManagerImpl implements SearchManager {

    private static final Logger LOG = LoggerFactory.getLogger(SearchManagerImpl.class);

    private SearchResultListener listener;

    @Override
    public void perform(SearchPerformer performer) {
        if (performer != null) {
            performer.registerListener(new PerformerResultListener(this));
        } else {
            LOG.warn("Search performer is null, review your logic");
        }
    }

    @Override
    public void registerListener(SearchResultListener listener) {
        this.listener = listener;
    }

    private static final class PerformerResultListener implements SearchResultListener {

        private final SearchManagerImpl manager;

        public PerformerResultListener(SearchManagerImpl manager) {
            this.manager = manager;
        }

        @Override
        public void onResults(SearchPerformer performer, List<?> results) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onFinished(SearchPerformer performer) {
            // TODO Auto-generated method stub

        }
    }
}
