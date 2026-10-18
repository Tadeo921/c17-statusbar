// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;
import android.content.SharedPreferences;
import java.util.Collections;
import java.util.Map;
/** Schema 41: optional portrait auxiliary row swap. Published older batches stay frozen. */
final class Upgrade174 {
    static final Map<String,Object> DEFAULTS=Collections.<String,Object>singletonMap(BigClockWidgetsSettings.SWAP_WITH_DATE,false);
    static void migrate(SharedPreferences preferences) {
        Map<String,?> old=preferences.getAll();
        if(old.isEmpty()||old.containsKey(BigClockWidgetsSettings.SWAP_WITH_DATE))return;
        SharedPreferences.Editor edit=preferences.edit();
        StatusBarSettings.copyPreference(edit,BigClockWidgetsSettings.SWAP_WITH_DATE,false);
        PreferenceWrites.commit(preferences,edit,DEFAULTS,false);
    }
    private Upgrade174(){ }
}
