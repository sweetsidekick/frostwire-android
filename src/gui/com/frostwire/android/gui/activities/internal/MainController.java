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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.Fragment;

import com.appia.sdk.Appia;
import com.appia.sdk.Appia.WallDisplayType;
import com.frostwire.android.gui.activities.MainActivity;
import com.frostwire.android.gui.activities.PreferencesActivity;

/**
 * 
 * @author gubatron
 * @author aldenml
 *
 */
public final class MainController {

    private static final Logger LOG = LoggerFactory.getLogger(MainController.class);

    private final MainActivity activity;

    public MainController(MainActivity activity) {
        this.activity = activity;
    }

    public MainActivity getActivity() {
        return activity;
    }

    public void closeSlideMenu() {
        activity.closeSlideMenu();
    }

    public void switchFragment(int itemId) {
        Fragment fragment = activity.getFragmentByMenuId(itemId);
        if (fragment != null) {
            activity.switchContent(fragment);
        }
    }

    public void showPreferences() {
        Intent i = new Intent(activity, PreferencesActivity.class);
        activity.startActivity(i);
    }

    /**
     * Will try to launch the app, if it cannot find the launch intent, it'll take the user to the Android market.
     */
    public void launchFrostWireTV() {
        Intent intent = null;
        try {
            intent = activity.getApplicationContext().getPackageManager().getLaunchIntentForPackage("com.frostwire.android.tv");

            //on the nexus it wasn't throwing the NameNotFoundException, it was just returning null
            if (intent == null) {
                throw new NullPointerException();
            }
        } catch (Throwable t) {
            intent = new Intent();
            intent.setData(Uri.parse("market://details?id=com.frostwire.android.tv"));
        }
        activity.startActivity(intent);
    }

    public void showFreeApps() {
        try {
            Appia appia = Appia.getAppia();
            appia.cacheAppWall(activity);
            appia.displayWall(activity, WallDisplayType.FULL_SCREEN);
        } catch (Throwable e) {
            LOG.error("Can't show app wall", e);
        }
    }
}
