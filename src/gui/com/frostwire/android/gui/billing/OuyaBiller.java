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

package com.frostwire.android.gui.billing;

import com.frostwire.android.R;
import com.frostwire.android.gui.util.UIUtils;

import tv.ouya.console.api.OuyaFacade;
import tv.ouya.console.api.OuyaResponseListener;
import tv.ouya.console.api.Purchasable;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

/**
 * @author gubatron
 * @author aldenml
 *
 */
final class OuyaBiller implements Biller, OuyaResponseListener<String> {

    private static final String DEVELOPER_ID = "000";
    
    private final Context context;

    public OuyaBiller(Activity activity) {
        OuyaFacade.getInstance().init(activity, DEVELOPER_ID);
        this.context = activity;
    }

    @Override
    public boolean isInAppBillingSupported() {
        return OuyaFacade.getInstance().isRunningOnOUYAHardware() && OuyaFacade.getInstance().isInitialized();
    }

    @Override
    public void onDestroy() {
        OuyaFacade.getInstance().shutdown();
    }

    @Override
    public void requestPurchase(String sku) {
        OuyaFacade.getInstance().requestPurchase(new Purchasable(sku), this);
    }

    @Override
    public void onCancel() {
    }

    @Override
    public void onFailure(int arg0, String arg1, Bundle arg2) {
    }

    @Override
    public void onSuccess(String arg0) {
        UIUtils.showLongMessage(context, R.string.donation_thanks);
    }
}
