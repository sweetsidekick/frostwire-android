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

package com.frostwire.android.core;

import java.util.Arrays;
import java.util.List;

import com.frostwire.search.SearchPerformer;
import com.frostwire.search.clearbits.ClearBitsSearchPerformer;
import com.frostwire.search.extratorrent.ExtratorrentSearchPerformer;
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
public abstract class SearchEngine2 {

    private static final int DEFAULT_TIMEOUT = 5000;

    private final String name;
    private final String preferenceKey;

    private boolean active;

    private SearchEngine2(String name, String preferenceKey) {
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

    public static List<SearchEngine2> getEngines() {
        return Arrays.asList(CLEARBITS, MININOVA, ISOHUNT, EXTRATORRENT, VERTOR/*, TPB*//*,KAT*/, YOUTUBE, SOUNCLOUD);
    }

    public static final SearchEngine2 CLEARBITS = new SearchEngine2("ClearBits", Constants.PREF_KEY_SEARCH_USE_CLEARBITS) {
        @Override
        public SearchPerformer getPerformer(long token, String keywords) {
            return new ClearBitsSearchPerformer(token, keywords, DEFAULT_TIMEOUT);
        }
    };

    public static final SearchEngine2 EXTRATORRENT = new SearchEngine2("Extratorrent", Constants.PREF_KEY_SEARCH_USE_EXTRATORRENT) {
        @Override
        public SearchPerformer getPerformer(long token, String keywords) {
            return new ExtratorrentSearchPerformer(token, keywords, DEFAULT_TIMEOUT);
        }
    };

    public static final SearchEngine2 ISOHUNT = new SearchEngine2("ISOHunt", Constants.PREF_KEY_SEARCH_USE_ISOHUNT) {
        @Override
        public SearchPerformer getPerformer(long token, String keywords) {
            return new ISOHuntSearchPerformer(token, keywords, DEFAULT_TIMEOUT);
        }
    };

    public static final SearchEngine2 MININOVA = new SearchEngine2("Mininova", Constants.PREF_KEY_SEARCH_USE_MININOVA) {
        @Override
        public SearchPerformer getPerformer(long token, String keywords) {
            return new MininovaSearchPerformer(token, keywords, DEFAULT_TIMEOUT);
        };
    };

    public static final SearchEngine2 VERTOR = new SearchEngine2("Vertor", Constants.PREF_KEY_SEARCH_USE_VERTOR) {
        @Override
        public SearchPerformer getPerformer(long token, String keywords) {
            return new VertorSearchPerformer(token, keywords, DEFAULT_TIMEOUT);
        };
    };

    public static final SearchEngine2 YOUTUBE = new SearchEngine2("YouTube", Constants.PREF_KEY_SEARCH_USE_YOUTUBE) {
        @Override
        public SearchPerformer getPerformer(long token, String keywords) {
            return new YouTubeSearchPerformer(token, keywords, DEFAULT_TIMEOUT);
        };
    };

    public static final SearchEngine2 SOUNCLOUD = new SearchEngine2("Soundcloud", Constants.PREF_KEY_SEARCH_USE_SOUNDCLOUD) {
        @Override
        public SearchPerformer getPerformer(long token, String keywords) {
            return new SoundcloudSearchPerformer(token, keywords, DEFAULT_TIMEOUT);
        };
    };
}
