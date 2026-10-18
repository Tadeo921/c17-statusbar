// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;
import android.content.SharedPreferences;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
/** Schema 39: notification glyphs stay flat until their own relief opt-in. */
final class Upgrade172 {
    static final Map<String,Object> DEFAULTS=Collections.<String,Object>singletonMap(ClockBackdropSettings.NOTIFICATION_RELIEF,false);
    static void migrate(SharedPreferences preferences) {
        Map<String,?> old=preferences.getAll();if(old.isEmpty()||old.containsKey(ClockBackdropSettings.NOTIFICATION_RELIEF))return;
        Map<String,Object> missing=new LinkedHashMap<>(DEFAULTS);SharedPreferences.Editor edit=preferences.edit();
        StatusBarSettings.copyPreference(edit,ClockBackdropSettings.NOTIFICATION_RELIEF,false);
        PreferenceWrites.commit(preferences,edit,missing,false);
    }
    private Upgrade172(){ }
}
