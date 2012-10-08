/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011, 2012, FrostWire(TM). All rights reserved.
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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.frostwire.android.R;
import com.frostwire.android.util.IOUtils;

/**
 * @author gubatron
 * @author aldenml
 * 
 */
public class AboutFragment extends Fragment implements MainFragment {

    public AboutFragment() {
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_about, container, false);

        TextView title = (TextView) view.findViewById(R.id.fragment_about_title);
        title.setText("FrostWire for Android");

        TextView content = (TextView) view.findViewById(R.id.fragment_about_content);
        content.setText(Html.fromHtml(getAboutText()));
        content.setMovementMethod(LinkMovementMethod.getInstance());

        setupDonateButton(view, R.id.fragment_about_button_donate1, "https://gumroad.com/l/pH");
        setupDonateButton(view, R.id.fragment_about_button_donate2, "https://gumroad.com/l/oox");
        setupDonateButton(view, R.id.fragment_about_button_donate3, "https://gumroad.com/l/rPl");
        setupDonateButton(view, R.id.fragment_about_button_donate4, "https://gumroad.com/l/XQW");

        return view;
    }

    @Override
    public View getHeader(Activity activity) {
        LayoutInflater inflater = LayoutInflater.from(activity);
        TextView header = (TextView) inflater.inflate(R.layout.view_main_fragment_simple_header, null);
        header.setText(R.string.about);

        return header;
    }

    private String getAboutText() {
        try {
            InputStream raw = getResources().openRawResource(R.raw.about);
            return IOUtils.toString(raw, Charset.forName("UTF-8"));
        } catch (IOException e) {
            return "";
        }
    }

    private void setupDonateButton(View view, int id, String url) {
        Button donate = (Button) view.findViewById(id);
        donate.setOnClickListener(new DonateButtonListener(url));
    }

    private final class DonateButtonListener implements OnClickListener {

        private final String url;

        public DonateButtonListener(String url) {
            this.url = url;
        }

        @Override
        public void onClick(View v) {
            Intent i = new Intent("android.intent.action.VIEW", Uri.parse(url));
            startActivity(i);
        }
    }
}