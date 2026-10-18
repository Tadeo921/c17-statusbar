package com.oplus.deviceplugin.sdk.entity;
/** The audited SDK state 1 is USABLE/SepQSActive; other values use inactive material. */
public final class PanelDevice {
    public int state;
    public int stateReads;
    public DeviceType type=DeviceType.PHONE;
    public String iconUri;
    public PanelDevice(int state){this.state=state;}
    public int getDeviceState(){stateReads++;return state;}
    public DeviceType getDeviceType(){return type;}
    public String getIconUri(){return iconUri;}
}
