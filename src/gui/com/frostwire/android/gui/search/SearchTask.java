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

import android.util.Log;

import com.frostwire.android.util.concurrent.AbstractRunnable;

/**
 * @author gubatron
 * @author aldenml
 * 
 */
abstract class SearchTask extends AbstractRunnable {

    private static final String TAG = "FW.SearchTask";

    private boolean isCancelled;
    private SearchTaskListener listener;

    public SearchTask(String name) {
        super(name);
    }

    public boolean isCancelled() {
        return isCancelled;
    }

    public void cancel() {
        isCancelled = true;
    }

    public SearchTaskListener getListener() {
        return listener;
    }

    public void setListener(SearchTaskListener listener) {
        this.listener = listener;
    }

    @Override
    public final void run() {
        try {
            runTask();
        } catch (Throwable e) {
            Log.e(TAG, "Task " + getName() + " execution failed", e);
        } finally {
            finish();
        }
    }

    public abstract void runTask();

    private void finish() {
        if (listener != null) {
            try {
                listener.onFinish(this);
            } catch (Throwable e) {
                Log.e(TAG, "Error calling listener", e);
            }
        }
    }

    public static interface SearchTaskListener {

        /**
         * Method called when the task is finished
         */
        public void onFinish(SearchTask task);
    }
}
