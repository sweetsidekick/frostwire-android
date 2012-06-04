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

package com.frostwire.android.bittorrent.websearch.extratorrent;

import java.util.ArrayList;
import java.util.List;

import com.frostwire.android.bittorrent.websearch.JsonSearchPerformer;
import com.frostwire.android.bittorrent.websearch.WebSearchResult;
import com.frostwire.android.util.JsonUtils;
import com.frostwire.android.util.StringUtils;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public class ExtratorrentWebSearchPerformer extends JsonSearchPerformer {

    @Override
    public List<WebSearchResult> search(String keywords) {
        List<WebSearchResult> result = new ArrayList<WebSearchResult>();

        ExtratorrentResponse response = searchExtratorrent(keywords);

        if (response != null && response.list != null)
            for (ExtratorrentItem item : response.list) {

                WebSearchResult sr = new ExtratorrentResponseWebSearchResult(item);

                result.add(sr);
            }

        return result;
    }

    private ExtratorrentResponse searchExtratorrent(String keywords) {
        String json = fetchJson("http://extratorrent.com/json/?search=" + StringUtils.encodeUrl(keywords));

        return json != null ? JsonUtils.toObject(json, ExtratorrentResponse.class) : null;
    }
}
