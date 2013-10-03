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

package com.frostwire.android.gui.views;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.view.View.OnClickListener;

import com.frostwire.android.gui.billing.Biller;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public final class DonateButtonListener implements OnClickListener {

    private static final Logger LOG = LoggerFactory.getLogger(DonateButtonListener.class);

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

        if (biller != null && biller.isInAppBillingSupported()) {
            biller.requestPurchase(sku);
        } else {
            Intent i = new Intent("android.intent.action.VIEW", Uri.parse(url));
            v.getContext().startActivity(i);
        }
    }
}