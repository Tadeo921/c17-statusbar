// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Schema 22: hidden control-centre top buttons may stay tappable, off by default (fully hidden as before). */
final class Upgrade105 {
    static final Map<String, Object> DEFAULTS = defaults();
    private static Map<String, Object> defaults() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(QsEntrances.HIDDEN_CLICKABLE, false);
        return Collections.unmodifiableMap(values);
    }
    private Upgrade105() { }
}
