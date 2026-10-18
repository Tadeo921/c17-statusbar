package com.oplus.systemui.plugins.shared.template.component.view;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.widget.Chronometer;
import android.widget.TextView;

/** Relevant SmartChronometer contract audited in the ColorOS 17 plugin DEX. Its opaque black foreground was
 * hidden by the native black capsule and becomes a block when the capsule background is glass or coloured. */
public final class r extends Chronometer {
    public int n = 20;
    public boolean o = true;
    public int widthForCheck = 78, heightForCheck = 14;
    public int nativeForegroundBuildsForCheck, nativeForegroundWritesForCheck, nativeFadeCallsForCheck;

    public r(Context context) { super(context); }
    @Override public int getWidth() { return widthForCheck; }
    @Override public int getHeight() { return heightForCheck; }

    /** The native g() flow, including its LayerDrawable identity gate: it does not rebuild a stable foreground. */
    public final void g() {
        nativeFadeCallsForCheck++;
        if (!o || getWidth() == 0 || getHeight() == 0 || n == 0) return;
        Rect ink = new Rect();
        String value = getText().toString();
        getPaint().getTextBounds(value, 0, value.length(), ink);
        boolean overflow = ink.width() > getWidth();
        if ((getForeground() instanceof LayerDrawable) == overflow) return;
        LayerDrawable foreground = null;
        if (overflow) {
            GradientDrawable gradient = new GradientDrawable();
            gradient.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
            gradient.setColors(new int[]{0, 0xff000000});
            gradient.setGradientType(GradientDrawable.LINEAR_GRADIENT);
            gradient.setSize(n, getHeight());
            foreground = new LayerDrawable(new Drawable[]{gradient});
            foreground.setLayerInset(0, getWidth() - n, 0, 0, 0);
            nativeForegroundBuildsForCheck++;
        }
        setForeground(foreground);
        invalidate();
    }

    @Override public void setForeground(Drawable value) {
        nativeForegroundWritesForCheck++;
        super.setForeground(value);
    }

    public final void setCustomFadingEdgeEnabled(boolean enabled) { o = enabled; }

    /** The native method calls View's ordinary fading edge if custom fading is disabled; only the custom branch
     * is relevant here, and that branch records the length and calls g(). */
    public void setFadingEdgeLength(int length) {
        if (o) { n = length; g(); }
        invalidate();
    }

    @Override public final void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        g();
    }
    @Override public final void setText(CharSequence value, TextView.BufferType type) { super.setText(value, type); }
    @Override public final void onAttachedToWindow() { super.onAttachedToWindow(); }
    @Override public final void onDetachedFromWindow() { super.onDetachedFromWindow(); }
}
