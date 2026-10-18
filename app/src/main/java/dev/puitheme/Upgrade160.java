// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.content.SharedPreferences;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Schema 34: page-turn styles (iOS by default) and the big clock's opt-in see-through. */
final class Upgrade160 {
    static final Map<String, Object> DEFAULTS;
    static {
        Map<String, Object> defaults = new LinkedHashMap<>();
        // Freeze this historical wire batch; later options belong to their own schema.
        defaults.put(TilePageEffects.STYLE, TilePageEffects.IOS);
        for (String prefix : new String[]{"notification_big_clock_", "notification_big_clock_landscape_"}) {
            defaults.put(prefix + "reveal_enabled", false);
            defaults.put(prefix + "reveal_amount", 100f);
            defaults.put(prefix + "reveal_feather", 48f);
            defaults.put(prefix + "digit_reveal_enabled", false);
            defaults.put(prefix + "digit_reveal_amount", 60f);
        }
        DEFAULTS = Collections.unmodifiableMap(defaults);
    }

    static void migrate(SharedPreferences preferences) {
        Map<String, ?> old = preferences.getAll();
        if (old.isEmpty()) return;
        Map<String, Object> missing = new LinkedHashMap<>();
        SharedPreferences.Editor editor = preferences.edit();
        for (Map.Entry<String, Object> entry : DEFAULTS.entrySet()) if (!old.containsKey(entry.getKey())) {
            missing.put(entry.getKey(), entry.getValue());
            StatusBarSettings.copyPreference(editor, entry.getKey(), entry.getValue());
        }
        if (!missing.isEmpty()) PreferenceWrites.commit(preferences, editor, missing, false);
    }

    private Upgrade160() { }
}
