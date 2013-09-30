package com.frostwire.android.gui.billing;

import com.frostwire.android.gui.util.OSUtils;

import android.app.Activity;

public class BillerFactory {
    public static Biller getInstance(Activity activity) {
        Biller billy = null;
        
        if (OSUtils.isKindleFire()) {
            billy = new KindleBiller(activity);
        } else if (OSUtils.isOUYA()) {
            billy = new OuyaBiller(activity);
        } else {
            billy = new GooglePlayBiller(activity);
        }
        
        return billy;
    }
    
    public static DonationSkus getDonationSkus() {
        if (OSUtils.isOUYA()) {
            return new DonationSkus.OuyaDonationSkus();
        } else {
            return new DonationSkus.DefaultDonationSkus();
        }
    }
}