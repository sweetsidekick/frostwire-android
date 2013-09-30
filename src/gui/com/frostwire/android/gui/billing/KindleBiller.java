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
        if (response!= null && response.getPurchaseRequestStatus() == PurchaseResponse.PurchaseRequestStatus.SUCCESSFUL) {
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