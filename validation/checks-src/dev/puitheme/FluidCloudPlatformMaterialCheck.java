package dev.puitheme;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import com.android.systemui.plugins.custom.CustomRowEntry;
import com.android.systemui.statusbar.notification.row.NotificationBackgroundView;
import com.oplus.systemui.statusbar.notification.customcard.OplusCustomRow;
import com.oplus.systemui.statusbar.notification.row.NotificationBackgroundViewExtImp;
import com.oplus.systemui.notification.blur.ViewBlurManager;
import com.oplus.systemui.notification.row.material.NotificationBlurMode;
import com.oplusos.systemui.common.blurability.BlurConfig;
import com.oplusos.systemui.common.blurability.BlurMixConfig;
import com.oplusos.systemui.common.blurability.ViewBlurProxy;

/** Exact native material factory, double-background removal and bounded refresh. */
public final class FluidCloudPlatformMaterialCheck {
    private static int checks;
    private static final class Body extends NotificationBackgroundView {int width=240;@Override public int getWidth(){return width;}}
    private static void check(boolean okay,String why){checks++;if(!okay)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception {
        FluidCloudStyle style=new FluidCloudStyle();style.resolveScene(FluidCloudPlatformMaterialCheck.class.getClassLoader());style.resolveNotification(FluidCloudPlatformMaterialCheck.class.getClassLoader());
        Body body=new Body();OplusCustomRow row=new OplusCustomRow();body.parent=row;
        row.setMBackgroundNormal(body);CustomRowEntry entry=new CustomRowEntry();row.setEntry(entry);
        body.width=240;body.actualHeight=90;
        NotificationBackgroundViewExtImp ext=(NotificationBackgroundViewExtImp)body.mExt;
        ViewBlurProxy proxy=ext.proxyForCheck;proxy.styleForCheck=style;
        BlurConfig config=proxy.blurConfig;Object originalMix=config.platformMixConfig,originalStroke=config.gradientStrokeLineParam,originalShadow=config.innerShadowParams;
        GradientDrawable original=new GradientDrawable();original.setColor(0xff555555);ext.tempDrawable=original;
        style.notificationRow(row);Handler.drainForCheck();
        check(style.sceneDrawable(body,original)==original,"OFF keeps native complete custom card");
        Bundle settings=new Bundle();settings.putBoolean(FluidCloudStyle.MASTER,true);settings.putBoolean(FluidCloudStyle.FOLLOW_NOTIFICATION,true);style.configure(settings);Handler.drainForCheck();
        Drawable target=style.sceneDrawable(body,original);
        check(target==proxy.drawableForCheck,"first custom row works without any ordinary donor by native global material factory");
        check(proxy.appliedMixForCheck!=originalMix&&((com.oplusos.systemui.common.blurability.BlurMixConfig)proxy.appliedMixForCheck).recipe==NotificationBlurMode.SHADE_LIGHT,"NOTIFICATION palette is applied rather than CUSTOM_CARD opaque mix");
        com.oplus.posteffect.params.GradientStrokeLineParams stroke=(com.oplus.posteffect.params.GradientStrokeLineParams)proxy.appliedStrokeForCheck;
        check(stroke.width==240&&stroke.height==90&&stroke.mode==NotificationBlurMode.SHADE_LIGHT,"native outline derives from this target size");
        check(config.platformMixConfig==originalMix&&config.gradientStrokeLineParam==originalStroke&&config.innerShadowParams==originalShadow,"all temporary fields restored after native submit");
        check(config.getCornerRadius()==63f,"target own rounded path retained");
        check(!style.sceneNeedsMeta(entry,true)&&style.sceneNeedsMeta(new CustomRowEntry(),true),"only the proven followed entry loses redundant plate");
        try(AutoCloseable scope=style.beginSceneDraw(ext)){check(ext.tempDrawable==null,"extra opaque template plate omitted during followed body draw");}
        check(ext.tempDrawable==original,"template plate restored outside owned draw");
        // The first OEM palette bind is an actual native event, not a stable
        // render frame. Let it settle before asserting the subsequent budget.
        style.sceneMaterialChanged(ext);Handler.drainForCheck();
        int applies=proxy.platformAppliesForCheck,mixes=ext.viewBlurManager.mixes;
        for(int i=0;i<1000;i++)check(style.sceneDrawable(body,original)==target,"stable own drawable reused");
        check(proxy.platformAppliesForCheck==applies&&ext.viewBlurManager.mixes==mixes,"stable draws do not submit or allocate new material recipes");
        body.actualHeight=120;style.sceneDrawable(body,original);
        check(proxy.platformAppliesForCheck==applies+1&&((com.oplus.posteffect.params.InnerShadowParams)proxy.appliedShadowForCheck).height==120,"height changes regenerate size-specific native shadow once");
        proxy.nativeReturnsFallbackForCheck=true;body.actualHeight=121;
        check(style.sceneDrawable(body,original)==original&&style.sceneNeedsMeta(entry,true),"normal void return with disabled/non-platform path keeps complete native plate");
        try(AutoCloseable scope=style.beginSceneDraw(ext)){check(ext.tempDrawable==original,"non-platform result never erases template background");}
        check(config.platformMixConfig==originalMix&&config.gradientStrokeLineParam==originalStroke&&config.innerShadowParams==originalShadow,"non-platform normal return restores all temporary fields");
        proxy.nativeReturnsFallbackForCheck=false;proxy.drawableForCheck.blurDrawable=null;body.actualHeight=122;
        check(style.sceneDrawable(body,original)==original&&style.sceneNeedsMeta(entry,true),"normal void return after platform creation failure keeps native Meta and drawing");
        try(AutoCloseable scope=style.beginSceneDraw(ext)){check(ext.tempDrawable==original,"empty lower platform drawable keeps template plate");}
        proxy.drawableForCheck.blurDrawable=new com.oplus.posteffect.BlurDrawable();style.sceneDrawable(body,original);
        check(!style.sceneNeedsMeta(entry,true),"valid platform recovery reacquires complete material");
        com.oplus.systemui.notification.row.NotificationBackgroundCustomRowHandler nativeHelper=new com.oplus.systemui.notification.row.NotificationBackgroundCustomRowHandler();
        com.oplus.posteffect.drawable.MetaBallBlurDrawable nativeMeta=new com.oplus.posteffect.drawable.MetaBallBlurDrawable();
        nativeHelper.updateDrawableMetaColor(body.getContext(),entry,nativeMeta);
        style.sceneMetaChanged(nativeHelper,body.getContext(),entry,nativeMeta);
        check(nativeMeta.metaColor==0,"proven platform owns material before failure");
        proxy.failDrawableForCheck=true;
        check(style.sceneDrawable(body,original)==original&&style.sceneNeedsMeta(entry,true),"throwing getter revokes previous platform readiness");
        check(nativeMeta.metaColor==nativeHelper.nativeColor,"throwing getter restores latest native MetaBall colour");
        try(AutoCloseable scope=style.beginSceneDraw(ext)){check(ext.tempDrawable==original,"throwing getter keeps template plate");}
        proxy.failDrawableForCheck=false;style.sceneDrawable(body,original);
        check(!style.sceneNeedsMeta(entry,true)&&nativeMeta.metaColor==0,"getter recovery reacquires material without stale failure");
        ext.proxyForCheck=null;
        check(style.sceneDrawable(body,original)==original&&style.sceneNeedsMeta(entry,true),"missing proxy revokes old readiness");
        check(nativeMeta.metaColor==nativeHelper.nativeColor,"missing proxy restores latest template colour");
        ext.proxyForCheck=proxy;style.sceneDrawable(body,original);
        ViewBlurProxy foreign=new ViewBlurProxy();foreign.view=new android.view.View(new android.content.Context());foreign.styleForCheck=style;
        check(style.beginScenePlatform(foreign)==null,"unknown owner never gets notification recipe");
        body.width=0;style.sceneDrawable(body,original);check(config.platformMixConfig==originalMix,"zero-width leaves config intact");body.width=240;
        body.actualHeight=130;proxy.failApplyCalls=1;style.sceneDrawable(body,original);
        check(config.platformMixConfig==originalMix&&config.gradientStrokeLineParam==originalStroke&&config.innerShadowParams==originalShadow,"native exception also restores temporary field ownership");
        check(style.sceneNeedsMeta(entry,true),"native exception revokes previous platform success before next draw");
        style.sceneDrawable(body,original);check(proxy.appliedMixForCheck!=originalMix,"failed submission can recover on next native draw");
        style.setKeyguard(true);Handler.drainForCheck();
        check(style.sceneDrawable(body,original)==original&&style.sceneNeedsMeta(entry,true),"lockscreen widget-native material and independent accent stay intact");
        style.sceneProxyChanged(proxy);check(proxy.appliedMixForCheck==originalMix,"scene transition restores actual native platform recipe");
        style.setKeyguard(false);Handler.drainForCheck();style.sceneDrawable(body,original);
        settings.putBoolean(FluidCloudStyle.FOLLOW_NOTIFICATION,false);style.configure(settings);Handler.drainForCheck();style.sceneProxyChanged(proxy);
        check(proxy.appliedMixForCheck==originalMix&&style.sceneDrawable(body,original)==original,"switch OFF restores own native material without donor state leaks");
        settings.putBoolean(FluidCloudStyle.FOLLOW_NOTIFICATION,true);style.configure(settings);Handler.drainForCheck();style.sceneDrawable(body,original);
        style.detach(body);check(proxy.appliedMixForCheck==originalMix,"detach restores submitted platform state");
        style.release();Handler.drainForCheck();
        motionHeadsUp();
        fillOnly(false);fillOnly(true);shaderFollowing();
        System.out.println("FluidCloudPlatformMaterialCheck: "+checks+" checks passed");
    }
    private static void fillOnly(boolean shader)throws Exception {
        Handler.postedForCheck.clear();FluidCloudStyle style=new FluidCloudStyle();ClassLoader loader=FluidCloudPlatformMaterialCheck.class.getClassLoader();
        style.resolveScene(loader);style.resolveNotification(loader);style.setNotificationStyler((body,enabled,keep)->{});
        Body body=new Body();body.actualHeight=90;OplusCustomRow row=new OplusCustomRow();body.parent=row;row.setMBackgroundNormal(body);
        CustomRowEntry entry=new CustomRowEntry();row.setEntry(entry);NotificationBackgroundViewExtImp ext=(NotificationBackgroundViewExtImp)body.mExt;
        ViewBlurProxy proxy=ext.proxyForCheck;proxy.styleForCheck=style;
        if(shader)proxy.blurConfig.platformMixConfig=new BlurMixConfig.BlurMixMultiWithShader("native-custom");
        BlurMixConfig raw=proxy.blurConfig.platformMixConfig;raw.setAlphaWithBlurAmount(true);raw.setMirrorScale(.83f);raw.setPlaceHolderColor(0x60403020);proxy.applyBlurConfig();
        GradientDrawable plate=new GradientDrawable();plate.setColor(0xff444444);ext.tempDrawable=plate;style.notificationRow(row);
        com.oplus.systemui.notification.row.NotificationBackgroundCustomRowHandler helper=new com.oplus.systemui.notification.row.NotificationBackgroundCustomRowHandler();
        com.oplus.posteffect.drawable.MetaBallBlurDrawable meta=new com.oplus.posteffect.drawable.MetaBallBlurDrawable();
        helper.updateDrawableMetaColor(body.getContext(),entry,meta);style.sceneMetaChanged(helper,body.getContext(),entry,meta);
        Bundle options=new Bundle();options.putBoolean(FluidCloudStyle.MASTER,true);options.putBoolean(FluidCloudStyle.SCOPE_SHADE,false);options.putBoolean(FluidCloudStyle.DESKTOP_BACKGROUND,true);
        options.putString(FluidCloudStyle.BACKGROUND_MODE,"native");options.putFloat(FluidCloudStyle.BACKGROUND_OPACITY,50f);style.configure(options);Handler.drainForCheck();
        check(proxy.appliedMixForCheck==raw,"floating opacity leaves native notification mix unchanged, shader="+shader);
        check(meta.metaColor==helper.nativeColor&&style.sceneNeedsMeta(entry,true),"appearance alone keeps notification native MetaBall");
        try(AutoCloseable scope=style.beginSceneDraw(ext)){check(ext.tempDrawable==plate,"appearance alone keeps readable notification plate");}
        check(style.sceneDrawable(body,plate)==plate,"floating fill never adopts notification compositor");
        for(String source:new String[]{"custom","notification","native"}) {
            options.putString(FluidCloudStyle.BACKGROUND_MODE,source);options.putInt(FluidCloudStyle.BACKGROUND_COLOR,0xffa04020);
            options.putFloat(FluidCloudStyle.BACKGROUND_OPACITY,0f);options.putBoolean(FluidCloudStyle.FOLLOW_MEDIA,true);
            style.configure(options);Handler.drainForCheck();
            check(proxy.appliedMixForCheck==raw&&meta.metaColor==helper.nativeColor&&style.sceneDrawable(body,plate)==plate,"all floating sources and independent media fill isolated from shade, source="+source);
        }
        style.setKeyguard(true);Handler.drainForCheck();
        check(proxy.appliedMixForCheck==raw&&meta.metaColor==helper.nativeColor,"floating fill isolated from lockscreen too");
        style.release();Handler.drainForCheck();
    }

    private static void shaderFollowing()throws Exception {
        Handler.postedForCheck.clear();FluidCloudStyle style=new FluidCloudStyle();ClassLoader loader=FluidCloudPlatformMaterialCheck.class.getClassLoader();
        style.resolveScene(loader);style.resolveNotification(loader);style.setNotificationStyler((body,enabled,keep)->{});
        Body body=new Body();body.actualHeight=90;OplusCustomRow row=new OplusCustomRow();body.parent=row;row.setMBackgroundNormal(body);row.setEntry(new CustomRowEntry());
        NotificationBackgroundViewExtImp ext=(NotificationBackgroundViewExtImp)body.mExt;ext.viewBlurManager.shaderForCheck=true;ext.viewBlurManager.motionForCheck=true;
        ViewBlurProxy proxy=ext.proxyForCheck;proxy.styleForCheck=style;style.notificationRow(row);
        Bundle options=new Bundle();options.putBoolean(FluidCloudStyle.MASTER,true);options.putBoolean(FluidCloudStyle.SCOPE_SHADE,false);options.putBoolean(FluidCloudStyle.FOLLOW_NOTIFICATION,true);
        options.putBoolean(FluidCloudStyle.DESKTOP_BACKGROUND,true);options.putString(FluidCloudStyle.BACKGROUND_MODE,"native");options.putFloat(FluidCloudStyle.BACKGROUND_OPACITY,50f);style.configure(options);Handler.drainForCheck();
        BlurMixConfig.BlurMixMultiWithShader own=(BlurMixConfig.BlurMixMultiWithShader)proxy.appliedMixForCheck;
        check(own.recipe==NotificationBlurMode.SHADE_LIGHT&&foreground(own)==0xb3445566,"normal NOTIFICATION shader keeps its native alpha despite floating opacity setting");
        check(own.getMirrorScale()==.94f&&own.getAlphaWithBlurAmount()&&own.getPlaceHolderColor()==0x60403020,"shader copy keeps alpha/placeholder and native shade motion mirror");
        check(own==ext.viewBlurManager.lastMixForCheck,"normal following uses native factory material without a floating fill copy");
        style.release();Handler.drainForCheck();
    }
    private static int foreground(BlurMixConfig value) {
        return value instanceof BlurMixConfig.BlurMixMultiWithShader?((BlurMixConfig.BlurMixMultiWithShader)value).getForegroundShaderParam().getTopLayerColor():((BlurMixConfig.BlurMixMulti)value).getForegroundMixColor().getTopLayerColor();
    }
    private static int background(BlurMixConfig value) {
        return value instanceof BlurMixConfig.BlurMixMultiWithShader?((BlurMixConfig.BlurMixMultiWithShader)value).getBackgroundShaderParam().getTopLayerColor():((BlurMixConfig.BlurMixMulti)value).getBackgroundMixColor().getTopLayerColor();
    }
    private static void motionHeadsUp()throws Exception {
        Handler.postedForCheck.clear();FluidCloudStyle style=new FluidCloudStyle();ClassLoader loader=FluidCloudPlatformMaterialCheck.class.getClassLoader();
        style.resolveScene(loader);style.resolveNotification(loader);
        Body body=new Body();body.actualHeight=90;OplusCustomRow row=new OplusCustomRow();body.parent=row;
        row.parent=new com.oplus.systemui.notification.headsup.windowframe.HeadsUpLayout();row.setMBackgroundNormal(body);
        CustomRowEntry entry=new CustomRowEntry();row.setEntry(entry);NotificationBackgroundViewExtImp ext=(NotificationBackgroundViewExtImp)body.mExt;
        ViewBlurProxy proxy=ext.proxyForCheck;proxy.styleForCheck=style;proxy.backendForCheck=1;ext.viewBlurManager.motionForCheck=true;
        // Real CUSTOM notifyBlurParamChanged returns before changing this mode.
        ext.notificationBlurMode=NotificationBlurMode.SHADE_LIGHT;
        ext.materialForCheck.headsModeForCheck=NotificationBlurMode.HEADS_UP_MATERIAL_LIGHT;
        Object rawMix=proxy.blurConfig.motionBlurMixConfig,rawPlatform=proxy.blurConfig.platformMixConfig;
        GradientDrawable plate=new GradientDrawable();plate.setColor(0xff444444);ext.tempDrawable=plate;style.notificationRow(row);
        Bundle options=new Bundle();options.putBoolean(FluidCloudStyle.MASTER,true);options.putBoolean(FluidCloudStyle.FOLLOW_NOTIFICATION,true);style.configure(options);Handler.drainForCheck();
        Drawable own=style.sceneDrawable(body,plate);
        check(own==proxy.motionDrawableForCheck,"native Motion heads-up backend is accepted without a shade/ordinary donor");
        check(((com.oplusos.systemui.common.blurability.BlurMixConfig)proxy.appliedMotionMixForCheck).recipe==NotificationBlurMode.HEADS_UP_MATERIAL_LIGHT,
                "CUSTOM stale SHADE mode uses OEM forced-heads-up controller and NOTIFICATION motion mix");
        check(proxy.blurConfig.motionBlurMixConfig==rawMix&&proxy.blurConfig.platformMixConfig==rawPlatform,"motion submission restores original raw config fields");
        check(!style.sceneNeedsMeta(entry,true),"actual motion blur removes only proven target's opaque second plate");
        try(AutoCloseable scope=style.beginSceneDraw(ext)){check(ext.tempDrawable==null,"heads-up glass retains native backdrop without gray template overlay");}
        // Initial native palette observation is a real bind event, not a stable
        // frame. Let that one queued material update finish before the budget.
        style.sceneMaterialChanged(ext);Handler.drainForCheck();
        int applies=proxy.motionAppliesForCheck,modeReads=ext.materialForCheck.headsReads,mixes=ext.viewBlurManager.motionMixes;
        for(int i=0;i<10000;i++) {
            boolean frame=style.beginSceneFrame();try{style.sceneDrawable(body,plate);proxy.applyConfigToMotionBlur();style.sceneMaterialChanged(ext);}finally{style.endSceneFrame(frame);}
        }
        Handler.drainForCheck();check(proxy.motionAppliesForCheck==applies&&ext.materialForCheck.headsReads==modeReads&&ext.viewBlurManager.motionMixes==mixes
                &&Handler.postedForCheck.isEmpty(),"10000 stable heads-up frames neither submit, query theme nor allocate recipes: applies "+applies+"/"+proxy.motionAppliesForCheck
                +", modes "+modeReads+"/"+ext.materialForCheck.headsReads+", mixes "+mixes+"/"+ext.viewBlurManager.motionMixes+", pending "+Handler.postedForCheck.size());
        ext.materialForCheck.headsModeForCheck=NotificationBlurMode.HEADS_UP_MATERIAL_DARK;body.actualHeight++;style.sceneGeometryChanged(body);Handler.drainForCheck();
        check(((com.oplusos.systemui.common.blurability.BlurMixConfig)proxy.appliedMotionMixForCheck).recipe==NotificationBlurMode.HEADS_UP_MATERIAL_DARK,"native heads-up dark palette refreshes on a material/geometry event");
        ext.viewBlurManager.noMotionMixForCheck=true;body.actualHeight++;style.sceneGeometryChanged(body);Handler.drainForCheck();
        check(style.sceneDrawable(body,plate)==plate&&style.sceneNeedsMeta(entry,true),"unavailable OEM motion factory never erases readable native template");
        ext.viewBlurManager.noMotionMixForCheck=false;body.actualHeight++;style.sceneGeometryChanged(body);Handler.drainForCheck();
        check(style.sceneDrawable(body,plate)==own,"motion backend recovers on a real native event");
        ext.materialForCheck.headsModeForCheck=NotificationBlurMode.NO_BLUR_LIGHT;body.actualHeight++;style.sceneGeometryChanged(body);Handler.drainForCheck();
        check(style.sceneDrawable(body,plate)==plate&&proxy.appliedMotionMixForCheck==rawMix,"OEM no-blur capability retains native material rather than a stale heads-up palette");
        ext.materialForCheck.headsModeForCheck=NotificationBlurMode.HEADS_UP_MATERIAL_LIGHT;body.actualHeight++;style.sceneGeometryChanged(body);Handler.drainForCheck();
        options.putBoolean(FluidCloudStyle.FOLLOW_NOTIFICATION,false);style.configure(options);Handler.drainForCheck();
        check(proxy.appliedMotionMixForCheck==rawMix&&style.sceneDrawable(body,plate)==plate,"disabling following restores actual Motion backend and original template");
        style.release();Handler.drainForCheck();
    }
}
