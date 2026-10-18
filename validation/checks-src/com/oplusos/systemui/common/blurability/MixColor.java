package com.oplusos.systemui.common.blurability;
/** Exact immutable OEM API, verified against cached SystemUI classes4.dex. */
public final class MixColor {
    private final int mode,topLayerColor,bottomLayerColor;
    public MixColor(int mode,int top,int bottom){this.mode=mode;topLayerColor=top;bottomLayerColor=bottom;}
    public int getMode(){return mode;}
    public int getTopLayerColor(){return topLayerColor;}
    public int getBottomLayerColor(){return bottomLayerColor;}
    public MixColor copy(int mode,int top,int bottom){return new MixColor(mode,top,bottom);}
}
