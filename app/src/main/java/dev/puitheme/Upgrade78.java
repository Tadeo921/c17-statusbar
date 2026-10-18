// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Two portrait click actions; historical visual and material settings remain intact. */
final class Upgrade78 {
    static final Map<String,Object> DEFAULTS;
    static {
        Map<String,Object> values=new LinkedHashMap<>();
        values.put(NotificationBigClockSettings.CLOCK_CLICK_ENABLED,true);
        values.put(NotificationBigClockSettings.DATE_CLICK_ENABLED,true);
        DEFAULTS=Collections.unmodifiableMap(values);
    }
    private Upgrade78() { }
}
