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
import java.util.Stack;

import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
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
import com.frostwire.android.gui.fragments.BrowsePeersFragment;
import com.frostwire.android.gui.fragments.MainFragment;
import com.frostwire.android.gui.fragments.SearchFragment;
import com.frostwire.android.gui.fragments.TransfersFragment;
import com.frostwire.android.gui.services.DesktopUploadManager;
import com.frostwire.android.gui.services.Engine;
import com.frostwire.android.gui.transfers.TransferManager;
import com.frostwire.android.gui.util.UIUtils;
import com.frostwire.android.gui.views.AbstractActivity;
import com.frostwire.android.gui.views.DesktopUploadRequestDialog;
import com.frostwire.android.gui.views.DesktopUploadRequestDialogResult;
import com.frostwire.android.gui.views.Refreshable;
import com.frostwire.android.gui.views.SlideMenu;
import com.frostwire.android.gui.views.SlideMenuInterface;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public class MainActivity extends AbstractActivity implements SlideMenuInterface.OnSlideMenuItemClickListener {

    private static final String TAG = "FW.MainActivity";

    private static final String CURRENT_FRAGMENT_SAVE_INSTANCE_KEY = "current_fragment";
    private static final String DUR_TOKEN_SAVE_INSTANCE_KEY = "dur_token";

    private static boolean firstTime = true;

    private SlideMenu menu;
    private int menuSelectedItemId;

    // not sure about this variable, quick solution for now
    private String durToken;

    private SearchFragment search;
    private BrowsePeerFragment library;
    private TransfersFragment transfers;
    private BrowsePeersFragment peers;
    private AboutFragment about;

    private Stack<Integer> lastMenuIdStack  = new Stack<Integer>();

    public MainActivity() {
        super(R.layout.activity_main, false, 2);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (menu.isMenuShown()) {
                menu.hide();
            } else if (!lastMenuIdStack.isEmpty()) {
                onSlideMenuItemClick(lastMenuIdStack.pop());
            } else {
                trackDialog(UIUtils.showYesNoDialog(this, R.string.are_you_sure_you_wanna_leave, R.string.minimize_frostwire, new OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        moveTaskToBack(true);
                    }
                }));
            }
        } else if (keyCode == KeyEvent.KEYCODE_SEARCH) {
            showFragment(search, R.id.menu_main_search);
            if (menu.isMenuShown()) {
                menu.hide();
            }
        } else if (keyCode == KeyEvent.KEYCODE_MENU) {
            if (menu.isMenuShown()) {
                menu.hide();
            } else {
                menu.show();
            }
        } else {
            return false;
        }

        return true;
    }

    @Override
    public void onSlideMenuItemClick(int itemId) {
        switch (itemId) {
        case R.id.menu_main_search:
            showFragment(search, itemId);
            break;
        case R.id.menu_main_library:
            showFragment(library, itemId);
            break;
        case R.id.menu_main_transfers:
            showFragment(transfers, itemId);
            break;
        case R.id.menu_main_peers:
            showFragment(peers, itemId);
            break;
        case R.id.menu_main_preferences:
            showPreferences();
            break;
        case R.id.menu_main_about:
            showFragment(about, itemId);
            break;
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        menu = findView(R.id.activity_main_menu);
        menu.init(this, R.menu.main, this, 400);

        /*
        // set optional header image
        slidemenu.setHeaderImage(R.drawable.ic_launcher);

        // this demonstrates how to dynamically add menu items
        SlideMenuItem item = new SlideMenuItem();
        item.id = MYITEMID;
        item.icon = getResources().getDrawable(R.drawable.ic_launcher);
        item.label = "Dynamically added item";
        slidemenu.addMenuItem(item);
        */

        // connect the fallback button in case there is no ActionBar
        ImageButton buttonMainMenu = findView(R.id.activity_main_button_menu);
        buttonMainMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                menu.show();
            }
        });

        FragmentManager manager = getSupportFragmentManager();
        search = (SearchFragment) manager.findFragmentById(R.id.activity_main_fragment_search);
        library = (BrowsePeerFragment) manager.findFragmentById(R.id.activity_main_fragment_library);
        transfers = (TransfersFragment) manager.findFragmentById(R.id.activity_main_fragment_transfers);
        peers = (BrowsePeersFragment) manager.findFragmentById(R.id.activity_main_fragment_peers);
        about = (AboutFragment) manager.findFragmentById(R.id.activity_main_fragment_about);

        library.setPeer(PeerManager.instance().getLocalPeer());

        showFragment(search, R.id.menu_main_search);

        if (savedInstanceState != null) {
            onSlideMenuItemClick(savedInstanceState.getInt(CURRENT_FRAGMENT_SAVE_INSTANCE_KEY));
            durToken = savedInstanceState.getString(DUR_TOKEN_SAVE_INSTANCE_KEY);
        }

        addRefreshable((Refreshable) findView(R.id.activity_main_player_notifier));

        onNewIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        String action = intent.getAction();

        if (action != null && action.equals(Constants.ACTION_SHOW_TRANSFERS)) {
            onResumeFragments();
            showFragment(transfers, R.id.menu_main_transfers);
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
            TransferManager.instance().download(intent);
        } else if ((action != null && action.equals(Constants.ACTION_DESKTOP_UPLOAD_REQUEST)) || durToken != null) {
            handleDesktopUploadRequest(intent);
        }
        // When another application wants to "Share" a file and has chosen FrostWire to do so.
        // We make the file "Shared" so it's visible for other FrostWire devices on the local network.
        else if (action != null && (action.equals(Intent.ACTION_SEND) || action.equals(Intent.ACTION_SEND_MULTIPLE))) {
            handleSendAction(intent);
        }

        if (intent.hasExtra(Constants.EXTRA_DOWNLOAD_COMPLETE_NOTIFICATION)) {
            onResumeFragments();
            showFragment(transfers, R.id.menu_main_transfers);
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
                Log.e(TAG, "Error handling download complete notification", e);
            }
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
        fileDescriptor.shared = true;

        Librarian.instance().updateSharedStates(fileDescriptor.fileType, Arrays.asList(fileDescriptor));
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

    @Override
    protected void onResume() {
        super.onResume();

        if (firstTime) {
            firstTime = false;
            Engine.instance().startServices(); // it's necessary for the first time after wizard
        }

        SoftwareUpdater.instance().checkForUpdate(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(CURRENT_FRAGMENT_SAVE_INSTANCE_KEY, menuSelectedItemId);
        outState.putString(DUR_TOKEN_SAVE_INSTANCE_KEY, durToken);
    }

    @Override
    protected void onPause() {
        super.onPause();
        search.dismissDialogs();
        library.dismissDialogs();
        peers.dismissDialogs();
    }

    private void showFragment(Fragment fragment, int menuId) {
        lastMenuIdStack.push(menuSelectedItemId);
        menuSelectedItemId = menuId;
        menu.setSelectedItem(menuSelectedItemId);

        FragmentManager manager = getSupportFragmentManager();

        FragmentTransaction transaction = manager.beginTransaction();

        transaction.hide(search);
        transaction.hide(library);
        transaction.hide(transfers);
        transaction.hide(peers);
        transaction.hide(about);

        transaction.show(fragment);
        transaction.commit();

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
    }

    private void showPreferences() {
        Context context = this;
        Intent i = new Intent(context, PreferencesActivity.class);
        context.startActivity(i);
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

    public void showMyFiles() {
        showFragment(library, R.id.menu_main_library);
    }
}