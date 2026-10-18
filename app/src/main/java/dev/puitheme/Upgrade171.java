// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;
import android.content.SharedPreferences;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
/** Schema 38: reversible backdrop edge controls and optional component app clicks. */
final class Upgrade171 {
    static final Map<String,Object> DEFAULTS;
    static {
        Map<String,Object> values=new LinkedHashMap<>();
        // Keep this published batch frozen when later global options are added.
        values.put(ClockBackdropSettings.NOTIFICATION_FOLLOW,false);
        for(boolean landscape:new boolean[]{false,true}) {
            for(Map.Entry<String,Boolean> item:ClockBackdropSettings.BOOLEANS.entrySet())values.put(key(item.getKey(),landscape),item.getValue());
            // Published schema 38 contains only these six numbers, even as current controls grow.
            values.put(key(ClockBackdropSettings.EDGE_WIDTH,landscape),.6f);
            values.put(key(ClockBackdropSettings.EDGE_OPACITY,landscape),60f);
            values.put(key(ClockBackdropSettings.HIGHLIGHT_ANGLE,landscape),315f);
            values.put(key(ClockBackdropSettings.HIGHLIGHT_STRENGTH,landscape),40f);
            values.put(key(ClockBackdropSettings.SHADE_STRENGTH,landscape),0f);
            values.put(key(ClockBackdropSettings.DEPTH_WIDTH,landscape),1.5f);
            for(Map.Entry<String,Integer> item:ClockBackdropSettings.COLORS.entrySet()) {
                String color=key(item.getKey(),landscape);values.put(color,item.getValue());values.put(StatusBarSettings.alphaKey(color),false);
            }
        }
        for(int side:new int[]{BigClockWidgetsSettings.LEFT,BigClockWidgetsSettings.RIGHT}) {
            values.put(BigClockWidgetsSettings.clickEnabledKey(side),false);values.put(BigClockWidgetsSettings.launchAppKey(side),"");
        }
        DEFAULTS=Collections.unmodifiableMap(values);
    }
    private static String key(String key,boolean landscape){return landscape?NotificationBigClockSettings.landscapeKey(key):key;}
    static void migrate(SharedPreferences preferences) {
        Map<String,?> old=preferences.getAll();if(old.isEmpty())return;
        Map<String,Object> missing=new LinkedHashMap<>();SharedPreferences.Editor edit=preferences.edit();
        for(Map.Entry<String,Object> item:DEFAULTS.entrySet())if(!old.containsKey(item.getKey())) {
            missing.put(item.getKey(),item.getValue());StatusBarSettings.copyPreference(edit,item.getKey(),item.getValue());
        }
        if(!missing.isEmpty())PreferenceWrites.commit(preferences,edit,missing,false);
    }
    private Upgrade171(){ }
}
