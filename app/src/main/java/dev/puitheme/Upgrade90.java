// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Schema 13 (code90): clock digit alignment, two rows and margins. This batch is fixed history;
 * later additions belong to Upgrade91. Older copies receive these defaults. */
final class Upgrade90 {
    static final Map<String, Object> DEFAULTS = defaults();

    private static Map<String, Object> defaults() {
        Map<String, Object> values = new LinkedHashMap<>();
        for (String group : ClockLayoutSettings.DIGIT_GROUPS) {
            values.put(ClockLayoutSettings.digitKey(group), true);
            values.put(ClockLayoutSettings.digitSpacingKey(group), 0f);
        }
        values.put(ClockLayoutSettings.DUAL_ENABLED, false);
        values.put(ClockLayoutSettings.MARGIN_ENABLED, false);
        values.put(ClockLayoutSettings.DUAL_UPPER_SCALE, 72f);
        values.put(ClockLayoutSettings.DUAL_LOWER_SCALE, 56f);
        values.put(ClockLayoutSettings.DUAL_UPPER_WEIGHT, 600f);
        values.put(ClockLayoutSettings.DUAL_LOWER_WEIGHT, 500f);
        values.put(ClockLayoutSettings.DUAL_LINE_GAP, 0f);
        values.put(ClockLayoutSettings.MARGIN_START, 0f);
        values.put(ClockLayoutSettings.MARGIN_END, 0f);
        values.put(ClockLayoutSettings.DUAL_UPPER_PATTERN, ClockLayoutSettings.DEFAULT_UPPER_PATTERN);
        values.put(ClockLayoutSettings.DUAL_LOWER_PATTERN, ClockLayoutSettings.DEFAULT_LOWER_PATTERN);
        values.put(ClockLayoutSettings.DUAL_ALIGN, ClockLayoutSettings.ALIGN_CENTER);
        return Collections.unmodifiableMap(values);
    }

    private Upgrade90() { }
}
