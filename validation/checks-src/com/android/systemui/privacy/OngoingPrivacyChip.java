package com.android.systemui.privacy;
public class OngoingPrivacyChip extends android.view.ViewGroup {
    public OngoingPrivacyChip(android.content.Context context) { super(context); }
    public java.util.List<Object> privacyList = new java.util.ArrayList<>();
    public java.util.List<Object> getPrivacyList() { return privacyList; }
    public android.content.res.Resources resourcesForCheck;
    @Override public android.content.res.Resources getResources() { return resourcesForCheck == null ? super.getResources() : resourcesForCheck; }
}
