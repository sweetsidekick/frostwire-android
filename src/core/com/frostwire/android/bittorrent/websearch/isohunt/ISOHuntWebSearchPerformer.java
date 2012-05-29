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

package com.frostwire.android.bittorrent.websearch.isohunt;

import java.util.ArrayList;
import java.util.List;

import com.frostwire.android.bittorrent.websearch.WebSearchPerformer;
import com.frostwire.android.bittorrent.websearch.WebSearchResult;
import com.frostwire.android.core.HttpFetcher;
import com.frostwire.android.util.JsonUtils;
import com.frostwire.android.util.StringUtils;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public class ISOHuntWebSearchPerformer implements WebSearchPerformer {

    public List<WebSearchResult> search(String keywords) {
        List<WebSearchResult> result = new ArrayList<WebSearchResult>();

        ISOHuntResponse response = searchISOHunt(keywords);

        if (response != null && response.items != null && response.items.list != null) {
            for (ISOHuntItem item : response.items.list) {
                WebSearchResult sr = new ISOHuntWebSearchResult(item);
                result.add(sr);
            }
        }

        return result;
    }

    public static ISOHuntResponse searchISOHunt(String keywords) {
        HttpFetcher fetcher = new HttpFetcher("http://isohunt.com/js/json.php?ihq=" + StringUtils.encodeUrl(keywords) + "&start=1&rows=100&sort=seeds", HTTP_TIMEOUT);

        byte[] bytes = fetcher.fetch();

        return bytes != null ? JsonUtils.toObject(StringUtils.getUTF8String(bytes), ISOHuntResponse.class) : null;
    }
}
