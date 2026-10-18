// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.content.res.ColorStateList;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ImageView;
import android.widget.TextView;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.WeakHashMap;

/** Adapts proven in-process Live Alert content. External Surface contents and
 * Compose draw commands have no editable TextView/ImageView here. Only native
 * neutral text and pre-existing monochrome icon tints participate; selectors,
 * album art, coloured actions and progress accents retain their native colours.
 * Roots are scanned on binding/content changes, never on stable refreshes. */
final class FluidCloudForeground {
    private static final int MAX_CONTENT_VIEWS = 192;
    private static final String BODY = "com.android.systemui.statusbar.notification.row.NotificationBackgroundView";
    private static final String EXT = "com.oplus.systemui.statusbar.notification.row.NotificationBackgroundViewExtImp";
    private static final String ROW = "com.android.systemui.statusbar.notification.row.ExpandableNotificationRow";
    private static final String COLOR_EX = "com.oplus.systemui.notification.row.material.NotificationMaterialColorEx";
    private static final String COLOR_MODE = "com.oplus.systemui.notification.row.material.MaterialColorMode";
    private static final String PALETTE = "com.oplus.systemui.notification.row.material.NotificationColorPalette";
    private static final Map<ClassLoader, NativePalette> palettes = Collections.synchronizedMap(new WeakHashMap<>());
    private final Map<View, Root> roots = Collections.synchronizedMap(new WeakHashMap<>());
    private final Map<View, Entry> candidates = Collections.synchronizedMap(new WeakHashMap<>());
    private final ThreadLocal<Boolean> writing = new ThreadLocal<>();
    private boolean enabled;

    /** Reads the OEM's actual material-colour mode and palette. NotificationBlurMode
     * describes a backdrop recipe, not its text colour. The native wrapper uses
     * NotificationColorPalette.Companion.from(mode).getPrimaryColor(); MATERIAL
     * in that method reads NotificationMaterialColorConfig.commonTextMaterialColor.
     * No copied resource, luminance heuristic or enum-name colour guess is used. */
    static Integer nativeColor(View body) {
        if (body == null) return null;
        NativePalette access = palette(body.getClass().getClassLoader());
        if (access == null || !access.bodyType.isInstance(body)) return null;
        try {
            Object extension = access.extension.get(body);
            if (!access.extensionType.isInstance(extension)) return null;
            Object owner = access.materialOwner.invoke(extension);
            if (!access.materialType.isInstance(owner)) return null;
            Object mode = null;
            for (ViewParent parent = body.getParent(); parent instanceof View; parent = parent.getParent()) {
                if (access.rowType.isInstance(parent)) { mode = access.rowMode.invoke(owner, parent); break; }
            }
            if (mode == null || mode == access.uninitiated) {
                Object flow = access.currentMode.invoke(owner);
                mode = flow == null ? null : access.flowValue.invoke(flow);
            }
            if (mode == access.uninitiated || !access.modeType.isInstance(mode)) return null;
            Object palette = access.from.invoke(access.companion, mode);
            if (palette == null) return null;
            int colour = (Integer) access.primary.invoke(palette);
            return (colour >>> 24) == 0 ? null : colour;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) { return null; }
    }

    private static NativePalette palette(ClassLoader loader) {
        synchronized (palettes) {
            if (palettes.containsKey(loader)) return palettes.get(loader);
            NativePalette value = null;
            try { value = new NativePalette(loader); }
            catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) { }
            palettes.put(loader, value);
            return value;
        }
    }

    void policy(boolean next) {
        if (enabled == next) return;
        enabled = next;
        if (!next) {
            for (Root root : rootSnapshot()) {
                restore(root, false);
                root.foreground = null;
            }
        }
    }

    /** Binding and reinflation events invalidate the tree; material/media refreshes
     * simply register the existing root and colour. A dirty scan preserves the
     * latest native base of views that remain in this root. */
    void invalidate(View root) {
        Root binding = root == null ? null : roots.get(root);
        if (binding != null) binding.dirty = true;
    }

    void register(View root, Integer foreground) {
        if (!enabled || root == null) return;
        if (foreground == null || (foreground >>> 24) == 0) { detach(root); return; }
        Root binding = roots.get(root);
        if (binding == null) { binding = new Root(root); roots.put(root, binding); }
        if (!binding.dirty && foreground.equals(binding.foreground)) return;
        if (binding.dirty) scan(root, binding);
        binding.foreground = foreground;
        for (WeakReference<View> reference : binding.views) {
            View view = reference.get();
            Entry entry = view == null ? null : candidates.get(view);
            if (entry != null && entry.root.get() == root) {
                entry.foreground = foreground;
                adapt(view, entry);
            }
        }
    }

    private void scan(View root, Root binding) {
        ArrayList<View> pending = new ArrayList<>();
        ArrayList<WeakReference<View>> next = new ArrayList<>();
        IdentityHashMap<View, Boolean> present = new IdentityHashMap<>();
        pending.add(root);
        for (int cursor = 0; cursor < pending.size() && cursor < MAX_CONTENT_VIEWS; cursor++) {
            View view = pending.get(cursor);
            if (view instanceof TextView || view instanceof ImageView) {
                Entry entry = candidates.get(view);
                if (entry == null) { entry = new Entry(root, read(view)); candidates.put(view, entry); }
                else entry.root = new WeakReference<>(root);
                present.put(view, Boolean.TRUE);
                next.add(new WeakReference<>(view));
            }
            if (view instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) view;
                int count = group.getChildCount();
                for (int i = 0; i < count && pending.size() < MAX_CONTENT_VIEWS; i++) pending.add(group.getChildAt(i));
            }
        }
        for (WeakReference<View> reference : binding.views) {
            View old = reference.get();
            Entry entry = old == null ? null : candidates.get(old);
            if (entry != null && entry.root.get() == root && !present.containsKey(old)) {
                restore(old, entry);
                candidates.remove(old);
            }
        }
        binding.views = next;
        binding.dirty = false;
    }

    private void adapt(View view, Entry entry) {
        ColorStateList current = read(view);
        if (entry.applied == null || current != entry.applied) {
            // Rebind/native updates that bypass a setter hook are authoritative.
            entry.original = current;
            entry.applied = null;
        }
        ColorStateList original = entry.original;
        if (!eligible(original)) return;
        int desired = desired(original.getDefaultColor(), entry.foreground);
        if (current != null && current.getDefaultColor() == desired) return;
        ColorStateList applied = replacement(entry, desired);
        if (write(view, applied)) entry.applied = read(view);
    }

    /** Called before the native setter. Transform the incoming value once, so an
     * OEM white update cannot be drawn before a later black correction. Unknown
     * views require only a weak-map lookup, with no view-tree/resource traversal.
     * colorApplied must run in the hook's finally block after the setter. */
    int textColor(View view, int incoming) {
        if (!(view instanceof TextView)) return incoming;
        Entry entry = candidate(view);
        if (entry == null || entry.awaiting) return incoming;
        return filter(entry, ColorStateList.valueOf(incoming)).getDefaultColor();
    }

    ColorStateList color(View view, ColorStateList incoming) {
        // TextView rejects null before changing its native field. Do not drop
        // restoration ownership for that failed call; ImageView accepts null.
        if (view instanceof TextView && incoming == null) return null;
        Entry entry = candidate(view);
        if (entry == null || entry.awaiting) return incoming;
        return filter(entry, incoming);
    }

    private Entry candidate(View view) {
        if (!enabled || view == null || Boolean.TRUE.equals(writing.get())) return null;
        Entry entry = candidates.get(view);
        View root = entry == null ? null : entry.root.get();
        if (root == null || !roots.containsKey(root)) return null;
        for (View parent = view; parent != null; ) {
            if (parent == root) return entry;
            ViewParent next = parent.getParent();
            parent = next instanceof View ? (View) next : null;
        }
        // A recycled view can leave this tree before the next content callback.
        // Its next native setter belongs to the new owner and must pass through.
        candidates.remove(view);
        return null;
    }

    private ColorStateList filter(Entry entry, ColorStateList incoming) {
        entry.original = incoming;
        entry.applied = null;
        entry.awaiting = false;
        if (!eligible(incoming)) return incoming;
        int desired = desired(incoming.getDefaultColor(), entry.foreground);
        if (desired == incoming.getDefaultColor()) return incoming;
        entry.awaiting = true;
        entry.pendingColour = desired;
        return replacement(entry, desired);
    }

    void colorApplied(View view) {
        Entry entry = candidate(view);
        if (entry == null || !entry.awaiting) return;
        ColorStateList actual = read(view);
        entry.applied = actual != null && !actual.isStateful() && actual.getDefaultColor() == entry.pendingColour ? actual : null;
        entry.awaiting = false;
    }

    private static ColorStateList replacement(Entry entry, int colour) {
        if (entry.replacement == null || entry.replacement.getDefaultColor() != colour)
            entry.replacement = ColorStateList.valueOf(colour);
        return entry.replacement;
    }
    private static boolean eligible(ColorStateList colour) {
        return colour != null && !colour.isStateful() && neutral(colour.getDefaultColor());
    }
    private static int desired(int nativeColour, int foreground) {
        return nativeColour & 0xff000000 | foreground & 0x00ffffff;
    }
    static boolean neutral(int color) {
        int r = color >> 16 & 255, g = color >> 8 & 255, b = color & 255;
        return (color >>> 24) != 0 && Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g, b)) <= 8;
    }
    private static ColorStateList read(View view) {
        try { return view instanceof TextView ? ((TextView) view).getTextColors() : ((ImageView) view).getImageTintList(); }
        catch (RuntimeException | LinkageError unavailable) { return null; }
    }
    private boolean write(View view, ColorStateList colour) {
        Boolean before = writing.get();
        writing.set(Boolean.TRUE);
        try {
            if (view instanceof TextView) ((TextView) view).setTextColor(colour);
            else ((ImageView) view).setImageTintList(colour);
            return read(view) == colour;
        } catch (RuntimeException | LinkageError unavailable) { return false; }
        finally { if (before == null) writing.remove(); else writing.set(before); }
    }

    void detach(View root) {
        if (root == null) return;
        Root binding = roots.remove(root);
        if (binding != null) restore(binding, true);
    }
    void release() {
        enabled = false;
        for (Root root : rootSnapshot()) restore(root, true);
        roots.clear();
        candidates.clear();
        writing.remove();
    }
    private ArrayList<Root> rootSnapshot() {
        synchronized (roots) { return new ArrayList<>(roots.values()); }
    }
    private void restore(Root binding, boolean forget) {
        View root = binding.root.get();
        for (WeakReference<View> reference : binding.views) {
            View view = reference.get();
            Entry entry = view == null ? null : candidates.get(view);
            if (entry == null || entry.root.get() != root) continue;
            restore(view, entry);
            if (forget) candidates.remove(view);
        }
    }
    private void restore(View view, Entry entry) {
        if (entry.applied != null && read(view) == entry.applied) write(view, entry.original);
        entry.applied = null;
        entry.awaiting = false;
    }
    private static final class Root {
        final WeakReference<View> root;
        ArrayList<WeakReference<View>> views = new ArrayList<>();
        Integer foreground;
        boolean dirty = true;
        Root(View root) { this.root = new WeakReference<>(root); }
    }
    private static final class Entry {
        WeakReference<View> root;
        ColorStateList original, applied, replacement;
        int foreground, pendingColour;
        boolean awaiting;
        Entry(View root, ColorStateList original) { this.root = new WeakReference<>(root); this.original = original; }
    }
    private static final class NativePalette {
        final Class<?> bodyType, extensionType, rowType, materialType, modeType;
        final Field extension;
        final Method materialOwner, rowMode, currentMode, flowValue, from, primary;
        final Object companion, uninitiated;
        NativePalette(ClassLoader loader) throws ReflectiveOperationException {
            bodyType = Class.forName(BODY, false, loader);
            extensionType = Class.forName(EXT, false, loader);
            rowType = Class.forName(ROW, false, loader);
            materialType = Class.forName(COLOR_EX, false, loader);
            modeType = Class.forName(COLOR_MODE, false, loader);
            Class<?> paletteType = Class.forName(PALETTE, false, loader);
            extension = bodyType.getDeclaredField("mExt");
            materialOwner = extensionType.getDeclaredMethod("getNotificationMaterialColorEx");
            rowMode = materialType.getDeclaredMethod("getColorModeForRow", rowType);
            currentMode = materialType.getDeclaredMethod("getCurrentMaterialColor");
            flowValue = currentMode.getReturnType().getMethod("getValue");
            uninitiated = modeType.getField("UNINITIATED").get(null);
            companion = paletteType.getField("Companion").get(null);
            from = companion.getClass().getDeclaredMethod("from", modeType);
            primary = paletteType.getDeclaredMethod("getPrimaryColor");
            if (materialOwner.getReturnType() != materialType || rowMode.getReturnType() != modeType
                    || from.getReturnType() != paletteType || primary.getReturnType() != Integer.TYPE
                    || !currentMode.getReturnType().getName().equals("kotlinx.coroutines.flow.StateFlow"))
                throw new NoSuchMethodException("Native notification foreground palette changed");
            extension.setAccessible(true);
            for (Method method : new Method[]{materialOwner, rowMode, currentMode, flowValue, from, primary}) method.setAccessible(true);
        }
    }
}
