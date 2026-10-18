// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Schema 16 (code96): centred clock colons for every clock group, and the camera-area fluid cloud (capsule) page. */
final class Upgrade96 {
    static final Map<String, Object> DEFAULTS = defaults();

    private static Map<String, Object> defaults() {
        Map<String, Object> values = new LinkedHashMap<>();
        for (String group : ClockLayoutSettings.DIGIT_GROUPS) values.put(ClockLayoutSettings.colonKey(group), true);
        values.put(FluidCloudCapsule.MASTER, false);
        values.put(FluidCloudCapsule.FILL_ENABLED, false);
        values.put(FluidCloudCapsule.FILL_COLOR, FluidCloudCapsule.DEFAULT_FILL_COLOR);
        values.put(StatusBarSettings.alphaKey(FluidCloudCapsule.FILL_COLOR), false);
        values.put(FluidCloudCapsule.FILL_OPACITY, FluidCloudCapsule.DEFAULT_FILL_OPACITY);
        values.put(FluidCloudCapsule.FOLLOW_MEDIA, false);
        values.put(FluidCloudCapsule.BORDER_ENABLED, false);
        values.put(FluidCloudCapsule.BORDER_COLOR, FluidCloudCapsule.DEFAULT_BORDER_COLOR);
        values.put(StatusBarSettings.alphaKey(FluidCloudCapsule.BORDER_COLOR), true);
        values.put(FluidCloudCapsule.BORDER_WIDTH, FluidCloudCapsule.DEFAULT_BORDER_WIDTH);
        values.put(FluidCloudCapsule.RADIUS_ENABLED, false);
        values.put(FluidCloudCapsule.RADIUS, FluidCloudCapsule.DEFAULT_RADIUS);
        return Collections.unmodifiableMap(values);
    }

    private Upgrade96() { }
}
