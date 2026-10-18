// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.content.SharedPreferences;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Schema 44 adds the four numbers that reshape the expanded fluid-cloud card's native gradient; the existing range is its end. */
final class Upgrade184 {
    static final Map<String,Object> DEFAULTS;
    static {
        Map<String,Object> defaults=new LinkedHashMap<>();
        defaults.put(FluidCloudStyle.BACKGROUND_GRADIENT_START,FluidCloudStyle.DEFAULT_GRADIENT_START);
        defaults.put(FluidCloudStyle.BACKGROUND_GRADIENT_MID,FluidCloudStyle.DEFAULT_GRADIENT_MID);
        defaults.put(FluidCloudStyle.BACKGROUND_GRADIENT_STRENGTH,FluidCloudStyle.DEFAULT_GRADIENT_STRENGTH);
        defaults.put(FluidCloudStyle.BACKGROUND_GRADIENT_OPACITY,FluidCloudStyle.DEFAULT_GRADIENT_OPACITY);
        DEFAULTS=Collections.unmodifiableMap(defaults);
    }
    /** The same four defaults as Float values, for the numeric registry. */
    static Map<String,Float> DEFAULTS_NUMBERS() {
        Map<String,Float> numbers=new LinkedHashMap<>();
        for(Map.Entry<String,Object> item:DEFAULTS.entrySet())numbers.put(item.getKey(),(Float)item.getValue());
        return numbers;
    }
    static void migrate(SharedPreferences preferences) {
        Map<String,?> old=preferences.getAll();if(old.isEmpty())return;
        Map<String,Object> missing=new LinkedHashMap<>();SharedPreferences.Editor edit=preferences.edit();
        for(Map.Entry<String,Object> item:DEFAULTS.entrySet())if(!old.containsKey(item.getKey())) {
            missing.put(item.getKey(),item.getValue());StatusBarSettings.copyPreference(edit,item.getKey(),item.getValue());
        }
        if(!missing.isEmpty())PreferenceWrites.commit(preferences,edit,missing,false);
    }
    private Upgrade184(){ }
}
