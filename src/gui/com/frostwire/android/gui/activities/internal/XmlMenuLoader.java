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

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParser;

import android.content.Context;
import android.content.res.XmlResourceParser;

import com.frostwire.android.R;
import com.frostwire.android.core.ConfigurationManager;
import com.frostwire.android.core.Constants;
import com.frostwire.android.gui.util.OSUtils;
import com.frostwire.android.gui.util.OfferUtils;

/**
 * 
 * @author gubatron
 * @author aldenml
 *
 */
public final class XmlMenuLoader {

    private static final Logger LOG = LoggerFactory.getLogger(XmlMenuLoader.class);

    public XmlMenuLoader() {
    }

    public XmlMenuItem[] load(Context context) {
        XmlMenuItem[] items = parseXml(context, R.menu.main).toArray(new XmlMenuItem[0]);
        ConfigurationManager config = ConfigurationManager.instance();
        if (!config.getBoolean(Constants.PREF_KEY_GUI_SHOW_TV_MENU_ITEM)) {
            items = removeMenuItem(R.id.menu_launch_tv, items);
        }

        if (!config.getBoolean(Constants.PREF_KEY_GUI_SUPPORT_FROSTWIRE) || !config.getBoolean(Constants.PREF_KEY_GUI_INITIALIZE_APPIA) || OSUtils.isAmazonDistribution()) { //!config.getBoolean(Constants.PREF_KEY_GUI_SHOW_FREE_APPS_MENU_ITEM)) {
            items = removeMenuItem(R.id.menu_free_apps, items);
        }

        if (!OfferUtils.isfreeAppsEnabled()) {
            items = removeMenuItem(R.id.menu_free_apps, items);
        }

        fillOversIcons(items);

        return items;
    }

    private XmlMenuItem[] removeMenuItem(int idToRemove, XmlMenuItem[] originalItems) {
        List<XmlMenuItem> items = new ArrayList<XmlMenuItem>();
        for (XmlMenuItem i : originalItems) {
            if (i.id != idToRemove) {
                items.add(i);
            }
        }
        return items.toArray(new XmlMenuItem[0]);
    }

    private List<XmlMenuItem> parseXml(Context context, int menu) {

        List<XmlMenuItem> list = new ArrayList<XmlMenuItem>();

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

                        XmlMenuItem item = new XmlMenuItem();
                        item.id = Integer.valueOf(resId.replace("@", ""));
                        item.iconResId = Integer.valueOf(iconId.replace("@", ""));
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

    private void fillOversIcons(XmlMenuItem[] items) {
        for (XmlMenuItem item : items) {
            switch (item.id) {
            case R.id.menu_main_search:
                item.iconOverResId = R.drawable.menu_icon_search_over;
                break;
            case R.id.menu_main_library:
                item.iconOverResId = R.drawable.menu_icon_library_over;
                break;
            case R.id.menu_main_transfers:
                item.iconOverResId = R.drawable.menu_icon_transfers_over;
                break;
            case R.id.menu_main_peers:
                item.iconOverResId = R.drawable.menu_icon_peers_over;
                break;
            case R.id.menu_free_apps:
                item.iconOverResId = R.drawable.menu_icon_free_apps_over;
                break;
            case R.id.menu_launch_tv:
                item.iconOverResId = item.iconResId; // missing icon // R.drawable.menu_icon_tv_over;
                break;
            case R.id.menu_main_preferences:
                item.iconOverResId = R.drawable.menu_icon_preferences_over;
                break;
            case R.id.menu_main_about:
                item.iconOverResId = R.drawable.menu_icon_about_over;
                break;
            default:
                item.iconOverResId = item.iconResId;
            }
        }
    }
}
