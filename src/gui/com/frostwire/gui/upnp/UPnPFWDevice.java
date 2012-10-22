package com.frostwire.gui.upnp;

public class UPnPFWDevice {

    private final UPnPFWDeviceDesc deviceDesc;

    public UPnPFWDevice() {
        this.deviceDesc = new UPnPFWDeviceDesc();
    }

    public UPnPFWDeviceDesc getDeviceDesc() {
        return deviceDesc;
    }
}
