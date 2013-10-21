/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2013, FrostWire(R). All rights reserved.
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

package com.frostwire.android.gui.activities.internal;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.frostwire.android.R;
import com.frostwire.android.gui.views.AbstractListAdapter;

/**
 * 
 * @author gubatron
 * @author aldenml
 *
 */
public final class XmlMenuAdapter extends AbstractListAdapter<XmlMenuItem> {

    private static final Logger LOG = LoggerFactory.getLogger(XmlMenuAdapter.class);

    private MainController controller;

    public XmlMenuAdapter(MainController controller, XmlMenuItem[] items) {
        super(controller.getActivity(), R.layout.slidemenu_listitem, Arrays.asList(items));
        this.controller = controller;
    }

    @Override
    protected void populateView(View view, XmlMenuItem item) {
        TextView label = (TextView) view.findViewById(R.id.slidemenu_listitem_label);
        ImageView icon = (ImageView) view.findViewById(R.id.slidemenu_listitem_icon);

        label.setText(item.label);
        icon.setImageResource(item.selected ? item.iconOverResId : item.iconResId);

        view.setBackgroundResource(item.selected ? R.drawable.slidemenu_listitem_background_selected : android.R.color.transparent);
    }

    @Override
    protected void onItemClicked(View v) {
        controller.closeSlideMenu();

        try {
            int id = (Integer) ((XmlMenuItem) v.getTag()).id;
            if (id == R.id.menu_main_preferences) {
                controller.showPreferences();
            } else if (id == R.id.menu_launch_tv) {
                controller.launchFrostWireTV();
            } else if (id == R.id.menu_free_apps) {
                controller.showFreeApps();
            } else {
                setSelectedItem(id);
                controller.switchFragment(id);
            }
        } catch (Throwable e) { // protecting from weird android UI engine issues
            LOG.error("Error clicking slide menu item", e);
        }
    }

    public void setSelectedItem(int id) {
        for (int i = 0; i < getCount(); i++) {
            XmlMenuItem item = getItem(i);
            item.selected = item.id == id;
        }

        notifyDataSetChanged();
    }
}
