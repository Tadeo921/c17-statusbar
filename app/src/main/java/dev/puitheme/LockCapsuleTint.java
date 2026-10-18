// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/** The lock-screen capsule: the pill at the bottom of the lock screen and of AOD that carries the music (and other live
 * alert) card while notifications are hidden. It is a different surface from the notification cards: a
 * {@code NotificationCapsuleContainer} holding one {@code CapsuleItemView} per live alert, whose {@code CapsuleBackgroundView}
 * draws a blur layer through {@code CapsuleBackgroundHelper.drawCapsuleBg}. This class lays one more translucent layer over
 * that background, in the capsule's own clip path, using the fluid-cloud colour policy (media accent, follow, custom base,
 * lock opacity and mask, lock gradient). It adds a draw after the native one and changes nothing the native code owns, so
 * switching it off leaves nothing behind. The audited structure is checked at install; anything else stays native. */
public final class LockCapsuleTint {
    public static final String HELPER = "com.oplus.systemui.notification.lockscreen.capsule.CapsuleBackgroundHelper";
    static final String MEDIA_TEMPLATE = "com.oplus.systemui.plugins.shared.template.component.media.";
    private static final long RESCAN_NANOS = 2_000_000_000L;
    private static final int MAX_VIEWS = 200;

    /** What the native colour policy said for one capsule: an ARGB, whether it fades to transparent toward the bottom and over how much
     * of the capsule's height (0..1), and why. */
    public static final class Plan {
        public final int argb;
        public final boolean gradient;
        public final String source;
        public final float range;
        public Plan(int argb, boolean gradient, String source) { this(argb, gradient, source, 1f); }
        public Plan(int argb, boolean gradient, String source, float range) {
            this.argb = argb; this.gradient = gradient; this.source = source; this.range = Math.max(.05f, Math.min(1f, range));
        }
    }

    /** The colour policy of the fluid-cloud style: null leaves the capsule native. */
    public interface Policy { Plan capsule(boolean media); }

    private final Handler main = new Handler(Looper.getMainLooper());
    private final Map<View, Binding> bindings = Collections.synchronizedMap(new WeakHashMap<>());
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bounds = new RectF();
    private volatile Policy policy;
    private volatile Hooks hooks;
    private volatile boolean released;
    private Shader gradient;
    private int gradientColor;
    private float gradientTop = Float.NaN, gradientBottom = Float.NaN;

    private static final class Binding {
        boolean media;
        long scanned;
        String state = "";
        /** The colour shown, fading to a new one (a new track, the media going, a changed setting) instead of jumping. */
        C17HighlightRemoval.Fade fade;
        boolean gradient;
        float range = 1f;
    }
    private static final java.util.function.BooleanSupplier ALWAYS = () -> true;

    /** The members this class uses, resolved once per class loader. */
    public static final class Hooks {
        public final Method draw;
        final Field pathProvider, immersive;
        final Method clipPath;
        public Hooks(ClassLoader loader) throws ReflectiveOperationException {
            Class<?> type = Class.forName(HELPER, false, loader);
            draw = type.getDeclaredMethod("drawCapsuleBg", View.class, Canvas.class);
            pathProvider = type.getDeclaredField("pathProvider");
            immersive = type.getDeclaredField("isImmersiveState");
            if (immersive.getType() != boolean.class) throw new NoSuchFieldException(HELPER + ".isImmersiveState");
            clipPath = pathProvider.getType().getMethod("getClipPath");
            if (!Path.class.isAssignableFrom(clipPath.getReturnType())) throw new NoSuchMethodException("getClipPath");
            pathProvider.setAccessible(true); immersive.setAccessible(true); draw.setAccessible(true); clipPath.setAccessible(true);
        }
    }

    public void setPolicy(Policy next) { policy = next; }

    /** Resolve the structure; null when this ROM's lock capsule is not the audited one. */
    public Hooks resolve(ClassLoader loader) {
        if (loader == null || released) return null;
        try {
            Hooks found = new Hooks(loader);
            hooks = found;
            return found;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException unavailable) {
            ModuleDiagnostics.info("fluid", "Lock capsule contract unavailable; the lock-screen capsule stays native");
            return null;
        }
    }

    /** Called right after the native background draw of one capsule, on the same canvas. */
    public void drawn(Object helper, View view, Canvas canvas) {
        Policy current = policy;
        Hooks access = hooks;
        if (released || current == null || access == null || helper == null || view == null || canvas == null) return;
        try {
            // The immersive music card has its own look; it is not tinted.
            if (access.immersive.getBoolean(helper)) return;
            Object provider = access.pathProvider.get(helper);
            if (provider == null) return;
            Object result = access.clipPath.invoke(provider);
            if (!(result instanceof Path)) return;
            Path clip = (Path) result;
            boolean media = media(view);
            Plan plan = current.capsule(media);
            Binding binding = binding(view);
            long now = android.os.SystemClock.uptimeMillis();
            boolean wanted = plan != null && (plan.argb >>> 24) != 0;
            C17HighlightRemoval.Fade next = C17HighlightRemoval.next(binding.fade, wanted ? Integer.valueOf(plan.argb) : null, ALWAYS, now);
            if (next != null) binding.fade = next;
            if (wanted) { binding.gradient = plan.gradient; binding.range = plan.range; }
            C17HighlightRemoval.Fade fade = binding.fade;
            if (fade == null) return;
            int shown = fade.shown(now);
            if (!fade.done(now)) view.postInvalidateOnAnimation();
            else if (fade.leaving) binding.fade = null;
            if ((shown >>> 24) == 0) return;
            clip.computeBounds(bounds, false);
            if (bounds.isEmpty()) return;
            int save = canvas.save();
            try {
                canvas.clipPath(clip);
                if (binding.gradient) {
                    fill.setShader(gradient(bounds, shown, binding.range));
                    canvas.drawRect(bounds, fill);
                    fill.setShader(null);
                } else canvas.drawColor(shown);
            } finally { canvas.restoreToCount(save); }
            if (wanted) record(view, media, plan);
        } catch (ReflectiveOperationException | RuntimeException failure) {
            ModuleDiagnostics.info("fluid", "Lock capsule tint skipped: " + failure.getClass().getSimpleName());
        }
    }

    private Shader gradient(RectF area, int argb, float range) {
        float end = area.top + Math.max(1f, area.height() * range);
        if (gradient == null || gradientColor != argb || gradientTop != area.top || gradientBottom != end) {
            gradient = new LinearGradient(0f, area.top, 0f, end, argb, argb & 0x00ffffff, Shader.TileMode.CLAMP);
            gradientColor = argb; gradientTop = area.top; gradientBottom = end;
        }
        return gradient;
    }

    /** One structured record per change of what a capsule shows, never per frame. */
    private void record(View view, boolean media, Plan plan) {
        if (!ModuleDiagnostics.enabled()) return;
        Binding binding = binding(view);
        String state = media + "|" + plan.source + "|" + Integer.toHexString(plan.argb) + "|" + plan.gradient;
        if (state.equals(binding.state)) return;
        binding.state = state;
        ModuleDiagnostics.fact("fluid", "fluid", "锁屏胶囊着色", "scene", "lock_capsule", "media_capsule", media, "source", plan.source,
                "fill_argb", Integer.toHexString(plan.argb), "mask", plan.gradient, "reason", "ok");
    }

    private Binding binding(View view) {
        synchronized (bindings) {
            Binding binding = bindings.get(view);
            if (binding == null) { binding = new Binding(); bindings.put(view, binding); }
            return binding;
        }
    }

    /** True when the capsule item that holds this background shows the music template. Looked up from the item's own
     * views, at most every two seconds per capsule: a capsule's content is fixed for its notification key. */
    private boolean media(View background) {
        Binding binding = binding(background);
        long now = System.nanoTime();
        if (binding.scanned != 0L && now - binding.scanned < RESCAN_NANOS) return binding.media;
        binding.scanned = now;
        ViewParent parent = background.getParent();
        binding.media = parent instanceof ViewGroup && hasMediaTemplate((ViewGroup) parent, background);
        return binding.media;
    }

    static boolean hasMediaTemplate(ViewGroup root, View skip) {
        ArrayList<View> pending = new ArrayList<>();
        pending.add(root);
        for (int cursor = 0; cursor < pending.size() && cursor < MAX_VIEWS; cursor++) {
            View view = pending.get(cursor);
            if (view != skip && view.getClass().getName().startsWith(MEDIA_TEMPLATE)) return true;
            if (view instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) view;
                for (int i = 0; i < group.getChildCount() && pending.size() < MAX_VIEWS; i++) pending.add(group.getChildAt(i));
            }
        }
        return false;
    }

    /** The policy's inputs changed (settings, media colour, playback, lock/AOD): redraw every capsule that is known. */
    public void invalidateAll() {
        if (released) return;
        final ArrayList<View> views;
        synchronized (bindings) { views = new ArrayList<>(bindings.keySet()); }
        if (views.isEmpty()) return;
        Runnable redraw = () -> { for (View view : views) if (view != null) view.invalidate(); };
        if (Looper.myLooper() == Looper.getMainLooper()) redraw.run(); else main.post(redraw);
    }

    public void release() {
        if (released) return;
        released = true;
        final ArrayList<View> views;
        synchronized (bindings) { views = new ArrayList<>(bindings.keySet()); }
        Runnable redraw = () -> { for (View view : views) if (view != null) view.invalidate(); };
        if (Looper.myLooper() == Looper.getMainLooper()) redraw.run(); else main.post(redraw);
        bindings.clear(); policy = null; hooks = null; gradient = null;
    }
}
