package com.frostwire.android.gui.activities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParser;

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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.frostwire.android.R;
import com.frostwire.android.core.ConfigurationManager;
import com.frostwire.android.core.Constants;
import com.frostwire.android.gui.util.OSUtils;
import com.frostwire.android.gui.views.AbstractActivity;
import com.frostwire.android.gui.views.AbstractListAdapter;

public class MainActivity3 extends AbstractActivity {

    private static final Logger LOG = LoggerFactory.getLogger(MainActivity3.class);

    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;

    private CharSequence mDrawerTitle;
    private CharSequence mTitle;
    private String[] mPlanetTitles;

    private static final String[] titles = new String[] { "a", "b", "c" };
    
    public MainActivity3() {
        super(R.layout.activity_main3, false, 2);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mTitle = mDrawerTitle = getTitle();
        mPlanetTitles = titles;
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        // set a custom shadow that overlays the main content when the drawer opens
        //mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        // set up the drawer's list view with items and click listener
        //mDrawerList.setAdapter(new ArrayAdapter<String>(this, R.layout.slidemenu_listitem, R.id.slidemenu_listitem_label, mPlanetTitles));
        setupMenuItems();
        //mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        // enable ActionBar app icon to behave as action to toggle nav drawer
        //getActionBar().setDisplayHomeAsUpEnabled(true);
        //getActionBar().setHomeButtonEnabled(true);

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the sliding drawer and the action bar app icon
        //        mDrawerToggle = new ActionBarDrawerToggle(this, /* host Activity */
        //        mDrawerLayout, /* DrawerLayout object */
        //        R.drawable.ic_drawer, /* nav drawer image to replace 'Up' caret */
        //        R.string.drawer_open, /* "open drawer" description for accessibility */
        //        R.string.drawer_close /* "close drawer" description for accessibility */
        //        ) {
        //            public void onDrawerClosed(View view) {
        //                getActionBar().setTitle(mTitle);
        //                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
        //            }
        //
        //            public void onDrawerOpened(View drawerView) {
        //                getActionBar().setTitle(mDrawerTitle);
        //                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
        //            }
        //        };
        //        mDrawerLayout.setDrawerListener(mDrawerToggle);

        if (savedInstanceState == null) {
            selectItem(0);
        }
    }

    private void selectItem(int position) {
        // update the main content by replacing fragments
        Fragment fragment = new PlanetFragment();
        Bundle args = new Bundle();
        args.putInt(PlanetFragment.ARG_PLANET_NUMBER, position);
        fragment.setArguments(args);

        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();

        // update selected item and title, then close the drawer
        mDrawerList.setItemChecked(position, true);
        setTitle(mPlanetTitles[position]);
        mDrawerLayout.closeDrawer(mDrawerList);
    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        //getActionBar().setTitle(mTitle);
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

    /**
     * Fragment that appears in the "content_frame", shows a planet
     */
    public static class PlanetFragment extends Fragment {
        public static final String ARG_PLANET_NUMBER = "planet_number";

        public PlanetFragment() {
            // Empty constructor required for fragment subclasses
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_about, container, false);

            return rootView;
        }
    }

    private static class XmlMenuItem {
        public int id;
        public int iconResId;
        public String label;
        public boolean selected;
    }

    private  class MenuAdapter2 extends AbstractListAdapter<XmlMenuItem> {

        public MenuAdapter2(Context context, XmlMenuItem[] items) {
            super(context, R.layout.slidemenu_listitem, Arrays.asList(items));
        }

        @Override
        protected void populateView(View view, XmlMenuItem item) {
            TextView label = (TextView) view.findViewById(R.id.slidemenu_listitem_label);
            ImageView icon = (ImageView) view.findViewById(R.id.slidemenu_listitem_icon);

            label.setText(item.label);
            icon.setImageResource(item.iconResId);

            view.setBackgroundResource(item.selected ? R.drawable.slidemenu_listitem_background_selected : android.R.color.transparent);
        }

        @Override
        protected void onItemClicked(View v) {
            mDrawerLayout.closeDrawer(mDrawerList);
            System.out.println("hello");
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
        MenuAdapter2 adapter = new MenuAdapter2(this, items);
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
