package com.oplus.systemui.notification.row;
public class NotificationBackgroundCustomRowHandler {
    public boolean needDrawBgColorByMetaBall(android.content.Context context,com.android.systemui.plugins.custom.CustomRowEntry entry){return true;}
    public int nativeUpdates;
    public int nativeColor=0xff101010;
    public final void updateDrawableMetaColor(android.content.Context context,com.android.systemui.plugins.custom.CustomRowEntry entry,com.oplus.posteffect.drawable.MetaBallBlurDrawable drawable){nativeUpdates++;drawable.setMetaColor(nativeColor);}
}
