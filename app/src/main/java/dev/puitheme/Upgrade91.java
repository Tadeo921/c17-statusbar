// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Schema 14 (code91): the two-row clock owns its position, letter spacing and colours.
 * Colours come with their alpha-mode markers, like every colour in a complete snapshot. */
final class Upgrade91 {
    static final Map<String, Object> DEFAULTS = defaults();

    private static Map<String, Object> defaults() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(ClockLayoutSettings.DUAL_OFFSET_X, 0f);
        values.put(ClockLayoutSettings.DUAL_OFFSET_Y, 0f);
        values.put(ClockLayoutSettings.DUAL_SPACING, 0f);
        values.put(ClockLayoutSettings.DUAL_COLOR_ENABLED, false);
        values.put(ClockLayoutSettings.DUAL_COLOR_LIGHT, 0xff000000);
        values.put(ClockLayoutSettings.DUAL_COLOR_DARK, 0xffffffff);
        values.put(StatusBarSettings.alphaKey(ClockLayoutSettings.DUAL_COLOR_LIGHT), false);
        values.put(StatusBarSettings.alphaKey(ClockLayoutSettings.DUAL_COLOR_DARK), false);
        // Fluid-cloud appearance scopes and notification cards following the media colour.
        values.put(FluidCloudStyle.SCOPE_FLOAT, true);
        values.put(FluidCloudStyle.SCOPE_SHADE, false);
        values.put(FluidCloudStyle.SCOPE_LOCK, true);
        values.put(FluidCloudStyle.NOTIFICATION_FOLLOW, false);
        values.put(FluidCloudStyle.NOTIFICATION_FOLLOW_OPACITY, FluidCloudStyle.DEFAULT_NOTIFICATION_FOLLOW_OPACITY);
        return Collections.unmodifiableMap(values);
    }

    private Upgrade91() { }
}
