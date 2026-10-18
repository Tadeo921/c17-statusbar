package com.oplus.systemui.statusbar.widget;
public class HiddenReceiverFrameLayout extends android.view.ViewGroup {
    public HiddenReceiverFrameLayout(android.content.Context context) { super(context); }
    public android.content.res.Resources resourcesForCheck;
    @Override public android.content.res.Resources getResources() { return resourcesForCheck == null ? super.getResources() : resourcesForCheck; }
}
