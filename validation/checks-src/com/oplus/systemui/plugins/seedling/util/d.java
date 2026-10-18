package com.oplus.systemui.plugins.seedling.util;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import kotlin.Triple;

/** Audited OEM expanded-card drawable. The field names/types, constructor,
 * shader endpoints, stops and draw order match plugin DEX 0x2078e0/0x207930/
 * 0x2079fc. The payload at 0x207a78 is exactly [0, 0, 1].
 * Smooth path construction is represented by a rounded path with the same
 * bounds and corner configuration; this check does not test OEM curve math.
 * This drawable intentionally has no ConstantState.
 */
public final class d extends Drawable {
    /** Check-only seams for hook dispatch. OEM bytecode contains neither hook:
     * tests can suppress the bounds observer and independently exercise the
     * exact draw entry used by StatusBarModule. */
    public static boolean dispatchBoundsHookForCheck = true;
    public static boolean dispatchDrawHookForCheck = true;
    public final int a, b, c;
    public final Triple<Float, Integer, Float> d;
    public final Paint e = new Paint(Paint.ANTI_ALIAS_FLAG);
    public final Path f = new Path();
    public LinearGradient g;

    public d(int first, int second, int third, Triple<Float, Integer, Float> cornerConfig) {
        if (cornerConfig == null) throw new NullPointerException("cornerConfig");
        a = first; b = second; c = third; d = cornerConfig;
    }
    @Override public void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        if (bounds.isEmpty()) g = null;
        else g = new LinearGradient(bounds.left, bounds.top, bounds.left, bounds.bottom,
                new int[]{a, b, c}, new float[]{0f, 0f, 1f}, Shader.TileMode.CLAMP);
        // The real OEM reset must precede the optional module observer.
        if (dispatchBoundsHookForCheck) dispatchForCheck("nativeBoundsChanged");
    }
    private void dispatchForCheck(String event) {
        try {
            Method hook = Class.forName("dev.puitheme.FluidCloudAppearance")
                    .getDeclaredMethod(event, Drawable.class);
            hook.setAccessible(true); hook.invoke(null, this);
        } catch (NoSuchMethodException oldBuild) {
            // A missing observer retains the OEM draw and supports red replay
            // against the previous compiled production build.
        } catch (InvocationTargetException failed) {
            throw new AssertionError("Native gradient " + event + " hook failed", failed.getCause());
        } catch (ReflectiveOperationException failed) {
            throw new AssertionError("Native gradient " + event + " hook unavailable", failed);
        }
    }
    @Override public void draw(Canvas canvas) {
        if (dispatchDrawHookForCheck) dispatchForCheck("nativeDrawStarting");
        nativeDrawForCheck(canvas);
        if (dispatchDrawHookForCheck) dispatchForCheck("nativeDrawCompleted");
    }
    private void nativeDrawForCheck(Canvas canvas) {
        LinearGradient shader = g;
        Rect bounds = getBounds();
        if (shader == null || bounds.isEmpty()) return;
        f.reset();
        float radius = d.getFirst();
        f.addRoundRect(new RectF(bounds.left, bounds.top, bounds.right, bounds.bottom),
                radius, radius, Path.Direction.CW);
        if (f.isEmpty()) return;
        e.setShader(shader);
        int save = canvas.save();
        canvas.clipPath(f);
        try {
            // Canvas stub has the equivalent coordinate overload, retaining
            // the native rectangle and Paint without changing rendering.
            canvas.drawRect(bounds.left, bounds.top, bounds.right, bounds.bottom, e);
        } finally { canvas.restoreToCount(save); }
    }
    @Override public int getAlpha() { return e.getAlpha(); }
    @Override public void setAlpha(int alpha) { e.setAlpha(alpha); }
    @Override public void setColorFilter(ColorFilter filter) { e.setColorFilter(filter); }
    @Override public int getOpacity() { return -3; }
}
