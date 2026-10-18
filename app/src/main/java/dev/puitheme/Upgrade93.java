// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Schema 15 (code93): the media-accent page owns the fill opacity of the colour it applies, following the
 * media colour only while something plays, and a lock-screen custom opacity and mask. */
final class Upgrade93 {
    static final Map<String, Object> DEFAULTS = defaults();

    private static Map<String, Object> defaults() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(FluidCloudStyle.FOLLOW_ONLY_PLAYING, true);
        values.put(FluidCloudAccent.OPACITY, FluidCloudAccent.DEFAULT_OPACITY);
        values.put(FluidCloudAccent.LOCK_CUSTOM, false);
        values.put(FluidCloudAccent.LOCK_GRADIENT, false);
        values.put(FluidCloudAccent.LOCK_OPACITY, FluidCloudAccent.DEFAULT_LOCK_OPACITY);
        values.put(FluidCloudAccent.LOCK_MASK_COLOR, FluidCloudAccent.DEFAULT_MASK_COLOR);
        values.put(StatusBarSettings.alphaKey(FluidCloudAccent.LOCK_MASK_COLOR), false);
        values.put(FluidCloudAccent.LOCK_MASK_OPACITY, FluidCloudAccent.DEFAULT_LOCK_MASK_OPACITY);
        return Collections.unmodifiableMap(values);
    }

    private Upgrade93() { }
}
