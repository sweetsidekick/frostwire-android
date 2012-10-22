package com.frostwire.gui.upnp;

import android.content.ServiceConnection;

public class UPnPManager {

    private UPnPRegistryListener registryListener;
    private UPnPServiceConnection serviceConnection;

    private static UPnPManager instance = new UPnPManager();

    public static UPnPManager instance() {
        return instance;
    }

    private UPnPManager() {
        registryListener = new UPnPRegistryListener();
        serviceConnection = new UPnPServiceConnection(registryListener);
    }

    public ServiceConnection getServiceConnection() {
        return serviceConnection;
    }
}
