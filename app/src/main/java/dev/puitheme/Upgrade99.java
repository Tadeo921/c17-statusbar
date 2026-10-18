// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Schema 18 (code99): the capsule's blur mask colour, six running-light colours with a count and an inward glow, the spectrum's
 * colour and its switch to follow the accent, the lock-screen white base, and the big clock's fade while its list springs back. */
final class Upgrade99 {
    static final Map<String, Object> DEFAULTS = defaults();

    private static Map<String, Object> defaults() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(FluidCloudCapsule.BLUR_MASK_COLOR, FluidCloudCapsule.DEFAULT_BLUR_MASK_COLOR);
        values.put(StatusBarSettings.alphaKey(FluidCloudCapsule.BLUR_MASK_COLOR), true);
        values.put(FluidCloudCapsule.MARQUEE_COLOR_4, FluidCloudCapsule.DEFAULT_MARQUEE_COLOR_4);
        values.put(FluidCloudCapsule.MARQUEE_COLOR_5, FluidCloudCapsule.DEFAULT_MARQUEE_COLOR_5);
        values.put(FluidCloudCapsule.MARQUEE_COLOR_6, FluidCloudCapsule.DEFAULT_MARQUEE_COLOR_6);
        values.put(StatusBarSettings.alphaKey(FluidCloudCapsule.MARQUEE_COLOR_4), false);
        values.put(StatusBarSettings.alphaKey(FluidCloudCapsule.MARQUEE_COLOR_5), false);
        values.put(StatusBarSettings.alphaKey(FluidCloudCapsule.MARQUEE_COLOR_6), false);
        values.put(FluidCloudCapsule.MARQUEE_COLOR_COUNT, (float) FluidCloudCapsule.DEFAULT_MARQUEE_COLOR_COUNT);
        values.put(FluidCloudCapsule.GLOW_ENABLED, false);
        values.put(FluidCloudCapsule.GLOW_DEPTH, FluidCloudCapsule.DEFAULT_GLOW_DEPTH);
        values.put(FluidCloudCapsule.SPECTRUM_COLOR, FluidCloudCapsule.DEFAULT_SPECTRUM_COLOR);
        values.put(StatusBarSettings.alphaKey(FluidCloudCapsule.SPECTRUM_COLOR), true);
        values.put(FluidCloudCapsule.SPECTRUM_FOLLOW, false);
        values.put(NotificationBigClockSettings.REBOUND_FADE, NotificationBigClockSettings.DEFAULT_REBOUND_FADE);
        values.put(FluidCloudAccent.LOCK_LIGHT, FluidCloudAccent.DEFAULT_LOCK_LIGHT);
        return Collections.unmodifiableMap(values);
    }

    private Upgrade99() { }
}
