// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.content.SharedPreferences;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Schema 29: camera-area persistence is opt-in and does not change appearance settings. */
final class Upgrade136 {
    static final Map<String, Object> DEFAULTS = Collections.<String, Object>singletonMap(CapsulePersistence.ENABLED, false);
    static void migrate(SharedPreferences preferences) {
        Map<String, ?> old = preferences.getAll();
        if (old.isEmpty() || old.containsKey(CapsulePersistence.ENABLED)) return;
        Map<String, Object> changed = new LinkedHashMap<>(DEFAULTS);
        PreferenceWrites.commit(preferences,
                preferences.edit().putBoolean(CapsulePersistence.ENABLED, false), changed, false);
    }
    private Upgrade136() { }
}
