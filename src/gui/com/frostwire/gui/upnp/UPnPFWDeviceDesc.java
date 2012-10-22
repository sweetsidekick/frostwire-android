package com.frostwire.gui.upnp;

public class UPnPFWDeviceDesc {

    private final String deviceType;
    private final int version;
    private final String friendlyName;
    private final String manufacturer;
    private final String modelName;
    private final String modelDescription;
    private final String modelNumber;

    public UPnPFWDeviceDesc() {
        this.deviceType = "UPnPFWDevice";
        this.version = 1;
        this.friendlyName = "FrostWire Android";
        this.manufacturer = "FrostWire";
        this.modelName = "FrostWire Android phone";
        this.modelDescription = "FrostWire Android phone device";
        this.modelNumber = "v1";
    }

    public String getDeviceType() {
        return deviceType;
    }

    public int getVersion() {
        return version;
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public String getModelName() {
        return modelName;
    }

    public String getModelDescription() {
        return modelDescription;
    }

    public String getModelNumber() {
        return modelNumber;
    }
}
