package com.oplus.systemui.notification.lockscreen.effect;

import android.graphics.Path;

/** The capsule's clip: a rounded rectangle path of the pill. */
public final class AutoRoundRectPathProvider {
    private final Path path = new Path();
    public AutoRoundRectPathProvider(float left, float top, float right, float bottom) {
        path.addRoundRect(new android.graphics.RectF(left, top, right, bottom), 96f, 96f, Path.Direction.CW);
    }
    public final Path getClipPath() { return path; }
}
