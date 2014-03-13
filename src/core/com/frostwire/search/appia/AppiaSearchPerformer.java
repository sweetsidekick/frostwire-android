/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2014,, FrostWire(R). All rights reserved.
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

package com.frostwire.search.appia;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.frostwire.android.gui.MainApplication;
import com.frostwire.android.gui.activities.MainActivity;
import com.frostwire.logging.Logger;
import com.frostwire.search.PagedWebSearchPerformer;
import com.frostwire.search.SearchResult;
import com.frostwire.search.domainalias.DomainAliasManager;
import com.frostwire.search.frostclick.UserAgent;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public class AppiaSearchPerformer extends PagedWebSearchPerformer {

    private static final Logger LOG = Logger.getLogger(AppiaSearchPerformer.class);

    private static final int MAX_RESULTS = 1;

    private final Map<String, String> customHeaders;
    

    public AppiaSearchPerformer(DomainAliasManager domainAliasManager, long token, String keywords, int timeout, UserAgent userAgent, String androidId) {
        super(domainAliasManager ,token, keywords, timeout, MAX_RESULTS);
        this.customHeaders = buildCustomHeaders(userAgent, androidId);
    }

    @Override
    protected String getUrl(int page, String encodedKeywords) {
        return "http://appia.frostclick.com/q?page=" + page + "&q=" + encodedKeywords;
    }

    @Override
    protected List<? extends SearchResult> searchPage(int page) {
        String url = getUrl(page, getEncodedKeywords());
        String text = null;
        List<? extends SearchResult> result = Collections.emptyList();
        try {
            text = fetch(url, null, customHeaders);
        } catch (IOException e) {
            checkAccesibleDomains();
            return Collections.emptyList();
        }
        
        if (text != null) {
            result = searchPage(text);
        } else {
            LOG.warn("Page content empty for url: " + url);
        }
        
        return result;
    }

    @Override
    protected List<? extends SearchResult> searchPage(String page) {
        // unused for this implementation since we still don't have search responses ready.
        return Collections.emptyList();
    }

    private Map<String, String> buildCustomHeaders(UserAgent userAgent, String androidId) {
        Map<String, String> map = new HashMap<String, String>();
        map.putAll(userAgent.getHeadersMap());
        map.put("User-Agent", userAgent.toString());
        map.put("sessionId", userAgent.getUUID());
        map.put("androidId", androidId);
        return map;
    }
}