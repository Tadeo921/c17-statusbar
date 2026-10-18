// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.view.View;
import android.view.ViewGroup;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/** The album cover at the left of the camera-area music capsule. The plugin draws it in its CustomLottieView, clipped by the view's
 * own outline: a shape (NONE, CIRCLE, ROUND_RECT, read by name) and a corner radius, set through its public setShape/setCornerRadius.
 * A custom radius switches the cover to ROUND_RECT with that radius; the native shape and radius are remembered and set back when the
 * option goes off, the capsule stops being a music capsule, or the module is released. Only a capsule whose right side shows the
 * spectrum (the plugin's voice print, or our bars) is a music capsule; timers, calls and the rest keep their icons as they are.
 * Its optional visual size is multiplied onto the plugin's current animation scale, independently of the corner setting. */
final class CapsuleCover {
    static final String LEFT = "left_section", RIGHT = "right_section", IMAGE = "img_primary";

    /** The view's members, resolved once per class loader; null fields mean the contract changed and nothing is touched. */
    static final class Access {
        final Method setShape, getShape, setRadius, getRadius;
        final Object roundRect;
        Access(Class<?> type) throws ReflectiveOperationException {
            getShape = type.getMethod("getCurrentShape");
            Class<?> shape = getShape.getReturnType();
            if (!shape.isEnum()) throw new NoSuchMethodException("CustomLottieView shape is not an enum");
            setShape = type.getMethod("setShape", shape);
            setRadius = type.getMethod("setCornerRadius", float.class);
            getRadius = type.getMethod("getCornerRadius");
            Object round = null;
            for (Object constant : shape.getEnumConstants()) if ("ROUND_RECT".equals(((Enum<?>) constant).name())) round = constant;
            if (round == null) throw new NoSuchFieldException("CustomLottieView ROUND_RECT");
            roundRect = round;
        }
    }

    private static final class Native {
        final Object shape;
        final float radius;
        Native(Object shape, float radius) { this.shape = shape; this.radius = radius; }
    }

    private final Map<ClassLoader, Access> contracts = Collections.synchronizedMap(new WeakHashMap<>());
    private final Map<View, Native> covers = Collections.synchronizedMap(new WeakHashMap<>());
    /** The radius each owned cover must keep (px). */
    private final Map<View, Float> wanted = Collections.synchronizedMap(new WeakHashMap<>());
    /** The plugin's current animation scale and the last values we wrote, separately for each axis. */
    private static final class Scale {
        float nativeX, nativeY, appliedX, appliedY, factor;
        Scale(View view) { nativeX = appliedX = view.getScaleX(); nativeY = appliedY = view.getScaleY(); }
    }
    private final Map<View, Scale> scales = Collections.synchronizedMap(new WeakHashMap<>());
    private final ThreadLocal<Boolean> applying = new ThreadLocal<>();
    /** For a cover the plugin is setting up: the radius it should have now (px), or a negative number for the plugin's own shape. */
    private volatile java.util.function.ToDoubleFunction<View> wantedFor;
    void setWantedFor(java.util.function.ToDoubleFunction<View> provider) { wantedFor = provider; }
    private final Map<View, WeakReference<View>> found = Collections.synchronizedMap(new WeakHashMap<>());
    private boolean loggedFailure;

    /** Brings the cover of one capsule to the wanted look ({@code radiusPx} < 0: native). {@code weight} is how far the capsule has
     * expanded (0 collapsed): collapsed, the cover sits on the punch-hole camera and keeps the plugin's circle; while it expands the
     * corners go from that circle to the chosen radius. Cheap when nothing changes. */
    void apply(View capsule, float radiusPx, float weight) {
        if (capsule == null) return;
        View cover = cover(capsule);
        if (cover == null) return;
        Access access = access(cover);
        if (access == null) return;
        boolean want = radiusPx >= 0f && weight > 0f && musicCapsuleCached(capsule);
        if (want) radiusPx = shown(cover, radiusPx, weight);
        Boolean previous = applying.get();
        applying.set(Boolean.TRUE);
        try {
            Native original = covers.get(cover);
            if (!want) {
                wanted.remove(cover);
                if (original != null) { covers.remove(cover); restore(access, cover, original); }
                return;
            }
            if (original == null) {
                original = new Native(access.getShape.invoke(cover), ((Number) access.getRadius.invoke(cover)).floatValue());
                covers.put(cover, original);
            }
            wanted.put(cover, radiusPx);
            if (access.getShape.invoke(cover) != access.roundRect) access.setShape.invoke(cover, access.roundRect);
            if (((Number) access.getRadius.invoke(cover)).floatValue() != radiusPx) access.setRadius.invoke(cover, radiusPx);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            if (!loggedFailure) { loggedFailure = true; ModuleDiagnostics.info("fluid", "Capsule cover shape unavailable; the cover stays native"); }
        } finally {
            if (previous == null) applying.remove(); else applying.set(previous);
        }
    }

    /** Whether the cover of this capsule already shows what is wanted ({@code radiusPx} < 0: the plugin's own shape). Cheap. */
    boolean settled(View capsule, float radiusPx) {
        WeakReference<View> known = found.get(capsule);
        View cover = known == null ? null : known.get();
        if (cover == null) return radiusPx < 0f;
        Float kept = wanted.get(cover);
        return radiusPx < 0f ? kept == null : kept != null && Math.abs(kept - radiusPx) < .5f;
    }

    /** Changes only the album image's visual size. The outer capsule and the plugin's layout and pivot stay native. */
    void applyScale(View capsule, float percent, float weight) {
        if (capsule == null) return;
        View image = cover(capsule);
        if (image == null) return;
        float factor = scaleFactor(percent, weight);
        if (factor == 1f || !musicCapsuleCached(capsule)) { restoreScale(image); return; }
        Scale kept = scales.get(image);
        if (kept == null) { kept = new Scale(image); scales.put(image, kept); }
        // The plugin can animate either axis between our draws. Its new value becomes the baseline, never our multiplied value.
        float x = image.getScaleX(), y = image.getScaleY();
        if (!sameScale(x, kept.appliedX)) kept.nativeX = x;
        if (!sameScale(y, kept.appliedY)) kept.nativeY = y;
        kept.factor = factor;
        kept.appliedX = kept.nativeX * factor; kept.appliedY = kept.nativeY * factor;
        if (!sameScale(x, kept.appliedX)) image.setScaleX(kept.appliedX);
        if (!sameScale(y, kept.appliedY)) image.setScaleY(kept.appliedY);
    }

    /** A cached, constant-time check before drawing; native animation writes make it false so their new scale can be adopted. */
    boolean scaleSettled(View capsule, float percent, float weight) {
        WeakReference<View> known = found.get(capsule);
        View image = known == null ? null : known.get();
        if (image == null) return known != null || scaleFactor(percent, weight) == 1f;
        Scale kept = scales.get(image);
        float factor = scaleFactor(percent, weight);
        if (factor != 1f && !musicCapsuleCached(capsule)) factor = 1f;
        return factor == 1f ? kept == null : kept != null && sameScale(kept.factor, factor)
                && sameScale(image.getScaleX(), kept.appliedX) && sameScale(image.getScaleY(), kept.appliedY);
    }

    static float scaleFactor(float percent, float weight) {
        float value = Float.isFinite(percent) ? Math.max(FluidCloudCapsule.MIN_COVER_SCALE, Math.min(FluidCloudCapsule.MAX_COVER_SCALE, percent))
                : FluidCloudCapsule.DEFAULT_COVER_SCALE;
        float expanded = Float.isFinite(weight) ? Math.max(0f, Math.min(1f, weight)) : 0f;
        return 1f + (value / 100f - 1f) * expanded;
    }

    private static boolean sameScale(float a, float b) { return Math.abs(a - b) < .0001f; }

    private void restoreScale(View image) {
        Scale kept = scales.remove(image);
        if (kept == null) return;
        // If the plugin has already written the next native animation frame, keep that value on release too.
        if (sameScale(image.getScaleX(), kept.appliedX)) image.setScaleX(kept.nativeX);
        if (sameScale(image.getScaleY(), kept.appliedY)) image.setScaleY(kept.nativeY);
    }

    /** Missing children may appear after inflation/binding. Misses stay cached between these existing lifecycle events. */
    void bindingChanged(View capsule) {
        WeakReference<View> left = found.get(capsule), right = rightImages.get(capsule);
        if (left != null && left.get() == null) found.remove(capsule);
        if (right != null && right.get() == null) rightImages.remove(capsule);
    }

    /** Result-changing hook of the cover's setShape: the plugin re-applies its circle when it binds a new track. While the cover is ours
     * the rounded rectangle stays and the plugin's choice is only remembered (given back when the option goes off), so a track change
     * never shows the circle for a frame. A cover the plugin is setting up for the first time is adopted right here when it should be ours. */
    Object shapeRequested(View cover, Object shape) {
        if (cover == null || Boolean.TRUE.equals(applying.get())) return shape;
        Access access = access(cover);
        if (access == null) return shape;
        Float kept = wanted.get(cover);
        if (kept == null) kept = adopt(cover, access, shape);
        if (kept == null) return shape;
        Native original = covers.get(cover);
        if (original != null) covers.put(cover, new Native(shape, original.radius));
        return access.roundRect;
    }

    /** Result-changing hook of the cover's setCornerRadius, the same way. */
    float radiusRequested(View cover, float radius) {
        if (cover == null || Boolean.TRUE.equals(applying.get())) return radius;
        Float kept = wanted.get(cover);
        if (kept == null) return radius;
        Native original = covers.get(cover);
        if (original != null) covers.put(cover, new Native(original.shape, radius));
        return kept;
    }

    private Float adopt(View cover, Access access, Object nativeShape) {
        java.util.function.ToDoubleFunction<View> provider = wantedFor;
        if (provider == null) return null;
        float radius = (float) provider.applyAsDouble(cover);
        if (!(radius >= 0f)) return null;
        try { covers.put(cover, new Native(nativeShape, ((Number) access.getRadius.invoke(cover)).floatValue())); }
        catch (ReflectiveOperationException | RuntimeException failure) { return null; }
        wanted.put(cover, radius);
        // The radius goes on right after the shape; the plugin's own setCornerRadius call is answered by radiusRequested.
        Boolean previous = applying.get();
        applying.set(Boolean.TRUE);
        try { access.setRadius.invoke(cover, radius); }
        catch (ReflectiveOperationException | RuntimeException ignored) { }
        finally { if (previous == null) applying.remove(); else applying.set(previous); }
        return radius;
    }

    /** The capsule that holds this cover (left_section's image inside a capsule view), or null. */
    static View capsuleOf(View cover, String capsuleClass) {
        View left = null;
        View current = cover.getParent() instanceof View ? (View) cover.getParent() : null;
        for (int depth = 0; current != null && depth < 4 && left == null; depth++) {
            if (current.getId() > 0) try {
                if (LEFT.equals(current.getResources().getResourceEntryName(current.getId()))) left = current;
            } catch (RuntimeException notAResource) { /* keep looking */ }
            current = current.getParent() instanceof View ? (View) current.getParent() : null;
        }
        if (left == null) return null;
        for (View up = left; up != null; up = up.getParent() instanceof View ? (View) up.getParent() : null)
            if (up.getClass().getName().equals(capsuleClass)) return up;
        return null;
    }

    /** The radius at this point of the expansion: from the circle the cover is collapsed to the chosen radius. */
    static float shown(View cover, float radiusPx, float weight) {
        float circle = Math.min(cover.getWidth(), cover.getHeight()) / 2f;
        float w = Math.max(0f, Math.min(1f, weight));
        return circle > 0f ? circle + (radiusPx - circle) * w : radiusPx;
    }

    /** Every cover back to the plugin's own shape, radius and current animation scale. */
    void restoreAll() {
        final ArrayList<Map.Entry<View, Native>> all;
        synchronized (covers) { all = new ArrayList<>(covers.entrySet()); covers.clear(); }
        wanted.clear();
        Boolean previous = applying.get();
        applying.set(Boolean.TRUE);
        try { restoreEach(all); } finally { if (previous == null) applying.remove(); else applying.set(previous); }
        final ArrayList<View> scaled;
        synchronized (scales) { scaled = new ArrayList<>(scales.keySet()); }
        for (View image : scaled) if (image != null) restoreScale(image);
        found.clear(); rightImages.clear();
    }

    private void restoreEach(ArrayList<Map.Entry<View, Native>> all) {
        for (Map.Entry<View, Native> entry : all) {
            View cover = entry.getKey();
            Access access = cover == null ? null : access(cover);
            if (access != null) try { restore(access, cover, entry.getValue()); } catch (ReflectiveOperationException | RuntimeException ignored) { }
        }
    }

    private static void restore(Access access, View cover, Native original) throws ReflectiveOperationException {
        access.setShape.invoke(cover, original.shape);
        access.setRadius.invoke(cover, original.radius);
    }

    private Access access(View cover) {
        ClassLoader loader = cover.getClass().getClassLoader();
        Access cached = loader == null ? null : contracts.get(loader);
        if (cached != null) return cached;
        try {
            Class<?> type = Class.forName(CapsuleSpectrum.VIEW, false, loader);
            if (!type.isInstance(cover)) return null;
            Access resolved = new Access(type);
            contracts.put(loader, resolved);
            return resolved;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) {
            if (!loggedFailure) { loggedFailure = true; ModuleDiagnostics.info("fluid", "Capsule cover contract unavailable; the cover stays native"); }
            return null;
        }
    }

    /** The cover: the first img_primary inside left_section. Remembered per capsule while it is still attached there. */
    View cover(View capsule) {
        WeakReference<View> known = found.get(capsule);
        View cover = known == null ? null : known.get();
        if (known != null && cover == null) return null;
        if (cover != null && within(cover, capsule)) return cover;
        if (cover != null) restoreScale(cover);
        View left = named(capsule, LEFT, 0);
        cover = left == null ? null : named(left, IMAGE, 0);
        found.put(capsule, new WeakReference<>(cover));
        return cover;
    }

    /** A music capsule shows the spectrum on its right: the plugin's voice print or our bars. */
    static boolean musicCapsule(View capsule) {
        View right = named(capsule, RIGHT, 0);
        View image = right == null ? null : named(right, IMAGE, 0);
        return image != null && CapsuleSpectrum.showsSpectrum(image);
    }

    private final Map<View, WeakReference<View>> rightImages = Collections.synchronizedMap(new WeakHashMap<>());
    /** The same, with the right-hand image remembered per capsule (this runs before each draw). */
    boolean musicCapsuleCached(View capsule) {
        WeakReference<View> known = rightImages.get(capsule);
        View image = known == null ? null : known.get();
        if (known != null && image == null) return false;
        if (image == null || !within(image, capsule)) {
            View right = named(capsule, RIGHT, 0);
            image = right == null ? null : named(right, IMAGE, 0);
            rightImages.put(capsule, new WeakReference<>(image));
        }
        return image != null && CapsuleSpectrum.showsSpectrum(image);
    }

    private static boolean within(View view, View ancestor) {
        for (View current = view; current != null; current = current.getParent() instanceof View ? (View) current.getParent() : null)
            if (current == ancestor) return true;
        return false;
    }

    static View named(View root, String name, int depth) {
        if (root == null || depth > 8) return null;
        if (root.getId() > 0) try {
            if (name.equals(root.getResources().getResourceEntryName(root.getId()))) return root;
        } catch (RuntimeException notAResource) { /* Not one of ours. */ }
        if (!(root instanceof ViewGroup)) return null;
        ViewGroup group = (ViewGroup) root;
        for (int i = 0; i < group.getChildCount(); i++) {
            View match = named(group.getChildAt(i), name, depth + 1);
            if (match != null) return match;
        }
        return null;
    }
}
