package com.frostwire.android.gui.upnp;

import org.teleal.cling.model.meta.Device;
import org.teleal.cling.model.meta.LocalDevice;
import org.teleal.cling.model.meta.RemoteDevice;
import org.teleal.cling.registry.DefaultRegistryListener;
import org.teleal.cling.registry.Registry;

import android.util.Log;

public class UPnPRegistryListener extends DefaultRegistryListener {

    private static final String TAG = "FW.UPnPRegistryListener";

    @Override
    public void remoteDeviceDiscoveryStarted(Registry registry, RemoteDevice device) {
        deviceAdded(device);
    }

    @Override
    public void remoteDeviceDiscoveryFailed(Registry registry, final RemoteDevice device, final Exception ex) {
        Log.e(TAG, "Discovery failed of '" + device.getDisplayString() + "': " + (ex != null ? ex.toString() : "Couldn't retrieve device/service descriptors"));
        deviceRemoved(device);
    }

    @Override
    public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
        deviceAdded(device);
    }

    @Override
    public void remoteDeviceRemoved(Registry registry, RemoteDevice device) {
        deviceRemoved(device);
    }

    @Override
    public void localDeviceAdded(Registry registry, LocalDevice device) {
        deviceAdded(device);
    }

    @Override
    public void localDeviceRemoved(Registry registry, LocalDevice device) {
        deviceRemoved(device);
    }

    public void deviceAdded(final Device<?, ?, ?> device) {
        //        runOnUiThread(new Runnable() {
        //            public void run() {
        //                DeviceDisplay d = new DeviceDisplay(device);
        //                int position = listAdapter.getPosition(d);
        //                if (position >= 0) {
        //                    // Device already in the list, re-set new value at same position
        //                    listAdapter.remove(d);
        //                    listAdapter.insert(d, position);
        //                } else {
        //                    listAdapter.add(d);
        //                }
        //            }
        //        });

        Log.v(TAG, "deviceAdded: " + device.getDisplayString());
    }

    public void deviceRemoved(final Device<?, ?, ?> device) {
        //        runOnUiThread(new Runnable() {
        //            public void run() {
        //                listAdapter.remove(new DeviceDisplay(device));
        //            }
        //        });

        Log.v(TAG, "deviceRemoved: " + device.getDisplayString());
    }
}
