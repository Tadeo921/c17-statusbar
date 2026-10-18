// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
/** Retired code175 parent keys. Only frozen schema42 migration and lossless
 * configuration compatibility remain; these keys never gate runtime effects. */
public final class FeatureGroups {
    public static final String LAYOUT="statusbar_layout_adjustments_enabled";
    public static final String STACKS="notification_stack_effects_enabled";
    public static final String CLEAR="notification_clear_controls_enabled";
    public static final String LOCK="lock_notification_effects_enabled";
    public static final Map<String,Boolean> BOOLEANS;
    static {Map<String,Boolean> values=new LinkedHashMap<>();values.put(LAYOUT,true);values.put(STACKS,true);values.put(CLEAR,true);values.put(LOCK,true);BOOLEANS=Collections.unmodifiableMap(values);}
    private FeatureGroups() { }
}
