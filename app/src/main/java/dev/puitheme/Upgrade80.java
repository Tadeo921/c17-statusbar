// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Exact material options batch. An old transparent background remains transparent. */
final class Upgrade80 {
    static final Map<String,Object> DEFAULTS;
    static {
        Map<String,Object> values=new LinkedHashMap<>();
        values.put(FluidCloudStyle.MEDIA_KEEP_HIGHLIGHT,false);
        values.put(FluidCloudStyle.FOLLOW_FOREGROUND,false);
        values.put(FluidCloudStyle.BACKGROUND_MODE,"native");
        values.put(FluidCloudStyle.BACKGROUND_COLOR,0xff303438);
        values.put(StatusBarSettings.alphaKey(FluidCloudStyle.BACKGROUND_COLOR),false);
        values.put(FluidCloudStyle.BACKGROUND_OPACITY,100f);
        DEFAULTS=Collections.unmodifiableMap(values);
    }
    static boolean legacyBackground(Map<String,?> values) {
        return values!=null&&!values.containsKey(FluidCloudStyle.BACKGROUND_MODE)
                &&Boolean.TRUE.equals(values.get(FluidCloudStyle.DESKTOP_BACKGROUND));
    }
    static Map<String,Object> inheritedDefaults(Map<String,?> values) {
        Map<String,Object> result=new LinkedHashMap<>(DEFAULTS);
        if(legacyBackground(values)) {
            result.put(FluidCloudStyle.BACKGROUND_MODE,"custom");
            result.put(FluidCloudStyle.BACKGROUND_OPACITY,0f);
        }
        return result;
    }
    private Upgrade80() { }
}
