// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.content.SharedPreferences;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Schema 24: one network slot with an explicit both-visible opt-in and shared position. */
final class Upgrade107 {
    static final Map<String, Object> DEFAULTS = from(Collections.emptyMap());
    static Map<String, Object> from(Map<String, ?> source) {
        Map<String, ?> values = source == null ? Collections.emptyMap() : source;
        boolean label = Boolean.TRUE.equals(values.get("label_enabled"))
                && !Boolean.FALSE.equals(values.get("label_position_enabled"));
        boolean wifi = Boolean.TRUE.equals(values.get("wifi_enabled"))
                && !Boolean.FALSE.equals(values.get("wifi_position_enabled"));
        String x = label ? StatusBarSettings.LABEL_OFFSET_X : StatusBarSettings.WIFI_OFFSET_X;
        String y = label ? StatusBarSettings.LABEL_OFFSET_Y : StatusBarSettings.WIFI_OFFSET_Y;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put(RadioState.BOTH_VISIBLE, false);
        result.put(NetworkSlotGeometry.POSITION, label || wifi);
        result.put(NetworkSlotGeometry.OFFSET_X, label || wifi ? StatusBarSettings.settingNumber(values, x, 0f) : 0f);
        result.put(NetworkSlotGeometry.OFFSET_Y, label || wifi ? StatusBarSettings.settingNumber(values, y, 0f) : 0f);
        return Collections.unmodifiableMap(result);
    }
    static void migrate(SharedPreferences preferences) {
        Map<String, ?> old = preferences.getAll();
        Map<String, Object> changed = new LinkedHashMap<>();
        for (Map.Entry<String, Object> item : from(old).entrySet())
            if (!old.containsKey(item.getKey())) changed.put(item.getKey(), item.getValue());
        if (changed.isEmpty()) return;
        SharedPreferences.Editor editor = preferences.edit();
        for (Map.Entry<String, Object> item : changed.entrySet())
            StatusBarSettings.copyPreference(editor, item.getKey(), item.getValue());
        PreferenceWrites.commit(preferences, editor, changed, false);
    }
    /** Old portable pages have no shared keys. Project an explicitly active old position;
     * unrelated/offset-only partial files keep their original scope, and explicit new values win. */
    static void importLegacy(Map<String, Object> values) {
        String prefix = legacyPosition(values, "label") ? "label" : legacyPosition(values, "wifi") ? "wifi" : null;
        if (prefix == null) return;
        if (!values.containsKey(NetworkSlotGeometry.POSITION)) values.put(NetworkSlotGeometry.POSITION, true);
        String x = prefix + "_offset_x", y = prefix + "_offset_y";
        if (!values.containsKey(NetworkSlotGeometry.OFFSET_X) && values.containsKey(x))
            values.put(NetworkSlotGeometry.OFFSET_X, values.get(x));
        if (!values.containsKey(NetworkSlotGeometry.OFFSET_Y) && values.containsKey(y))
            values.put(NetworkSlotGeometry.OFFSET_Y, values.get(y));
    }
    private static boolean legacyPosition(Map<String, Object> values, String prefix) {
        return !Boolean.FALSE.equals(values.get(prefix + "_enabled"))
                && !Boolean.FALSE.equals(values.get(prefix + "_position_enabled"))
                && (Boolean.TRUE.equals(values.get(prefix + "_enabled"))
                    || Boolean.TRUE.equals(values.get(prefix + "_position_enabled")))
                && (values.containsKey(prefix + "_position_enabled")
                    || values.containsKey(prefix + "_offset_x") || values.containsKey(prefix + "_offset_y"));
    }
    private Upgrade107() { }
}
