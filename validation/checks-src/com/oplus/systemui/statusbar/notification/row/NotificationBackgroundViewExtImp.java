package com.oplus.systemui.statusbar.notification.row;
import android.graphics.drawable.Drawable;
import com.android.systemui.statusbar.notification.row.NotificationBackgroundView;
import com.android.systemui.statusbar.notification.row.NotificationBackgroundViewExt;
/** Actual OEM superclass/field names from SystemUI, not a heuristic view-tree walk. */
public class NotificationBackgroundViewExtImp extends NotificationBackgroundViewExt {
    public android.graphics.drawable.Drawable tempDrawable;
    public com.oplus.systemui.notification.row.material.NotificationBlurMode notificationBlurMode=com.oplus.systemui.notification.row.material.NotificationBlurMode.UNINITIATED;
    public int materialUpdates,blurUpdates;
    public com.oplus.systemui.notification.blur.ViewBlurManager viewBlurManager=new com.oplus.systemui.notification.blur.ViewBlurManager();
    public com.oplusos.systemui.common.blurability.ViewBlurProxy proxyForCheck=new com.oplusos.systemui.common.blurability.ViewBlurProxy();
    public final com.oplus.systemui.notification.row.material.NotificationMaterialColorEx materialForCheck=new com.oplus.systemui.notification.row.material.NotificationMaterialColorEx();
    public final com.oplus.systemui.notification.aod.OplusPanoramicAodRepository oplusPanoramicAodRepository=new com.oplus.systemui.notification.aod.OplusPanoramicAodCoreStartable();
    public Drawable panoramicAodDrawable;
    public dev.puitheme.FluidCloudStyle maskStyleForCheck;
    public void setPanoramicAodDrawable(Drawable drawable){panoramicAodDrawable=maskStyleForCheck==null?drawable:maskStyleForCheck.sceneAodMask(this,drawable);}
    private Drawable zoomAppIcon;
    public NotificationBackgroundViewExtImp(NotificationBackgroundView host){super(host);proxyForCheck.view=host;}
    public com.oplusos.systemui.common.blurability.ViewBlurProxy getViewBlurProxy(){return proxyForCheck;}
    private com.oplus.systemui.notification.row.material.NotificationMaterialColorEx getNotificationMaterialColorEx(){return materialForCheck;}
    public void icon(Drawable icon){zoomAppIcon=icon;}
    public void notifyBlurParamChanged(com.oplus.systemui.notification.row.material.NotificationBlurMode mode){notificationBlurMode=mode;blurUpdates++;}
    public void setDrawableMaterialColor(){materialUpdates++;}
    public void notifyMaterialColorChanged(){materialUpdates++;}
    public void onHeadsUpStateChanged(boolean floating){ }
    public void draw(android.graphics.Canvas canvas,Drawable drawable){if(tempDrawable!=null)tempDrawable.draw(canvas);if(drawable!=null)drawable.draw(canvas);}
}
