package com.oplus.systemui.plugins.seedling.capsule.ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.GradientDrawable;

/** The structure of the audited ColorOS 17 capsule: a black pill as the background, a transparent outline as the
 * foreground (cached in the single GradientDrawable field) and one (float)void expansion step that rewrites the
 * outline's stroke: shown while collapsed (progress at most 0.13), none while expanded. */
public final class CapsuleView extends android.widget.FrameLayout {
    public GradientDrawable f;
    public float d;
    public int steps;

    public CapsuleView(Context context) {
        super(context);
        GradientDrawable pill = new GradientDrawable();
        pill.setColor(0xff000000);
        pill.setCornerRadius(40f);
        GradientDrawable outline = new GradientDrawable();
        outline.setCornerRadius(40f);
        setBackground(pill);
        setForeground(outline);
        f = outline;
        b(0f);
    }

    public final void b(float progress) {
        steps++;
        d = progress > .13f ? 1f : 0f;
        f.setStroke(d > 0f ? 0 : 3, 0x40959595);
    }

    public final void onFinishInflate() { }
    public final void onAttachedToWindow() { }
    public final void setHidden(boolean hidden) { }
    public final void onDraw(Canvas canvas) { }
}
