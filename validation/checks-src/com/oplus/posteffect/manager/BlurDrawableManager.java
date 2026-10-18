package com.oplus.posteffect.manager;
public final class BlurDrawableManager {
    public static int sBlendTypeValue=-1;
    public void addBlurDrawable(android.view.SurfaceControl surface,int id,com.oplus.posteffect.BlurParam param,
            boolean exclusive,BlurStateListener listener,boolean enabled){param.toFloatArray(enabled);}
}
