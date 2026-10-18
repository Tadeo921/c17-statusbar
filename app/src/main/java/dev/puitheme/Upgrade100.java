// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Schema 19 (code100): transition ranges for every gradient (capsule, lock-screen music card and capsule, control-centre tiles,
 * clear button portrait and landscape), the camera capsule's album-cover radius, and the control centre's top buttons. Every new
 * value keeps the old look (ranges 100 %, switches off). */
final class Upgrade100 {
    static final Map<String, Object> DEFAULTS = defaults();

    private static Map<String, Object> defaults() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(FluidCloudAccent.GRADIENT_RANGE, FluidCloudAccent.DEFAULT_GRADIENT_RANGE);
        values.put(FluidCloudCapsule.GRADIENT_RANGE, FluidCloudCapsule.DEFAULT_GRADIENT_RANGE);
        values.put(FluidCloudCapsule.COVER_RADIUS_ENABLED, false);
        values.put(FluidCloudCapsule.COVER_RADIUS, FluidCloudCapsule.DEFAULT_COVER_RADIUS);
        values.put(CapsuleEffects.BOUNCE_ENABLED, false);
        values.put(CapsuleEffects.BOUNCE, CapsuleEffects.DEFAULT_BOUNCE);
        values.put(QsTileAppearance.prefix("light") + "_gradient_range", 100f);
        values.put(QsTileAppearance.prefix("dark") + "_gradient_range", 100f);
        values.put(NotificationClearAppearance.GRADIENT_RANGE, 100f);
        values.put(NotificationClearAppearance.LANDSCAPE_GRADIENT_RANGE, 100f);
        values.put(SignalResources.ROAMING_HIDDEN, false);
        values.put(ResidentCache.INSTALLED, false);
        values.put(ResidentCache.RUNNING, false);
        values.put(QsEntrances.MASTER, false);
        values.put(QsEntrances.EDIT_HIDDEN, false);
        values.put(QsEntrances.SETTINGS_HIDDEN, false);
        return Collections.unmodifiableMap(values);
    }

    private Upgrade100() { }
}
