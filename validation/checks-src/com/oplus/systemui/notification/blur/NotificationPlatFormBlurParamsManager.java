package com.oplus.systemui.notification.blur;
import android.content.Context;
import com.oplus.posteffect.params.GradientStrokeLineParams;
import com.oplus.posteffect.params.InnerShadowParams;
import com.oplus.systemui.notification.row.material.NotificationBlurMode;
public class NotificationPlatFormBlurParamsManager {
    public static final NotificationPlatFormBlurParamsManager INSTANCE=new NotificationPlatFormBlurParamsManager();
    public int allocations;
    public boolean noStrokeForCheck,noShadowForCheck;
    public GradientStrokeLineParams strokeForCheck;
    public InnerShadowParams shadowForCheck;
    public void updateMirrorScale(ViewBlurManager.CardType type,NotificationBlurMode mode,com.oplusos.systemui.common.blurability.BlurMixConfig mix,boolean motion){
        mix.setMirrorScale(motion&&mode.name().startsWith("SHADE_")?0.94f:1f);
    }
    public GradientStrokeLineParams getGradientStrokeLineParams(ViewBlurManager.CardType type,NotificationBlurMode mode){allocations++;return noStrokeForCheck?null:strokeForCheck!=null?strokeForCheck:new GradientStrokeLineParams();}
    public InnerShadowParams getInnerShadowParams(ViewBlurManager.CardType type,NotificationBlurMode mode){allocations++;return noShadowForCheck?null:shadowForCheck!=null?shadowForCheck:new InnerShadowParams();}
    public void updateGradientStrokeLineParamsForNotificationSize(Context context,GradientStrokeLineParams value,float width,float height,NotificationBlurMode mode){value.width=width;value.height=height;value.mode=mode;}
    public void updateInnerShadowParamsForNotificationSize(Context context,InnerShadowParams value,float width,float height,NotificationBlurMode mode){value.width=width;value.height=height;value.mode=mode;}
}
