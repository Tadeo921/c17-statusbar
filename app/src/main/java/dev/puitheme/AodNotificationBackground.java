// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/** A light white-glass tint over audited AOD background draws. The native blur, wallpaper colour,
 * texture, rounded clipping and animated alpha remain in the original draw. Content never enters
 * this layer; the OEM's one background drawIcon is deferred outside it. No foreground setters,
 * content-tree traversal, opaque replacement plate or shared drawable mutation are involved. */
public final class AodNotificationBackground {
    /** Keep the saved setting key compatible with the original white-background option. */
    public static final String ENABLED = "lock_aod_notification_white_background_enabled";
    public static final String MEDIA_HOST = "com.oplus.systemui.media.controls.ui.OplusMediaCarouselView";
    private static final String BODY = "com.android.systemui.statusbar.notification.row.NotificationBackgroundView";
    private static final String EXT = "com.oplus.systemui.statusbar.notification.row.NotificationBackgroundViewExtImp";
    private static final String EXT_BASE = "com.android.systemui.statusbar.notification.row.NotificationBackgroundViewExt";
    private static final String ROW = "com.android.systemui.statusbar.notification.row.ExpandableNotificationRow";
    private static final String CUSTOM = "com.oplus.systemui.statusbar.notification.customcard.OplusCustomRow";
    private static final String HEADS = "com.oplus.systemui.notification.headsup.windowframe.HeadsUpLayout";
    private static final String STATE = "com.android.systemui.statusbar.StatusBarStateControllerImpl";
    private static final String KEYGUARD = "com.android.systemui.statusbar.policy.KeyguardStateControllerImpl";
    /** A slight white veil: retain 88% of every native RGB difference and all native opacity. */
    private static final float NATIVE_RGB = .88f, WHITE_RGB = 255f * (1f - NATIVE_RGB);
    private final Map<ClassLoader, Access> contracts = Collections.synchronizedMap(new WeakHashMap<>());
    private final Map<View, Binding> bodies = Collections.synchronizedMap(new WeakHashMap<>());
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ThreadLocal<Frame> drawing = new ThreadLocal<>();
    private final Paint glass = new Paint();
    private boolean enabled, keyguard, keyguardKnown, dozing, released, pending;

    public AodNotificationBackground() {
        // Positive RGB slopes preserve native wallpaper/blur detail. The alpha row is exactly
        // the identity, so a transparent hole stays invisible and no opaque base is introduced.
        glass.setColorFilter(new ColorMatrixColorFilter(new ColorMatrix(new float[]{
                NATIVE_RGB, 0, 0, 0, WHITE_RGB, 0, NATIVE_RGB, 0, 0, WHITE_RGB,
                0, 0, NATIVE_RGB, 0, WHITE_RGB, 0, 0, 0, 1, 0})));
    }
    public void configure(Bundle settings) {
        boolean next = settings != null && settings.getBoolean(ENABLED, false)
                && !settings.getBoolean(StatusBarSettings.SAFE_MODE, false);
        if (released || enabled == next) return;
        enabled = next; refresh();
    }
    public void onNativeState(Object controller) {
        if (released || controller == null || !STATE.equals(controller.getClass().getName())) return;
        try {
            Method state = controller.getClass().getDeclaredMethod("getState"), doze = controller.getClass().getDeclaredMethod("isDozing");
            if (state.getReturnType() != Integer.TYPE || doze.getReturnType() != Boolean.TYPE) return;
            state.setAccessible(true); doze.setAccessible(true);
            int value = (Integer) state.invoke(controller);
            scene(keyguardKnown ? keyguard : value == 1 || value == 2, (Boolean) doze.invoke(controller));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) { scene(false, false); }
    }
    public void onNativeKeyguard(Object controller) {
        if (released || controller == null || !KEYGUARD.equals(controller.getClass().getName())) return;
        try {
            Field showing = controller.getClass().getDeclaredField("mShowing");
            if (showing.getType() != Boolean.TYPE) return;
            showing.setAccessible(true); keyguardKnown = true; scene(showing.getBoolean(controller), dozing);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) { scene(false, false); }
    }
    void scene(boolean locked, boolean asleep) {
        if (released || keyguard == locked && dozing == asleep) return;
        keyguard = locked; dozing = asleep; refresh();
    }
    private boolean active() { return !released && enabled && keyguard && dozing; }
    private void refresh() {
        if (Looper.myLooper() == Looper.getMainLooper()) { invalidateBodies(); return; }
        if (pending) return;
        pending = true; main.post(() -> { pending = false; if (!released) invalidateBodies(); });
    }
    private void invalidateBodies() {
        for (Binding binding : snapshot()) { View body = binding.body.get(); if (body != null) body.invalidate(); }
    }
    public void observeExtension(Object extension) {
        Access access = access(extension == null ? null : extension.getClass().getClassLoader());
        if (access == null || !access.extension.isInstance(extension)) return;
        try { Object body = access.host.invoke(extension); if (body instanceof View) observeBody((View) body); }
        catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) { }
    }
    /** Only the native body's actual row parent establishes notification ownership, never entry caches. */
    public void observeBody(View body) {
        if (released || body == null) return;
        Access access = access(body.getClass().getClassLoader());
        if (access == null || !access.body.isInstance(body)) return;
        View row = row(body);
        if (row == null) { detach(body); return; }
        Binding binding = bodies.get(body);
        if (binding == null) bodies.put(body, new Binding(body, row, access, false));
        else binding.row = new WeakReference<>(row);
    }
    public void observeMedia(View media) {
        if (released || media == null) return;
        Access access = access(media.getClass().getClassLoader());
        if (access != null && access.media != null && access.media.isInstance(media) && !bodies.containsKey(media))
            bodies.put(media, new Binding(media, media, access, true));
    }
    public interface ContentDraw { Object draw(Canvas canvas) throws Throwable; }
    /** Native onDraw contains backgrounds only; media children are drawn later by dispatchDraw. */
    public Object drawMediaBackground(View media, Canvas canvas, ContentDraw nativeDraw) throws Throwable {
        if (active() && !bodies.containsKey(media)) observeMedia(media);
        return drawNotificationBackground(media, canvas, nativeDraw);
    }
    public Object drawNotificationBackgroundExtension(Object extension, Canvas canvas, ContentDraw nativeDraw) throws Throwable {
        Access access = access(extension == null ? null : extension.getClass().getClassLoader());
        if (access == null || !access.extension.isInstance(extension)) return nativeDraw.draw(canvas);
        View body;
        try { body = (View) access.host.invoke(extension); }
        catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) { return nativeDraw.draw(canvas); }
        return drawNotificationBackground(body, canvas, nativeDraw);
    }
    /** Enclose the existing native/C17 background composition while leaving its blur recipe intact. */
    public Object drawNotificationBackground(View body, Canvas canvas, ContentDraw nativeDraw) throws Throwable {
        Frame previous = drawing.get();
        if (!active() || body == null || canvas == null || previous != null && previous.body == body && previous.canvas == canvas)
            return nativeDraw.draw(canvas);
        Binding binding = bodies.get(body);
        if (binding == null) { observeBody(body); binding = bodies.get(body); }
        if (binding == null || !eligible(binding) || !bounds(binding)) return nativeDraw.draw(canvas);
        Frame frame = binding.frame.canvas == null ? binding.frame : new Frame();
        if (frame != binding.frame) frame.bounds.set(binding.frame.bounds.left, binding.frame.bounds.top, binding.frame.bounds.right, binding.frame.bounds.bottom);
        int saved;
        try { saved = canvas.saveLayer(frame.bounds, glass); }
        catch (RuntimeException unavailable) { return nativeDraw.draw(canvas); }
        frame.body = body; frame.canvas = canvas; frame.count = 0; drawing.set(frame);
        try { return nativeDraw.draw(canvas); }
        finally {
            if (previous == null) drawing.remove(); else drawing.set(previous);
            try { canvas.restoreToCount(saved); for (int i = 0; i < frame.count; i++) frame.icons[i].draw(canvas); }
            finally {
                for (int i = 0; i < frame.count; i++) frame.icons[i] = null;
                frame.body = null; frame.canvas = null; frame.count = 0;
            }
        }
    }
    private boolean eligible(Binding binding) {
        View body = binding.body.get(), row = binding.row.get();
        if (body == null || row == null || !body.isAttachedToWindow() || !row.isAttachedToWindow()) return false;
        try {
            if (!binding.media) return row(body) == row && binding.access.keyguard.getBoolean(body);
            Object repository = binding.access.mediaRepository.get(body);
            float progress = (Float) binding.access.mediaProgress.invoke(repository);
            return Float.isFinite(progress) && progress > 0f && progress <= 1f;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) { return false; }
    }
    /** The native app icon bypasses both this glass tint and the inner C17 background layer. */
    public boolean deferNotificationIcon(Object extension, Canvas canvas) {
        Frame frame = drawing.get();
        if (frame == null || frame.canvas != canvas || released) return false;
        Access access = access(extension == null ? null : extension.getClass().getClassLoader());
        if (access == null || !access.extension.isInstance(extension)) return false;
        try {
            if (access.host.invoke(extension) != frame.body) return false;
            Object icon = access.icon.get(extension);
            if (!(icon instanceof Drawable) || frame.count == frame.icons.length) return false;
            frame.icons[frame.count++] = (Drawable) icon; return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) { return false; }
    }
    private boolean bounds(Binding binding) {
        View body = binding.body.get();
        try {
            if (binding.media) {
                int width = body.getWidth(), height = body.getHeight();
                int scroll = binding.access.mediaScroll.getInt(body); float translation = binding.access.mediaTranslation.getFloat(body);
                if (width <= 0 || height <= 0 || !Float.isFinite(translation)) return false;
                ViewParent parent = body.getParent(); int parentWidth = parent instanceof View ? ((View) parent).getWidth() : width;
                binding.frame.bounds.set(Math.min(0f, scroll - Math.min(translation, 0f)) - 1f, -1f,
                        Math.max(width, (float) parentWidth + scroll - Math.max(translation, 0f)) + 1f, height + 1f); return true;
            }
            int width = body.getWidth(), actualWidth = (Integer) binding.access.width.invoke(body);
            int actualHeight = (Integer) binding.access.height.invoke(body), height = Math.max(body.getHeight(), actualHeight);
            if (width <= 0 || actualWidth <= 0 || actualHeight <= 0 || height <= 0) return false;
            binding.frame.bounds.set(Math.min(0, width - actualWidth) - 1f, -1f, Math.max(width, actualWidth) + 1f, height + 1f); return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) { return false; }
    }
    public void detach(View body) {
        if (bodies.remove(body) != null || body == null) return;
        for (Binding binding : snapshot()) if (binding.row.get() == body) { View background = binding.body.get(); if (background != null) bodies.remove(background); }
    }
    public void release() { released = true; bodies.clear(); contracts.clear(); drawing.remove(); }
    private ArrayList<Binding> snapshot() { synchronized (bodies) { return new ArrayList<>(bodies.values()); } }
    private Access access(ClassLoader loader) {
        if (released || loader == null) return null;
        synchronized (contracts) {
            if (contracts.containsKey(loader)) return contracts.get(loader);
            Access result = null;
            try { result = new Access(loader); } catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) { }
            contracts.put(loader, result); return result;
        }
    }
    private static View row(View body) {
        for (ViewParent parent = body.getParent(); parent instanceof View; parent = parent.getParent()) {
            View candidate = (View) parent;
            if (type(candidate.getClass(), HEADS)) return null;
            if (type(candidate.getClass(), ROW) || type(candidate.getClass(), CUSTOM)) return candidate;
        }
        return null;
    }
    private static boolean type(Class<?> type, String name) {
        for (Class<?> next = type; next != null; next = next.getSuperclass()) if (name.equals(next.getName())) return true;
        return false;
    }
    private static final class Binding {
        final WeakReference<View> body; WeakReference<View> row; final Access access; final boolean media; final Frame frame = new Frame();
        Binding(View body, View row, Access access, boolean media) { this.body = new WeakReference<>(body); this.row = new WeakReference<>(row); this.access = access; this.media = media; }
    }
    private static final class Frame { final RectF bounds = new RectF(); final Drawable[] icons = new Drawable[4]; View body; Canvas canvas; int count; }
    private static final class Access {
        final Class<?> body, extension, media;
        final Field keyguard, icon, mediaRepository, mediaScroll, mediaTranslation;
        final Method host, width, height, mediaProgress;
        Access(ClassLoader loader) throws ReflectiveOperationException {
            body = loader.loadClass(BODY); extension = loader.loadClass(EXT);
            if (!View.class.isAssignableFrom(body)) throw new NoSuchMethodException("Native AOD body changed");
            host = loader.loadClass(EXT_BASE).getDeclaredMethod("getBgView");
            width = body.getDeclaredMethod("getActualWidth"); height = body.getDeclaredMethod("getActualHeight");
            keyguard = body.getDeclaredField("mOnKeyguard"); icon = extension.getDeclaredField("zoomAppIcon");
            if (host.getReturnType() != body || width.getReturnType() != Integer.TYPE || height.getReturnType() != Integer.TYPE
                    || keyguard.getType() != Boolean.TYPE || !Drawable.class.isAssignableFrom(icon.getType())) throw new NoSuchMethodException("Native AOD background ownership changed");
            host.setAccessible(true); width.setAccessible(true); height.setAccessible(true); keyguard.setAccessible(true); icon.setAccessible(true);
            Class<?> mediaType = null; Field repository = null, scroll = null, translation = null; Method progress = null;
            try {
                mediaType = loader.loadClass(MEDIA_HOST);
                if (!ViewGroup.class.isAssignableFrom(mediaType)) throw new NoSuchMethodException("Native media AOD host changed");
                repository = mediaType.getDeclaredField("oplusPanoramicAodRepository");
                Class<?> repositoryType = loader.loadClass("com.oplus.systemui.notification.aod.OplusPanoramicAodRepository");
                progress = repositoryType.getDeclaredMethod("getPanoramicAodProgress");
                scroll = mediaType.getDeclaredField("bgScrollerAmount"); translation = mediaType.getDeclaredField("contextViewTransitionX");
                if (repository.getType() != repositoryType || progress.getReturnType() != Float.TYPE || scroll.getType() != Integer.TYPE || translation.getType() != Float.TYPE)
                    throw new NoSuchMethodException("Native media AOD progress/geometry changed");
                repository.setAccessible(true); scroll.setAccessible(true); translation.setAccessible(true); progress.setAccessible(true);
            } catch (ReflectiveOperationException | LinkageError unavailable) { mediaType = null; repository = null; scroll = null; translation = null; progress = null; }
            media = mediaType; mediaRepository = repository; mediaScroll = scroll; mediaTranslation = translation; mediaProgress = progress;
        }
    }
}
