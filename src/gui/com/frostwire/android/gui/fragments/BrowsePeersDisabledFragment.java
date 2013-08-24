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

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.frostwire.android.R;
import com.frostwire.android.core.ConfigurationManager;
import com.frostwire.android.core.Constants;
import com.frostwire.android.gui.activities.MainActivity;
import com.frostwire.android.gui.util.OfferUtils;
import com.frostwire.gui.upnp.UPnPManager;

public class BrowsePeersDisabledFragment extends Fragment implements MainFragment {

    private TextView header;
    private Button wifiEnableButton;
    private Button freeAppsButton;

    public BrowsePeersDisabledFragment() {
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setRetainInstance(true);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_browse_peers_disabled, container, false);
        
        if (!view.isInEditMode()) {
            wifiEnableButton = (Button) view.findViewById(R.id.view_wifi_sharing_disabled_button_enable_wifi_sharing);
            freeAppsButton = (Button) view.findViewById(R.id.view_wifi_sharing_disabled_button_free_apps);
            freeAppsButton.setVisibility(OfferUtils.isfreeAppsEnabled() ? View.VISIBLE : View.GONE);
            freeAppsButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    OfferUtils.onFreeAppsClick(v.getContext());
                }
            });
            
            wifiEnableButton.setOnClickListener(new View.OnClickListener() {
                
                @Override
                public void onClick(View v) {
                    onWifiEnableButtonClick();
                }
            });
        }
        
        return view;
    }
    
    @Override
    public View getHeader(Activity activity) {
        LayoutInflater inflater = LayoutInflater.from(activity);
        header = (TextView) inflater.inflate(R.layout.view_main_fragment_simple_header, null);
        header.setText(R.string.wifi_sharing);

        return header;
    }


    private void onWifiEnableButtonClick() {
        if (getActivity() instanceof MainActivity) {
            ConfigurationManager.instance().setBoolean(Constants.PREF_KEY_NETWORK_USE_UPNP, true);
            UPnPManager.instance().resume();
            MainActivity activity = (MainActivity) getActivity();
            activity.switchFragment(R.id.menu_main_peers);
        }
    }
 }