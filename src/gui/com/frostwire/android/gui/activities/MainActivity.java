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

package com.frostwire.android.gui.activities;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.NotificationManager;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import com.frostwire.android.R;
import com.frostwire.android.core.ConfigurationManager;
import com.frostwire.android.core.Constants;
import com.frostwire.android.core.DesktopUploadRequest;
import com.frostwire.android.core.DesktopUploadRequestStatus;
import com.frostwire.android.core.FileDescriptor;
import com.frostwire.android.gui.Librarian;
import com.frostwire.android.gui.PeerManager;
import com.frostwire.android.gui.SoftwareUpdater;
import com.frostwire.android.gui.fragments.AboutFragment;
import com.frostwire.android.gui.fragments.BrowsePeerFragment;
import com.frostwire.android.gui.fragments.BrowsePeersDisabledFragment;
import com.frostwire.android.gui.fragments.BrowsePeersFragment;
import com.frostwire.android.gui.fragments.MainFragment;
import com.frostwire.android.gui.fragments.SearchFragment;
import com.frostwire.android.gui.fragments.SlideMenuFragment;
import com.frostwire.android.gui.fragments.TransfersFragment;
import com.frostwire.android.gui.services.DesktopUploadManager;
import com.frostwire.android.gui.services.Engine;
import com.frostwire.android.gui.transfers.TransferManager;
import com.frostwire.android.gui.util.UIUtils;
import com.frostwire.android.gui.views.AbstractSlidingActivity;
import com.frostwire.android.gui.views.DesktopUploadRequestDialog;
import com.frostwire.android.gui.views.DesktopUploadRequestDialogResult;
import com.frostwire.android.gui.views.Refreshable;
import com.frostwire.android.gui.views.ShareIndicationDialog;
import com.frostwire.android.gui.views.TOS;
import com.frostwire.android.gui.views.TOS.OnTOSAcceptListener;
import com.offercast.android.sdk.OffercastSDK;
import com.slidingmenu.lib.SlidingMenu;
import com.slidingmenu.lib.SlidingMenu.CanvasTransformer;
import com.slidingmenu.lib.SlidingMenu.OnOpenListener;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public class MainActivity extends AbstractSlidingActivity {

    private static final Logger LOG = LoggerFactory.getLogger(MainActivity.class);

    private static final String FRAGMENT_STACK_TAG = "fragment_stack";
    private static final String CURRENT_FRAGMENT_KEY = "current_fragment";
    private static final String DUR_TOKEN_KEY = "dur_token";
    private static final String OFFERCAST_STARTED_KEY = "offercast_started";

    private static boolean firstTime = true;

    private SlideMenuFragment menuFragment;

    private SearchFragment search;
    private BrowsePeerFragment library;
    private TransfersFragment transfers;
    private BrowsePeersFragment peers;
    private BrowsePeersDisabledFragment peersDisabled;
    private AboutFragment about;

    // not sure about this variable, quick solution for now
    private String durToken;
    
    private boolean offercastStarted = false;

    public MainActivity() {
        super(R.layout.activity_main, false, 2);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupFragments();

        setupInitialFragment(savedInstanceState);

        setupSlideMenu();

        ImageButton buttonMenu = findView(R.id.activity_main_button_menu);
        buttonMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getSlidingMenu().toggle();
            }
        });

        if (savedInstanceState != null) {
            durToken = savedInstanceState.getString(DUR_TOKEN_KEY);
            offercastStarted = savedInstanceState.getBoolean(OFFERCAST_STARTED_KEY);
        }

        addRefreshable((Refreshable) findView(R.id.activity_main_player_notifier));

        onNewIntent(getIntent());
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
            getSlidingMenu().toggle();
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

    @Override
    protected void onResume() {
        super.onResume();
        
        if (!offercastStarted && ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_GUI_INITIALIZE_OFFERCAST)) {
            startOffercast();
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
    }

    private void startOffercast() {
        try {
            OffercastSDK offercast = OffercastSDK.getInstance(getApplicationContext());
            offercast.authorize();
            offercastStarted = true;
            LOG.info("Offercast started.");
        } catch (Exception e) {
            offercastStarted = false;
            LOG.error("Offercast could not start.",e);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        search.dismissDialogs();
        library.dismissDialogs();
        peers.dismissDialogs();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        saveLastFragment(outState);

        outState.putString(DUR_TOKEN_KEY, durToken);
        outState.putBoolean(OFFERCAST_STARTED_KEY, offercastStarted);
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

    private Fragment getCurrentFragment() {
        return getSupportFragmentManager().findFragmentByTag(FRAGMENT_STACK_TAG);
    }

    private void saveLastFragment(Bundle outState) {
        Fragment fragment = getCurrentFragment();
        if (fragment != null) {
            getSupportFragmentManager().putFragment(outState, CURRENT_FRAGMENT_KEY, fragment);
        }
    }

    private void handleLastBackPressed() {
        trackDialog(UIUtils.showYesNoDialog(this, R.string.are_you_sure_you_wanna_leave, R.string.minimize_frostwire, new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        }));
    }

    private void switchContent(Fragment fragment) {
        getSupportFragmentManager().beginTransaction().replace(R.id.activity_main_content_frame, fragment, FRAGMENT_STACK_TAG).addToBackStack(null).commit();
        getSupportFragmentManager().executePendingTransactions();
        getSlidingMenu().showContent();

        syncSlideMenu();

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

    private void setupFragments() {
        menuFragment = new SlideMenuFragment();

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

        getSupportFragmentManager().beginTransaction().replace(R.id.activity_main_content_frame, fragment, FRAGMENT_STACK_TAG).commit();

        updateHeader(fragment);
    }

    private void setupSlideMenu() {
        setBehindContentView(R.layout.slidemenu_frame);
        getSupportFragmentManager().beginTransaction().replace(R.id.slidemenu_frame, menuFragment).commit();

        SlidingMenu menu = getSlidingMenu();

        menu.setShadowWidthRes(R.dimen.mainmenu_shadow_width);
        menu.setShadowDrawable(R.drawable.mainmenu_shadow);
        menu.setBehindWidthRes(R.dimen.mainmenu_width);
        menu.setFadeDegree(0.35f);
        menu.setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
        menu.setBehindScrollScale(0.0f);

        menu.setBehindCanvasTransformer(new CanvasTransformer() {
            @Override
            public void transformCanvas(Canvas canvas, float percentOpen) {
                float scale = (float) (percentOpen * 0.25 + 0.75);
                canvas.scale(scale, scale, canvas.getWidth() / 2, canvas.getHeight() / 2);
            }
        });

        menu.setOnOpenListener(new OnOpenListener() {
            @Override
            public void onOpen() {
                menuFragment.refreshPlayerItem();
            }
        });
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

    private void syncSlideMenu() {
        if (menuFragment != null) {
            Fragment fragment = getCurrentFragment();

            if (fragment instanceof SearchFragment) {
                menuFragment.setSelectedItem(R.id.menu_main_search);
            } else if (fragment instanceof BrowsePeerFragment) {
                menuFragment.setSelectedItem(R.id.menu_main_library);
            } else if (fragment instanceof TransfersFragment) {
                menuFragment.setSelectedItem(R.id.menu_main_transfers);
            } else if (fragment instanceof BrowsePeersFragment ||
                       fragment instanceof BrowsePeersDisabledFragment) {
                menuFragment.setSelectedItem(R.id.menu_main_peers);
            } else if (fragment instanceof AboutFragment) {
                menuFragment.setSelectedItem(R.id.menu_main_about);
            }
        }
    }

    private void showTransfers() {
        if (!(getCurrentFragment() instanceof TransfersFragment)) {
            switchFragment(R.id.menu_main_transfers);
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
                            MainActivity.this.startActivity(i.setClass(MainActivity.this, MainActivity.class));
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

    private void handleSendAction(Intent intent) {
        String action = intent.getAction();
        if (action.equals(Intent.ACTION_SEND)) {
            handleSendSingleFile(intent);
        } else if (action.equals(Intent.ACTION_SEND_MULTIPLE)) {
            handleSendMultipleFiles(intent);
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

    private void handleSendMultipleFiles(Intent intent) {
        ArrayList<Uri> fileUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        if (fileUris != null) {
            for (Uri uri : fileUris) {
                shareFileByUri(uri);
            }
            UIUtils.showLongMessage(this, getString(R.string.n_files_shared, fileUris.size()));
        }
    }

    private void showShareIndication() {
        ShareIndicationDialog dlg = new ShareIndicationDialog();
        dlg.show(getSupportFragmentManager());
    }

    private void startWizardActivity() {
        Intent i = new Intent(this, WizardActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
    }

    private void mainResume() {
        syncSlideMenu();

        if (firstTime) {
            firstTime = false;
            Engine.instance().startServices(); // it's necessary for the first time after wizard
        }

        SoftwareUpdater.instance().checkForUpdate(this);
    }
}