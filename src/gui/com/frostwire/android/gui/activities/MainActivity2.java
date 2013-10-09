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
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
import com.frostwire.android.gui.SoftwareUpdater;
import com.frostwire.android.gui.fragments.SlideMenuFragment;
import com.frostwire.android.gui.util.OSUtils;
import com.frostwire.android.gui.views.AbstractActivity;

public class MainActivity2 extends AbstractActivity {

    private static final Logger LOG = LoggerFactory.getLogger(MainActivity2.class);

    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;

    private String[] mPlanetTitles;

    private static final String[] titles = new String[] { "a1", "a2", "a3", "a4", "a5", "a6" };

    public MainActivity2() {
        super(R.layout.activity_main2, false, 2);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPlanetTitles = titles;
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        // set a custom shadow that overlays the main content when the drawer opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        // set up the drawer's list view with items and click listener
        setupMenuItems();
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the sliding drawer and the action bar app icon
        mDrawerToggle = new ActionBarDrawerToggle(this, /* host Activity */
        mDrawerLayout, /* DrawerLayout object */
        R.drawable.ic_drawer, /* nav drawer image to replace 'Up' caret */
        R.string.open, /* "open drawer" description for accessibility */
        R.string.clear /* "close drawer" description for accessibility */
        ) {
            public void onDrawerClosed(View view) {
                //getActionBar().setTitle(mTitle);
                //invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            public void onDrawerOpened(View drawerView) {
                //getActionBar().setTitle(mDrawerTitle);
                //invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);

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

    /* Called whenever we call invalidateOptionsMenu() */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content view
        boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
        //menu.findItem(R.id.action_websearch).setVisible(!drawerOpen);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // The action bar home/up action should open or close the drawer.
        // ActionBarDrawerToggle will take care of this.
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        // Handle action buttons
        switch (item.getItemId()) {
        //        case R.id.action_websearch:
        //            // create intent to perform web search for this planet
        //            Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
        //            intent.putExtra(SearchManager.QUERY, getActionBar().getTitle());
        //            // catch event that there's no activity to handle intent
        //            if (intent.resolveActivity(getPackageManager()) != null) {
        //                startActivity(intent);
        //            } else {
        //                Toast.makeText(this, R.string.app_not_available, Toast.LENGTH_LONG).show();
        //            }
        //            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
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
        Fragment fragment = new PlanetFragment();
        Bundle args = new Bundle();
        args.putInt(PlanetFragment.ARG_PLANET_NUMBER, position);
        fragment.setArguments(args);

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
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggls
        mDrawerToggle.onConfigurationChanged(newConfig);
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
            int i = getArguments().getInt(ARG_PLANET_NUMBER);
            String planet = titles[i];

            //int imageId = getResources().getIdentifier(planet.toLowerCase(Locale.getDefault()), "drawable", getActivity().getPackageName());
            //((ImageView) rootView.findViewById(R.id.image)).setImageResource(imageId);
            getActivity().setTitle(planet);
            return rootView;
        }
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
