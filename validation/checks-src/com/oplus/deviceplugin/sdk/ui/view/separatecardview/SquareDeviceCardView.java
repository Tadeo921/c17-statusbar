package com.oplus.deviceplugin.sdk.ui.view.separatecardview;
public class SquareDeviceCardView extends DeviceCardFixture {
    public com.oplus.deviceplugin.sdk.entity.PanelDevice q;
    public final android.view.View s;
    public final com.oplus.deviceplugin.sdk.ui.view.progressbar.BatteryCircularProgressLayout p;
    public SquareDeviceCardView(){super("square_device_constraintlayout");s=body;p=new com.oplus.deviceplugin.sdk.ui.view.progressbar.BatteryCircularProgressLayout();p.parent=body;}
    public void d(com.oplus.deviceplugin.sdk.entity.PanelDevice model){q=model;}
    public void g(){}
}
