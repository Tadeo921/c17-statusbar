// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.content.SharedPreferences;
import java.util.Collections;
import java.util.Map;

/** Existing v3 choices keep their behavior; the new pause switch never edits a record or file. */
final class Upgrade188 {
    static final Map<String,Object> DEFAULTS=Collections.<String,Object>singletonMap(IconManagementSettings.ENABLED,true);
    static void migrate(SharedPreferences preferences){
        Map<String,?> old=preferences.getAll();if(old.isEmpty()||old.containsKey(IconManagementSettings.ENABLED))return;
        SharedPreferences.Editor edit=preferences.edit().putBoolean(IconManagementSettings.ENABLED,true);
        PreferenceWrites.commit(preferences,edit,DEFAULTS,false);
    }
    private Upgrade188(){ }
}
