// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** How the lock screen's and AOD's ordinary notifications are coloured, apart from the fluid-cloud cards. */
public final class LockNotificationSettings {
    /**
     * With no media playing there is no colour to follow, and the AOD card (the system's black mask over a black screen) looks nothing like
     * the awake lock card. This keeps the lock screen's own card material in AOD too: the same neutral glass and the same colour layers.
     */
    public static final String UNIFY_AOD = "lock_aod_unify_enabled";
    /** How strongly AOD's ordinary notifications follow the media colour (the lit screen has {@link FluidCloudStyle#NOTIFICATION_FOLLOW_OPACITY}). */
    public static final String AOD_NOTIFICATION_FOLLOW_OPACITY = "lock_aod_notification_follow_opacity";
    public static final Map<String, Boolean> BOOLEANS;
    public static final Map<String, Float> NUMBERS;
    static {
        Map<String, Boolean> booleans = new LinkedHashMap<>();
        booleans.put(UNIFY_AOD, false);
        booleans.put(AodNotificationBackground.ENABLED, false);
        BOOLEANS = Collections.unmodifiableMap(booleans);
        Map<String, Float> numbers = new LinkedHashMap<>();
        numbers.put(AOD_NOTIFICATION_FOLLOW_OPACITY, FluidCloudStyle.DEFAULT_NOTIFICATION_FOLLOW_OPACITY);
        NUMBERS = Collections.unmodifiableMap(numbers);
    }
    private LockNotificationSettings() { }
}
