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

package com.frostwire.android.gui.search;

import java.util.List;

import android.util.Log;

/**
 * @author gubatron
 * @author aldenml
 * 
 */
class LocalSearchTask extends SearchTask {

    private static final String TAG = "FW.LocalSearchTask";

    private final String query;

    public LocalSearchTask(String query) {
        super("LocalSearchTask: " + query);
        this.query = query;
    }

    public void run() {
        if (isCancelled()) {
            return;
        }

        try {
            List<BittorrentSearchResult> results = LocalSearchEngine.instance().search(query);

            if (!isCancelled()) {
                LocalSearchEngine.instance().addResults(results);
            }
        } catch (Throwable e) {
            Log.e(TAG, "Error getting data from local search manager", e);
        }
    }
}
