// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie

package dev.puitheme;

import android.graphics.Bitmap;
import android.graphics.RenderEffect;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * The lock screen's own clock glass, borrowed for the notification page's clock.
 *
 * The clock plugin builds its glass (digits that show the wallpaper through them, with refraction, glow and rim) in a
 * {@code GlassEffectBuilder}. While the lock screen runs, one such builder is alive; this class remembers it (from the plugin's
 * own {@code buildRenderEffect} call), and lets another view own a second one of the same class: the same region colours and
 * material, its own size, and its own picture: the pull-down panel's wallpaper, never the lock screen's. Nothing of the plugin is
 * copied: its classes are used by reflection, and anything missing or changed (another firmware, no lock screen shown yet) simply
 * reports "unavailable" so the caller keeps its own look.
 */
final class LockGlassBridge {
    static final String BUILDER = "com.oplus.keyguard.clock.common.view.livecontent.effect.shader.glass.GlassEffectBuilder";
    private static volatile WeakReference<Object> source = new WeakReference<>(null);
    private static Members members;
    private static volatile boolean unsupported;
    private static volatile String lastFailure = "";

    private LockGlassBridge() { }

    /** Called after every plugin build; only remembers the instance. */
    static void captured(Object builder) {
        if (builder == null || unsupported || building) return;
        if (source.get() != builder) source = new WeakReference<>(builder);
    }

    static boolean available() { return !unsupported && source.get() != null; }
    static String lastFailure() { return lastFailure; }

    /** True while one of this class's own builders is building, so the hook on the plugin's build does not mistake it for the lock clock's. */
    private static boolean building;

    /** One view's borrowed effect. Main thread only; the compiled shaders live as long as the session. */
    static final class Session {
        private Object mine;
        private Members m;
        private Bitmap syncedPicture;
        private int width, height, rectX = Integer.MIN_VALUE, rectY = Integer.MIN_VALUE, pictureWidth, pictureHeight;
        private float amount = Float.NaN;
        private RenderEffect effect;

        /**
         * The glass of the lock clock's material, over what is really behind the notification page's clock: the panel's own wallpaper (blurred,
         * with its zone and brightness) cropped to the window, the clock's rectangle in the window as the area it samples. Never the lock
         * screen's wallpaper. Null when the lock clock's builder has not been seen yet, or its classes changed.
         *
         * @param x, y the clock view's position in the window (pixels)
         */
        RenderEffect update(int newWidth, int newHeight, float newAmount, ShadeWallpaper.Glass picture, int x, int y) {
            Object src = source.get();
            if (src == null || unsupported || picture == null || picture.bitmap == null || newWidth <= 0 || newHeight <= 0) return null;
            try {
                synchronized (LockGlassBridge.class) {
                    if (m == null || m.type != src.getClass()) { m = members(src.getClass()); mine = null; syncedPicture = null; effect = null; }
                    boolean dirty = effect == null;
                    Bitmap bitmap = picture.bitmap;
                    if (bitmap.isRecycled()) { lastFailure = "wallpaper picture recycled"; return effect; }
                    Object[] fromConfig = (Object[]) m.regionFrom.get(src), toConfig = (Object[]) m.regionTo.get(src);
                    if (fromConfig == null || toConfig == null) { lastFailure = "no region colours"; return effect; }
                    if (mine == null) {
                        mine = m.constructor.newInstance();
                        m.init.invoke(mine, newWidth, newHeight, m.effectLight.getBoolean(src), "c17-notification-clock");
                        width = newWidth; height = newHeight; dirty = true;
                        for (int i = 0; i < Math.min(6, Math.min(fromConfig.length, toConfig.length)); i++)
                            if (fromConfig[i] != null && toConfig[i] != null) m.setConfig.invoke(mine, i, fromConfig[i], toConfig[i]);
                        m.setP3.invoke(mine, m.p3.getBoolean(src));
                        m.setHasColon.invoke(mine, m.hasColon.getBoolean(src));
                        m.setMaskColorProgress.invoke(mine, m.maskColorProgress.getFloat(src));
                        m.setMixProgress.invoke(mine, 0f);
                        m.setOnlyViewContext.invoke(mine, false);
                    }
                    if (newWidth != width || newHeight != height) {
                        m.setEffectSize.invoke(mine, newWidth, newHeight); width = newWidth; height = newHeight; dirty = true;
                    }
                    if (syncedPicture != bitmap) {
                        // The same picture twice: there is no second wallpaper to cross over to.
                        m.setWallpaperBg.invoke(mine, bitmap, bitmap);
                        syncedPicture = bitmap; dirty = true; rectX = Integer.MIN_VALUE;
                    }
                    if (rectX != x || rectY != y || pictureWidth != picture.width || pictureHeight != picture.height) {
                        // The picture is cropped to fill the resolution; the crop rectangle is the clock's own place in it; no rotation, no zoom.
                        Object resolution = m.vec2.newInstance((float) picture.width, (float) picture.height);
                        Object crop = m.vec4.newInstance((float) x, (float) y, (float) newWidth, (float) newHeight);
                        m.setClockRect.invoke(mine, resolution, crop, 0, 1f);
                        rectX = x; rectY = y; pictureWidth = picture.width; pictureHeight = picture.height; dirty = true;
                    }
                    if (newAmount != amount) { m.setGlass.invoke(mine, Math.max(0f, Math.min(1f, newAmount))); amount = newAmount; dirty = true; }
                    if (dirty) {
                        building = true;
                        try { m.build.invoke(mine); } finally { building = false; }
                        Object built = m.getEffect.invoke(mine);
                        effect = built instanceof RenderEffect ? (RenderEffect) built : null;
                        if (effect == null) lastFailure = "builder returned no effect";
                    }
                    if (effect != null) lastFailure = "";
                    return effect;
                }
            } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
                Throwable cause = failure.getCause() != null ? failure.getCause() : failure;
                lastFailure = cause.getClass().getSimpleName() + ": " + cause.getMessage();
                if (failure instanceof NoSuchMethodException || failure instanceof NoSuchFieldException || failure instanceof ClassNotFoundException)
                    unsupported = true;
                mine = null; effect = null; syncedPicture = null;
                ModuleDiagnostics.error("lock_glass", "Lock-screen glass could not be borrowed", failure);
                return null;
            }
        }

        void release() {
            Object builder = mine; mine = null; effect = null; syncedPicture = null; rectX = rectY = Integer.MIN_VALUE;
            if (builder == null || m == null) return;
            try { m.release.invoke(builder); } catch (ReflectiveOperationException | RuntimeException ignored) { }
        }
    }

    private static Members members(Class<?> type) throws ReflectiveOperationException {
        if (members != null && members.type == type) return members;
        members = new Members(type);
        return members;
    }

    private static final class Members {
        final Class<?> type;
        final Constructor<?> constructor, vec2, vec4;
        final Method init, setEffectSize, setWallpaperBg, setClockRect, setConfig, setP3, setHasColon, setMaskColorProgress,
                setMixProgress, setGlass, setOnlyViewContext, build, getEffect, release;
        final Field fromWallpaper, toWallpaper, wallpaperResolution, wallpaperCropRect, rotation, contentScale, regionFrom, regionTo,
                effectLight, p3, hasColon, maskColorProgress;
        Members(Class<?> type) throws ReflectiveOperationException {
            this.type = type;
            constructor = type.getDeclaredConstructor(); constructor.setAccessible(true);
            init = method(type, "init", Integer.TYPE, Integer.TYPE, Boolean.TYPE, String.class);
            setEffectSize = method(type, "setEffectSize", Integer.TYPE, Integer.TYPE);
            setWallpaperBg = method(type, "setWallpaperBg", Bitmap.class, Bitmap.class);
            Class<?> vec2Type = Class.forName(type.getName() + "$Vec2", false, type.getClassLoader());
            Class<?> vec4Type = Class.forName(type.getName() + "$Vec4", false, type.getClassLoader());
            vec2 = vec2Type.getDeclaredConstructor(Float.TYPE, Float.TYPE); vec2.setAccessible(true);
            vec4 = vec4Type.getDeclaredConstructor(Float.TYPE, Float.TYPE, Float.TYPE, Float.TYPE); vec4.setAccessible(true);
            setClockRect = method(type, "setClockRect", vec2Type, vec4Type, Integer.TYPE, Float.TYPE);
            Class<?> config = Class.forName(type.getPackage().getName() + ".GlassRegionConfig", false, type.getClassLoader());
            setConfig = method(type, "setConfig", Integer.TYPE, config, config);
            setP3 = method(type, "setP3ColorSpace", Boolean.TYPE);
            setHasColon = method(type, "setHasColonMix", Boolean.TYPE);
            setMaskColorProgress = method(type, "setMaskColorProgress", Float.TYPE);
            setMixProgress = method(type, "setMixProgress", Float.TYPE);
            setGlass = method(type, "setGlass", Float.TYPE);
            setOnlyViewContext = method(type, "setOnlyViewContext", Boolean.TYPE);
            build = method(type, "buildRenderEffect");
            getEffect = method(type, "getRenderEffect");
            release = method(type, "release");
            fromWallpaper = field(type, "fromWallpaperBitmap"); toWallpaper = field(type, "toWallpaperBitmap");
            wallpaperResolution = field(type, "wallpaperResolution"); wallpaperCropRect = field(type, "wallpaperCropRect");
            rotation = field(type, "wallpaperContentRotation"); contentScale = field(type, "contentDisplayScale");
            regionFrom = field(type, "regionConfigsFrom"); regionTo = field(type, "regionConfigsTo");
            effectLight = field(type, "effectLightEnabled"); p3 = field(type, "isP3ColorSpace");
            hasColon = field(type, "hasColonMix"); maskColorProgress = field(type, "maskColorProgress");
        }
        private static Method method(Class<?> type, String name, Class<?>... parameters) throws NoSuchMethodException {
            Method found = type.getDeclaredMethod(name, parameters); found.setAccessible(true); return found;
        }
        private static Field field(Class<?> type, String name) throws NoSuchFieldException {
            Field found = type.getDeclaredField(name); found.setAccessible(true); return found;
        }
    }
}
