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

package com.frostwire.search.archive;

import java.util.LinkedList;
import java.util.List;

import com.frostwire.search.PagedWebSearchPerformer;
import com.frostwire.search.SearchResult;
import com.frostwire.util.JsonUtils;
import com.frostwire.websearch.WebSearchResult;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public class ArchiveSearchPerformer extends PagedWebSearchPerformer {

    public ArchiveSearchPerformer(String keywords, int timeout) {
        super(keywords, timeout, 1);
    }

    @Override
    protected String getUrl(int page, String encodedKeywords) {
        return "http://archive.org/advancedsearch.php?q="
                + encodedKeywords
                + "&fl[]=avg_rating&fl[]=call_number&fl[]=collection&fl[]=contributor&fl[]=coverage&fl[]=creator&fl[]=date&fl[]=description&fl[]=downloads&fl[]=foldoutcount&fl[]=format&fl[]=headerImage&fl[]=identifier&fl[]=imagecount&fl[]=language&fl[]=licenseurl&fl[]=mediatype&fl[]=month&fl[]=num_reviews&fl[]=oai_updatedate&fl[]=publicdate&fl[]=publisher&fl[]=rights&fl[]=scanningcentre&fl[]=source&fl[]=subject&fl[]=title&fl[]=type&fl[]=volume&fl[]=week&fl[]=year&sort[]=avg_rating+desc&sort[]=downloads+desc&sort[]=createdate+desc&rows=50&page=1&indent=yes&output=json";
    }

    @Override
    protected List<? extends SearchResult<?>> searchPage(String page) {
        List<SearchResult<WebSearchResult>> result = new LinkedList<SearchResult<WebSearchResult>>();

        ArchiveResponse response = JsonUtils.toObject(page, ArchiveResponse.class);

        for (ArchiveItem item : response.response.docs) {
            if (!isStopped()) {
                ArchiveSearchResult sr = new ArchiveSearchResult(item);
                result.add(new SearchResult<WebSearchResult>(sr));
            }
        }

        return result;
    }
}
