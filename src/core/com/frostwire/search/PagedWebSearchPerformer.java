/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011, 2012, FrostWire(TM). All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.frostwire.search;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public abstract class PagedWebSearchPerformer<T extends CrawlableSearchResult> extends WebSearchPerformer {

    private static final Logger LOG = LoggerFactory.getLogger(PagedWebSearchPerformer.class);

    private static final int DEFAULT_NUM_CRAWLS = 4;

    private final int pages;

    private int numCrawls;

    public PagedWebSearchPerformer(long token, String keywords, int timeout, int pages, int numCrawls) {
        super(token, keywords, timeout);
        this.pages = pages;

        this.numCrawls = numCrawls;
    }

    public PagedWebSearchPerformer(long token, String keywords, int timeout, int pages) {
        this(token, keywords, timeout, pages, DEFAULT_NUM_CRAWLS);
    }

    @Override
    public void perform() {
        for (int i = 1; !isStopped() && i <= pages; i++) {
            onResults(this, searchPage(i));
        }
    }

    @Override
    public void crawl(CrawlableSearchResult sr) {
        if (numCrawls > 0) {
            numCrawls--;

            T obj = cast(sr);
            if (obj != null) {
                crawlSearchResult(obj);
            }
        }
    }

    protected List<? extends SearchResult> searchPage(int page) {
        String url = getUrl(page, encodeKeywords());
        String text = fetch(url);
        if (text != null) {
            return searchPage(text);
        } else {
            LOG.warn("Page content empty for url: " + url);
            return Collections.emptyList();
        }
    }

    protected abstract String getUrl(int page, String encodedKeywords);

    protected abstract List<? extends SearchResult> searchPage(String page);

    protected void crawlSearchResult(T sr) {
    }

    @SuppressWarnings("unchecked")
    private T cast(CrawlableSearchResult sr) {
        try {
            return (T) sr;
        } catch (ClassCastException e) {
            LOG.warn("Something wrong with the logic, need to pass a crawlable search result with the correct type");
        }

        return null;
    }
}
