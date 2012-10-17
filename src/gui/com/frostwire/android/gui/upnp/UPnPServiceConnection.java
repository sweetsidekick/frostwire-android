package com.frostwire.android.gui.upnp;

import org.teleal.cling.android.AndroidUpnpService;
import org.teleal.cling.model.meta.Device;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;

public class UPnPServiceConnection implements ServiceConnection {

    private AndroidUpnpService service;
    private UPnPRegistryListener registryListener;

    public UPnPServiceConnection(UPnPRegistryListener registryListener) {
        this.registryListener = registryListener;
    }

    public AndroidUpnpService getService() {
        return service;
    }

    public void unregister() {
        if (service != null && registryListener != null) {
            service.getRegistry().removeListener(registryListener);
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        this.service = (AndroidUpnpService) service;

        // refresh the list with all known devices
        for (Device<?, ?, ?> device : this.service.getRegistry().getDevices()) {
            registryListener.deviceAdded(device);
        }

        // getting ready for future device advertisements
        this.service.getRegistry().addListener(registryListener);

        // search asynchronously for all devices
        this.service.getControlPoint().search();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        service = null;
    }
}
