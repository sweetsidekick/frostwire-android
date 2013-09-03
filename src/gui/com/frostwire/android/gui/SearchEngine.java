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

package com.frostwire.android.gui;

import java.util.Arrays;
import java.util.List;

import com.frostwire.android.core.ConfigurationManager;
import com.frostwire.android.core.Constants;
import com.frostwire.android.gui.util.OSUtils;
import com.frostwire.search.SearchPerformer;
import com.frostwire.search.UserAgent;
import com.frostwire.search.archiveorg.ArchiveorgSearchPerformer;
import com.frostwire.search.clearbits.ClearBitsSearchPerformer;
import com.frostwire.search.extratorrent.ExtratorrentSearchPerformer;
import com.frostwire.search.frostclick.FrostClickSearchPerformer;
import com.frostwire.search.isohunt.ISOHuntSearchPerformer;
import com.frostwire.search.mininova.MininovaSearchPerformer;
import com.frostwire.search.soundcloud.SoundcloudSearchPerformer;
import com.frostwire.search.vertor.VertorSearchPerformer;
import com.frostwire.search.youtube.YouTubeSearchPerformer;

/**
 * 
 * @author gubatron
 * @author aldenml
 *
 */
public abstract class SearchEngine {

    private static final int DEFAULT_TIMEOUT = 5000;

    private final String name;
    private final String preferenceKey;

    private boolean active;

    private SearchEngine(String name, String preferenceKey) {
        this.name = name;
        this.preferenceKey = preferenceKey;

        this.active = true;
    }

    public String getName() {
        return name;
    }

    public abstract SearchPerformer getPerformer(long token, String keywords);

    public String getPreferenceKey() {
        return preferenceKey;
    }

    public boolean isEnabled() {
        return isActive() && ConfigurationManager.instance().getBoolean(preferenceKey);
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return name;
    }

    public static List<SearchEngine> getEngines() {
        return ALL_ENGINES;
    }

    public static SearchEngine forName(String name) {
        for (SearchEngine engine : getEngines()) {
            if (engine.getName().equals(name)) {
                return engine;
            }
        }

        return null;
    }

    public static final SearchEngine CLEARBITS = new SearchEngine("ClearBits", Constants.PREF_KEY_SEARCH_USE_CLEARBITS) {
        @Override
        public SearchPerformer getPerformer(long token, String keywords) {
            return new ClearBitsSearchPerformer(token, keywords, DEFAULT_TIMEOUT);
        }
    };

    public static final SearchEngine EXTRATORRENT = new SearchEngine("Extratorrent", Constants.PREF_KEY_SEARCH_USE_EXTRATORRENT) {
        @Override
        public SearchPerformer getPerformer(long token, String keywords) {
            return new ExtratorrentSearchPerformer(token, keywords, DEFAULT_TIMEOUT);
        }
    };

    public static final SearchEngine ISOHUNT = new SearchEngine("ISOHunt", Constants.PREF_KEY_SEARCH_USE_ISOHUNT) {
        @Override
        public SearchPerformer getPerformer(long token, String keywords) {
            return new ISOHuntSearchPerformer(token, keywords, DEFAULT_TIMEOUT);
        }
    };

    public static final SearchEngine MININOVA = new SearchEngine("Mininova", Constants.PREF_KEY_SEARCH_USE_MININOVA) {
        @Override
        public SearchPerformer getPerformer(long token, String keywords) {
            return new MininovaSearchPerformer(token, keywords, DEFAULT_TIMEOUT);
        }
    };

    public static final SearchEngine VERTOR = new SearchEngine("Vertor", Constants.PREF_KEY_SEARCH_USE_VERTOR) {
        @Override
        public SearchPerformer getPerformer(long token, String keywords) {
            return new VertorSearchPerformer(token, keywords, DEFAULT_TIMEOUT);
        }
    };

    public static final SearchEngine YOUTUBE = new SearchEngine("YouTube", Constants.PREF_KEY_SEARCH_USE_YOUTUBE) {
        @Override
        public SearchPerformer getPerformer(long token, String keywords) {
            return new YouTubeSearchPerformer(token, keywords, DEFAULT_TIMEOUT);
        }
    };

    public static final SearchEngine SOUNCLOUD = new SearchEngine("Soundcloud", Constants.PREF_KEY_SEARCH_USE_SOUNDCLOUD) {
        @Override
        public SearchPerformer getPerformer(long token, String keywords) {
            return new SoundcloudSearchPerformer(token, keywords, DEFAULT_TIMEOUT);
        }
    };

    public static final SearchEngine ARCHIVE = new SearchEngine("Archive.org", Constants.PREF_KEY_SEARCH_USE_ARCHIVEORG) {
        @Override
        public SearchPerformer getPerformer(long token, String keywords) {
            return new ArchiveorgSearchPerformer(token, keywords, DEFAULT_TIMEOUT);
        }
    };
    
    private static final UserAgent FROSTCLICK_ANDROID_USER_AGENT = new UserAgent(OSUtils.getOSVersionString(), Constants.FROSTWIRE_VERSION_STRING, Constants.FROSTWIRE_BUILD);
    
    public static final SearchEngine FROSTCLICK = new SearchEngine("FrostClick", Constants.PREF_KEY_SEARCH_USE_FROSTCLICK) {
        @Override
        public SearchPerformer getPerformer(long token, String keywords) {
            return new FrostClickSearchPerformer(token, keywords, DEFAULT_TIMEOUT, FROSTCLICK_ANDROID_USER_AGENT);
        }
    };
    

    private static final List<SearchEngine> ALL_ENGINES = Arrays.asList(FROSTCLICK, CLEARBITS, MININOVA, ISOHUNT, EXTRATORRENT, VERTOR, YOUTUBE, SOUNCLOUD, ARCHIVE);
}