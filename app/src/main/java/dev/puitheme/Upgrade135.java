// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.content.SharedPreferences;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Schema 28: independent proportional digits and time-change animation switches for both big-clock orientations. */
final class Upgrade135 {
    static final Map<String, Object> DEFAULTS = defaults();
    private static Map<String, Object> defaults() {
        Map<String, Object> values = new LinkedHashMap<>();
        for (String portrait : new String[]{NotificationBigClockSettings.PROPORTIONAL_DIGITS,
                NotificationBigClockSettings.TIME_ANIMATION_DISABLED}) {
            values.put(portrait, false);
            values.put(NotificationBigClockSettings.landscapeKey(portrait), false);
        }
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
    private Upgrade135() { }
}
