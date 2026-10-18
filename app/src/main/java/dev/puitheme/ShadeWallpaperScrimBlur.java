// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.os.Looper;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Takes the picture's share of the system's full-screen blur away from the shade's behind-scrim.
 *
 * <p>The system blurs everything behind the shade window by 250 pixels with the behind-scrim, and fades that blur in with the pull. The
 * pull-down picture is a window of its own <em>behind</em> the shade (see {@link ShadeWallpaperWindow}), so that blur would land on it as
 * a second blur on top of the one the author chose. As much of the picture as is showing replaces the same share of the system's blur:
 * half shown, half of the blur. The cards' own glass is another blur and is left alone.
 */
final class ShadeWallpaperScrimBlur {
    static final String SCRIM = "com.oplus.systemui.scrim.ScrimViewExImp";
    static final String SCRIM_BASE = "com.android.systemui.scrim.ScrimViewEx";
    private static final String FROM = "c17-wallpaper";

    private final ShadeWallpaper wallpaper;
    private Field behind;
    private Method setAmount;
    private WeakReference<Object> scrim = new WeakReference<>(null);
    /** What the system last asked for, so the share can follow the pull between its calls. */
    private float requested = Float.NaN;
    private boolean reapplying;

    ShadeWallpaperScrimBlur(ShadeWallpaper wallpaper) { this.wallpaper = wallpaper; }

    /** Resolved once by the installer; the hook asks {@link #amount} for every value the system sets. */
    void bind(Class<?> scrimBase, Method setter) throws NoSuchFieldException {
        behind = scrimBase.getDeclaredField("isBehind"); behind.setAccessible(true);
        setAmount = setter;
        wallpaper.onShareChanged(this::reapply);
    }

    /** The blur amount (0..1) the behind-scrim really gets for the amount the system asks for. */
    float amount(Object owner, float asked) {
        if (!isBehind(owner) || !Float.isFinite(asked)) return asked;
        if (!reapplying) { scrim = new WeakReference<>(owner); requested = asked; }
        return asked * (1f - wallpaper.nativeBlurShare());
    }

    /** The picture's share changed between two calls of the system: set the same asked amount again. Main thread. */
    private void reapply() {
        Object owner = scrim.get();
        if (owner == null || !Float.isFinite(requested) || setAmount == null || reapplying || Looper.myLooper() != Looper.getMainLooper()) return;
        reapplying = true;
        try { setAmount.invoke(owner, requested, FROM); }
        catch (ReflectiveOperationException | RuntimeException ignored) { /* the system's own value stays */ }
        finally { reapplying = false; }
    }

    private boolean isBehind(Object owner) {
        try { return owner != null && behind != null && behind.getBoolean(owner); }
        catch (IllegalAccessException | RuntimeException unavailable) { return false; }
    }
}
