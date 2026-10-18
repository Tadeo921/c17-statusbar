// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Schema 20 (code101): "始终显示网络制式文字" (off) and "Wi-Fi 与网络制式文字合并" (on, as asked for). code100 phones already hold
 * schema-19 copies without them, so they are their own batch. */
final class Upgrade101 {
    static final Map<String, Object> DEFAULTS = defaults();

    private static Map<String, Object> defaults() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(RadioState.ALWAYS_SHOWN, false);
        values.put(RadioState.WIFI_MERGED, true);
        return Collections.unmodifiableMap(values);
    }

    private Upgrade101() { }
}
