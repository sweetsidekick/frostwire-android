package com.frostwire.android.gui.upnp;

import org.teleal.cling.binding.annotations.UpnpAction;
import org.teleal.cling.binding.annotations.UpnpOutputArgument;
import org.teleal.cling.binding.annotations.UpnpService;
import org.teleal.cling.binding.annotations.UpnpServiceId;
import org.teleal.cling.binding.annotations.UpnpServiceType;
import org.teleal.cling.binding.annotations.UpnpStateVariable;

import com.frostwire.android.core.ConfigurationManager;
import com.frostwire.android.gui.Librarian;
import com.frostwire.android.gui.NetworkManager;
import com.frostwire.android.util.JsonUtils;

@UpnpService(serviceId = @UpnpServiceId("UPnPFWDeviceInfo"), serviceType = @UpnpServiceType(value = "UPnPFWDeviceInfo", version = 1))
public class UPnPFWDeviceInfo {

    @UpnpStateVariable(defaultValue = "", sendEvents = false)
    private String basicInfo;

    public UPnPFWDeviceInfo() {
    }

    @UpnpAction(out = @UpnpOutputArgument(name = "RetBasicInfo"))
    public String getBasicInfo() {
        BasicInfo d = new BasicInfo();
        d.listeningPort = NetworkManager.instance().getListeningPort();
        d.numSharedFiles = Librarian.instance().getNumFiles();
        d.nickname = ConfigurationManager.instance().getNickname();
        return basicInfo = JsonUtils.toJson(d);
    }
}
