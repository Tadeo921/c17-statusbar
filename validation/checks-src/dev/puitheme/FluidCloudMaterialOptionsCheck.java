package dev.puitheme;

import android.os.Bundle;
import android.os.Handler;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import com.heytap.log.nx.obus.a;
import com.oplus.systemui.plugins.seedling.card.ui.view.CardBackgroundView;
import com.oplus.systemui.plugins.seedling.card.ui.view.CardView;
import com.oplus.systemui.plugins.shared.template.section.media.K0;
import com.oplus.systemui.plugins.shared.template.section.media.s1;
import com.oplus.view.ViewRootManager;

/** Real policy paths: opacity once, live media ownership, neutral foreground restore. */
public final class FluidCloudMaterialOptionsCheck {
    private static int checks;
    private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    private static final class Resources extends android.content.res.Resources {
        int notification=0xffeeeeee;
        @Override public String getResourceEntryName(int id){return id==1?"card_background_mix_color":"card_background_gradient_middle_color";}
        @Override public int getIdentifier(String name,String type,String pkg){return name.equals("notification_material_background_color_compass")?3:0;}
        @Override public int getColor(int id,Theme theme){return notification;}
    }
    public static void main(String[] args)throws Exception {
        FluidCloudStyle style=new FluidCloudStyle();FluidCloudAccent accent=new FluidCloudAccent();
        ClassLoader loader=FluidCloudMaterialOptionsCheck.class.getClassLoader();style.resolve(loader);style.setAccentController(accent);accent.resolve(loader);
        Resources resources=new Resources();CardView card=new CardView();CardBackgroundView host=new CardBackgroundView(resources);card.addView(host);
        ViewRootManager manager=new ViewRootManager();a helper=new a(null,3);a.style=style;a.controller=accent;ViewRootManager.style=style;
        helper.b(host,manager);
        Bundle settings=new Bundle();settings.putBoolean(FluidCloudStyle.MASTER,true);settings.putBoolean(FluidCloudStyle.DESKTOP_BACKGROUND,true);settings.putString(FluidCloudStyle.BACKGROUND_MODE,"native");
        style.configure(settings);Handler.drainForCheck();style.finishFillFadesForCheck();check(a.mixed==0xb3262626,"new native source keeps system default fill");
        settings.putFloat(FluidCloudStyle.BACKGROUND_OPACITY,50f);style.configure(settings);Handler.drainForCheck();style.finishFillFadesForCheck();
        check(a.mixed==0x5a262626,"native source 50 percent retains RGB and applies resource alpha once");
        check(Math.abs(manager.saved[7]-90f/255)<.00001f,"resource native alpha is not squared by hardware parameters");
        settings.putFloat(FluidCloudStyle.BACKGROUND_OPACITY,0f);style.configure(settings);Handler.drainForCheck();style.finishFillFadesForCheck();
        check(a.mixed==0x00262626&&manager.saved[7]==0f,"native source zero removes fill without replacing RGB");
        settings.putFloat(FluidCloudStyle.BACKGROUND_OPACITY,50f);style.configure(settings);Handler.drainForCheck();style.finishFillFadesForCheck();
        com.oplus.graphics.OplusBlurParam hardwareOnly=new com.oplus.graphics.OplusBlurParam();
        float[] sourceParams=hardwareOnly.toFloatArray();Object firstHardware;
        try(FluidCloudStyle.BackgroundScope scope=style.beginBackground(helper,host,manager)) {
            firstHardware=style.blurParams(manager,hardwareOnly);
            float[] applied=((com.oplus.graphics.OplusBlurParam)firstHardware).toFloatArray();
            check(firstHardware!=hardwareOnly,"hardware-only opacity derives a private native parameter");
            for(int offset:new int[]{7,11,15})check(Math.abs(applied[offset]-sourceParams[offset]*.5f)<.00001f,"hardware-only native alpha multiplied once at "+offset);
            check(java.util.Arrays.equals(hardwareOnly.toFloatArray(),sourceParams),"native hardware parameter array remains unchanged");
            check(style.blurParams(manager,hardwareOnly)==firstHardware,"stable hardware inputs reuse the private derived parameter");
            scope.completed();
        }
        settings.putFloat(FluidCloudStyle.BACKGROUND_OPACITY,0f);style.configure(settings);Handler.drainForCheck();style.finishFillFadesForCheck();
        try(FluidCloudStyle.BackgroundScope scope=style.beginBackground(helper,host,manager)) {
            float[] applied=((com.oplus.graphics.OplusBlurParam)style.blurParams(manager,hardwareOnly)).toFloatArray();
            check(applied[7]==0f&&applied[11]==0f&&applied[15]==0f,"hardware-only zero opacity clears all native fill alpha");
            check(applied[3]==sourceParams[3]&&applied[16]==sourceParams[16]&&applied[17]==sourceParams[17],"native blend mode and geometry retained at zero opacity");
            scope.completed();
        }
        settings.putString(FluidCloudStyle.BACKGROUND_MODE,"custom");settings.putInt(FluidCloudStyle.BACKGROUND_COLOR,0xffa04020);settings.putFloat(FluidCloudStyle.BACKGROUND_OPACITY,50f);
        style.configure(settings);Handler.drainForCheck();style.finishFillFadesForCheck();check(a.mixed==0x5aa04020,"custom opacity multiplies native alpha once");
        check(Math.abs(manager.saved[7]-90f/255)<.00001f,"native blur param doesn't square resource-substituted opacity");
        settings.putInt(FluidCloudStyle.BACKGROUND_COLOR,0x80a04020);settings.putBoolean(StatusBarSettings.alphaKey(FluidCloudStyle.BACKGROUND_COLOR),true);settings.putFloat(FluidCloudStyle.BACKGROUND_OPACITY,50f);
        style.configure(settings);Handler.drainForCheck();style.finishFillFadesForCheck();check(a.mixed==0x2da04020,"color custom alpha and fill opacity combine once");
        try(FluidCloudStyle.BackgroundScope scope=style.beginBackground(helper,host,manager)) {
            float[] applied=((com.oplus.graphics.OplusBlurParam)style.blurParams(manager,hardwareOnly)).toFloatArray();
            check(Math.abs(applied[7]-sourceParams[7]*(64f/255))<.00001f,"hardware-only custom fill alpha is applied exactly once with native integer alpha rounding");
        }
        settings.putInt(FluidCloudStyle.BACKGROUND_COLOR,FluidCloudStyle.DEFAULT_BACKGROUND_COLOR);settings.putBoolean(StatusBarSettings.alphaKey(FluidCloudStyle.BACKGROUND_COLOR),false);
        settings.putString(FluidCloudStyle.BACKGROUND_MODE,"notification");settings.putFloat(FluidCloudStyle.BACKGROUND_OPACITY,100f);style.configure(settings);Handler.drainForCheck();style.finishFillFadesForCheck();
        check(a.mixed==0xb3262626,"the retired notification source falls back to the system style");
        settings.putString(FluidCloudStyle.BACKGROUND_MODE,"media");style.configure(settings);Handler.drainForCheck();style.finishFillFadesForCheck();
        check(a.mixed==0xb3262626,"non-media source absence keeps native color instead of fake black");
        settings.putBoolean(FluidCloudAccent.ENABLED,true);accent.configure(settings);style.configure(settings);Handler.drainForCheck();style.finishFillFadesForCheck();
        ViewGroup media=new ViewGroup(null);card.addView(media);kotlinx.coroutines.flow.p flow=new kotlinx.coroutines.flow.p(new com.oplus.systemui.plugins.shared.data.media.model.a(0xff307090));
        s1 section=new s1(null,new K0(flow),media,null);accent.mediaChanged(section);style.refreshAfterAccent();Handler.drainForCheck();
        check(a.mixed==0xb3307090,"native media final color is available through the media-accent page");
        flow.value=new com.oplus.systemui.plugins.shared.data.media.model.a(0xffd08040);accent.mediaChanged(section);style.refreshAfterAccent();Handler.drainForCheck();
        check(a.mixed==0xb3d08040,"new native color event replays source once");
        CardView timerCard=new CardView();CardBackgroundView timerHost=new CardBackgroundView(resources);timerCard.addView(timerHost);
        ViewRootManager timerManager=new ViewRootManager();helper.b(timerHost,timerManager);
        check(a.mixed==0xb3d08040&&accent.nativeBackgroundMediaColor(timerHost)==null,"non-media desktop card follows current media fill without borrowing accent ownership");
        // code100: following also needs the style page's custom base colour switched on.
        Bundle independent=new Bundle();independent.putBoolean(FluidCloudStyle.FOLLOW_MEDIA,true);independent.putBoolean(FluidCloudAccent.ENABLED,true);
        independent.putBoolean(FluidCloudStyle.MASTER,true);independent.putBoolean(FluidCloudStyle.DESKTOP_BACKGROUND,true);
        independent.putString(FluidCloudStyle.BACKGROUND_MODE,"native");
        Bundle sharedMask=new Bundle();sharedMask.putBoolean(FluidCloudAccent.MASK_ENABLED,true);sharedMask.putBoolean(FluidCloudAccent.ENABLED,true);
        sharedMask.putInt(FluidCloudAccent.MASK_COLOR,0xff000000);sharedMask.putFloat(FluidCloudAccent.MASK_OPACITY,50f);
        accent.configure(sharedMask);style.configure(independent);Handler.drainForCheck();style.finishFillFadesForCheck();
        check(a.mixed==0xb3684020,"media fill follows with the page master on and shares its mask");
        Bundle mediaWithOpacity=new Bundle(independent);mediaWithOpacity.putBoolean(FluidCloudStyle.MASTER,true);
        mediaWithOpacity.putBoolean(FluidCloudStyle.DESKTOP_BACKGROUND,true);mediaWithOpacity.putString(FluidCloudStyle.BACKGROUND_MODE,"native");
        mediaWithOpacity.putFloat(FluidCloudStyle.BACKGROUND_OPACITY,50f);style.configure(mediaWithOpacity);Handler.drainForCheck();style.finishFillFadesForCheck();
        check(a.mixed==0xb3684020,"the style page's opacity does not touch the media colour");
        sharedMask.putFloat(FluidCloudAccent.OPACITY,50f);accent.configure(sharedMask);style.configure(mediaWithOpacity);Handler.drainForCheck();style.finishFillFadesForCheck();
        check(a.mixed==0x5a684020,"the media colour shares the media mask and multiplies the media-accent page's own opacity once");
        check(Math.abs(timerManager.saved[7]-90f/255)<.00001f,"independent media resource alpha is not squared by the hardware path");
        settings.putBoolean(FluidCloudAccent.ENABLED,true);settings.putFloat(FluidCloudAccent.OPACITY,50f);accent.configure(settings);style.configure(mediaWithOpacity);Handler.drainForCheck();style.finishFillFadesForCheck();
        check(a.mixed==0x5ad08040,"live media source remains selectable while independent accent owns the same music background");
        accent.configure(sharedMask);style.configure(independent);Handler.drainForCheck();style.finishFillFadesForCheck();
        check(accent.enabled()&&a.mixed==0x5a684020,"turning the appearance background OFF keeps the media colour and the media-accent page's own opacity and removes only the style page's policy");
        sharedMask.putBoolean(FluidCloudAccent.MASK_ENABLED,false);accent.configure(sharedMask);
        style.configure(settings);Handler.drainForCheck();style.finishFillFadesForCheck();
        long revision=accent.colorRevision();int pending=Handler.postedForCheck.size();
        for(int i=0;i<1000;i++){accent.mediaChanged(section);style.refreshAfterAccent();}
        check(accent.colorRevision()==revision&&Handler.postedForCheck.size()==pending,"stable native media events neither change revision nor enqueue refresh batches");
        flow.value=new com.oplus.systemui.plugins.shared.data.media.model.a(0xff4080c0);accent.mediaChanged(section);Handler.drainForCheck();style.finishFillFadesForCheck();
        check(Math.abs(timerManager.saved[4]-64f/255)<.00001f&&Math.abs(timerManager.saved[6]-192f/255)<.00001f,"non-media current fill updates from native event with accent switch OFF");
        timerManager.saved=null;
        media.attached=false;for(android.view.View.OnAttachStateChangeListener listener:new java.util.ArrayList<>(media.attachListeners))listener.onViewDetachedFromWindow(media);Handler.drainForCheck();style.finishFillFadesForCheck();
        check(accent.nativeCurrentMediaColor(timerHost)==null&&Math.abs(timerManager.saved[4]-38f/255)<.00001f,"last live media detach restores native fill rather than a stale cached album");
        media.attached=true;for(android.view.View.OnAttachStateChangeListener listener:new java.util.ArrayList<>(media.attachListeners))listener.onViewAttachedToWindow(media);Handler.drainForCheck();
        check(accent.nativeCurrentMediaColor(timerHost)==0xff4080c0,"native attach restores current colour without polling or artwork extraction");
        s1 replacement=new s1(null,section.k,media,null);accent.mediaChanged(replacement);accent.mediaDisposed(section);Handler.drainForCheck();
        check(accent.nativeBackgroundMediaColor(host)==0xff4080c0,"late disposal cannot remove a replacement section using the same root");
        section=replacement;flow.value=new com.oplus.systemui.plugins.shared.data.media.model.a(0xffd08040);accent.mediaChanged(section);Handler.drainForCheck();
        try(FluidCloudAccent.NativeRestoreScope nativeAccent=accent.beginNativeRestore();FluidCloudStyle.NativeRestoreScope nativeStyle=style.beginNativeRestore()){
            style.detach(timerHost);accent.detach(timerHost);
        }
        int builds=a.builds;for(int i=0;i<1000;i++){style.refreshAfterAccent();Handler.drainForCheck();}check(a.builds==builds,"identical media events don't repeatedly rebuild native glass");
        settings.putFloat(FluidCloudAccent.OPACITY,100f);settings.putBoolean(FluidCloudAccent.ENABLED,true);accent.configure(settings);settings.putString(FluidCloudStyle.BACKGROUND_MODE,"custom");settings.putInt(FluidCloudStyle.BACKGROUND_COLOR,0xff000000);style.configure(settings);Handler.drainForCheck();style.finishFillFadesForCheck();
        check(a.mixed==0xb3d08040&&accent.nativeBackgroundMediaColor(host)==0xffd08040,
                "an active media accent outranks the explicit custom fill while the live native media source stays available");
        settings.putBoolean(C17HighlightRemoval.ENABLED,true);settings.putBoolean(FluidCloudStyle.MEDIA_KEEP_HIGHLIGHT,true);style.configure(settings);Handler.drainForCheck();style.finishFillFadesForCheck();
        check(manager.saved[3]==4f,"keep-highlights applies only to recognized music owner");
        accent.mediaDisposed(section);style.refreshAfterAccent();Handler.drainForCheck();check(manager.saved[3]==2f,"recycled non-media card restores normal highlight removal");
        settings.putBoolean(C17HighlightRemoval.ENABLED,false);style.configure(settings);Handler.drainForCheck();style.finishFillFadesForCheck();check(manager.saved[3]==4f,"dependent keep-highlights OFF cannot activate removal");
        GradientDrawable nativeShape=new GradientDrawable();nativeShape.setColor(0xb3223344);nativeShape.setCornerRadius(36);nativeShape.setStroke(2,0x40ffffff);nativeShape.setAlpha(123);
        GradientDrawable copy=(GradientDrawable)FluidCloudFallback.material(nativeShape,resources,0x804080c0,false,false);
        check(copy.getColor().getDefaultColor()==0x5a4080c0&&copy.getAlpha()==123&&copy.getCornerRadius()==36,"static fallback uses same one-time alpha recipe and shape");
        check(nativeShape.getColor().getDefaultColor()==0xb3223344,"static source remains shared/native");
        GradientDrawable nativeOpacity=(GradientDrawable)FluidCloudFallback.opacity(nativeShape,resources,.5f);
        check(nativeOpacity!=nativeShape&&nativeOpacity.getColor().getDefaultColor()==0x5a223344,"native fallback opacity retains native RGB and multiplies alpha once");
        check(nativeOpacity.getAlpha()==123&&nativeOpacity.getCornerRadius()==36&&nativeOpacity.mStrokePaint.strokeWidth==2&&nativeOpacity.mStrokePaint.getColor()==0x40ffffff,"native fallback retains animation alpha, corners and stroke");
        check(nativeShape.getColor().getDefaultColor()==0xb3223344&&nativeShape.getAlpha()==123,"native fallback source is not changed");
        GradientDrawable transparent=(GradientDrawable)FluidCloudFallback.opacity(nativeShape,resources,0f);
        check(transparent.getColor().getDefaultColor()==0x00223344&&transparent.getAlpha()==123,"fallback zero alpha does not replace RGB or animation alpha");
        check(FluidCloudFallback.opacity(nativeShape,resources,1f)==nativeShape&&FluidCloudFallback.opacity(nativeShape,resources,Float.NaN)==nativeShape,"identity and invalid fallback alpha retain native object");
        GradientDrawable stops=new GradientDrawable();stops.setColors(new int[]{0xb3112233,0x80556677,0x00778899});stops.setCornerRadius(31);stops.setAlpha(201);
        int[] nativeStops=stops.getColors();GradientDrawable scaledStops=(GradientDrawable)FluidCloudFallback.opacity(stops,resources,.5f);
        check(java.util.Arrays.equals(scaledStops.getColors(),new int[]{0x5a112233,0x40556677,0x00778899}),"native fallback scales each gradient stop while preserving RGB");
        check(java.util.Arrays.equals(stops.getColors(),nativeStops)&&scaledStops.getAlpha()==201&&scaledStops.getCornerRadius()==31,"gradient source, radius and animation alpha retained");
        TextView nativeTitle=new TextView(new android.content.Context());nativeTitle.setTextColor(0xe6ffffff);card.addView(nativeTitle);
        ImageView nativeIcon=new ImageView(new android.content.Context());ColorStateList nativeTint=ColorStateList.valueOf(0xccffffff);nativeIcon.setImageTintList(nativeTint);card.addView(nativeIcon);
        settings.putBoolean(FluidCloudStyle.FOLLOW_FOREGROUND,true);style.configure(settings);Handler.drainForCheck();style.finishFillFadesForCheck();
        check(nativeTitle.getCurrentTextColor()==0xe6ffffff&&nativeIcon.getImageTintList()==nativeTint,"saved retired foreground flag leaves active native card text and icon tint untouched");
        check(style.foregroundTextColor(nativeTitle,0xe6ffffff)==0xe6ffffff&&style.foregroundColor(nativeIcon,nativeTint)==nativeTint,"retired foreground bridge preserves new OEM setter inputs");
        settings.putBoolean(StatusBarSettings.SAFE_MODE,true);style.configure(settings);Handler.drainForCheck();style.finishFillFadesForCheck();check(!style.enabled()&&manager.saved[3]==4f,"safe mode disables all new policies and restores native style");
        style.release();accent.release();Handler.drainForCheck();a.style=null;a.controller=null;ViewRootManager.style=null;
        nativeExplicitColor();accentStaticFallback();foreground();
        System.out.println("FluidCloudMaterialOptionsCheck: "+checks+" checks passed");
    }
    private static void nativeExplicitColor()throws Exception {
        FluidCloudStyle style=new FluidCloudStyle();style.resolve(FluidCloudMaterialOptionsCheck.class.getClassLoader());
        Resources resources=new Resources();CardView card=new CardView();CardBackgroundView host=new CardBackgroundView(resources);card.addView(host);
        a helper=new a(null,3);ViewRootManager manager=new ViewRootManager();a.style=style;a.controller=null;ViewRootManager.style=style;
        Bundle settings=new Bundle();settings.putBoolean(FluidCloudStyle.MASTER,true);settings.putBoolean(FluidCloudStyle.DESKTOP_BACKGROUND,true);
        settings.putString(FluidCloudStyle.BACKGROUND_MODE,"native");settings.putInt(FluidCloudStyle.BACKGROUND_COLOR,FluidCloudStyle.DEFAULT_BACKGROUND_COLOR);
        settings.putBoolean(FluidCloudStyle.BACKGROUND_COLOR_CUSTOMIZED,false);style.configure(settings);Handler.drainForCheck();style.finishFillFadesForCheck();helper.b(host,manager);
        check(a.mixed==0xb3262626&&a.gradient==0xff000000,"unedited stored default colour does not override native system material");
        settings.putBoolean(FluidCloudStyle.BACKGROUND_COLOR_CUSTOMIZED,true);style.configure(settings);Handler.drainForCheck();style.finishFillFadesForCheck();
        check(a.mixed==0xb3303438&&a.gradient==0xff000000,"an explicitly selected native-mode base colour still tints the material while the original gradient keeps its own RGB");
        settings.putFloat(FluidCloudStyle.BACKGROUND_OPACITY,50f);style.configure(settings);Handler.drainForCheck();style.finishFillFadesForCheck();
        check(a.mixed==0x5a303438&&a.gradient==0xff000000,"native mode explicit base RGB and opacity affect the material once while the original gradient stays independent");
        check(Math.abs(manager.saved[7]-90f/255)<.00001f&&manager.saved[3]==4f&&manager.blurRadius==540,
                "native RGB adjustment retains material blend type and radius without alpha being applied twice");
        settings.putBoolean(FluidCloudStyle.BACKGROUND_COLOR_CUSTOMIZED,false);settings.putInt(FluidCloudStyle.BACKGROUND_COLOR,0xff406080);
        style.configure(settings);Handler.drainForCheck();style.finishFillFadesForCheck();
        check(a.mixed==0x5a406080,"previous non-default saved RGB is compatible without requiring a new edit marker");
        com.oplus.graphics.OplusBlurParam source=new com.oplus.graphics.OplusBlurParam();float[] original=source.toFloatArray();
        try(FluidCloudStyle.BackgroundScope scope=style.beginBackground(helper,host,manager)) {
            Object transformed=style.blurParams(manager,source);float[] derived=((com.oplus.graphics.OplusBlurParam)transformed).toFloatArray();
            check(transformed!=source&&java.util.Arrays.equals(source.toFloatArray(),original),"native explicit hardware material derives a private unmodified source");
            for(int offset:new int[]{4,8,12}) {
                check(Math.abs(derived[offset]-64f/255)<.00001f&&Math.abs(derived[offset+1]-96f/255)<.00001f&&Math.abs(derived[offset+2]-128f/255)<.00001f,
                        "native explicit RGB covers each hardware material stop at "+offset);
                check(Math.abs(derived[offset+3]-original[offset+3]*(128f/255))<.00001f,
                        "native explicit hardware alpha multiplies configured opacity once at "+offset);
            }
            check(derived[0]==original[0]&&derived[1]==original[1]&&derived[2]==original[2]&&derived[3]==original[3]
                    &&derived[16]==original[16]&&derived[17]==original[17],"native explicit hardware colour keeps OEM blur type and geometry");
            check(style.blurParams(manager,source)==transformed,"unchanged native explicit hardware colour reuses one private parameter");scope.completed();
        }
        CardView fallbackCard=new CardView();CardBackgroundView fallback=new CardBackgroundView(resources);fallbackCard.addView(fallback);helper.b(fallback,null);
        GradientDrawable fill=(GradientDrawable)fallback.getBackground();
        check(fill.getColor().getDefaultColor()==0x5a406080&&fill.getCornerRadius()==36f&&fill.mStrokePaint.strokeWidth==2
                &&fill.mStrokePaint.getColor()==0x40ffffff,"manager-null initializer applies native custom RGB and opacity while preserving shape and border");
        settings.putInt(FluidCloudStyle.BACKGROUND_COLOR,0x80303438);settings.putBoolean(StatusBarSettings.alphaKey(FluidCloudStyle.BACKGROUND_COLOR),true);
        style.configure(settings);Handler.drainForCheck();style.finishFillFadesForCheck();
        check(a.mixed==0x2d303438&&a.gradient==0xff000000,"saved native-mode base alpha combines with material opacity once without tinting or fading the independent gradient");
        check(((GradientDrawable)fallback.getBackground()).getColor().getDefaultColor()==0x2d303438,
                "native custom alpha and opacity use the same recipe in the actual static initializer");
        settings.putBoolean(FluidCloudStyle.DESKTOP_BACKGROUND,false);style.configure(settings);Handler.drainForCheck();style.finishFillFadesForCheck();
        check(a.mixed==0xb3262626&&a.gradient==0xff000000&&manager.saved[3]==4f,
                "turning native appearance OFF restores native RGB and both original resource alphas");
        check(((GradientDrawable)fallback.getBackground()).getColor().getDefaultColor()==0xb3262626,
                "turning native appearance OFF restores original manager-null fill rather than a scaled copy");
        style.release();Handler.drainForCheck();a.style=null;ViewRootManager.style=null;
    }
    private static void accentStaticFallback()throws Exception {
        FluidCloudStyle style=new FluidCloudStyle();FluidCloudAccent accent=new FluidCloudAccent();
        ClassLoader loader=FluidCloudMaterialOptionsCheck.class.getClassLoader();style.resolve(loader);accent.resolve(loader);style.setAccentController(accent);
        Resources resources=new Resources();CardView card=new CardView();CardBackgroundView host=new CardBackgroundView(resources);card.addView(host);
        a helper=new a(null,3);a.style=style;a.controller=accent;ViewRootManager.style=style;helper.b(host,null);
        ViewGroup media=new ViewGroup(null);card.addView(media);
        kotlinx.coroutines.flow.p flow=new kotlinx.coroutines.flow.p(new com.oplus.systemui.plugins.shared.data.media.model.a(0xffc02060));
        s1 section=new s1(null,new K0(flow),media,null);accent.mediaChanged(section);
        Bundle accentSettings=new Bundle();accentSettings.putBoolean(FluidCloudAccent.ENABLED,true);accentSettings.putFloat(FluidCloudAccent.OPACITY,50f);accent.configure(accentSettings);Handler.drainForCheck();
        Bundle appearance=new Bundle();appearance.putBoolean(FluidCloudStyle.MASTER,true);appearance.putBoolean(FluidCloudStyle.DESKTOP_BACKGROUND,true);
        appearance.putString(FluidCloudStyle.BACKGROUND_MODE,"native");appearance.putFloat(FluidCloudStyle.BACKGROUND_OPACITY,50f);
        style.configure(appearance);Handler.drainForCheck();style.finishFillFadesForCheck();
        check(((GradientDrawable)host.getBackground()).getColor().getDefaultColor()==0x5ac02060,
                "manager-null music accent keeps album RGB but obeys the media-accent page opacity after accent completion");
        appearance.putString(FluidCloudStyle.BACKGROUND_MODE,"custom");appearance.putInt(FluidCloudStyle.BACKGROUND_COLOR,0xff4080c0);
        style.configure(appearance);Handler.drainForCheck();style.finishFillFadesForCheck();
        check(((GradientDrawable)host.getBackground()).getColor().getDefaultColor()==0x5ac02060,
                "manager-null: the media accent outranks an explicit custom fill and the opacity still applies once");
        flow.value=new com.oplus.systemui.plugins.shared.data.media.model.a(0xff204080);accent.mediaChanged(section);style.refreshAfterAccent();Handler.drainForCheck();
        check(((GradientDrawable)host.getBackground()).getColor().getDefaultColor()==0x5a204080&&accent.nativeBackgroundMediaColor(host)==0xff204080,
                "manager-null accent follows the new song while the custom fill stays saved underneath");
        appearance.putBoolean(FluidCloudStyle.DESKTOP_BACKGROUND,false);style.configure(appearance);Handler.drainForCheck();style.finishFillFadesForCheck();
        check(((GradientDrawable)host.getBackground()).getColor().getDefaultColor()==0xb3204080,
                "manager-null appearance OFF restores the newest captured album and original alpha");
        accent.configure(new Bundle());style.refreshAfterAccent();Handler.drainForCheck();
        check(((GradientDrawable)host.getBackground()).getColor().getDefaultColor()==0xb3262626,
                "manager-null accent OFF finally restores the original native static material");
        style.release();accent.release();Handler.drainForCheck();a.style=null;a.controller=null;ViewRootManager.style=null;
    }
    private static void foreground(){
        FluidCloudForeground controller=new FluidCloudForeground();ViewGroup root=new ViewGroup(new android.content.Context());
        TextView title=new TextView(new android.content.Context());title.setTextColor(0xe6ffffff);ColorStateList original=title.getTextColors();root.addView(title);
        TextView disabled=new TextView(new android.content.Context());disabled.setTextColor(0x8affffff);root.addView(disabled);
        TextView semantic=new TextView(new android.content.Context());semantic.setTextColor(0xff2060ff);root.addView(semantic);
        ImageView symbol=new ImageView(new android.content.Context());symbol.setImageTintList(ColorStateList.valueOf(0xccffffff));ColorStateList iconNative=symbol.getImageTintList();root.addView(symbol);
        ImageView cover=new ImageView(new android.content.Context());GradientDrawable artwork=new GradientDrawable();artwork.setColor(0xffda3080);cover.setImageDrawable(artwork);root.addView(cover);
        controller.policy(true);controller.register(root,0xff000000);
        check(title.getCurrentTextColor()==0xe6000000&&disabled.getCurrentTextColor()==0x8a000000,"system dark foreground keeps original title/secondary opacity");
        check(symbol.getImageTintList().getDefaultColor()==0xcc000000&&cover.getDrawable()==artwork&&cover.getImageTintList()==null,"only pre-existing native monochrome icon tint changes, album untouched");
        check(semantic.getCurrentTextColor()==0xff2060ff,"blue action/color semantics are preserved");
        ColorStateList unchanged=title.getTextColors();controller.register(root,0xff000000);check(title.getTextColors()==unchanged,"stable event performs no redundant text writes");
        controller.register(root,0xffffffff);check(title.getCurrentTextColor()==0xe6ffffff,"theme change restores white using original native alpha");
        title.setTextColor(0xff10f040);controller.invalidate(root);controller.register(root,0xff000000);controller.policy(false);
        check(title.getCurrentTextColor()==0xff10f040&&symbol.getImageTintList()==iconNative,"native rebind color wins over stale restoration owner");
        disabled.setTextColor(0xccffffff);controller.policy(true);controller.invalidate(root);controller.register(root,0xff000000);controller.detach(null);check(disabled.getCurrentTextColor()==0xcc000000,"null root detach never clears other owners");
        controller.detach(root);check(disabled.getCurrentTextColor()==0xccffffff,"actual root detach restores its latest original color");
        controller.release();
    }
}
