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

package com.frostwire.gui.upnp.android;

import org.teleal.cling.UpnpService;
import org.teleal.cling.android.AndroidUpnpService;
import org.teleal.cling.model.ValidationException;
import org.teleal.cling.model.meta.Device;
import org.teleal.cling.model.meta.LocalDevice;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.frostwire.gui.upnp.UPnPFWDevice;
import com.frostwire.gui.upnp.UPnPManager;
import com.frostwire.gui.upnp.UPnPRegistryListener;

/**
 * 
 * @author gubatron
 * @author aldenml
 * 
 */
public class UPnPServiceConnection implements ServiceConnection {

    private static final String TAG = "FW.UPnPServiceConnection";

    private AndroidUpnpService service;
    private UPnPRegistryListener registryListener;

    private static LocalDevice localDevice;

    public UPnPServiceConnection(UPnPRegistryListener registryListener) {
        this.registryListener = registryListener;
    }

    public UpnpService getService() {
        return service != null ? service.get() : null;
    }

    public static LocalDevice getLocalDevice() {
        return localDevice;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        this.service = (AndroidUpnpService) service;

        if (localDevice == null) {
            try {
                localDevice = createLocalDevice();
                this.service.getRegistry().addDevice(localDevice);
            } catch (ValidationException e) {
                Log.e(TAG, "Unable to create and register local UPnP frostwire device", e);
            }
        }

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
        if (service != null) {
            service.getRegistry().removeListener(registryListener);
        }
        service = null;
    }

    private LocalDevice createLocalDevice() throws ValidationException {
        UPnPFWDevice device = UPnPManager.instance().getUPnPLocalDevice();

        return new LocalDevice(device.getIdentity(), device.getType(), device.getDetails(), device.getIcon(), device.getServices());
    }
}
