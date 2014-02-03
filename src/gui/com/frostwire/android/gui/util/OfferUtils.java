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

package com.frostwire.android.gui.util;

import android.content.Context;

import com.appia.sdk.Appia;
import com.appia.sdk.Appia.WallDisplayType;
import com.frostwire.android.core.ConfigurationManager;
import com.frostwire.android.core.Constants;
import com.frostwire.logging.Logger;
import com.offercast.android.sdk.OffercastSDK;

public class OfferUtils {

    private static final Logger LOG = Logger.getLogger(OfferUtils.class);

    public static boolean isfreeAppsEnabled() {
        ConfigurationManager config = null;
        boolean isFreeAppsEnabled = false;
        try {
            config = ConfigurationManager.instance();
            isFreeAppsEnabled = (config.getBoolean(Constants.PREF_KEY_GUI_SUPPORT_FROSTWIRE) && config.getBoolean(Constants.PREF_KEY_GUI_INITIALIZE_APPIA)) && !OSUtils.isAmazonDistribution() && !OSUtils.isOUYA();
            //config.getBoolean(Constants.PREF_KEY_GUI_SHOW_FREE_APPS_MENU_ITEM);
        } catch (Throwable t) {
        }
        return isFreeAppsEnabled;
    }

    public static void startOffercast(final Context context) throws Exception {
        if (!OSUtils.isAmazonDistribution()) {
            try {
                OffercastSDK offercast = OffercastSDK.getInstance(context);
                offercast.authorize();
                LOG.info("Offercast started.");
            } catch (Exception e) {
                LOG.error("Offercast could not start.", e);
            }
        }
    }

    public static void onFreeAppsClick(Context context) {
        if (isfreeAppsEnabled()) {
            try {
                Appia appia = Appia.getAppia();
                appia.cacheAppWall(context);
                appia.displayWall(context, WallDisplayType.FULL_SCREEN);
            } catch (Throwable t) {
                LOG.error("can't show app wall", t);
                t.printStackTrace();
            }
        }
    }
}