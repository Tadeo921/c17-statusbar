// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.os.Bundle;

/** One settings snapshot shared by the clock and notification glyph materials.
 * Widths stay in dp until the actual target display density is known. */
final class ClockBackdropStyle {
    final boolean enabled;
    final int color;
    final float opacity,dateOpacity,footerOpacity;
    private final boolean borderEnabled;
    private final int borderColor;
    private final float borderWidth,borderOpacity,angle,highlight,shade,reliefWidth;
    private float density=Float.NaN;
    private ClockBackdropMaterial.Appearance cached;

    ClockBackdropStyle(Bundle source,String prefix) {
        boolean horizontal=NotificationBigClockSettings.LANDSCAPE_PREFIX.equals(prefix);
        enabled=flag(source,prefix+"digit_reveal_enabled",false)&&!SafetyMode.enabled(source);
        color=colour(source,prefix+"reveal_mask_color",0xffffffff);
        opacity=number(source,prefix+"reveal_mask_opacity",20f,100f)/100f;
        // Partial snapshots from earlier schemas inherit this orientation's
        // saved main strength. An explicit zero remains an independent choice.
        dateOpacity=number(source,key(ClockBackdropSettings.DATE_MASK_OPACITY,horizontal),opacity*100f,100f)/100f;
        footerOpacity=number(source,key(ClockBackdropSettings.FOOTER_MASK_OPACITY,horizontal),opacity*100f,100f)/100f;
        borderEnabled=flag(source,key(ClockBackdropSettings.EDGE_ENABLED,horizontal),true);
        borderColor=colour(source,key(ClockBackdropSettings.EDGE_COLOR,horizontal),0xffffffff);
        borderWidth=number(source,key(ClockBackdropSettings.EDGE_WIDTH,horizontal),.6f,4f);
        borderOpacity=number(source,key(ClockBackdropSettings.EDGE_OPACITY,horizontal),60f,100f)/100f;
        angle=number(source,key(ClockBackdropSettings.HIGHLIGHT_ANGLE,horizontal),315f,360f);
        highlight=number(source,key(ClockBackdropSettings.HIGHLIGHT_STRENGTH,horizontal),40f,100f)/100f;
        shade=number(source,key(ClockBackdropSettings.SHADE_STRENGTH,horizontal),0f,100f)/100f;
        reliefWidth=number(source,key(ClockBackdropSettings.DEPTH_WIDTH,horizontal),1.5f,8f);
    }

    ClockBackdropMaterial.Appearance appearance(float targetDensity) {
        float next=Float.isFinite(targetDensity)&&targetDensity>0f?targetDensity:1f;
        if(cached==null||density!=next) {
            density=next;
            cached=new ClockBackdropMaterial.Appearance(borderEnabled,borderWidth*next,borderColor,
                    borderOpacity,angle,highlight,shade,reliefWidth*next);
        }
        return cached;
    }
    private static String key(String portrait,boolean horizontal) {
        return horizontal?NotificationBigClockSettings.landscapeKey(portrait):portrait;
    }
    private static boolean flag(Bundle source,String key,boolean fallback) {
        Object value=source==null?null:source.get(key);return value instanceof Boolean?(Boolean)value:fallback;
    }
    private static float number(Bundle source,String key,float fallback,float maximum) {
        Object value=source==null?null:source.get(key);
        float number=value instanceof Number?((Number)value).floatValue():fallback;
        return Float.isFinite(number)?Math.max(0f,Math.min(maximum,number)):fallback;
    }
    private static int colour(Bundle source,String key,int fallback) {
        Object value=source==null?null:source.get(key);
        int color=value instanceof Integer?(Integer)value:fallback;
        return flag(source,key+"_custom_alpha",false)?color:color|0xff000000;
    }
}
