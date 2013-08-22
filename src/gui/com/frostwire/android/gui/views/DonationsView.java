/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011, 2012, 2013, FrostWire(R). All rights reserved.
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
import com.frostwire.android.gui.Biller;

public class DonationsView extends LinearLayout {
    private Biller biller;
    private static final String SKU_01_DOLLARS = "frostwire.donation.one";
    private static final String SKU_05_DOLLARS = "frostwire.donation.five";
    private static final String SKU_10_DOLLARS = "frostwire.donation.ten";
    private static final String SKU_25_DOLLARS = "frostwire.donation.twentyfive";

    public DonationsView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    public void setBiller(Biller b) {
        biller = b;
        setupDonationButtons();
    }
    
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        View.inflate(getContext(), R.layout.view_donations, this);
        setupDonationButtons();
    }

    private void setupDonationButtons() {
        setupDonateButton(R.id.fragment_about_button_donate1, SKU_01_DOLLARS, "https://gumroad.com/l/pH");
        setupDonateButton(R.id.fragment_about_button_donate2, SKU_05_DOLLARS, "https://gumroad.com/l/oox");
        setupDonateButton(R.id.fragment_about_button_donate3, SKU_10_DOLLARS, "https://gumroad.com/l/rPl");
        setupDonateButton(R.id.fragment_about_button_donate4, SKU_25_DOLLARS, "https://gumroad.com/l/XQW");
    }

    private void setupDonateButton(int id, String sku, String url) {
        Button donate = (Button) findViewById(id);
        donate.setOnClickListener(new DonateButtonListener(sku, url, biller));
    }
}