package com.frostwire.gui.upnp;

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

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        this.service = (AndroidUpnpService) service;
        this.registryListener.setService(this.service.get());

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
        DeviceIdentity identity = new DeviceIdentity(UDN.uniqueSystemIdentifier(ConfigurationManager.instance().getUUIDString()));

        UPnPFWDevice device = new UPnPFWDevice();

        UPnPFWDeviceDesc d = device.getDeviceDesc();

        DeviceType type = new UDADeviceType(d.getDeviceType(), d.getVersion());

        DeviceDetails details = new DeviceDetails(d.getFriendlyName(), new ManufacturerDetails(d.getManufacturer()), new ModelDetails(d.getModelName(), d.getModelDescription(), d.getModelNumber()));

        //Icon icon = new Icon("image/png", 48, 48, 8, getClass().getResource("icon.png"));

        LocalService<UPnPFWDeviceInfo> deviceService = new AnnotationLocalServiceBinder().read(UPnPFWDeviceInfo.class);

        deviceService.setManager(new DefaultServiceManager<UPnPFWDeviceInfo>(deviceService, UPnPFWDeviceInfo.class));

        return new LocalDevice(identity, type, details, (Icon) null/*icon*/, deviceService);
    }
}
