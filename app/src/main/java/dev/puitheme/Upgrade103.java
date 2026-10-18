// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Schema 21: an optional music-cover scale, leaving the native size until enabled. */
final class Upgrade103 {
    static final Map<String, Object> DEFAULTS = defaults();
    private static Map<String, Object> defaults() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(FluidCloudCapsule.COVER_SIZE_ENABLED, false);
        values.put(FluidCloudCapsule.COVER_SCALE, FluidCloudCapsule.DEFAULT_COVER_SCALE);
        return Collections.unmodifiableMap(values);
    }
    private Upgrade103() { }
}
