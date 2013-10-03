/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011 - 2013, FrostWire(TM). All rights reserved.
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


package com.frostwire.android.gui.util;

import tv.ouya.console.api.OuyaFacade;

public final class OSUtils {
    
    public static boolean isKindleFire() {
        return android.os.Build.MANUFACTURER.equals("Amazon")
                && (android.os.Build.MODEL.equals("Kindle Fire")
                    || android.os.Build.MODEL.startsWith("KF"));
    }
    
    public static boolean isOUYA() {
        return OuyaFacade.getInstance().isRunningOnOUYAHardware();
    }
    
    public static String getOSName() {
        String osName = "Android";
        if (OSUtils.isKindleFire()) {
            osName = "Kindle Fire";
        } else if (OSUtils.isOUYA()) {
            osName = "OUYA";
        }
        return osName;
    }
    
    public static String getOSVersionString() {
        return android.os.Build.VERSION.CODENAME + "_" + android.os.Build.VERSION.INCREMENTAL + "_" + android.os.Build.VERSION.RELEASE + "_" + android.os.Build.VERSION.SDK_INT;
    }
    
    public static String getOSNameAndVersionString() {
        return OSUtils.getOSName()+"-v"+OSUtils.getOSVersionString();
    }
}