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

package com.frostwire.android.gui.billing;

import java.io.InputStream;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tv.ouya.console.api.OuyaFacade;
import tv.ouya.console.api.OuyaResponseListener;
import tv.ouya.console.api.Purchasable;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Base64;

import com.frostwire.android.R;
import com.frostwire.android.gui.util.UIUtils;

/**
 * @author gubatron
 * @author aldenml
 *
 */
final class OuyaBiller implements Biller, OuyaResponseListener<String> {
    private static final Logger LOG = LoggerFactory.getLogger(OuyaBiller.class);

    //private final String TESTING = "true";
    private final String TESTING = "false";

    private static final String DEVELOPER_ID = "4112997b-67db-4ce3-96f0-12b0373895db";

    private final Context context;

    private PublicKey mPublicKey;

    public OuyaBiller(Activity activity) {
        OuyaFacade.getInstance().init(activity, DEVELOPER_ID);
        context = activity;
        mPublicKey = loadPublicKey();
    }

    @Override
    public boolean isInAppBillingSupported() {
        return OuyaFacade.getInstance().isRunningOnOUYAHardware() && OuyaFacade.getInstance().isInitialized();
    }

    @Override
    public void onDestroy() {
        OuyaFacade.getInstance().shutdown();
    }

    @Override
    public void requestPurchase(String sku) {
        try {
            SecureRandom sr;
            sr = SecureRandom.getInstance("SHA1PRNG");
            String uniqueId = Long.toHexString(sr.nextLong());

            JSONObject purchaseRequest = new JSONObject();
            purchaseRequest.put("uuid", uniqueId);
            purchaseRequest.put("identifier", sku);
            purchaseRequest.put("testing", TESTING);

            String purchaseRequestJson = purchaseRequest.toString();

            byte[] keyBytes = new byte[16];
            sr.nextBytes(keyBytes);
            SecretKey key = new SecretKeySpec(keyBytes, "AES");

            byte[] ivBytes = new byte[16];
            sr.nextBytes(ivBytes);
            IvParameterSpec iv = new IvParameterSpec(ivBytes);

            Cipher cipher;
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "BC");
            cipher.init(Cipher.ENCRYPT_MODE, key, iv);
            byte[] payload = cipher.doFinal(purchaseRequestJson.getBytes("UTF-8"));

            cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding", "BC");
            cipher.init(Cipher.ENCRYPT_MODE, mPublicKey);
            byte[] encryptedKey = cipher.doFinal(keyBytes);

            Purchasable purchasable = new Purchasable(sku, 
                    Base64.encodeToString(encryptedKey, Base64.NO_WRAP), 
                    Base64.encodeToString(ivBytes, Base64.NO_WRAP), 
                    Base64.encodeToString(payload,
                    Base64.NO_WRAP));

            //            synchronized (mOutstandingPurchaseRequests) {
            //                mOutstandingPurchaseRequests.put(uniqueId, product);
            //            }

            OuyaFacade.getInstance().requestPurchase(purchasable, this);

        } catch (Throwable e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onCancel() {
    }

    @Override
    public void onFailure(int arg0, String arg1, Bundle arg2) {
    }

    @Override
    public void onSuccess(String arg0) {
        UIUtils.showLongMessage(context, R.string.donation_thanks);
    }

    private PublicKey loadPublicKey() {
        PublicKey pKey = null;
        try {
            InputStream inputStream = context.getResources().openRawResource(R.raw.key);
            byte[] applicationKey = new byte[inputStream.available()];
            inputStream.read(applicationKey);
            inputStream.close();
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(applicationKey);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            pKey = keyFactory.generatePublic(keySpec);
        } catch (Exception e) {
            LOG.error("Unable to create encryption key", e);
        }
        return pKey;
    }
}