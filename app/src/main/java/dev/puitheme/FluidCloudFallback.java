// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie

package dev.puitheme;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;

/** Private copies of the OEM's static expanded-card fallback material.
 *
 * BackgroundUtils also runs with a null ViewRootManager and inflates a drawable
 * resource directly. That path has no getColor/blur-parameter call to intercept.
 * Only a bounded tree of gradient leaves is supported here; artwork, shaders,
 * blur drawables and unrecognised containers retain their native drawing path.
 */
public final class FluidCloudFallback {
    private static final int MAX_DEPTH = 4, MAX_LAYERS = 8, MAX_NODES = 24;
    private FluidCloudFallback() { }

    /** Never mutates source, its ConstantState, or a shared child drawable. */
    public static Drawable derive(Drawable source, Resources resources, Integer accentRgb,
            boolean removeHighlights, boolean removeBackground) {
        if (source == null || accentRgb == null && !removeHighlights && !removeBackground) return source;
        try {
            if (!supported(source, 0, new int[]{0})) return source;
            Drawable.ConstantState state = source.getConstantState();
            if (state == null) return source;
            Drawable copy = state.newDrawable(resources);
            if (copy == null || copy == source) return source;
            copy = copy.mutate();
            if (!privateCopy(source, copy, 0, new int[]{0})) return source;
            apply(source, copy, accentRgb, removeHighlights, removeBackground);
            return copy;
        } catch (RuntimeException | LinkageError unavailable) {
            return source;
        }
    }

    /** A configured material has its own alpha. Apply it once to the native leaf
     * fill; strokes, bounds, interaction state and drawable animation alpha stay OEM. */
    public static Drawable material(Drawable source, Resources resources, Integer color,
            boolean removeHighlights, boolean removeBackground) {
        Drawable copy = derive(source, resources, color, removeHighlights, removeBackground);
        if (copy == source || color == null || removeBackground) return copy;
        try { materialLeaves(source, copy, color); return copy; }
        catch (RuntimeException | LinkageError unsupported) { return source; }
    }
    private static void materialLeaves(Drawable source, Drawable copy, int color) {
        if (source.getClass() == GradientDrawable.class && copy.getClass() == GradientDrawable.class) {
            GradientDrawable original = (GradientDrawable) source, target = (GradientDrawable) copy;
            int[] colors = original.getColors();
            if (colors != null) {
                colors = colors.clone();
                for (int i = 0; i < colors.length; i++) colors[i] = FluidCloudStyle.materialFill(colors[i], color);
                target.setColors(colors);
            } else if (original.getColor() != null)
                target.setColor(FluidCloudStyle.materialFill(original.getColor().getDefaultColor(), color));
        } else if (source instanceof LayerDrawable && copy instanceof LayerDrawable) {
            LayerDrawable original = (LayerDrawable) source, target = (LayerDrawable) copy;
            for (int i = 0; i < original.getNumberOfLayers(); i++) materialLeaves(original.getDrawable(i), target.getDrawable(i), color);
        }
    }

    /** Native-source opacity preserves each stop's RGB and all animation alpha. */
    public static Drawable opacity(Drawable source, Resources resources, float opacity) {
        if (opacity == 1f || !Float.isFinite(opacity) || opacity < 0f || opacity > 1f) return source;
        Drawable copy = derive(source, resources, null, false, true);
        if (copy == source) return source;
        try { opacityLeaves(source, copy, opacity); return copy; }
        catch (RuntimeException | LinkageError unsupported) { return source; }
    }
    private static void opacityLeaves(Drawable source, Drawable copy, float opacity) {
        if (source.getClass() == GradientDrawable.class && copy.getClass() == GradientDrawable.class) {
            GradientDrawable original = (GradientDrawable) source, target = (GradientDrawable) copy;
            int[] colors = original.getColors();
            if (colors != null) {
                colors = colors.clone();
                for (int i = 0; i < colors.length; i++) colors[i] = FluidCloudStyle.materialOpacity(colors[i], opacity);
                target.setColors(colors);
            } else if (original.getColor() != null)
                target.setColor(FluidCloudStyle.materialOpacity(original.getColor().getDefaultColor(), opacity));
        } else if (source instanceof LayerDrawable && copy instanceof LayerDrawable) {
            LayerDrawable original = (LayerDrawable) source, target = (LayerDrawable) copy;
            for (int i = 0; i < original.getNumberOfLayers(); i++) opacityLeaves(original.getDrawable(i), target.getDrawable(i), opacity);
        }
    }

    /** Native static template base: private rounded shape, album primary at the
     * top, transparent bottom. Backdrop blur and text stay in their OEM layers. */
    public static Drawable gradient(Drawable source,Resources resources,int accent) {
        Drawable copy=derive(source,resources,accent,false,false);
        if(copy==source)return source;
        try{gradientLeaves(copy,accent);return copy;}catch(RuntimeException|LinkageError unsupported){return source;}
    }
    private static void gradientLeaves(Drawable value,int accent) {
        if(value.getClass()==GradientDrawable.class) {
            GradientDrawable gradient=(GradientDrawable)value;
            // The native immersive GradientDrawable carries a solid setTint()
            // from setEntry; a private copy must remove that tint/filter before
            // its own spatial stops can be seen. The shared source stays native.
            gradient.setTintList(null);gradient.setColorFilter((android.graphics.ColorFilter)null);
            gradient.setOrientation(GradientDrawable.Orientation.TOP_BOTTOM);
            gradient.setColors(new int[]{accent,accent&0x00ffffff});
        } else if(value.getClass()==LayerDrawable.class) {
            LayerDrawable layers=(LayerDrawable)value;
            for(int i=0;i<layers.getNumberOfLayers();i++)gradientLeaves(layers.getDrawable(i),accent);
        }
    }

    private static boolean supported(Drawable value, int depth, int[] nodes) {
        if (value == null || depth > MAX_DEPTH || ++nodes[0] > MAX_NODES) return false;
        if (value.getClass() == GradientDrawable.class) {
            ColorStateList fill = ((GradientDrawable) value).getColor();
            // A stateful selector has OEM interaction colours. Keep it intact.
            return fill == null || !fill.isStateful();
        }
        if (value.getClass() != LayerDrawable.class) return false;
        LayerDrawable layers = (LayerDrawable) value;
        int count = layers.getNumberOfLayers();
        if (count < 1 || count > MAX_LAYERS) return false;
        for (int i = 0; i < count; i++) if (!supported(layers.getDrawable(i), depth + 1, nodes)) return false;
        return true;
    }

    private static boolean privateCopy(Drawable source, Drawable copy, int depth, int[] nodes) {
        if (source == null || copy == null || source == copy || depth > MAX_DEPTH || ++nodes[0] > MAX_NODES
                || source.getClass() != copy.getClass()
                || source.getConstantState() == copy.getConstantState()) return false;
        if (source instanceof GradientDrawable) return copy instanceof GradientDrawable;
        if (!(source instanceof LayerDrawable) || !(copy instanceof LayerDrawable)) return false;
        LayerDrawable original = (LayerDrawable) source, cloned = (LayerDrawable) copy;
        int count = original.getNumberOfLayers();
        if (count < 1 || count > MAX_LAYERS || count != cloned.getNumberOfLayers()) return false;
        for (int i = 0; i < count; i++)
            if (!privateCopy(original.getDrawable(i), cloned.getDrawable(i), depth + 1, nodes)) return false;
        return true;
    }

    private static void apply(Drawable source, Drawable copy, Integer accentRgb,
            boolean removeHighlights, boolean removeBackground) {
        // Containers propagate runtime properties to children. Restore their
        // own properties first, then each leaf's actual native state and alpha.
        copy.setBounds(source.getBounds());
        copy.setLevel(source.getLevel());
        copy.setState(source.getState().clone());
        copy.setAlpha(source.getAlpha());
        if (source instanceof GradientDrawable) {
            GradientDrawable original = (GradientDrawable) source, gradient = (GradientDrawable) copy;
            // A copied native tint still wins over the new fill. Clear it only
            // on this private copy; shared resources retain the OEM tint.
            if(accentRgb!=null||removeBackground) {
                gradient.setTintList(null);gradient.setColorFilter((android.graphics.ColorFilter)null);
            }
            int[] colors = original.getColors();
            if (colors != null && colors.length > 0) {
                int[] replaced = colors.clone();
                for (int i = 0; i < replaced.length; i++) replaced[i] = fill(replaced[i], accentRgb, removeBackground);
                gradient.setColors(replaced);
            } else {
                ColorStateList color = original.getColor();
                if (color != null) gradient.setColor(fill(color.getDefaultColor(), accentRgb, removeBackground));
            }
            if (removeHighlights) gradient.setStroke(0, 0);
            gradient.setAlpha(source.getAlpha());
        } else {
            LayerDrawable original = (LayerDrawable) source, layers = (LayerDrawable) copy;
            for (int i = 0; i < original.getNumberOfLayers(); i++)
                apply(original.getDrawable(i), layers.getDrawable(i), accentRgb, removeHighlights, removeBackground);
        }
    }

    private static int fill(int nativeColor, Integer accentRgb, boolean removeBackground) {
        if (removeBackground) return nativeColor & 0x00ffffff;
        return accentRgb == null ? nativeColor : FluidCloudAccent.preserveAlpha(nativeColor, accentRgb);
    }
}
