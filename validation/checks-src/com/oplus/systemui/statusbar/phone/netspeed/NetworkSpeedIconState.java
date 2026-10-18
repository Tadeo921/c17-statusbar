package com.oplus.systemui.statusbar.phone.netspeed;
/** Inspected OEM state; the original native view copies this when binding. */
public final class NetworkSpeedIconState {
    public boolean visible;public long speedText;public String slot,contentDescription;
    public NetworkSpeedIconState copy(){NetworkSpeedIconState result=new NetworkSpeedIconState();result.visible=visible;result.speedText=speedText;result.slot=slot;result.contentDescription=contentDescription;return result;}
    @Override public boolean equals(Object value){
        if(!(value instanceof NetworkSpeedIconState))return false;NetworkSpeedIconState other=(NetworkSpeedIconState)value;
        return visible==other.visible&&speedText==other.speedText&&java.util.Objects.equals(slot,other.slot)
                &&java.util.Objects.equals(contentDescription,other.contentDescription);
    }
    @Override public int hashCode(){return java.util.Objects.hash(visible,speedText,slot,contentDescription);}
}
