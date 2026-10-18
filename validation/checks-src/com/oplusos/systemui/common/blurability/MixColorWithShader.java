package com.oplusos.systemui.common.blurability;
/** Actual OEM shader material contract, independent top and bottom blend modes. */
public final class MixColorWithShader {
    private final int topMode,topLayerColor,bottomMode,bottomLayerColor;
    public MixColorWithShader(int topMode,int top,int bottomMode,int bottom){this.topMode=topMode;topLayerColor=top;this.bottomMode=bottomMode;bottomLayerColor=bottom;}
    public int getTopMode(){return topMode;}
    public int getBottomMode(){return bottomMode;}
    public int getTopLayerColor(){return topLayerColor;}
    public int getBottomLayerColor(){return bottomLayerColor;}
    public MixColorWithShader copy(int topMode,int top,int bottomMode,int bottom){return new MixColorWithShader(topMode,top,bottomMode,bottom);}
}
