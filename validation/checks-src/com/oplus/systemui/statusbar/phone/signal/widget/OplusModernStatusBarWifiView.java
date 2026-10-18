package com.oplus.systemui.statusbar.phone.signal.widget;

import android.content.res.Resources;
import android.view.ViewGroup;

/** Native owner identity fixture; tests retain connection, appearance and coordinate animation state. */
public class OplusModernStatusBarWifiView extends ViewGroup implements com.android.systemui.statusbar.StatusIconDisplayable {
    public String resourceName="wifi_combo",resourcePackage="com.android.systemui";
    public float appearAmount=1f,transXForCoord,translationX,translationY;
    public int visibleState;
    private final Resources resources=new Resources() {
        @Override public String getResourceEntryName(int id){return resourceName;}
        @Override public String getResourcePackageName(int id){return resourcePackage;}
    };
    public OplusModernStatusBarWifiView(){super(null);}
    public String getSlot(){return "wifi";}
    public boolean isIconVisible(){return true;}
    public boolean isIconBlocked(){return false;}
    public int getVisibleState(){return visibleState;}
    @Override public int getId(){return 100;}
    @Override public Resources getResources(){return resources;}
}
