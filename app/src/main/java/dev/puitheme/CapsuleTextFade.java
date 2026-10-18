// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Chronometer;
import android.widget.TextView;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Predicate;

/** The plugin's clipped capsule text fades to black, assuming its native black pill. On a custom pill the same
 * gradient removes text ink instead. Only that small text view gets an isolated layer; the parent's glass stays outside it.
 * Keep the native LayerDrawable and its insets: SmartChronometer uses that type as its overflow flag. */
public final class CapsuleTextFade {
    static final String CONTENT = "com.oplus.systemui.plugins.shared.template.component.view.ContentSwitchView";
    private final Predicate<View> wanted;
    private final Map<View, Binding> bindings = new WeakHashMap<>();
    private final ArrayList<WeakReference<View>> owners = new ArrayList<>();
    private Class<?> textType;
    private boolean released;

    public CapsuleTextFade(Predicate<View> wanted) { this.wanted = wanted; }

    /** Discover the obfuscated text class through the two typed children, then hook only its own declared methods. */
    public static final class Hooks {
        public final Class<?> type;
        public final Method[] updates;
        public final Method detached;

        Hooks(ClassLoader loader) throws ReflectiveOperationException {
            Class<?> content = Class.forName(CONTENT, false, loader), found = null;
            if (!android.widget.FrameLayout.class.isAssignableFrom(content)) throw new NoSuchMethodException(CONTENT);
            int children = 0;
            for (Field field : content.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || !Chronometer.class.isAssignableFrom(field.getType())) continue;
                if (found != null && found != field.getType()) throw new NoSuchMethodException("Capsule text children differ");
                found = field.getType(); children++;
            }
            if (children != 2 || found == null || found.getSuperclass() != Chronometer.class || !Modifier.isFinal(found.getModifiers()))
                throw new NoSuchMethodException("Capsule text structure changed");
            type = found;
            updates = new Method[]{declared(found, "onAttachedToWindow"),
                    declared(found, "onSizeChanged", int.class, int.class, int.class, int.class),
                    declared(found, "setFadingEdgeLength", int.class),
                    declared(found, "setCustomFadingEdgeEnabled", boolean.class),
                    declared(found, "setText", CharSequence.class, TextView.BufferType.class)};
            detached = declared(found, "onDetachedFromWindow");
        }

        private static Method declared(Class<?> type, String name, Class<?>... args) throws ReflectiveOperationException {
            Method method = type.getDeclaredMethod(name, args);
            if (Modifier.isStatic(method.getModifiers()) || method.getReturnType() != void.class) throw new NoSuchMethodException(name);
            method.setAccessible(true);
            return method;
        }
    }

    public Hooks resolve(ClassLoader loader) {
        if (released || loader == null) return null;
        try {
            Hooks hooks = new Hooks(loader);
            textType = hooks.type;
            return hooks;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException unavailable) {
            ModuleDiagnostics.info("fluid", "Capsule text fade contract unavailable; native text fade retained");
            return null;
        }
    }

    private static final class Binding {
        final LayerDrawable foreground;
        final GradientDrawable nativeMask;
        final FadeMask mask;
        final boolean borrowedLayer;
        View.OnAttachStateChangeListener listener;
        Binding(LayerDrawable foreground, GradientDrawable nativeMask, FadeMask mask, boolean borrowedLayer) {
            this.foreground = foreground; this.nativeMask = nativeMask; this.mask = mask; this.borrowedLayer = borrowedLayer;
        }
    }

    /** After a native text update, inspect its current foreground. Unrelated templates and backgrounds stay native. */
    public void changed(View text) {
        if (text == null) return;
        Binding old = bindings.get(text);
        if (released || textType == null || text.getClass() != textType || !text.isAttachedToWindow()
                || text.getBackground() != null || !wanted(text)) {
            restore(text, old); return;
        }
        Drawable foreground = text.getForeground();
        if (old != null) {
            if (foreground == old.foreground && old.foreground.getNumberOfLayers() == 1 && old.foreground.getDrawable(0) == old.mask) {
                // A native owner may stop using its layer between updates. Reacquire only the NONE layer we own.
                if (text.getLayerType() == View.LAYER_TYPE_NONE && old.borrowedLayer) text.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                if (text.getLayerType() != View.LAYER_TYPE_NONE) return;
            }
            restore(text, old);
        }
        if (!(foreground instanceof LayerDrawable)) return;
        LayerDrawable layers = (LayerDrawable) foreground;
        if (layers.getNumberOfLayers() != 1 || !(layers.getDrawable(0) instanceof GradientDrawable)) return;
        GradientDrawable gradient = (GradientDrawable) layers.getDrawable(0);
        if (!nativeMask(gradient)) return;
        boolean borrowed = text.getLayerType() == View.LAYER_TYPE_NONE;
        if (borrowed) text.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        if (text.getLayerType() == View.LAYER_TYPE_NONE) return;
        FadeMask mask = new FadeMask(text, gradient);
        Binding next = new Binding(layers, gradient, mask, borrowed);
        next.listener = new View.OnAttachStateChangeListener() {
            @Override public void onViewAttachedToWindow(View view) { changed(view); }
            @Override public void onViewDetachedFromWindow(View view) { detached(view); }
        };
        mask.setBounds(gradient.getBounds());
        try { layers.setDrawable(0, mask); }
        catch (RuntimeException unavailable) {
            if (borrowed && text.getLayerType() == View.LAYER_TYPE_HARDWARE) text.setLayerType(View.LAYER_TYPE_NONE, null);
            return;
        }
        gradient.setCallback(mask);
        bindings.put(text, next);
        owners.add(new WeakReference<>(text));
        text.addOnAttachStateChangeListener(next.listener);
        text.invalidate();
    }

    private boolean wanted(View text) {
        View capsule = capsuleOf(text);
        return capsule != null && wanted != null && wanted.test(capsule);
    }

    static View capsuleOf(View text) {
        ViewParent parent = text.getParent();
        for (int depth = 0; parent instanceof View && depth < 16; depth++, parent = parent.getParent()) {
            View view = (View) parent;
            if (FluidCloudCapsule.VIEW.equals(view.getClass().getName())) return view;
        }
        return null;
    }

    static boolean nativeMask(GradientDrawable gradient) {
        int[] colors = gradient.getColors();
        return gradient.getShape() == GradientDrawable.RECTANGLE && gradient.getGradientType() == GradientDrawable.LINEAR_GRADIENT
                && gradient.getOrientation() == GradientDrawable.Orientation.LEFT_RIGHT && colors != null && colors.length == 2
                && colors[0] == 0 && colors[1] == 0xff000000 && gradient.getCornerRadius() == 0f && gradient.getCornerRadii() == null;
    }

    /** Discovery is bounded and happens when the capsule changes, never as a full hierarchy walk on stable draw frames. */
    public void capsuleChanged(View capsule) {
        if (released || textType == null || capsule == null) return;
        ArrayList<View> queue = new ArrayList<>(); queue.add(capsule);
        for (int index = 0; index < queue.size() && index < 128; index++) {
            View view = queue.get(index);
            if (view.getClass() == textType) changed(view);
            if (view instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) view;
                for (int child = 0; child < group.getChildCount() && queue.size() < 128; child++) queue.add(group.getChildAt(child));
            }
        }
    }

    /** Configuration changes revisit only known text owners. */
    public void refresh() {
        // The loop is safe when changed removes an owner: stay on the same index if that weak entry was removed.
        for (int index = 0; index < owners.size();) {
            WeakReference<View> entry = owners.get(index);
            View view = entry.get();
            if (view == null) { owners.remove(index); continue; }
            changed(view);
            if (index < owners.size() && owners.get(index) == entry) index++;
        }
    }
    public void detached(View text) { restore(text, bindings.get(text)); }
    public void restoreAll() { for (View view : new ArrayList<>(bindings.keySet())) restore(view, bindings.get(view)); }
    public void release() { released = true; restoreAll(); textType = null; }

    private void restore(View view, Binding binding) {
        if (view == null || binding == null) return;
        bindings.remove(view);
        for (int index = owners.size() - 1; index >= 0; index--)
            if (owners.get(index).get() == null || owners.get(index).get() == view) owners.remove(index);
        binding.mask.enabled = false;
        view.removeOnAttachStateChangeListener(binding.listener);
        // Restore our child even when the native code replaced the whole foreground. Do not write over that replacement.
        if (binding.foreground.getNumberOfLayers() == 1 && binding.foreground.getDrawable(0) == binding.mask) {
            binding.nativeMask.setBounds(binding.mask.getBounds());
            binding.foreground.setDrawable(0, binding.nativeMask);
        }
        if (binding.borrowedLayer && view.getLayerType() == View.LAYER_TYPE_HARDWARE) view.setLayerType(View.LAYER_TYPE_NONE, null);
        view.invalidate();
    }

    /** Draw the original gradient as an alpha mask inside the isolated text layer. Its native geometry and alpha stay exact. */
    static final class FadeMask extends Drawable implements Drawable.Callback {
        private final WeakReference<View> owner;
        final GradientDrawable nativeMask;
        private final Paint remove = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF bounds = new RectF();
        private boolean enabled = true;
        FadeMask(View owner, GradientDrawable nativeMask) {
            this.owner = new WeakReference<>(owner); this.nativeMask = nativeMask;
            remove.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
            setState(nativeMask.getState()); setLevel(nativeMask.getLevel());
        }
        @Override public void draw(Canvas canvas) {
            View text = owner.get();
            if (!enabled || text == null || text.getLayerType() == View.LAYER_TYPE_NONE || getBounds().isEmpty()) return;
            bounds.set(getBounds());
            int saved = canvas.saveLayer(bounds, remove);
            try { nativeMask.setBounds(getBounds()); nativeMask.draw(canvas); }
            finally { canvas.restoreToCount(saved); }
        }
        @Override protected void onBoundsChange(Rect bounds) { nativeMask.setBounds(bounds); }
        @Override public void setAlpha(int alpha) { nativeMask.setAlpha(alpha); invalidateSelf(); }
        @Override public int getAlpha() { return nativeMask.getAlpha(); }
        @Override public void setColorFilter(ColorFilter filter) { nativeMask.setColorFilter(filter); invalidateSelf(); }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
        @Override public int getIntrinsicWidth() { return nativeMask.getIntrinsicWidth(); }
        @Override public int getIntrinsicHeight() { return nativeMask.getIntrinsicHeight(); }
        @Override public boolean getPadding(Rect padding) { return nativeMask.getPadding(padding); }
        @Override public boolean isStateful() { return nativeMask.isStateful(); }
        @Override protected boolean onStateChange(int[] state) { return nativeMask.setState(state); }
        @Override protected boolean onLevelChange(int level) { return nativeMask.setLevel(level); }
        @Override public boolean setVisible(boolean visible, boolean restart) {
            return super.setVisible(visible, restart) | nativeMask.setVisible(visible, restart);
        }
        @Override public void invalidateDrawable(Drawable drawable) { invalidateSelf(); }
        @Override public void scheduleDrawable(Drawable drawable, Runnable task, long when) { scheduleSelf(task, when); }
        @Override public void unscheduleDrawable(Drawable drawable, Runnable task) { unscheduleSelf(task); }
    }
}
