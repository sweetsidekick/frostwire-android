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

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

/**
 * 
 * 
 * @author gubatron
 * @author aldenml
 * 
 */
public abstract class AbstractActivity extends Activity {

    private final int layoutResId;

    public AbstractActivity(int layoutResId) {
        if (layoutResId == 0) {
            throw new RuntimeException("Resource id can't be 0");
        }

        this.layoutResId = layoutResId;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(layoutResId);
        initComponents(savedInstanceState);
    }

    protected abstract void initComponents(Bundle savedInstanceState);

    @SuppressWarnings("unchecked")
    protected final <T extends View> T findView(int id) {
        return (T) super.findViewById(id);
    }
}
