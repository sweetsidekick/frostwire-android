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
public abstract class CrawlPagedWebSearchPerformer<T extends CrawlableSearchResult> extends PagedWebSearchPerformer {

    private static final Logger LOG = LoggerFactory.getLogger(CrawlPagedWebSearchPerformer.class);

    private static final int DEFAULT_NUM_CRAWLS = 4;

    private static final CrawlCache cache = new DiskCrawlCache();

    private int numCrawls;

    public CrawlPagedWebSearchPerformer(long token, String keywords, int timeout, int pages, int numCrawls) {
        super(token, keywords, timeout, pages);
        this.numCrawls = numCrawls;
    }

    public CrawlPagedWebSearchPerformer(long token, String keywords, int timeout, int pages) {
        this(token, keywords, timeout, pages, DEFAULT_NUM_CRAWLS);
    }

    @Override
    public void crawl(CrawlableSearchResult sr) {
        if (numCrawls > 0) {
            numCrawls--;

            T obj = cast(sr);
            if (obj != null) {
                onResults(this, crawlResult(obj));
            }
        }
    }

    protected List<? extends SearchResult> crawlResult(T sr) {
        return Collections.emptyList();
    }

    protected byte[] cacheGet(String key) {
        synchronized (cache) {
            return cache.get(key);
        }
    }

    protected void cachePut(String key, byte[] data) {
        synchronized (cache) {
            cache.put(key, data);
        }
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
