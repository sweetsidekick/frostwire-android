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

package com.frostwire.android.gui.fragments;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParser;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.frostwire.android.R;
import com.frostwire.android.gui.activities.MainActivity;
import com.frostwire.android.gui.activities.PreferencesActivity;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public class SlideMenuFragment extends ListFragment {

    private static final Logger LOG = LoggerFactory.getLogger(SlideMenuFragment.class);

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_slidemenu, null);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        MenuItem[] items = parseXml(getActivity(), R.menu.main).toArray(new MenuItem[0]);
        MenuAdapter adapter = new MenuAdapter(getActivity(), items);
        setListAdapter(adapter);
    }

    @Override
    public void onListItemClick(ListView lv, View v, int position, long id) {
        if (getActivity() == null) {
            return;
        }

        try {
            MenuAdapter adapter = (MenuAdapter) lv.getAdapter();
            MenuItem item = (MenuItem) adapter.getItem(position);

            if (item.id == R.id.menu_main_preferences) {
                adapter.notifyDataSetChanged();
                showPreferences(getActivity());
            } else {
                adapter.setSelectedItem(item.id);
                switchFragment(item.id);
            }
        } catch (Throwable e) { // protecting from weird android UI engine issues
            LOG.warn("Error clicking slide menu item", e);
        }
    }

    public void setSelectedItem(int id) {
        try {
            MenuAdapter adapter = (MenuAdapter) getListAdapter();
            adapter.setSelectedItem(id);
        } catch (Throwable e) { // protecting from weird android UI engine issues
            LOG.warn("Error setting slide menu item selected", e);
        }
    }

    private void switchFragment(int itemId) {
        if (getActivity() == null)
            return;

        if (getActivity() instanceof MainActivity) {
            MainActivity activity = (MainActivity) getActivity();
            activity.switchFragment(itemId);
        }
    }

    private List<MenuItem> parseXml(Context context, int menu) {

        List<MenuItem> list = new ArrayList<MenuItem>();

        try {
            XmlResourceParser xpp = context.getResources().getXml(menu);

            xpp.next();
            int eventType = xpp.getEventType();

            while (eventType != XmlPullParser.END_DOCUMENT) {

                if (eventType == XmlPullParser.START_TAG) {

                    String elemName = xpp.getName();

                    if (elemName.equals("item")) {

                        String textId = xpp.getAttributeValue("http://schemas.android.com/apk/res/android", "title");
                        String iconId = xpp.getAttributeValue("http://schemas.android.com/apk/res/android", "icon");
                        String resId = xpp.getAttributeValue("http://schemas.android.com/apk/res/android", "id");

                        MenuItem item = new MenuItem();
                        item.id = Integer.valueOf(resId.replace("@", ""));
                        item.icon = context.getResources().getDrawable(Integer.valueOf(iconId.replace("@", "")));
                        item.label = resourceIdToString(context, textId);

                        list.add(item);
                    }
                }

                eventType = xpp.next();
            }
        } catch (Throwable e) {
            LOG.error("Error loading menu items from resource", e);
        }

        return list;
    }

    private String resourceIdToString(Context context, String text) {
        if (!text.contains("@")) {
            return text;
        } else {
            String id = text.replace("@", "");
            return context.getResources().getString(Integer.valueOf(id));
        }
    }

    private void showPreferences(Context context) {
        Intent i = new Intent(context, PreferencesActivity.class);
        context.startActivity(i);
    }

    private static class MenuItem {
        public int id;
        public Drawable icon;
        public String label;
        public boolean selected;
    }

    private static class MenuAdapter extends ArrayAdapter<MenuItem> {

        private Activity activity;

        public MenuAdapter(Activity activity, MenuItem[] items) {
            super(activity, R.id.slidemenu_listitem_label, items);
            this.activity = activity;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View rowView = convertView;

            if (rowView == null) {
                LayoutInflater inflater = activity.getLayoutInflater();
                rowView = inflater.inflate(R.layout.slidemenu_listitem, null);
            }

            TextView label = (TextView) rowView.findViewById(R.id.slidemenu_listitem_label);
            ImageView icon = (ImageView) rowView.findViewById(R.id.slidemenu_listitem_icon);

            MenuItem item = getItem(position);

            label.setText(item.label);
            icon.setImageDrawable(item.icon);

            rowView.setBackgroundResource(item.selected ? R.drawable.slidemenu_listitem_background_selected : android.R.color.transparent);

            return rowView;
        }

        public void setSelectedItem(int id) {
            for (int i = 0; i < getCount(); i++) {
                MenuItem item = getItem(i);
                item.selected = item.id == id;
            }

            notifyDataSetChanged();
        }
    }
}
