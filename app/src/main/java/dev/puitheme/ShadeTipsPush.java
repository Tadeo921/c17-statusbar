// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.lang.ref.WeakReference;

/**
 * Pushes the shade's small bottom text (the lock-screen tips line, a blessing or a caption) down when the clear-all button reaches it.
 *
 * <p>The system keeps that text under the button and never lets them meet. The module lets the button follow the last notification
 * and sit where the author puts it, so the button can come down onto the text; instead of covering it, the button pushes it down, out of
 * the way, the way a list item is pushed by the one that grows above it. The text is moved by the view that holds it (its translation is
 * restored the moment it is not needed), never rewritten, and nothing happens when the text is not there.
 */
final class ShadeTipsPush {
    static final String TIPS_VIEW = "oplus_keyguard_tips_view";
    static final String TIPS_HOST = "keyguard_bottom_tips_host";
    private static final String PACKAGE = "com.android.systemui";

    private int tipsId = -1, hostId = -1;
    private WeakReference<View> tipsRef = new WeakReference<>(null);
    private WeakReference<View> moved = new WeakReference<>(null);
    private WeakReference<View> owner = new WeakReference<>(null);
    /** The moved view's own translation, what it is without us; the amount we add; what we wrote last (to tell a native write from ours). */
    private float nativeY, applied, written = Float.NaN;
    private final int[] where = new int[2];

    /**
     * How far down the text must go so that the button, with {@code gap} between them, does not cover it. 0 when they do not meet, or are
     * apart sideways (the button's span widened by {@code margin} against the text's). All in the same coordinates; y grows downward.
     */
    static float push(float buttonBottom, float buttonLeft, float buttonRight, float textTop, float textLeft, float textRight, float gap, float margin) {
        if (!(buttonBottom == buttonBottom) || !(textTop == textTop)) return 0f;
        if (buttonRight + margin <= textLeft || buttonLeft - margin >= textRight) return 0f;
        return Math.max(0f, buttonBottom + Math.max(0f, gap) - textTop);
    }

    /**
     * Called for every frame the module owns the button's place. {@code active}: the button is shown and the module is moving it; when it is
     * not, whatever this pushed goes back (only the button that pushed it may take the push back, so two buttons never fight over it).
     */
    void update(View button, boolean active, float gapPixels, float marginPixels) {
        if (button == null) return;
        if (!active) { if (owner.get() == button) release(); return; }
        try {
            View root = button.getRootView();
            if (root == null || !resolveIds(button)) { release(); return; }
            View tips = tipsRef.get();
            if (tips == null || !tips.isAttachedToWindow() || tips.getRootView() != root) {
                tips = tipsId == 0 ? null : root.findViewById(tipsId);
                tipsRef = new WeakReference<>(tips);
            }
            if (tips == null || !tips.isAttachedToWindow() || !tips.isShown() || tips.getAlpha() <= .01f || tips.getWidth() <= 0) { release(); return; }
            View host = tips.getParent() instanceof View && hostId != 0 && ((View) tips.getParent()).getId() == hostId ? (View) tips.getParent() : tips;
            View previous = moved.get();
            if (previous != null && previous != host) release();
            if (moved.get() != host) { moved = new WeakReference<>(host); nativeY = host.getTranslationY(); applied = 0f; written = Float.NaN; }
            // Someone else wrote the translation since we did: that value is the view's own now.
            if (written == written && Float.compare(host.getTranslationY(), written) != 0) { nativeY = host.getTranslationY(); applied = 0f; written = Float.NaN; }
            View text = firstText(tips);
            View measured = text != null ? text : tips;
            measured.getLocationOnScreen(where);
            float textTop = where[1] - applied, textLeft = where[0], textRight = where[0] + measured.getWidth() * Math.max(.01f, measured.getScaleX());
            button.getLocationOnScreen(where);
            float height = button.getHeight() * Math.max(.01f, button.getScaleY()), width = button.getWidth() * Math.max(.01f, button.getScaleX());
            float wanted = push(where[1] + height, where[0], where[0] + width, textTop, textLeft, textRight, gapPixels, marginPixels);
            owner = new WeakReference<>(button);
            apply(host, wanted);
        } catch (RuntimeException unavailable) {
            release();
        }
    }

    private boolean resolveIds(View button) {
        if (tipsId >= 0) return tipsId != 0;
        tipsId = button.getResources().getIdentifier(TIPS_VIEW, "id", PACKAGE);
        hostId = button.getResources().getIdentifier(TIPS_HOST, "id", PACKAGE);
        return tipsId != 0;
    }

    private void apply(View host, float wanted) {
        float next = Float.isFinite(wanted) ? Math.max(0f, wanted) : 0f;
        if (applied == 0f && next == 0f) return;
        if (next != 0f && Math.abs(next - applied) < .5f && written == written) return;
        host.setTranslationY(nativeY + next);
        applied = next; written = host.getTranslationY();
        if (next == 0f) written = Float.NaN;
    }

    /** Whatever this moved goes back to where the system had it. */
    void release() {
        View host = moved.get();
        if (host != null && applied != 0f && written == written && Float.compare(host.getTranslationY(), written) == 0) host.setTranslationY(nativeY);
        moved = new WeakReference<>(null); owner = new WeakReference<>(null);
        applied = 0f; written = Float.NaN;
    }

    /** The first shown text inside {@code view}, or null. */
    private static View firstText(View view) {
        if (view instanceof TextView) {
            CharSequence text = ((TextView) view).getText();
            return text != null && text.length() > 0 && view.getVisibility() == View.VISIBLE && view.getWidth() > 0 ? view : null;
        }
        if (!(view instanceof ViewGroup)) return null;
        ViewGroup group = (ViewGroup) view;
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child == null || child.getVisibility() != View.VISIBLE) continue;
            View found = firstText(child);
            if (found != null) return found;
        }
        return null;
    }
}
