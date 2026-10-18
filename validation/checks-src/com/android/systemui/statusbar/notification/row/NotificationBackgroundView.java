package com.android.systemui.statusbar.notification.row;
import android.content.Context;
import android.view.View;
/** Real native background host identity; draw branching is exercised by callers. */
public class NotificationBackgroundView extends View {
    public int actualWidth=124,actualHeight=80;
    public int mTintColor=0xffeeeeee;
    public final int mNormalColor=0xffdddddd;
    public boolean mOnKeyguard;
    public final NotificationBackgroundViewExt mExt=new com.oplus.systemui.statusbar.notification.row.NotificationBackgroundViewExtImp(this);
    public NotificationBackgroundView(){super(new Context());}
    public final int getActualWidth(){return actualWidth;}
    public final int getActualHeight(){return actualHeight;}
}
