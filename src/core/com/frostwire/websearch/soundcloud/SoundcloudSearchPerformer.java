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

package com.frostwire.websearch.soundcloud;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.frostwire.android.util.UrlUtils;
import com.frostwire.util.JsonUtils;
import com.frostwire.websearch.HttpClient;
import com.frostwire.websearch.WebSearchPerformer;
import com.frostwire.websearch.WebSearchResult;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public class SoundcloudSearchPerformer implements WebSearchPerformer {

    private static final int SOUNDCLOUD_MAX_RESULTS = 8;

    private SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMMM, dd yyyy HH:mm:ss Z");

    public List<WebSearchResult> search(String keywords) {
        List<WebSearchResult> result = new ArrayList<WebSearchResult>();

        keywords = UrlUtils.encode(keywords);

        int pages = SOUNDCLOUD_MAX_RESULTS / 10;

        for (int i = 0; i <= pages; i++) {
            result.addAll(searchPage(i + 1, keywords));
        }

        return result;
    }

    private List<WebSearchResult> searchPage(int page, String keywords) {
        List<WebSearchResult> result = new ArrayList<WebSearchResult>();

        HttpClient c = new HttpClient(getUrl(page, keywords));

        String html = c.get();

        String regex = getRegex();

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(html);

        int max = SOUNDCLOUD_MAX_RESULTS;

        int i = 0;

        while (matcher.find() && i < max) {
            try {
                SoundcloudItem item = JsonUtils.toObject(matcher.group(2), SoundcloudItem.class);
                try {
                    item.date = DATE_FORMAT.parse(matcher.group(1)).getTime();
                } catch (Throwable e) {
                    item.date = -1;
                }
                WebSearchResult sr = new SoundcloudTrackSearchResult(item);
                if (sr != null) {
                    result.add(sr);
                    i++;
                }
            } catch (Exception e) {
                // do nothing
            }
        }

        return result;
    }

    public String getUrl(int page, String encodedKeywords) {
        return "http://soundcloud.com/tracks/search?page=" + page + "&q[fulltext]=" + encodedKeywords;
    }

    public String getRegex() {
        return "(?is)<abbr title='(.*?)'.*?window.SC.bufferTracks.push\\((.*?)\\);";
    }
}
