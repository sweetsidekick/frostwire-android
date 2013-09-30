package com.frostwire.android.gui.billing;

import java.util.logging.Logger;

import android.app.Activity;
import android.content.Context;
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

class GooglePlayBiller extends PurchaseObserver implements Biller {
    private static final Logger LOG = Logger.getLogger(GooglePlayBiller.class.getName());
    
    private boolean inAppBillingSupported = false;
    private final BillingService billingService;
    private final Context context;

    public GooglePlayBiller(Activity activity) {
        super(activity, new Handler());
        this.context = activity;
        billingService = new BillingService();
        billingService.setContext(context.getApplicationContext());
        // Check if billing is supported.
        ResponseHandler.register(this);

        //        if (!billingService.checkBillingSupported()) {
        //            showDialog(DIALOG_CANNOT_CONNECT_ID);
        //        }
        inAppBillingSupported = billingService.checkBillingSupported(Consts.ITEM_TYPE_INAPP);
    }

    /* (non-Javadoc)
     * @see com.frostwire.android.gui.Biller#isInAppBillingSupported()
     */
    @Override
    public boolean isInAppBillingSupported() {
        return inAppBillingSupported;
    }

    @Override
    public void onBillingSupported(boolean supported, String type) {
        LOG.info("Market In-app billing support: " + supported);
        if (type == null || type.equals(Consts.ITEM_TYPE_INAPP)) {
            inAppBillingSupported = supported;
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

    /* (non-Javadoc)
     * @see com.frostwire.android.gui.Biller#onDestroy()
     */
    @Override
    public void onDestroy() {
        if (billingService != null) {
            ResponseHandler.unregister(this);
            billingService.unbind();
        }
    }

    /* (non-Javadoc)
     * @see com.frostwire.android.gui.Biller#requestPurchase(java.lang.String)
     */
    @Override
    public void requestPurchase(String sku) {
        inAppBillingSupported = billingService.requestPurchase(sku, Consts.ITEM_TYPE_INAPP, null);
    }
}