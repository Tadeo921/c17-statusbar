// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.content.SharedPreferences;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Schema 36: opt-in appearance following, glyph tint, and two portrait widget slots. */
final class Upgrade168 {
    static final Map<String,Object> DEFAULTS;
    static {
        Map<String,Object> values=new LinkedHashMap<>();
        for(String prefix:new String[]{NotificationBigClockSettings.PORTRAIT_PREFIX,
                NotificationBigClockSettings.LANDSCAPE_PREFIX}) {
            values.put(prefix+"follow_style_enabled",false);
            values.put(prefix+"reveal_mask_color",0xffffffff);
            values.put(StatusBarSettings.alphaKey(prefix+"reveal_mask_color"),false);
            values.put(prefix+"reveal_mask_opacity",20f);
        }
        values.put(BigClockWidgetsSettings.LEFT_ENABLED,false);
        values.put(BigClockWidgetsSettings.RIGHT_ENABLED,false);
        values.put(BigClockWidgetsSettings.LEFT_PROVIDER,"");
        values.put(BigClockWidgetsSettings.RIGHT_PROVIDER,"");
        values.put(BigClockWidgetsSettings.LEFT_HEIGHT,96f);
        values.put(BigClockWidgetsSettings.RIGHT_HEIGHT,96f);
        DEFAULTS=Collections.unmodifiableMap(values);
    }

    static void migrate(SharedPreferences preferences) {
        Map<String,?> old=preferences.getAll();
        if(old.isEmpty())return;
        Map<String,Object> missing=new LinkedHashMap<>();
        SharedPreferences.Editor editor=preferences.edit();
        for(Map.Entry<String,Object> item:DEFAULTS.entrySet())if(!old.containsKey(item.getKey())) {
            missing.put(item.getKey(),item.getValue());
            StatusBarSettings.copyPreference(editor,item.getKey(),item.getValue());
        }
        Object battery=old.get(StatusBarSettings.BATTERY_STYLE);
        if(battery instanceof String&&Ios27BatteryStyle.known((String)battery)) {
            missing.put(StatusBarSettings.BATTERY_STYLE,"pui");
            editor.putString(StatusBarSettings.BATTERY_STYLE,"pui");
        }
        if(!missing.isEmpty())PreferenceWrites.commit(preferences,editor,missing,false);
    }
    private Upgrade168() { }
}
