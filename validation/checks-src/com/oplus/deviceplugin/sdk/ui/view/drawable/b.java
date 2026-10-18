package com.oplus.deviceplugin.sdk.ui.view.drawable;
import android.content.Context;
import android.view.View;
import com.oplus.systemui.plugins.drawable.MixColorPluginDrawable;
/** Current ROM b.a selects the complete device body material; it never reads tile tags. */
public abstract class b {
    public static int builds;
    public static boolean fail;
    public static View body;
    public static c state;
    public static boolean night;
    public static MixColorPluginDrawable last;
    public static MixColorPluginDrawable a(Context context, View view, c value) {
        builds++; body=view; state=value;
        night=(context.getResources().getConfiguration().uiMode&0x30)==0x20;
        if(fail)throw new IllegalStateException("device material unavailable");
        return last=new MixColorPluginDrawable();
    }
}
