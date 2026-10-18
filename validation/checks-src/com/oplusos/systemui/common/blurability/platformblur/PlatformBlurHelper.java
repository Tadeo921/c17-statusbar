package com.oplusos.systemui.common.blurability.platformblur;
/** Captured native helper binds host before ensureBlurDrawable creates wrapper. */
public final class PlatformBlurHelper {
    public android.view.View hostView;
    public PlatformBlurHelper(android.view.View view){hostView=view;}
    public void ensureBlurDrawable(){}
}
