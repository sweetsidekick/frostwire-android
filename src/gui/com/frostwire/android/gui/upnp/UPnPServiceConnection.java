package com.frostwire.android.gui.upnp;

import org.teleal.cling.android.AndroidUpnpService;
import org.teleal.cling.binding.annotations.AnnotationLocalServiceBinder;
import org.teleal.cling.model.DefaultServiceManager;
import org.teleal.cling.model.ValidationException;
import org.teleal.cling.model.meta.Device;
import org.teleal.cling.model.meta.DeviceDetails;
import org.teleal.cling.model.meta.DeviceIdentity;
import org.teleal.cling.model.meta.Icon;
import org.teleal.cling.model.meta.LocalDevice;
import org.teleal.cling.model.meta.LocalService;
import org.teleal.cling.model.meta.ManufacturerDetails;
import org.teleal.cling.model.meta.ModelDetails;
import org.teleal.cling.model.types.DeviceType;
import org.teleal.cling.model.types.UDADeviceType;
import org.teleal.cling.model.types.UDN;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.frostwire.android.core.ConfigurationManager;

public class UPnPServiceConnection implements ServiceConnection {

    private static final String TAG = "FW.UPnPServiceConnection";

    private AndroidUpnpService service;
    private UPnPRegistryListener registryListener;

    private static LocalDevice localDevice;

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
        service = null;
    }

    private LocalDevice createLocalDevice() throws ValidationException {
        DeviceIdentity identity = new DeviceIdentity(UDN.uniqueSystemIdentifier(ConfigurationManager.instance().getUUIDString()));

        UPnPFWDevice device = new UPnPFWDevice();

        DeviceType type = new UDADeviceType(device.getDeviceType(), device.getVersion());

        DeviceDetails details = new DeviceDetails(device.getFriendlyName(), new ManufacturerDetails(device.getManufacturer()), new ModelDetails(device.getModelName(), device.getModelDescription(), device.getModelNumber()));

        //Icon icon = new Icon("image/png", 48, 48, 8, getClass().getResource("icon.png"));

        LocalService<UPnPFWDeviceService> deviceService = new AnnotationLocalServiceBinder().read(UPnPFWDeviceService.class);

        deviceService.setManager(new DefaultServiceManager<UPnPFWDeviceService>(deviceService, UPnPFWDeviceService.class));

        return new LocalDevice(identity, type, details, (Icon) null/*icon*/, deviceService);
    }
}
