// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** The camera-area capsule's own appear bounce, made adjustable. When a capsule appears, the plugin first grows it past its final size
 * by capsule_appear_extra_expansion_width (16 dp) and _height (3 dp) and then lets it settle; it reads both values with
 * Resources.getDimensionPixelOffset every time it builds that animation (audited in SystemUIPlugin 17000002). Scaling exactly those two
 * reads, and only from the plugin's own Resources, changes the bounce of the next appearance: 0 % has none, 100 % is the plugin's own.
 * Nothing else the plugin reads is touched, and the capsule's position over the camera is not part of these values. */
final class CapsuleEffects {
    static final String BOUNCE_ENABLED = "fluid_cloud_capsule_bounce_enabled";
    static final String BOUNCE = "fluid_cloud_capsule_bounce";
    static final float DEFAULT_BOUNCE = 100f, MAX_BOUNCE = 200f;
    static final String WIDTH = "capsule_appear_extra_expansion_width", HEIGHT = "capsule_appear_extra_expansion_height";

    private volatile boolean active;
    private volatile float factor = 1f;
    private volatile Resources plugin;
    /** Ids of the plugin's Resources by entry name, learnt on first read: a SystemUI resource with the same number is never touched. */
    private final Map<Integer, Boolean> scaled = Collections.synchronizedMap(new HashMap<Integer, Boolean>());

    void configure(Bundle settings) {
        boolean master = settings != null && settings.getBoolean(FluidCloudCapsule.MASTER, false);
        boolean on = master && settings.getBoolean(BOUNCE_ENABLED, false) && !SafetyMode.enabled(settings);
        float percent = settings == null ? DEFAULT_BOUNCE : NumericPolicy.finite(settings.get(BOUNCE), DEFAULT_BOUNCE);
        factor = Math.max(0f, Math.min(MAX_BOUNCE, percent)) / 100f;
        active = on;
    }

    /** A bounce other than the system's own is wanted: only then is the resource read hooked at all. */
    boolean active() { return active && factor != 1f; }

    /** The plugin's Resources, taken from a capsule it inflated. */
    void capsuleSeen(View capsule) {
        if (capsule == null) return;
        Resources resources = capsule.getResources();
        if (resources != null && resources != plugin) { plugin = resources; scaled.clear(); }
    }

    /** Result hook of Resources.getDimensionPixelOffset(int). */
    int pixelOffset(Resources resources, int id, int nativeValue) {
        if (!active || resources == null || resources != plugin) return nativeValue;
        Boolean bounce = scaled.get(id);
        if (bounce == null) {
            String name;
            try { name = resources.getResourceEntryName(id); }
            catch (RuntimeException unknown) { name = null; }
            bounce = WIDTH.equals(name) || HEIGHT.equals(name);
            if (scaled.size() < 256) scaled.put(id, bounce);
        }
        return bounce ? scale(nativeValue, factor) : nativeValue;
    }

    static int scale(int nativeValue, float factor) {
        if (!(factor >= 0f) || nativeValue <= 0) return nativeValue;
        return Math.round(nativeValue * factor);
    }

    void release() { active = false; plugin = null; scaled.clear(); }
}
