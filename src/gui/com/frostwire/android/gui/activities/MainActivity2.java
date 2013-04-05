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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Canvas;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;

import com.frostwire.android.R;
import com.frostwire.android.gui.PeerManager;
import com.frostwire.android.gui.fragments.AboutFragment;
import com.frostwire.android.gui.fragments.BrowsePeerFragment;
import com.frostwire.android.gui.fragments.BrowsePeersFragment;
import com.frostwire.android.gui.fragments.SearchFragment;
import com.frostwire.android.gui.fragments.SlideMenuFragment;
import com.frostwire.android.gui.fragments.TransfersFragment;
import com.frostwire.android.gui.util.UIUtils;
import com.frostwire.android.gui.views.AbstractSlidingActivity;
import com.slidingmenu.lib.SlidingMenu;
import com.slidingmenu.lib.SlidingMenu.CanvasTransformer;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public class MainActivity2 extends AbstractSlidingActivity {

    private static final Logger LOG = LoggerFactory.getLogger(MainActivity2.class);

    private static final String FRAGMENT_STACK_TAG = "fragment_stack";
    private static final String CURRENT_FRAGMENT_KEY = "current_fragment";
    private static final String DUR_TOKEN_KEY = "dur_token";

    private SlideMenuFragment menuFragment;

    private SearchFragment search;
    private BrowsePeerFragment library;
    private TransfersFragment transfers;
    private BrowsePeersFragment peers;
    private AboutFragment about;

    public MainActivity2() {
        super(R.layout.activity_main2, false, 2);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupFragments();

        setupInitialFragment(savedInstanceState);

        setupSlideMenu();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        saveLastFragment(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();

        syncSlideMenu();
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            super.onBackPressed();
        } else {
            handleLastBackPressed();
        }

        syncSlideMenu();
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
                moveTaskToBack(true);
            }
        }));
    }

    private void switchContent(Fragment fragment) {
        getSupportFragmentManager().beginTransaction().replace(R.id.activity_main_content_frame, fragment, FRAGMENT_STACK_TAG).addToBackStack(null).commit();
        getSupportFragmentManager().executePendingTransactions();
        getSlidingMenu().showContent();

        syncSlideMenu();
    }

    private void setupFragments() {
        menuFragment = new SlideMenuFragment();

        search = new SearchFragment();
        library = new BrowsePeerFragment();
        transfers = new TransfersFragment();
        peers = new BrowsePeersFragment();
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
    }

    private void setupSlideMenu() {
        setBehindContentView(R.layout.slidemenu_frame);
        getSupportFragmentManager().beginTransaction().replace(R.id.slidemenu_frame, menuFragment).commit();

        SlidingMenu menu = getSlidingMenu();
        menu.setShadowWidthRes(R.dimen.shadow_width);
        menu.setShadowDrawable(R.drawable.mainmenu_shadow);
        menu.setBehindOffsetRes(R.dimen.slidingmenu_offset);
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
            return peers;
        case R.id.menu_main_about:
            return about;
        default:
            return null;
        }
    }

    private void syncSlideMenu() {
        Fragment fragment = getCurrentFragment();

        if (fragment instanceof SearchFragment) {
            menuFragment.setSelectedItem(R.id.menu_main_search);
        } else if (fragment instanceof BrowsePeerFragment) {
            menuFragment.setSelectedItem(R.id.menu_main_library);
        } else if (fragment instanceof TransfersFragment) {
            menuFragment.setSelectedItem(R.id.menu_main_transfers);
        } else if (fragment instanceof BrowsePeersFragment) {
            menuFragment.setSelectedItem(R.id.menu_main_peers);
        } else if (fragment instanceof AboutFragment) {
            menuFragment.setSelectedItem(R.id.menu_main_about);
        }
    }
}
