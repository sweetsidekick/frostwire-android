package com.frostwire.android.gui.upnp;

import java.net.InetAddress;

import org.teleal.cling.UpnpService;
import org.teleal.cling.controlpoint.ActionCallback;
import org.teleal.cling.model.action.ActionInvocation;
import org.teleal.cling.model.message.UpnpResponse;
import org.teleal.cling.model.meta.Device;
import org.teleal.cling.model.meta.LocalDevice;
import org.teleal.cling.model.meta.RemoteDevice;
import org.teleal.cling.model.meta.RemoteDeviceIdentity;
import org.teleal.cling.model.meta.Service;
import org.teleal.cling.model.types.InvalidValueException;
import org.teleal.cling.model.types.ServiceId;
import org.teleal.cling.model.types.UDAServiceId;
import org.teleal.cling.registry.DefaultRegistryListener;
import org.teleal.cling.registry.Registry;

import com.frostwire.android.core.messages.PingMessage;
import com.frostwire.android.gui.PeerManager;
import com.frostwire.android.util.JsonUtils;

import android.util.Log;

public class UPnPRegistryListener extends DefaultRegistryListener {

    private static final String TAG = "FW.UPnPRegistryListener";

    private final ServiceId deviceInfoId;

    private UpnpService service;

    public UPnPRegistryListener() {
        this.deviceInfoId = new UDAServiceId("UPnPFWDeviceInfo");
    }

    public UpnpService getService() {
        return service;
    }

    public void setService(UpnpService service) {
        this.service = service;
    }

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
        handleDevice(device, false);
    }

    public void deviceRemoved(final Device<?, ?, ?> device) {
        handleDevice(device, true);
    }

    private void handleDevice(final Device<?, ?, ?> device, boolean bye) {
        Service deviceInfo;
        if ((deviceInfo = device.findService(deviceInfoId)) != null) {
            handlePing(service, deviceInfo, bye);
        }
    }

    private void handlePing(UpnpService service, final Service deviceInfo, final boolean bye) {
        ActionInvocation pingDataInvocation = new ActionInvocation(deviceInfo.getAction("GetBasicInfo"));

        service.getControlPoint().execute(new ActionCallback(pingDataInvocation) {
            @Override
            public void success(ActionInvocation invocation) {
                try {
                    String json = invocation.getOutput()[0].toString();
                    BasicInfo d = JsonUtils.toObject(json, BasicInfo.class);
                    PingMessage ping = new PingMessage(d.listeningPort, d.numSharedFiles, d.nickname, bye);
                    InetAddress address = null;
                    if (deviceInfo.getDevice().getIdentity() instanceof RemoteDeviceIdentity) {
                        address = ((RemoteDeviceIdentity) deviceInfo.getDevice().getIdentity()).getDiscoveredOnLocalAddress();
                    } else {
                        address = InetAddress.getByName("127.0.0.1");
                    }
                    PeerManager.instance().onMessageReceived(address, ping);
                    Log.d(TAG, json);
                } catch (Throwable e) {
                    Log.e(TAG, "Error processing GetBasicInfo return", e);
                }
            }

            @Override
            public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                Log.e(TAG, defaultMsg);
            }
        });
    }
}
