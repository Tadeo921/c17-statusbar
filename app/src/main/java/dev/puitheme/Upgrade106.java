// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Schema 23: the same-app mini stack and its count badge switch, both off by default (native groups unchanged). */
final class Upgrade106 {
    static final Map<String, Object> DEFAULTS = defaults();
    private static Map<String, Object> defaults() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(NotificationMiniStack.ENABLED, false);
        values.put(NotificationMiniStack.HIDE_BADGE, false);
        return Collections.unmodifiableMap(values);
    }
    private Upgrade106() { }
}
