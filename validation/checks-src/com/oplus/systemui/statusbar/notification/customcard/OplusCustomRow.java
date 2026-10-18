package com.oplus.systemui.statusbar.notification.customcard;
import com.android.systemui.statusbar.notification.row.NotificationBackgroundView;
/** Exact native body lifecycle contract, independent of immersive music art. */
public class OplusCustomRow extends android.view.ViewGroup {
    private NotificationBackgroundView mBackgroundNormal;
    private com.android.systemui.plugins.custom.CustomRowEntry entry;
    public android.graphics.drawable.Drawable immersiveCustomDrawable;
    public int actualHeight=80,clipTopAmount,clipBottomAmount;
    public OplusCustomRow(){super(null);}
    public final NotificationBackgroundView getMBackgroundNormal(){return mBackgroundNormal;}
    public final void setMBackgroundNormal(NotificationBackgroundView value){mBackgroundNormal=value;}
    protected void onFinishInflate(){mBackgroundNormal=new NotificationBackgroundView();}
    protected void onAttachedToWindow(){ }
    public final void updateResource(boolean changed){ }
    public final void setEntry(com.android.systemui.plugins.custom.CustomRowEntry entry){ this.entry=entry; }
    public final com.android.systemui.plugins.custom.CustomRowEntry getEntry(){ return entry; }
    public final void setNewSeedlingView(android.view.View value){ }
    public int getActualHeight(){return actualHeight;}
    public int getClipTopAmount(){return clipTopAmount;}
    public int getClipBottomAmount(){return clipBottomAmount;}
}
