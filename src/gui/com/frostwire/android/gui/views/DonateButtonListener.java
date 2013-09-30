package com.frostwire.android.gui.views;

import java.util.logging.Logger;

import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.view.View.OnClickListener;

import com.frostwire.android.gui.billing.Biller;

public final class DonateButtonListener implements OnClickListener {
    private static final Logger LOG = Logger.getLogger(DonateButtonListener.class.getName());
    private final String sku;
    private final String url;
    private final Biller biller;

    public DonateButtonListener(String sku, String url, Biller biller) {
        this.sku = sku;
        this.url = url;
        this.biller = biller;
    }

    @Override
    public void onClick(View v) {
        LOG.info("Donation sku: " + sku);

        if (biller.isInAppBillingSupported()) {
            // TODO: evaluate sending some value in the payload (last parameter)
            biller.requestPurchase(sku);
        } else {
            Intent i = new Intent("android.intent.action.VIEW", Uri.parse(url));
            v.getContext().startActivity(i);
        }
    }
}