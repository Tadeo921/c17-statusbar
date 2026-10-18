// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.content.SharedPreferences;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** V3 owns one independent record. The application retires old resources; SystemUI never migrates files. */
final class Upgrade187 {
    static final Map<String,Object> DEFAULTS=Collections.<String,Object>singletonMap(IconManagementSettings.KEY,IconManagementSettings.DEFAULT);
    static void migrate(SharedPreferences preferences) {
        Map<String,?> old=preferences.getAll();if(old.isEmpty()||old.containsKey(IconManagementSettings.KEY))return;
        Map<String,Object> missing=new LinkedHashMap<>(DEFAULTS);SharedPreferences.Editor edit=preferences.edit();
        StatusBarSettings.copyPreference(edit,IconManagementSettings.KEY,IconManagementSettings.DEFAULT);
        PreferenceWrites.commit(preferences,edit,missing,false);
    }
    private Upgrade187(){ }
}
