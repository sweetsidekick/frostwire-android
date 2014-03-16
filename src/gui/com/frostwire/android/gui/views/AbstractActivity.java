/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2014, FrostWire(R). All rights reserved.
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

package com.frostwire.android.gui.views;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.ViewGroup;

import com.frostwire.logging.Logger;

/**
 * 
 * 
 * @author gubatron
 * @author aldenml
 * 
 */
public abstract class AbstractActivity extends FragmentActivity {

    private static final Logger LOG = Logger.getLogger(AbstractActivity.class);

    private final int layoutResId;

    private final long refreshInterval;
    private final List<Refreshable> refreshables;

    private Handler refreshHandler;
    private Runnable refreshTask;

    public AbstractActivity(int layoutResId, int refreshIntervalSec) {
        if (layoutResId == 0) {
            throw new RuntimeException("Resource id can't be 0");
        }
        
        this.layoutResId = layoutResId;

        this.refreshInterval = refreshIntervalSec * 1000;
        this.refreshables = new ArrayList<Refreshable>();

        if (refreshInterval > 0) {
            refreshHandler = new Handler();
            refreshTask = new Runnable() {
                public void run() {
                    onRefresh();
                    refreshHandler.postDelayed(refreshTask, refreshInterval);
                }
            };
        }
    }

    public AbstractActivity(int layoutResID) {
        this(layoutResID, 0);
    }

    public void addRefreshable(Refreshable refreshable) {
        if (!refreshables.contains(refreshable)) {
            refreshables.add(refreshable);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);

        if (layoutResId != 0) {
            setContentView(layoutResId);
            initComponents();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (refreshHandler != null) {
            refreshHandler.postDelayed(refreshTask, refreshInterval);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (refreshHandler != null) {
            refreshHandler.removeCallbacks(refreshTask);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // read link: http://code.google.com/p/android/issues/detail?id=8488#c109
        unbindDrawables(findViewById(android.R.id.content));
    }

    protected void onRefresh() {
        for (Refreshable refreshable : refreshables) {
            try {
                refreshable.refresh();
            } catch (Throwable e) {
                LOG.error("Error refreshing component", e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected final <T extends View> T findView(int id) {
        return (T) super.findViewById(id);
    }

    protected void initComponents() {
    }

    private void unbindDrawables(View view) {
        try {
            if (view.getBackground() != null) {
                view.getBackground().setCallback(null);
            }
            if (view instanceof ViewGroup) {
                for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                    unbindDrawables(((ViewGroup) view).getChildAt(i));
                }
                //((ViewGroup) view).removeAllViews();
            }
        } catch (Throwable e) {
            LOG.warn("Failed to unbind drawables and remove views", e);
        }
    }
}
