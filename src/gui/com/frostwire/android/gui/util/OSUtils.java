package com.frostwire.android.gui.util;

public final class OSUtils {
    
    public static boolean isKindleFire() {
        return android.os.Build.MANUFACTURER.equals("Amazon")
                && (android.os.Build.MODEL.equals("Kindle Fire")
                    || android.os.Build.MODEL.startsWith("KF"));
    }
}
