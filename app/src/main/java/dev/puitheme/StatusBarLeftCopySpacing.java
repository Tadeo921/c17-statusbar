// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.view.View;
import android.view.ViewTreeObserver;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Map;
import java.util.WeakHashMap;

/** Draw-only spacing between the verified native clock and notification animation copies. */
final class StatusBarLeftCopySpacing {
    interface ClockOffset { float pixels(View source); }
    private final Map<View, Binding> bindings = new WeakHashMap<>();
    private final Map<View, Pair> pairs = new WeakHashMap<>();
    private final ClockOffset clockOffset;
    private boolean enabled;
    StatusBarLeftCopySpacing(ClockOffset reader) { clockOffset = reader; }
    void configure(boolean value) {
        enabled = value && !ModuleLifecycle.removed();
        if (!enabled) clear();
    }
    void observe(View copy, View source, int kind, float nativeOffset) {
        if (!enabled || ModuleLifecycle.removed() || copy == null) return;
        if (source == null || kind != StatusIconTransition.CLOCK && kind != StatusIconTransition.NOTIFICATIONS
                || !Float.isFinite(nativeOffset)) { forget(copy); return; }
        Binding old = bindings.get(copy);
        boolean changed = old == null || old.source.get() != source || old.kind != kind || old.offset != nativeOffset;
        if (changed) {
            forget(copy); Binding binding = new Binding(copy, source, kind, nativeOffset);
            bindings.put(copy, binding);
            copy.addOnAttachStateChangeListener(binding); source.addOnAttachStateChangeListener(binding);
        }
        if (kind == StatusIconTransition.NOTIFICATIONS) pair(copy);
        else if (changed) for (Map.Entry<View, Binding> entry : new ArrayList<>(bindings.entrySet()))
            if (entry.getValue().kind == StatusIconTransition.NOTIFICATIONS) pair(entry.getKey());
    }
    private void pair(View notifications) {
        if (notifications == null) return;
        Pair existing = pairs.get(notifications);
        if (existing != null) {
            if (!existing.released && host(notifications) == existing.row.get()
                    && host(existing.clock.get()) == existing.row.get()) return;
            existing.release(); pairs.remove(notifications);
        }
        View row = host(notifications);
        if (row == null) return;
        Binding iconBinding = bindings.get(notifications);
        View iconSource = iconBinding == null ? null : iconBinding.source.get();
        if (iconSource == null) return;
        for (Map.Entry<View, Binding> entry : bindings.entrySet()) {
            View clock = entry.getKey();
            if (entry.getValue().kind != StatusIconTransition.CLOCK || clock == null || host(clock) != row) continue;
            View timeSource = entry.getValue().source.get();
            if (timeSource == null || timeSource.getRootView() != iconSource.getRootView()) continue;
            Pair pair = new Pair(clock, notifications, row);
            pairs.put(notifications, pair);
            row.getViewTreeObserver().addOnPreDrawListener(pair);
            row.addOnAttachStateChangeListener(pair);
            notifications.invalidate();
            return;
        }
    }
    // Each fake frame lives in one direct native container under the same fake row.
    private static View host(View copy) {
        if (copy == null || !(copy.getParent() instanceof View)) return null;
        View container = (View) copy.getParent();
        return container.getParent() instanceof View ? (View) container.getParent() : null;
    }
    DrawScope beginDraw(View copy, Canvas canvas) {
        Pair pair = pairs.get(copy);
        if (pair == null || canvas == null) return null;
        float distance = pair.distance(); pair.recorded = distance;
        if (!Float.isFinite(distance) || Math.abs(distance) < .001f) return null;
        int saved = canvas.save(); canvas.translate(distance, 0f);
        return new DrawScope(canvas, saved);
    }
    boolean paired(View copy) { return pairs.containsKey(copy); }
    void forget(View copy) {
        Binding binding = bindings.remove(copy);
        if (binding != null) binding.release();
        for (Map.Entry<View, Pair> entry : new ArrayList<>(pairs.entrySet()))
            if (entry.getKey() == copy || entry.getValue().clock.get() == copy) {
                entry.getValue().release(); pairs.remove(entry.getKey());
            }
    }
    void clear() {
        for (Pair pair : new ArrayList<>(pairs.values())) pair.release();
        pairs.clear();
        for (Binding binding : new ArrayList<>(bindings.values())) binding.release();
        bindings.clear();
    }
    static float correction(RectF clock, RectF icons, boolean rtl, float gap) {
        if (clock == null || icons == null || !Float.isFinite(gap) || !finite(clock) || !finite(icons)
                || clock.width() <= 0f || icons.width() <= 0f) return 0f;
        return rtl ? Math.min(0f, clock.left - gap - icons.right) : Math.max(0f, clock.right + gap - icons.left);
    }
    private static boolean finite(RectF bounds) {
        return Float.isFinite(bounds.left) && Float.isFinite(bounds.top)
                && Float.isFinite(bounds.right) && Float.isFinite(bounds.bottom);
    }
    static final class DrawScope implements AutoCloseable {
        private final Canvas canvas; private final int saved; private boolean closed;
        DrawScope(Canvas value, int save) { canvas = value; saved = save; }
        @Override public void close() { if (!closed) { closed = true; canvas.restoreToCount(saved); } }
    }
    private final class Binding implements View.OnAttachStateChangeListener {
        final WeakReference<View> copy, source; final int kind; final float offset;
        Binding(View frame, View view, int type, float x) {
            copy = new WeakReference<>(frame); source = new WeakReference<>(view); kind = type; offset = x;
        }
        @Override public void onViewAttachedToWindow(View view) {
            if (kind == StatusIconTransition.NOTIFICATIONS) pair(copy.get());
            else for (Map.Entry<View, Binding> entry : new ArrayList<>(bindings.entrySet()))
                if (entry.getValue().kind == StatusIconTransition.NOTIFICATIONS) pair(entry.getKey());
        }
        @Override public void onViewDetachedFromWindow(View view) { forget(copy.get()); }
        void release() {
            View frame = copy.get(), view = source.get();
            if (frame != null) frame.removeOnAttachStateChangeListener(this);
            if (view != null) view.removeOnAttachStateChangeListener(this);
        }
    }
    private final class Pair implements ViewTreeObserver.OnPreDrawListener, View.OnAttachStateChangeListener {
        final WeakReference<View> clock, notifications, row;
        final Matrix clockMatrix = new Matrix(), iconMatrix = new Matrix();
        final RectF clockBounds = new RectF(), iconBounds = new RectF();
        final float[] axes = new float[9], screen = new float[2];
        final int[] where = new int[2];
        float recorded = Float.NaN; boolean released;
        Pair(View time, View icons, View host) {
            clock = new WeakReference<>(time); notifications = new WeakReference<>(icons); row = new WeakReference<>(host);
        }
        float distance() {
            View time = clock.get(), icons = notifications.get();
            Binding timeBinding = bindings.get(time), iconBinding = bindings.get(icons);
            View timeSource = timeBinding == null ? null : timeBinding.source.get();
            View iconSource = iconBinding == null ? null : iconBinding.source.get();
            if (released || !enabled || ModuleLifecycle.removed() || time == null || icons == null
                    || host(time) != row.get() || host(icons) != row.get() || !attached(time) || !attached(icons)
                    || timeSource == null || iconSource == null || !timeSource.isAttachedToWindow()
                    || !iconSource.isAttachedToWindow() || timeSource.getRootView() != iconSource.getRootView()
                    || timeSource.getWidth() <= 0 || iconSource.getWidth() <= 0) return 0f;
            float timeX = clockOffset == null ? 0f : clockOffset.pixels(timeSource);
            float iconX = icons.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL
                    ? icons.getWidth() - iconSource.getWidth() - iconBinding.offset : iconBinding.offset;
            clockBounds.set(timeX, 0f, timeX + timeSource.getWidth(), timeSource.getHeight());
            iconBounds.set(iconX, 0f, iconX + iconSource.getWidth(), iconSource.getHeight());
            clockMatrix.reset(); iconMatrix.reset();
            time.transformMatrixToGlobal(clockMatrix); icons.transformMatrixToGlobal(iconMatrix);
            clockMatrix.getValues(axes);
            if (!supportedAxes()) return 0f;
            clockMatrix.mapRect(clockBounds); iconMatrix.mapRect(iconBounds); iconMatrix.getValues(axes);
            // Verified native springs only scale and translate. Unknown transforms retain the native draw.
            if (!supportedAxes()) return 0f;
            boolean rtl = icons.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
            float gap = 4f * icons.getResources().getDisplayMetrics().density;
            // The copies fly into the real status bar. The space the real clock and icons keep at rest is the most the
            // animation may ask for: a larger gap would vanish in one frame when the real views take over.
            if (!rtl) gap = Math.min(gap, restingGap(timeSource, iconSource, timeX));
            return correction(clockBounds, iconBounds, rtl, gap) / axes[0];
        }
        /** The gap the real Phone views keep between clock and notifications: where the copies end up. */
        private float restingGap(View timeSource, View iconSource, float timeOffset) {
            timeSource.getLocationOnScreen(where); screen[0] = where[0];
            iconSource.getLocationOnScreen(where); screen[1] = where[0];
            return screen[1] - (screen[0] + timeOffset + timeSource.getWidth());
        }
        private boolean supportedAxes() {
            for (float value : axes) if (!Float.isFinite(value)) return false;
            return Math.abs(axes[0]) >= .001f && Math.abs(axes[1]) <= .001f && Math.abs(axes[3]) <= .001f
                    && axes[6] == 0f && axes[7] == 0f && axes[8] == 1f;
        }
        // Native alpha and vertical springs must not switch an X correction on/off at a frame boundary.
        // Prepare the same projected gap while transparent, before the first visible display list.
        private boolean attached(View view) {
            for (View current = view; current != null; current = current.getParent() instanceof View ? (View) current.getParent() : null) {
                if (!current.isAttachedToWindow() || current.getVisibility() == View.GONE) return false;
                if (current == row.get()) return true;
            }
            return false;
        }
        @Override public boolean onPreDraw() {
            View icons = notifications.get(), time = clock.get();
            if (icons == null || time == null || host(time) != row.get() || host(icons) != row.get()) {
                release(); if (icons != null) pairs.remove(icons); return true;
            }
            float next = distance();
            if (!Float.isFinite(recorded) || Math.abs(next - recorded) > .001f) {
                recorded = next; icons.invalidate();
            }
            return true;
        }
        @Override public void onViewAttachedToWindow(View view) { }
        @Override public void onViewDetachedFromWindow(View view) {
            View icons = notifications.get(); release(); if (icons != null) pairs.remove(icons);
        }
        void release() {
            if (released) return; released = true;
            View host = row.get(), icons = notifications.get();
            if (host != null) {
                ViewTreeObserver observer = host.getViewTreeObserver();
                if (observer.isAlive()) observer.removeOnPreDrawListener(this);
                host.removeOnAttachStateChangeListener(this);
            }
            if (icons != null) icons.invalidate();
        }
    }
}
