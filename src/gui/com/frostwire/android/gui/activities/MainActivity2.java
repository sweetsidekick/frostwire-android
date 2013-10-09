package com.frostwire.android.gui.activities;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParser;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.frostwire.android.R;
import com.frostwire.android.core.ConfigurationManager;
import com.frostwire.android.core.Constants;
import com.frostwire.android.gui.fragments.AboutFragment;
import com.frostwire.android.gui.util.OSUtils;
import com.frostwire.android.gui.views.AbstractActivity;

public class MainActivity2 extends AbstractActivity {

    private static final Logger LOG = LoggerFactory.getLogger(MainActivity2.class);

    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;

    public MainActivity2() {
        super(R.layout.activity_main2, false, 2);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        // set a custom shadow that overlays the main content when the drawer opens
        //mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        // set up the drawer's list view with items and click listener
        setupMenuItems();
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        // TODO:
        //mDrawerLayout.setDrawerListener(mDrawerToggle);

        if (savedInstanceState == null) {
            selectItem(0);
        }
    }

    private void setupMenuItems() {
        XmlMenuItem[] items = parseXml(this, R.menu.main).toArray(new XmlMenuItem[0]);
        ConfigurationManager config = ConfigurationManager.instance();
        if (!config.getBoolean(Constants.PREF_KEY_GUI_SHOW_TV_MENU_ITEM)) {
            items = removeMenuItem(R.id.menu_launch_tv, items);
        }

        if (!config.getBoolean(Constants.PREF_KEY_GUI_SUPPORT_FROSTWIRE) || !config.getBoolean(Constants.PREF_KEY_GUI_INITIALIZE_APPIA) || OSUtils.isAmazonDistribution()) { //!config.getBoolean(Constants.PREF_KEY_GUI_SHOW_FREE_APPS_MENU_ITEM)) {
            items = removeMenuItem(R.id.menu_free_apps, items);
        }
        MenuAdapter adapter = new MenuAdapter(this, items);
        mDrawerList.setAdapter(adapter);
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

    /* The click listner for ListView in the navigation drawer */
    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position);
        }
    }

    private void selectItem(int position) {
        // update the main content by replacing fragments
        Fragment fragment = new AboutFragment();
//        Bundle args = new Bundle();
//        args.putInt(PlanetFragment.ARG_PLANET_NUMBER, position);
//        fragment.setArguments(args);

        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();

        // update selected item and title, then close the drawer
        mDrawerList.setItemChecked(position, true);
        mDrawerLayout.closeDrawer(mDrawerList);
    }

    /**
     * When using the ActionBarDrawerToggle, you must call it during
     * onPostCreate() and onConfigurationChanged()...
     */

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        //mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggls
        //mDrawerToggle.onConfigurationChanged(newConfig);
    }

    private static class XmlMenuItem {
        public int id;
        public int iconResId;
        public String label;
        public boolean selected;
    }

    private static class MenuAdapter extends ArrayAdapter<XmlMenuItem> {

        private Activity activity;

        public MenuAdapter(Activity activity, XmlMenuItem[] items) {
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

            XmlMenuItem item = getItem(position);

            label.setText(item.label);
            icon.setImageResource(item.iconResId);

            rowView.setBackgroundResource(item.selected ? R.drawable.slidemenu_listitem_background_selected : android.R.color.transparent);

            return rowView;
        }

        public void setSelectedItem(int id) {
            for (int i = 0; i < getCount(); i++) {
                XmlMenuItem item = getItem(i);
                item.selected = item.id == id;
            }

            notifyDataSetChanged();
        }
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
}
