/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2015, FrostWire(TM). All rights reserved.
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

import android.app.Activity;
import android.content.Context;
import com.appia.sdk.Appia;
import com.appia.sdk.Appia.WallDisplayType;
import com.frostwire.android.core.ConfigurationManager;
import com.frostwire.android.core.Constants;
import com.frostwire.logging.Logger;
import com.ironsource.mobilcore.CallbackResponse;
import com.ironsource.mobilcore.MobileCore;

public class OfferUtils {

    private static final Logger LOG = Logger.getLogger(OfferUtils.class);
    public static boolean MOBILE_CORE_NATIVE_ADS_READY = false;

    /**
     * True if user has enabled support for frostwire, Appia is enabled and it's not an Amazon distribution build. 
     * @return
     */
    public static boolean isfreeAppsEnabled() {
        ConfigurationManager config = null;
        boolean isFreeAppsEnabled = false;
        try {
            config = ConfigurationManager.instance();
            isFreeAppsEnabled = (config.getBoolean(Constants.PREF_KEY_GUI_SUPPORT_FROSTWIRE) && config.getBoolean(Constants.PREF_KEY_GUI_INITIALIZE_APPIA)) && !OSUtils.isAmazonDistribution();
            //config.getBoolean(Constants.PREF_KEY_GUI_SHOW_FREE_APPS_MENU_ITEM);
        } catch (Throwable t) {
        }
        return isFreeAppsEnabled;
    }
    
    public static boolean isAppiaSearchEnabled() {
        ConfigurationManager config = null;
        boolean isAppiaSearchEnabled = false;
        try {
            config = ConfigurationManager.instance();
            isAppiaSearchEnabled = (config.getBoolean(Constants.PREF_KEY_GUI_SUPPORT_FROSTWIRE) && config.getBoolean(Constants.PREF_KEY_GUI_USE_APPIA_SEARCH)) && !OSUtils.isAmazonDistribution();
        } catch (Throwable t) {
        }
        return isAppiaSearchEnabled;
    }
    public static boolean isMobileCoreEnabled() {
        ConfigurationManager config = null;
        boolean isMobileCoreEnabled = false;
        try {
            config = ConfigurationManager.instance();
            isMobileCoreEnabled = (config.getBoolean(Constants.PREF_KEY_GUI_SUPPORT_FROSTWIRE) && config.getBoolean(Constants.PREF_KEY_GUI_USE_MOBILE_CORE)) && !OSUtils.isAmazonDistribution();
        }  catch (Throwable e) {
            e.printStackTrace();
        }
        return isMobileCoreEnabled;
    }

    /**
     * If mobileCore is active, it will show the interstitial, then perform the callback.
     * Otherwise, it will perform the callback logic.
     *
     * @param callerActivity
     * @param mobileCoreStarted
     * @param callbackResponse
     */
    public static void showInterstitial(Activity callerActivity, boolean mobileCoreStarted, CallbackResponse callbackResponse) {
        if (isMobileCoreEnabled() && mobileCoreStarted && MobileCore.isInterstitialReady()) {
            try {
                MobileCore.showInterstitial(callerActivity, callbackResponse);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        } else {
            if (callbackResponse != null) {
                callbackResponse.onConfirmation(null);
            }
        }
    }

    public static void showSticky(Activity callerActivity) {
        if (isMobileCoreEnabled() && MobileCore.isStickeeReady() && !MobileCore.isStickeeShowing()) {
            try {
                MobileCore.setStickeezPosition(MobileCore.EStickeezPosition.MIDDLE_RIGHT);
                MobileCore.showStickee(callerActivity);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    public static void startOffercastLockScreen(final Context context) throws Exception {
        if (!OSUtils.isAmazonDistribution()) {
            try {
                /*
                OffercastSDK offercast = OffercastSDK.getInstance(context);
                offercast.authorize();
                LOG.info("Offercast started.");
                */
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