package com.oplus.view;
/** Desktop fixture; no Android framework implementation is redistributed. */
public class ViewRootManager {
    public static dev.puitheme.FluidCloudStyle style;
    public com.oplus.graphics.OplusBlurParam received;
    public float[] saved;
    public int writes;
    public int blurRadius = 540;
    public void setBlurParams(com.oplus.graphics.OplusBlurParam params) {
        received = (com.oplus.graphics.OplusBlurParam) (style == null ? params : style.blurParams(this, params));
        saved = received.toFloatArray(); writes++;
    }
}
