package dev.puitheme;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import com.android.systemui.plugins.custom.CustomRowEntry;
import com.android.systemui.statusbar.notification.row.ExpandableNotificationRow;
import com.android.systemui.statusbar.notification.row.NotificationBackgroundView;
import com.oplus.posteffect.drawable.MetaBallBlurDrawable;
import com.oplus.systemui.notification.row.NotificationBackgroundCustomRowHandler;
import com.oplus.systemui.notification.row.material.NotificationBlurMode;
import com.oplus.systemui.statusbar.notification.customcard.OplusCustomRow;
import com.oplus.systemui.statusbar.notification.row.NotificationBackgroundViewExtImp;
import com.oplusos.systemui.common.blurability.BlurMixConfig;
import com.oplusos.systemui.common.blurability.ViewBlurProxy;

/** Business opt-out restores actual blur/MetaBall inputs, independent of the C17 optics marker. */
public final class FluidCloudScopeMaterialCheck {
    private static int checks;
    private static final android.content.res.Resources SCENE_RESOURCES=new android.content.res.Resources();
    private static final class Body extends NotificationBackgroundView {
        @Override public int getWidth(){return 240;}
        @Override public android.content.res.Resources getResources(){return SCENE_RESOURCES;}
    }
    private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    private static void drain(){Handler.drainForCheck();}
    private static NotificationBackgroundViewExtImp ext(NotificationBackgroundView body){return (NotificationBackgroundViewExtImp)body.mExt;}
    private static void realBackendRestore()throws Exception {
        Handler.postedForCheck.clear();FluidCloudStyle style=new FluidCloudStyle();
        try {
            ClassLoader loader=FluidCloudScopeMaterialCheck.class.getClassLoader();
            style.resolveScene(loader);style.resolveNotification(loader);style.setNotificationStyler((body,on,keep)->{});
            Body body=new Body();body.actualHeight=90;OplusCustomRow row=new OplusCustomRow();body.parent=row;
            row.setMBackgroundNormal(body);CustomRowEntry entry=new CustomRowEntry("268451854","");row.setEntry(entry);
            NotificationBackgroundViewExtImp extension=ext(body);ViewBlurProxy proxy=extension.proxyForCheck;proxy.styleForCheck=style;
            Object rawMix=proxy.blurConfig.platformMixConfig,rawStroke=proxy.blurConfig.gradientStrokeLineParam,rawShadow=proxy.blurConfig.innerShadowParams;
            GradientDrawable plate=new GradientDrawable();plate.setColor(0xff555555);extension.tempDrawable=plate;
            style.notificationRow(row);drain();
            Bundle settings=new Bundle();settings.putBoolean(FluidCloudStyle.MASTER,true);settings.putBoolean(FluidCloudStyle.FOLLOW_NOTIFICATION,true);
            style.configure(settings);drain();
            check(style.sceneDrawable(body,plate)==proxy.drawableForCheck,"selected card acquires the actual native platform backend");
            check(proxy.appliedMixForCheck!=rawMix&&((BlurMixConfig)proxy.appliedMixForCheck).recipe==NotificationBlurMode.SHADE_LIGHT,"selected card uses the normal notification recipe");
            NotificationBackgroundCustomRowHandler helper=new NotificationBackgroundCustomRowHandler();MetaBallBlurDrawable meta=new MetaBallBlurDrawable();
            helper.updateDrawableMetaColor(body.getContext(),entry,meta);style.sceneMetaChanged(helper,body.getContext(),entry,meta);
            check(meta.metaColor==0&&!style.sceneNeedsMeta(entry,true),"confirmed platform removes only its redundant template plate");

            settings.putBoolean(FluidCloudScope.Category.TIMER.key,false);style.configure(settings);drain();
            check(proxy.appliedMixForCheck==rawMix&&proxy.appliedStrokeForCheck==rawStroke&&proxy.appliedShadowForCheck==rawShadow,"business opt-out restores all actual native compositor inputs");
            check(style.sceneDrawable(body,plate)==plate&&style.sceneNeedsMeta(entry,true),"business opt-out returns the complete native drawable and template plate");
            check(meta.metaColor==helper.nativeColor,"business opt-out restores native MetaBall colour");
            try(AutoCloseable ignored=style.beginSceneDraw(extension)){check(extension.tempDrawable==plate,"opted-out card keeps the native temporary plate during drawing");}
            helper.nativeColor=0xff345678;helper.updateDrawableMetaColor(body.getContext(),entry,meta);style.sceneMetaChanged(helper,body.getContext(),entry,meta);
            check(meta.metaColor==helper.nativeColor,"later native business updates remain native while opted out");

            settings.putBoolean(FluidCloudScope.Category.TIMER.key,true);style.configure(settings);drain();
            check(style.sceneDrawable(body,plate)==proxy.drawableForCheck&&!style.sceneNeedsMeta(entry,true),"reenabling the target reacquires the saved follow policy");
            settings.putBoolean(FluidCloudScope.Category.TIMER.key,false);style.configure(settings);drain();
            check(meta.metaColor==0xff345678,"second opt-out restores latest native colour rather than a stale snapshot");
        } finally {style.release();drain();}
    }
    private static void modeIsolationAndIndependentAccent()throws Exception {
        Handler.postedForCheck.clear();FluidCloudSceneMaterial material=new FluidCloudSceneMaterial();
        try {
            material.resolve(FluidCloudScopeMaterialCheck.class.getClassLoader());
            NotificationBackgroundView ordinary=new Body();ordinary.parent=new ExpandableNotificationRow(new Context(),80,0,0);
            ext(ordinary).notifyBlurParamChanged((NotificationBlurMode)material.mode(ext(ordinary),NotificationBlurMode.SHADE_LIGHT));material.observed(ext(ordinary));
            NotificationBackgroundView first=new Body(),second=new Body();
            first.parent=new OplusCustomRow();second.parent=new OplusCustomRow();CustomRowEntry entry=new CustomRowEntry();
            material.register(first,entry);material.register(second,new CustomRowEntry());
            ext(first).notifyBlurParamChanged((NotificationBlurMode)material.mode(ext(first),NotificationBlurMode.WALLPAPER_A));material.observed(ext(first));
            ext(second).notifyBlurParamChanged((NotificationBlurMode)material.mode(ext(second),NotificationBlurMode.WALLPAPER_B));material.observed(ext(second));
            material.policy(true,false);drain();
            check(ext(first).notificationBlurMode==NotificationBlurMode.SHADE_LIGHT&&ext(second).notificationBlurMode==NotificationBlurMode.SHADE_LIGHT,"both selected targets may borrow the same-scene native mode");
            material.businessFollowing(first,false);drain();
            check(ext(first).notificationBlurMode==NotificationBlurMode.WALLPAPER_A,"opted-out target restores its own immutable native mode");
            check(ext(second).notificationBlurMode==NotificationBlurMode.SHADE_LIGHT,"per-target opt-out leaves another target following");
            check(material.mode(ext(first),NotificationBlurMode.NO_BLUR_LIGHT)==NotificationBlurMode.NO_BLUR_LIGHT,"future native mode setter bypasses donors for an opted-out target");

            NotificationBackgroundCustomRowHandler helper=new NotificationBackgroundCustomRowHandler();MetaBallBlurDrawable meta=new MetaBallBlurDrawable();
            helper.updateDrawableMetaColor(first.getContext(),entry,meta);material.meta(helper,first.getContext(),entry,meta);
            material.policy(true,true);material.accent(first,0xff2468ac);drain();
            check(meta.gradientColors!=null&&meta.gradientColors[0]==0xff2468ac,"independent lockscreen accent remains active for an opted-out business");
            material.businessFollowing(first,true);material.businessFollowing(first,false);drain();
            check(meta.gradientColors!=null&&meta.gradientColors[0]==0xff2468ac,"business toggles never clear independent accent ownership");
            material.accent(first,null);material.lockLook(first,true);drain();
            check(meta.metaColor==helper.nativeColor,"lock look without a colour retains native MetaBall while keeping its own material policy");
        } finally {material.release();drain();}
    }
    public static void main(String[] args)throws Exception {
        realBackendRestore();modeIsolationAndIndependentAccent();
        System.out.println("FluidCloudScopeMaterialCheck: "+checks+" checks passed");
    }
}
