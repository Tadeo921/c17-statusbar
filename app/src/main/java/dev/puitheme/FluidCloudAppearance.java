// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.LinearGradient;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Shader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/** Appearance of the audited expanded-card background, registered at native initialization.
 * The native material, shape, layer insets and animation stay outside this policy.
 * Unknown containers, stateful colours, artwork and shared copies stay untouched.
 */
final class FluidCloudAppearance {
    private static final int MAX_DEPTH = 4, MAX_LAYERS = 8, MAX_NODES = 24;
    private static volatile boolean positionsResolved;
    private static Field gradientState, positions;
    private static Method setColors;
    static final String NATIVE_GRADIENT = "com.oplus.systemui.plugins.seedling.util.d";
    private static final Map<Class<?>, NativeAccess> nativeAccess = Collections.synchronizedMap(new WeakHashMap<>());
    // A value never holds its drawable key. Only backgrounds registered by the
    // expanded-card initializer can acquire a range; other instances stay native.
    private static final Map<Drawable, NativeRange> nativeRanges = Collections.synchronizedMap(new WeakHashMap<>());
    private FluidCloudAppearance() { }

    static float range(float value) {
        return Float.isFinite(value) ? Math.max(1f, Math.min(100f, value))
                : FluidCloudStyle.DEFAULT_BACKGROUND_GRADIENT_RANGE;
    }

    /**
     * The five numbers behind the expanded card's own gradient. The OEM recipe runs from its start colour at the top to its end
     * colour at the bottom; the card keeps both colours and only reshapes how one becomes the other. With every value at its default
     * the shape is the native gradient itself, and the native shader is restored untouched.
     * <ul><li>start / end: where the change begins and ends, as a share of the card height; flat before and after.
     * <li>mid: how far through that span the colour is half way (50 is a straight ramp; less turns early, more late).
     * <li>strength: how far the end colour is from the start colour (100 native, 0 no gradient, above 100 more contrast).
     * <li>opacity: fades the whole gradient layer; the base colour and blur beneath are unaffected.</ul>
     */
    static final class GradientShape {
        static final GradientShape NATIVE = new GradientShape(0f, 1f, .5f, 1f, 1f);
        static final int STEPS = 12;
        final float start, end, mid, strength, opacity;
        private GradientShape(float start, float end, float mid, float strength, float opacity) {
            this.start = start; this.end = end; this.mid = mid; this.strength = strength; this.opacity = opacity;
        }
        /** Percent inputs: start 0-98, end 1-100, mid 1-99, strength 0-200, opacity 0-100; end is kept at least 1 point after start. */
        static GradientShape of(float startPercent, float endPercent, float midPercent, float strengthPercent, float opacityPercent) {
            float s = clamp(startPercent, 0f, 98f, 0f), e = clamp(endPercent, 1f, 100f, 100f);
            if (e < s + 1f) e = Math.min(100f, s + 1f);
            return new GradientShape(s / 100f, e / 100f, clamp(midPercent, 1f, 99f, 50f) / 100f,
                    clamp(strengthPercent, 0f, 200f, 100f) / 100f, clamp(opacityPercent, 0f, 100f, 100f) / 100f);
        }
        /** What the single range setting used to mean: only the end position moves. */
        static GradientShape ofEnd(float endPercent) { return of(0f, endPercent, 50f, 100f, 100f); }
        private static float clamp(float value, float min, float max, float fallback) {
            return Float.isFinite(value) ? Math.max(min, Math.min(max, value)) : fallback;
        }
        boolean isNative() { return equals(NATIVE); }
        /** Share (0-1) of the colour change reached at position t (0-1) inside the start-end span. Continuous in every parameter. */
        float weight(float t) {
            float x = Math.max(0f, Math.min(1f, t));
            float gamma = (float) (Math.log(.5) / Math.log(mid));
            float u = (float) Math.pow(x, gamma);
            // A flat stretch beside the ramp would show as a kink; ease the ends in proportion to how much flat there is.
            float edges = Math.min(1f, Math.max(start, 1f - end) * 10f);
            float smooth = u * u * (3f - 2f * u);
            return u + (smooth - u) * edges;
        }
        @Override public boolean equals(Object other) {
            if (!(other instanceof GradientShape)) return false;
            GradientShape shape = (GradientShape) other;
            return start == shape.start && end == shape.end && mid == shape.mid && strength == shape.strength && opacity == shape.opacity;
        }
        @Override public int hashCode() { return java.util.Objects.hash(start, end, mid, strength, opacity); }
        @Override public String toString() { return "start=" + start * 100f + ",end=" + end * 100f + ",mid=" + mid * 100f + ",strength=" + strength * 100f + ",opacity=" + opacity * 100f; }
    }

    static Drawable gradient(Drawable source, Resources resources, float percent) {
        return gradient(source, resources, GradientShape.ofEnd(range(percent)));
    }

    /** Preserve the native gradient colours and direction and reshape only how one becomes the other. */
    static Drawable gradient(Drawable source, Resources resources, GradientShape shape) {
        if (source == null) return source;
        if (nativeGradient(source, shape, null)) return source;
        if (shape.isNative() || !resolvePositions()) return source;
        try {
            if (!supported(source, 0, new int[]{0})) return source;
            Drawable copy = privateCopy(source, resources);
            if (copy == null || !gradientLeaves(source, copy, shape)) return source;
            return copy;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) { return source; }
    }

    /** An explicit base colour must also reach the visible OEM gradient above
     * the blur. Its RGB changes; the OEM stop alphas and gradient opacity stay
     * independent of the base material's colour alpha and opacity. */
    static Drawable gradient(Drawable source, Resources resources, GradientShape shape, Integer colour) {
        if (source == null) return source;
        if (nativeGradient(source, shape, null, colour)) return source;
        // Resource fallbacks already receive their selected colour through the
        // existing material copy. Unknown native drawables remain untouched.
        return gradient(source, resources, shape);
    }

    /** The OEM light-effect background is [hardware blur, util.d], not a
     * GradientDrawable. Its shader has native stops [0, 0, 1] and no ConstantState.
     * Keep the owner, blur sibling, Paint and smooth-corner path intact. */
    private static Drawable nativeLeaf(Drawable source) {
        if (source == null) return null;
        if (NATIVE_GRADIENT.equals(source.getClass().getName())) return source;
        if (source.getClass() != LayerDrawable.class) return null;
        LayerDrawable layers = (LayerDrawable) source;
        if (layers.getNumberOfLayers() != 2) return null;
        String blur = layers.getDrawable(0).getClass().getName();
        if (!"com.android.internal.graphics.drawable.BackgroundBlurDrawable".equals(blur)
                && !"android.view.ViewRootImpl$BackgroundBlurDrawable".equals(blur)) return null;
        Drawable leaf = layers.getDrawable(1);
        return NATIVE_GRADIENT.equals(leaf.getClass().getName()) ? leaf : null;
    }

    static Method nativeBoundsMethod(ClassLoader loader) throws ReflectiveOperationException {
        Class<?> type = loader.loadClass(NATIVE_GRADIENT);
        NativeAccess access = nativeContract(type);
        return access.bounds;
    }

    static Method nativeDrawMethod(ClassLoader loader) throws ReflectiveOperationException {
        return nativeContract(loader.loadClass(NATIVE_GRADIENT)).draw;
    }

    private static NativeAccess nativeContract(Class<?> type) throws ReflectiveOperationException {
        NativeAccess access = nativeAccess.get(type);
        if (access == null) {
            access = new NativeAccess(type); nativeAccess.put(type, access);
        }
        return access;
    }

    /** With {@code solid}, the OEM gradient becomes that one colour (blur sibling and shape kept). */
    private static boolean nativeGradient(Drawable root, GradientShape shape, Integer solid) {
        return nativeGradient(root, shape, solid, null);
    }
    private static boolean nativeGradient(Drawable root, GradientShape shape, Integer solid, Integer colour) {
        Drawable leaf = nativeLeaf(root);
        if (leaf == null) return false;
        Integer tint = colour == null ? null : Integer.valueOf(colour & 0x00ffffff);
        try {
            NativeRange previous = nativeRanges.get(leaf);
            if (shape.isNative() && solid == null && tint == null) {
                if (previous != null) {
                    previous.restore(leaf);
                    nativeRanges.remove(leaf); leaf.invalidateSelf();
                }
                return true;
            }
            if (previous != null && previous.shape.equals(shape) && java.util.Objects.equals(previous.solid, solid)
                    && java.util.Objects.equals(previous.tint, tint)) return true;
            NativeAccess access = previous == null ? nativeContract(leaf.getClass()) : previous.access;
            LinearGradient original = previous == null ? (LinearGradient) access.shader.get(leaf) : previous.original(leaf);
            NativeRange next = new NativeRange(access, shape, original, solid, tint);
            next.apply(leaf);
            nativeRanges.put(leaf, next); leaf.invalidateSelf();
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) { return false; }
    }

    static boolean nativeGradientOwned(Drawable root) {
        Drawable leaf = nativeLeaf(root);
        return leaf != null && nativeRanges.containsKey(leaf);
    }

    /** Called after this exact OEM class rebuilds g for new local bounds. */
    static void nativeBoundsChanged(Drawable leaf) {
        NativeRange range = nativeRanges.get(leaf);
        if (range == null) return;
        try {
            range.synchronize(leaf);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) {
            nativeRanges.remove(leaf);
        }
    }

    /** The final boundary before the OEM reads g into its Paint. A compiled
     * bounds caller or another native reset may bypass the after-bounds hook.
     * Only registered instances participate; steady draws keep the same shader
     * and allocate neither a replacement gradient nor its colour/stop arrays. */
    static void nativeDrawStarting(Drawable leaf) {
        NativeRange range = nativeRanges.get(leaf);
        if (range == null) return;
        try {
            LinearGradient before = range.appliedShader;
            range.synchronize(leaf);
            if (before != range.appliedShader && range.appliedShader != null) range.drawRebuilt = true;
        }
        catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) {
            nativeRanges.remove(leaf);
        }
    }

    /** One structured observation per registered range, after the native draw
     * returns. Empty frames do not claim a visible draw; no notification text
     * or artwork enters the diagnostic report. */
    static void nativeDrawCompleted(Drawable leaf) {
        if (!ModuleDiagnostics.enabled()) return;
        NativeRange range = nativeRanges.get(leaf);
        if (range == null || range.drawReported || range.appliedShader == null || leaf.getBounds().isEmpty()) return;
        range.drawReported = true;
        ModuleDiagnostics.fact("fluid", "fluid", "悬浮卡片渐变绘制", "scene", "float", "backend", "oem_card",
                "gradient_range", range.shape.end * 100f, "gradient_start", range.shape.start * 100f, "gradient_mid", range.shape.mid * 100f,
                "gradient_strength", range.shape.strength * 100f, "gradient_opacity", range.shape.opacity * 100f, "gradient_owned", true, "gradient_drawn", true,
                "gradient_rebuilt", range.drawRebuilt, "view_width", leaf.getBounds().width(),
                "view_height", leaf.getBounds().height());
    }

    static void releaseNativeGradients() {
        synchronized (nativeRanges) {
            for (Map.Entry<Drawable, NativeRange> entry : nativeRanges.entrySet()) {
                Drawable leaf = entry.getKey(); NativeRange range = entry.getValue();
                if (leaf == null) continue;
                try { range.restore(leaf); leaf.invalidateSelf(); }
                catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) { }
            }
            nativeRanges.clear();
        }
        nativeAccess.clear();
    }

    private static final class NativeRange {
        final NativeAccess access; final GradientShape shape; final Integer solid, tint;
        LinearGradient nativeShader, appliedShader;
        boolean drawReported, drawRebuilt;
        NativeRange(NativeAccess access, GradientShape shape, LinearGradient nativeShader, Integer solid, Integer tint) {
            this.access = access; this.shape = shape; this.nativeShader = nativeShader; this.solid = solid; this.tint = tint;
        }
        LinearGradient original(Drawable leaf) throws ReflectiveOperationException {
            LinearGradient current = (LinearGradient) access.shader.get(leaf);
            // A reset which has not drawn yet still owns the newest OEM recipe.
            return current == appliedShader ? nativeShader : current;
        }
        void restore(Drawable leaf) throws ReflectiveOperationException {
            nativeShader = original(leaf);
            access.shader.set(leaf, nativeShader); appliedShader = null;
        }
        void synchronize(Drawable leaf) throws ReflectiveOperationException {
            LinearGradient current = (LinearGradient) access.shader.get(leaf);
            if (current == appliedShader) return;
            nativeShader = current; appliedShader = null;
            apply(leaf);
        }
        void apply(Drawable leaf) throws ReflectiveOperationException {
            Rect bounds = leaf.getBounds();
            if (bounds.isEmpty() || nativeShader == null) return;
            if (solid != null) {
                appliedShader = new LinearGradient(bounds.left, bounds.top, bounds.left, bounds.bottom,
                        new int[]{solid, solid}, new float[]{0f, 1f}, Shader.TileMode.CLAMP);
                access.shader.set(leaf, appliedShader);
                return;
            }
            int a = access.top.getInt(leaf), b = access.middle.getInt(leaf), c = access.bottom.getInt(leaf);
            if (tint != null) {
                a = a & 0xff000000 | tint; b = b & 0xff000000 | tint; c = c & 0xff000000 | tint;
                if (shape.isNative()) {
                    // Colour-only edits retain the exact OEM [0, 0, 1] ramp,
                    // including its fully opaque first visible stop.
                    appliedShader = new LinearGradient(bounds.left, bounds.top, bounds.left, bounds.bottom,
                            new int[]{a, b, c}, new float[]{0f, 0f, 1f}, Shader.TileMode.CLAMP);
                    access.shader.set(leaf, appliedShader); return;
                }
            }
            // Native colours and their repeated first stop stay. Between start and end the colour moves from the start colour to
            // an end colour set by strength, along a curve whose half-way point is the mid line; opacity fades the whole layer.
            int from = fade(b, shape.opacity), to = fade(blend(b, c, shape.strength), shape.opacity);
            int steps = GradientShape.STEPS, extra = (shape.start > 0f ? 1 : 0) + (shape.end < 1f ? 1 : 0);
            int[] colors = new int[steps + 2 + extra]; float[] stops = new float[steps + 2 + extra];
            int at = 0;
            colors[at] = a; stops[at++] = 0f; colors[at] = from; stops[at++] = 0f;
            if (shape.start > 0f) { colors[at] = from; stops[at++] = shape.start; }
            for (int i = 1; i <= steps; i++) {
                float t = i / (float) steps;
                colors[at] = blend(from, to, shape.weight(t)); stops[at++] = shape.start + (shape.end - shape.start) * t;
            }
            if (shape.end < 1f) { colors[at] = to; stops[at++] = 1f; }
            appliedShader = new LinearGradient(bounds.left, bounds.top, bounds.left, bounds.bottom,
                    colors, stops, Shader.TileMode.CLAMP);
            access.shader.set(leaf, appliedShader);
        }
    }

    /** Per-channel ARGB blend that may overshoot (t above 1) and clamps each channel. */
    static int blend(int from, int to, float t) {
        int result = 0;
        for (int shift = 0; shift <= 24; shift += 8) {
            int x = (from >>> shift) & 0xff, y = (to >>> shift) & 0xff;
            result |= (Math.max(0, Math.min(255, Math.round(x + (y - x) * t))) & 0xff) << shift;
        }
        return result;
    }

    /** Scales the alpha channel only. */
    static int fade(int color, float opacity) {
        if (opacity >= 1f) return color;
        int alpha = Math.max(0, Math.min(255, Math.round((color >>> 24) * Math.max(0f, opacity))));
        return (alpha << 24) | (color & 0x00ffffff);
    }

    /** Per-channel ARGB blend. */
    static int mix(int from, int to, float t) {
        int result = 0;
        for (int shift = 0; shift <= 24; shift += 8) {
            int x = (from >>> shift) & 0xff, y = (to >>> shift) & 0xff;
            result |= (Math.round(x + (y - x) * t) & 0xff) << shift;
        }
        return result;
    }

    private static final class NativeAccess {
        final Field top, middle, bottom, shader; final Method bounds, draw;
        NativeAccess(Class<?> type) throws ReflectiveOperationException {
            if (!NATIVE_GRADIENT.equals(type.getName()) || type.getSuperclass() != Drawable.class)
                throw new NoSuchFieldException("Native card gradient changed");
            top = field(type, "a", Integer.TYPE); middle = field(type, "b", Integer.TYPE);
            bottom = field(type, "c", Integer.TYPE); shader = field(type, "g", LinearGradient.class);
            field(type, "e", Paint.class); field(type, "f", Path.class);
            if (!"kotlin.Triple".equals(type.getDeclaredField("d").getType().getName()))
                throw new NoSuchFieldException("Native card corners changed");
            bounds = type.getDeclaredMethod("onBoundsChange", Rect.class);
            if (bounds.getReturnType() != Void.TYPE) throw new NoSuchMethodException("Native card bounds changed");
            bounds.setAccessible(true);
            draw = type.getDeclaredMethod("draw", Canvas.class);
            if (draw.getReturnType() != Void.TYPE) throw new NoSuchMethodException("Native card draw changed");
            draw.setAccessible(true);
        }
        private static Field field(Class<?> type, String name, Class<?> expected) throws ReflectiveOperationException {
            Field field = type.getDeclaredField(name);
            if (field.getType() != expected) throw new NoSuchFieldException("Native card " + name + " changed");
            field.setAccessible(true); return field;
        }
    }

    /** The base shape has one selected fill; any second native fill must not
     * stack the selected opacity twice. Native outlines and stroke colours remain. */
    static Drawable solid(Drawable source, Resources resources, int color) {
        if (source == null) return source;
        // Device code161: the real card is [blur, OEM gradient]; recolour that gradient in place.
        if (nativeGradient(source, GradientShape.NATIVE, color)) return source;
        try {
            if (!supported(source, 0, new int[]{0})) return source;
            Drawable copy = privateCopy(source, resources);
            if (copy == null) return source;
            boolean[] filled = new boolean[]{false};
            solidLeaves(source, copy, color, filled);
            return filled[0] ? copy : source;
        } catch (RuntimeException | LinkageError unavailable) { return source; }
    }

    private static boolean resolvePositions() {
        if (!positionsResolved) synchronized (FluidCloudAppearance.class) {
            if (!positionsResolved) {
                try {
                    Field state = GradientDrawable.class.getDeclaredField("mGradientState");
                    Field offsets = state.getType().getDeclaredField("mPositions");
                    Method setter = GradientDrawable.class.getMethod("setColors", int[].class, float[].class);
                    state.setAccessible(true); offsets.setAccessible(true);
                    gradientState = state; positions = offsets; setColors = setter;
                } catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) {
                    gradientState = null; positions = null; setColors = null;
                }
                positionsResolved = true;
            }
        }
        return setColors != null;
    }

    private static boolean supported(Drawable source, int depth, int[] nodes) {
        if (source == null || depth > MAX_DEPTH || ++nodes[0] > MAX_NODES) return false;
        if (source.getClass() == GradientDrawable.class) {
            GradientDrawable shape = (GradientDrawable) source;
            ColorStateList color = shape.getColor();
            int[] colors = shape.getColors();
            return !shape.isStateful() && (color == null || !color.isStateful())
                    && (colors == null || colors.length <= MAX_NODES);
        }
        if (source.getClass() != LayerDrawable.class) return false;
        LayerDrawable layers = (LayerDrawable) source;
        if (layers.getNumberOfLayers() < 1 || layers.getNumberOfLayers() > MAX_LAYERS) return false;
        for (int i = 0; i < layers.getNumberOfLayers(); i++)
            if (!supported(layers.getDrawable(i), depth + 1, nodes)) return false;
        return true;
    }

    private static Drawable privateCopy(Drawable source, Resources resources) {
        Drawable.ConstantState state = source.getConstantState();
        if (state == null) return null;
        Drawable copy = state.newDrawable(resources);
        if (copy == null || copy == source) return null;
        copy = copy.mutate();
        if (!independent(source, copy, 0, new int[]{0})) return null;
        runtime(source, copy);
        return copy;
    }

    private static boolean independent(Drawable source, Drawable copy, int depth, int[] nodes) {
        if (source == null || copy == null || source == copy || source.getClass() != copy.getClass()
                || source.getConstantState() == copy.getConstantState()
                || depth > MAX_DEPTH || ++nodes[0] > MAX_NODES) return false;
        if (source.getClass() == GradientDrawable.class) return true;
        if (source.getClass() != LayerDrawable.class) return false;
        LayerDrawable a = (LayerDrawable) source, b = (LayerDrawable) copy;
        if (a.getNumberOfLayers() != b.getNumberOfLayers()) return false;
        for (int i = 0; i < a.getNumberOfLayers(); i++)
            if (!independent(a.getDrawable(i), b.getDrawable(i), depth + 1, nodes)) return false;
        return true;
    }

    private static void runtime(Drawable source, Drawable copy) {
        copy.setBounds(source.getBounds()); copy.setLevel(source.getLevel());
        copy.setState(source.getState().clone()); copy.setAlpha(source.getAlpha());
        if (source.getClass() == LayerDrawable.class) {
            LayerDrawable a = (LayerDrawable) source, b = (LayerDrawable) copy;
            for (int i = 0; i < a.getNumberOfLayers(); i++) runtime(a.getDrawable(i), b.getDrawable(i));
        }
    }

    private static boolean gradientLeaves(Drawable source, Drawable copy, GradientShape shape)
            throws ReflectiveOperationException {
        if (source.getClass() == GradientDrawable.class) {
            GradientDrawable original = (GradientDrawable) source;
            int[] colors = original.getColors();
            if (colors == null || colors.length < 2 || original.getGradientType() != GradientDrawable.LINEAR_GRADIENT) return false;
            float[] nativePositions = (float[]) positions.get(gradientState.get(original));
            if (nativePositions != null && nativePositions.length != colors.length) throw new IllegalArgumentException("Native stops changed");
            // A resource gradient has its own stop list: move the stops into the start-end span, pull every colour towards the first
            // by strength and fade the layer. The mid line needs the OEM shader recipe above and does not apply here.
            float[] moved = new float[colors.length + 1]; int[] extended = new int[colors.length + 1];
            float previous = -1f;
            for (int i = 0; i < colors.length; i++) {
                float value = nativePositions == null ? i / (float) (colors.length - 1) : nativePositions[i];
                if (!Float.isFinite(value) || value < previous || value < 0f || value > 1f)
                    throw new IllegalArgumentException("Invalid native stops");
                previous = value; moved[i] = shape.start + (shape.end - shape.start) * value;
                extended[i] = fade(blend(colors[0], colors[i], shape.strength), shape.opacity);
            }
            moved[colors.length] = 1f; extended[colors.length] = extended[colors.length - 1];
            setColors.invoke(copy, extended, moved);
            return true;
        }
        LayerDrawable a = (LayerDrawable) source, b = (LayerDrawable) copy; boolean changed = false;
        for (int i = 0; i < a.getNumberOfLayers(); i++)
            changed |= gradientLeaves(a.getDrawable(i), b.getDrawable(i), shape);
        return changed;
    }

    private static void solidLeaves(Drawable source, Drawable copy, int color, boolean[] filled) {
        if (source.getClass() == GradientDrawable.class) {
            GradientDrawable original = (GradientDrawable) source, target = (GradientDrawable) copy;
            int[] colors = original.getColors(); ColorStateList nativeColor = original.getColor();
            boolean ownsFill = nativeColor != null || colors != null && colors.length != 0;
            if (!ownsFill) return;
            boolean visibleFill = nativeColor != null && (nativeColor.getDefaultColor() >>> 24) != 0;
            if (colors != null) for (int value : colors) visibleFill |= (value >>> 24) != 0;
            target.setTintList(null); target.setColorFilter((android.graphics.ColorFilter) null);
            target.setColor(filled[0] || !visibleFill ? color & 0x00ffffff : color);
            filled[0] |= visibleFill;
            target.setAlpha(original.getAlpha());
            return;
        }
        LayerDrawable a = (LayerDrawable) source, b = (LayerDrawable) copy;
        for (int i = 0; i < a.getNumberOfLayers(); i++) solidLeaves(a.getDrawable(i), b.getDrawable(i), color, filled);
    }
}
