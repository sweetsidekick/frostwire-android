package com.frostwire.gui.upnp;

import org.teleal.cling.android.AndroidUpnpServiceConfiguration;
import org.teleal.cling.android.AndroidUpnpServiceImpl;

import android.net.wifi.WifiManager;

public class UPnPService extends AndroidUpnpServiceImpl {

    private static final int REGISTRY_MAINTENANCE_INTERVAL_MILLIS = 5000; // 5 seconds

    @Override
    protected AndroidUpnpServiceConfiguration createConfiguration(WifiManager wifiManager) {
        return new AndroidUpnpServiceConfiguration(wifiManager) {
            @Override
            public int getRegistryMaintenanceIntervalMillis() {
                return REGISTRY_MAINTENANCE_INTERVAL_MILLIS;
            }

            /*
            @Override
            public ServiceType[] getExclusiveServiceTypes() {
                return new ServiceType[] {
                        new UDAServiceType("SwitchPower")
                };
            }*/
        };
    }
}
