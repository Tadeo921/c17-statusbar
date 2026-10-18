// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.RenderEffect;

/** Cached colour/opacity outlet for an already selected clock material.
 * Plain text never creates an effect. No background or pixel source is read.
 */
final class ClockMaterialMask {
    private RenderEffect input,output;
    private int color;
    private float opacity,alpha;
    RenderEffect apply(RenderEffect source,int mask,float amount,float textAlpha) {
        if(source==null){clear();return null;}
        float strength=finite(amount),coverage=finite(textAlpha);
        if(input==source&&color==mask&&opacity==strength&&alpha==coverage)return output;
        float weight=(mask>>>24)/255f*strength;
        RenderEffect next=source;
        if(weight>0f||coverage<1f) {
            float keep=1f-weight;
            ColorMatrix matrix=new ColorMatrix(new float[]{
                    keep,0,0,0,((mask>>>16)&255)*weight,
                    0,keep,0,0,((mask>>>8)&255)*weight,
                    0,0,keep,0,(mask&255)*weight,
                    0,0,0,coverage,0});
            next=RenderEffect.createChainEffect(RenderEffect.createColorFilterEffect(new ColorMatrixColorFilter(matrix)),source);
        }
        input=source;output=next;color=mask;opacity=strength;alpha=coverage;return next;
    }
    void clear(){input=output=null;}
    private static float finite(float value){return Float.isFinite(value)?Math.max(0f,Math.min(1f,value)):0f;}
}
