package com.frostwire.android.gui.activities;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParser;

import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.XmlResourceParser;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.DrawerLayout.SimpleDrawerListener;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.appia.sdk.Appia;
import com.appia.sdk.Appia.WallDisplayType;
import com.frostwire.android.R;
import com.frostwire.android.core.ConfigurationManager;
import com.frostwire.android.core.Constants;
import com.frostwire.android.core.DesktopUploadRequest;
import com.frostwire.android.core.DesktopUploadRequestStatus;
import com.frostwire.android.core.FileDescriptor;
import com.frostwire.android.gui.Librarian;
import com.frostwire.android.gui.PeerManager;
import com.frostwire.android.gui.SoftwareUpdater;
import com.frostwire.android.gui.SoftwareUpdater.ConfigurationUpdateListener;
import com.frostwire.android.gui.fragments.AboutFragment;
import com.frostwire.android.gui.fragments.BrowsePeerFragment;
import com.frostwire.android.gui.fragments.BrowsePeersDisabledFragment;
import com.frostwire.android.gui.fragments.BrowsePeersFragment;
import com.frostwire.android.gui.fragments.MainFragment;
import com.frostwire.android.gui.fragments.SearchFragment;
import com.frostwire.android.gui.fragments.TransfersFragment;
import com.frostwire.android.gui.services.DesktopUploadManager;
import com.frostwire.android.gui.services.Engine;
import com.frostwire.android.gui.transfers.TransferManager;
import com.frostwire.android.gui.util.OSUtils;
import com.frostwire.android.gui.util.OfferUtils;
import com.frostwire.android.gui.util.UIUtils;
import com.frostwire.android.gui.views.AbstractActivity;
import com.frostwire.android.gui.views.AbstractListAdapter;
import com.frostwire.android.gui.views.DesktopUploadRequestDialog;
import com.frostwire.android.gui.views.DesktopUploadRequestDialogResult;
import com.frostwire.android.gui.views.PlayerMenuItemView;
import com.frostwire.android.gui.views.Refreshable;
import com.frostwire.android.gui.views.ShareIndicationDialog;
import com.frostwire.android.gui.views.TOS;
import com.frostwire.android.gui.views.TOS.OnTOSAcceptListener;
import com.frostwire.uxstats.UXAction;
import com.frostwire.uxstats.UXStats;

public class MainActivity3 extends AbstractActivity implements ConfigurationUpdateListener {

    private static final Logger LOG = LoggerFactory.getLogger(MainActivity3.class);

    private static final String FRAGMENT_STACK_TAG = "fragment_stack";
    private static final String CURRENT_FRAGMENT_KEY = "current_fragment";
    private static final String DUR_TOKEN_KEY = "dur_token";
    private static final String APPIA_STARTED_KEY = "appia_started";

    private static boolean firstTime = true;

    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private View leftDrawer;

    private SearchFragment search;
    private BrowsePeerFragment library;
    private TransfersFragment transfers;
    private BrowsePeersFragment peers;
    private BrowsePeersDisabledFragment peersDisabled;
    private AboutFragment about;
    private PlayerMenuItemView playerItem;

    // not sure about this variable, quick solution for now
    private String durToken;

    //private boolean offercastStarted = false;
    private boolean appiaStarted = false;

    public MainActivity3() {
        super(R.layout.activity_main3, false, 2);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        leftDrawer = findView(R.id.activity_main_left_drawer);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        playerItem = (PlayerMenuItemView) findViewById(R.id.slidemenu_player_menuitem);
        playerItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchPlayerActivity();
            }
        });

        setupFragments();

        setupInitialFragment(savedInstanceState);

        setupMenuItems();
        mDrawerLayout.setDrawerListener(new SimpleDrawerListener() {
            @Override
            public void onDrawerStateChanged(int newState) {
                refreshPlayerItem();
            }
        });

        ImageButton buttonMenu = findView(R.id.activity_main_button_menu);
        buttonMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleDrawer();
            }
        });

        if (savedInstanceState != null) {
            durToken = savedInstanceState.getString(DUR_TOKEN_KEY);
            //offercastStarted = savedInstanceState.getBoolean(OFFERCAST_STARTED_KEY);
            appiaStarted = savedInstanceState.getBoolean(APPIA_STARTED_KEY);
        }

        addRefreshable((Refreshable) findView(R.id.activity_main_player_notifier));

        onNewIntent(getIntent());

        SoftwareUpdater.instance().addConfigurationUpdateListener(this);
    }

    private void showTransfers() {
        if (!(getCurrentFragment() instanceof TransfersFragment)) {
            switchFragment(R.id.menu_main_transfers);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!appiaStarted && ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_GUI_INITIALIZE_APPIA)) {
            startAppia();
        }

        if (ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_GUI_TOS_ACCEPTED)) {
            if (ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_GUI_INITIAL_SETTINGS_COMPLETE)) {
                mainResume();
            } else {
                startWizardActivity();
            }
        } else {
            trackDialog(TOS.showEula(this, new OnTOSAcceptListener() {
                public void onAccept() {
                    startWizardActivity();
                }
            }));
        }

        checkLastSeenVersion();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        saveLastFragment(outState);

        outState.putString(DUR_TOKEN_KEY, durToken);
        //outState.putBoolean(OFFERCAST_STARTED_KEY, offercastStarted);
        outState.putBoolean(APPIA_STARTED_KEY, appiaStarted);
    }

    private void saveLastFragment(Bundle outState) {
        Fragment fragment = getCurrentFragment();
        if (fragment != null) {
            getSupportFragmentManager().putFragment(outState, CURRENT_FRAGMENT_KEY, fragment);
        }
    }

    private void mainResume() {
        syncSlideMenu();

        if (firstTime) {
            firstTime = false;
            Engine.instance().startServices(); // it's necessary for the first time after wizard
        }

        SoftwareUpdater.instance().checkForUpdate(this);
    }

    private void startWizardActivity() {
        Intent i = new Intent(this, WizardActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
    }

    private void checkLastSeenVersion() {
        final String lastSeenVersion = ConfigurationManager.instance().getString(Constants.PREF_KEY_CORE_LAST_SEEN_VERSION);
        if (lastSeenVersion == null || lastSeenVersion.equals("")) {
            //fresh install
            ConfigurationManager.instance().setString(Constants.PREF_KEY_CORE_LAST_SEEN_VERSION, Constants.FROSTWIRE_VERSION_STRING);
            UXStats.instance().log(UXAction.CONFIGURATION_WIZARD_FIRST_TIME);
        } else if (!Constants.FROSTWIRE_VERSION_STRING.equals(lastSeenVersion)) {
            //just updated.
            ConfigurationManager.instance().setString(Constants.PREF_KEY_CORE_LAST_SEEN_VERSION, Constants.FROSTWIRE_VERSION_STRING);
            UXStats.instance().log(UXAction.CONFIGURATION_WIZARD_AFTER_UPDATE);
        }
    }

    private void startAppia() {
        try {
            Appia appia = Appia.getAppia();
            appia.setSiteId(3867);
            appiaStarted = true;
        } catch (Throwable t) {
            appiaStarted = false;
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        String action = intent.getAction();
        onResumeFragments();

        if (action != null && action.equals(Constants.ACTION_SHOW_TRANSFERS)) {
            showTransfers();
        } else if (action != null && action.equals(Constants.ACTION_OPEN_TORRENT_URL)) {
            //Open a Torrent from a URL or from a local file :), say from Astro File Manager.
            /**
             * TODO: Ask @aldenml the best way to plug in NewTransferDialog.
             * I've refactored this dialog so that it is forced (no matter if the setting
             * to not show it again has been used) and when that happens the checkbox is hidden.
             * 
             * However that dialog requires some data about the download, data which is not
             * obtained until we have instantiated the Torrent object.
             * 
             * I'm thinking that we can either:
             * a) Pass a parameter to the transfer manager, but this would probably
             * not be cool since the transfer manager (I think) should work independently from
             * the UI thread.
             * 
             * b) Pass a "listener" to the transfer manager, once the transfer manager has the torrent
             * it can notify us and wait for the user to decide wether or not to continue with the transfer
             * 
             * c) Forget about showing that dialog, and just start the download, the user can cancel it.
             */

            //Show me the transfer tab
            Intent i = new Intent(this, MainActivity.class);
            i.setAction(Constants.ACTION_SHOW_TRANSFERS);
            i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(i);

            //go!
            TransferManager.instance().downloadTorrent(intent.getDataString());
        } else if ((action != null && action.equals(Constants.ACTION_DESKTOP_UPLOAD_REQUEST)) || durToken != null) {
            handleDesktopUploadRequest(intent);
        }
        // When another application wants to "Share" a file and has chosen FrostWire to do so.
        // We make the file "Shared" so it's visible for other FrostWire devices on the local network.
        else if (action != null && (action.equals(Intent.ACTION_SEND) || action.equals(Intent.ACTION_SEND_MULTIPLE))) {
            handleSendAction(intent);
        }

        if (intent.hasExtra(Constants.EXTRA_DOWNLOAD_COMPLETE_NOTIFICATION)) {
            showTransfers();
            TransferManager.instance().clearDownloadsToReview();
            try {
                ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancel(Constants.NOTIFICATION_DOWNLOAD_TRANSFER_FINISHED);
                Bundle extras = intent.getExtras();
                if (extras.containsKey(Constants.EXTRA_DOWNLOAD_COMPLETE_PATH)) {
                    File file = new File(extras.getString(Constants.EXTRA_DOWNLOAD_COMPLETE_PATH));
                    if (file.isFile()) {
                        UIUtils.openFile(this, file.getAbsoluteFile());
                    }
                }
            } catch (Throwable e) {
                LOG.warn("Error handling download complete notification", e);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        search.dismissDialogs();
        library.dismissDialogs();
        peers.dismissDialogs();
    }

    private void handleSendAction(Intent intent) {
        String action = intent.getAction();
        if (action.equals(Intent.ACTION_SEND)) {
            handleSendSingleFile(intent);
        } else if (action.equals(Intent.ACTION_SEND_MULTIPLE)) {
            handleSendMultipleFiles(intent);
        }
    }

    private void handleSendMultipleFiles(Intent intent) {
        ArrayList<Uri> fileUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        if (fileUris != null) {
            for (Uri uri : fileUris) {
                shareFileByUri(uri);
            }
            UIUtils.showLongMessage(this, getString(R.string.n_files_shared, fileUris.size()));
        }
    }

    private void handleSendSingleFile(Intent intent) {
        Uri uri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (uri == null) {
            return;
        }
        shareFileByUri(uri);
        UIUtils.showLongMessage(this, R.string.one_file_shared);
    }

    private void shareFileByUri(Uri uri) {
        if (uri == null) {
            return;
        }

        FileDescriptor fileDescriptor = Librarian.instance().getFileDescriptor(uri);

        if (fileDescriptor != null) {
            fileDescriptor.shared = true;
            Librarian.instance().updateSharedStates(fileDescriptor.fileType, Arrays.asList(fileDescriptor));
        }
    }

    private void handleDesktopUploadRequest(Intent intent) {
        String action = intent.getAction();

        if (durToken == null && action.equals(Constants.ACTION_DESKTOP_UPLOAD_REQUEST)) {
            durToken = intent.getStringExtra(Constants.EXTRA_DESKTOP_UPLOAD_REQUEST_TOKEN);
        }

        final DesktopUploadManager dum = Engine.instance().getDesktopUploadManager();

        if (dum == null) {
            return;
        }

        DesktopUploadRequest dur = dum.getRequest(durToken);

        if (durToken != null && dur != null && dur.status == DesktopUploadRequestStatus.PENDING) {
            DesktopUploadRequestDialog dlg = new DesktopUploadRequestDialog(this, dur, new DesktopUploadRequestDialog.OnDesktopUploadListener() {
                @Override
                public void onResult(DesktopUploadRequestDialog dialog, DesktopUploadRequestDialogResult result) {
                    switch (result) {
                    case ACCEPT:
                        dum.authorizeRequest(durToken);
                        if (ConfigurationManager.instance().showTransfersOnDownloadStart()) {
                            Intent i = new Intent(Constants.ACTION_SHOW_TRANSFERS);
                            MainActivity3.this.startActivity(i.setClass(MainActivity3.this, MainActivity.class));
                        }
                        break;
                    case REJECT:
                        dum.rejectRequest(durToken);
                        break;
                    case BLOCK:
                        dum.blockComputer(durToken);
                        break;
                    }
                    durToken = null;
                }
            });

            trackDialog(dlg).show();
        }
    }

    private void toggleDrawer() {
        if (mDrawerLayout.isDrawerOpen(leftDrawer)) {
            mDrawerLayout.closeDrawer(leftDrawer);
        } else {
            mDrawerLayout.openDrawer(leftDrawer);
        }
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            super.onBackPressed();
        } else {
            handleLastBackPressed();
        }

        syncSlideMenu();
        updateHeader(getCurrentFragment());
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_SEARCH) {
            if (!(getCurrentFragment() instanceof SearchFragment)) {
                switchFragment(R.id.menu_main_search);
            }
        } else if (keyCode == KeyEvent.KEYCODE_MENU) {
            toggleDrawer();
        } else {
            return super.onKeyDown(keyCode, event);
        }

        return true;
    }

    public void switchFragment(int itemId) {
        Fragment fragment = getFragmentByMenuId(itemId);
        if (fragment != null) {
            switchContent(fragment);
        }
    }

    public void showMyFiles() {
        if (!(getCurrentFragment() instanceof BrowsePeerFragment)) {
            switchFragment(R.id.menu_main_library);
        }
        if (ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_GUI_SHOW_SHARE_INDICATION)) {
            showShareIndication();
        }
    }

    private void showShareIndication() {
        ShareIndicationDialog dlg = new ShareIndicationDialog();
        dlg.show(getSupportFragmentManager());
    }

    private Fragment getFragmentByMenuId(int id) {
        switch (id) {
        case R.id.menu_main_search:
            return search;
        case R.id.menu_main_library:
            return library;
        case R.id.menu_main_transfers:
            return transfers;
        case R.id.menu_main_peers:
            return getWifiSharingFragment();
        case R.id.menu_main_about:
            return about;
        default:
            return null;
        }
    }

    private Fragment getWifiSharingFragment() {
        return (Fragment) (isWifiSharingEnabled() ? peers : peersDisabled);
    }

    private boolean isWifiSharingEnabled() {
        return Engine.instance().isStarted() && ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_NETWORK_USE_UPNP);
    }

    private void switchContent(Fragment fragment) {
        getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, fragment, FRAGMENT_STACK_TAG).addToBackStack(null).commit();
        //mDrawerLayout.openDrawer(mDrawerList);
        syncSlideMenu();
        updateHeader(fragment);
    }

    private Fragment getCurrentFragment() {
        return getSupportFragmentManager().findFragmentByTag(FRAGMENT_STACK_TAG);
    }

    private void handleLastBackPressed() {
        trackDialog(UIUtils.showYesNoDialog(this, R.string.are_you_sure_you_wanna_leave, R.string.minimize_frostwire, new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        }));
    }

    private void syncSlideMenu() {
        //        if (menuFragment != null) {
        //            Fragment fragment = getCurrentFragment();
        //
        //            if (fragment instanceof SearchFragment) {
        //                menuFragment.setSelectedItem(R.id.menu_main_search);
        //            } else if (fragment instanceof BrowsePeerFragment) {
        //                menuFragment.setSelectedItem(R.id.menu_main_library);
        //            } else if (fragment instanceof TransfersFragment) {
        //                menuFragment.setSelectedItem(R.id.menu_main_transfers);
        //            } else if (fragment instanceof BrowsePeersFragment ||
        //                       fragment instanceof BrowsePeersDisabledFragment) {
        //                menuFragment.setSelectedItem(R.id.menu_main_peers);
        //            } else if (fragment instanceof AboutFragment) {
        //                menuFragment.setSelectedItem(R.id.menu_main_about);
        //            }
        //        }
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
        mDrawerLayout.closeDrawer(leftDrawer);
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

        //initMenuItems();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        //avoid memory leaks when the device is tilted and the menu gets recreated.
        SoftwareUpdater.instance().removeConfigurationUpdateListener(this);

        if (playerItem != null) {
            playerItem.unbindDrawables();
        }
    }

    public void refreshPlayerItem() {
        playerItem.refresh();
    }

    private static class XmlMenuItem {
        public int id;
        public int iconResId;
        public String label;
        public boolean selected;
    }

    private class MenuAdapter2 extends AbstractListAdapter<XmlMenuItem> {

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
            mDrawerLayout.closeDrawer(leftDrawer);

            try {
                int id = (Integer) ((XmlMenuItem) v.getTag()).id;
                if (id == R.id.menu_main_preferences) {
                    //adapter.notifyDataSetChanged();
                    showPreferences(MainActivity3.this);
                } else if (id == R.id.menu_launch_tv) {
                    launchFrostWireTV();
                } else if (id == R.id.menu_free_apps) {
                    showFreeApps();
                } else {
                    //adapter.setSelectedItem(item.id);
                    switchFragment(id);
                }
            } catch (Throwable e) { // protecting from weird android UI engine issues
                LOG.error("Error clicking slide menu item", e);
            }
        }
    }

    /**
     * Will try to launch the app, if it cannot find the launch intent, it'll take the user to the Android market.
     */
    private void launchFrostWireTV() {
        Intent intent = null;
        try {
            intent = getApplicationContext().getPackageManager().getLaunchIntentForPackage("com.frostwire.android.tv");

            //on the nexus it wasn't throwing the NameNotFoundException, it was just returning null
            if (intent == null) {
                throw new NullPointerException();
            }
        } catch (Throwable t) {
            intent = new Intent();
            intent.setData(Uri.parse("market://details?id=com.frostwire.android.tv"));
        }
        startActivity(intent);
    }

    private void launchPlayerActivity() {
        //        if (getActivity() == null) {
        //            return;
        //        }
        //
        //        if (getActivity() instanceof MainActivity) {
        //            ((MainActivity) getActivity()).showContent();
        //        }

        if (Engine.instance().getMediaPlayer().getCurrentFD() != null) {
            Intent i = new Intent(this, MediaPlayerActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        }
    }

    private void showPreferences(Context context) {
        Intent i = new Intent(context, PreferencesActivity.class);
        context.startActivity(i);
    }

    private void showFreeApps() {
        try {
            //OffercastSDK offercast = OffercastSDK.getInstance(getActivity());
            //offercast.showAppWallAd();
            Appia appia = Appia.getAppia();
            appia.cacheAppWall(this);
            appia.displayWall(this, WallDisplayType.FULL_SCREEN);
        } catch (Throwable e) {
            LOG.error("Can't show app wall", e);
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

        if (!OfferUtils.isfreeAppsEnabled()) {
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

    private void setupFragments() {
        search = new SearchFragment();
        library = new BrowsePeerFragment();
        transfers = new TransfersFragment();
        peers = new BrowsePeersFragment();
        peersDisabled = new BrowsePeersDisabledFragment();
        about = new AboutFragment();

        library.setPeer(PeerManager.instance().getLocalPeer());
    }

    private void setupInitialFragment(Bundle savedInstanceState) {
        Fragment fragment = null;

        if (savedInstanceState != null) {
            fragment = getSupportFragmentManager().getFragment(savedInstanceState, CURRENT_FRAGMENT_KEY);
        }
        if (fragment == null) {
            fragment = search;
        }

        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.content_frame, fragment, FRAGMENT_STACK_TAG).commit();

        updateHeader(fragment);
    }

    private void updateHeader(Fragment fragment) {
        try {
            RelativeLayout placeholder = findView(R.id.activity_main_layout_header_placeholder);
            if (placeholder.getChildCount() > 0) {
                placeholder.removeAllViews();
            }

            if (fragment instanceof MainFragment) {
                View header = ((MainFragment) fragment).getHeader(this);
                if (header != null) {
                    RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                    params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
                    placeholder.addView(header, params);
                }
            }
        } catch (Throwable e) {
            LOG.error("Error updating main header", e);
        }
    }

    @Override
    public void onConfigurationUpdate() {
        setupMenuItems();
    }
}
