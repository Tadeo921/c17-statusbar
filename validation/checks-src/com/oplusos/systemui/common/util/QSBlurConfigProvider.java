package com.oplusos.systemui.common.util;

import android.graphics.drawable.Drawable;
import android.view.View;
import java.util.Map;
import java.util.WeakHashMap;

/** Native View-tag caches (ids 0x7f0a0eb0 / 0x7f0a0eb4) read by MixColor/Gradient builders. */
public final class QSBlurConfigProvider {
    private static final Map<View, Drawable> BLUR = new WeakHashMap<>(), GRADIENT = new WeakHashMap<>();
    private QSBlurConfigProvider() { }
    public static Drawable getBlurDrawableTag(View view) { return BLUR.get(view); }
    public static void setBlurDrawableTag(View view, Drawable value) { if (value == null) BLUR.remove(view); else BLUR.put(view, value); }
    public static Drawable getGradientDrawableTag(View view) { return GRADIENT.get(view); }
    public static void setGradientDrawableTag(View view, Drawable value) { if (value == null) GRADIENT.remove(view); else GRADIENT.put(view, value); }
}
