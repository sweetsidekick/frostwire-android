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

import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.frostwire.android.R;
import com.frostwire.android.core.ConfigurationManager;
import com.frostwire.android.core.Constants;
import com.frostwire.android.core.DesktopUploadRequest;
import com.frostwire.android.core.DesktopUploadRequestStatus;
import com.frostwire.android.gui.PeerManager;
import com.frostwire.android.gui.SoftwareUpdater;
import com.frostwire.android.gui.fragments.BrowsePeerFragment;
import com.frostwire.android.gui.fragments.BrowsePeersFragment;
import com.frostwire.android.gui.fragments.SearchFragment;
import com.frostwire.android.gui.fragments.TransfersFragment;
import com.frostwire.android.gui.services.DesktopUploadManager;
import com.frostwire.android.gui.services.Engine;
import com.frostwire.android.gui.transfers.TransferManager;
import com.frostwire.android.gui.util.UIUtils;
import com.frostwire.android.gui.views.AbstractActivity;
import com.frostwire.android.gui.views.DesktopUploadRequestDialog;
import com.frostwire.android.gui.views.DesktopUploadRequestDialogResult;
import com.frostwire.android.gui.views.SwipeyTabs;
import com.frostwire.android.gui.views.SwipeyTabs.SwipeyTabsAdapter;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public class MainActivity extends AbstractActivity {

    private static final String TAG = "FW.MainActivity";

    @SuppressWarnings("unused")
    private static final int TAB_LIBRARY_INDEX = 0;
    private static final int TAB_SEARCH_INDEX = 1;
    private static final int TAB_TRANSFERS_INDEX = 2;
    @SuppressWarnings("unused")
    private static final int TAB_PEERS_INDEX = 3;

    private static final String CURRENT_TAB_SAVE_INSTANCE_KEY = "current_tab";
    private static final String DUR_TOKEN_SAVE_INSTANCE_KEY = "dur_token";

    private SwipeyTabs swipeyTabs;
    private ViewPager viewPager;
    private TabsAdapter tabsAdapter;

    // not sure about this variable, quick solution for now
    private String durToken;

    public MainActivity() {
        super(R.layout.activity_main, false, 1);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            trackDialog(UIUtils.showYesNoDialog(this, R.string.are_you_sure_you_wanna_leave, R.string.minimize_frostwire, new OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    moveTaskToBack(true);
                }
            }));
        } else if (keyCode == KeyEvent.KEYCODE_SEARCH) {
            viewPager.setCurrentItem(TAB_SEARCH_INDEX);
        } else {
            return false;
        }

        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        swipeyTabs = findView(R.id.swipeytabs);
        viewPager = findView(R.id.pager);

        tabsAdapter = new TabsAdapter(this, viewPager);

        Bundle browseBundle = new Bundle();
        browseBundle.putByteArray(Constants.EXTRA_PEER_UUID, PeerManager.instance().getLocalPeer().getUUID());
        tabsAdapter.addTab(R.layout.view_tab_indicator_library, BrowsePeerFragment.class, browseBundle);

        tabsAdapter.addTab(R.layout.view_tab_indicator_search, SearchFragment.class, null);
        tabsAdapter.addTab(R.layout.view_tab_indicator_transfers, TransfersFragment.class, null);
        tabsAdapter.addTab(R.layout.view_tab_indicator_peers, BrowsePeersFragment.class, null);

        viewPager.setAdapter(tabsAdapter);
        swipeyTabs.setAdapter(tabsAdapter);
        viewPager.setOnPageChangeListener(swipeyTabs);
        viewPager.setCurrentItem(TAB_SEARCH_INDEX);

        if (savedInstanceState != null) {
            viewPager.setCurrentItem(savedInstanceState.getInt(CURRENT_TAB_SAVE_INSTANCE_KEY));
            durToken = savedInstanceState.getString(DUR_TOKEN_SAVE_INSTANCE_KEY);
        }

        onNewIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        String action = intent.getAction();

        if (action != null && action.equals(Constants.ACTION_SHOW_TRANSFERS)) {
            viewPager.setCurrentItem(TAB_TRANSFERS_INDEX);
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

        if (intent.hasExtra(Constants.EXTRA_DOWNLOAD_COMPLETE_NOTIFICATION)) {
            viewPager.setCurrentItem(TAB_TRANSFERS_INDEX);
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

    @Override
    protected void onResume() {
        super.onResume();

        Engine.instance().startServices(); // it's necessary for the first time after wizard
        SoftwareUpdater.instance().checkForUpdate(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(CURRENT_TAB_SAVE_INSTANCE_KEY, viewPager.getCurrentItem());
        outState.putString(DUR_TOKEN_SAVE_INSTANCE_KEY, durToken);
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

    private class TabsAdapter extends FragmentPagerAdapter implements SwipeyTabsAdapter {

        private final Context context;
        private final ViewPager viewPager;
        private final ArrayList<TabInfo> tabs = new ArrayList<TabInfo>();

        public TabsAdapter(FragmentActivity activity, ViewPager pager) {
            super(activity.getSupportFragmentManager());
            this.context = activity;
            this.viewPager = pager;
            this.viewPager.setAdapter(this);
            this.viewPager.setOnPageChangeListener(this);
        }

        public void addTab(int indicatorId, Class<?> clazz, Bundle args) {
            TabInfo info = new TabInfo(indicatorId, clazz, args);
            tabs.add(info);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return tabs.size();
        }

        @Override
        public Fragment getItem(int position) {
            TabInfo info = tabs.get(position);

            Fragment f = null;
            try {
                f = (Fragment) info.clazz.newInstance();
                f.setArguments(info.args);
            } catch (Exception e) {
                e.printStackTrace();
            }

            return f;
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            hideSoftKeys();
        }

        @Override
        public void onPageScrollStateChanged(int state) {
        }

        private void hideSoftKeys() {
            InputMethodManager manager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            manager.hideSoftInputFromWindow(viewPager.getApplicationWindowToken(), 0);
        }

        final class TabInfo {
            private final int indicatorId;
            private final Class<?> clazz;
            private final Bundle args;

            TabInfo(int indicatorId, Class<?> clazz, Bundle args) {
                this.indicatorId = indicatorId;
                this.clazz = clazz;
                this.args = args;
            }
        }

        @Override
        public View getTab(final int position, SwipeyTabs root) {
            View view = getLayoutInflater().inflate(tabs.get(position).indicatorId, null);

            view.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    viewPager.setCurrentItem(position);
                }
            });

            return view;
        }
    }
}
