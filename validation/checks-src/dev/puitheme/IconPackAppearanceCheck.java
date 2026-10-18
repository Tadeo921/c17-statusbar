package dev.puitheme;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.Bundle;
import java.util.*;
import com.oplus.systemui.statusbar.pipeline.battery.ui.drawable.HorizontalBatteryContentDrawable;
import com.oplus.systemui.statusbar.pipeline.battery.ui.view.StatBatteryMeterView;

/** The real status-bar drawing adapter consumes appearance-only Runtime records.
 * No archive, bitmap, package source, or always-success renderer is inserted. */
public final class IconPackAppearanceCheck {
    private static int checks;
    private static void equal(Object expected,Object actual){checks++;if(!Objects.equals(expected,actual))throw new AssertionError("Status appearance "+checks+": "+expected+" != "+actual);}
    private static String record(String saved,String key,String mode,int light,int dark,float alpha)throws Exception{return IconManagementSettings.applyAppearance(saved,key,new IconManagementSettings.Appearance(mode,light,dark,alpha));}
    private static void configure(String saved){Bundle settings=new Bundle();settings.putString(IconManagementSettings.KEY,saved);IconManagementRuntime.configure(new Context(),settings);}
    private static ColorFilter tint(int value){return new PorterDuffColorFilter(value,PorterDuff.Mode.SRC_IN);}
    private static IconNativeAppearanceCheck.Glyph glyph(int alpha,int color){return new IconNativeAppearanceCheck.Glyph(alpha,tint(color));}
    private static void signals()throws Exception{
        String records=record(IconManagementSettings.DEFAULT,"scope:wifi","custom",0xff123456,0xffabcdef,50);records=record(records,"role:cellular.none","system",0xff123456,0xffabcdef,25);configure(records);
        IconPackDrawing drawing=new IconPackDrawing();IconNativeAppearanceCheck.Glyph wifi=glyph(200,0x80112233);wifi.setBounds(4,8,62,34);Canvas canvas=new Canvas();ColorFilter original=wifi.filter;
        equal(true,drawing.drawSignal("stat_signal_wifi_signal_4",wifi,canvas,0x80112233,200,original,false));equal(50,wifi.drawAlpha);equal(0xff123456,IconManagementRuntime.filterColor(wifi.drawFilter));equal(200,wifi.alpha);equal(original,wifi.filter);equal(4,wifi.getBounds().left);
        wifi.filter=tint(0xffeeeeee);equal(true,drawing.drawSignal("stat_signal_wifi_signal_4",wifi,canvas,0xffeeeeee,200,wifi.filter,false));equal(0xffabcdef,IconManagementRuntime.filterColor(wifi.drawFilter));
        IconNativeAppearanceCheck.Glyph none=glyph(180,0xff445566);equal(true,drawing.drawSignal("stat_sys_signal_null",none,canvas,0xff445566,180,none.filter,false));equal(45,none.drawAlpha);equal(none.filter,none.drawFilter);equal(180,none.alpha);
        equal(false,drawing.drawSignal("stat_signal_activity_wifi_none",wifi,canvas,0xffffffff,200,null,false));equal(false,drawing.drawSignal("stat_signal_lte_signal_stacked_secondary_4",none,canvas,0xffffffff,180,null,true));
        int before=wifi.draws;configure(IconManagementSettings.DEFAULT);equal(false,drawing.drawSignal("stat_signal_wifi_signal_4",wifi,canvas,0xffffffff,200,null,false));equal(before,wifi.draws);
        drawing.releaseRuntime();
    }
    private static void hints()throws Exception{
        String records=record(IconManagementSettings.DEFAULT,"role:hint.bluetooth.off","custom",0xff123456,0xffabcdef,50);records=record(records,"role:hint.location.default","system",0xff123456,0xffabcdef,25);configure(records);
        IconPackDrawing drawing=new IconPackDrawing();IconPackDrawingCheck.HintView view=new IconPackDrawingCheck.HintView();IconNativeAppearanceCheck.Glyph original=glyph(200,0x80112233);Drawable hint=drawing.wrapHint(view,new IconPackDrawingCheck.Model(1),original);Canvas canvas=new Canvas();
        hint.draw(canvas);equal(50,original.drawAlpha);equal(0xff123456,IconManagementRuntime.filterColor(original.drawFilter));equal(200,original.alpha);equal(0x80112233,IconManagementRuntime.filterColor(original.filter));
        equal(hint,drawing.wrapHint(view,new IconPackDrawingCheck.Model(2),hint));hint.draw(canvas);equal(200,original.drawAlpha);equal(original.filter,original.drawFilter);
        view.slot="location";IconNativeAppearanceCheck.Glyph location=glyph(176,0xff8899aa);Drawable wrapped=drawing.wrapHint(view,null,location);wrapped.draw(canvas);equal(44,location.drawAlpha);equal(location.filter,location.drawFilter);equal(176,location.alpha);
        drawing.releaseRuntime();wrapped.draw(canvas);equal(176,location.drawAlpha);equal(location.filter,location.drawFilter);
    }
    private static void battery()throws Exception{
        String ordinary=record(IconManagementSettings.DEFAULT,"target:battery","custom",0xff123456,0xffabcdef,50);String charging=record(ordinary,"role:battery.charging.80","custom",0xff00aa55,0xff33cc77,25);configure(ordinary);
        IconPackDrawing drawing=new IconPackDrawing();drawing.bindBatteryClass(HorizontalBatteryContentDrawable.class);HorizontalBatteryContentDrawable b=new HorizontalBatteryContentDrawable();
        IconNativeAppearanceCheck.Glyph frame=glyph(200,0xff112233),inside=glyph(80,0x80112233),rim=glyph(160,0xff112233),bg=glyph(60,0x40112233),progress=glyph(180,0xff112233);
        b.outsideDrawable=frame;b.insideDrawable=new LayerDrawable(new Drawable[]{inside});b.frameDrawable=rim;b.bgDrawable=bg;b.progressDrawable=new LayerDrawable(new Drawable[]{progress});b.outlineColor=0xff112233;b.percentInPaint.setColor(0xffaabbcc);b.percentInPaint.setAlpha(177);b.percentInPaint.setTextSize(18);b.chargeIconId=1;
        Drawable[] nativeFields={b.outsideDrawable,b.insideDrawable,b.frameDrawable,b.bgDrawable,b.progressDrawable};ColorFilter[] nativeFilters={frame.filter,inside.filter,rim.filter,bg.filter,progress.filter};
        StatBatteryMeterView view=new StatBatteryMeterView();view.style=b;android.provider.Settings.System.values.put("display_power_percent",1);drawing.syncBattery(view,null);Canvas canvas=new Canvas();
        // The real native charge ID selects a separate family. No explicit
        // charging palette yet: ordinary battery appearance must not leak in.
        equal(null,drawing.beginBattery(b));b.chargeIconId=0;
        IconPackDrawing.BatteryScope first=drawing.beginBattery(b);equal(true,first!=null);try(IconPackDrawing.BatteryScope scope=first){b.draw(canvas);}
        equal(100,frame.drawAlpha);equal(20,inside.drawAlpha);equal(80,rim.drawAlpha);equal(0xff123456,IconManagementRuntime.filterColor(frame.drawFilter));equal(1,b.textDraws);equal("84",canvas.text);equal(177,b.percentInPaint.getAlpha());equal(0xffaabbcc,b.percentInPaint.getColor());equal(200,frame.alpha);equal(nativeFilters[1],inside.filter);
        for(int i=0;i<100;i++){try(IconPackDrawing.BatteryScope scope=drawing.beginBattery(b)){equal(first,scope);b.draw(canvas);}}equal(200,frame.alpha);equal(80,inside.alpha);
        configure(charging);b.chargeIconId=1;try(IconPackDrawing.BatteryScope scope=drawing.beginBattery(b)){b.draw(canvas);}equal(50,frame.drawAlpha);equal(10,inside.drawAlpha);equal(0xff00aa55,IconManagementRuntime.filterColor(frame.drawFilter));equal(1,b.chargeDraws);equal(177,b.percentInPaint.getAlpha());
        // Percent disabled still styles only the visible original shape slots.
        android.provider.Settings.System.values.put("display_power_percent",0);drawing.syncBattery(view,null);b.isShowPercentIn=false;int text=b.textDraws;try(IconPackDrawing.BatteryScope scope=drawing.beginBattery(b)){equal(true,scope!=null);b.draw(canvas);}equal(text,b.textDraws);equal(2,b.chargeDraws);equal(4,bg.drawAlpha);equal(45,progress.drawAlpha);equal(180,progress.alpha);equal(nativeFilters[4],progress.filter);
        // Both audited OEM draw methods explicitly permit null inactive slots.
        // The out-of-battery mode must still style its own bg/progress only.
        b.outsideDrawable=null;b.insideDrawable=null;b.frameDrawable=null;
        try(IconPackDrawing.BatteryScope scope=drawing.beginBattery(b)){equal(true,scope!=null);b.draw(canvas);}equal(4,bg.drawAlpha);equal(45,progress.drawAlpha);equal(null,b.outsideDrawable);equal(null,b.insideDrawable);equal(null,b.frameDrawable);
        b.outsideDrawable=nativeFields[0];b.insideDrawable=(LayerDrawable)nativeFields[1];b.frameDrawable=nativeFields[2];
        b.isShowPercentIn=true;b.failContent=true;try(IconPackDrawing.BatteryScope scope=drawing.beginBattery(b)){b.draw(canvas);throw new AssertionError("battery draw should fail");}catch(IllegalStateException expected){equal("Native content failed",expected.getMessage());}
        Drawable[] restored={b.outsideDrawable,b.insideDrawable,b.frameDrawable,b.bgDrawable,b.progressDrawable};for(int i=0;i<restored.length;i++)equal(nativeFields[i],restored[i]);equal(200,frame.alpha);equal(nativeFilters[0],frame.filter);equal(177,b.percentInPaint.getAlpha());b.failContent=false;
        configure(IconManagementSettings.DEFAULT);equal(null,drawing.beginBattery(b));drawing.releaseRuntime();
    }
    public static void main(String[] args)throws Exception{signals();hints();battery();IconManagementRuntime.releaseRuntime();android.os.Handler.drainForCheck();System.out.println("IconPackAppearance checks passed: "+checks);}
}
