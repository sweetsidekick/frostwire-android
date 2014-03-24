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

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public abstract class AbstractExpandableListFragment extends ExpandableListFragment {

    private final int resource;

    public AbstractExpandableListFragment(int resource) {
        this.resource = resource;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(resource, container, false);
        initComponents(v);
        return v;
    }

    protected void initComponents(View v) {
    }

    protected void onRefresh() {
    }

    @SuppressWarnings("unchecked")
    protected final <T extends View> T findView(View v, int id) {
        return (T) v.findViewById(id);
    }

    @SuppressWarnings("unchecked")
    protected final <T extends Fragment> T findFragment(int id) {
        return (T) getFragmentManager().findFragmentById(id);
    }
}
