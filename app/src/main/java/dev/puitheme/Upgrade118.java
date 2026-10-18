// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.content.SharedPreferences;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Schema 27: how the lock screen and AOD show ordinary notifications (flat in AOD, the same card colour with no media), and the
 * pull-down panel wallpaper's blur, the "blur with the pull" switch and the notification centre's clock zone, and the music card's
 * background source and tint. The wallpaper's own
 * switches, brightness and pictures belong to the first release of that feature and are not repeated here.
 */
final class Upgrade118 {
    static final Map<String, Object> DEFAULTS = defaults();
    private static Map<String, Object> defaults() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.putAll(LockNotificationSettings.BOOLEANS);
        // Keep this schema27 batch frozen as the settings page gains later options.
        values.remove(AodNotificationBackground.ENABLED);
        for (Map.Entry<String, Integer> item : ShadeWallpaperSettings.COLORS.entrySet()) {
            values.put(item.getKey(), item.getValue());
            values.put(StatusBarSettings.alphaKey(item.getKey()), false);
        }
        values.put(QsMediaAppearance.TRANSITION, QsMediaAppearance.DEFAULT_TRANSITION_MS);
        values.put(QsMediaAppearance.SETTLE, QsMediaAppearance.DEFAULT_SETTLE_MS);
        // AOD gets a look of its own (defaults equal to the lit lock screen's; an upgrade copies what the user had set there).
        values.put(FluidCloudAccent.AOD_CUSTOM, false);
        values.put(FluidCloudAccent.AOD_GRADIENT, false);
        values.put(FluidCloudAccent.AOD_OPACITY, FluidCloudAccent.DEFAULT_LOCK_OPACITY);
        values.put(FluidCloudAccent.AOD_MASK_OPACITY, FluidCloudAccent.DEFAULT_LOCK_MASK_OPACITY);
        values.put(FluidCloudAccent.AOD_LIGHT, FluidCloudAccent.DEFAULT_LOCK_LIGHT);
        values.put(FluidCloudAccent.AOD_MASK_COLOR, FluidCloudAccent.DEFAULT_MASK_COLOR);
        values.put(StatusBarSettings.alphaKey(FluidCloudAccent.AOD_MASK_COLOR), false);
        values.putAll(LockNotificationSettings.NUMBERS);
        values.put(SpeedPosition.EDGE_RESERVE, SpeedPosition.DEFAULT_RESERVE);
        Map<String, Object> first = ShadeWallpaperSettings.firstReleaseDefaults();
        for (Map.Entry<String, Boolean> item : ShadeWallpaperSettings.BOOLEANS.entrySet()) if (!first.containsKey(item.getKey())) values.put(item.getKey(), item.getValue());
        for (Map.Entry<String, Float> item : ShadeWallpaperSettings.NUMBERS.entrySet()) if (!first.containsKey(item.getKey())) values.put(item.getKey(), item.getValue());
        for (Map.Entry<String, String> item : ShadeWallpaperSettings.STRINGS.entrySet()) if (!first.containsKey(item.getKey())) values.put(item.getKey(), item.getValue());
        // The control-centre music card: where its background comes from and the colour laid over it (all off).
        for (String scene : new String[]{"light", "dark"}) {
            for (String key : QsMediaAppearance.backgroundOptionKeys(scene)) {
                if (QsMediaAppearance.BOOLEANS.containsKey(key)) values.put(key, QsMediaAppearance.BOOLEANS.get(key));
                else if (QsMediaAppearance.NUMBERS.containsKey(key)) values.put(key, QsMediaAppearance.NUMBERS.get(key));
                else {
                    values.put(key, QsMediaAppearance.COLORS.get(key));
                    values.put(StatusBarSettings.alphaKey(key), false);
                }
            }
        }
        return Collections.unmodifiableMap(values);
    }
    /** AOD's keys and the lit lock screen's keys they start as a copy of. */
    private static final String[][] AOD_FROM_LOCK = {
        {FluidCloudAccent.AOD_CUSTOM, FluidCloudAccent.LOCK_CUSTOM}, {FluidCloudAccent.AOD_GRADIENT, FluidCloudAccent.LOCK_GRADIENT},
        {FluidCloudAccent.AOD_OPACITY, FluidCloudAccent.LOCK_OPACITY}, {FluidCloudAccent.AOD_MASK_OPACITY, FluidCloudAccent.LOCK_MASK_OPACITY},
        {FluidCloudAccent.AOD_LIGHT, FluidCloudAccent.LOCK_LIGHT}, {FluidCloudAccent.AOD_MASK_COLOR, FluidCloudAccent.LOCK_MASK_COLOR},
        {StatusBarSettings.alphaKey(FluidCloudAccent.AOD_MASK_COLOR), StatusBarSettings.alphaKey(FluidCloudAccent.LOCK_MASK_COLOR)},
        {LockNotificationSettings.AOD_NOTIFICATION_FOLLOW_OPACITY, FluidCloudStyle.NOTIFICATION_FOLLOW_OPACITY}};
    /** Overwrite AOD's values in {@code target} with what {@code source} holds for the lit lock screen (only those it holds). */
    static void carryLockLook(Map<String, Object> target, Map<String, ?> source) {
        for (String[] pair : AOD_FROM_LOCK) {
            Object value = source.get(pair[1]);
            if (value != null && target.containsKey(pair[0])) target.put(pair[0], value);
        }
    }
    static void migrate(SharedPreferences preferences) {
        Map<String, ?> old = preferences.getAll();
        if (old.isEmpty()) return;
        Map<String, Object> changed = new LinkedHashMap<>();
        for (Map.Entry<String, Object> item : DEFAULTS.entrySet())
            if (!old.containsKey(item.getKey())) changed.put(item.getKey(), item.getValue());
        // AOD had no look of its own: where it is new it starts as a copy of what the lit lock screen has, so nothing changes until edited.
        Map<String, Object> carried = new LinkedHashMap<>(changed);
        carryLockLook(carried, old);
        changed.putAll(carried);
        // The first release's one switch is now one per orientation: an "on" stays on for both.
        if (Boolean.TRUE.equals(old.get(ShadeWallpaperSettings.MASTER))) {
            if (changed.containsKey(ShadeWallpaperSettings.PORTRAIT_MASTER)) changed.put(ShadeWallpaperSettings.PORTRAIT_MASTER, true);
            if (changed.containsKey(ShadeWallpaperSettings.LANDSCAPE_MASTER)) changed.put(ShadeWallpaperSettings.LANDSCAPE_MASTER, true);
        }
        if (changed.isEmpty()) return;
        SharedPreferences.Editor editor = preferences.edit();
        for (Map.Entry<String, Object> item : changed.entrySet())
            StatusBarSettings.copyPreference(editor, item.getKey(), item.getValue());
        PreferenceWrites.commit(preferences, editor, changed, false);
    }
    private Upgrade118() { }
}
