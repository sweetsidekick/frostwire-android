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

package com.frostwire.android.gui.fragments;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.frostwire.android.R;
import com.frostwire.android.core.Constants;
import com.frostwire.android.gui.Biller;
import com.frostwire.android.gui.views.DonateButtonListener;
import com.frostwire.android.market.ResponseHandler;

/**
 * @author gubatron
 * @author aldenml
 * 
 */
public class AboutFragment extends Fragment implements MainFragment {

    private static final String SKU_01_DOLLARS = "frostwire.donation.one";
    private static final String SKU_05_DOLLARS = "frostwire.donation.five";
    private static final String SKU_10_DOLLARS = "frostwire.donation.ten";
    private static final String SKU_25_DOLLARS = "frostwire.donation.twentyfive";

    private Biller biller;

    public AboutFragment() {
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        biller = new Biller(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_about, container, false);

        TextView title = (TextView) view.findViewById(R.id.fragment_about_title);
        title.setText("FrostWire v" + Constants.FROSTWIRE_VERSION_STRING + " build " + Constants.FROSTWIRE_BUILD);

        TextView content = (TextView) view.findViewById(R.id.fragment_about_content);
        content.setText(Html.fromHtml(getAboutText()));
        content.setMovementMethod(LinkMovementMethod.getInstance());
        
        if (biller == null) {
            biller = new Biller(getActivity());
        }

        setupDonateButton(view, R.id.fragment_about_button_donate1, SKU_01_DOLLARS, "https://gumroad.com/l/pH", biller);
        setupDonateButton(view, R.id.fragment_about_button_donate2, SKU_05_DOLLARS, "https://gumroad.com/l/oox", biller);
        setupDonateButton(view, R.id.fragment_about_button_donate3, SKU_10_DOLLARS, "https://gumroad.com/l/rPl", biller);
        setupDonateButton(view, R.id.fragment_about_button_donate4, SKU_25_DOLLARS, "https://gumroad.com/l/XQW", biller);

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
    public void onStart() {
        super.onStart();
        if (biller != null) {
            ResponseHandler.register(biller);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        
        if (biller != null) {
            ResponseHandler.unregister(biller);
        }
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
            return IOUtils.toString(raw, Charset.forName("UTF-8"));
        } catch (IOException e) {
            return "";
        }
    }

    private void setupDonateButton(View view, int id, String sku, String url, Biller biller) {
        Button donate = (Button) view.findViewById(id);
        donate.setOnClickListener(new DonateButtonListener(sku, url, biller));
    }
}