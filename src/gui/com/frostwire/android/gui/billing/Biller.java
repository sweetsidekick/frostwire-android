package com.frostwire.android.gui.billing;

public interface Biller {
    public boolean isInAppBillingSupported();

    public void onDestroy();

    public void requestPurchase(String sku);
}