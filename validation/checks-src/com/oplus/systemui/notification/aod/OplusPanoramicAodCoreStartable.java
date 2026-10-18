package com.oplus.systemui.notification.aod;
/** Exact repository/setter contract, not a guessed dozing flag. */
public class OplusPanoramicAodCoreStartable implements OplusPanoramicAodRepository {
    public float panoramicAodProgress;
    public int readsForCheck;
    public float getPanoramicAodProgress(){readsForCheck++;return panoramicAodProgress;}
    private void setPanoramicAodProgress(float value){panoramicAodProgress=value;}
    public void progressForCheck(float value){setPanoramicAodProgress(value);}
}
