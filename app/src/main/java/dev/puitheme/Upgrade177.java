// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.content.SharedPreferences;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Schema 43 adds only the two independent text-component font weights. */
final class Upgrade177 {
    static final Map<String,Object> DEFAULTS;
    static {
        Map<String,Object> defaults=new LinkedHashMap<>();
        defaults.put("notification_big_clock_widget_left_weight",400f);
        defaults.put("notification_big_clock_widget_right_weight",400f);
        DEFAULTS=Collections.unmodifiableMap(defaults);
    }
    static void migrate(SharedPreferences preferences) {
        Map<String,?> old=preferences.getAll();if(old.isEmpty())return;
        Map<String,Object> missing=new LinkedHashMap<>();SharedPreferences.Editor edit=preferences.edit();
        for(Map.Entry<String,Object> item:DEFAULTS.entrySet())if(!old.containsKey(item.getKey())) {
            missing.put(item.getKey(),item.getValue());StatusBarSettings.copyPreference(edit,item.getKey(),item.getValue());
        }
        if(!missing.isEmpty())PreferenceWrites.commit(preferences,edit,missing,false);
    }
    private Upgrade177(){ }
}
