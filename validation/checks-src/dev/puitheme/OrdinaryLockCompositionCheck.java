package dev.puitheme;

import android.graphics.Canvas;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import com.android.systemui.plugins.custom.CustomRowEntry;
import com.android.systemui.statusbar.notification.row.ExpandableNotificationRow;
import com.android.systemui.statusbar.notification.row.NotificationBackgroundView;
import com.oplus.posteffect.params.GradientStrokeLineParams;
import com.oplus.posteffect.params.InnerShadowParams;
import com.oplus.systemui.notification.row.material.NotificationBlurMode;
import com.oplus.systemui.statusbar.notification.customcard.OplusCustomRow;
import com.oplus.systemui.statusbar.notification.row.NotificationBackgroundViewExtImp;
import com.oplusos.systemui.common.blurability.ViewBlurProxy;

/** Native ordinary-body events, shared lock layers, optical policy and AOD continuity. */
public final class OrdinaryLockCompositionCheck {
    private static int checks;
    private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    private static final class Body extends NotificationBackgroundView {@Override public int getWidth(){return 240;}}
    private static final class Card {
        final Body body=new Body();final GradientDrawable plate=new GradientDrawable();
        final NotificationBackgroundViewExtImp ext=(NotificationBackgroundViewExtImp)body.mExt;
        final ViewBlurProxy proxy=ext.proxyForCheck;final CustomRowEntry entry=new CustomRowEntry();
        final Object row;
        Card(FluidCloudStyle style,boolean ordinary){this(style,ordinary,true);}
        Card(FluidCloudStyle style,boolean ordinary,boolean observe){
            body.actualHeight=120;body.mOnKeyguard=true;plate.setColor(0xff444444);proxy.styleForCheck=style;
            row=ordinary?new ExpandableNotificationRow(body.getContext(),120,0,0):new OplusCustomRow();body.parent=(View)row;
            if(!ordinary){OplusCustomRow custom=(OplusCustomRow)row;custom.setEntry(entry);custom.setMBackgroundNormal(body);
                ext.tempDrawable=plate;custom.immersiveCustomDrawable=plate;style.notificationRow(custom);}
            ext.notifyBlurParamChanged((NotificationBlurMode)style.sceneBlurMode(ext,NotificationBlurMode.WALLPAPER_A));
            ext.viewBlurManager.mode=NotificationBlurMode.WALLPAPER_A;if(observe)style.sceneMaterialChanged(ext);
        }
        int strokeHue(){return ((GradientStrokeLineParams)proxy.appliedStrokeForCheck).getStrokeLineColor().toArgb()&0xffffff;}
        int shadowHue(){return ((InnerShadowParams)proxy.appliedShadowForCheck).getShadowColor().toArgb()&0xffffff;}
    }
    private static Integer gradient(C17HighlightRemoval control,View body)throws Exception{
        java.lang.reflect.Field field=C17HighlightRemoval.class.getDeclaredField("fluidGradients");field.setAccessible(true);
        C17HighlightRemoval.Fade value=(C17HighlightRemoval.Fade)((java.util.Map<?,?>)field.get(control)).get(body);
        return value==null?null:Integer.valueOf(value.to);
    }
    /** The layer that is shown for good: a layer that is fading out (leaving) is not one. */
    private static Integer liveGradient(C17HighlightRemoval control,View body)throws Exception{
        java.lang.reflect.Field field=C17HighlightRemoval.class.getDeclaredField("fluidGradients");field.setAccessible(true);
        C17HighlightRemoval.Fade value=(C17HighlightRemoval.Fade)((java.util.Map<?,?>)field.get(control)).get(body);
        return value==null||value.leaving?null:Integer.valueOf(value.to);
    }
    private static Integer overlay(C17HighlightRemoval control,View body)throws Exception{
        java.lang.reflect.Field field=C17HighlightRemoval.class.getDeclaredField("fluidOverlays");field.setAccessible(true);
        C17HighlightRemoval.Fade value=(C17HighlightRemoval.Fade)((java.util.Map<?,?>)field.get(control)).get(body);
        return value==null||value.leaving?null:Integer.valueOf(value.to);
    }
    private static boolean marked(C17HighlightRemoval control,View body)throws Exception{
        java.lang.reflect.Field field=C17HighlightRemoval.class.getDeclaredField("lockMediaCompositions");field.setAccessible(true);
        return Boolean.TRUE.equals(((java.util.Map<?,?>)field.get(control)).get(body));
    }
    private static void hue(Card[] cards,int color){for(Card card:cards){check(card.strokeHue()==color,"every lock card has the same stroke hue");check(card.shadowHue()==color,"every lock card has the same inner-edge hue");}}
    private static Canvas draw(C17HighlightRemoval control,View body)throws Throwable{
        Canvas canvas=new Canvas();int[] calls={0};control.drawNotificationBackground(body,canvas,c->{calls[0]++;return null;});
        check(calls[0]==1,"native body content draws exactly once");return canvas;
    }
    public static void main(String[] args)throws Throwable{
        Handler.postedForCheck.clear();android.os.SystemClock.uptime=1000;
        FluidCloudStyle style=new FluidCloudStyle();FluidCloudAccent accent=new FluidCloudAccent();C17HighlightRemoval control=new C17HighlightRemoval();
        style.setAccentController(accent);ClassLoader loader=OrdinaryLockCompositionCheck.class.getClassLoader();
        style.resolveScene(loader);style.resolveNotification(loader);style.resolveMedia(loader);
        style.setNotificationStyler(new FluidCloudStyle.NotificationStyler(){
            public void update(View body,boolean follow,boolean keep){control.setFluidNotificationBackground(body,follow,keep);}
            public void forget(View body){control.forgetFluidNotificationBackground(body);}
            public void lockMediaComposition(View body,boolean active){control.setLockMediaComposition(body,active);}
            public void mediaFollow(Integer color){control.setMediaFollowColor(color);}
            public void gradient(View body,Integer color){control.setFluidGradient(body,color);}
            public void overlay(View body,Integer color,java.util.function.BooleanSupplier gate){control.setFluidOverlay(body,color,gate);}
        });style.setKeyguard(true);
        Card music=new Card(style,false),stopwatch=new Card(style,false),ordinary=new Card(style,true);Card[] cards={music,stopwatch,ordinary};
        ViewGroup mediaRoot=new ViewGroup(music.body.getContext());music.entry.immersiveCardView=mediaRoot;
        kotlinx.coroutines.flow.p colour=new kotlinx.coroutines.flow.p(new com.oplus.systemui.plugins.shared.data.media.model.a(0xffa34759));
        com.oplus.systemui.plugins.shared.template.section.media.K0 model=new com.oplus.systemui.plugins.shared.template.section.media.K0(colour);
        com.oplus.systemui.plugins.shared.template.section.media.s1 section=new com.oplus.systemui.plugins.shared.template.section.media.s1(music.body.getContext(),model,mediaRoot,null);
        Bundle options=new Bundle();options.putBoolean(FluidCloudAccent.ENABLED,true);options.putBoolean(FluidCloudStyle.MASTER,true);
        options.putBoolean(FluidCloudStyle.DESKTOP_BACKGROUND,true);options.putBoolean(FluidCloudStyle.FOLLOW_NOTIFICATION,true);
        options.putBoolean(FluidCloudStyle.FOLLOW_MEDIA,true);options.putBoolean(FluidCloudStyle.NOTIFICATION_FOLLOW,true);
        options.putBoolean(FluidCloudStyle.FOLLOW_ONLY_PLAYING,false);options.putString(FluidCloudStyle.BACKGROUND_MODE,"native");
        options.putString(FluidCloudStyle.COLOR_STYLE,FluidCloudStyle.GRADIENT);options.putFloat(FluidCloudAccent.LOCK_LIGHT,0f);options.putFloat(FluidCloudAccent.AOD_LIGHT,0f);
        options.putFloat(FluidCloudAccent.OPACITY,40f);accent.configure(options);style.configure(options);style.mediaChanged(section);
        style.notificationRow(music.row);Handler.drainForCheck();
        check(marked(control,ordinary.body),"the existing OEM material event registers an exact ordinary body");
        for(Card card:cards){check(style.sceneDrawable(card.body,card.plate)==card.proxy.drawableForCheck,"same native lock glass is confirmed");
            check(gradient(control,card.body).equals(gradient(control,music.body)),"music, stopwatch and ordinary cards use one final gradient and opacity");}
        hue(cards,0xa34759);
        android.os.SystemClock.uptime+=C17HighlightRemoval.COLOUR_FADE_MS;
        Canvas normal=draw(control,ordinary.body);check(normal.lastLayerPaint!=null&&normal.lastLayerPaint.getColorFilter()==null,"ordinary media is not a flat finished-card filter");
        check(normal.rectFills==1&&normal.lastFillShader instanceof android.graphics.LinearGradient,"ordinary body draws the same single transparent gradient");
        android.graphics.LinearGradient stops=(android.graphics.LinearGradient)normal.lastFillShader;
        check((stops.colors[0]>>>24)==102&&(stops.colors[stops.colors.length-1]>>>24)==0,"the media gradient fades to transparent with shared opacity");
        // A user's unified acrylic base remains lower priority while media owns this body.
        options.putBoolean(C17HighlightRemoval.UNIFORM_NOTIFICATION_ENABLED,true);options.putInt(C17HighlightRemoval.LIGHT_BACKGROUND,0xb2112233);control.configure(options);
        for(Card card:cards){Canvas drawing=draw(control,card.body);
            check(drawing.lastLayerPaint.getColorFilter()==null&&drawing.rectFills==1,"every media lock card replaces the old uniform flat filter with one final layer");
            check(((android.graphics.LinearGradient)drawing.lastFillShader).colors[0]==stops.colors[0],"ordinary, music and stopwatch have the same gradient over native glass with uniform enabled");}
        // Settled AOD recolours every card with one common gradient when the next track arrives asleep; only the crossfade holds the
        // awake recipe, and the native optical edges stay the held awake recipe until the screen wakes.
        com.android.systemui.statusbar.StatusBarStateControllerImpl state=new com.android.systemui.statusbar.StatusBarStateControllerImpl();state.state=1;state.dozing=true;
        Integer awake=gradient(control,ordinary.body);Object oldStroke=ordinary.proxy.appliedStrokeForCheck;
        style.onNativeState(state);Handler.drainForCheck();colour.value=new com.oplus.systemui.plugins.shared.data.media.model.a(0xff12789a);
        style.mediaChanged(section);Handler.drainForCheck();hue(cards,0xa34759);
        Integer recoloured=gradient(control,music.body);
        check(!awake.equals(recoloured)&&(recoloured&0xffffff)==0x12789a,"settled AOD takes the new track's colour");
        // An ordinary notification is decided like the fluid cards: with the gradient style they all share the one gradient in AOD too.
        for(Card card:new Card[]{music,stopwatch,ordinary})check(recoloured.equals(gradient(control,card.body)),"settled AOD recolours every card's common gradient across a track change");
        check(overlay(control,ordinary.body)==null,"and the ordinary notification gets no second, flat layer");
        check(ordinary.proxy.appliedStrokeForCheck==oldStroke,"ordinary AOD keeps the proven awake optical object");
        options.putFloat(FluidCloudStyle.NOTIFICATION_FOLLOW_OPACITY,50f);options.putFloat(LockNotificationSettings.AOD_NOTIFICATION_FOLLOW_OPACITY,50f);
        options.putBoolean(FluidCloudAccent.MASK_ENABLED,true);options.putInt(FluidCloudAccent.MASK_COLOR,0xff000000);
        options.putFloat(FluidCloudAccent.MASK_OPACITY,50f);accent.configure(options);style.configure(options);Handler.drainForCheck();
        Card late=new Card(style,true);Handler.drainForCheck();
        check(gradient(control,late.body)==null||(gradient(control,late.body)>>>24)==0x33,"an ordinary notification arriving after an AOD track change takes the new colour at its own strength");
        options.putFloat(FluidCloudStyle.NOTIFICATION_FOLLOW_OPACITY,100f);options.putFloat(LockNotificationSettings.AOD_NOTIFICATION_FOLLOW_OPACITY,100f);options.putBoolean(FluidCloudAccent.MASK_ENABLED,false);
        accent.configure(options);style.configure(options);Handler.drainForCheck();
        state.dozing=false;style.onNativeState(state);Handler.drainForCheck();hue(cards,0x12789a);
        check(late.strokeHue()==0x12789a&&gradient(control,late.body).equals(gradient(control,music.body)),"waking updates the late arrival and the original cards to the latest shared media colour");
        for(Card card:cards)check(gradient(control,card.body).equals(gradient(control,music.body)),"wake updates all card layers together");
        // The old notification strength still scales its one layer, without changing optical alpha.
        float edgeAlpha=((GradientStrokeLineParams)ordinary.proxy.appliedStrokeForCheck).getStrokeLineColor().alpha();
        options.putFloat(FluidCloudStyle.NOTIFICATION_FOLLOW_OPACITY,50f);options.putFloat(LockNotificationSettings.AOD_NOTIFICATION_FOLLOW_OPACITY,50f);style.configure(options);Handler.drainForCheck();
        check((gradient(control,ordinary.body)>>>24)==51&&(gradient(control,music.body)>>>24)==102,"ordinary strength scales the shared opacity once");
        check(((GradientStrokeLineParams)ordinary.proxy.appliedStrokeForCheck).getStrokeLineColor().alpha()==edgeAlpha,"ordinary strength preserves the native edge alpha");
        options.putFloat(FluidCloudStyle.NOTIFICATION_FOLLOW_OPACITY,100f);options.putFloat(LockNotificationSettings.AOD_NOTIFICATION_FOLLOW_OPACITY,100f);options.putBoolean(FluidCloudStyle.FOLLOW_MEDIA,false);style.configure(options);Handler.drainForCheck();
        check(marked(control,ordinary.body)&&ordinary.strokeHue()==0x12789a,"ordinary following remains independent when fluid following is off");
        options.putBoolean(FluidCloudStyle.FOLLOW_ONLY_PLAYING,true);style.configure(options);Handler.drainForCheck();
        check(style.wantsPlaybackState(),"ordinary following alone observes the playback state");
        style.setMediaPlaying(false);style.expireStopGraceForCheck();Handler.drainForCheck();
        check(!marked(control,ordinary.body)&&gradient(control,ordinary.body)==null,"ordinary following alone stops after the playing grace");
        style.setMediaPlaying(true);Handler.drainForCheck();check(marked(control,ordinary.body),"ordinary following alone resumes with playback");
        options.putBoolean(FluidCloudStyle.FOLLOW_ONLY_PLAYING,false);
        options.putBoolean(FluidCloudStyle.FOLLOW_MEDIA,true);style.configure(options);Handler.drainForCheck();
        Card firstDraw=new Card(style,true,false);
        boolean firstFrame=style.beginSceneFrame();try{style.sceneMaterialChanged(firstDraw.ext);
            check(!marked(control,firstDraw.body),"a first native draw never registers or mutates an ordinary body inline");
        }finally{style.endSceneFrame(firstFrame);}
        Handler.drainForCheck();check(marked(control,firstDraw.body)&&firstDraw.strokeHue()==0x12789a,"the first native draw's snapshot registers once outside drawing");
        // Exact optical suppression remains the user's C17 policy.
        com.oplus.posteffect.drawable.BlendDrawable engine=new com.oplus.posteffect.drawable.BlendDrawable();
        C17HighlightRemovalCheck.NativeMaterial material=new C17HighlightRemovalCheck.NativeMaterial();
        C17HighlightRemovalCheck.HookedShader shader=new C17HighlightRemovalCheck.HookedShader(control);material.shader=shader;
        java.util.Arrays.fill(material.optics.values,.7f);QsTileAppearance.setField(engine,"drawableShader",material);
        try(C17HighlightRemoval.SurfaceScope scope=control.beginSurface(ordinary.body)){control.bindDrawable(engine);}
        options.putBoolean(C17HighlightRemoval.ENABLED,true);control.configure(options);style.configure(options);Handler.drainForCheck();
        check(shader.floats.get("u_opticsArray")[3]==0f,"normal lock cards obey C17 highlight removal");
        options.putBoolean(C17HighlightRemoval.ENABLED,false);control.configure(options);style.configure(options);Handler.drainForCheck();
        check(shader.floats.get("u_opticsArray")[3]==.7f,"disabling highlight removal restores its exact native optical control");
        // Thousands of native frames reuse the snapshot and never register, clone or submit anew.
        int copies=GradientStrokeLineParams.copiesForCheck,submits=ordinary.proxy.platformAppliesForCheck,invalidates=ordinary.body.invalidations;
        check(Handler.postedForCheck.isEmpty(),"ordinary setup queue settled before the stable budget");
        for(int i=0;i<10000;i++){boolean frame=style.beginSceneFrame();try{style.sceneDrawable(ordinary.body,ordinary.plate);ordinary.proxy.applyConfigToPlatformBlur();style.sceneMaterialChanged(ordinary.ext);}finally{style.endSceneFrame(frame);}}
        check(Handler.postedForCheck.isEmpty()&&ordinary.body.invalidations==invalidates,"stable normal draws do not request refresh frames: pending="+Handler.postedForCheck.size()+", invalidates="+ordinary.body.invalidations+", baseline="+invalidates);
        check(copies==GradientStrokeLineParams.copiesForCheck&&submits==ordinary.proxy.platformAppliesForCheck,"stable normal draws do not copy or submit optical recipes");
        // Heads-up and shade recover their native material; body registration never leaks between scenes.
        com.oplus.systemui.notification.headsup.windowframe.HeadsUpLayout heads=new com.oplus.systemui.notification.headsup.windowframe.HeadsUpLayout();
        ((ViewGroup)ordinary.row).parent=heads;style.scenePaletteChanged(ordinary.ext);Handler.drainForCheck();
        check(!marked(control,ordinary.body)&&gradient(control,ordinary.body)==null,"moving the same ordinary row to heads-up clears lock composition");
        check(draw(control,ordinary.body).lastLayerPaint==null,"heads-up keeps its own native body");
        ((ViewGroup)ordinary.row).parent=null;style.scenePaletteChanged(ordinary.ext);Handler.drainForCheck();
        check(marked(control,ordinary.body),"moving back to lock restores its same common composition");
        style.setKeyguard(false);Handler.drainForCheck();check(!marked(control,ordinary.body)&&gradient(control,ordinary.body)==null,"shade clears ordinary lock ownership");
        control.finishFollowFadeForCheck();normal=draw(control,ordinary.body);check(((android.graphics.PorterDuffColorFilter)normal.lastLayerPaint.getColorFilter()).getColor()==0xb2112233,"shade recovers the user's unified acrylic colour after the existing follow fade");
        style.setKeyguard(true);Handler.drainForCheck();check(marked(control,ordinary.body),"next lock registers the current composition anew");
        options.putBoolean(FluidCloudStyle.NOTIFICATION_FOLLOW,false);style.configure(options);Handler.drainForCheck();
        check(!marked(control,ordinary.body)&&gradient(control,ordinary.body)==null,"turning ordinary follow off forgets the body registration");
        check(ordinary.proxy.appliedStrokeForCheck==ordinary.proxy.blurConfig.gradientStrokeLineParam,"ordinary follow off restores the native stroke parameter");
        control.finishFollowFadeForCheck();normal=draw(control,ordinary.body);
        check(((android.graphics.PorterDuffColorFilter)normal.lastLayerPaint.getColorFilter()).getColor()==0xb2112233,"ordinary follow off recovers the unified colour");
        options.putBoolean(FluidCloudStyle.NOTIFICATION_FOLLOW,true);style.configure(options);Handler.drainForCheck();
        state.dozing=true;style.onNativeState(state);Handler.drainForCheck();style.mediaDisposed(section);Handler.drainForCheck();
        check(!marked(control,ordinary.body)&&gradient(control,ordinary.body)==null,"media disposal in AOD removes ordinary held composition");
        check(ordinary.proxy.appliedStrokeForCheck==ordinary.proxy.blurConfig.gradientStrokeLineParam,"no-media AOD restores ordinary native optical recipe");
        style.release();control.release();accent.release();Handler.drainForCheck();
        check(!marked(control,ordinary.body)&&gradient(control,ordinary.body)==null,"release forgets all ordinary composition ownership");
        System.out.println("OrdinaryLockCompositionCheck: "+checks+" checks passed");
    }
}
