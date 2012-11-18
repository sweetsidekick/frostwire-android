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
import java.util.logging.Logger;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
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
import com.frostwire.android.gui.util.UIUtils;
import com.frostwire.android.market.BillingService;
import com.frostwire.android.market.BillingService.RequestPurchase;
import com.frostwire.android.market.BillingService.RestoreTransactions;
import com.frostwire.android.market.Consts;
import com.frostwire.android.market.Consts.PurchaseState;
import com.frostwire.android.market.Consts.ResponseCode;
import com.frostwire.android.market.PurchaseObserver;
import com.frostwire.android.market.ResponseHandler;
import com.frostwire.android.util.IOUtils;

/**
 * @author gubatron
 * @author aldenml
 * 
 */
public class AboutFragment extends Fragment implements MainFragment {

    private static final Logger LOG = Logger.getLogger(AboutFragment.class.getName());

    private Handler handler = new Handler();
    private DonationsPurchaseObserver donationsPurchaseObserver;

    private BillingService billingService;

    private boolean inAppSupported;

    public AboutFragment() {
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        handler = new Handler();
        donationsPurchaseObserver = new DonationsPurchaseObserver(getActivity(), handler);

        billingService = new BillingService();
        billingService.setContext(getActivity());

        inAppSupported = false;

        // Check if billing is supported.
        ResponseHandler.register(donationsPurchaseObserver);
        //        if (!billingService.checkBillingSupported()) {
        //            showDialog(DIALOG_CANNOT_CONNECT_ID);
        //        }

        if (!billingService.checkBillingSupported(Consts.ITEM_TYPE_INAPP)) {
            inAppSupported = false;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_about, container, false);

        TextView title = (TextView) view.findViewById(R.id.fragment_about_title);
        title.setText("FrostWire for Android");

        TextView content = (TextView) view.findViewById(R.id.fragment_about_content);
        content.setText(Html.fromHtml(getAboutText()));
        content.setMovementMethod(LinkMovementMethod.getInstance());

        setupDonateButton(view, R.id.fragment_about_button_donate1, "donation_001", "https://gumroad.com/l/pH");
        setupDonateButton(view, R.id.fragment_about_button_donate2, "donation_005", "https://gumroad.com/l/oox");
        setupDonateButton(view, R.id.fragment_about_button_donate3, "donation_010", "https://gumroad.com/l/rPl");
        setupDonateButton(view, R.id.fragment_about_button_donate4, "donation_025", "https://gumroad.com/l/XQW");

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
        ResponseHandler.register(donationsPurchaseObserver);
    }

    @Override
    public void onStop() {
        super.onStop();
        ResponseHandler.unregister(donationsPurchaseObserver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        billingService.unbind();
    }

    private String getAboutText() {
        try {
            InputStream raw = getResources().openRawResource(R.raw.about);
            return IOUtils.toString(raw, Charset.forName("UTF-8"));
        } catch (IOException e) {
            return "";
        }
    }

    private void setupDonateButton(View view, int id, String sku, String url) {
        Button donate = (Button) view.findViewById(id);
        donate.setOnClickListener(new DonateButtonListener(sku, url));
    }

    private class DonationsPurchaseObserver extends PurchaseObserver {

        public DonationsPurchaseObserver(Activity activity, Handler handler) {
            super(activity, handler);
        }

        @Override
        public void onBillingSupported(boolean supported, String type) {
            LOG.info("Market In-app billing support: " + supported);
            if (type == null || type.equals(Consts.ITEM_TYPE_INAPP)) {
                if (supported) {
                    inAppSupported = true;
                } else {
                    inAppSupported = false;
                }
            }
        }

        @Override
        public void onPurchaseStateChange(PurchaseState purchaseState, String itemId, int quantity, long purchaseTime, String developerPayload) {
            LOG.info("onPurchaseStateChange() itemId: " + itemId + " " + purchaseState);

            if (purchaseState == PurchaseState.PURCHASED) {
                UIUtils.showLongMessage(getActivity(), R.string.application_label);
            }
        }

        @Override
        public void onRequestPurchaseResponse(RequestPurchase request, ResponseCode responseCode) {
            LOG.info("onRequestPurchaseResponse" + request.mProductId + ": " + responseCode);

            if (responseCode == ResponseCode.RESULT_OK) {
                LOG.info("donation request was successfully sent to server");
            } else if (responseCode == ResponseCode.RESULT_USER_CANCELED) {
                LOG.info("user canceled donation");
            } else {
                LOG.info("donation failed");
                LOG.info(request.mProductId + " request donation returned " + responseCode);
            }
        }

        @Override
        public void onRestoreTransactionsResponse(RestoreTransactions request, ResponseCode responseCode) {
            LOG.info("onRestoreTransactionsResponse: " + responseCode);
        }
    }

    private final class DonateButtonListener implements OnClickListener {

        private final String sku;
        private final String url;

        public DonateButtonListener(String sku, String url) {
            // Static test
            this.sku = "android.test.purchased";
            //this.sku = sku;
            this.url = url;
        }

        @Override
        public void onClick(View v) {
            LOG.info("Donation sku: " + sku);

            if (inAppSupported) {
                // TODO: evaluate sending some value in the payload (last parameter)
                if (!billingService.requestPurchase(sku, Consts.ITEM_TYPE_INAPP, null)) {
                    inAppSupported = false;
                }
            } else {
                Intent i = new Intent("android.intent.action.VIEW", Uri.parse(url));
                startActivity(i);
            }
        }
    }
}