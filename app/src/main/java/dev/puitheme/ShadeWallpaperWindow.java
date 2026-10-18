// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

/**
 * The pull-down wallpaper's own window, under the notification shade.
 *
 * <p>The system builds the shade's glass (the notification cards, the tiles, the window blur) by blurring what is <em>behind the shade
 * window</em>. A picture painted inside the shade window can therefore never be what the glass is made of: the cards would blur the real
 * wallpaper or the app underneath while the picture shows around them. This window sits between the apps and the shade, shows only the
 * picture (not touchable, not focusable, hidden unless the panel is being pulled), and so everything the shade does to what is behind it
 * is done to the picture. If the system refuses the window, the picture is still painted inside the shade as before.
 *
 * <p>Device code162 (swiping between the two panels must not stutter): each panel's picture is drawn once into a view of its own. A
 * swipe only sets their alpha, translation and blend, which the render thread applies; the pictures are drawn again only when they
 * change (picture, size or pull).
 */
final class ShadeWallpaperWindow implements ShadeWallpaper.Layer {
    private static final String TAG = "C17ShadeWallpaper";
    /**
     * The window types to try, in order: TYPE_APPLICATION_OVERLAY (layer 11 on this system: above the apps, under the status bar and the
     * shade, which are 15 and 17), then TYPE_STATUS_BAR_ADDITIONAL (layer 16). The legacy TYPE_STATUS_BAR_PANEL is not valid any more.
     */
    static final int[] TYPES = {2038, 2041};
    private int type = TYPES[0];
    private final ShadeWallpaper source;
    private final ShadeWallpaper.Composition composition = new ShadeWallpaper.Composition();
    private final View[] sides = new View[2];
    private final boolean[] layered = new boolean[2];
    private final Paint add = new Paint();
    private WindowManager manager;
    private View view;
    private boolean failed, lastVisible;
    private int drawnRevision = Integer.MIN_VALUE;

    ShadeWallpaperWindow(ShadeWallpaper source) {
        this.source = source;
        add.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.ADD));
    }

    @Override public boolean ready() { return view != null && !failed; }

    @Override public void sync(Context context, boolean visible) {
        if (visible != lastVisible) { lastVisible = visible; Log.i(TAG, "sync visible=" + visible + " failed=" + failed + " context=" + (context != null) + " view=" + (view != null)); }
        if (failed || context == null) return;
        if (view == null) {
            if (!visible) return;
            if (!add(context)) { Log.w(TAG, "wallpaper window could not be added (failed=" + failed + ")"); return; }
        }
        int wanted = visible ? View.VISIBLE : View.GONE;
        if (view.getVisibility() != wanted) view.setVisibility(wanted);
        if (!visible) return;
        int revision = source.contentRevision();
        if (revision != drawnRevision) {
            drawnRevision = revision;
            for (View side : sides) if (side != null) side.invalidate();
        }
        compose();
    }

    @Override public void compose() {
        View container = view;
        if (container == null || container.getVisibility() != View.VISIBLE) return;
        source.compose(composition, container.getWidth());
        place(0, composition.alpha0, composition.shift0, false);
        place(1, composition.alpha1, composition.shift1, composition.additive);
    }

    private void place(int index, float alpha, float shift, boolean additive) {
        View side = sides[index];
        if (side == null) return;
        if (side.getAlpha() != alpha) side.setAlpha(alpha);
        if (side.getTranslationX() != shift) side.setTranslationX(shift);
        if (layered[index] != additive) {
            // The see-through mix adds the second picture onto the first: a cached layer composited with ADD.
            layered[index] = additive;
            side.setLayerType(additive ? View.LAYER_TYPE_HARDWARE : View.LAYER_TYPE_NONE, additive ? add : null);
        }
    }

    private boolean add(Context context) {
        WindowManager windows = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (windows == null) { failed = true; return false; }
        Throwable last = null;
        for (int candidate : TYPES) {
            try {
                FrameLayout created = new FrameLayout(context);
                View[] made = new View[2];
                for (int index = 0; index < made.length; index++) {
                    final int side = index;
                    made[index] = new View(context) {
                        @Override protected void onDraw(Canvas canvas) { source.paintSide(canvas, getWidth(), getHeight(), side); }
                        // One picture (or its sharp/blurred pair at the ends of the pull): the view alpha goes to each bitmap draw,
                        // so a fade never needs an offscreen buffer.
                        @Override public boolean hasOverlappingRendering() { return false; }
                    };
                    made[index].setWillNotDraw(false);
                    created.addView(made[index], new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
                }
                created.setVisibility(View.GONE);
                WindowManager.LayoutParams params = new WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT, candidate,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                                | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                        PixelFormat.TRANSLUCENT);
                params.setTitle(TAG);
                params.gravity = Gravity.TOP | Gravity.START;
                params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
                windows.addView(created, params);
                manager = windows; view = created; type = candidate;
                sides[0] = made[0]; sides[1] = made[1]; layered[0] = layered[1] = false; drawnRevision = Integer.MIN_VALUE;
                Log.i(TAG, "wallpaper window added under the shade (type " + candidate + ")");
                return true;
            } catch (Throwable refused) {
                last = refused;
                Log.w(TAG, "wallpaper window type " + candidate + " refused: " + refused.getMessage());
            }
        }
        failed = true; view = null; manager = null;
        Log.w(TAG, "no wallpaper window type accepted; the picture stays inside the shade", last);
        return false;
    }

    @Override public void release() {
        View old = view; WindowManager windows = manager;
        view = null; manager = null; sides[0] = sides[1] = null;
        if (old == null || windows == null) return;
        try { windows.removeViewImmediate(old); } catch (Throwable ignored) { /* already gone */ }
    }
}
