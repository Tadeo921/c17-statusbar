// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Exact schema-6 addition batch. Existing accent and material settings remain untouched. */
final class Upgrade76 {
    static final Map<String,Object> DEFAULTS;
    static {
        Map<String,Object> values = new LinkedHashMap<>();
        values.put(FluidCloudStyle.FOLLOW_NOTIFICATION, false);
        values.put(FluidCloudStyle.DESKTOP_HIGHLIGHTS, false);
        values.put(FluidCloudStyle.DESKTOP_BACKGROUND, false);
        values.put(FluidCloudAccent.MASK_ENABLED, false);
        values.put(FluidCloudAccent.MASK_COLOR, 0xff000000);
        values.put(StatusBarSettings.alphaKey(FluidCloudAccent.MASK_COLOR), false);
        values.put(FluidCloudAccent.MASK_OPACITY, 20f);
        DEFAULTS = Collections.unmodifiableMap(values);
    }
    private Upgrade76() { }
}
