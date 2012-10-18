package com.frostwire.android.gui.upnp;

import org.teleal.cling.binding.annotations.UpnpAction;
import org.teleal.cling.binding.annotations.UpnpInputArgument;
import org.teleal.cling.binding.annotations.UpnpOutputArgument;
import org.teleal.cling.binding.annotations.UpnpService;
import org.teleal.cling.binding.annotations.UpnpServiceId;
import org.teleal.cling.binding.annotations.UpnpServiceType;
import org.teleal.cling.binding.annotations.UpnpStateVariable;

import com.frostwire.android.gui.NetworkManager;

@UpnpService(serviceId = @UpnpServiceId("UPnPFWDeviceService"), serviceType = @UpnpServiceType(value = "UPnPFWDeviceService", version = 1))
public class UPnPFWDeviceService {

    @UpnpStateVariable(defaultValue = "0", sendEvents = false)
    private boolean target = false;

    @UpnpStateVariable(defaultValue = "0", sendEvents = false)
    private int listeningPort = 0;

    public UPnPFWDeviceService() {
        this.listeningPort = NetworkManager.instance().getListeningPort();
    }

    @UpnpAction
    public void setTarget(@UpnpInputArgument(name = "NewTargetValue") boolean newTargetValue) {
        target = newTargetValue;
        //System.out.println("Switch is: " + status);
    }

    @UpnpAction(out = @UpnpOutputArgument(name = "RetTargetValue"))
    public boolean getTarget() {
        return target;
    }

    @UpnpAction(out = @UpnpOutputArgument(name = "RetListeningPort"))
    public int getListeningPort() {
        return listeningPort;
    }
}
