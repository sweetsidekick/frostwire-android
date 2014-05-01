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

package com.frostwire.android.gui.fragments;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.frostwire.android.R;
import com.frostwire.android.core.Constants;
import com.frostwire.android.gui.billing.Biller;
import com.frostwire.android.gui.billing.BillerFactory;
import com.frostwire.android.gui.billing.DonationSkus;
import com.frostwire.android.gui.billing.DonationSkus.DonationSkuType;
import com.frostwire.android.gui.util.OfferUtils;
import com.frostwire.android.gui.views.ClickAdapter;
import com.frostwire.android.gui.views.DonateButtonListener;

/**
 * @author gubatron
 * @author aldenml
 * 
 */
public class AboutFragment extends Fragment implements MainFragment {

    private Biller biller;

    public AboutFragment() {
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        biller = BillerFactory.getInstance(getActivity());
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_about, container, false);

        TextView title = (TextView) view.findViewById(R.id.fragment_about_title);
        title.setText("FrostWire v" + Constants.FROSTWIRE_VERSION_STRING + " build " + Constants.FROSTWIRE_BUILD);

        TextView content = (TextView) view.findViewById(R.id.fragment_about_content);
        content.setText(Html.fromHtml(getAboutText()));
        content.setMovementMethod(LinkMovementMethod.getInstance());
        
        setupBitcoinDonateButton(view);
        DonationSkus skus = BillerFactory.getDonationSkus();
        setupDonateButton(getButton(view, R.id.fragment_about_button_donate1), skus.getSku(DonationSkuType.SKU_01_DOLLARS), "https://gumroad.com/l/pH", biller);
        setupDonateButton(getButton(view, R.id.fragment_about_button_donate2), skus.getSku(DonationSkuType.SKU_05_DOLLARS), "https://gumroad.com/l/oox", biller);
        setupDonateButton(getButton(view, R.id.fragment_about_button_donate3), skus.getSku(DonationSkuType.SKU_10_DOLLARS), "https://gumroad.com/l/rPl", biller);
        setupDonateButton(getButton(view, R.id.fragment_about_button_donate4), skus.getSku(DonationSkuType.SKU_25_DOLLARS), "https://gumroad.com/l/XQW", biller);

        return view;
    }

    @Override
    public View getHeader(Activity activity) {
        LayoutInflater inflater = LayoutInflater.from(activity);
        TextView header = (TextView) inflater.inflate(R.layout.view_main_fragment_simple_header, null);
        header.setText(R.string.about);

        return header;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (biller != null) {
            biller.onDestroy();
        }
    }

    private String getAboutText() {
        try {
            InputStream raw = getResources().openRawResource(R.raw.about);
            return IOUtils.toString(raw, "UTF-8");
        } catch (IOException e) {
            return "";
        }
    }
    
    private Button getButton(View v, int buttonId) {
        return (Button) v.findViewById(buttonId);
    }

    private void setupDonateButton(Button donateButton, String sku, String url, Biller biller) {
        donateButton.setOnClickListener(new DonateButtonListener2(this, sku, url));
    }
    
    private void setupBitcoinDonateButton(View v) {
        Button btc = (Button) v.findViewById(R.id.fragment_about_button_bitcoin);
        btc.setOnClickListener(new BitcoinButtonListener(this));
    }
    
    private static final class BitcoinButtonListener extends ClickAdapter<AboutFragment> {

        public BitcoinButtonListener(AboutFragment owner) {
            super(owner);
        }

        @Override
        public void onClick(AboutFragment owner, View v) {
            OfferUtils.onBTCDonationButtonClick(owner.getActivity());
        }
    }
    
    private static final class DonateButtonListener2 extends ClickAdapter<AboutFragment> {

        private final DonateButtonListener delegate;

        public DonateButtonListener2(AboutFragment owner, String sku, String url) {
            super(owner);
            this.delegate = new DonateButtonListener(null, sku, url);
        }

        @Override
        public void onClick(AboutFragment owner, View v) {
            if (owner.biller != null) {
                delegate.onClick(owner.biller, v);
            }
        }
    }
}