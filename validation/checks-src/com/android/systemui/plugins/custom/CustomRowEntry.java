package com.android.systemui.plugins.custom;
/** Public audited native entry identity; no device data in the fixture. */
public class CustomRowEntry {
    private final String serviceId,packageName;
    public boolean identityUnavailableForCheck;
    public int keyReadsForCheck,extrasReadsForCheck;
    public CustomRowEntry(){this("","");}
    public CustomRowEntry(String serviceId,String packageName){this.serviceId=serviceId;this.packageName=packageName;}
    public final String getServiceId(){if(identityUnavailableForCheck)throw new IllegalStateException("Disposed native entry");return serviceId;}
    public final String getPackageName(){return packageName;}
    public final String getKey(){keyReadsForCheck++;return "fixture-instance-key";}
    public final android.os.Bundle getExtras(){extrasReadsForCheck++;return new android.os.Bundle();}
    public android.view.View immersiveCardView;
    public android.view.View customView,miniCardView;
    public final android.view.View getImmersiveCardView(){ return immersiveCardView; }
    public final android.view.View getCustomView(){return customView;}
    public final android.view.View getMiniCardView(){return miniCardView;}
}
