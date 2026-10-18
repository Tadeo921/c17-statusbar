// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.content.SharedPreferences;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Schema 25: the shared network host gains independent left/right spacing, both zero by default. */
final class Upgrade108 {
    static final Map<String, Object> DEFAULTS = defaults();
    private static Map<String, Object> defaults() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(NetworkSlotGeometry.MARGIN_LEFT, 0f);
        values.put(NetworkSlotGeometry.MARGIN_RIGHT, 0f);
        return Collections.unmodifiableMap(values);
    }
    static void migrate(SharedPreferences preferences) {
        Map<String, ?> old = preferences.getAll();
        if (old.isEmpty()) return;
        Map<String, Object> changed = new LinkedHashMap<>();
        for (Map.Entry<String, Object> item : DEFAULTS.entrySet())
            if (!old.containsKey(item.getKey())) changed.put(item.getKey(), item.getValue());
        if (changed.isEmpty()) return;
        SharedPreferences.Editor editor = preferences.edit();
        for (Map.Entry<String, Object> item : changed.entrySet())
            StatusBarSettings.copyPreference(editor, item.getKey(), item.getValue());
        PreferenceWrites.commit(preferences, editor, changed, false);
    }
    private Upgrade108() { }
}
