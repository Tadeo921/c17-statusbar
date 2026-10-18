package dev.puitheme;

import android.os.Bundle;
import java.util.Map;

/** Execute the shared production parser and real Appearance construction. */
public final class ClockBackdropStyleCheck {
    private static int checks;
    private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    private static void near(float expected,float value,String why){check(Math.abs(expected-value)<.00001f,why+": "+expected+" != "+value);}
    private static String key(String base,boolean landscape){return landscape?NotificationBigClockSettings.landscapeKey(base):base;}
    private static String prefix(boolean landscape){return landscape?NotificationBigClockSettings.LANDSCAPE_PREFIX:NotificationBigClockSettings.PORTRAIT_PREFIX;}
    private static ClockBackdropStyle style(Bundle source,boolean landscape){return new ClockBackdropStyle(source,prefix(landscape));}
    private static void defaults() {
        for(boolean landscape:new boolean[]{false,true})for(Bundle source:new Bundle[]{null,new Bundle()}){
            ClockBackdropStyle value=style(source,landscape);check(!value.enabled,"missing switch stays disabled");check(value.color==0xffffffff,"default mask is white");near(.2f,value.opacity,"default tint strength");
            ClockBackdropMaterial.Appearance look=value.appearance(1f);check(look.borderEnabled,"default edge is on");near(.6f,look.borderWidthPx,"default edge dp at density one");check(look.borderColor==0xffffffff,"default edge white");near(.6f,look.borderOpacity,"default edge opacity");near(315,look.angleDeg,"default angle");near(.4f,look.highlight,"default high light");near(0,look.shade,"default never adds dark edge");near(1.5f,look.reliefWidthPx,"default relief width");
        }
    }
    private static void directions() {
        Bundle source=new Bundle();
        for(boolean landscape:new boolean[]{false,true}){
            String prefix=prefix(landscape);source.putBoolean(prefix+"digit_reveal_enabled",true);
            source.putInt(prefix+"reveal_mask_color",landscape?0x66556677:0x22123456);source.putBoolean(prefix+"reveal_mask_color_custom_alpha",landscape);source.putFloat(prefix+"reveal_mask_opacity",landscape?80:35);
            source.putBoolean(key(ClockBackdropSettings.EDGE_ENABLED,landscape),landscape);
            source.putFloat(key(ClockBackdropSettings.EDGE_WIDTH,landscape),landscape?3.5f:1.25f);
            source.putInt(key(ClockBackdropSettings.EDGE_COLOR,landscape),landscape?0x88543210:0x33abcdef);source.putBoolean(StatusBarSettings.alphaKey(key(ClockBackdropSettings.EDGE_COLOR,landscape)),!landscape);
            source.putFloat(key(ClockBackdropSettings.EDGE_OPACITY,landscape),landscape?25:75);
            source.putFloat(key(ClockBackdropSettings.HIGHLIGHT_ANGLE,landscape),landscape?120:240);
            source.putFloat(key(ClockBackdropSettings.HIGHLIGHT_STRENGTH,landscape),landscape?10:85);
            source.putFloat(key(ClockBackdropSettings.SHADE_STRENGTH,landscape),landscape?0:18);
            source.putFloat(key(ClockBackdropSettings.DEPTH_WIDTH,landscape),landscape?2.25f:4f);
        }
        ClockBackdropStyle portrait=style(source,false),landscape=style(source,true);check(portrait.enabled&&landscape.enabled,"each direction sees its own enabled switch");
        check(portrait.color==0xff123456&&landscape.color==0x66556677,"mask alpha follows each direction's explicit flag");near(.35f,portrait.opacity,"portrait mask opacity");near(.8f,landscape.opacity,"landscape mask opacity");
        ClockBackdropMaterial.Appearance p=portrait.appearance(2.5f),l=landscape.appearance(1.25f);
        check(!p.borderEnabled&&l.borderEnabled,"edge switches are independent");near(3.125f,p.borderWidthPx,"portrait edge multiplied by actual density once");near(4.375f,l.borderWidthPx,"landscape edge density once");
        check(p.borderColor==0x33abcdef&&l.borderColor==0xff543210,"edge custom-alpha rules are independent");near(.75f,p.borderOpacity,"portrait edge opacity");near(.25f,l.borderOpacity,"landscape edge opacity");
        near(240,p.angleDeg,"portrait angle");near(120,l.angleDeg,"landscape angle");near(.85f,p.highlight,"portrait highlight");near(.1f,l.highlight,"landscape highlight");near(.18f,p.shade,"portrait chosen shade");near(0,l.shade,"landscape no shade");near(10,p.reliefWidthPx,"portrait relief density once");near(2.8125f,l.reliefWidthPx,"landscape relief density once");
        source.putFloat(ClockBackdropSettings.EDGE_WIDTH,4);check(portrait.appearance(2.5f)==p,"source edits cannot mutate an existing settings snapshot");near(3.125f,p.borderWidthPx,"old appearance is immutable");near(10,style(source,false).appearance(2.5f).borderWidthPx,"a new snapshot takes the edited value");
    }
    private static float field(ClockBackdropStyle style,String key) {
        if(ClockBackdropSettings.DATE_MASK_OPACITY.equals(key))return style.dateOpacity*100f;
        if(ClockBackdropSettings.FOOTER_MASK_OPACITY.equals(key))return style.footerOpacity*100f;
        ClockBackdropMaterial.Appearance value=style.appearance(1f);
        if(ClockBackdropSettings.EDGE_WIDTH.equals(key))return value.borderWidthPx;
        if(ClockBackdropSettings.EDGE_OPACITY.equals(key))return value.borderOpacity*100;
        if(ClockBackdropSettings.HIGHLIGHT_ANGLE.equals(key))return value.angleDeg;
        if(ClockBackdropSettings.HIGHLIGHT_STRENGTH.equals(key))return value.highlight*100;
        if(ClockBackdropSettings.SHADE_STRENGTH.equals(key))return value.shade*100;
        return value.reliefWidthPx;
    }
    private static void malformed() {
        Object[] invalid={"2",Boolean.TRUE,new Object(),Float.NaN,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY,Double.NaN,Double.POSITIVE_INFINITY};
        for(boolean landscape:new boolean[]{false,true})for(Map.Entry<String,Float> item:ClockBackdropSettings.NUMBERS.entrySet()){
            String key=key(item.getKey(),landscape);
            for(Object bad:invalid){Bundle source=new Bundle();source.putForWallpaperCheck(key,bad);near(item.getValue(),field(style(source,landscape),item.getKey()),"bad backdrop value falls back at "+key);}
            Bundle source=new Bundle();source.putFloat(key,-1);near(0,field(style(source,landscape),item.getKey()),"finite negative clamps to zero");
            source.putFloat(key,10000);float max=ClockBackdropSettings.maximum(key);near(ClockBackdropSettings.HIGHLIGHT_ANGLE.equals(item.getKey())?0:max,field(style(source,landscape),item.getKey()),"finite overflow clamps once, with 360-degree wrap");
        }
        for(boolean landscape:new boolean[]{false,true})for(Object bad:invalid){
            Bundle source=new Bundle();source.putForWallpaperCheck(prefix(landscape)+"reveal_mask_opacity",bad);near(.2f,style(source,landscape).opacity,"bad mask opacity falls back");
            source.putForWallpaperCheck(key(ClockBackdropSettings.EDGE_COLOR,landscape),bad);source.putForWallpaperCheck(prefix(landscape)+"reveal_mask_color",bad);ClockBackdropStyle value=style(source,landscape);check(value.color==0xffffffff&&value.appearance(1).borderColor==0xffffffff,"non-integer colors use white fallback");
        }
        Bundle source=new Bundle();source.putForWallpaperCheck(ClockBackdropSettings.EDGE_ENABLED,"false");source.putForWallpaperCheck(NotificationBigClockSettings.DIGIT_REVEAL_ENABLED,"true");ClockBackdropStyle value=style(source,false);check(!value.enabled&&value.appearance(1).borderEnabled,"Boolean flags do not coerce strings");
    }
    private static void colorAndSafety() {
        for(boolean landscape:new boolean[]{false,true})for(boolean alpha:new boolean[]{false,true})for(int color:new int[]{0x00123456,0x80123456,0xff123456}){
            Bundle source=new Bundle();String mask=prefix(landscape)+"reveal_mask_color",edge=key(ClockBackdropSettings.EDGE_COLOR,landscape);
            source.putInt(mask,color);source.putBoolean(mask+"_custom_alpha",alpha);source.putFloat(prefix(landscape)+"reveal_mask_opacity",25);
            source.putInt(edge,color);source.putBoolean(edge+"_custom_alpha",alpha);source.putFloat(key(ClockBackdropSettings.EDGE_OPACITY,landscape),25);
            ClockBackdropStyle value=style(source,landscape);int expected=alpha?color:color|0xff000000;check(value.color==expected&&value.appearance(1).borderColor==expected,"selected color alpha is preserved only when requested");near(.25f,value.opacity,"mask alpha is not premultiplied into opacity twice");near(.25f,value.appearance(1).borderOpacity,"edge alpha remains independent from edge opacity");
        }
        Bundle source=new Bundle();source.putBoolean(NotificationBigClockSettings.DIGIT_REVEAL_ENABLED,true);source.putBoolean(NotificationBigClockSettings.landscapeKey(NotificationBigClockSettings.DIGIT_REVEAL_ENABLED),true);source.putBoolean(StatusBarSettings.SAFE_MODE,true);
        check(!style(source,false).enabled&&!style(source,true).enabled,"safe mode suspends both directions");check(source.getBoolean(NotificationBigClockSettings.DIGIT_REVEAL_ENABLED)&&source.getBoolean(NotificationBigClockSettings.landscapeKey(NotificationBigClockSettings.DIGIT_REVEAL_ENABLED)),"safe mode parser never rewrites saved material switches");
        source.putBoolean(StatusBarSettings.SAFE_MODE,false);source.putBoolean(NotificationBigClockSettings.DIGIT_REVEAL_ENABLED,false);check(!style(source,false).enabled&&style(source,true).enabled,"one direction does not enable the other");
    }
    private static void densityCache() {
        ClockBackdropStyle value=style(new Bundle(),false);ClockBackdropMaterial.Appearance first=value.appearance(2f);
        for(int i=0;i<128;i++)check(value.appearance(2f)==first,"stable density shares exact Appearance");near(1.2f,first.borderWidthPx,"first edge conversion");near(3f,first.reliefWidthPx,"first relief conversion");
        ClockBackdropMaterial.Appearance second=value.appearance(3f);check(second!=first,"density change creates updated immutable appearance");near(1.8f,second.borderWidthPx,"new edge density");near(4.5f,second.reliefWidthPx,"new relief density");near(1.2f,first.borderWidthPx,"prior density appearance remains unchanged");
        ClockBackdropMaterial.Appearance fallback=value.appearance(Float.NaN);for(float density:new float[]{0,-2,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY,Float.NaN,1})check(value.appearance(density)==fallback,"invalid display density normalises to one and shares cached appearance");near(.6f,fallback.borderWidthPx,"fallback density applied once");near(1.5f,fallback.reliefWidthPx,"fallback relief applied once");
    }
    public static void main(String[] args){defaults();directions();malformed();colorAndSafety();densityCache();System.out.println("ClockBackdropStyleCheck: "+checks+" checks passed");}
}
