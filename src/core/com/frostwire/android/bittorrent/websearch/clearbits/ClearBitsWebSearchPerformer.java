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

package com.frostwire.android.bittorrent.websearch.clearbits;

import java.util.ArrayList;
import java.util.List;

import com.frostwire.android.util.StringUtils;
import com.frostwire.util.JsonUtils;
import com.frostwire.websearch.JsonSearchPerformer;
import com.frostwire.websearch.TorrentWebSearchResult;
import com.frostwire.websearch.WebSearchResult;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public class ClearBitsWebSearchPerformer extends JsonSearchPerformer {

    public List<WebSearchResult> search(String keywords) {
        List<WebSearchResult> result = new ArrayList<WebSearchResult>();

        ClearBitsResponse response = searchClearBits(keywords);

        if (response != null && response.results != null)
            for (ClearBitsItem bucket : response.results) {

                TorrentWebSearchResult sr = new ClearBitsWebSearchResult(bucket);

                result.add(sr);
            }

        return result;
    }

    private ClearBitsResponse searchClearBits(String keywords) {
        String json = fetchJson("http://www.clearbits.net/home/search/index.json?query=" + StringUtils.encodeUrl(keywords));

        if (json == null) {
            return null;
        }

        ClearBitsResponse response = null;

        try {
            response = JsonUtils.toObject(json, ClearBitsResponse.class);
        } catch (Throwable e) {
            return null;
        }

        response.fixItems();

        return response;
    }
}
