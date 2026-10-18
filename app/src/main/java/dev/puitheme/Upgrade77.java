// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Exact schema-7 batch; all existing music accent, mask and material values survive. */
final class Upgrade77 {
    static final Map<String,Object> DEFAULTS;
    static {
        Map<String,Object> values=new LinkedHashMap<>();
        values.put(FluidCloudAccent.LOCKSCREEN_ENABLED,false);
        DEFAULTS=Collections.unmodifiableMap(values);
    }
    private Upgrade77() { }
}
