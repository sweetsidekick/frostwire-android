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

import java.lang.ref.WeakReference;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.frostwire.android.R;
import com.frostwire.android.core.Constants;
import com.frostwire.android.gui.billing.Biller;
import com.frostwire.android.gui.billing.BillerFactory;
import com.frostwire.android.gui.billing.DonationSkus;
import com.frostwire.android.gui.billing.DonationSkus.DonationSkuType;
import com.frostwire.android.gui.util.UIUtils;
import com.frostwire.util.Ref;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public final class DonationsController {

    public void setup(Context ctx, View v, Biller b) {
        setupBTC(ctx, v);

        DonationSkus skus = BillerFactory.getDonationSkus();
        setupDonate(ctx, v, b, R.id.view_donations_support_textview_1, skus.getSku(DonationSkuType.SKU_01_DOLLARS), "https://gumroad.com/l/pH");
        setupDonate(ctx, v, b, R.id.view_donations_support_textview_2, skus.getSku(DonationSkuType.SKU_01_DOLLARS), "https://gumroad.com/l/pH");
        setupDonate(ctx, v, b, R.id.view_donations_button_donate1, skus.getSku(DonationSkuType.SKU_01_DOLLARS), "https://gumroad.com/l/pH");
        setupDonate(ctx, v, b, R.id.view_donations_button_donate2, skus.getSku(DonationSkuType.SKU_05_DOLLARS), "https://gumroad.com/l/oox");
        setupDonate(ctx, v, b, R.id.view_donations_button_donate3, skus.getSku(DonationSkuType.SKU_10_DOLLARS), "https://gumroad.com/l/rPl");
        setupDonate(ctx, v, b, R.id.view_donations_button_donate4, skus.getSku(DonationSkuType.SKU_25_DOLLARS), "https://gumroad.com/l/XQW");
    }

    private void setupBTC(Context ctx, View v) {
        Button btn = findButton(v, R.id.view_donations_button_bitcoin);
        if (btn != null) {
            btn.setOnClickListener(new BitcoinListener(ctx));
        }
    }

    private void setupDonate(Context ctx, View v, Biller b, int id, String sku, String url) {
        Button btn = findButton(v, id);
        if (btn != null) {
            btn.setOnClickListener(new DonateListener(ctx, b, sku, url));
        } else {
            TextView tv = findTextView(v, id);
            if (tv != null) {
                tv.setOnClickListener(new DonateListener(ctx, b, sku, url));
            }
        }
    }

    private Button findButton(View v, int id) {
        View b = v.findViewById(id);
        if (b != null && b instanceof Button) {
            return (Button) b;
        } else {
            return null;
        }
    }
    
    private TextView findTextView(View v, int id) {
        View b = v.findViewById(id);
        if (b != null && b instanceof TextView) {
            return (TextView) b;
        } else {
            return null;
        }
    }

    private static final class BitcoinListener extends ClickAdapter2<Context> {

        public BitcoinListener(Context ctx) {
            super(ctx);
        }

        @Override
        public void onClick(Context ctx, View v) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(Constants.BITCOIN_DONATION_URI));
            try {
                ctx.startActivity(intent);
            } catch (Throwable t) {
                UIUtils.showLongMessage(ctx, R.string.you_need_a_bitcoin_wallet_app);
            }
        }
    }

    private static final class DonateListener extends ClickAdapter2<Context> {

        private final WeakReference<Biller> billerRef;
        private final String sku;
        private final String url;

        public DonateListener(Context ctx, Biller biller, String sku, String url) {
            super(ctx);
            this.billerRef = Ref.weak(biller);
            this.sku = sku;
            this.url = url;
        }

        @Override
        public void onClick(Context ctx, View v) {
            if (Ref.alive(billerRef)) {
                Biller biller = billerRef.get();

                // NP test again? yes
                if (biller != null && biller.isInAppBillingSupported()) {
                    biller.requestPurchase(sku);
                } else {
                    Intent i = new Intent("android.intent.action.VIEW", Uri.parse(url));
                    v.getContext().startActivity(i);
                }
            }
        }
    }
}
