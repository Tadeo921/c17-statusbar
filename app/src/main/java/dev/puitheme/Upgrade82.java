// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
/** Separate network-grade formatting batch; no existing selection is overwritten. */
final class Upgrade82 {
    static final Map<String,Object> DEFAULTS;
    static {
        Map<String,Object> values=new LinkedHashMap<>();
        values.put(NetworkLabelFormats.ENABLED,false);values.putAll(NetworkLabelFormats.STRINGS);
        DEFAULTS=Collections.unmodifiableMap(values);
    }
    private Upgrade82(){ }
}
