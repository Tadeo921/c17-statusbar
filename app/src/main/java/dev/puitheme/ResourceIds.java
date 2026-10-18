// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie

package dev.puitheme;

import android.content.res.Resources;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Resource ids by name. {@code Resources.getIdentifier} walks the string pool of the whole package (SystemUI's is very large), and
 * several callers asked for the same few names on every refresh, every tile and every frame of a transition. The ids of a package do
 * not change while its {@code Resources} lives, so each name is looked up once per {@code Resources}. A name that is not found is
 * not remembered (another theme or package state may provide it later).
 */
final class ResourceIds {
    private static final Map<Resources, Map<String, Integer>> KNOWN = Collections.synchronizedMap(new WeakHashMap<>());

    private ResourceIds() { }

    static int get(Resources resources, String name, String type, String packageName) {
        if (resources == null) return 0;
        Map<String, Integer> byName;
        synchronized (KNOWN) {
            byName = KNOWN.get(resources);
            if (byName == null) { byName = new HashMap<>(); KNOWN.put(resources, byName); }
        }
        String key = type + '/' + packageName + '/' + name;
        synchronized (byName) {
            Integer known = byName.get(key);
            if (known != null) return known;
            int found = resources.getIdentifier(name, type, packageName);
            if (found != 0) byName.put(key, found);
            return found;
        }
    }
}
