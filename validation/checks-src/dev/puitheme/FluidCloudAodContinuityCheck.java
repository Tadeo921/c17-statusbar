package dev.puitheme;

import android.os.Bundle;
import android.os.Handler;
import android.graphics.drawable.GradientDrawable;
import com.android.systemui.plugins.custom.CustomRowEntry;
import com.android.systemui.statusbar.StatusBarStateControllerImpl;
import com.android.systemui.statusbar.notification.row.NotificationBackgroundView;
import com.oplus.systemui.statusbar.notification.customcard.OplusCustomRow;
import com.oplus.systemui.statusbar.notification.row.NotificationBackgroundViewExtImp;
import com.oplus.systemui.notification.aod.OplusPanoramicAodCoreStartable;
import com.oplus.systemui.notification.row.NotificationBackgroundCustomRowHandler;
import com.oplus.systemui.notification.row.material.NotificationBlurMode;
import com.oplus.posteffect.drawable.MetaBallBlurDrawable;
import com.oplusos.systemui.common.blurability.ViewBlurProxy;

/** First/last AOD frames must not swap the visible body to opaque CUSTOM gray. */
public final class FluidCloudAodContinuityCheck {
    private static int checks;
    private static void check(boolean okay,String why){checks++;if(!okay)throw new AssertionError(why);}
    private static void drain(){int turns=0;while(!Handler.postedForCheck.isEmpty()){
        if(++turns>30)throw new AssertionError("AOD refresh did not settle");Handler.drainForCheck();}}
    public static void main(String[] args)throws Exception {
        for(int backend:new int[]{0,1,2})checkBackend(backend);mediaMask();lockColourTargets();
        System.out.println("FluidCloudAodContinuityCheck: "+checks+" checks passed");
    }
    private static void checkBackend(int backend)throws Exception {
        Handler.postedForCheck.clear();FluidCloudStyle style=new FluidCloudStyle();ClassLoader loader=FluidCloudAodContinuityCheck.class.getClassLoader();
        style.resolveScene(loader);style.resolveNotification(loader);style.setNotificationStyler((view,following,keep)->{});
        NotificationBackgroundView body=new NotificationBackgroundView(){
            final android.content.res.Resources resources=new android.content.res.Resources(){
                @Override public int getIdentifier(String name,String type,String pkg){return name.equals("notification_material_background_color_compass")?7:0;}
                @Override public int getColor(int id,Theme theme){return 0xff667788;}
            };
            @Override public android.content.res.Resources getResources(){return resources;}
        };body.mOnKeyguard=true;
        OplusCustomRow row=new OplusCustomRow();body.parent=row;CustomRowEntry entry=new CustomRowEntry();row.setEntry(entry);row.setMBackgroundNormal(body);
        NotificationBackgroundViewExtImp ext=(NotificationBackgroundViewExtImp)body.mExt;
        ViewBlurProxy proxy=ext.proxyForCheck;proxy.styleForCheck=style;proxy.backendForCheck=backend;
        ext.viewBlurManager.motionForCheck=backend==1;ext.viewBlurManager.mode=NotificationBlurMode.WALLPAPER_A;
        ext.notificationBlurMode=NotificationBlurMode.WALLPAPER_A;
        Object rawMix=proxy.blurConfig.platformMixConfig;
        GradientDrawable plate=new GradientDrawable();plate.setColor(0xff333333);plate.setAlpha(147);ext.tempDrawable=plate;
        style.notificationRow(row);style.sceneMaterialChanged(ext);style.setKeyguard(true);
        Bundle options=new Bundle();options.putBoolean(FluidCloudStyle.MASTER,true);options.putBoolean(FluidCloudStyle.FOLLOW_NOTIFICATION,true);style.configure(options);drain();
        android.graphics.drawable.Drawable glass=style.sceneDrawable(body,plate);
        check(glass!=plate,"normal notification glass proven before transition: "+backend);
        NotificationBackgroundCustomRowHandler handler=new NotificationBackgroundCustomRowHandler();MetaBallBlurDrawable meta=new MetaBallBlurDrawable();
        handler.updateDrawableMetaColor(body.getContext(),entry,meta);style.sceneMetaChanged(handler,body.getContext(),entry,meta);
        check(meta.metaColor==0,"native gray template omitted before AOD");
        OplusPanoramicAodCoreStartable aod=(OplusPanoramicAodCoreStartable)ext.oplusPanoramicAodRepository;
        ext.maskStyleForCheck=style;
        int applies=proxy.platformAppliesForCheck+proxy.motionAppliesForCheck+proxy.wallpaperAppliesForCheck;
        int factories=ext.viewBlurManager.mixes+ext.viewBlurManager.motionMixes,reads=aod.readsForCheck;
        Object mix=backend==1?proxy.appliedMotionMixForCheck:proxy.appliedMixForCheck;
        for(float progress:new float[]{.0001f,.3f,.9f,1f,.9f,.3f,.0001f,-.00001f}) {
            style.prepareSceneAodProgress(aod,progress);aod.progressForCheck(progress);
            // The audited OEM setter invokes native listeners synchronously.
            // Their material submissions occur before our post-setter hook.
            proxy.applyConfigToPlatformBlur();proxy.applyConfigToMotionBlur();proxy.applyConfigToBlendWallpaper();
            check(proxy.platformAppliesForCheck+proxy.motionAppliesForCheck+proxy.wallpaperAppliesForCheck==applies,"first synchronous native callback cannot overwrite glass before post-setter hook");
            style.sceneAodProgressChanged(aod,progress);drain();
            com.oplus.systemui.notification.aod.notification.OplusAodRowBackgroundDrawable nativeMask=new com.oplus.systemui.notification.aod.notification.OplusAodRowBackgroundDrawable(body.getContext());
            nativeMask.setColor(0x78000000);nativeMask.setBounds(0,0,240,80);
            ext.setPanoramicAodDrawable(nativeMask);
            android.graphics.drawable.Drawable first=ext.panoramicAodDrawable;
            check(first!=nativeMask&&first.getClass()==nativeMask.getClass(),"AOD uses private native rounded mask instead of shared black mask");
            check(((android.graphics.drawable.ColorDrawable)first).getColor()==0x78667788&&nativeMask.getColor()==0x78000000,"AOD alpha retained and shared native mask untouched");
            int allocations=com.oplus.systemui.notification.aod.notification.OplusAodRowBackgroundDrawable.creations;
            for(int frame=0;frame<100;frame++)check(style.sceneAodMask(ext,nativeMask)==first,"stable AOD mask reuses one owned drawable");
            check(com.oplus.systemui.notification.aod.notification.OplusAodRowBackgroundDrawable.creations==allocations,"no allocation per AOD frame");

            check(style.sceneDrawable(body,plate)==glass&&meta.metaColor==0&&!style.sceneNeedsMeta(entry,true),"first/intermediate/last nonzero AOD frame retains same glass, no gray flash: "+progress);
            boolean previous=style.beginSceneFrame();
            try(AutoCloseable scope=style.beginSceneDraw(ext)){check(ext.tempDrawable==null,"no extra CUSTOM plate enters native panoramic crossfade");}
            finally{style.endSceneFrame(previous);}
            check(ext.tempDrawable==plate&&plate.getAlpha()==147,"draw restores exact native field and leaves animation alpha unchanged");
            proxy.applyConfigToPlatformBlur();proxy.applyConfigToMotionBlur();proxy.applyConfigToBlendWallpaper();
        }
        check(proxy.platformAppliesForCheck+proxy.motionAppliesForCheck+proxy.wallpaperAppliesForCheck==applies
                &&ext.viewBlurManager.mixes+ext.viewBlurManager.motionMixes==factories,"AOD intermediate frames never resubmit or compute material");
        StatusBarStateControllerImpl state=new StatusBarStateControllerImpl();state.state=1;state.dozing=true;style.onNativeState(state);drain();
        reads=aod.readsForCheck;
        for(int i=0;i<10000;i++){boolean previous=style.beginSceneFrame();try{style.sceneDrawable(body,plate);style.sceneMaterialChanged(ext);style.scenePaletteChanged(ext);proxy.applyConfigToBlendWallpaper();}finally{style.endSceneFrame(previous);}}
        check(Handler.postedForCheck.isEmpty()&&aod.readsForCheck==reads&&style.sceneDrawable(body,plate)==glass,"10000 AOD draw frames keep native animation ownership without polling or refresh loops");
        aod.progressForCheck(0);style.sceneAodProgressChanged(aod,0);drain();state.dozing=false;style.onNativeState(state);drain();
        check(style.sceneDrawable(body,plate)==glass&&meta.metaColor==0,"wake finishes on same native glass, without an intervening CUSTOM frame");
        aod.progressForCheck(.1f);style.sceneAodProgressChanged(aod,.1f);drain();
        aod.progressForCheck(Float.NaN);style.sceneAodProgressChanged(aod,Float.NaN);drain();
        check(style.sceneDrawable(body,plate)==plate&&meta.metaColor==handler.nativeColor,"unknown progress restores native; no frozen unverified material");
        aod.progressForCheck(0);style.sceneAodProgressChanged(aod,0);drain();
        aod.progressForCheck(.2f);style.sceneAodProgressChanged(aod,.2f);drain();
        com.oplus.systemui.notification.aod.notification.OplusAodRowBackgroundDrawable currentNativeMask=new com.oplus.systemui.notification.aod.notification.OplusAodRowBackgroundDrawable(body.getContext());
        currentNativeMask.setColor(0x99000000);ext.setPanoramicAodDrawable(currentNativeMask);
        check(ext.panoramicAodDrawable!=currentNativeMask,"active AOD ownership established before disabling");
        options.putBoolean(FluidCloudStyle.FOLLOW_NOTIFICATION,false);style.configure(options);drain();
        check(ext.panoramicAodDrawable==currentNativeMask,"disable restores latest native AOD object immediately");
        com.oplus.systemui.notification.aod.notification.OplusAodRowBackgroundDrawable disabledMask=new com.oplus.systemui.notification.aod.notification.OplusAodRowBackgroundDrawable(body.getContext());
        check(style.sceneAodMask(ext,disabledMask)==disabledMask,"disabling following during AOD retains native mask");
        check(style.sceneDrawable(body,plate)==plate&&meta.metaColor==handler.nativeColor
                &&(backend==1?proxy.appliedMotionMixForCheck==proxy.blurConfig.motionBlurMixConfig:proxy.appliedMixForCheck==rawMix),"disable during AOD restores raw current native material and template");
        options.putBoolean(FluidCloudStyle.FOLLOW_NOTIFICATION,true);style.configure(options);drain();
        ext.setPanoramicAodDrawable(null);style.sceneMaterialChanged(ext);drain();
        check(ext.panoramicAodDrawable==null,"native clear stays clear across policy refresh");
        style.release();drain();
    }
    private static void mediaMask()throws Exception {
        Handler.postedForCheck.clear();ClassLoader loader=FluidCloudAodContinuityCheck.class.getClassLoader();
        FluidCloudStyle style=new FluidCloudStyle();FluidCloudAccent accent=new FluidCloudAccent();style.setAccentController(accent);
        accent.resolve(loader);style.resolveScene(loader);style.resolveNotification(loader);style.resolveMedia(loader);style.setNotificationStyler((body,enabled,keep)->{});
        android.view.ViewGroup root=new android.view.ViewGroup(new android.content.Context());
        com.oplus.systemui.plugins.shared.template.section.media.K0 model=new com.oplus.systemui.plugins.shared.template.section.media.K0(new kotlinx.coroutines.flow.p(new com.oplus.systemui.plugins.shared.data.media.model.a(0xffa08060)));
        com.oplus.systemui.plugins.shared.template.section.media.s1 section=new com.oplus.systemui.plugins.shared.template.section.media.s1(root.getContext(),model,root,null);
        ((kotlinx.coroutines.flow.p)model.U.b).value=Boolean.TRUE;style.mediaChanged(section);
        NotificationBackgroundView body=paletteBody();body.mOnKeyguard=true;OplusCustomRow row=new OplusCustomRow();body.parent=row;
        CustomRowEntry entry=new CustomRowEntry();entry.immersiveCardView=root;row.setEntry(entry);row.setMBackgroundNormal(body);
        NotificationBackgroundViewExtImp ext=(NotificationBackgroundViewExtImp)body.mExt;ext.maskStyleForCheck=style;
        ext.notificationBlurMode=NotificationBlurMode.WALLPAPER_A;ext.viewBlurManager.mode=NotificationBlurMode.WALLPAPER_A;ext.proxyForCheck.styleForCheck=style;
        style.notificationRow(row);style.setKeyguard(true);
        Bundle config=new Bundle();config.putBoolean(FluidCloudStyle.MASTER,true);config.putBoolean(FluidCloudStyle.FOLLOW_NOTIFICATION,true);
        config.putBoolean(FluidCloudStyle.DESKTOP_BACKGROUND,true);config.putFloat(FluidCloudStyle.BACKGROUND_OPACITY,15.12f);config.putBoolean(FluidCloudStyle.FOLLOW_MEDIA,true);
        config.putBoolean(FluidCloudAccent.ENABLED,true);
        config.putBoolean(FluidCloudAccent.MASK_ENABLED,true);config.putInt(FluidCloudAccent.MASK_COLOR,0xff000000);config.putFloat(FluidCloudAccent.MASK_OPACITY,50f);
        accent.configure(config);style.configure(config);drain();
        int tint=FluidCloudAccent.mask(0xffa08060,0xff000000,50f);
        check(style.notificationUsesAccent(body)&&style.normalNotificationColor(body)==tint,"media tint and shared mask apply on awake lockscreen independently from floating opacity");
        OplusPanoramicAodCoreStartable aod=(OplusPanoramicAodCoreStartable)ext.oplusPanoramicAodRepository;
        com.oplus.systemui.notification.aod.notification.OplusAodRowBackgroundDrawable nativeMask=new com.oplus.systemui.notification.aod.notification.OplusAodRowBackgroundDrawable(body.getContext());nativeMask.setColor(0x78000000);
        for(float progress:new float[]{.001f,.4f,1f,.4f,.001f}) {
            style.prepareSceneAodProgress(aod,progress);aod.progressForCheck(progress);ext.setPanoramicAodDrawable(nativeMask);style.sceneAodProgressChanged(aod,progress);drain();
            check(((android.graphics.drawable.ColorDrawable)ext.panoramicAodDrawable).getColor()==0x78667788,"AOD media uses neutral native glass below the sole accent layer: "+progress);
        }
        aod.progressForCheck(1f);style.sceneAodProgressChanged(aod,1f);StatusBarStateControllerImpl state=new StatusBarStateControllerImpl();state.state=1;state.dozing=true;style.onNativeState(state);drain();
        ext.setPanoramicAodDrawable(nativeMask);check(((android.graphics.drawable.ColorDrawable)ext.panoramicAodDrawable).getColor()==0x78667788,"settled dozing keeps the same neutral glass below the scoped media layer");
        config.putBoolean(FluidCloudStyle.FOLLOW_NOTIFICATION,false);config.putBoolean(FluidCloudStyle.FOLLOW_MEDIA,false);config.putBoolean(FluidCloudStyle.DESKTOP_BACKGROUND,false);
        style.configure(config);config.putBoolean(FluidCloudAccent.ENABLED,false);accent.configure(config);style.refreshAfterAccent();drain();
        check(ext.panoramicAodDrawable==nativeMask,"turning every lockscreen owner off restores latest native mask during AOD");
        style.release();accent.release();drain();
    }

    /** Exercise the visible colour policy at the lock/AOD boundary, including the paths that do not
     * require playback or the optional bottom-capsule tint. The native mask is a separate blend layer;
     * equality here is about the actual overlay/gradient target passed to the background renderer. */
    private static void lockColourTargets()throws Exception {
        for(boolean media:new boolean[]{false,true}) {
            Handler.postedForCheck.clear();ClassLoader loader=FluidCloudAodContinuityCheck.class.getClassLoader();
            FluidCloudStyle style=new FluidCloudStyle();FluidCloudAccent accent=new FluidCloudAccent();style.setAccentController(accent);
            accent.resolve(loader);style.resolveScene(loader);style.resolveNotification(loader);style.resolveMedia(loader);
            NotificationBackgroundView body=paletteBody();body.mOnKeyguard=true;
            OplusCustomRow row=new OplusCustomRow();body.parent=row;CustomRowEntry entry=new CustomRowEntry();row.setEntry(entry);row.setMBackgroundNormal(body);
            NotificationBackgroundViewExtImp ext=(NotificationBackgroundViewExtImp)body.mExt;ext.maskStyleForCheck=style;
            ext.notificationBlurMode=NotificationBlurMode.WALLPAPER_A;ext.viewBlurManager.mode=NotificationBlurMode.WALLPAPER_A;ext.proxyForCheck.styleForCheck=style;
            final Integer[] overlay={null},gradient={null};
            // The real host owns its media section until disposal; production bindings are deliberately weak.
            Object retainedMediaSection=null;
            style.setNotificationStyler(new FluidCloudStyle.NotificationStyler() {
                @Override public void update(android.view.View view,boolean enabled,boolean keep) { }
                @Override public void overlay(android.view.View view,Integer argb,java.util.function.BooleanSupplier needed) {if(view==body)overlay[0]=argb;}
                @Override public void gradient(android.view.View view,Integer argb) {if(view==body)gradient[0]=argb;}
            });
            if(media) {
                android.view.ViewGroup root=new android.view.ViewGroup(new android.content.Context());
                com.oplus.systemui.plugins.shared.template.section.media.K0 model=new com.oplus.systemui.plugins.shared.template.section.media.K0(new kotlinx.coroutines.flow.p(new com.oplus.systemui.plugins.shared.data.media.model.a(0xffa08060)));
                com.oplus.systemui.plugins.shared.template.section.media.s1 section=new com.oplus.systemui.plugins.shared.template.section.media.s1(root.getContext(),model,root,null);
                ((kotlinx.coroutines.flow.p)model.U.b).value=Boolean.TRUE;entry.immersiveCardView=root;style.mediaChanged(section);
                retainedMediaSection=section;
            }
            style.notificationRow(row);style.setKeyguard(true);
            Bundle config=new Bundle();config.putBoolean(FluidCloudStyle.MASTER,true);config.putBoolean(FluidCloudStyle.FOLLOW_NOTIFICATION,true);
            config.putBoolean(FluidCloudStyle.DESKTOP_BACKGROUND,true);config.putString(FluidCloudStyle.BACKGROUND_MODE,"custom");
            config.putInt(FluidCloudStyle.BACKGROUND_COLOR,0xff204060);config.putFloat(FluidCloudStyle.BACKGROUND_OPACITY,50f);
            config.putBoolean(FluidCloudStyle.LOCK_CAPSULE,false);
            config.putBoolean(FluidCloudAccent.ENABLED,media);config.putBoolean(FluidCloudAccent.LOCK_CUSTOM,true);config.putBoolean(FluidCloudAccent.AOD_CUSTOM,true);
            config.putFloat(FluidCloudAccent.LOCK_OPACITY,50f);config.putFloat(FluidCloudAccent.AOD_OPACITY,50f);config.putFloat(FluidCloudAccent.LOCK_MASK_OPACITY,0f);config.putFloat(FluidCloudAccent.AOD_MASK_OPACITY,0f);
            config.putFloat(FluidCloudAccent.LOCK_LIGHT,70f);config.putFloat(FluidCloudAccent.AOD_LIGHT,70f);config.putBoolean(FluidCloudAccent.LOCK_GRADIENT,media);config.putBoolean(FluidCloudAccent.AOD_GRADIENT,media);
            accent.configure(config);style.configure(config);style.refreshAfterAccent();drain();style.finishFillFadesForCheck();drain();
            Integer awake=media?gradient[0]:overlay[0];
            check(awake!=null&&(awake>>>24)==128,"lock target remains translucent with 50% opacity, media="+media);
            check((awake&0xffffff)!=(media?0xa08060:0x204060),"lock light changes RGB without making the target opaque, media="+media);
            check(media?overlay[0]==null:gradient[0]==null,"only the selected fill style is submitted, media="+media);
            check(style.lockCapsulePlan(media)==null,"optional bottom capsule remains off independently from card policy");
            OplusPanoramicAodCoreStartable aod=(OplusPanoramicAodCoreStartable)ext.oplusPanoramicAodRepository;
            StatusBarStateControllerImpl state=new StatusBarStateControllerImpl();state.state=1;
            for(float progress:new float[]{.001f,.4f,1f,.4f,.001f,0f}) {
                style.prepareSceneAodProgress(aod,progress);aod.progressForCheck(progress);style.sceneAodProgressChanged(aod,progress);
                state.dozing=progress==1f;style.onNativeState(state);drain();style.finishFillFadesForCheck();drain();
                check(awake.equals(media?gradient[0]:overlay[0]),"every AOD boundary uses the same translucent lock colour target: "+progress+", media="+media);
                check(media?overlay[0]==null:gradient[0]==null,"AOD never replaces a chosen gradient with a flat fill, media="+media);
                if(progress!=0f) {
                    com.oplus.systemui.notification.aod.notification.OplusAodRowBackgroundDrawable nativeMask=new com.oplus.systemui.notification.aod.notification.OplusAodRowBackgroundDrawable(body.getContext());
                    nativeMask.setColor(0x78000000);ext.setPanoramicAodDrawable(nativeMask);
                    check(((android.graphics.drawable.ColorDrawable)ext.panoramicAodDrawable).getColor()==0x78667788,
                            "AOD mask stays neutral instead of duplicating or making the chosen colour opaque, media="+media);
                    check(nativeMask.getColor()==0x78000000,"the original OEM mask remains untouched");
                }
            }
            java.lang.ref.Reference.reachabilityFence(retainedMediaSection);
            style.release();accent.release();drain();
        }
    }

    private static NotificationBackgroundView paletteBody() {
        return new NotificationBackgroundView() {
            final android.content.res.Resources palette=new android.content.res.Resources() {
                @Override public int getIdentifier(String name,String type,String pkg){return name.equals("notification_material_background_color_compass")?7:0;}
                @Override public int getColor(int id,Theme theme){return 0xff667788;}
            };
            @Override public android.content.res.Resources getResources(){return palette;}
        };
    }

}
