/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2014, FrostWire(R). All rights reserved.
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

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.frostwire.android.R;
import com.frostwire.android.gui.util.OfferUtils;

/**
 * @author guabtron
 * @author aldenml
 *
 */
public class DonationsView2 extends LinearLayout {

    public DonationsView2(Context context, AttributeSet attrs) {
        super(context, attrs);

        View.inflate(context, R.layout.view_donations2, this);

        Button btc = (Button) findViewById(R.id.view_donations_button_bitcoin);
        btc.setOnClickListener(new BitcoinDonationListener(context));
    }

    private static final class BitcoinDonationListener extends ClickAdapter2<Context> {

        public BitcoinDonationListener(Context ctx) {
            super(ctx);
        }

        @Override
        public void onClick(Context ctx, View v) {
            OfferUtils.onBTCDonationButtonClick(ctx);
        }
    }
}