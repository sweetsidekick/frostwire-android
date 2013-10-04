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

import com.frostwire.android.gui.util.OSUtils;

import android.app.Activity;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public class BillerFactory {
    
    public static Biller getInstance(Activity activity) {
        Biller billy = null;

        if (OSUtils.isKindleFire() || OSUtils.isAmazonDistribution()) {
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