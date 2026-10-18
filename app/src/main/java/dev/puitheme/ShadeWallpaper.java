// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Paints the user's own wallpaper beneath the native children of the one fixed shade window; the native gesture owns every frame.
 *
 * <ul>
 * <li>How much of the picture shows follows the panel's own pull (the system's curve for its blur and dim), so it comes in with the
 *     gesture, goes with the close, and is gone when the panel is.</li>
 * <li>With "blur with the pull" the picture is sharp at the first pixel and has its set blur at full expansion.</li>
 * <li>In the separate mode a swipe between the notification centre and the control centre blends their two pictures by the native
 *     horizontal progress.</li>
 * <li>The notification centre's picture can have a clock zone: a band at the top (the big clock's height, or a set one) blurred
 *     differently from the rest.</li>
 * <li>The big clock's lock-style glass is shown what is really behind it: {@link #glass()}.</li>
 * </ul>
 * All pixel work (decode, blur, zone, brightness) happens on one worker thread on small copies; drawing only draws bitmaps.
 */
public final class ShadeWallpaper {
    private static final int CACHE_BYTES = 24 * 1024 * 1024;
    /** Longest edge of the blurred copies: a blurred picture has no detail to keep, and these are small to hold and quick to draw. */
    static final int SMALL_EDGE = 540;
    private final Handler main = new Handler(Looper.getMainLooper());
    interface ImageReader { Bitmap read(Context context, String scene, String revision) throws Exception; }
    private final Executor worker, completion;
    private final ImageReader imageReader;
    // Accessed only by the worker. Eviction releases references; displayed bitmaps are never recycled.
    private final Map<String, Bitmap> cache = new LinkedHashMap<>(4, .75f, true);
    private final Set<String> failed = new HashSet<>();
    private int cacheBytes;
    private volatile long generation;
    private Context context;
    private Config config = new Config(null);
    private WeakReference<View> root = new WeakReference<>(null);
    private String mode = "unknown";
    private boolean landscape, visible;
    private float controlWeight, fraction;
    private int rootWidth, rootHeight;
    /** The big clock's bottom edge (px, in the shade window) while the zone follows the clock; NaN when unknown. */
    private float clockBottom = Float.NaN;
    private Frame frame;
    private LoadKey pending;
    /** A layer under the shade that shows the picture instead of the shade itself (see {@link ShadeWallpaperWindow}); null: paint inside the shade. */
    private Layer layer;
    private long zoneRequest;

    /**
     * The shade's pull as the picture follows it. The separate shade's two panels each have a pull of their own, and a swipe between them
     * hands the shade over from one to the other: taking the sum, the hand-over can never dip the picture (one of the two is always 0 when
     * a panel is simply pulled or closed, so then it is that panel's own pull).
     */
    static float pullOf(boolean separate, float notification, float qs) {
        float first = Float.isFinite(notification) ? Math.max(0f, notification) : 0f;
        if (!separate) return Math.min(1f, first);
        float second = Float.isFinite(qs) ? Math.max(0f, qs) : 0f;
        return Math.min(1f, first + second);
    }

    /** Paints the picture on a canvas of this size. */
    interface Painter { void paint(Canvas canvas, int width, int height); }
    /** Where the picture is shown when it is not painted inside the shade. Main thread only. */
    interface Layer {
        boolean ready();
        void sync(Context context, boolean visible);
        void release();
        /** Only the swipe moved: re-place the two already drawn pictures; nothing is drawn again. */
        default void compose() { }
    }
    /**
     * How the layer combines its two pictures right now (device code162: the swipe between the notification centre and the control
     * centre must not stutter). Each picture is drawn once into its own view; a swipe only changes these view properties, which the
     * render thread applies without drawing anything again. {@code additive}: the second view is a layer added onto the first.
     */
    static final class Composition { float alpha0, alpha1, shift0, shift1; boolean additive; }
    /** Changes whenever what the pictures look like changes (picture, size, pull); a swipe alone keeps it. */
    private int contentRevision;
    int contentRevision() { return contentRevision; }
    /** The picture goes to this layer from now on, as soon as the layer is ready (before that it is painted inside the shade). */
    public void useLayer(Layer next) {
        android.util.Log.i("C17ShadeWallpaper", "layer " + (next == null ? "removed" : "installed") + ", context " + (context != null));
        Layer previous = layer;
        layer = next;
        if (previous != null && previous != next) previous.release();
        invalidate();
    }
    /** What the layer paints. */
    void paintLayer(Canvas canvas, int width, int height) { paint(canvas, width, height); }
    /** One of the layer's two pictures at the current pull: 0 the notification centre's (or the only one), 1 the control centre's. */
    void paintSide(Canvas canvas, int width, int height, int side) {
        Frame current = frame;
        if (canvas == null || !visible || config.disabled || ModuleLifecycle.removed() || current == null || width <= 0 || height <= 0) return;
        float power = WallpaperPixels.pullCurve(fraction);
        if (!(power > 0f)) return;
        float blend = config.pull(landscape) ? power : 1f;
        current.resize(width, height);
        (side == 0 ? current.first : current.second).draw(canvas, power, blend, false);
    }
    /** The same combination {@link #paint} draws, as view properties of the two pictures. */
    void compose(Composition out, int width) {
        out.alpha0 = 0f; out.alpha1 = 0f; out.shift0 = 0f; out.shift1 = 0f; out.additive = false;
        Frame current = frame;
        if (current == null || !visible || config.disabled || ModuleLifecycle.removed()) return;
        float mix = "separate".equals(mode) ? controlWeight : 0f;
        if (mix <= 0f) { out.alpha0 = 1f; return; }
        if (mix >= 1f) { out.alpha1 = 1f; return; }
        if (current.first.look == null && current.second.look == null) return;
        if (config.slide(landscape) && !current.sameAsset) {
            out.alpha0 = 1f; out.alpha1 = 1f; out.shift0 = -mix * width; out.shift1 = (1f - mix) * width; return;
        }
        if (WallpaperPixels.pullCurve(fraction) >= .999f && current.first.look != null && current.second.look != null
                && current.first.look.opaque && current.second.look.opaque) {
            out.alpha0 = 1f; out.alpha1 = mix; return;
        }
        out.alpha0 = 1f - mix; out.alpha1 = mix; out.additive = true;
    }

    public ShadeWallpaper() {
        worker = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(4), runnable -> {
                    Thread thread = new Thread(runnable, "C17ShadeWallpaper"); thread.setDaemon(true); return thread;
                }, new ThreadPoolExecutor.DiscardOldestPolicy());
        completion = main::post;
        imageReader = ShadeWallpaper::readNormalized;
    }
    /** Deterministic scheduling fixture; the public constructor always uses the private PNG reader. */
    ShadeWallpaper(Executor worker, Executor completion, ImageReader imageReader) {
        this.worker = worker; this.completion = completion; this.imageReader = imageReader;
    }

    /** Whether the module needs the panel hooks at all: the master switch and at least one scene with a picture. */
    public boolean wanted() {
        Config now = config;
        if (now.disabled) return false;
        for (SceneConfig scene : now.scenes) if (!scene.asset().isEmpty()) return true;
        return false;
    }

    /** Called by the existing settings delivery, never by draw(). */
    public void configure(Context candidate, Bundle values) {
        Config replacement = new Config(values);
        if (candidate == null || !"com.android.systemui".equals(candidate.getPackageName())) return;
        if (Looper.myLooper() != Looper.getMainLooper()) {
            main.post(() -> configure(candidate, replacement));
        } else configure(candidate, replacement);
    }
    private void configure(Context candidate, Config replacement) {
        if (context != null && config.equals(replacement)) return;
        Context application = candidate.getApplicationContext();
        context = application != null ? application : candidate;
        Config previous = config; config = replacement;
        if (config.disabled) { invalidateLoad(); frame = null; invalidate(); return; }
        if (frame != null && previous.sameLooks(config)) { requestZone(); invalidate(); return; }
        // Only a sliding blur or brightness: the picture on screen stays until its replacement is ready, instead of flashing the native one.
        if (!(frame != null && previous.sameAssets(config))) frame = null;
        invalidateLoad(); requestLoad(); invalidate();
    }
    /** Bind a fixed native shade drawing root, not an animated notification or QS page. */
    public void onPanelChanged(View nativeRoot, String nativeMode, int barState, float expanded, boolean closed) {
        if (Looper.myLooper() != Looper.getMainLooper()) return;
        String nextMode = "classic".equals(nativeMode) ? "classic" : "separate".equals(nativeMode) ? "separate" : "unknown";
        boolean nextLandscape = nativeRoot != null && nativeRoot.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        boolean changed = root.get() != nativeRoot || !mode.equals(nextMode) || landscape != nextLandscape;
        if (changed) {
            root = new WeakReference<>(nativeRoot); mode = nextMode; landscape = nextLandscape;
            // An orientation/mode switch never displays the previous scene while loading the new one.
            frame = null; invalidateLoad();
        }
        if (nativeRoot != null) noteRootSize(nativeRoot);
        boolean nextVisible = nativeRoot != null && barState == 0 && !closed && Float.isFinite(expanded) && expanded > 0f;
        float nextFraction = Float.isFinite(expanded) ? Math.max(0f, Math.min(1f, expanded)) : 0f;
        // A swipe hands the pull from one panel to the other and repeats the same values every frame: nothing to draw again.
        boolean same = !changed && nextVisible == visible && Float.compare(nextFraction, fraction) == 0;
        visible = nextVisible; fraction = nextFraction;
        if (changed) requestLoad();
        // The layer may have just become ready: its share of the system's blur is still checked.
        if (same) noteShare(); else invalidate();
    }
    /** Absolute control-center visibility 0..1 from the native horizontal animated value. */
    public void onHorizontalProgress(float nativeControlWeight) {
        if (!Float.isFinite(nativeControlWeight)) return;
        float next = Math.max(0f, Math.min(1f, nativeControlWeight));
        if (Float.compare(controlWeight, next) == 0) return;
        boolean glassBefore = controlWeight > .01f;
        controlWeight = next;
        Layer current = layer;
        if (current == null || !current.ready() || !layerWanted()) { invalidate(); return; }
        // The layer re-places its two drawn pictures; the shade itself is asked again only when the clock's glass appears or goes.
        current.compose();
        if (glassBefore != next > .01f) { View view = root.get(); if (view != null) view.invalidate(); }
    }
    public void onLayoutChanged(View nativeRoot) {
        if (nativeRoot != root.get()) return;
        noteRootSize(nativeRoot);
        invalidate();
    }
    /** The zone and the glass are built for the shade window's size: taken whenever the panel is bound and on every draw (nothing else reports it). */
    private void noteRootSize(View nativeRoot) {
        int width = nativeRoot.getWidth(), height = nativeRoot.getHeight();
        if (width <= 0 || height <= 0 || width == rootWidth && height == rootHeight) return;
        rootWidth = width; rootHeight = height; requestZone();
    }
    public void onDetached(View nativeRoot) { if (nativeRoot == root.get()) release(false); }
    /** The big clock's lower edge in the shade window, for a zone that follows the clock. Called when the clock's layout changes. */
    public void onClockBottom(float bottomPixels) {
        float next = Float.isFinite(bottomPixels) && bottomPixels > 0f ? bottomPixels : Float.NaN;
        if (Float.compare(clockBottom, next) == 0) return;
        float before = zoneBottomPixels();
        clockBottom = next;
        if (Math.abs(before - zoneBottomPixels()) >= 2f) requestZone();
    }
    /** Lifecycle retirement cancels queued publication and releases native view/image references. */
    public void reset() { release(true); Layer current = layer; layer = null; if (current != null) current.release(); }
    private void release(boolean clearContext) {
        invalidateLoad(); root.clear(); visible = false; mode = "unknown"; frame = null; controlWeight = 0f; fraction = 0f;
        rootWidth = rootHeight = 0; clockBottom = Float.NaN;
        Layer current = layer; if (current != null) current.sync(context, false);
        if (clearContext) context = null;
        worker.execute(() -> { cache.clear(); cacheBytes = 0; failed.clear(); });
    }
    private void invalidateLoad() { generation++; pending = null; zoneRequest++; }
    private void invalidate() {
        contentRevision++;
        View view = root.get(); if (view != null) view.invalidate();
        Layer current = layer;
        if (current != null) {
            boolean wanted = layerWanted();
            if (wanted != lastLayerWanted) {
                lastLayerWanted = wanted;
                android.util.Log.i("C17ShadeWallpaper", "layer wanted=" + wanted + " visible=" + visible + " frame=" + (frame != null)
                        + " fraction=" + fraction + " disabled=" + config.disabled + " context=" + (context != null));
            }
            current.sync(context, wanted);
            noteShare();
        }
    }
    private boolean lastLayerWanted;
    private Runnable shareChanged;
    private float lastShare;
    /** Told on the main thread whenever {@link #nativeBlurShare()} has changed noticeably. */
    void onShareChanged(Runnable listener) { shareChanged = listener; }
    /**
     * How much of the system's own full-screen blur the picture replaces right now, 0..1: the picture is a window behind the shade, so
     * the system's blur of "what is behind the shade" would be a second blur on top of the author's. 0 while the shade paints the
     * picture itself or nothing is showing.
     */
    float nativeBlurShare() {
        Layer current = layer;
        return current != null && current.ready() && layerWanted() ? WallpaperPixels.pullCurve(fraction) : 0f;
    }
    private void noteShare() {
        Runnable listener = shareChanged;
        if (listener == null) return;
        float share = nativeBlurShare();
        if (Math.abs(share - lastShare) < .004f && (share == 0f) == (lastShare == 0f)) return;
        lastShare = share;
        listener.run();
    }
    /** Whether the layer has anything to show right now: an open panel with a loaded picture, pulled far enough to show it. */
    private boolean layerWanted() {
        return !config.disabled && visible && frame != null && !ModuleLifecycle.removed() && WallpaperPixels.pullCurve(fraction) > 0f;
    }
    private SceneConfig sceneConfig(boolean control) {
        String scene = ShadeWallpaperSettings.scene(mode, control, landscape);
        return scene == null ? SceneConfig.EMPTY : config.scenes[ShadeWallpaperSettings.SCENES.indexOf(scene)];
    }
    /**
     * What a panel really shows. With a picture on only one of the two panels the other one shows the same picture (without the clock zone,
     * which belongs to the notification centre), so a swipe between them changes nothing instead of fading the picture out into the system's
     * own background.
     */
    private SceneConfig effective(boolean control) {
        SceneConfig own = sceneConfig(control);
        if (!own.asset().isEmpty() || !"separate".equals(mode)) return own;
        SceneConfig other = sceneConfig(!control);
        return other.asset().isEmpty() ? own : other.withoutZone();
    }
    private float density() {
        Context reader = context;
        return reader == null ? 1f : Math.max(.5f, reader.getResources().getDisplayMetrics().density);
    }
    /** The zone's lower edge in window pixels: the clock's bottom while following it (else the set height); 0 when there is no zone. */
    private float zoneBottomPixels() {
        SceneConfig scene = effective(false);
        if (!scene.zoneEnabled) return 0f;
        if (scene.zoneFollow && Float.isFinite(clockBottom)) return clockBottom;
        return scene.zoneHeight * density();
    }
    private void requestLoad() {
        if (context == null || config.disabled || "unknown".equals(mode)) return;
        SceneConfig first = effective(false), second = "separate".equals(mode) ? effective(true) : SceneConfig.EMPTY;
        LoadKey key = new LoadKey(first, second);
        if (key.equals(pending)) return;
        pending = key;
        final long token = generation;
        final Context reader = context;
        final float scale = density();
        final boolean wide = landscape;
        worker.execute(() -> {
            if (token != generation) return;
            Look a = prepare(reader, key.first, wide, scale), b = prepare(reader, key.second, wide, scale);
            if (token != generation) return;
            // Retain only the active mode/orientation pair. At most two normalized images stay cached.
            retain(key.first.asset(), key.second.asset());
            completion.execute(() -> {
                if (token != generation || !key.equals(pending) || config.disabled) return;
                frame = new Frame(a, b, key.first.brightness, key.second.brightness, key.first.asset().equals(key.second.asset()));
                invalidate(); requestZone();
            });
        });
    }
    /** The zone's blend depends on the window's size and the zone's height: rebuilt on the worker whenever either changes. */
    private void requestZone() {
        Frame current = frame;
        if (current == null || context == null || rootWidth <= 0 || rootHeight <= 0) return;
        final Look target = current.first.look;
        if (target == null || !target.zone) return;
        final long token = ++zoneRequest, load = generation;
        final float bottom = zoneBottomPixels(), feather = effective(false).zoneFeather * density();
        final int width = rootWidth, height = rootHeight;
        worker.execute(() -> {
            if (load != generation) return;
            Composite next = target.composite(width, height, bottom, feather);
            if (load != generation) return;
            completion.execute(() -> {
                if (token != zoneRequest || load != generation || frame == null || frame.first.look != target) return;
                target.publish(next);
                invalidate();
            });
        });
    }

    /** Decode, then the blurred copies the settings call for. Worker thread only. */
    private Look prepare(Context reader, SceneConfig settings, boolean wide, float scale) {
        Bitmap sharp = load(reader, settings);
        if (sharp == null) return null;
        android.util.DisplayMetrics metrics = reader.getResources().getDisplayMetrics();
        return Look.build(sharp, settings, wide, scale, metrics.widthPixels, metrics.heightPixels);
    }
    private Bitmap load(Context reader, SceneConfig settings) {
        String asset = settings.asset();
        if (asset.isEmpty() || failed.contains(asset)) return null;
        Bitmap cached = cache.get(asset);
        if (cached != null) return cached;
        Bitmap image = null;
        try {
            image = imageReader.read(reader, settings.scene, settings.revision);
            if (image == null || image.getAllocationByteCount() > ShadeWallpaperRepository.MAX_PIXELS * 4)
                throw new java.io.IOException("Malformed normalized wallpaper");
            cache.put(asset, image); cacheBytes += image.getAllocationByteCount(); trim();
            return image;
        } catch (Exception | OutOfMemoryError failure) {
            if (image != null) image.recycle();
            // No per-frame retry. A new revision or lifecycle reset allows a new bounded attempt.
            failed.add(asset);
            if (failed.size() > 12) failed.clear();
            ModuleDiagnostics.error("shade-wallpaper", "Private wallpaper unavailable; native background retained", failure);
            return null;
        }
    }
    private static Bitmap readNormalized(Context reader, String scene, String revision) throws Exception {
        BitmapFactory.Options bounds = new BitmapFactory.Options(); bounds.inJustDecodeBounds = true;
        // The resident cache keeps the normalized file in SystemUI's own storage: no call into the app after the first time.
        ResidentCache cache = ResidentCache.current;
        if (cache != null) {
            byte[] data = cache.entry("wallpaper-" + scene, revision,
                    () -> reader.getContentResolver().openInputStream(ShadeWallpaperRepository.uri(scene, revision)), 48 * 1024 * 1024);
            if (data == null) throw new java.io.IOException("Unavailable wallpaper");
            BitmapFactory.decodeByteArray(data, 0, data.length, bounds);
            checkNormalized(bounds);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888; options.inScaled = false;
            return BitmapFactory.decodeByteArray(data, 0, data.length, options);
        }
        try (InputStream input = reader.getContentResolver().openInputStream(ShadeWallpaperRepository.uri(scene, revision))) {
            if (input == null) throw new java.io.IOException("Unavailable wallpaper");
            BitmapFactory.decodeStream(input, null, bounds);
        }
        checkNormalized(bounds);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888; options.inScaled = false;
        try (InputStream input = reader.getContentResolver().openInputStream(ShadeWallpaperRepository.uri(scene, revision))) {
            return BitmapFactory.decodeStream(input, null, options);
        }
    }
    private static void checkNormalized(BitmapFactory.Options bounds) throws java.io.IOException {
        if (!"image/png".equals(bounds.outMimeType) || bounds.outWidth < 1 || bounds.outHeight < 1
                || bounds.outWidth > ShadeWallpaperRepository.MAX_EDGE || bounds.outHeight > ShadeWallpaperRepository.MAX_EDGE
                || (long) bounds.outWidth * bounds.outHeight > ShadeWallpaperRepository.MAX_PIXELS)
            throw new java.io.IOException("Malformed normalized wallpaper");
    }
    private void retain(String first, String second) {
        Iterator<Map.Entry<String, Bitmap>> items = cache.entrySet().iterator();
        while (items.hasNext()) {
            Map.Entry<String, Bitmap> item = items.next();
            if (!item.getKey().equals(first) && !item.getKey().equals(second)) {
                cacheBytes -= item.getValue().getAllocationByteCount(); items.remove();
            }
        }
    }
    private void trim() {
        Iterator<Map.Entry<String, Bitmap>> items = cache.entrySet().iterator();
        while (cacheBytes > CACHE_BYTES && items.hasNext()) {
            cacheBytes -= items.next().getValue().getAllocationByteCount(); items.remove();
        }
    }

    /** Hook before the native panel pager is drawn, after scrims, on the exact bound shade root. */
    public void draw(View nativeRoot, Canvas canvas) {
        if (nativeRoot == null || nativeRoot != root.get() || canvas == null) return;
        int width = nativeRoot.getWidth(), height = nativeRoot.getHeight();
        if (width <= 0 || height <= 0) return;
        noteRootSize(nativeRoot);
        // With the layer under the shade the picture is not painted in here: the shade's glass is made of what is behind it.
        Layer current = layer;
        if (current != null && current.ready()) return;
        paint(canvas, width, height);
    }
    /** The picture for this moment of the pull and of the swipe, on a canvas of the window's size. */
    private void paint(Canvas canvas, int width, int height) {
        Frame current = frame;
        if (canvas == null || !visible || config.disabled || ModuleLifecycle.removed() || current == null || width <= 0 || height <= 0) return;
        float power = WallpaperPixels.pullCurve(fraction);
        if (!(power > 0f)) return;
        // Pulled down only a little the picture is sharp and faint; at full expansion it has the blur that was set.
        float blend = config.pull(landscape) ? power : 1f;
        current.resize(width, height);
        float mix = "separate".equals(mode) ? controlWeight : 0f;
        if (mix <= 0f) { current.first.draw(canvas, power, blend, false); return; }
        if (mix >= 1f) { current.second.draw(canvas, power, blend, false); return; }
        if (current.first.look == null && current.second.look == null) return;
        if (config.slide(landscape) && !current.sameAsset) {
            // The pictures move with the pages: the first leaves to the left as the second comes in from the right.
            int first = canvas.save(); canvas.translate(-mix * width, 0f);
            current.first.draw(canvas, power, blend, false);
            canvas.restoreToCount(first);
            int second = canvas.save(); canvas.translate((1f - mix) * width, 0f);
            current.second.draw(canvas, power, blend, false);
            canvas.restoreToCount(second);
            return;
        }
        if (power >= .999f && current.first.look != null && current.second.look != null && current.first.look.opaque && current.second.look.opaque) {
            // Whole and solid pictures: the first at full strength, the second over it at its weight is the exact mix, and a swipe then needs no
            // full-screen offscreen buffer on every frame.
            current.first.draw(canvas, 1f, blend, false);
            current.second.draw(canvas, mix, blend, false);
            return;
        }
        // Add premultiplied image weights in a temporary layer; this avoids the midpoint dimming
        // of two sequential SRC_OVER fades and preserves native fallback through transparent pixels.
        int saved = canvas.saveLayer(current.bounds, null);
        current.first.draw(canvas, power * (1f - mix), blend, false);
        current.second.draw(canvas, power * mix, blend, true);
        canvas.restoreToCount(saved);
    }

    /** What the big clock's glass is shown instead of the lock screen's wallpaper: the picture as it looks behind the clock. */
    public static final class Glass {
        public final Bitmap bitmap;
        public final int width, height;
        Glass(Bitmap bitmap, int width, int height) { this.bitmap = bitmap; this.width = width; this.height = height; }
    }
    /**
     * The notification centre's picture as it is displayed (blurred, zone and brightness applied) while that picture is showing: the
     * glass needs it only when the panel really draws it. Null in the control centre, on the lock screen, with no picture, or before
     * the first load has finished.
     */
    public Glass glass() {
        Frame current = frame;
        if (current == null || config.disabled || !visible || !"separate".equals(mode) || rootWidth <= 0 || rootHeight <= 0) return null;
        if (controlWeight > .01f) return null;
        Look look = current.first.look;
        Bitmap bitmap = look == null ? null : look.glass();
        return bitmap == null ? null : new Glass(bitmap, rootWidth, rootHeight);
    }

    // ---- images

    /** One scene's pictures: the decoded one, its blurred copy (zone included), and the brightened copy the glass samples. */
    static final class Look {
        final Bitmap sharp;
        final boolean zone;
        private final float brightness;
        /** Worker thread only: the small, un-blurred copy the blurred ones are made of, and the blurs of it. Kept only while a zone needs them. */
        private int[] smallPixels, mainPixels, zonePixels;
        private final int smallWidth, smallHeight;
        private final float sigmaMain, sigmaZone, zoneLift, zoneSaturation;
        private final int zoneTint;
        /** No pixel of the picture is see-through: two such pictures can be cross-faded without an offscreen buffer. */
        final boolean opaque;
        private volatile Bitmap blurred, glass;
        private Look(Bitmap sharp, int[] small, int smallWidth, int smallHeight, float brightness, boolean zone, float sigmaMain, float sigmaZone,
                float zoneLift, float zoneSaturation, int zoneTint) {
            boolean solid = true;
            for (int i = 0; i < small.length && solid; i++) solid = (small[i] >>> 24) == 255;
            this.opaque = solid;
            this.sharp = sharp; this.smallPixels = small; this.smallWidth = smallWidth; this.smallHeight = smallHeight;
            this.brightness = brightness; this.zone = zone; this.sigmaMain = sigmaMain; this.sigmaZone = sigmaZone;
            this.zoneLift = zoneLift; this.zoneSaturation = zoneSaturation; this.zoneTint = zoneTint;
        }
        private boolean zoneAdjusts() { return zoneLift != 0f || zoneSaturation != 100f || (zoneTint >>> 24) != 0; }
        Bitmap blurred() { return blurred; }
        Bitmap glass() { return glass; }
        /** Worker thread. {@code displayWidth/Height}: the screen's size in pixels, which a screen-pixel blur is measured against. */
        static Look build(Bitmap sharp, SceneConfig settings, boolean landscape, float density, int displayWidth, int displayHeight) {
            int bw = sharp.getWidth(), bh = sharp.getHeight();
            float shortEdge = Math.min(displayWidth, displayHeight), longEdge = Math.max(displayWidth, displayHeight);
            float screenW = landscape ? longEdge : shortEdge, screenH = landscape ? shortEdge : longEdge;
            // Screen pixels covered by one picture pixel (the picture is cropped to fill the screen).
            float cover = Math.max(screenW / bw, screenH / bh);
            float small = Math.min(1f, SMALL_EDGE / (float) Math.max(bw, bh));
            int sw = Math.max(2, Math.round(bw * small)), sh = Math.max(2, Math.round(bh * small));
            Bitmap scaled = scaleDown(sharp, sw, sh);
            int[] pixels = new int[sw * sh];
            scaled.getPixels(pixels, 0, sw, 0, 0, sw, sh);
            if (scaled != sharp) scaled.recycle();
            // A blur of {@code dp} on the screen is that many screen pixels; in the small copy one pixel is cover / small screen pixels.
            float perSmallPixel = cover / small;
            float sigmaMain = settings.blur * density / perSmallPixel, sigmaZone = settings.zoneBlur * density / perSmallPixel;
            Look look = new Look(sharp, pixels, sw, sh, settings.brightness, settings.zoneEnabled, sigmaMain, sigmaZone,
                    settings.zoneLift, settings.zoneSaturation, settings.zoneTint);
            look.publish(look.composite(0, 0, 0f, 0f));
            return look;
        }
        private static Bitmap scaleDown(Bitmap source, int width, int height) {
            Bitmap current = source;
            while (current.getWidth() / 2 >= width && current.getHeight() / 2 >= height) {
                Bitmap half = Bitmap.createScaledBitmap(current, current.getWidth() / 2, current.getHeight() / 2, true);
                if (current != source) current.recycle();
                current = half;
            }
            if (current.getWidth() == width && current.getHeight() == height) return current;
            Bitmap result = Bitmap.createScaledBitmap(current, width, height, true);
            if (current != source && current != result) current.recycle();
            return result;
        }
        /**
         * Worker thread. The blurred copy for this window size and zone: the main blur, with the zone's blur blended in down to the zone's
         * lower edge. With neither a blur nor a zone there is no blurred copy at all (the sharp picture is the picture), only the copy the glass
         * samples.
         */
        Composite composite(int width, int height, float zoneBottom, float feather) {
            if (mainPixels == null) {
                mainPixels = smallPixels.clone();
                if (sigmaMain >= .5f) WallpaperPixels.blur(mainPixels, smallWidth, smallHeight, sigmaMain);
            }
            int[] shown = mainPixels;
            if (zone && width > 0 && height > 0 && zoneBottom > 0f) {
                if (zonePixels == null) {
                    zonePixels = smallPixels.clone();
                    if (sigmaZone >= .5f) WallpaperPixels.blur(zonePixels, smallWidth, smallHeight, sigmaZone);
                    // What the zone does to the picture under the clock, after its blur: saturation, then the signed lift, then the tint.
                    WallpaperPixels.saturation(zonePixels, zoneSaturation);
                    WallpaperPixels.lift(zonePixels, zoneLift);
                    WallpaperPixels.tint(zonePixels, zoneTint);
                }
                // The picture is cropped to the window's shape: which of its rows show, and how many of them one window pixel covers.
                float cover = Math.max(width / (float) smallWidth, height / (float) smallHeight);
                float rowsPerPixel = 1f / cover, visibleRows = height / cover, top = (smallHeight - visibleRows) / 2f;
                shown = WallpaperPixels.zoneComposite(mainPixels, zonePixels, smallWidth, smallHeight, top, rowsPerPixel, zoneBottom, feather);
            }
            boolean blurredAtAll = sigmaMain >= .5f || zone && (sigmaZone >= .5f || zoneAdjusts());
            Bitmap blurredCopy = blurredAtAll ? Bitmap.createBitmap(shown, smallWidth, smallHeight, Bitmap.Config.ARGB_8888) : null;
            int[] lit = shown.clone();
            WallpaperPixels.brightness(lit, brightness);
            Bitmap glassCopy = Bitmap.createBitmap(lit, smallWidth, smallHeight, Bitmap.Config.ARGB_8888);
            if (!zone) { smallPixels = null; mainPixels = null; zonePixels = null; }
            return new Composite(blurredCopy, glassCopy);
        }
        void publish(Composite next) {
            if (next == null) return;
            blurred = next.blurred; glass = next.glass;
        }
    }
    static final class Composite {
        final Bitmap blurred, glass;
        Composite(Bitmap blurred, Bitmap glass) { this.blurred = blurred; this.glass = glass; }
    }

    private static final class Image {
        final Look look;
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        final PorterDuffXfermode add = new PorterDuffXfermode(PorterDuff.Mode.ADD);
        /** The window's visible part of the picture, as fractions of it (the picture is cropped to fill the window). */
        float cropLeft, cropTop, cropRight = 1f, cropBottom = 1f;
        final RectF destination = new RectF();
        private final Rect source = new Rect();
        Image(Look look, float brightness) {
            this.look = look;
            float multiplier = brightness <= 100f ? brightness / 100f : 2f - brightness / 100f;
            float offset = brightness > 100f ? (brightness - 100f) * 2.55f : 0f;
            if (brightness != 100f) paint.setColorFilter(new ColorMatrixColorFilter(new ColorMatrix(new float[]{
                    multiplier, 0, 0, 0, offset, 0, multiplier, 0, 0, offset,
                    0, 0, multiplier, 0, offset, 0, 0, 0, 1, 0})));
        }
        void resize(int width, int height) {
            destination.set(0f, 0f, width, height);
            Bitmap bitmap = look == null ? null : look.sharp;
            if (bitmap == null) return;
            int bitmapWidth = bitmap.getWidth(), bitmapHeight = bitmap.getHeight();
            cropLeft = 0f; cropTop = 0f; cropRight = 1f; cropBottom = 1f;
            if ((long) bitmapWidth * height > (long) bitmapHeight * width) {
                float visible = bitmapHeight * width / (float) height / bitmapWidth;
                cropLeft = (1f - visible) / 2f; cropRight = cropLeft + visible;
            } else {
                float visible = bitmapWidth * height / (float) width / bitmapHeight;
                cropTop = (1f - visible) / 2f; cropBottom = cropTop + visible;
            }
        }
        private void drawBitmap(Canvas canvas, Bitmap bitmap, float alpha, boolean additive) {
            if (bitmap == null || !(alpha > 0f)) return;
            int bw = bitmap.getWidth(), bh = bitmap.getHeight();
            int left = Math.round(cropLeft * bw), top = Math.round(cropTop * bh);
            source.set(left, top, Math.max(left + 1, Math.round(cropRight * bw)), Math.max(top + 1, Math.round(cropBottom * bh)));
            paint.setAlpha(Math.round(Math.max(0f, Math.min(1f, alpha)) * 255f));
            paint.setXfermode(additive ? add : null);
            canvas.drawBitmap(bitmap, source, destination, paint);
        }
        /**
         * {@code strength}: how much of the picture shows (0..1). {@code blend}: 0 the sharp picture, 1 the blurred copy. Two layers make the
         * mix without an offscreen buffer: the sharp one first, the blurred one over it; their alphas are chosen so the pair is
         * {@code (1 - blend)} sharp and {@code blend} blurred at the total coverage {@code strength} (added, the two weights are simply summed).
         */
        void draw(Canvas canvas, float strength, float blend, boolean additive) {
            if (look == null || look.sharp == null || !(strength > 0f)) return;
            Bitmap blurred = look.blurred();
            if (blurred == null || !(blend > 0f)) { drawBitmap(canvas, look.sharp, strength, additive); return; }
            if (blend >= 1f) { drawBitmap(canvas, blurred, strength, additive); return; }
            if (additive) {
                drawBitmap(canvas, look.sharp, strength * (1f - blend), true);
                drawBitmap(canvas, blurred, strength * blend, true);
                return;
            }
            float second = strength * blend;
            drawBitmap(canvas, look.sharp, WallpaperPixels.firstLayerAlpha(strength, second), false);
            drawBitmap(canvas, blurred, second, false);
        }
    }
    private static final class Frame {
        final Image first, second;
        final RectF bounds = new RectF();
        /** Both panels show the same picture (one was set and the other borrows it): a swipe changes nothing. */
        final boolean sameAsset;
        int width = -1, height = -1;
        Frame(Look first, Look second, float firstBrightness, float secondBrightness, boolean sameAsset) {
            this.first = new Image(first, firstBrightness); this.second = new Image(second, secondBrightness); this.sameAsset = sameAsset;
        }
        void resize(int width, int height) {
            if (this.width == width && this.height == height) return;
            this.width = width; this.height = height; bounds.set(0f, 0f, width, height);
            first.resize(width, height); second.resize(width, height);
        }
    }
    static final class SceneConfig {
        static final SceneConfig EMPTY = new SceneConfig("", false, "", 100f, 0f, false, true, 0f, 0f, 0f, 0f, 100f, 0);
        final String scene, revision;
        final boolean enabled, zoneEnabled, zoneFollow;
        final float brightness, blur, zoneBlur, zoneHeight, zoneFeather, zoneLift, zoneSaturation;
        /** The zone's tint: the colour with its strength as alpha (0 = none). */
        final int zoneTint;
        SceneConfig(String scene, boolean enabled, String revision, float brightness, float blur,
                boolean zoneEnabled, boolean zoneFollow, float zoneBlur, float zoneHeight, float zoneFeather,
                float zoneLift, float zoneSaturation, int zoneTint) {
            this.scene = scene; this.enabled = enabled; this.revision = revision; this.brightness = brightness; this.blur = blur;
            this.zoneEnabled = zoneEnabled; this.zoneFollow = zoneFollow; this.zoneBlur = zoneBlur; this.zoneHeight = zoneHeight; this.zoneFeather = zoneFeather;
            this.zoneLift = zoneLift; this.zoneSaturation = zoneSaturation; this.zoneTint = zoneTint;
        }
        /** Whether the zone changes the picture under the clock beyond blurring it. */
        boolean zoneAdjusts() { return zoneLift != 0f || zoneSaturation != 100f || (zoneTint >>> 24) != 0; }
        String asset() { return enabled && !revision.isEmpty() ? scene + "/" + revision : ""; }
        /** The same picture, brightness and blur, as shown on the other panel. */
        SceneConfig withoutZone() { return new SceneConfig(scene, enabled, revision, brightness, blur, false, true, 0f, 0f, 0f, 0f, 100f, 0); }
        /** Everything that changes the pictures that are drawn: the image, its brightness, its blur and its zone's blur. */
        boolean samePixels(SceneConfig other) {
            return asset().equals(other.asset()) && brightness == other.brightness && blur == other.blur && zoneEnabled == other.zoneEnabled
                    && (!zoneEnabled || zoneBlur == other.zoneBlur && zoneLift == other.zoneLift && zoneSaturation == other.zoneSaturation
                    && zoneTint == other.zoneTint);
        }
        @Override public boolean equals(Object object) {
            if (!(object instanceof SceneConfig)) return false;
            SceneConfig other = (SceneConfig) object;
            return enabled == other.enabled && brightness == other.brightness && scene.equals(other.scene) && revision.equals(other.revision)
                    && blur == other.blur && zoneEnabled == other.zoneEnabled && zoneFollow == other.zoneFollow && zoneBlur == other.zoneBlur
                    && zoneHeight == other.zoneHeight && zoneFeather == other.zoneFeather && zoneLift == other.zoneLift
                    && zoneSaturation == other.zoneSaturation && zoneTint == other.zoneTint;
        }
        @Override public int hashCode() { return Objects.hash(scene, enabled, revision, brightness, blur, zoneEnabled, zoneBlur, zoneHeight); }
    }
    private static final class LoadKey {
        final SceneConfig first, second;
        LoadKey(SceneConfig first, SceneConfig second) { this.first = first; this.second = second; }
        @Override public boolean equals(Object value) {
            if (!(value instanceof LoadKey)) return false;
            LoadKey other = (LoadKey) value;
            return first.samePixels(other.first) && second.samePixels(other.second);
        }
        @Override public int hashCode() { return Objects.hash(first.asset(), first.blur, first.brightness, second.asset(), second.blur, second.brightness); }
    }
    static final class Config {
        final boolean disabled, blurWithPull, blurWithPullLandscape, slidePortrait, slideLandscape;
        final SceneConfig[] scenes = new SceneConfig[6];
        boolean pull(boolean landscape) { return landscape ? blurWithPullLandscape : blurWithPull; }
        boolean slide(boolean landscape) { return landscape ? slideLandscape : slidePortrait; }
        Config(Bundle source) {
            boolean off = SafetyMode.enabled(source) || source == null;
            Object pull = source == null ? null : source.get(ShadeWallpaperSettings.BLUR_WITH_PULL);
            blurWithPull = !(pull instanceof Boolean) || (Boolean) pull;
            Object pullLandscape = source == null ? null : source.get(ShadeWallpaperSettings.LANDSCAPE_BLUR_WITH_PULL);
            blurWithPullLandscape = !(pullLandscape instanceof Boolean) || (Boolean) pullLandscape;
            slidePortrait = source != null && Boolean.TRUE.equals(source.get(ShadeWallpaperSettings.SWIPE_SLIDE));
            slideLandscape = source != null && Boolean.TRUE.equals(source.get(ShadeWallpaperSettings.LANDSCAPE_SWIPE_SLIDE));
            for (int i = 0; i < scenes.length; i++) {
                String scene = ShadeWallpaperSettings.SCENES.get(i);
                Object revision = source == null ? null : source.get(ShadeWallpaperSettings.revisionKey(scene));
                String valid = revision instanceof String && ShadeWallpaperSettings.validRevision((String) revision) ? (String) revision : "";
                boolean zone = false, follow = true;
                float zoneBlur = ShadeWallpaperSettings.DEFAULT_ZONE_BLUR_DP, zoneHeight = ShadeWallpaperSettings.DEFAULT_ZONE_HEIGHT_DP,
                        zoneFeather = ShadeWallpaperSettings.DEFAULT_ZONE_FEATHER_DP;
                float zoneLift = ShadeWallpaperSettings.DEFAULT_ZONE_LIFT, zoneSaturation = ShadeWallpaperSettings.DEFAULT_ZONE_SATURATION;
                int zoneTint = 0;
                if (ShadeWallpaperSettings.hasZone(scene) && source != null) {
                    zone = Boolean.TRUE.equals(source.get(ShadeWallpaperSettings.zoneEnabledKey(scene)));
                    Object followValue = source.get(ShadeWallpaperSettings.zoneFollowKey(scene));
                    follow = !(followValue instanceof Boolean) || (Boolean) followValue;
                    zoneBlur = ShadeWallpaperSettings.zoneBlur(source.get(ShadeWallpaperSettings.zoneBlurKey(scene)));
                    zoneHeight = ShadeWallpaperSettings.zoneHeight(source.get(ShadeWallpaperSettings.zoneHeightKey(scene)));
                    zoneFeather = ShadeWallpaperSettings.zoneFeather(source.get(ShadeWallpaperSettings.zoneFeatherKey(scene)));
                    zoneLift = ShadeWallpaperSettings.zoneLift(source.get(ShadeWallpaperSettings.zoneLiftKey(scene)));
                    zoneSaturation = ShadeWallpaperSettings.zoneSaturation(source.get(ShadeWallpaperSettings.zoneSaturationKey(scene)));
                    float strength = ShadeWallpaperSettings.zoneTintStrength(source.get(ShadeWallpaperSettings.zoneTintStrengthKey(scene)));
                    Object tint = source.get(ShadeWallpaperSettings.zoneTintKey(scene));
                    int rgb = (tint instanceof Number ? ((Number) tint).intValue() : ShadeWallpaperSettings.DEFAULT_ZONE_TINT) & 0x00ffffff;
                    zoneTint = Math.round(strength * 2.55f) << 24 | rgb;
                }
                scenes[i] = new SceneConfig(scene, ShadeWallpaperSettings.enabled(source, scene), valid,
                        ShadeWallpaperSettings.brightness(source == null ? null : source.get(ShadeWallpaperSettings.brightnessKey(scene))),
                        ShadeWallpaperSettings.blur(source == null ? null : source.get(ShadeWallpaperSettings.blurKey(scene))),
                        zone, follow, zoneBlur, zoneHeight, zoneFeather, zoneLift, zoneSaturation, zoneTint);
            }
            // Nothing to show (no page and scene on with a picture): the whole feature rests, as when it is switched off.
            boolean any = false;
            for (SceneConfig scene : scenes) if (!scene.asset().isEmpty()) any = true;
            disabled = off || !any;
        }
        boolean sameAssets(Config other) {
            if (disabled != other.disabled) return false;
            for (int i = 0; i < scenes.length; i++) if (!scenes[i].asset().equals(other.scenes[i].asset())) return false;
            return true;
        }
        /** The same pictures: only the pull behaviour, the zone's height or feather may differ (those need no new pixels). */
        boolean sameLooks(Config other) {
            if (disabled != other.disabled) return false;
            for (int i = 0; i < scenes.length; i++) if (!scenes[i].samePixels(other.scenes[i])) return false;
            return true;
        }
        @Override public boolean equals(Object value) {
            return value instanceof Config && disabled == ((Config) value).disabled && blurWithPull == ((Config) value).blurWithPull
                    && blurWithPullLandscape == ((Config) value).blurWithPullLandscape && slidePortrait == ((Config) value).slidePortrait
                    && slideLandscape == ((Config) value).slideLandscape && Arrays.equals(scenes, ((Config) value).scenes);
        }
        @Override public int hashCode() { return 31 * Boolean.hashCode(disabled) + Arrays.hashCode(scenes); }
    }
}
