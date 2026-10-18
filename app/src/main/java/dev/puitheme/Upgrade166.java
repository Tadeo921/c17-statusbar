// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.content.SharedPreferences;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Schema 35: independent mini-stack child policy and opt-in per-slot hint placement. */
final class Upgrade166 {
    static final Map<String,Object> DEFAULTS;
    static {
        Map<String,Object> values=new LinkedHashMap<>();
        values.put(NotificationMiniStack.AUTO_HIDE_IN_STACK,true);
        values.put(NativeStatusIcons.LAYOUT,"{}");
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
        if(!missing.isEmpty())PreferenceWrites.commit(preferences,editor,missing,false);
    }

    private Upgrade166() { }
}
