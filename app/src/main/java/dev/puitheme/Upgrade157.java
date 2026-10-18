// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.content.SharedPreferences;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Schema 33: native 2x1 tile fill remains an independent, default-off choice. */
final class Upgrade157 {
    static final Map<String, Object> DEFAULTS = Collections.<String, Object>singletonMap(
            QsTileFullFill.ENABLED, false);

    static void migrate(SharedPreferences preferences) {
        Map<String, ?> old = preferences.getAll();
        if (old.isEmpty() || old.containsKey(QsTileFullFill.ENABLED)) return;
        PreferenceWrites.commit(preferences,
                preferences.edit().putBoolean(QsTileFullFill.ENABLED, false),
                new LinkedHashMap<>(DEFAULTS), false);
    }

    private Upgrade157() { }
}
