/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011, 2012, FrostWire(R). All rights reserved.
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

package com.frostwire.websearch.youtube;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import com.frostwire.websearch.WebSearchResult;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public class YouTubeSearchResult implements WebSearchResult {

    //2010-07-15T16:02:42
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    private final YouTubeEntry entry;

    private final long creationTime;
    private final String videoUrl;

    public YouTubeSearchResult(YouTubeEntry entry) {
        this.entry = entry;

        this.creationTime = readCreationTime(entry);
        this.videoUrl = readVideoUrl(entry);
    }

    public YouTubeEntry getYouTubeEntry() {
        return entry;
    }

    @Override
    public String getDisplayName() {
        return entry.title.title;
    }

    @Override
    public String getFileName() {
        return getDisplayName() + ".mp4";
    }

    @Override
    public long getCreationTime() {
        return creationTime;
    }

    @Override
    public String getSource() {
        if (entry.author != null && entry.author.size() > 0 && entry.author.get(0).name != null && entry.author.get(0).name.title != null) {
            return "YouTube - " + entry.author.get(0).name.title;
        } else {
            return "YouTube";
        }
    }

    @Override
    public int getRank() {
        return 20000;
    }

    @Override
    public String getDetailsUrl() {
        return videoUrl;
    }

    private long readCreationTime(YouTubeEntry entry) {
        try {
            return DATE_FORMAT.parse(entry.published.title.replace("000Z", "")).getTime();
        } catch (ParseException e) {
            return System.currentTimeMillis();
        }
    }

    private String readVideoUrl(YouTubeEntry entry) {
        String url = null;

        for (YouTubeEntryLink link : entry.link) {
            if (link.rel.equals("alternate")) {
                url = link.href;
            }
        }

        url = url.replace("https://", "http://").replace("&feature=youtube_gdata", "");

        return url;
    }
}
