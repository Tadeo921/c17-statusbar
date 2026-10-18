// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.content.SharedPreferences;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Schema 30 adds opt-in layout fields without interpreting or discarding old drawing offsets. */
final class Upgrade144 {
    static final Map<String, Object> DEFAULTS;
    static {
        Map<String, Object> defaults = new LinkedHashMap<>();
        // Freeze this historical wire batch; later layout options belong to their own schema.
        defaults.put("statusbar_layout_enabled", false);
        for (String token : new String[]{"clock", "notification", "carrier", "speed", "wifi", "mobile", "battery", "system"}) {
            defaults.put("statusbar_layout_" + token + "_side", "native");
            defaults.put("statusbar_layout_" + token + "_order", 0f);
            defaults.put("statusbar_layout_" + token + "_margin_left", 0f);
            defaults.put("statusbar_layout_" + token + "_margin_right", 0f);
        }
        defaults.put("statusbar_layout_label_order", 0f);
        defaults.put("statusbar_layout_label_margin_left", 0f);
        defaults.put("statusbar_layout_label_margin_right", 0f);
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
    private Upgrade144() { }
}
