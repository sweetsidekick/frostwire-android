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

import org.fourthline.cling.UpnpService;
import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.meta.LocalDevice;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.frostwire.android.core.ConfigurationManager;
import com.frostwire.android.core.Constants;
import com.frostwire.android.gui.services.Engine;
import com.frostwire.android.upnp.android.cling.AndroidUpnpService;
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
        if (localDevice == null) {
            localDevice = createLocalDevice();
        }
        return localDevice;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        this.service = (AndroidUpnpService) service;

        // getting ready for future device advertisements
        this.service.getRegistry().addListener(registryListener);

        if (ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_NETWORK_USE_UPNP)) {
            this.service.getRegistry().addDevice(getLocalDevice());

            // refresh the list with all known devices
            for (Device<?, ?, ?> device : this.service.getRegistry().getDevices()) {
                registryListener.deviceAdded(device);
            }

            // search asynchronously for all devices
            this.service.getControlPoint().search();
        }

        startSearchRefresher();

    }

    private void startSearchRefresher() {
        Engine.instance().getThreadPool().execute(new Runnable() {

            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(5000);

                        if (ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_NETWORK_USE_UPNP)) {
                            UPnPServiceConnection.this.service.getControlPoint().search();
                        }
                    } catch (Throwable t) {
                        // ignore
                    }
                }
            }
        });
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        if (service != null) {
            service.getRegistry().removeListener(registryListener);
        }
        service = null;
    }

    private static LocalDevice createLocalDevice() {
        try {
            UPnPFWDevice device = UPnPManager.instance().getUPnPLocalDevice();

            return new LocalDevice(device.getIdentity(), device.getType(), device.getDetails(), device.getIcon(), device.getServices());
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
