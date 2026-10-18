// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Schema 17 (code98): the lock-screen capsule switch, the solid/gradient colour style, the camera-area capsule's gradient,
 * blur, running light and real-time spectrum, and the big clock's blur while its list springs back. */
final class Upgrade98 {
    static final Map<String, Object> DEFAULTS = defaults();

    private static Map<String, Object> defaults() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(FluidCloudStyle.LOCK_CAPSULE, false);
        values.put(FluidCloudStyle.COLOR_STYLE, FluidCloudStyle.SOLID);
        values.put(FluidCloudCapsule.GRADIENT_ENABLED, false);
        values.put(FluidCloudCapsule.GRADIENT_MODE, FluidCloudCapsule.TOP_BOTTOM);
        values.put(FluidCloudCapsule.GRADIENT_COLOR, FluidCloudCapsule.DEFAULT_GRADIENT_COLOR);
        values.put(StatusBarSettings.alphaKey(FluidCloudCapsule.GRADIENT_COLOR), false);
        values.put(FluidCloudCapsule.BLUR_ENABLED, false);
        values.put(FluidCloudCapsule.BLUR_RADIUS, FluidCloudCapsule.DEFAULT_BLUR_RADIUS);
        values.put(FluidCloudCapsule.MARQUEE_ENABLED, false);
        values.put(FluidCloudCapsule.MARQUEE_PRESET, FluidCloudCapsule.DEFAULT_MARQUEE_PRESET);
        values.put(FluidCloudCapsule.MARQUEE_COLOR_1, FluidCloudCapsule.DEFAULT_MARQUEE_COLOR_1);
        values.put(FluidCloudCapsule.MARQUEE_COLOR_2, FluidCloudCapsule.DEFAULT_MARQUEE_COLOR_2);
        values.put(FluidCloudCapsule.MARQUEE_COLOR_3, FluidCloudCapsule.DEFAULT_MARQUEE_COLOR_3);
        // The running light's colours are opaque; its opacity is its own setting.
        values.put(StatusBarSettings.alphaKey(FluidCloudCapsule.MARQUEE_COLOR_1), false);
        values.put(StatusBarSettings.alphaKey(FluidCloudCapsule.MARQUEE_COLOR_2), false);
        values.put(StatusBarSettings.alphaKey(FluidCloudCapsule.MARQUEE_COLOR_3), false);
        values.put(FluidCloudCapsule.MARQUEE_OPACITY, FluidCloudCapsule.DEFAULT_MARQUEE_OPACITY);
        values.put(FluidCloudCapsule.MARQUEE_PERIOD, FluidCloudCapsule.DEFAULT_MARQUEE_PERIOD);
        values.put(FluidCloudCapsule.MARQUEE_REVERSE, false);
        values.put(FluidCloudCapsule.SPECTRUM_ENABLED, false);
        values.put(FluidCloudCapsule.SPECTRUM_SENSITIVITY, FluidCloudCapsule.DEFAULT_SPECTRUM_SENSITIVITY);
        values.put(NotificationBigClockSettings.REBOUND_BLUR_ENABLED, false);
        values.put(NotificationBigClockSettings.REBOUND_BLUR_RADIUS, NotificationBigClockSettings.DEFAULT_REBOUND_BLUR_RADIUS);
        return Collections.unmodifiableMap(values);
    }

    private Upgrade98() { }
}
