package com.frostwire.android.gui.activities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import com.frostwire.android.R;
import com.frostwire.android.gui.PeerManager;
import com.frostwire.android.gui.fragments.AboutFragment;
import com.frostwire.android.gui.fragments.BrowsePeerFragment;
import com.frostwire.android.gui.fragments.BrowsePeersFragment;
import com.frostwire.android.gui.fragments.SearchFragment;
import com.frostwire.android.gui.fragments.SlideMenuFragment;
import com.frostwire.android.gui.fragments.TransfersFragment;
import com.frostwire.android.gui.views.AbstractSlidingActivity;
import com.slidingmenu.lib.SlidingMenu;

public class MainActivity2 extends AbstractSlidingActivity {

    private static final Logger LOG = LoggerFactory.getLogger(MainActivity2.class);

    private static final String CURRENT_FRAGMENT_SAVE_INSTANCE_KEY = "current_fragment";
    private static final String DUR_TOKEN_SAVE_INSTANCE_KEY = "dur_token";

    private Fragment currentFragment;

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

        // set the Above View
        if (savedInstanceState != null)
            currentFragment = getSupportFragmentManager().getFragment(savedInstanceState, CURRENT_FRAGMENT_SAVE_INSTANCE_KEY);
        if (currentFragment == null)
            currentFragment = search;

        // set the Above View
        getSupportFragmentManager().beginTransaction().replace(R.id.activity_main_content_frame, currentFragment).commit();

        // set the Behind View
        setBehindContentView(R.layout.slidemenu_frame);
        getSupportFragmentManager().beginTransaction().replace(R.id.slidemenu_frame, new SlideMenuFragment()).commit();

        // customize the SlidingMenu
        getSlidingMenu().setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        getSupportFragmentManager().putFragment(outState, CURRENT_FRAGMENT_SAVE_INSTANCE_KEY, currentFragment);
    }

    private void switchContent(Fragment fragment) {
        currentFragment = fragment;
        getSupportFragmentManager().beginTransaction().replace(R.id.activity_main_content_frame, fragment).addToBackStack(null).commit();
        getSupportFragmentManager().executePendingTransactions();
        getSlidingMenu().showContent();
    }

    private void setupFragments() {
        search = new SearchFragment();
        library = new BrowsePeerFragment();
        transfers = new TransfersFragment();
        peers = new BrowsePeersFragment();
        about = new AboutFragment();

        library.setPeer(PeerManager.instance().getLocalPeer());
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

    public void switchFragment(int itemId) {
        Fragment fragment = getFragmentByMenuId(itemId);
        if (fragment != null) {
            switchContent(fragment);
        }
    }
}
