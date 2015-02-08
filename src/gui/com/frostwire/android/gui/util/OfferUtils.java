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
import com.appia.sdk.BannerAd;
import com.appia.sdk.BannerAdSize;
import com.appia.sdk.InterstitialSize;
import com.frostwire.android.core.ConfigurationManager;
import com.frostwire.android.core.Constants;
import com.frostwire.logging.Logger;
import com.frostwire.uxstats.UXAction;
import com.frostwire.uxstats.UXStats;
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

    public static boolean isAppiaInterstitialEnabled() {
        ConfigurationManager config = null;
        boolean isAppiaInterstitialEnabled = false;
        try {
            config = ConfigurationManager.instance();
            isAppiaInterstitialEnabled = (config.getBoolean(Constants.PREF_KEY_GUI_SUPPORT_FROSTWIRE) && config.getBoolean(Constants.PREF_KEY_GUI_USE_APPIA_INTERSTITIAL)) && !OSUtils.isAmazonDistribution();
        } catch (Throwable t) {
        }
        return isAppiaInterstitialEnabled;
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
    public static void showInterstitial(Activity callerActivity, boolean mobileCoreStarted, boolean appiaStarted, CallbackResponse callbackResponse) {
        if (isMobileCoreEnabled() && mobileCoreStarted && MobileCore.isInterstitialReady()) {
            try {
                MobileCore.showInterstitial(callerActivity, callbackResponse);
                UXStats.instance().log(UXAction.MISC_INTERSTITIAL_SHOW);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        } /*
         else if (isAppiaInterstitialEnabled() && appiaStarted) {
            try {
                final Appia appia = Appia.getAppia(callerActivity);
                BannerAdSize size = OSUtils.isScreenOrientationPortrait(callerActivity) ?
                        BannerAdSize.SIZE_320x480 : BannerAdSize.SIZE_480x320;

                appia.displayInterstitial(callerActivity, null, size);
                if (callbackResponse != null) {
                    callbackResponse.onConfirmation(null);
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        */
        else {
            if (callbackResponse != null) {
                callbackResponse.onConfirmation(null);
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
        if (isfreeAppsEnabled() && isMobileCoreEnabled() && MobileCore.isDirectToMarketReady()) {
            try {
                LOG.debug("onFreeAppsClick");
                MobileCore.directToMarket((Activity) context);
                /**
                Appia appia = Appia.getAppia();
                appia.cacheAppWall(context);
                appia.displayWall(context, WallDisplayType.FULL_SCREEN);
                 */
            } catch (Throwable t) {
                LOG.error("can't show app wall", t);
                t.printStackTrace();
            }
        }
    }
}