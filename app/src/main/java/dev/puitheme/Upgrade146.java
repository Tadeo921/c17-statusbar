// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.content.SharedPreferences;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Schema 31: Wi-Fi and the network-type label may opt in to one relative slot. */
final class Upgrade146 {
    static final Map<String, Object> DEFAULTS = Collections.<String, Object>singletonMap(
            StatusBarLayoutSettings.WIFI_LABEL_SHARED, false);

    static void migrate(SharedPreferences preferences) {
        Map<String, ?> old = preferences.getAll();
        if (old.isEmpty() || old.containsKey(StatusBarLayoutSettings.WIFI_LABEL_SHARED)) return;
        PreferenceWrites.commit(preferences,
                preferences.edit().putBoolean(StatusBarLayoutSettings.WIFI_LABEL_SHARED, false),
                new LinkedHashMap<>(DEFAULTS), false);
    }

    private Upgrade146() { }
}
