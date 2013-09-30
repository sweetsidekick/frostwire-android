package com.frostwire.android.gui.billing;

import com.frostwire.android.R;
import com.frostwire.android.gui.util.UIUtils;

import tv.ouya.console.api.OuyaFacade;
import tv.ouya.console.api.OuyaResponseListener;
import tv.ouya.console.api.Purchasable;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

class OuyaBiller implements Biller, OuyaResponseListener<String> {
    
    private static final String DEVELOPER_ID = "000";
    private final Context context;
    
    public OuyaBiller(Activity activity) {
        OuyaFacade.getInstance().init(activity, DEVELOPER_ID);
        context = activity;
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
