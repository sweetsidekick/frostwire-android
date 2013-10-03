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

import android.content.Context;

import com.amazon.inapp.purchasing.GetUserIdResponse;
import com.amazon.inapp.purchasing.ItemDataResponse;
import com.amazon.inapp.purchasing.PurchaseResponse;
import com.amazon.inapp.purchasing.PurchaseUpdatesResponse;
import com.amazon.inapp.purchasing.PurchasingManager;
import com.amazon.inapp.purchasing.PurchasingObserver;
import com.frostwire.android.R;
import com.frostwire.android.gui.util.UIUtils;

/**
 * @author gubatron
 * @author aldenml
 *
 */
final class KindleBiller extends PurchasingObserver implements Biller {

    private final Context context;

    public KindleBiller(Context context) {
        super(context);
        this.context = context;
        PurchasingManager.registerObserver(this);
    }

    @Override
    public boolean isInAppBillingSupported() {
        return true;
    }

    @Override
    public void onDestroy() {
    }

    @Override
    public void requestPurchase(String sku) {
        PurchasingManager.initiatePurchaseRequest(sku);
    }

    @Override
    public void onGetUserIdResponse(GetUserIdResponse arg0) {
    }

    @Override
    public void onItemDataResponse(ItemDataResponse arg0) {
    }

    @Override
    public void onPurchaseResponse(PurchaseResponse response) {
        if (response != null && response.getPurchaseRequestStatus() == PurchaseResponse.PurchaseRequestStatus.SUCCESSFUL) {
            UIUtils.showLongMessage(context, R.string.donation_thanks);
        }
    }

    @Override
    public void onPurchaseUpdatesResponse(PurchaseUpdatesResponse arg0) {
    }

    @Override
    public void onSdkAvailable(boolean arg0) {
    }
}