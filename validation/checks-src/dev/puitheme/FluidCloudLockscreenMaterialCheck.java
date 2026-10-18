package dev.puitheme;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import com.android.systemui.plugins.custom.CustomRowEntry;
import com.android.systemui.statusbar.StatusBarStateControllerImpl;
import com.android.systemui.statusbar.notification.row.NotificationBackgroundView;
import com.oplus.systemui.statusbar.notification.customcard.OplusCustomRow;
import com.oplus.systemui.statusbar.notification.row.NotificationBackgroundViewExtImp;
import com.oplus.systemui.notification.row.material.NotificationBlurMode;
import com.oplus.systemui.notification.row.NotificationBackgroundCustomRowHandler;
import com.oplus.posteffect.drawable.MetaBallBlurDrawable;
import com.oplusos.systemui.common.blurability.ViewBlurProxy;

/** Native WALLPAPER material, independent music gradient, doze and restoration. */
public final class FluidCloudLockscreenMaterialCheck {
    private static int checks;
    private static final class Body extends NotificationBackgroundView {@Override public int getWidth(){return 240;}}
    private static void check(boolean okay,String why){checks++;if(!okay)throw new AssertionError(why);}
    private static GradientDrawable spatial(FluidCloudStyle style,NotificationBackgroundViewExtImp ext,GradientDrawable source,int color)throws Exception {
        GradientDrawable copy;
        try(AutoCloseable scope=style.beginSceneDraw(ext)) {
            check(ext.tempDrawable!=source&&ext.tempDrawable instanceof GradientDrawable,"actual visible immersive shape has a private spatial-gradient copy");
            copy=(GradientDrawable)ext.tempDrawable;
            check(copy.orientation==GradientDrawable.Orientation.TOP_BOTTOM&&java.util.Arrays.equals(copy.getColors(),new int[]{color,color&0x00ffffff}),"visible shape uses spatial top-to-bottom accent/transparent stops");
            check(copy.tintForCheck==null&&copy.getAlpha()==source.getAlpha()&&copy.getCornerRadius()==source.getCornerRadius(),"visible gradient clears copied solid tint and retains native animation alpha/corners");
        }
        check(ext.tempDrawable==source,"exact draw scope restores original immersive shape identity");
        return copy;
    }
    private static void nativePlateOmitted(FluidCloudStyle style,NotificationBackgroundViewExtImp ext,GradientDrawable source)throws Exception {
        int alpha=source.getAlpha();
        try(AutoCloseable scope=style.beginSceneDraw(ext)) {
            check(ext.tempDrawable==null,"proven normal glass omits the duplicate immersive colour plate");
        }
        check(ext.tempDrawable==source&&source.getAlpha()==alpha,"one-colour draw restores the exact native shape and animation alpha");
    }
    private static void drain() {
        int turns=0;while(!Handler.postedForCheck.isEmpty()) {
            if(++turns>30)throw new AssertionError("lockscreen refresh never settles");
            java.util.List<Runnable> pending=new java.util.ArrayList<>(Handler.postedForCheck);
            Handler.postedForCheck.clear();for(Runnable task:pending)task.run();
        }
    }
    private static void mode(FluidCloudStyle style,NotificationBackgroundViewExtImp ext,NotificationBlurMode mode) {
        ext.notifyBlurParamChanged((NotificationBlurMode)style.sceneBlurMode(ext,mode));style.sceneMaterialChanged(ext);
    }
    /** A countdown/stopwatch has no media binding and does not need the native-follow switch to obtain proven glass. */
    private static void nonMediaGradient() throws Exception {
        FluidCloudStyle style=new FluidCloudStyle();ClassLoader loader=FluidCloudLockscreenMaterialCheck.class.getClassLoader();
        style.resolveScene(loader);style.resolveNotification(loader);
        Body body=new Body();body.actualHeight=120;body.mOnKeyguard=true;
        OplusCustomRow row=new OplusCustomRow();body.parent=row;CustomRowEntry entry=new CustomRowEntry();row.setEntry(entry);row.setMBackgroundNormal(body);
        NotificationBackgroundViewExtImp ext=(NotificationBackgroundViewExtImp)body.mExt;ViewBlurProxy proxy=ext.proxyForCheck;proxy.styleForCheck=style;
        Object nativeMix=proxy.blurConfig.platformMixConfig;
        GradientDrawable plate=new GradientDrawable();plate.setColor(0xff444444);ext.tempDrawable=plate;row.immersiveCustomDrawable=plate;
        final Integer[] fade={null},flat={null};
        style.setNotificationStyler(new FluidCloudStyle.NotificationStyler(){
            @Override public void update(android.view.View view,boolean following,boolean keep){}
            @Override public void gradient(android.view.View view,Integer colour){if(view==body)fade[0]=colour;}
            @Override public void overlay(android.view.View view,Integer colour,java.util.function.BooleanSupplier gate){if(view==body)flat[0]=colour;}
        });
        style.notificationRow(row);mode(style,ext,NotificationBlurMode.WALLPAPER_A);ext.viewBlurManager.mode=NotificationBlurMode.WALLPAPER_A;
        style.setKeyguard(true);Bundle options=new Bundle();options.putBoolean(FluidCloudStyle.MASTER,true);
        options.putBoolean(FluidCloudStyle.DESKTOP_BACKGROUND,true);options.putBoolean(FluidCloudStyle.FOLLOW_NOTIFICATION,false);
        options.putString(FluidCloudStyle.BACKGROUND_MODE,"custom");options.putInt(FluidCloudStyle.BACKGROUND_COLOR,0xff76a1a8);
        options.putString(FluidCloudStyle.COLOR_STYLE,FluidCloudStyle.GRADIENT);options.putFloat(FluidCloudStyle.BACKGROUND_OPACITY,50f);
        style.configure(options);drain();style.notificationRow(row);drain();
        check(fade[0]!=null&&(fade[0]>>>24)==128&&flat[0]==null,"non-media fade keeps the selected opacity without a solid cover");
        check(style.sceneDrawable(body,plate)==proxy.drawableForCheck,"a final non-media colour layer alone requests proven normal wallpaper glass");
        check(proxy.appliedMixForCheck==nativeMix,"with native-follow disabled, non-media colour ownership retains its own native glass recipe");
        NotificationBackgroundCustomRowHandler helper=new NotificationBackgroundCustomRowHandler();MetaBallBlurDrawable meta=new MetaBallBlurDrawable();
        helper.updateDrawableMetaColor(body.getContext(),entry,meta);style.sceneMetaChanged(helper,body.getContext(),entry,meta);
        check(meta.metaColor==0&&!style.sceneNeedsMeta(entry,true),"confirmed non-media glass omits the extra opaque MetaBall base");
        nativePlateOmitted(style,ext,plate);
        Integer locked=fade[0];StatusBarStateControllerImpl state=new StatusBarStateControllerImpl();state.state=1;state.dozing=true;
        style.onNativeState(state);drain();check(locked.equals(fade[0])&&flat[0]==null&&style.sceneDrawable(body,plate)==proxy.drawableForCheck,
                "AOD holds the same non-media fade and proven glass");
        state.dozing=false;style.onNativeState(state);drain();
        proxy.nativeReturnsFallbackForCheck=true;body.actualHeight=121;style.sceneGeometryChanged(body);drain();
        check(style.sceneDrawable(body,plate)==plate&&meta.metaColor==helper.nativeColor&&style.sceneNeedsMeta(entry,true),
                "an unconfirmed non-media blur retains its real native base instead of deleting it");
        options.putString(FluidCloudStyle.COLOR_STYLE,FluidCloudStyle.SOLID);style.configure(options);drain();
        check(fade[0]==null&&flat[0]!=null,"solid selection removes the fading layer");
        style.release();check(fade[0]==null&&flat[0]==null,"release clears all non-media colour ownership");
    }
    /**
     * The lock screen's own page asks for the card look (its gradient or its custom look): the card takes the system's normal wallpaper glass and
     * loses the dark template base without the fluid-cloud style master, the media-accent master or "follow system notification".
     */
    private static void lockLookWithoutMasters() throws Exception {
        FluidCloudStyle style=new FluidCloudStyle();ClassLoader loader=FluidCloudLockscreenMaterialCheck.class.getClassLoader();
        style.resolveScene(loader);style.resolveNotification(loader);style.resolveMedia(loader);style.setNotificationStyler((view,following,keep)->{});
        Body body=new Body();body.actualHeight=120;body.mOnKeyguard=true;
        OplusCustomRow row=new OplusCustomRow();body.parent=row;CustomRowEntry entry=new CustomRowEntry();row.setEntry(entry);row.setMBackgroundNormal(body);
        NotificationBackgroundViewExtImp ext=(NotificationBackgroundViewExtImp)body.mExt;ViewBlurProxy proxy=ext.proxyForCheck;proxy.styleForCheck=style;
        Object nativeMix=proxy.blurConfig.platformMixConfig;
        GradientDrawable plate=new GradientDrawable();plate.setColor(0xff444444);plate.setCornerRadius(36);plate.setTint(0xff222222);ext.tempDrawable=plate;row.immersiveCustomDrawable=plate;
        android.view.ViewGroup mediaRoot=new android.view.ViewGroup(body.getContext());entry.immersiveCardView=mediaRoot;
        style.notificationRow(row);mode(style,ext,NotificationBlurMode.WALLPAPER_A);ext.viewBlurManager.mode=NotificationBlurMode.WALLPAPER_A;
        style.setKeyguard(true);
        FluidCloudAccent accent=new FluidCloudAccent();style.setAccentController(accent);
        com.oplus.systemui.plugins.shared.template.section.media.K0 mediaModel=new com.oplus.systemui.plugins.shared.template.section.media.K0(
                new kotlinx.coroutines.flow.p(new com.oplus.systemui.plugins.shared.data.media.model.a(0xffa34759)));
        com.oplus.systemui.plugins.shared.template.section.media.s1 mediaSection=new com.oplus.systemui.plugins.shared.template.section.media.s1(body.getContext(),mediaModel,mediaRoot,null);
        style.configure(new Bundle());drain();
        accent.configure(new Bundle());style.mediaChanged(mediaSection);style.notificationRow(row);drain();
        check(style.sceneDrawable(body,plate)==plate,"masters off and nothing asked: the card keeps its own native material");
        // the lock page's own gradient asks for the look
        Bundle options=new Bundle();options.putBoolean(FluidCloudAccent.LOCK_GRADIENT,true);
        accent.configure(options);style.configure(options);style.mediaChanged(mediaSection);style.notificationRow(row);drain();
        check(!accent.enabled(),"the media-accent master is still off");
        check(style.sceneDrawable(body,plate)==proxy.drawableForCheck,"the lock page's gradient obtains the native platform glass");
        check(proxy.appliedMixForCheck!=nativeMix&&((com.oplusos.systemui.common.blurability.BlurMixConfig)proxy.appliedMixForCheck).recipe==NotificationBlurMode.WALLPAPER_A,
                "with the same normal wallpaper glass as other lock cards, not the card's own dark recipe");
        nativePlateOmitted(style,ext,plate);
        // the custom look alone asks too; both off: native again
        options.putBoolean(FluidCloudAccent.LOCK_GRADIENT,false);options.putBoolean(FluidCloudAccent.LOCK_CUSTOM,true);
        accent.configure(options);style.configure(options);style.notificationRow(row);drain();
        check(style.sceneDrawable(body,plate)==proxy.drawableForCheck&&proxy.appliedMixForCheck!=nativeMix,"the custom look asks for the normal glass too");
        options.putBoolean(FluidCloudAccent.LOCK_CUSTOM,false);
        accent.configure(options);style.configure(options);style.notificationRow(row);drain();
        check(style.sceneDrawable(body,plate)==plate,"nothing asked again: the card's own material comes back");
        // off the lock screen the look is not applied
        options.putBoolean(FluidCloudAccent.LOCK_GRADIENT,true);accent.configure(options);style.configure(options);style.notificationRow(row);drain();
        check(style.sceneDrawable(body,plate)==proxy.drawableForCheck,"asked again: glass");
        style.setKeyguard(false);style.notificationRow(row);drain();
        check(style.sceneDrawable(body,plate)==plate,"unlocked: the lock look does not reach the card");
        style.release();
    }

    /** Only the AOD page or only the awake lock page owns the card. Native restoration ends the old composition proof. */
    private static void independentLockPages(boolean aodOnly,boolean custom,int backend)throws Exception {
        Handler.postedForCheck.clear();FluidCloudStyle style=new FluidCloudStyle();FluidCloudAccent accent=new FluidCloudAccent();
        style.setAccentController(accent);ClassLoader loader=FluidCloudLockscreenMaterialCheck.class.getClassLoader();
        style.resolveScene(loader);style.resolveNotification(loader);style.setNotificationStyler((view,following,keep)->{});
        android.content.res.Resources palette=new android.content.res.Resources(){
            @Override public int getIdentifier(String name,String type,String pkg){return name.equals("notification_material_background_color_compass")?7:0;}
            @Override public int getColor(int id,Theme theme){return 0xff667788;}
        };
        NotificationBackgroundView body=new NotificationBackgroundView(){
            @Override public int getWidth(){return 240;}
            @Override public android.content.res.Resources getResources(){return palette;}
        };
        body.actualHeight=120;body.mOnKeyguard=true;
        OplusCustomRow row=new OplusCustomRow();body.parent=row;CustomRowEntry entry=new CustomRowEntry();row.setEntry(entry);row.setMBackgroundNormal(body);
        NotificationBackgroundViewExtImp ext=(NotificationBackgroundViewExtImp)body.mExt;ViewBlurProxy proxy=ext.proxyForCheck;
        proxy.styleForCheck=style;ext.maskStyleForCheck=style;proxy.backendForCheck=backend;
        Object nativeMix=proxy.blurConfig.platformMixConfig,nativeMotion=proxy.blurConfig.motionBlurMixConfig;
        Drawable glass=backend==1?proxy.motionDrawableForCheck:backend==2?proxy.wallpaperDrawableForCheck:proxy.drawableForCheck;
        GradientDrawable plate=new GradientDrawable();plate.setColor(0xff222222);ext.tempDrawable=plate;row.immersiveCustomDrawable=plate;
        style.notificationRow(row);mode(style,ext,NotificationBlurMode.WALLPAPER_A);ext.viewBlurManager.mode=NotificationBlurMode.WALLPAPER_A;style.setKeyguard(true);
        Bundle options=new Bundle();String key=aodOnly?(custom?FluidCloudAccent.AOD_CUSTOM:FluidCloudAccent.AOD_GRADIENT)
                :(custom?FluidCloudAccent.LOCK_CUSTOM:FluidCloudAccent.LOCK_GRADIENT);
        options.putBoolean(key,true);accent.configure(options);style.configure(options);drain();
        String label=(aodOnly?"AOD-only":"lock-only")+(custom?" custom":" gradient")+" backend="+backend;
        check(!accent.enabled(),label+": neither master is needed, with no media colour to own");
        check(style.sceneDrawable(body,plate)==(aodOnly?plate:glass),label+": awake scene follows only its page");
        NotificationBackgroundCustomRowHandler helper=new NotificationBackgroundCustomRowHandler();MetaBallBlurDrawable meta=new MetaBallBlurDrawable();
        helper.updateDrawableMetaColor(body.getContext(),entry,meta);style.sceneMetaChanged(helper,body.getContext(),entry,meta);
        com.oplus.systemui.notification.aod.OplusPanoramicAodCoreStartable repo=
                (com.oplus.systemui.notification.aod.OplusPanoramicAodCoreStartable)ext.oplusPanoramicAodRepository;
        com.oplus.systemui.notification.aod.notification.OplusAodRowBackgroundDrawable mask=
                new com.oplus.systemui.notification.aod.notification.OplusAodRowBackgroundDrawable(body.getContext());
        mask.setColor(0x78000000);mask.setBounds(0,0,240,120);ext.setPanoramicAodDrawable(mask);
        StatusBarStateControllerImpl state=new StatusBarStateControllerImpl();state.state=1;
        for(int cycle=1;cycle<=3;cycle++) {
            state.dozing=true;style.onNativeState(state);repo.progressForCheck(.5f);style.sceneAodProgressChanged(repo,.5f);drain();
            repo.progressForCheck(1f);style.sceneAodProgressChanged(repo,1f);drain();
            check(style.sceneDrawable(body,plate)==(aodOnly?glass:plate),label+": sleep "+cycle+" follows its page after native restoration");
            if(aodOnly) {
                check(meta.metaColor==0&&!style.sceneNeedsMeta(entry,true),label+": AOD glass omits the opaque MetaBall base");
                nativePlateOmitted(style,ext,plate);
                check(ext.panoramicAodDrawable!=mask&&((android.graphics.drawable.ColorDrawable)ext.panoramicAodDrawable).getColor()==0x78667788,
                        label+": AOD replaces the black mask with neutral native glass");
            } else {
                check(meta.metaColor==helper.nativeColor&&ext.panoramicAodDrawable==mask,label+": AOD restores its native template and black mask");
            }
            state.dozing=false;style.onNativeState(state);drain();
            check(style.sceneDrawable(body,plate)==(aodOnly?plate:glass),label+": wake "+cycle+" restores its page before progress zero");
            repo.progressForCheck(0f);style.sceneAodProgressChanged(repo,0f);drain();
            check(style.sceneDrawable(body,plate)==(aodOnly?plate:glass),label+": awake progress does not change the scene decision");
            if(aodOnly)check(meta.metaColor==helper.nativeColor&&(backend==1?proxy.appliedMotionMixForCheck==nativeMotion:proxy.appliedMixForCheck==nativeMix),
                    label+": wake restores current OEM material and template colours");
        }
        options.putBoolean(key,false);accent.configure(options);style.configure(options);drain();
        check(style.sceneDrawable(body,plate)==plate&&meta.metaColor==helper.nativeColor,label+": both page switches off restore native awake card");
        state.dozing=true;style.onNativeState(state);repo.progressForCheck(1f);style.sceneAodProgressChanged(repo,1f);drain();
        check(style.sceneDrawable(body,plate)==plate&&ext.panoramicAodDrawable==mask,label+": both page switches off leave native AOD card and mask");
        check(mask.getColor()==0x78000000,label+": shared OEM black mask remains unchanged");
        style.release();accent.release();drain();
    }

    public static void main(String[] args)throws Exception {
        nonMediaGradient();
        lockLookWithoutMasters();
        for(int backend=0;backend<=2;backend++)for(boolean custom:new boolean[]{false,true}) {
            independentLockPages(true,custom,backend);independentLockPages(false,custom,backend);
        }
        Handler.postedForCheck.clear();FluidCloudStyle style=new FluidCloudStyle();ClassLoader loader=FluidCloudLockscreenMaterialCheck.class.getClassLoader();
        style.resolveScene(loader);style.resolveNotification(loader);style.setNotificationStyler((view,following,keep)->{});
        Body body=new Body();body.actualHeight=120;body.mOnKeyguard=true;
        OplusCustomRow row=new OplusCustomRow();body.parent=row;CustomRowEntry entry=new CustomRowEntry();row.setEntry(entry);row.setMBackgroundNormal(body);
        NotificationBackgroundViewExtImp ext=(NotificationBackgroundViewExtImp)body.mExt;ViewBlurProxy proxy=ext.proxyForCheck;proxy.styleForCheck=style;
        Object nativeMix=proxy.blurConfig.platformMixConfig,nativeStroke=proxy.blurConfig.gradientStrokeLineParam;
        GradientDrawable plate=new GradientDrawable();plate.setColor(0xff444444);plate.setCornerRadius(36);plate.setTint(0xff222222);ext.tempDrawable=plate;row.immersiveCustomDrawable=plate;
        style.notificationRow(row);mode(style,ext,NotificationBlurMode.WALLPAPER_A);ext.viewBlurManager.mode=NotificationBlurMode.WALLPAPER_A;
        int offUpdates=ext.blurUpdates,offInvalidations=body.invalidations;
        style.sceneMaterialChanged(ext);drain();
        check(ext.blurUpdates==offUpdates&&body.invalidations==offInvalidations,"all-OFF observation does not rewrite native mode or invalidate carrier/panel");
        style.setKeyguard(true);
        Bundle options=new Bundle();options.putBoolean(FluidCloudStyle.MASTER,true);options.putBoolean(FluidCloudStyle.FOLLOW_NOTIFICATION,true);style.configure(options);drain();
        check(style.sceneDrawable(body,plate)==proxy.drawableForCheck,"lockscreen obtains complete native platform backdrop");
        check(((com.oplusos.systemui.common.blurability.BlurMixConfig)proxy.appliedMixForCheck).recipe==NotificationBlurMode.WALLPAPER_A,"lockscreen uses real WALLPAPER_A, never SHADE mode");
        check(((com.oplus.posteffect.params.GradientStrokeLineParams)proxy.appliedStrokeForCheck).mode==NotificationBlurMode.WALLPAPER_A,"native keyguard stroke and shadow recipe retained");
        check(proxy.blurConfig.platformMixConfig==nativeMix&&proxy.blurConfig.gradientStrokeLineParam==nativeStroke,"temporary NOTIFICATION recipe does not overwrite raw OEM fields");
        NotificationBackgroundCustomRowHandler helper=new NotificationBackgroundCustomRowHandler();MetaBallBlurDrawable meta=new MetaBallBlurDrawable();
        helper.updateDrawableMetaColor(body.getContext(),entry,meta);style.sceneMetaChanged(helper,body.getContext(),entry,meta);
        check(meta.metaColor==0&&!style.sceneNeedsMeta(entry,true),"proven blurred lockscreen backdrop removes redundant opaque template plate");
        boolean frame=style.beginSceneFrame();try(AutoCloseable scope=style.beginSceneDraw(ext)){check(ext.tempDrawable==null,"non-media lockscreen draw omits only its own extra opaque base");}finally{style.endSceneFrame(frame);}
        check(ext.tempDrawable==plate,"owned draw scope restores native temp field");
        java.lang.reflect.Field sceneField=FluidCloudStyle.class.getDeclaredField("sceneMaterial");sceneField.setAccessible(true);
        FluidCloudSceneMaterial scene=(FluidCloudSceneMaterial)sceneField.get(style);
        com.oplus.systemui.notification.aod.OplusPanoramicAodCoreStartable aod=
                (com.oplus.systemui.notification.aod.OplusPanoramicAodCoreStartable)ext.oplusPanoramicAodRepository;
        FluidCloudAccent accent=new FluidCloudAccent();style.setAccentController(accent);style.resolveMedia(loader);
        android.view.ViewGroup mediaRoot=new android.view.ViewGroup(body.getContext());entry.immersiveCardView=mediaRoot;
        com.oplus.systemui.plugins.shared.template.section.media.K0 mediaModel=new com.oplus.systemui.plugins.shared.template.section.media.K0(
                new kotlinx.coroutines.flow.p(new com.oplus.systemui.plugins.shared.data.media.model.a(0xffa34759)));
        com.oplus.systemui.plugins.shared.template.section.media.s1 mediaSection=new com.oplus.systemui.plugins.shared.template.section.media.s1(body.getContext(),mediaModel,mediaRoot,null);
        Bundle accentOptions=new Bundle();accentOptions.putBoolean(FluidCloudAccent.ENABLED,true);accentOptions.putBoolean(FluidCloudAccent.LOCKSCREEN_ENABLED,true);accentOptions.putBoolean(FluidCloudAccent.LOCK_GRADIENT,true);accentOptions.putBoolean(FluidCloudAccent.AOD_GRADIENT,true);
        accent.configure(accentOptions);style.mediaChanged(mediaSection);style.notificationRow(row);drain();
        check(meta.gradientColors==null&&meta.metaColor==0,"proven music glass removes a duplicate native MetaBall tint");
        check(!style.sceneNeedsMeta(entry,true),"one final gradient layer owns colour above proven glass");
        check(style.sceneDrawable(body,plate)==proxy.drawableForCheck,"music accent does not prevent actual native lockscreen backdrop");
        check(proxy.appliedMixForCheck!=nativeMix&&((com.oplusos.systemui.common.blurability.BlurMixConfig)proxy.appliedMixForCheck).recipe==NotificationBlurMode.WALLPAPER_A,
                "music uses the same normal native wallpaper glass as other lock cards");
        plate.setAlpha(51);nativePlateOmitted(style,ext,plate);
        check(plate.getColor().getDefaultColor()==0xff444444&&plate.tintForCheck.getDefaultColor()==0xff222222,"private gradient leaves original fill and tint unchanged");
        for(int alpha:new int[]{0,51,127,201,255}){plate.setAlpha(alpha);nativePlateOmitted(style,ext,plate);}
        GradientDrawable unrelated=new GradientDrawable();unrelated.setColor(0xff778899);ext.tempDrawable=unrelated;
        try(AutoCloseable scope=style.beginSceneDraw(ext)){check(ext.tempDrawable==unrelated,"unknown temp shape is never recolored instead of exact immersive owner");}
        ext.tempDrawable=plate;
        int submissions=proxy.platformAppliesForCheck;
        int aodReads=aod.readsForCheck;
        for(int i=0;i<10000;i++) {
            frame=style.beginSceneFrame();try{style.sceneDrawable(body,plate);proxy.applyConfigToPlatformBlur();style.sceneMaterialChanged(ext);try(AutoCloseable scope=style.beginSceneDraw(ext)){check(ext.tempDrawable==null,"stable frames omit the duplicate plate without allocations");}}finally{style.endSceneFrame(frame);}
        }
        drain();check(proxy.platformAppliesForCheck==submissions&&aod.readsForCheck==aodReads,"lockscreen music stable drawing does not recompute, poll AOD or replace native backdrop");
        aod.progressForCheck(0.01f);style.sceneAodProgressChanged(aod,0.01f);drain();
        check(style.sceneDrawable(body,plate)==proxy.drawableForCheck&&meta.metaColor==0,
                "panoramic animation before isDozing retains proven music glass and one colour layer");
        submissions=proxy.platformAppliesForCheck;
        for(int i=0;i<10000;i++){float progress=0.1f+(i%100)*0.008f;aod.progressForCheck(progress);style.sceneAodProgressChanged(aod,progress);}
        check(Handler.postedForCheck.isEmpty()&&proxy.platformAppliesForCheck==submissions,"intermediate AOD progress never reschedules unchanged native eligibility");
        aod.progressForCheck(Float.NaN);style.sceneAodProgressChanged(aod,Float.NaN);drain();
        check(style.sceneDrawable(body,plate)==plate,"unknown progress keeps native fallback");
        aod.progressForCheck(0f);style.sceneAodProgressChanged(aod,0f);drain();
        check(style.sceneDrawable(body,plate)==proxy.drawableForCheck&&meta.metaColor==0,"wake progress zero retains music glass without waiting for another card");
        nativePlateOmitted(style,ext,plate);
        StatusBarStateControllerImpl state=new StatusBarStateControllerImpl();state.state=1;state.dozing=true;style.onNativeState(state);drain();
        check(style.sceneDrawable(body,plate)==proxy.drawableForCheck&&meta.metaColor==0&&meta.gradientColors==null,
                "AOD retains the same proven music glass without an extra colour plate");
        check(proxy.blurConfig.platformMixConfig==nativeMix,"AOD retains the immutable raw native config");
        frame=style.beginSceneFrame();try{check(style.deferScenePlatform(proxy),"AOD native submit cannot replace already-proven music glass");}finally{style.endSceneFrame(frame);}
        state.dozing=false;style.onNativeState(state);drain();
        check(style.sceneDrawable(body,plate)==proxy.drawableForCheck&&meta.metaColor==0,"wake retains music glass and single colour ownership");
        nativePlateOmitted(style,ext,plate);
        accent.configure(new Bundle());drain();mode(style,ext,NotificationBlurMode.WALLPAPER_B);ext.viewBlurManager.mode=NotificationBlurMode.WALLPAPER_B;drain();
        check(((com.oplusos.systemui.common.blurability.BlurMixConfig)proxy.appliedMixForCheck).recipe==NotificationBlurMode.WALLPAPER_B,"real wallpaper region change refreshes B palette");
        proxy.nativeReturnsFallbackForCheck=true;body.actualHeight=121;style.sceneGeometryChanged(body);drain();
        check(style.sceneDrawable(body,plate)==plate&&meta.metaColor==helper.nativeColor&&style.sceneNeedsMeta(entry,true),"normal return without active blur restores opaque native fallback");
        proxy.nativeReturnsFallbackForCheck=false;body.actualHeight=122;style.sceneGeometryChanged(body);drain();
        check(style.sceneDrawable(body,plate)==proxy.drawableForCheck,"real geometry event can recover missing lockscreen backdrop");
        com.oplus.systemui.notification.blur.NotificationPlatFormBlurParamsManager params=
                com.oplus.systemui.notification.blur.NotificationPlatFormBlurParamsManager.INSTANCE;
        try {
            params.noStrokeForCheck=true;params.noShadowForCheck=true;body.actualHeight=123;style.sceneGeometryChanged(body);drain();
            check(style.sceneDrawable(body,plate)==proxy.drawableForCheck&&proxy.appliedStrokeForCheck==null&&proxy.appliedShadowForCheck==null,
                    "optional native stroke/shadow absence does not reject valid optical backdrop");
        }finally{params.noStrokeForCheck=false;params.noShadowForCheck=false;}
        proxy.backendForCheck=2;body.actualHeight++;style.sceneGeometryChanged(body);drain();
        check(style.sceneDrawable(body,plate)==proxy.wallpaperDrawableForCheck&&!style.sceneNeedsMeta(entry,true),
                "actual BlendWallpaper backend uses native wallpaper glass and omits redundant gray base");
        check(proxy.wallpaperDrawableForCheck.currentBlurMixConfig==proxy.appliedMixForCheck
                &&((com.oplusos.systemui.common.blurability.BlurMixConfig)proxy.appliedMixForCheck).recipe==NotificationBlurMode.WALLPAPER_B,
                "BlendWallpaper submission proves its exact host and native WALLPAPER palette");
        check(proxy.blurConfig.platformMixConfig==nativeMix,"wallpaper scope restores immutable raw native config");
        accent.configure(accentOptions);style.mediaChanged(mediaSection);drain();
        check(meta.metaColor==0&&meta.gradientColors==null&&style.sceneDrawable(body,plate)==proxy.wallpaperDrawableForCheck,
                "one final media gradient is composed over actual normal BlendWallpaper glass");
        nativePlateOmitted(style,ext,plate);
        ext.viewBlurManager.shaderForCheck=true;body.actualHeight++;style.sceneGeometryChanged(body);drain();
        check(proxy.appliedMixForCheck instanceof com.oplusos.systemui.common.blurability.BlurMixConfig.BlurMixMultiWithShader,
                "music retains the normal native factory's shader glass instead of the dark custom mix");
        nativePlateOmitted(style,ext,plate);
        submissions=proxy.wallpaperAppliesForCheck;aodReads=aod.readsForCheck;
        for(int i=0;i<10000;i++) {
            frame=style.beginSceneFrame();try{style.sceneDrawable(body,plate);proxy.applyConfigToBlendWallpaper();style.sceneMaterialChanged(ext);}finally{style.endSceneFrame(frame);}
        }
        drain();check(proxy.wallpaperAppliesForCheck==submissions&&aod.readsForCheck==aodReads&&Handler.postedForCheck.isEmpty(),"10000 BlendWallpaper frames do not resubmit, poll AOD or queue another frame");
        proxy.nativeReturnsFallbackForCheck=true;body.actualHeight++;style.sceneGeometryChanged(body);drain();
        check(style.sceneDrawable(body,plate)==plate&&meta.gradientColors!=null&&meta.gradientColors[0]==0xffa34759&&style.sceneNeedsMeta(entry,true),"failed compositor proof keeps independent media fallback and never clears it");
        GradientDrawable visible=spatial(style,ext,plate,0xffa34759);
        check(spatial(style,ext,plate,0xffa34759)==visible,"unproven backend reuses the native-alpha fallback gradient");
        proxy.nativeReturnsFallbackForCheck=false;body.actualHeight++;style.sceneGeometryChanged(body);drain();
        check(style.sceneDrawable(body,plate)==proxy.wallpaperDrawableForCheck&&meta.metaColor==0&&meta.gradientColors==null,"successful recovery retires the fallback tint at the proven material event");
        nativePlateOmitted(style,ext,plate);
        accent.configure(new Bundle());drain();proxy.wallpaperSkipsApplyForCheck=true;body.actualHeight++;style.sceneGeometryChanged(body);drain();
        check(style.sceneDrawable(body,plate)==plate&&meta.metaColor==helper.nativeColor,
                "lazily created wallpaper drawable with stale/unsubmitted mix cannot erase native MetaBall");
        proxy.wallpaperSkipsApplyForCheck=false;body.actualHeight++;style.sceneGeometryChanged(body);drain();
        check(style.sceneDrawable(body,plate)==proxy.wallpaperDrawableForCheck,"fresh native wallpaper submission recovers actual glass");
        aod.progressForCheck(0.01f);style.sceneAodProgressChanged(aod,0.01f);drain();
        check(style.sceneDrawable(body,plate)==proxy.wallpaperDrawableForCheck&&proxy.wallpaperDrawableForCheck.currentBlurMixConfig==proxy.appliedMixForCheck
                &&meta.metaColor==0,"plain following preserves proven wallpaper glass across AOD crossfade instead of swapping to CUSTOM color");
        aod.progressForCheck(0f);style.sceneAodProgressChanged(aod,0f);drain();
        options.putBoolean(FluidCloudStyle.FOLLOW_NOTIFICATION,false);style.configure(options);drain();
        check(style.sceneDrawable(body,plate)==plate&&proxy.appliedMixForCheck==nativeMix&&meta.metaColor==helper.nativeColor,"disable restores current OEM platform and latest template colors");
        style.release();drain();
        directLock();
        tintOverlay();
        System.out.println("FluidCloudLockscreenMaterialCheck: "+checks+" checks passed");
    }

    /** code99: a fill the card wants is drawn over the card as one flat colour, whatever the platform material did with it: awake or
     * asleep, platform ready or not, in the crossfade or not. The answer is only about the card's own wish. */
    private static void tintOverlay()throws Exception {
        Handler.postedForCheck.clear();FluidCloudStyle style=new FluidCloudStyle();ClassLoader loader=FluidCloudLockscreenMaterialCheck.class.getClassLoader();
        style.resolveScene(loader);style.resolveNotification(loader);style.setNotificationStyler((view,following,keep)->{});
        Body body=new Body();body.actualHeight=120;body.mOnKeyguard=true;
        OplusCustomRow row=new OplusCustomRow();body.parent=row;CustomRowEntry entry=new CustomRowEntry();row.setEntry(entry);row.setMBackgroundNormal(body);
        NotificationBackgroundViewExtImp ext=(NotificationBackgroundViewExtImp)body.mExt;ViewBlurProxy proxy=ext.proxyForCheck;proxy.styleForCheck=style;
        GradientDrawable plate=new GradientDrawable();plate.setColor(0xff444444);plate.setCornerRadius(36);ext.tempDrawable=plate;row.immersiveCustomDrawable=plate;
        style.notificationRow(row);mode(style,ext,NotificationBlurMode.WALLPAPER_A);ext.viewBlurManager.mode=NotificationBlurMode.WALLPAPER_A;
        style.setKeyguard(true);
        StatusBarStateControllerImpl state=new StatusBarStateControllerImpl();state.state=1;state.dozing=false;style.onNativeState(state);drain();
        Bundle options=new Bundle();options.putBoolean(FluidCloudStyle.MASTER,true);options.putBoolean(FluidCloudStyle.FOLLOW_NOTIFICATION,true);style.configure(options);drain();
        java.lang.reflect.Field sceneField=FluidCloudStyle.class.getDeclaredField("sceneMaterial");sceneField.setAccessible(true);
        FluidCloudSceneMaterial scene=(FluidCloudSceneMaterial)sceneField.get(style);
        com.oplus.systemui.notification.aod.OplusPanoramicAodCoreStartable aod=
                (com.oplus.systemui.notification.aod.OplusPanoramicAodCoreStartable)ext.oplusPanoramicAodRepository;
        check(style.sceneDrawable(body,plate)==proxy.drawableForCheck,"the platform has the card");
        check(!scene.tintOverlay(body),"no fill wanted, nothing to draw over the card");
        scene.fill(body,0x80aa5533,1f);
        check(scene.tintOverlay(body),"a fill wanted is drawn over the card even when the platform has taken it too");
        check(!scene.tintOverlay(null),"no card is no answer");
        // The platform does not take the colour: the overlay does not depend on it.
        proxy.nativeReturnsFallbackForCheck=true;body.actualHeight++;style.sceneGeometryChanged(body);drain();scene.fill(body,0x80aa5533,1f);
        check(style.sceneDrawable(body,plate)==plate&&scene.tintOverlay(body),"the platform did not take the card: the overlay is there all the same");
        // The OEM crossfade and the doze: the overlay is part of the body's own draw, so it fades with the body and never pops.
        aod.progressForCheck(0.3f);style.sceneAodProgressChanged(aod,0.3f);drain();scene.fill(body,0x80aa5533,1f);
        check(scene.tintOverlay(body),"during the AOD crossfade it stays, fading with the body");
        state.dozing=true;style.onNativeState(state);drain();scene.fill(body,0x80aa5533,1f);
        check(scene.tintOverlay(body),"asleep it is still the card's wish");
        state.dozing=false;style.onNativeState(state);drain();
        aod.progressForCheck(0f);style.sceneAodProgressChanged(aod,0f);drain();scene.fill(body,0x80aa5533,1f);
        check(scene.tintOverlay(body),"awake again");
        scene.fill(body,null,1f);
        check(!scene.tintOverlay(body),"the wish taken back");
        scene.accent(body,0xffa34759);
        check(scene.tintOverlay(body),"the music card's accent is a wish too: its own gradient is not always visible");
        scene.accent(body,null);
        check(!scene.tintOverlay(body),"and taken back");
        scene.fill(body,0x80aa5533,1f);
        check(scene.tintOverlay(body),"and made again");
        style.release();drain();
        check(!scene.tintOverlay(body),"released: never");
    }

    /** A direct lock from the desktop: AOD is the first thing the lock screen shows, with no awake composition to hold.
     * The card takes the glass at once, and waking the screen changes nothing (no colour jump). */
    private static void directLock()throws Exception {
        Handler.postedForCheck.clear();FluidCloudStyle style=new FluidCloudStyle();ClassLoader loader=FluidCloudLockscreenMaterialCheck.class.getClassLoader();
        style.resolveScene(loader);style.resolveNotification(loader);style.setNotificationStyler((view,following,keep)->{});
        Body body=new Body();body.actualHeight=120;body.mOnKeyguard=true;
        OplusCustomRow row=new OplusCustomRow();body.parent=row;CustomRowEntry entry=new CustomRowEntry();row.setEntry(entry);row.setMBackgroundNormal(body);
        NotificationBackgroundViewExtImp ext=(NotificationBackgroundViewExtImp)body.mExt;ViewBlurProxy proxy=ext.proxyForCheck;proxy.styleForCheck=style;
        GradientDrawable plate=new GradientDrawable();plate.setColor(0xff444444);plate.setCornerRadius(36);ext.tempDrawable=plate;row.immersiveCustomDrawable=plate;
        style.notificationRow(row);mode(style,ext,NotificationBlurMode.WALLPAPER_A);ext.viewBlurManager.mode=NotificationBlurMode.WALLPAPER_A;
        style.setKeyguard(true);
        StatusBarStateControllerImpl state=new StatusBarStateControllerImpl();state.state=1;state.dozing=true;style.onNativeState(state);drain();
        Bundle options=new Bundle();options.putBoolean(FluidCloudStyle.MASTER,true);options.putBoolean(FluidCloudStyle.FOLLOW_NOTIFICATION,true);style.configure(options);drain();
        android.graphics.drawable.Drawable asleep=style.sceneDrawable(body,plate);
        check(asleep==proxy.drawableForCheck,"direct lock: the first AOD frame already shows the follow-notification glass, not the native plate");
        state.dozing=false;style.onNativeState(state);drain();
        check(style.sceneDrawable(body,plate)==asleep,"direct lock: waking the screen keeps the very same glass");
        state.dozing=true;style.onNativeState(state);drain();
        check(style.sceneDrawable(body,plate)==asleep,"direct lock: sleeping again holds it");
        state.state=0;state.dozing=false;style.onNativeState(state);style.setKeyguard(false);drain();
        state.state=1;state.dozing=true;style.setKeyguard(true);style.onNativeState(state);drain();
        check(style.sceneDrawable(body,plate)==proxy.drawableForCheck,"a second direct lock after unlocking behaves the same");
        style.release();drain();
    }
}
