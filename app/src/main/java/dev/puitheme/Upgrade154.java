// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.content.SharedPreferences;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Schema 32: the white native AOD notification background is an independent opt-in. */
final class Upgrade154 {
    static final Map<String, Object> DEFAULTS = Collections.<String, Object>singletonMap(
            AodNotificationBackground.ENABLED, false);

    static void migrate(SharedPreferences preferences) {
        Map<String, ?> old = preferences.getAll();
        if (old.isEmpty() || old.containsKey(AodNotificationBackground.ENABLED)) return;
        PreferenceWrites.commit(preferences,
                preferences.edit().putBoolean(AodNotificationBackground.ENABLED, false),
                new LinkedHashMap<>(DEFAULTS), false);
    }

    private Upgrade154() { }
}
