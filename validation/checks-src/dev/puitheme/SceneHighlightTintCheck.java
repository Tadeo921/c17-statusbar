package dev.puitheme;

import android.graphics.Color;
import android.graphics.ColorSpace;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.ViewGroup;
import com.android.systemui.plugins.custom.CustomRowEntry;
import com.android.systemui.statusbar.notification.row.NotificationBackgroundView;
import com.oplus.posteffect.params.GradientStrokeLineParams;
import com.oplus.posteffect.params.InnerShadowParams;
import com.oplus.systemui.notification.blur.NotificationPlatFormBlurParamsManager;
import com.oplus.systemui.notification.row.material.NotificationBlurMode;
import com.oplus.systemui.statusbar.notification.customcard.OplusCustomRow;
import com.oplus.systemui.statusbar.notification.row.NotificationBackgroundViewExtImp;
import com.oplusos.systemui.common.blurability.ViewBlurProxy;

/** Actual notification recipe submissions: native lock optics follow media hue, independently of the fill. */
public final class SceneHighlightTintCheck {
    private static int checks;
    private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    private static final class Body extends NotificationBackgroundView {@Override public int getWidth(){return 240;}}
    private static final class Card {
        final Body body=new Body();final OplusCustomRow row=new OplusCustomRow();final CustomRowEntry entry=new CustomRowEntry();
        final NotificationBackgroundViewExtImp ext=(NotificationBackgroundViewExtImp)body.mExt;
        final ViewBlurProxy proxy=ext.proxyForCheck;final GradientDrawable plate=new GradientDrawable();
        Card(FluidCloudStyle style){body.actualHeight=120;body.mOnKeyguard=true;body.parent=row;row.setEntry(entry);row.setMBackgroundNormal(body);
            plate.setColor(0xff444444);ext.tempDrawable=plate;row.immersiveCustomDrawable=plate;proxy.styleForCheck=style;
            style.notificationRow(row);ext.notifyBlurParamChanged((NotificationBlurMode)style.sceneBlurMode(ext,NotificationBlurMode.WALLPAPER_A));
            ext.viewBlurManager.mode=NotificationBlurMode.WALLPAPER_A;style.sceneMaterialChanged(ext);}
        GradientStrokeLineParams stroke(){return (GradientStrokeLineParams)proxy.appliedStrokeForCheck;}
        InnerShadowParams shadow(){return (InnerShadowParams)proxy.appliedShadowForCheck;}
    }
    private static void hue(Card card,int expected){
        check((card.stroke().getStrokeLineColor().toArgb()&0xffffff)==expected,"stroke RGB follows the live music hue");
        check((card.shadow().getShadowColor().toArgb()&0xffffff)==expected,"inner bright edge RGB follows the live music hue");
    }
    private static void controls(Object expected,Object actual)throws Exception{
        for(java.lang.reflect.Field field:expected.getClass().getFields()){
            if(java.lang.reflect.Modifier.isStatic(field.getModifiers())||field.getType()!=Float.TYPE||field.getName().equals("width")||field.getName().equals("height"))continue;
            check(Float.floatToIntBits(field.getFloat(expected))==Float.floatToIntBits(field.getFloat(actual)),"native optical control retained exactly: "+field.getName());
        }
    }
    private static Integer nextMediaAccent(FluidCloudStyle style,Object section)throws Exception{
        java.lang.reflect.Field sections=FluidCloudStyle.class.getDeclaredField("mediaSections");sections.setAccessible(true);
        Object binding=((java.util.Map<?,?>)sections.get(style)).get(section);
        java.lang.reflect.Field next=binding.getClass().getDeclaredField("nextAccent");next.setAccessible(true);
        return (Integer)next.get(binding);
    }
    public static void main(String[] args)throws Exception{
        FluidCloudStyle style=new FluidCloudStyle();FluidCloudAccent accent=new FluidCloudAccent();style.setAccentController(accent);
        ClassLoader loader=SceneHighlightTintCheck.class.getClassLoader();style.resolveScene(loader);style.resolveNotification(loader);style.resolveMedia(loader);
        java.util.Map<android.view.View,Integer> gradients=new java.util.IdentityHashMap<>();
        java.util.Map<android.view.View,Boolean> keeps=new java.util.IdentityHashMap<>();int[] updates={0};
        style.setNotificationStyler(new FluidCloudStyle.NotificationStyler(){
            public void update(android.view.View body,boolean following,boolean keep){keeps.put(body,keep);updates[0]++;}
            public void gradient(android.view.View body,Integer argb){gradients.put(body,argb);}
        });style.setKeyguard(true);
        Card music=new Card(style),stopwatch=new Card(style);Card[] cards={music,stopwatch};
        ViewGroup mediaRoot=new ViewGroup(music.body.getContext());music.entry.immersiveCardView=mediaRoot;
        kotlinx.coroutines.flow.p colour=new kotlinx.coroutines.flow.p(new com.oplus.systemui.plugins.shared.data.media.model.a(0xffa34759));
        com.oplus.systemui.plugins.shared.template.section.media.K0 model=new com.oplus.systemui.plugins.shared.template.section.media.K0(colour);
        com.oplus.systemui.plugins.shared.template.section.media.s1 section=new com.oplus.systemui.plugins.shared.template.section.media.s1(music.body.getContext(),model,mediaRoot,null);
        Bundle options=new Bundle();options.putBoolean(FluidCloudAccent.ENABLED,true);options.putBoolean(FluidCloudStyle.MASTER,true);
        options.putBoolean(FluidCloudStyle.DESKTOP_BACKGROUND,true);options.putBoolean(FluidCloudStyle.FOLLOW_NOTIFICATION,true);options.putBoolean(FluidCloudStyle.FOLLOW_MEDIA,true);
        options.putString(FluidCloudStyle.BACKGROUND_MODE,"native");
        options.putBoolean(FluidCloudStyle.FOLLOW_ONLY_PLAYING,false);options.putString(FluidCloudStyle.COLOR_STYLE,FluidCloudStyle.GRADIENT);
        accent.configure(options);style.configure(options);style.mediaChanged(section);style.notificationRow(music.row);Handler.drainForCheck();
        for(Card card:cards){
            check(style.sceneDrawable(card.body,card.plate)==card.proxy.drawableForCheck,"actual lock material is confirmed");
            check((card.stroke().getStrokeLineColor().toArgb()&0xffffff)==0xa34759,"music and non-media lock bright edge must use the current music hue");
            check((card.shadow().getShadowColor().toArgb()&0xffffff)==0xa34759,"inner optical edge follows the same hue");
            check(card.stroke().getStrokeLineColor().alpha()==Color.valueOf(0xd8ffffff).alpha(),"native edge color alpha is exact");
            check(card.shadow().getShadowColor().alpha()==1f,"media opacity does not multiply native optical alpha");
        }
        // Stable actual draw/submit hooks must reuse the event-built optical copies.
        int strokeCopies=GradientStrokeLineParams.copiesForCheck,shadowCopies=InnerShadowParams.copiesForCheck;
        int musicSubmits=music.proxy.platformAppliesForCheck,stopwatchSubmits=stopwatch.proxy.platformAppliesForCheck;
        Object stableStroke=stopwatch.stroke(),stableShadow=stopwatch.shadow();
        for(int i=0;i<10000;i++)for(Card card:cards){
            boolean frame=style.beginSceneFrame();
            try{style.sceneDrawable(card.body,card.plate);card.proxy.applyConfigToPlatformBlur();style.sceneMaterialChanged(card.ext);}
            finally{style.endSceneFrame(frame);}
        }
        Handler.drainForCheck();check(strokeCopies==GradientStrokeLineParams.copiesForCheck&&shadowCopies==InnerShadowParams.copiesForCheck,"10000 stable frames make no optical copies");
        check(musicSubmits==music.proxy.platformAppliesForCheck&&stopwatchSubmits==stopwatch.proxy.platformAppliesForCheck,"10000 stable frames make no native recipe submissions");
        check(stopwatch.stroke()==stableStroke&&stopwatch.shadow()==stableShadow,"stable draw retains exact cached optical instances");
        // A new media event recolours both rows. Its alpha never replaces the native optical alpha.
        colour.value=new com.oplus.systemui.plugins.shared.data.media.model.a(0x2312789a);style.mediaChanged(section);Handler.drainForCheck();
        for(Card card:cards){hue(card,0x12789a);check(card.stroke().getStrokeLineColor().alpha()==Color.valueOf(0xd8ffffff).alpha(),"new media alpha leaves native bright-edge alpha intact");}
        // Native palette events may replace controls without changing the row's flat tint.
        NotificationPlatFormBlurParamsManager manager=NotificationPlatFormBlurParamsManager.INSTANCE;
        GradientStrokeLineParams nativeStroke=new GradientStrokeLineParams();InnerShadowParams nativeShadow=new InnerShadowParams();
        nativeStroke.strokeLineColor=Color.valueOf(.8f,.9f,1f,.53125f,ColorSpace.get(ColorSpace.Named.DISPLAY_P3));
        nativeStroke.strokeLineAlphaNear=.27125f;nativeStroke.strokeLineAlphaFar=.64375f;nativeStroke.strokeLinePow=2.25f;
        nativeShadow.shadowColor=Color.valueOf(.9f,.7f,.6f,.3125f,ColorSpace.get(ColorSpace.Named.DISPLAY_P3));
        nativeShadow.offsetShadowX=13f;nativeShadow.offsetShadowY=-22f;nativeShadow.offsetShadowRange=49f;nativeShadow.noOffsetShadowAlpha=.21875f;
        manager.strokeForCheck=nativeStroke;manager.shadowForCheck=nativeShadow;
        try{
            for(Card card:cards)style.scenePaletteChanged(card.ext);Handler.drainForCheck();
            for(Card card:cards){
                hue(card,0x12789a);controls(nativeStroke,card.stroke());controls(nativeShadow,card.shadow());
                check(card.stroke()!=nativeStroke&&card.shadow()!=nativeShadow,"factory/shared parameters are independent owned copies");
                check(card.stroke().getStrokeLineColor().alpha()==.53125f&&card.shadow().getShadowColor().alpha()==.3125f,"non-8-bit native alpha retained exactly");
                check(card.stroke().getStrokeLineColor().getColorSpace()==nativeStroke.strokeLineColor.getColorSpace()&&card.shadow().getShadowColor().getColorSpace()==nativeShadow.shadowColor.getColorSpace(),"native color-space identity retained after RGB conversion");
                check(card.stroke().width==240f&&card.stroke().height==120f&&card.shadow().width==240f&&card.shadow().height==120f,"existing native geometry updater runs on the owned recipe");
            }
            check(nativeStroke.width==0&&nativeShadow.height==0&&nativeStroke.strokeLineColor.red()==.8f&&nativeShadow.shadowColor.alpha()==.3125f,"native factory colors and geometry never overwritten");
        }finally{manager.strokeForCheck=null;manager.shadowForCheck=null;}
        // The awake recipe, including tinted edges, is held unchanged through panoramic AOD.
        Object awakeStroke=stopwatch.stroke(),awakeShadow=stopwatch.shadow();
        com.android.systemui.statusbar.StatusBarStateControllerImpl state=new com.android.systemui.statusbar.StatusBarStateControllerImpl();state.state=1;state.dozing=true;
        style.onNativeState(state);Handler.drainForCheck();
        check(style.sceneDrawable(stopwatch.body,stopwatch.plate)==stopwatch.proxy.drawableForCheck&&stopwatch.stroke()==awakeStroke&&stopwatch.shadow()==awakeShadow,"AOD holds the exact awake media-hued optical recipe");
        Integer musicGradient=gradients.get(music.body),stopwatchGradient=gradients.get(stopwatch.body),pageAccent=nextMediaAccent(style,section);
        // Settled AOD: a track change recolours the cards. (Only the crossfade holds the awake recipe untouched; a track that changes
        // with the screen off must not keep the previous track's colour until the screen wakes.)
        colour.value=new com.oplus.systemui.plugins.shared.data.media.model.a(0xff335577);style.mediaChanged(section);Handler.drainForCheck();
        for(Card card:cards)hue(card,0x12789a); // the optical edges are the native recipe, held until the screen wakes
        check(!musicGradient.equals(gradients.get(music.body))&&!stopwatchGradient.equals(gradients.get(stopwatch.body)),"a track change in settled AOD recolours the gradient");
        musicGradient=gradients.get(music.body);stopwatchGradient=gradients.get(stopwatch.body);
        check(!pageAccent.equals(nextMediaAccent(style,section)),"settled AOD: the immersive media-page accent follows the new track too");
        pageAccent=nextMediaAccent(style,section);
        check((style.normalNotificationColor(music.body)&0xffffff)==0x335577&&(style.normalNotificationColor(stopwatch.body)&0xffffff)==0x335577,"settled AOD takes the new colour decision");
        int previousUpdates=updates[0];options.putBoolean(C17HighlightRemoval.ENABLED,true);
        options.putBoolean(C17HighlightRemoval.HEADS_UP_ENABLED,true);options.putBoolean(C17HighlightRemoval.NOTIFICATION_ENABLED,true);
        options.putBoolean(FluidCloudStyle.MEDIA_KEEP_HIGHLIGHT,true);style.configure(options);Handler.drainForCheck();
        check(updates[0]>previousUpdates&&Boolean.TRUE.equals(keeps.get(music.body))&&!Boolean.TRUE.equals(keeps.get(stopwatch.body)),"AOD color holding still publishes keep-music-highlight priority immediately");
        previousUpdates=updates[0];options.putBoolean(FluidCloudStyle.MEDIA_KEEP_HIGHLIGHT,false);style.configure(options);Handler.drainForCheck();
        check(updates[0]>previousUpdates&&!Boolean.TRUE.equals(keeps.get(music.body)),"AOD color holding does not freeze the user's new highlight-removal policy");
        check(musicGradient.equals(gradients.get(music.body))&&stopwatchGradient.equals(gradients.get(stopwatch.body)),"optical policy changes preserve only the held colors");
        options.putBoolean(C17HighlightRemoval.ENABLED,false);style.configure(options);Handler.drainForCheck();
        strokeCopies=GradientStrokeLineParams.copiesForCheck;shadowCopies=InnerShadowParams.copiesForCheck;
        musicSubmits=music.proxy.platformAppliesForCheck;stopwatchSubmits=stopwatch.proxy.platformAppliesForCheck;
        for(int i=0;i<10000;i++)for(Card card:cards){boolean frame=style.beginSceneFrame();
            try{style.sceneDrawable(card.body,card.plate);card.proxy.applyConfigToPlatformBlur();style.sceneMaterialChanged(card.ext);}
            finally{style.endSceneFrame(frame);}}
        Handler.drainForCheck();
        check(strokeCopies==GradientStrokeLineParams.copiesForCheck&&shadowCopies==InnerShadowParams.copiesForCheck,"10000 held AOD frames make no new optical copies");
        check(musicSubmits==music.proxy.platformAppliesForCheck&&stopwatchSubmits==stopwatch.proxy.platformAppliesForCheck,"10000 held AOD frames make no recipe submissions");
        style.mediaDisposed(section);Handler.drainForCheck();
        for(Card card:cards){hue(card,0xffffff);check(gradients.get(card.body)==null,"real media disposal clears held colors even with following still enabled");}
        state.dozing=false;style.onNativeState(state);style.mediaChanged(section);Handler.drainForCheck();
        for(Card card:cards){hue(card,0x335577);check((gradients.get(card.body)&0xffffff)==0x335577,"waking publishes the latest media hue in the gradient and bright edge together");}
        colour.value=new com.oplus.systemui.plugins.shared.data.media.model.a(0x2312789a);style.mediaChanged(section);Handler.drainForCheck();
        // Independent media following also works without replacing the card's native CUSTOM glass.
        options.putBoolean(FluidCloudStyle.FOLLOW_NOTIFICATION,false);style.configure(options);Handler.drainForCheck();
        for(Card card:cards)hue(card,0x12789a);
        awakeStroke=music.stroke();awakeShadow=music.shadow();state.dozing=true;style.onNativeState(state);Handler.drainForCheck();
        check(style.sceneDrawable(music.body,music.plate)==music.proxy.drawableForCheck&&music.stroke()==awakeStroke&&music.shadow()==awakeShadow,
                "AOD must keep a confirmed media-hued bright-edge recipe even when native-follow is off");
        musicGradient=gradients.get(music.body);pageAccent=nextMediaAccent(style,section);
        colour.value=new com.oplus.systemui.plugins.shared.data.media.model.a(0xff775533);style.mediaChanged(section);Handler.drainForCheck();
        check(!musicGradient.equals(gradients.get(music.body))&&!pageAccent.equals(nextMediaAccent(style,section)),"native-follow off: a track change in settled AOD recolours the music composition too");
        hue(music,0x12789a);
        options.putBoolean(FluidCloudStyle.FOLLOW_MEDIA,false);style.configure(options);Handler.drainForCheck();
        hue(stopwatch,0xffffff);check(gradients.get(stopwatch.body)==null,"disabling following during AOD restores the non-media card immediately");
        options.putBoolean(FluidCloudStyle.MASTER,false);style.configure(options);Handler.drainForCheck();
        hue(stopwatch,0xffffff);check(gradients.get(stopwatch.body)==null,"disabling the style master during AOD does not retain media colors");
        style.mediaDisposed(section);Handler.drainForCheck();
        for(Card card:cards){hue(card,0xffffff);check(gradients.get(card.body)==null,"media removal during AOD clears the held gradient");
            check(card.proxy.appliedStrokeForCheck==card.proxy.blurConfig.gradientStrokeLineParam,"media removal during AOD restores native optical parameters");}
        state.dozing=false;style.onNativeState(state);colour.value=new com.oplus.systemui.plugins.shared.data.media.model.a(0x2312789a);
        options.putBoolean(FluidCloudStyle.MASTER,true);options.putBoolean(FluidCloudStyle.FOLLOW_MEDIA,true);style.configure(options);
        style.mediaChanged(section);Handler.drainForCheck();state.dozing=true;style.onNativeState(state);Handler.drainForCheck();
        options.putBoolean(FluidCloudStyle.FOLLOW_NOTIFICATION,true);options.putBoolean(FluidCloudAccent.ENABLED,false);options.putBoolean(FluidCloudStyle.FOLLOW_MEDIA,false);
        accent.configure(options);style.configure(options);Handler.drainForCheck();
        for(Card card:cards){hue(card,0xffffff);check(card.proxy.appliedStrokeForCheck==card.proxy.blurConfig.gradientStrokeLineParam,"disabling media hue during AOD restores genuine native optical parameters");}
        state.dozing=false;style.onNativeState(state);options.putBoolean(FluidCloudAccent.ENABLED,true);options.putBoolean(FluidCloudStyle.FOLLOW_MEDIA,true);
        accent.configure(options);style.configure(options);Handler.drainForCheck();
        // Uncertain native copies leave both original optical parameters fully intact.
        GradientStrokeLineParams.failCopiesForCheck=true;
        try{colour.value=new com.oplus.systemui.plugins.shared.data.media.model.a(0xff554477);style.mediaChanged(section);Handler.drainForCheck();
            for(Card card:cards)hue(card,0xffffff);
            musicGradient=gradients.get(music.body);state.dozing=true;style.onNativeState(state);Handler.drainForCheck();
            colour.value=new com.oplus.systemui.plugins.shared.data.media.model.a(0xff886699);style.mediaChanged(section);Handler.drainForCheck();
            check(!musicGradient.equals(gradients.get(music.body))&&(gradients.get(music.body)&0xffffff)==0x886699,"an unknown optical-copy contract never freezes the final color layer");
            for(Card card:cards)hue(card,0xffffff);
            state.dozing=false;style.onNativeState(state);colour.value=new com.oplus.systemui.plugins.shared.data.media.model.a(0xff554477);
            style.mediaChanged(section);Handler.drainForCheck();
        }finally{GradientStrokeLineParams.failCopiesForCheck=false;}
        style.scenePaletteChanged(stopwatch.ext);style.scenePaletteChanged(music.ext);Handler.drainForCheck();
        for(Card card:cards)hue(card,0x554477);
        // No active colour or a custom base alone restores the native bright edges.
        options.putBoolean(FluidCloudAccent.ENABLED,false);options.putBoolean(FluidCloudStyle.FOLLOW_MEDIA,false);
        options.putString(FluidCloudStyle.BACKGROUND_MODE,"custom");options.putInt(FluidCloudStyle.BACKGROUND_COLOR,0xff338855);
        accent.configure(options);style.configure(options);Handler.drainForCheck();
        for(Card card:cards)hue(card,0xffffff);
        options.putBoolean(FluidCloudAccent.ENABLED,true);options.putBoolean(FluidCloudStyle.FOLLOW_MEDIA,true);accent.configure(options);style.configure(options);Handler.drainForCheck();
        for(Card card:cards)hue(card,0x554477);
        // A copied recipe without a confirmed native drawable is never an AOD colour hold.
        music.proxy.nativeReturnsFallbackForCheck=true;style.scenePaletteChanged(music.ext);Handler.drainForCheck();
        check(style.sceneDrawable(music.body,music.plate)==music.plate,"unconfirmed native submission uses its original drawable");
        musicGradient=gradients.get(music.body);state.dozing=true;style.onNativeState(state);Handler.drainForCheck();
        colour.value=new com.oplus.systemui.plugins.shared.data.media.model.a(0xff665544);style.mediaChanged(section);Handler.drainForCheck();
        check(!musicGradient.equals(gradients.get(music.body))&&(gradients.get(music.body)&0xffffff)==0x665544,"unconfirmed platform optics never freeze media color updates");
        check(style.sceneDrawable(music.body,music.plate)==music.plate,"unknown platform readiness retains the genuine native drawable");
        state.dozing=false;style.onNativeState(state);music.proxy.nativeReturnsFallbackForCheck=false;
        colour.value=new com.oplus.systemui.plugins.shared.data.media.model.a(0xff554477);style.mediaChanged(section);
        style.scenePaletteChanged(music.ext);Handler.drainForCheck();for(Card card:cards)hue(card,0x554477);
        // The notification shade keeps an ordinary notification's exact native optics.
        style.setKeyguard(false);Handler.drainForCheck();
        Body ordinary=new Body();ordinary.actualHeight=120;ordinary.parent=new com.android.systemui.statusbar.notification.row.ExpandableNotificationRow(ordinary.getContext(),120,0,0);
        NotificationBackgroundViewExtImp ordinaryExt=(NotificationBackgroundViewExtImp)ordinary.mExt;ordinaryExt.proxyForCheck.styleForCheck=style;
        ordinaryExt.proxyForCheck.applyConfigToPlatformBlur();
        check(ordinaryExt.proxyForCheck.appliedStrokeForCheck==ordinaryExt.proxyForCheck.blurConfig.gradientStrokeLineParam&&ordinaryExt.proxyForCheck.appliedShadowForCheck==ordinaryExt.proxyForCheck.blurConfig.innerShadowParams,"ordinary shade notifications retain their exact native optical parameters");
        check(((GradientStrokeLineParams)ordinaryExt.proxyForCheck.appliedStrokeForCheck).getStrokeLineColor().toArgb()==0xd8ffffff,"ordinary shade native white edge is unchanged");
        style.setKeyguard(true);Handler.drainForCheck();
        // The same custom row as a native banner, or in the shade, must not retain lock tint.
        music.row.parent=new com.oplus.systemui.notification.headsup.windowframe.HeadsUpLayout();style.notificationRow(music.row);style.scenePaletteChanged(music.ext);Handler.drainForCheck();
        hue(music,0xffffff);music.row.parent=null;
        style.setKeyguard(false);style.notificationRow(music.row);Handler.drainForCheck();for(Card card:cards)hue(card,0xffffff);
        style.setKeyguard(true);style.notificationRow(music.row);Handler.drainForCheck();for(Card card:cards)hue(card,0x554477);
        style.release();Handler.drainForCheck();
        for(Card card:cards){
            check(card.proxy.appliedStrokeForCheck==card.proxy.blurConfig.gradientStrokeLineParam&&card.proxy.appliedShadowForCheck==card.proxy.blurConfig.innerShadowParams,"release resubmits the exact current native config objects");
            hue(card,0xffffff);
        }
        check(SceneHighlightTint.resolve(new ClassLoader(loader){@Override public Class<?> loadClass(String name)throws ClassNotFoundException{
            if(name.equals("com.oplus.posteffect.params.GradientStrokeLineParams"))return Object.class;return super.loadClass(name);}})==null,"unknown optical API remains optional and preserves native material");
        System.out.println("SceneHighlightTintCheck: "+checks+" checks passed");
    }
}
