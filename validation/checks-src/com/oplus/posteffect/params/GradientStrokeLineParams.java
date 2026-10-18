package com.oplus.posteffect.params;
import android.graphics.Color;
/** Audited classes3.dex: Color plus thirteen native optical controls. */
public class GradientStrokeLineParams {
    public static int copiesForCheck;
    public static boolean failCopiesForCheck;
    public float width,height;public Object mode;
    public Color strokeLineColor=Color.valueOf(0xd8ffffff);
    public float strokeLineVerticalNearSolid=2f,strokeLineVerticalNearFade=6f,strokeLineVerticalFarSolid=2f,strokeLineVerticalFarFade=4f;
    public float strokeLineAlphaNear=.4f,strokeLineAlphaFar=.56f;
    public float strokeLineTransverseNearSolid=.2f,strokeLineTransverseNearFade=.4f,strokeLineTransverseFarSolid=.2f,strokeLineTransverseFarFade=.35f;
    public float ratio,strokeLinePow=3f,strokeLineMix=1f;
    public Color getStrokeLineColor(){return strokeLineColor;}
    public void setStrokeLineColor(Color value){strokeLineColor=value;}
    public void copyFrom(GradientStrokeLineParams source){
        if(failCopiesForCheck)throw new IllegalStateException("Changed native copy contract");
        copiesForCheck++;strokeLineColor=source.strokeLineColor;
        strokeLineVerticalNearSolid=source.strokeLineVerticalNearSolid;strokeLineVerticalNearFade=source.strokeLineVerticalNearFade;
        strokeLineVerticalFarSolid=source.strokeLineVerticalFarSolid;strokeLineVerticalFarFade=source.strokeLineVerticalFarFade;
        strokeLineAlphaNear=source.strokeLineAlphaNear;strokeLineAlphaFar=source.strokeLineAlphaFar;
        strokeLineTransverseNearSolid=source.strokeLineTransverseNearSolid;strokeLineTransverseNearFade=source.strokeLineTransverseNearFade;
        strokeLineTransverseFarSolid=source.strokeLineTransverseFarSolid;strokeLineTransverseFarFade=source.strokeLineTransverseFarFade;
        ratio=source.ratio;strokeLinePow=source.strokeLinePow;strokeLineMix=source.strokeLineMix;
        width=source.width;height=source.height;mode=source.mode;
    }
}
