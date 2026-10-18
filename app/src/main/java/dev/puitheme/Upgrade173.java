// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;
import android.content.SharedPreferences;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
/** Schema 40: independently adjustable glyph tint strengths, inherited from the old shared strength. */
final class Upgrade173 {
    static final Map<String,Object> DEFAULTS;
    static {
        Map<String,Object> values=new LinkedHashMap<>();
        for(boolean landscape:new boolean[]{false,true})for(String key:new String[]{ClockBackdropSettings.DATE_MASK_OPACITY,
                ClockBackdropSettings.FOOTER_MASK_OPACITY})
            values.put(landscape?NotificationBigClockSettings.landscapeKey(key):key,20f);
        for(int side:new int[]{BigClockWidgetsSettings.LEFT,BigClockWidgetsSettings.RIGHT})values.put(BigClockWidgetsSettings.maskOpacityKey(side),20f);
        DEFAULTS=Collections.unmodifiableMap(values);
    }
    static float inheritedOpacity(Map<String,?> values,String key) {
        String main=key.startsWith(NotificationBigClockSettings.LANDSCAPE_PREFIX)
                ?NotificationBigClockSettings.landscapeKey(NotificationBigClockSettings.REVEAL_MASK_OPACITY)
                :NotificationBigClockSettings.REVEAL_MASK_OPACITY;
        return Math.max(0f,Math.min(100f,StatusBarSettings.number(values,main,20f)));
    }
    static Map<String,Object> inheritedDefaults(Map<String,?> values) {
        Map<String,Object> result=new LinkedHashMap<>();
        for(String key:DEFAULTS.keySet())result.put(key,inheritedOpacity(values,key));
        return result;
    }
    static void migrate(SharedPreferences preferences) {
        Map<String,?> old=preferences.getAll();if(old.isEmpty())return;
        Map<String,Object> missing=new LinkedHashMap<>();SharedPreferences.Editor edit=preferences.edit();
        for(Map.Entry<String,Object> item:inheritedDefaults(old).entrySet())if(!old.containsKey(item.getKey())) {
            missing.put(item.getKey(),item.getValue());StatusBarSettings.copyPreference(edit,item.getKey(),item.getValue());
        }
        if(!missing.isEmpty())PreferenceWrites.commit(preferences,edit,missing,false);
    }
    private Upgrade173(){ }
}
