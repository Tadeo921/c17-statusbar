// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Portrait appearance defaults; NotificationBigClockSettings registers the
 * independent landscape copy. These controls never request another blur. */
public final class ClockBackdropSettings {
    private static final String PREFIX="notification_big_clock_backdrop_";
    public static final String EDGE_ENABLED=PREFIX+"edge_enabled";
    public static final String EDGE_WIDTH=PREFIX+"edge_width";
    public static final String EDGE_COLOR=PREFIX+"edge_color";
    public static final String EDGE_OPACITY=PREFIX+"edge_opacity";
    public static final String HIGHLIGHT_ANGLE=PREFIX+"highlight_angle";
    public static final String HIGHLIGHT_STRENGTH=PREFIX+"highlight_strength";
    public static final String SHADE_STRENGTH=PREFIX+"shade_strength";
    public static final String DEPTH_WIDTH=PREFIX+"depth_width";
    public static final String DATE_MASK_OPACITY=PREFIX+"date_mask_opacity";
    public static final String FOOTER_MASK_OPACITY=PREFIX+"footer_mask_opacity";
    /** Retired notification-text protocol keys: imported/stored values remain compatible, with no runtime consumer. */
    public static final String NOTIFICATION_FOLLOW="notification_text_follow_clock_backdrop";
    public static final String NOTIFICATION_RELIEF="notification_text_backdrop_relief_enabled";
    public static final Map<String,Boolean> GLOBAL_BOOLEANS;
    public static final Map<String,Boolean> BOOLEANS=Collections.singletonMap(EDGE_ENABLED,true);
    public static final Map<String,Integer> COLORS=Collections.singletonMap(EDGE_COLOR,0xffffffff);
    public static final Map<String,Float> NUMBERS;
    static {
        Map<String,Boolean> global=new LinkedHashMap<>();global.put(NOTIFICATION_FOLLOW,false);global.put(NOTIFICATION_RELIEF,false);
        GLOBAL_BOOLEANS=Collections.unmodifiableMap(global);
        Map<String,Float> values=new LinkedHashMap<>();values.put(EDGE_WIDTH,.6f);values.put(EDGE_OPACITY,60f);
        values.put(HIGHLIGHT_ANGLE,315f);values.put(HIGHLIGHT_STRENGTH,40f);values.put(SHADE_STRENGTH,0f);values.put(DEPTH_WIDTH,1.5f);
        values.put(DATE_MASK_OPACITY,20f);values.put(FOOTER_MASK_OPACITY,20f);
        NUMBERS=Collections.unmodifiableMap(values);
    }
    public static boolean owns(String key) {
        if(key!=null&&key.endsWith("_custom_alpha"))key=key.substring(0,key.length()-"_custom_alpha".length());
        key=NotificationBigClockSettings.portraitKey(key);
        return BOOLEANS.containsKey(key)||COLORS.containsKey(key)||NUMBERS.containsKey(key);
    }
    public static boolean numericKey(String key){return NUMBERS.containsKey(NotificationBigClockSettings.portraitKey(key));}
    public static float minimum(String key){return 0f;}
    public static float maximum(String key) {
        key=NotificationBigClockSettings.portraitKey(key);
        return EDGE_WIDTH.equals(key)?4f:DEPTH_WIDTH.equals(key)?8f:HIGHLIGHT_ANGLE.equals(key)?360f:100f;
    }
    /** Compare exact decimal input before Float rounding at a hard boundary. */
    public static String numericInputError(String key,BigDecimal value) {
        if(!numericKey(key))return null;
        return value==null||value.signum()<0||value.compareTo(BigDecimal.valueOf(maximum(key)))>0?rangeError(key):null;
    }
    public static String numericValidationError(String key,float value) {
        return numericKey(key)&&(!Float.isFinite(value)||value<0f||value>maximum(key))?rangeError(key):null;
    }
    private static String rangeError(String key) {
        key=NotificationBigClockSettings.portraitKey(key);
        if(EDGE_WIDTH.equals(key))return "描边宽度应为 0 到 4 dp";
        if(DEPTH_WIDTH.equals(key))return "立体边缘宽度应为 0 到 8 dp";
        if(HIGHLIGHT_ANGLE.equals(key))return "高光角度应为 0 到 360°";
        return "强度应为 0 到 100%";
    }
    private ClockBackdropSettings() { }
}
