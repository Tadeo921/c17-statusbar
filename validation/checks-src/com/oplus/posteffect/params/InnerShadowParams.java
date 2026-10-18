package com.oplus.posteffect.params;
import android.graphics.Color;
/** Audited classes3.dex: Color plus thirteen native optical controls. */
public class InnerShadowParams {
    public static int copiesForCheck;
    public float width,height;public Object mode;
    public Color shadowColor=Color.valueOf(Color.WHITE);
    public float offsetShadowMaxAlpha=.12f,offsetShadowMinAlpha,noOffsetShadowAlpha=.32f;
    public float offsetShadowRange=70f,offsetShadowX,offsetShadowY=90f,offsetShadowScaleX=1.3f,offsetShadowScaleY=1.8f;
    public float offsetShadowPow=10f,offsetShadowMix=.4f,noOffsetShadowRange=.5f,noOffsetShadowPow=8f,noOffsetShadowMix=.5f;
    public Color getShadowColor(){return shadowColor;}
    public void setShadowColor(Color value){shadowColor=value;}
    public void copyFrom(InnerShadowParams source){
        copiesForCheck++;shadowColor=source.shadowColor;offsetShadowMaxAlpha=source.offsetShadowMaxAlpha;
        offsetShadowMinAlpha=source.offsetShadowMinAlpha;noOffsetShadowAlpha=source.noOffsetShadowAlpha;
        offsetShadowRange=source.offsetShadowRange;offsetShadowX=source.offsetShadowX;offsetShadowY=source.offsetShadowY;
        offsetShadowScaleX=source.offsetShadowScaleX;offsetShadowScaleY=source.offsetShadowScaleY;offsetShadowPow=source.offsetShadowPow;
        offsetShadowMix=source.offsetShadowMix;noOffsetShadowRange=source.noOffsetShadowRange;noOffsetShadowPow=source.noOffsetShadowPow;noOffsetShadowMix=source.noOffsetShadowMix;
        width=source.width;height=source.height;mode=source.mode;
    }
}
