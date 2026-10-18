// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.content.SharedPreferences;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Schema 45 adds thirteen opt-out categories for fluid-cloud styling; all defaults preserve the old coverage. */
final class Upgrade185 {
    static final Map<String,Object> DEFAULTS;
    static {
        Map<String,Object> defaults=new LinkedHashMap<>();
        defaults.putAll(FluidCloudScope.BOOLEANS);
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
    private Upgrade185(){ }
}
