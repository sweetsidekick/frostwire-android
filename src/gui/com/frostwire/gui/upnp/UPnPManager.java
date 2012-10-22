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

package com.frostwire.gui.upnp;

import java.net.InetAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.teleal.cling.UpnpService;
import org.teleal.cling.controlpoint.ActionCallback;
import org.teleal.cling.model.action.ActionInvocation;
import org.teleal.cling.model.message.UpnpResponse;
import org.teleal.cling.model.meta.Device;
import org.teleal.cling.model.meta.RemoteDeviceIdentity;
import org.teleal.cling.model.meta.Service;
import org.teleal.cling.model.types.ServiceId;
import org.teleal.cling.model.types.UDAServiceId;

import com.frostwire.android.util.JsonUtils;
import com.frostwire.gui.upnp.android.AndroidUPnPManager;

/**
 * 
 * @author gubatron
 * @author aldenml
 * 
 */
public abstract class UPnPManager {

    private static final Logger LOG = Logger.getLogger(UPnPManager.class.getName());

    protected UPnPRegistryListener registryListener;
    private final ServiceId deviceInfoId;

    private static UPnPManager instance = new AndroidUPnPManager();

    public static UPnPManager instance() {
        return instance;
    }

    protected UPnPManager() {
        this.registryListener = new UPnPRegistryListener() {
            @Override
            protected void handleDevice(Device<?, ?, ?> device, boolean added) {
                UPnPManager.this.handleDevice(device, added);
            }
        };

        this.deviceInfoId = new UDAServiceId("UPnPFWDeviceInfo");
    }

    public abstract UpnpService getService();

    public abstract UPnPFWDevice getUPnPLocalDevice();

    public abstract PingInfo getLocalPingInfo();

    protected abstract void handlePeerDevice(PingInfo p, InetAddress address, boolean added);

    private void handleDevice(final Device<?, ?, ?> device, boolean added) {
        Service<?, ?> deviceInfo;
        if ((deviceInfo = device.findService(deviceInfoId)) != null) {
            invokePingInfo(getService(), deviceInfo, added);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void invokePingInfo(UpnpService service, final Service<?, ?> deviceInfo, final boolean added) {
        ActionInvocation<?> pingDataInvocation = new ActionInvocation(deviceInfo.getAction("GetPingInfo"));

        service.getControlPoint().execute(new ActionCallback(pingDataInvocation) {
            @Override
            public void success(ActionInvocation invocation) {
                try {
                    String json = invocation.getOutput()[0].toString();
                    PingInfo p = JsonUtils.toObject(json, PingInfo.class);
                    InetAddress address = null;
                    if (deviceInfo.getDevice().getIdentity() instanceof RemoteDeviceIdentity) {
                        address = ((RemoteDeviceIdentity) deviceInfo.getDevice().getIdentity()).getDiscoveredOnLocalAddress();
                    } else {
                        address = InetAddress.getByName("127.0.0.1");
                    }

                    handlePeerDevice(p, address, added);
                } catch (Throwable e) {
                    LOG.log(Level.INFO, "Error processing GetPingInfo return", e);
                }
            }

            @Override
            public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                LOG.info(defaultMsg);
            }
        });
    }
}
