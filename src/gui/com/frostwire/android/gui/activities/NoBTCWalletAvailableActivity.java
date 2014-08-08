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

package com.frostwire.android.gui.activities;

import java.lang.ref.WeakReference;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.frostwire.android.R;
import com.frostwire.android.gui.views.AbstractActivity;

public class NoBTCWalletAvailableActivity extends AbstractActivity {
    
    public NoBTCWalletAvailableActivity() {
        super(R.layout.activity_no_btcwallet_available);
    }


    private class OnInstallWalletButtonListener implements OnClickListener {
        
        WeakReference<NoBTCWalletAvailableActivity> activityReference;
        
        public OnInstallWalletButtonListener(NoBTCWalletAvailableActivity activity) {
            activityReference = new WeakReference<NoBTCWalletAvailableActivity>(activity);
        }

        @Override
        public void onClick(View v) {
            Intent intent = new Intent(Intent.ACTION_VIEW,Uri.parse("market://details?id=de.schildbach.wallet"));
            NoBTCWalletAvailableActivity activity = activityReference.get();
            if (activity != null) {
                try {
                    activity.startActivity(intent);
                } catch (Throwable t) { 
                    //avoids crash on android-15
                }
            }
        }
    }

    @Override
    protected void initComponents(Bundle savedInstanceState) {
        Button installButton = findView(R.id.activity_no_btcwallet_available_install_wallet_button);
        installButton.setOnClickListener(new OnInstallWalletButtonListener(this));
    }       
}