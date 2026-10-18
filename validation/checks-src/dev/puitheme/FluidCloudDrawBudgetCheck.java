package dev.puitheme;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import com.android.systemui.plugins.custom.CustomRowEntry;
import com.android.systemui.statusbar.notification.row.ExpandableNotificationRow;
import com.android.systemui.statusbar.notification.row.NotificationBackgroundView;
import com.oplus.systemui.statusbar.notification.customcard.OplusCustomRow;
import com.oplus.systemui.statusbar.notification.row.NotificationBackgroundViewExtImp;
import com.oplus.systemui.notification.row.material.NotificationBlurMode;
import com.oplusos.systemui.common.blurability.ViewBlurProxy;

/** Draw-stack deferral, stable/failure budgets and geometry/lifecycle recovery. */
public final class FluidCloudDrawBudgetCheck {
    private static int checks;
    private static final android.content.res.Resources RESOURCES=new android.content.res.Resources();
    private static final class Body extends NotificationBackgroundView {
        int width=240;
        @Override public int getWidth(){return width;}
        @Override public android.content.res.Resources getResources(){return RESOURCES;}
    }
    private static void check(boolean okay,String why){checks++;if(!okay)throw new AssertionError(why);}
    private static void drain() {
        int turns=0;
        while(!Handler.postedForCheck.isEmpty()) {
            if(++turns>30)throw new AssertionError("refresh queue does not settle");
            java.util.List<Runnable> pending=new java.util.ArrayList<>(Handler.postedForCheck);
            Handler.postedForCheck.clear();for(Runnable task:pending)task.run();
        }
    }
    private static Drawable frame(FluidCloudStyle style,Body body,Drawable original,ViewBlurProxy proxy) {
        boolean previous=style.beginSceneFrame();
        try {
            Drawable value=style.sceneDrawable(body,original);
            style.sceneMaterialChanged(body.mExt);style.sceneProxyChanged(proxy);
            proxy.applyConfigToPlatformBlur();return value;
        }finally{style.endSceneFrame(previous);}
    }
    public static void main(String[] args)throws Exception {
        Handler.postedForCheck.clear();
        FluidCloudStyle style=new FluidCloudStyle();ClassLoader loader=FluidCloudDrawBudgetCheck.class.getClassLoader();
        style.resolveScene(loader);style.resolveNotification(loader);
        Body body=new Body();body.actualHeight=90;OplusCustomRow row=new OplusCustomRow();body.parent=row;
        CustomRowEntry entry=new CustomRowEntry();row.setEntry(entry);row.setMBackgroundNormal(body);
        NotificationBackgroundViewExtImp ext=(NotificationBackgroundViewExtImp)body.mExt;
        ext.notificationBlurMode=NotificationBlurMode.WALLPAPER_A;ViewBlurProxy proxy=ext.proxyForCheck;proxy.styleForCheck=style;
        GradientDrawable nativePlate=new GradientDrawable();nativePlate.setColor(0xff555555);ext.tempDrawable=nativePlate;
        style.notificationRow(row);
        Bundle settings=new Bundle();settings.putBoolean(FluidCloudStyle.MASTER,true);settings.putBoolean(FluidCloudStyle.FOLLOW_NOTIFICATION,true);
        style.configure(settings);drain();style.sceneGeometryChanged(body);drain();
        frame(style,body,nativePlate,proxy);drain();
        int applies=proxy.platformAppliesForCheck,mixes=ext.viewBlurManager.mixes,invalidates=body.invalidations;
        Object appliedMix=proxy.appliedMixForCheck;
        for(int i=0;i<10000;i++)frame(style,body,nativePlate,proxy);
        check(proxy.platformAppliesForCheck==applies&&ext.viewBlurManager.mixes==mixes,"10000 stable draw stacks do not submit or rebuild material");
        check(Handler.postedForCheck.isEmpty()&&body.invalidations==invalidates,"stable draw does not schedule its next frame");
        check(proxy.appliedMixForCheck==appliedMix&&appliedMix!=proxy.blurConfig.platformMixConfig,"native draw cannot overwrite cached NOTIFICATION recipe with raw CUSTOM fields");
        check(frame(style,body,nativePlate,proxy)==proxy.drawableForCheck,"stable frame uses proven owned material drawable");
        for(int i=0;i<10000;i++)style.sceneGeometryChanged(body);
        check(Handler.postedForCheck.isEmpty(),"repeated identical setActualHeight does not invalidate geometry");
        body.actualHeight=120;style.sceneGeometryChanged(body);
        check(Handler.postedForCheck.size()==1,"actual height change queues one update");
        boolean previous=style.beginSceneFrame();
        try{proxy.applyConfigToPlatformBlur();check(proxy.platformAppliesForCheck==applies,"height animation never submits from draw");}
        finally{style.endSceneFrame(previous);}
        drain();check(proxy.platformAppliesForCheck==applies+1,"geometry refresh submits once outside draw");
        check(((com.oplus.posteffect.params.InnerShadowParams)proxy.appliedShadowForCheck).height==120,"size-specific native shadow follows actual height");
        proxy.failApplyCalls=1;body.actualHeight=121;style.sceneGeometryChanged(body);drain();
        applies=proxy.platformAppliesForCheck;mixes=ext.viewBlurManager.mixes;invalidates=body.invalidations;
        for(int i=0;i<10000;i++)check(frame(style,body,nativePlate,proxy)==nativePlate,"failed native material preserves original fallback");
        check(proxy.platformAppliesForCheck==applies&&ext.viewBlurManager.mixes==mixes&&Handler.postedForCheck.isEmpty(),"failed same geometry never retries per frame");
        check(body.invalidations==invalidates,"failure does not keep SystemUI rendering itself");
        body.actualHeight=122;style.sceneGeometryChanged(body);drain();
        check(frame(style,body,nativePlate,proxy)==proxy.drawableForCheck,"real next geometry event allows one recovery");
        drain();
        Body ordinary=new Body();ordinary.parent=new ExpandableNotificationRow(new Context(),80,0,0);
        NotificationBackgroundViewExtImp donor=(NotificationBackgroundViewExtImp)ordinary.mExt;
        donor.notificationBlurMode=NotificationBlurMode.SHADE_DARK;ordinary.mTintColor=0xcc123456;
        int blurUpdates=ext.blurUpdates;
        previous=style.beginSceneFrame();
        try{style.sceneMaterialChanged(donor);check(ext.blurUpdates==blurUpdates,"first donor observation cannot mutate another body during draw");}
        finally{style.endSceneFrame(previous);}
        drain();check(ext.notificationBlurMode==NotificationBlurMode.SHADE_DARK,"first ordinary donor discovered in draw supplies real scene after deferred event");
        for(int i=0;i<10000;i++) {
            previous=style.beginSceneFrame();try{style.sceneMaterialChanged(donor);}finally{style.endSceneFrame(previous);}
        }
        check(Handler.postedForCheck.isEmpty(),"stable ordinary colour setter is not a polling loop");
        applies=proxy.platformAppliesForCheck;ext.viewBlurManager.motionForCheck=true;
        style.sceneMotionChanged(ext.viewBlurManager,proxy);drain();
        check(proxy.platformAppliesForCheck==applies+1&&((com.oplusos.systemui.common.blurability.BlurMixConfig)proxy.appliedMixForCheck).mirrorScale==0.94f,
                "motion-only native event updates full same-scene notification recipe once");
        for(int i=0;i<10000;i++)style.sceneMotionChanged(ext.viewBlurManager,proxy);
        check(Handler.postedForCheck.isEmpty(),"unchanged native motion event cannot create refresh loop");
        ext.viewBlurManager.motionForCheck=false;style.sceneMotionChanged(ext.viewBlurManager,proxy);drain();
        check(((com.oplusos.systemui.common.blurability.BlurMixConfig)proxy.appliedMixForCheck).mirrorScale==1f,"motion disable restores native mirror scale");
        style.setKeyguard(true);drain();
        previous=style.beginSceneFrame();try{check(style.deferScenePlatform(proxy),"confirmed local WALLPAPER material stays owned even when global panel mode is SHADE");}finally{style.endSceneFrame(previous);}
        drain();
        Body unknown=new Body();unknown.actualHeight=90;unknown.mOnKeyguard=true;
        OplusCustomRow unknownRow=new OplusCustomRow();unknown.parent=unknownRow;unknownRow.setEntry(new CustomRowEntry());unknownRow.setMBackgroundNormal(unknown);
        NotificationBackgroundViewExtImp unknownExt=(NotificationBackgroundViewExtImp)unknown.mExt;
        unknownExt.notificationBlurMode=NotificationBlurMode.SHADE_DARK;unknownExt.viewBlurManager.mode=NotificationBlurMode.SHADE_DARK;
        style.notificationRow(unknownRow);style.sceneGeometryChanged(unknown);drain();
        previous=style.beginSceneFrame();try{check(!style.deferScenePlatform(unknownExt.proxyForCheck),"unconfirmed lockscreen material keeps native submissions");}finally{style.endSceneFrame(previous);}
        style.setKeyguard(false);drain();body.actualHeight=123;style.sceneGeometryChanged(body);style.detach(body);drain();
        check(style.sceneDrawable(body,nativePlate)==nativePlate,"queued geometry cannot reacquire detached owner");
        style.release();drain();check(Handler.postedForCheck.isEmpty(),"release drains without self-resurrection");
        System.out.println("FluidCloudDrawBudgetCheck: "+checks+" checks passed");
    }
}
