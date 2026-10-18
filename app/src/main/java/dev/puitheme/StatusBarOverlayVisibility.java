// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** Passive root layers inherit their original status-icon wrappers, never the changing radio children. */
final class StatusBarOverlayVisibility {
    private static final int MAX_DEPTH = 16;
    private static final Map<ViewGroup, Root> ROOTS = new WeakHashMap<>();
    private static final Map<View, List<WeakReference<Binding>>> WRAPPERS = new WeakHashMap<>();
    private static final Method TRANSITION_ALPHA = transitionAlphaMethod();
    private StatusBarOverlayVisibility() { }

    static Binding bind(ViewGroup container, ViewGroup root, View layer) {
        Binding binding = new Binding(container, root, layer);
        Root observer = ROOTS.get(root);
        if (observer == null) { observer = new Root(root); ROOTS.put(root, observer); }
        observer.bindings.add(new WeakReference<>(binding)); binding.observer = observer;
        observer.observe(); binding.sync();
        return binding;
    }

    /** Reused by the already installed visibility/alpha hooks; ordinary unrelated Views do no work. */
    static void changed(View wrapper) {
        List<WeakReference<Binding>> bindings = WRAPPERS.get(wrapper);
        if (bindings == null) return;
        // sync can rebuild a reparented path, so snapshot only at this native property event.
        for (WeakReference<Binding> reference : new ArrayList<>(bindings)) {
            Binding binding = reference.get(); if (binding != null) binding.sync();
        }
    }

    static final class Binding {
        private final WeakReference<ViewGroup> container, root;
        private final WeakReference<View> layer;
        private final List<WeakReference<View>> path = new ArrayList<>();
        private Root observer;
        private boolean released, syncing, contentVisible = true;
        Binding(ViewGroup container, ViewGroup root, View layer) {
            this.container = new WeakReference<>(container); this.root = new WeakReference<>(root); this.layer = new WeakReference<>(layer);
        }
        void contentVisible(boolean visible) { contentVisible = visible; }
        void sync() {
            if (released || syncing) return;
            View target = layer.get(); ViewGroup owner = container.get(), destination = root.get();
            if (target == null || owner == null || destination == null) { release(); return; }
            syncing = true;
            try {
                if (observer != null) observer.observe();
                if (!validPath(owner, destination)) rebuildPath(owner, destination);
                float alpha = 1f; boolean shown = contentVisible && !path.isEmpty();
                for (int i = 0; i < path.size(); i++) {
                    View wrapper = path.get(i).get();
                    if (wrapper == null || !wrapper.isAttachedToWindow() || wrapper.getVisibility() != View.VISIBLE
                            || wrapper.getWindowVisibility() != View.VISIBLE) { shown = false; break; }
                    // ViewGroupOverlay already inherits the root's drawing alpha; do not square it.
                    if (wrapper != destination) alpha *= opacity(wrapper.getAlpha()) * transitionAlpha(wrapper);
                }
                if (target.getAlpha() != alpha) target.setAlpha(alpha);
                int visibility = !contentVisible ? View.GONE : shown ? View.VISIBLE : View.INVISIBLE;
                if (target.getVisibility() != visibility) target.setVisibility(visibility);
            } finally { syncing = false; }
        }
        private boolean validPath(View owner, View destination) {
            if (path.isEmpty() || path.get(0).get() != owner) return false;
            for (int i = 0; i < path.size(); i++) {
                View current = path.get(i).get(); if (current == null) return false;
                if (current == destination) return i == path.size() - 1;
                if (i + 1 == path.size() || current.getParent() != path.get(i + 1).get()) return false;
            }
            return false;
        }
        private void rebuildPath(View owner, View destination) {
            removePath(); View current = owner;
            for (int depth = 0; current != null && depth < MAX_DEPTH; depth++) {
                path.add(new WeakReference<>(current));
                if (current == destination) {
                    for (WeakReference<View> reference : path) {
                        View wrapper = reference.get();
                        List<WeakReference<Binding>> bindings = WRAPPERS.get(wrapper);
                        if (bindings == null) { bindings = new ArrayList<>(); WRAPPERS.put(wrapper, bindings); }
                        bindings.add(new WeakReference<>(this));
                    }
                    return;
                }
                current = current.getParent() instanceof View ? (View) current.getParent() : null;
            }
            path.clear(); // An unknown/reparented path cannot draw in another root.
        }
        private void removePath() {
            for (WeakReference<View> reference : path) {
                View wrapper = reference.get(); List<WeakReference<Binding>> bindings = WRAPPERS.get(wrapper);
                if (bindings == null) continue;
                for (int i = bindings.size() - 1; i >= 0; i--) {
                    Binding known = bindings.get(i).get(); if (known == null || known == this) bindings.remove(i);
                }
                if (bindings.isEmpty()) WRAPPERS.remove(wrapper);
            }
            path.clear();
        }
        void release() {
            if (released) return; released = true; removePath();
            Root known = observer; observer = null;
            if (known != null) known.remove(this);
            container.clear(); root.clear(); layer.clear();
        }
    }

    /** One traversal observer per exact native root; it never schedules another frame. */
    private static final class Root implements ViewTreeObserver.OnPreDrawListener {
        final WeakReference<ViewGroup> root;
        final List<WeakReference<Binding>> bindings = new ArrayList<>();
        WeakReference<ViewTreeObserver> observer = new WeakReference<>(null);
        Root(ViewGroup root) { this.root = new WeakReference<>(root); }
        void observe() {
            ViewGroup owner = root.get(); if (owner == null) return;
            ViewTreeObserver current = owner.getViewTreeObserver(), previous = observer.get();
            if (current == previous && current.isAlive()) return;
            if (previous != null && previous.isAlive()) previous.removeOnPreDrawListener(this);
            if (current.isAlive()) { current.addOnPreDrawListener(this); observer = new WeakReference<>(current); }
        }
        @Override public boolean onPreDraw() {
            for (int i = bindings.size() - 1; i >= 0; i--) {
                Binding binding = bindings.get(i).get();
                if (binding == null) bindings.remove(i); else binding.sync();
            }
            if (bindings.isEmpty()) detach();
            return true;
        }
        void remove(Binding released) {
            for (int i = bindings.size() - 1; i >= 0; i--) {
                Binding binding = bindings.get(i).get(); if (binding == null || binding == released) bindings.remove(i);
            }
            if (bindings.isEmpty()) detach();
        }
        private void detach() {
            ViewTreeObserver previous = observer.get(); if (previous != null && previous.isAlive()) previous.removeOnPreDrawListener(this);
            ViewGroup owner = root.get(); if (owner != null && ROOTS.get(owner) == this) ROOTS.remove(owner);
            observer.clear();
        }
    }
    private static float opacity(float value) { return Float.isFinite(value) ? Math.max(0f, Math.min(1f, value)) : 0f; }
    private static Method transitionAlphaMethod() {
        try { Method method = View.class.getDeclaredMethod("getTransitionAlpha"); method.setAccessible(true); return method; }
        catch (ReflectiveOperationException | RuntimeException unavailable) { return null; }
    }
    private static float transitionAlpha(View wrapper) {
        if (TRANSITION_ALPHA != null) try {
            Object value = TRANSITION_ALPHA.invoke(wrapper); if (value instanceof Float) return opacity((Float) value);
        } catch (ReflectiveOperationException | RuntimeException unavailable) { /* Ordinary alpha remains authoritative. */ }
        return 1f;
    }
}
