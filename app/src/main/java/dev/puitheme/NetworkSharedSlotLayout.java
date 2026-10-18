// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Map;
import java.util.WeakHashMap;

/** Instance bridge between measured native mobile fragments and the outer status-bar flow.
 * All coordinates are physical pixels inside the original mobile host. Neither this
 * bridge nor its backends change that host's translation, subscription or visibility. */
public final class NetworkSharedSlotLayout {
    interface Backend {
        Frame frame(View host);
        boolean target(Target value);
        void restore();
    }
    public static final class Frame {
        public final boolean valid, labelPresent;
        public final int labelWidthPx, signalWidthPx, nativeLabelLeftPx, nativeSignalLeftPx, nativeHostWidthPx;
        /** Current measured placement, rather than the native-order baseline. */
        public final int currentSignalLeftPx;
        private final WeakReference<View> host;
        private final Backend backend;
        Frame(View host, Backend backend, boolean valid, boolean present, int labelWidth, int signalWidth,
                int labelLeft, int signalLeft, int nativeWidth) {
            this(host, backend, valid, present, labelWidth, signalWidth, labelLeft, signalLeft, nativeWidth, signalLeft);
        }
        Frame(View host, Backend backend, boolean valid, boolean present, int labelWidth, int signalWidth,
                int labelLeft, int signalLeft, int nativeWidth, int currentSignalLeft) {
            this.host = new WeakReference<>(host); this.backend = backend; this.valid = valid;
            labelPresent = present; labelWidthPx = labelWidth; signalWidthPx = signalWidth;
            nativeLabelLeftPx = labelLeft; nativeSignalLeftPx = signalLeft; nativeHostWidthPx = nativeWidth;
            currentSignalLeftPx = currentSignalLeft;
        }
        public View host() { return host.get(); }
    }
    static final class Target {
        final int width, labelLeft, signalLeft;
        final boolean labelVisible, signalVisible;
        Target(int width, int label, int signal, boolean labelVisible, boolean signalVisible) {
            this.width = width; labelLeft = label; signalLeft = signal;
            this.labelVisible = labelVisible; this.signalVisible = signalVisible;
        }
        boolean same(Target other) { return other != null && width == other.width && labelLeft == other.labelLeft
                && signalLeft == other.signalLeft && labelVisible == other.labelVisible && signalVisible == other.signalVisible; }
    }
    private final Map<View, Backend> backends = new WeakHashMap<>();
    private Class<?> nativeContainer, displayable;
    private Method slot;
    private boolean enabled, released;
    private String lastFailure = "none";
    public String failure() { return lastFailure; }

    public void resolve(ClassLoader loader) throws ReflectiveOperationException {
        nativeContainer = loader.loadClass("com.android.systemui.statusbar.phone.StatusIconContainer");
        displayable = loader.loadClass("com.android.systemui.statusbar.StatusIconDisplayable");
        slot = displayable.getMethod("getSlot");
    }
    public void configure(boolean value) {
        value &= !released;
        if (enabled == value) return;
        enabled = value;
        if (!enabled) for (Backend backend : new ArrayList<>(backends.values())) backend.restore();
    }
    public boolean enabled() { return enabled && !released; }
    /** Exact native mobile child of StatusIconContainer; never an arbitrary nearby row. */
    View host(View anchor) {
        if (!enabled() || slot == null) return null;
        View current = anchor;
        for (int depth = 0; current != null && depth < 16; depth++) {
            ViewParent parent = current.getParent();
            if (parent instanceof ViewGroup && nativeContainer.isInstance(parent) && displayable.isInstance(current)) {
                try {
                    Object name = slot.invoke(current);
                    if (name instanceof String && mobile((String) name)) return current;
                } catch (ReflectiveOperationException | RuntimeException unavailable) { return null; }
                return null;
            }
            current = parent instanceof View ? (View) parent : null;
        }
        return null;
    }
    private static boolean mobile(String name) { return name.equals("mobile") || name.startsWith("mobile_")
            || name.equals("stacked_mobile") || name.startsWith("stacked_mobile_"); }
    /** Physical native layout offset, excluding all transient translations/animation layers. */
    static int offset(View child, View ancestor) {
        long left = 0;
        for (int depth = 0; child != null && depth < 16; depth++) {
            if (child == ancestor) return left >= 0 && left <= 65535 ? (int) left : -1;
            left += child.getLeft();
            ViewParent parent = child.getParent(); child = parent instanceof View ? (View) parent : null;
        }
        return -1;
    }
    void register(View host, Backend backend) {
        if (!enabled() || host == null || backend == null) return;
        Backend previous = backends.put(host, backend);
        if (previous != null && previous != backend) previous.restore();
    }
    void unregister(Backend backend) {
        for (Map.Entry<View, Backend> entry : new ArrayList<>(backends.entrySet()))
            if (entry.getValue() == backend) { backend.restore(); backends.remove(entry.getKey()); }
    }
    public Frame frame(View nativeMobileHost) {
        if (!enabled() || nativeMobileHost == null || host(nativeMobileHost) != nativeMobileHost) return null;
        Backend backend = backends.get(nativeMobileHost);
        if (backend == null) return null;
        try { return backend.frame(nativeMobileHost); }
        catch (RuntimeException unsupported) { backend.restore(); return null; }
    }
    /** Store a native placement target and invalidate its measurement once. The outer
     * flow owns synchronous host.measure(EXACTLY hullWidth) and native host.layout(). */
    public boolean apply(Frame frame, int hullWidthPx, int labelLeftPx, int signalLeftPx,
            boolean labelVisible, boolean signalVisible) {
        return apply(frame, hullWidthPx, labelLeftPx, signalLeftPx, labelVisible, signalVisible, false);
    }
    /** A deliberate negative outer margin may overlap fragments. The scene adapter
     * opts in only for that signed spacing; hull and native visibility guards remain. */
    public boolean apply(Frame frame, int hullWidthPx, int labelLeftPx, int signalLeftPx,
            boolean labelVisible, boolean signalVisible, boolean allowOverlap) {
        lastFailure = "bridge_validation";
        if (!enabled() || frame == null || !frame.valid || frame.host() == null
                || backends.get(frame.host()) != frame.backend || hullWidthPx < 0 || hullWidthPx > 65535
                || labelVisible && (!fits(labelLeftPx, frame.labelWidthPx, hullWidthPx) || !frame.labelPresent)
                || signalVisible && !fits(signalLeftPx, frame.signalWidthPx, hullWidthPx)
                || !allowOverlap && labelVisible && signalVisible && frame.labelWidthPx > 0 && frame.signalWidthPx > 0
                    && labelLeftPx < (long) signalLeftPx + frame.signalWidthPx
                    && signalLeftPx < (long) labelLeftPx + frame.labelWidthPx) return false;
        try {
            boolean applied = frame.backend.target(new Target(hullWidthPx, labelLeftPx, signalLeftPx, labelVisible, signalVisible));
            lastFailure = applied ? "none" : "native_target_rejected"; return applied;
        }
        catch (RuntimeException unsupported) { frame.backend.restore(); lastFailure = "native_target_exception"; return false; }
    }
    private static boolean fits(int left, int width, int total) { return left >= 0 && width >= 0 && (long) left + width <= total; }
    public void restore(View host) { Backend backend = backends.get(host); if (backend != null) backend.restore(); }
    public void releaseRuntime() { configure(false); released = true; backends.clear(); nativeContainer = null; displayable = null; slot = null; }
    public String summary() { return "enabled=" + enabled() + ", measuredHosts=" + backends.size(); }
}
