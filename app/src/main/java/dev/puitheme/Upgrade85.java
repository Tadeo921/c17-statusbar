// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;
import java.util.Collections;
import java.util.Map;
/** Preserve the old media source without enabling a new policy for other users. */
final class Upgrade85 {
    static final Map<String,Object> DEFAULTS=Collections.<String,Object>singletonMap(FluidCloudStyle.FOLLOW_MEDIA,false);
    static boolean legacyMedia(Map<String,?> values) {
        return values!=null&&!Boolean.FALSE.equals(values.get(FluidCloudStyle.MASTER))
                &&Boolean.TRUE.equals(values.get(FluidCloudStyle.DESKTOP_BACKGROUND))
                &&"media".equals(values.get(FluidCloudStyle.BACKGROUND_MODE));
    }
    private Upgrade85() { }
}
