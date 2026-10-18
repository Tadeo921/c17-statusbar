package com.oplus.systemui.plugins.shared.template.component.view;

import android.content.Context;
import android.widget.FrameLayout;

/** The audited template switches between two SmartChronometers; both can draw during a content transition. */
public final class ContentSwitchView extends FrameLayout {
    public final r b, c;
    public ContentSwitchView(Context context) {
        super(context);
        b = new r(context);
        c = new r(context);
        addView(b);
        addView(c);
    }
    public final r getCurrentView() { return b; }
    public final r getTargetView() { return c; }
}
