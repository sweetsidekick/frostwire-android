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

import java.util.List;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public abstract class PagedWebSearchPerformer extends WebSearchPerformer {

    private final int pages;

    public PagedWebSearchPerformer(String keywords, int timeout, int pages) {
        super(keywords, timeout);
        this.pages = pages;
    }

    @Override
    public void perform() {
        for (int i = 1; !isStopped() && i <= pages; i++) {
            onResults(this, searchPage(i));
        }

        onFinished(this);
    }

    protected List<? extends SearchResult<?>> searchPage(int page) {
        String url = getUrl(page, encodeKeywords());
        String text = fetch(url);
        return searchPage(text);
    }

    protected abstract String getUrl(int page, String encodedKeywords);

    protected abstract List<? extends SearchResult<?>> searchPage(String page);
}
