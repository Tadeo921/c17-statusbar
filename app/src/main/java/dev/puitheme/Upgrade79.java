// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** App selection and desktop-themed notifications; preserve active legacy fluid styles. */
final class Upgrade79 {
    static final Map<String,Object> DEFAULTS;
    static {
        Map<String,Object> values=new LinkedHashMap<>();
        values.put(NotificationBigClockSettings.CLOCK_LAUNCH_APP, "");
        values.put(NotificationBigClockSettings.DATE_LAUNCH_APP, "");
        values.put(NotificationIconArea.FOLLOW_LAUNCHER, false);
        values.put(FluidCloudStyle.MASTER, false);
        DEFAULTS=Collections.unmodifiableMap(values);
    }
    static boolean legacyStyleEnabled(Map<String,?> values) {
        return values!=null && (Boolean.TRUE.equals(values.get(FluidCloudStyle.FOLLOW_NOTIFICATION))
                || Boolean.TRUE.equals(values.get(FluidCloudStyle.DESKTOP_HIGHLIGHTS))
                || Boolean.TRUE.equals(values.get(FluidCloudStyle.DESKTOP_BACKGROUND)));
    }
    static Map<String,Object> inheritedDefaults(Map<String,?> values) {
        Map<String,Object> result=new LinkedHashMap<>(DEFAULTS);
        result.put(FluidCloudStyle.MASTER,legacyStyleEnabled(values));
        return result;
    }
    private Upgrade79() { }
}
