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

/**
 * @author gubatron
 * @author aldenml
 *
 */
public interface DonationSkus {
    
    public enum DonationSkuType {
        SKU_01_DOLLARS, SKU_05_DOLLARS, SKU_10_DOLLARS, SKU_25_DOLLARS
    }

    public String getSku(DonationSkuType type);

    static final class DefaultDonationSkus implements DonationSkus {

        @Override
        public String getSku(DonationSkuType type) {
            switch (type) {
            case SKU_01_DOLLARS:
                return "frostwire.donation.one";
            case SKU_05_DOLLARS:
                return "frostwire.donation.five";
            case SKU_10_DOLLARS:
                return "frostwire.donation.ten";
            case SKU_25_DOLLARS:
                return "frostwire.donation.twentyfive";
            default:
                return "frostwire.donation.twentyfive";
            }
        }
    }
}
