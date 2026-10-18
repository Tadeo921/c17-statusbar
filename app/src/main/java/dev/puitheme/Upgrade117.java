// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.content.SharedPreferences;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Schema 26: the notification page's big clock gains "no rebound" (portrait) and the lock-screen glass style with its strengths
 * (portrait and landscape), all off or at 100%. The lock-screen notification page and the network-generation texts reuse keys
 * that already exist, so they add nothing here.
 */
final class Upgrade117 {
    static final Map<String, Object> DEFAULTS = defaults();
    private static Map<String, Object> defaults() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(NotificationBigClockSettings.REBOUND_LOCK, false);
        for (String portrait : new String[]{NotificationBigClockSettings.LOCK_STYLE}) {
            values.put(portrait, false);
            values.put(NotificationBigClockSettings.landscapeKey(portrait), false);
        }
        values.put(NotificationBigClockSettings.LOCK_STYLE_NATIVE, true);
        values.put(NotificationBigClockSettings.landscapeKey(NotificationBigClockSettings.LOCK_STYLE_NATIVE), true);
        for (String portrait : new String[]{NotificationBigClockSettings.LOCK_STYLE_BODY,
                NotificationBigClockSettings.LOCK_STYLE_GRADIENT, NotificationBigClockSettings.LOCK_STYLE_RIM,
                NotificationBigClockSettings.LOCK_STYLE_RIM_DEPTH, NotificationBigClockSettings.LOCK_STYLE_ROUND,
                NotificationBigClockSettings.LOCK_STYLE_SHADE, NotificationBigClockSettings.LOCK_STYLE_GLOW}) {
            values.put(portrait, 100f);
            values.put(NotificationBigClockSettings.landscapeKey(portrait), 100f);
        }
        values.put(NotificationBigClockSettings.LOCK_STYLE_ANGLE, 300f);
        values.put(NotificationBigClockSettings.landscapeKey(NotificationBigClockSettings.LOCK_STYLE_ANGLE), 300f);
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
    private Upgrade117() { }
}
