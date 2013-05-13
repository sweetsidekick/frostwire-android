package com.frostwire.android.gui;

import java.util.logging.Logger;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;

import com.frostwire.android.R;
import com.frostwire.android.gui.util.UIUtils;
import com.frostwire.android.market.BillingService;
import com.frostwire.android.market.BillingService.RequestPurchase;
import com.frostwire.android.market.BillingService.RestoreTransactions;
import com.frostwire.android.market.Consts;
import com.frostwire.android.market.Consts.PurchaseState;
import com.frostwire.android.market.Consts.ResponseCode;
import com.frostwire.android.market.PurchaseObserver;
import com.frostwire.android.market.ResponseHandler;

public class Biller extends PurchaseObserver {
    private static final Logger LOG = Logger.getLogger(Biller.class.getName());
    
    private boolean inAppBillingSupported;
    private final BillingService billingService;
    private final Context context;

    public Biller(Activity activity) {
        super(activity, new Handler());
        this.context = activity;
        billingService = new BillingService();
        billingService.setContext(context);

        updateBillingSupportStatus(false);

        // Check if billing is supported.
        ResponseHandler.register(this);

        //        if (!billingService.checkBillingSupported()) {
        //            showDialog(DIALOG_CANNOT_CONNECT_ID);
        //        }
        updateBillingSupportStatus(billingService.checkBillingSupported(Consts.ITEM_TYPE_INAPP));
    }

    public void updateBillingSupportStatus(boolean supported) {
        inAppBillingSupported = supported;
    }

    public boolean isInAppBillingSupported() {
        return inAppBillingSupported;
    }

    public BillingService getBillingService() {
        return billingService;
    }

    @Override
    public void onBillingSupported(boolean supported, String type) {
        LOG.info("Market In-app billing support: " + supported);
        if (type == null || type.equals(Consts.ITEM_TYPE_INAPP)) {
            updateBillingSupportStatus(supported);
        }
    }

    @Override
    public void onPurchaseStateChange(PurchaseState purchaseState, String itemId, int quantity, long purchaseTime, String developerPayload) {
        LOG.info("onPurchaseStateChange() itemId: " + itemId + " " + purchaseState);

        if (purchaseState == PurchaseState.PURCHASED) {
            UIUtils.showLongMessage(context, R.string.donation_thanks);
        }
    }

    @Override
    public void onRequestPurchaseResponse(RequestPurchase request, ResponseCode responseCode) {
        LOG.info("onRequestPurchaseResponse" + request.mProductId + ": " + responseCode);

        if (responseCode == ResponseCode.RESULT_OK) {
            LOG.info("donation request was successfully sent to server");
        } else if (responseCode == ResponseCode.RESULT_USER_CANCELED) {
            LOG.info("user canceled donation");
        } else {
            LOG.info("donation failed");
            LOG.info(request.mProductId + " request donation returned " + responseCode);
        }
    }

    @Override
    public void onRestoreTransactionsResponse(RestoreTransactions request, ResponseCode responseCode) {
        LOG.info("onRestoreTransactionsResponse: " + responseCode);
    }

    public void startActivity(Intent i) {
        context.startActivity(i);
    }

    public void onDestroy() {
        if (getBillingService() != null) {
            getBillingService().unbind();
        }
    }
}