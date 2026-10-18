package com.oplus.systemui.notification.lockscreen.capsule;

import android.graphics.Canvas;
import android.view.View;
import com.oplus.systemui.notification.lockscreen.effect.AutoRoundRectPathProvider;

/** The audited ColorOS 17 capsule background helper: drawCapsuleBg draws the blur layer (or a plain colour) of one pill. */
public final class CapsuleBackgroundHelper {
    private AutoRoundRectPathProvider pathProvider = new AutoRoundRectPathProvider(394f, 0f, 1046f, 192f);
    private boolean isImmersiveState;
    public int nativeDraws;

    public final void drawCapsuleBg(View view, Canvas canvas) { nativeDraws++; }
    public final void setImmersiveForCheck(boolean value) { isImmersiveState = value; }
    public final void clearPathForCheck() { pathProvider = null; }
}
