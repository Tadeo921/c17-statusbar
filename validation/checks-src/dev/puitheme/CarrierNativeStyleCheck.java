package dev.puitheme;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.text.TextPaint;
import com.oplus.systemui.qs.OplusQuickStatusBarHeader;
import com.oplus.systemui.qs.widget.OplusSecondCarrierText;
import com.oplus.systemui.separate.OplusQSSimpleHeader;
import java.util.Collections;

/** Native inherited setters can run after early classification, without a module observer. */
public final class CarrierNativeStyleCheck {
    private static int checks;
    private static void near(float expected, float actual) {
        checks++;
        if (Math.abs(expected - actual) > .0001f) throw new AssertionError(expected + " != " + actual);
    }
    private static void same(Object expected, Object actual) {
        checks++;
        if (expected != actual) throw new AssertionError("Newer native object was replaced");
    }
    private static void equal(int expected, int actual) {
        checks++;
        if (expected != actual) throw new AssertionError(expected + " != " + actual);
    }
    private static void truth(boolean expected, boolean actual) {
        checks++;
        if (expected != actual) throw new AssertionError(expected + " != " + actual);
    }
    private static final class CountingCarrier extends OplusSecondCarrierText {
        int sizeWrites;
        CountingCarrier(Context context) { super(context); }
        @Override public void setTextSize(int unit, float size) { sizeWrites++; super.setTextSize(unit, size); }
    }
    /** Replay a MATCH_PARENT single row and a custom family's taller metrics, without changing the shared Paint fixture. */
    private static final class FixedRowCarrier extends OplusSecondCarrierText {
        int height=80,width=708,sizeWrites;
        final TextPaint metricPaint=new TextPaint() {
            @Override public float getFontMetrics(Paint.FontMetrics metrics) {
                if(getTypeface()!=null&&getTypeface().getWeight()>=700) {
                    float size=getTextSize();metrics.top=-size*1.3f;metrics.bottom=size*.6f;
                    metrics.ascent=-size*1.2f;metrics.descent=size*.5f;return size*1.7f;
                }
                return super.getFontMetrics(metrics);
            }
        };
        FixedRowCarrier(Context context){super(context);setLayoutParams(new ViewGroup.LayoutParams(708,ViewGroup.LayoutParams.MATCH_PARENT));}
        @Override public int getHeight(){return height;}
        @Override public int getMeasuredHeight(){return height;}
        @Override public int getWidth(){return width;}
        @Override public int getMeasuredWidth(){return width;}
        @Override public int getPaddingTop(){return 3;}
        @Override public int getPaddingBottom(){return 5;}
        @Override public TextPaint getPaint(){metricPaint.set(super.getPaint());return metricPaint;}
        @Override public void setTextSize(int unit,float size){sizeWrites++;super.setTextSize(unit,size);}
    }
    private static final class ResizingCarrier extends OplusSecondCarrierText {
        boolean resizeText;
        ResizingCarrier(Context context){super(context);}
        @Override public void setText(CharSequence value){
            super.setText(value);
            if(resizeText)super.setTextSize(TypedValue.COMPLEX_UNIT_PX,19f);
        }
    }
    private static final class WidthCarrier extends OplusSecondCarrierText {
        int width=480,direction,sizeWrites;
        WidthCarrier(Context context){super(context);setLayoutParams(new ViewGroup.LayoutParams(480,300));}
        @Override public int getWidth(){return width;}
        @Override public int getMeasuredWidth(){return width;}
        @Override public int getHeight(){return 300;}
        @Override public int getMeasuredHeight(){return 300;}
        @Override public int getPaddingLeft(){return 12;}
        @Override public int getPaddingRight(){return 18;}
        @Override public int getLayoutDirection(){return direction;}
        @Override public void setTextSize(int unit,float pixels){sizeWrites++;super.setTextSize(unit,pixels);}
    }
    private static Bundle settings(String group) {
        Bundle settings = new Bundle();
        settings.putBoolean(group + "_enabled", true);
        settings.putBoolean(group + "_size_enabled", false);
        settings.putBoolean(group + "_text_style_enabled", false);
        settings.putBoolean(group + "_color_enabled", false);
        settings.putBoolean(group + "_position_enabled", false);
        settings.putString(group + "_mode", "text"); settings.putString(group + "_text", "自定义运营商");
        settings.putFloat(group + "_scale", 150f);
        return settings;
    }
    private static void run(String group, boolean classic) {
        PanelMode.report(!classic);
        Context context = new Context();
        TextControls control = new TextControls(new Handler(Looper.getMainLooper()));
        CountingCarrier carrier = new CountingCarrier(context);
        if (CarrierPanels.NOTIFICATION.equals(group)) new OplusQSSimpleHeader(context).addView(carrier);
        else new OplusQuickStatusBarHeader(context).addView(carrier);
        carrier.setText("中国电信|中国联通");
        control.attach(carrier); // Classification may occur at the early 13px default.
        near(13f, carrier.getTextSize());
        Typeface latestFace = Typeface.create(Typeface.DEFAULT, 550, false);
        // Audited constructor/classic dimensions are max=12dp/min=10dp (48/40px at density 4);
        // separated mode updates these from the native lockDpi/language-scale configuration.
        // mTargetSizePx is already pixels when AnimateChangeTextView calls its super setter.
        carrier.setTextSize(TypedValue.COMPLEX_UNIT_PX, 48f);
        carrier.setTypeface(latestFace); carrier.setLetterSpacing(.125f); carrier.setTextColor(0xdd123456);
        int writes = carrier.sizeWrites, faceWrites = carrier.typefaceWrites;
        for (int i = 0; i < 250; i++) {
            control.attach(carrier); control.beforeMeasure(carrier); control.beforeDraw(carrier, new Canvas()); control.refresh();
            near(48f, carrier.getTextSize()); same(latestFace, carrier.getTypeface());
            truth(false, control.sizeControlsEnabled(carrier));
        }
        equal(writes, carrier.sizeWrites); equal(faceWrites, carrier.typefaceWrites);
        Bundle settings = settings(group);
        control.configure(settings, StatusBarSettings.COLOR_DEFAULTS, Collections.emptyMap());
        near(48f, carrier.getTextSize()); same(latestFace, carrier.getTypeface());
        near(.125f, carrier.getLetterSpacing()); equal(0xdd123456, carrier.getCurrentTextColor()); equal(writes, carrier.sizeWrites);
        // Replacing text owns no size/font/color. A later native fitted target remains native.
        carrier.setTextSize(TypedValue.COMPLEX_UNIT_PX, 40f); writes = carrier.sizeWrites;
        try (TextControls.NativeStyleScope scope = control.beginNativeStyle(carrier)) {
            near(40f, carrier.getTextSize()); equal(writes, carrier.sizeWrites);
            carrier.setTextSize(TypedValue.COMPLEX_UNIT_PX, 44f);
        }
        near(44f, carrier.getTextSize()); equal(writes + 1, carrier.sizeWrites);
        settings.putBoolean(group + "_size_enabled", true);
        control.configure(settings, StatusBarSettings.COLOR_DEFAULTS, Collections.emptyMap());
        near(66f, carrier.getTextSize()); truth(true, control.sizeControlsEnabled(carrier));
        for (int i = 0; i < 1000; i++) {
            float nativePixels = 40f + i % 9;
            control.nativeSize(carrier, nativePixels);
            carrier.setTextSize(TypedValue.COMPLEX_UNIT_PX, control.styledSize(carrier));
            control.beforeMeasure(carrier); control.beforeDraw(carrier, new Canvas());
            near(nativePixels * 1.5f, carrier.getTextSize());
        }
        // Observed real OEM targets are supplied before style application; the former applied size is never a baseline.
        for (int i = 0; i < 500; i++) {
            control.nativeSize(carrier, 40f); control.beforeMeasure(carrier); near(60f, carrier.getTextSize());
        }
        // Only size was changed. Disabling it must leave independent latest OEM fields intact.
        Typeface replacementFace = Typeface.create(Typeface.DEFAULT, 725, true);
        carrier.setTypeface(replacementFace); carrier.setLetterSpacing(.375f); carrier.setTextColor(0xbb654321);
        settings.putBoolean(group + "_size_enabled", false);
        control.configure(settings, StatusBarSettings.COLOR_DEFAULTS, Collections.emptyMap());
        near(40f, carrier.getTextSize()); same(replacementFace, carrier.getTypeface());
        near(.375f, carrier.getLetterSpacing()); equal(0xbb654321, carrier.getCurrentTextColor());
        carrier.setTextSize(TypedValue.COMPLEX_UNIT_PX, 52f);
        settings.putBoolean(group + "_enabled", false);
        control.configure(settings, StatusBarSettings.COLOR_DEFAULTS, Collections.emptyMap());
        near(52f, carrier.getTextSize()); same(replacementFace, carrier.getTypeface());
        // An observed native zero or large transition is never clamped while size is off.
        for (float nativePixels : new float[]{0f, 5000f, 48f}) {
            control.nativeSize(carrier, nativePixels); carrier.setTextSize(TypedValue.COMPLEX_UNIT_PX, nativePixels);
            control.refresh(); near(nativePixels, carrier.getTextSize());
        }
        settings.putBoolean(group + "_enabled", true); settings.putBoolean(group + "_size_enabled", true);
        control.configure(settings, StatusBarSettings.COLOR_DEFAULTS, Collections.emptyMap()); near(72f, carrier.getTextSize());
        try (TextControls.NativeStyleScope scope = control.beginNativeStyle(carrier)) {
            near(48f, carrier.getTextSize());
            carrier.setTextSize(TypedValue.COMPLEX_UNIT_PX, 64f);
        }
        near(96f, carrier.getTextSize());
        settings.putBoolean(StatusBarSettings.SAFE_MODE, true);
        control.configure(settings, StatusBarSettings.COLOR_DEFAULTS, Collections.emptyMap()); near(64f, carrier.getTextSize());
        truth(false, control.sizeControlsEnabled(carrier));
        settings.putBoolean(StatusBarSettings.SAFE_MODE, false);
        control.configure(settings, StatusBarSettings.COLOR_DEFAULTS, Collections.emptyMap()); near(96f, carrier.getTextSize());
        // A newer native setter may bypass the observer. An OFF transition must not restore
        // yesterday's 64px over the system's new 84px, even while size was previously owned.
        carrier.setTextSize(TypedValue.COMPLEX_UNIT_PX, 84f);
        settings.putBoolean(group + "_size_enabled", false);
        control.configure(settings, StatusBarSettings.COLOR_DEFAULTS, Collections.emptyMap()); near(84f, carrier.getTextSize());
        settings.putBoolean(group + "_text_style_enabled", true); settings.putFloat(group + "_weight", 900f);
        control.configure(settings, StatusBarSettings.COLOR_DEFAULTS, Collections.emptyMap()); near(84f, carrier.getTextSize());
        carrier.setTextSize(TypedValue.COMPLEX_UNIT_PX, 56f);
        for (int i = 0; i < 200; i++) { control.beforeMeasure(carrier); near(56f, carrier.getTextSize()); }
        settings.putBoolean(group + "_enabled", false);
        control.configure(settings, StatusBarSettings.COLOR_DEFAULTS, Collections.emptyMap()); near(56f, carrier.getTextSize());
        same(replacementFace, carrier.getTypeface());
    }
    private static void fixedRow(String group,boolean classic) {
        PanelMode.report(!classic);Context context=new Context();
        TextControls control=new TextControls(new Handler(Looper.getMainLooper()));FixedRowCarrier carrier=new FixedRowCarrier(context);
        if(CarrierPanels.NOTIFICATION.equals(group))new OplusQSSimpleHeader(context).addView(carrier);
        else new OplusQuickStatusBarHeader(context).addView(carrier);
        carrier.setText("中国电信|中国联通");carrier.setTextSize(TypedValue.COMPLEX_UNIT_PX,48f);control.attach(carrier);
        ViewGroup.LayoutParams nativeParams=carrier.getLayoutParams();
        Bundle options=settings(group);options.putBoolean(group+"_size_enabled",true);options.putFloat(group+"_scale",200f);
        options.putBoolean(group+"_text_style_enabled",true);options.putFloat(group+"_weight",731f);
        control.configure(options,StatusBarSettings.COLOR_DEFAULTS,Collections.emptyMap());
        // A tall font's line box (1.9em here, close to 3em in the real variable font) is not what has to fit the row:
        // the ink is. 120% always showed up as the native size when the line box was the limit.
        near(72f,carrier.getTextSize());same(nativeParams,carrier.getLayoutParams());
        // The row is shorter than this font's line box, so the text is top-aligned and its baseline would leave the row as it
        // grows: the font padding goes, and the baseline keeps the place it has at 100% (1.3em at the native 48px).
        truth(false,carrier.getIncludeFontPadding());
        Canvas moved=new Canvas();control.beforeDraw(carrier,moved);near(1.3f*48f-1.2f*72f,moved.translateY);
        int writes=carrier.sizeWrites,requests=carrier.layoutRequests;
        for(int sample=0;sample<250;sample++) {
            control.beforeMeasure(carrier);control.beforeDraw(carrier,new Canvas());control.refresh();near(72f,carrier.getTextSize());
        }
        equal(writes,carrier.sizeWrites);equal(requests,carrier.layoutRequests);
        // The first measured row and later expanded rows re-evaluate the original 48px baseline, not the fitted pixels.
        carrier.height=120;carrier.dispatchLayoutChange(708,120,708,80);near(96f,carrier.getTextSize());
        carrier.height=80;carrier.dispatchLayoutChange(708,80,708,120);near(72f,carrier.getTextSize());
        // A row too low for even the native size never shrinks the text below the 100% baseline.
        carrier.height=40;carrier.dispatchLayoutChange(708,40,708,80);near(48f,carrier.getTextSize());
        carrier.height=80;carrier.dispatchLayoutChange(708,80,708,40);near(72f,carrier.getTextSize());
        try(TextControls.NativeStyleScope scope=control.beginNativeStyle(carrier)) {
            near(48f,carrier.getTextSize());carrier.setTextSize(TypedValue.COMPLEX_UNIT_PX,52f);
        }
        near(72f,carrier.getTextSize()); // the new native target 52px is the baseline; 200% of it is fitted to the row's ink
        options.putBoolean(group+"_size_enabled",false);control.configure(options,StatusBarSettings.COLOR_DEFAULTS,Collections.emptyMap());
        // A font-only customization keeps exactly the latest native target; it owns no row-fitting size.
        truth(false,control.sizeControlsEnabled(carrier));near(52f,carrier.getTextSize());
        truth(true,carrier.getIncludeFontPadding());Canvas native100=new Canvas();control.beforeDraw(carrier,native100);near(0f,native100.translateY);
        options.putBoolean(group+"_text_style_enabled",false);control.configure(options,StatusBarSettings.COLOR_DEFAULTS,Collections.emptyMap());
        near(52f,carrier.getTextSize());
        options.putBoolean(group+"_text_style_enabled",true);control.configure(options,StatusBarSettings.COLOR_DEFAULTS,Collections.emptyMap());
        near(52f,carrier.getTextSize());options.putBoolean(StatusBarSettings.SAFE_MODE,true);
        control.configure(options,StatusBarSettings.COLOR_DEFAULTS,Collections.emptyMap());near(52f,carrier.getTextSize());
        same(nativeParams,carrier.getLayoutParams());
        // Without an initial measurement, retain requested pixels then fit exactly once on the first real row layout.
        options.putBoolean(StatusBarSettings.SAFE_MODE,false);options.putBoolean(group+"_size_enabled",true);
        carrier.height=0;control.configure(options,StatusBarSettings.COLOR_DEFAULTS,Collections.emptyMap());near(104f,carrier.getTextSize());
        carrier.height=80;carrier.dispatchLayoutChange(708,80,708,0);near(72f,carrier.getTextSize());
        // WRAP_CONTENT locations can grow with their text and must not inherit a fixed QS-row cap.
        nativeParams.height=ViewGroup.LayoutParams.WRAP_CONTENT;control.beforeMeasure(carrier);near(104f,carrier.getTextSize());
        options.putBoolean(group+"_enabled",false);control.configure(options,StatusBarSettings.COLOR_DEFAULTS,Collections.emptyMap());near(52f,carrier.getTextSize());
    }
    private static void nativeHundredPercent(String group,boolean classic) {
        PanelMode.report(!classic);Context context=new Context();
        TextControls control=new TextControls(new Handler(Looper.getMainLooper()));FixedRowCarrier carrier=new FixedRowCarrier(context);
        if(CarrierPanels.NOTIFICATION.equals(group))new OplusQSSimpleHeader(context).addView(carrier);
        else new OplusQuickStatusBarHeader(context).addView(carrier);
        carrier.setText("中国电信|中国联通");carrier.setTextSize(TypedValue.COMPLEX_UNIT_PX,44f);control.attach(carrier);
        Bundle options=settings(group);options.putBoolean(group+"_size_enabled",true);options.putFloat(group+"_scale",100f);
        options.putBoolean(group+"_text_style_enabled",true);options.putFloat(group+"_weight",731f);
        control.configure(options,StatusBarSettings.COLOR_DEFAULTS,Collections.emptyMap());near(44f,carrier.getTextSize());
        truth(carrier.getTypeface().getWeight()>=700,true);
        for(int i=0;i<250;i++) {
            control.refresh();control.beforeMeasure(carrier);control.beforeDraw(carrier,new Canvas());near(44f,carrier.getTextSize());
            // OEM width/configuration callbacks read their own family and actual target, rather than max-size or styled Paint.
            try(TextControls.NativeStyleScope scope=control.beginNativeStyle(carrier)){
                near(44f,carrier.getTextSize());truth(carrier.getTypeface().getWeight()<700,true);
                carrier.setTextSize(TypedValue.COMPLEX_UNIT_PX,40f+i%9);
            }
            float nativePixels=40f+i%9;near(nativePixels,carrier.getTextSize());
            options.putFloat(group+"_scale",80f);control.configure(options,StatusBarSettings.COLOR_DEFAULTS,Collections.emptyMap());
            near(nativePixels*.8f,carrier.getTextSize());
            options.putFloat(group+"_scale",100f);control.configure(options,StatusBarSettings.COLOR_DEFAULTS,Collections.emptyMap());
            near(nativePixels,carrier.getTextSize());
            // No-change callbacks still capture the restored native size rather than a previously fitted value.
            try(TextControls.NativeStyleScope scope=control.beginNativeStyle(carrier)){near(nativePixels,carrier.getTextSize());}
            near(nativePixels,carrier.getTextSize());
            options.putBoolean(StatusBarSettings.SAFE_MODE,true);control.configure(options,StatusBarSettings.COLOR_DEFAULTS,Collections.emptyMap());
            near(nativePixels,carrier.getTextSize());
            options.putBoolean(StatusBarSettings.SAFE_MODE,false);control.configure(options,StatusBarSettings.COLOR_DEFAULTS,Collections.emptyMap());
            near(nativePixels,carrier.getTextSize());
            // Reset the next iteration's native baseline without relying on the module's external size observer.
            try(TextControls.NativeStyleScope scope=control.beginNativeStyle(carrier)){carrier.setTextSize(TypedValue.COMPLEX_UNIT_PX,44f);}
        }
        options.putBoolean(group+"_size_enabled",false);control.configure(options,StatusBarSettings.COLOR_DEFAULTS,Collections.emptyMap());
        near(44f,carrier.getTextSize());
        carrier.setTextSize(TypedValue.COMPLEX_UNIT_PX,46f);control.refresh();near(46f,carrier.getTextSize());
        options.putBoolean(group+"_enabled",false);control.configure(options,StatusBarSettings.COLOR_DEFAULTS,Collections.emptyMap());near(46f,carrier.getTextSize());
    }
    private static void replacementDoesNotResizeBaseline() {
        PanelMode.report(true);Context context=new Context();
        TextControls control=new TextControls(new Handler(Looper.getMainLooper()));ResizingCarrier carrier=new ResizingCarrier(context);
        new OplusQuickStatusBarHeader(context).addView(carrier);
        carrier.setText("原生运营商");carrier.setTextSize(TypedValue.COMPLEX_UNIT_PX,44f);control.attach(carrier);carrier.resizeText=true;
        Bundle options=settings(CarrierPanels.CONTROL);options.putBoolean(CarrierPanels.CONTROL+"_size_enabled",true);
        options.putFloat(CarrierPanels.CONTROL+"_scale",100f);
        control.configure(options,StatusBarSettings.COLOR_DEFAULTS,Collections.emptyMap());near(44f,carrier.getTextSize());
        for(int i=0;i<200;i++) {
            options.putString(CarrierPanels.CONTROL+"_text","新的自定义文本"+i);
            control.configure(options,StatusBarSettings.COLOR_DEFAULTS,Collections.emptyMap());near(44f,carrier.getTextSize());
            // Replay the external native setText hook: replacement auto-sizing is scoped before attach observes Paint.
            TextControls.CarrierTextScope scope=control.beginCarrierText(carrier);
            truth(scope!=null,true);carrier.setText(control.nativeText(carrier,"系统最新运营商"+i));near(19f,carrier.getTextSize());
            scope.close();scope.close();control.attach(carrier);near(44f,carrier.getTextSize());truth(false,control.isInternal());
        }
        // Genuine OEM style updates can still supply a new smaller target inside their separate native scope.
        try(TextControls.NativeStyleScope scope=control.beginNativeStyle(carrier)) {
            carrier.setText("新的系统原文");carrier.setTextSize(TypedValue.COMPLEX_UNIT_PX,40f);
            truth(control.beginCarrierText(carrier)==null,true);
        }
        near(40f,carrier.getTextSize());
        options.putBoolean(CarrierPanels.CONTROL+"_size_enabled",false);control.configure(options,StatusBarSettings.COLOR_DEFAULTS,Collections.emptyMap());near(40f,carrier.getTextSize());
        options.putBoolean(StatusBarSettings.SAFE_MODE,true);control.configure(options,StatusBarSettings.COLOR_DEFAULTS,Collections.emptyMap());near(40f,carrier.getTextSize());
        truth(control.beginCarrierText(carrier)==null,true);
    }
    private static void cachedNativeTargets(String group,boolean classic) {
        PanelMode.report(!classic);Context context=new Context();
        TextControls control=new TextControls(new Handler(Looper.getMainLooper()));CountingCarrier carrier=new CountingCarrier(context);
        if(CarrierPanels.NOTIFICATION.equals(group))new OplusQSSimpleHeader(context).addView(carrier);
        else new OplusQuickStatusBarHeader(context).addView(carrier);
        carrier.setText("原生运营商");control.attach(carrier);near(13f,carrier.getTextSize());
        Bundle options=settings(group);options.putBoolean(group+"_size_enabled",true);options.putFloat(group+"_scale",100f);
        control.configure(options,StatusBarSettings.COLOR_DEFAULTS,Collections.emptyMap());near(13f,carrier.getTextSize());
        // Native needApplyNewTextSize may hit its text/width cache, while mAppliedSizePx already holds 48px.
        TextControls.NativeStyleScope scope=control.beginNativeStyle(carrier);truth(scope!=null,true);
        scope.carrierPixels(48f);scope.close();scope.close();near(48f,carrier.getTextSize());
        scope.carrierPixels(99f);near(48f,carrier.getTextSize());
        for(int i=0;i<250;i++) {
            float target=40f+i%9;
            try(TextControls.NativeStyleScope callback=control.beginNativeStyle(carrier)) {
                callback.carrierPixels(target); // No Paint setter: replay the real cached native callback.
            }
            near(target,carrier.getTextSize());
            options.putFloat(group+"_scale",150f);control.configure(options,StatusBarSettings.COLOR_DEFAULTS,Collections.emptyMap());
            near(target*1.5f,carrier.getTextSize());
            try(TextControls.NativeStyleScope callback=control.beginNativeStyle(carrier)) {
                near(target,carrier.getTextSize());callback.carrierPixels(target);
            }
            near(target*1.5f,carrier.getTextSize());
            options.putFloat(group+"_scale",100f);control.configure(options,StatusBarSettings.COLOR_DEFAULTS,Collections.emptyMap());
            near(target,carrier.getTextSize());control.refresh();control.beforeMeasure(carrier);near(target,carrier.getTextSize());
        }
        try(TextControls.NativeStyleScope callback=control.beginNativeStyle(carrier)) {
            carrier.setTextSize(TypedValue.COMPLEX_UNIT_PX,46f);
            callback.carrierPixels(Float.NaN);callback.carrierPixels(Float.POSITIVE_INFINITY);
            callback.carrierPixels(0f);callback.carrierPixels(-1f);
        }
        near(46f,carrier.getTextSize());
        options.putBoolean(group+"_size_enabled",false);control.configure(options,StatusBarSettings.COLOR_DEFAULTS,Collections.emptyMap());
        try(TextControls.NativeStyleScope callback=control.beginNativeStyle(carrier)){callback.carrierPixels(44f);}
        near(44f,carrier.getTextSize());truth(false,control.sizeControlsEnabled(carrier));
        options.putBoolean(group+"_size_enabled",true);control.configure(options,StatusBarSettings.COLOR_DEFAULTS,Collections.emptyMap());
        // The positive px target in native setTextWithAnima must be supplied before its replacement is attached.
        for(int i=0;i<100;i++) {
            control.nativeSize(carrier,42f);carrier.setText(control.nativeText(carrier,"最新原生运营商"+i));
            control.attach(carrier);near(42f,carrier.getTextSize());
        }
        options.putBoolean(StatusBarSettings.SAFE_MODE,true);control.configure(options,StatusBarSettings.COLOR_DEFAULTS,Collections.emptyMap());
        near(42f,carrier.getTextSize());truth(false,control.isInternal());
    }
    private static void replacementWidth(String group,boolean classic) {
        PanelMode.report(!classic);Context context=new Context();
        TextControls control=new TextControls(new Handler(Looper.getMainLooper()));WidthCarrier carrier=new WidthCarrier(context);
        if(CarrierPanels.NOTIFICATION.equals(group))new OplusQSSimpleHeader(context).addView(carrier);
        else new OplusQuickStatusBarHeader(context).addView(carrier);
        carrier.setText("native");carrier.setTextSize(TypedValue.COMPLEX_UNIT_PX,48f);
        carrier.setEllipsize(android.text.TextUtils.TruncateAt.MARQUEE);control.attach(carrier);
        ViewGroup.LayoutParams params=carrier.getLayoutParams();
        Bundle options=settings(group);options.putBoolean(group+"_size_enabled",true);
        options.putBoolean(group+"_position_enabled",true);options.putFloat(group+"_offset_x",4f);
        options.putString(group+"_text","ABCDEFGHIJK");options.putFloat(group+"_scale",150f);
        control.configure(options,StatusBarSettings.COLOR_DEFAULTS,Collections.emptyMap());
        // Eleven fixture glyphs at .6em, 480px slot minus 30px padding and 16px local X.
        near(434f/6.6f,carrier.getTextSize());
        truth(carrier.getText().toString().equals("ABCDEFGHIJK"),true);
        same(params,carrier.getLayoutParams());same(android.text.TextUtils.TruncateAt.MARQUEE,carrier.getEllipsize());
        int writes=carrier.sizeWrites,requests=carrier.layoutRequests;
        for(int i=0;i<300;i++){control.beforeMeasure(carrier);control.beforeDraw(carrier,new Canvas());}
        equal(writes,carrier.sizeWrites);equal(requests,carrier.layoutRequests);
        // Re-evaluate on a native width-only layout change; no height change is required.
        carrier.width=450;carrier.dispatchLayoutChange(450,300,480,300);near(404f/6.6f,carrier.getTextSize());
        carrier.width=900;carrier.dispatchLayoutChange(900,300,450,300);near(72f,carrier.getTextSize());
        carrier.width=480;carrier.dispatchLayoutChange(480,300,900,300);near(434f/6.6f,carrier.getTextSize());
        carrier.direction=android.view.View.LAYOUT_DIRECTION_RTL;control.beforeMeasure(carrier);
        near(450f/6.6f,carrier.getTextSize());
        options.putFloat(group+"_offset_x",-4f);control.configure(options,StatusBarSettings.COLOR_DEFAULTS,Collections.emptyMap());
        near(434f/6.6f,carrier.getTextSize());
        options.putFloat(group+"_scale",100f);control.configure(options,StatusBarSettings.COLOR_DEFAULTS,Collections.emptyMap());
        near(48f,carrier.getTextSize());
        options.putString(group+"_text","ABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZ");
        options.putFloat(group+"_scale",150f);control.configure(options,StatusBarSettings.COLOR_DEFAULTS,Collections.emptyMap());
        near(48f,carrier.getTextSize()); // Long custom text uses native marquee; never shrink below 100%.
        options.putFloat(group+"_scale",80f);control.configure(options,StatusBarSettings.COLOR_DEFAULTS,Collections.emptyMap());
        near(38.4f,carrier.getTextSize());
        options.putFloat(group+"_scale",100f);control.configure(options,StatusBarSettings.COLOR_DEFAULTS,Collections.emptyMap());near(48f,carrier.getTextSize());
        options.putBoolean(StatusBarSettings.SAFE_MODE,true);control.configure(options,StatusBarSettings.COLOR_DEFAULTS,Collections.emptyMap());
        near(48f,carrier.getTextSize());truth(carrier.getText().toString().equals("native"),true);
        same(params,carrier.getLayoutParams());same(android.text.TextUtils.TruncateAt.MARQUEE,carrier.getEllipsize());
    }
    private static void nativeCallbackText() {
        PanelMode.report(true);Context context=new Context();
        TextControls control=new TextControls(new Handler(Looper.getMainLooper()));CountingCarrier carrier=new CountingCarrier(context);
        new OplusQuickStatusBarHeader(context).addView(carrier);
        carrier.setText("旧原生运营商");carrier.setTextSize(TypedValue.COMPLEX_UNIT_PX,48f);control.attach(carrier);
        Bundle options=settings(CarrierPanels.CONTROL);options.putBoolean(CarrierPanels.CONTROL+"_size_enabled",true);
        options.putFloat(CarrierPanels.CONTROL+"_scale",150f);
        control.configure(options,StatusBarSettings.COLOR_DEFAULTS,Collections.emptyMap());near(72f,carrier.getTextSize());
        TextControls.NativeStyleScope callback=control.beginNativeStyle(carrier);truth(control.isInternal(),true);
        near(48f,carrier.getTextSize());
        carrier.setText(control.nativeText(carrier,"回调内真实新运营商"));
        truth(carrier.getText().toString().equals("回调内真实新运营商"),true);
        callback.carrierText("字段中的最新原生运营商");callback.carrierPixels(44f);callback.close();callback.close();
        near(66f,carrier.getTextSize());truth(carrier.getText().toString().equals("自定义运营商"),true);
        callback.carrierText("已关闭作用域的原文不得生效");truth(control.isInternal(),false);
        options.putBoolean(StatusBarSettings.SAFE_MODE,true);control.configure(options,StatusBarSettings.COLOR_DEFAULTS,Collections.emptyMap());
        near(44f,carrier.getTextSize());truth(carrier.getText().toString().equals("字段中的最新原生运营商"),true);
        options.putBoolean(StatusBarSettings.SAFE_MODE,false);control.configure(options,StatusBarSettings.COLOR_DEFAULTS,Collections.emptyMap());
        // Native fade has not assigned TextView text yet: field evidence still owns the next feature-off restoration.
        try(TextControls.NativeStyleScope delayed=control.beginNativeStyle(carrier)) {
            delayed.carrierText("异步原生回调运营商");delayed.carrierPixels(46f);
        }
        near(69f,carrier.getTextSize());truth(carrier.getText().toString().equals("自定义运营商"),true);
        options.putBoolean(CarrierPanels.CONTROL+"_enabled",false);control.configure(options,StatusBarSettings.COLOR_DEFAULTS,Collections.emptyMap());
        near(46f,carrier.getTextSize());truth(carrier.getText().toString().equals("异步原生回调运营商"),true);
        options.putBoolean(CarrierPanels.CONTROL+"_enabled",true);control.configure(options,StatusBarSettings.COLOR_DEFAULTS,Collections.emptyMap());
        try(TextControls.NativeStyleScope empty=control.beginNativeStyle(carrier)) { empty.carrierText(null); }
        options.putBoolean(StatusBarSettings.SAFE_MODE,true);control.configure(options,StatusBarSettings.COLOR_DEFAULTS,Collections.emptyMap());
        truth(carrier.getText().toString().isEmpty(),true);truth(control.isInternal(),false);
    }
    public static void main(String[] args) {
        run(CarrierPanels.NOTIFICATION, false);
        run(CarrierPanels.CONTROL, false);
        run(CarrierPanels.CLASSIC, true);
        fixedRow(CarrierPanels.NOTIFICATION,false);
        fixedRow(CarrierPanels.CONTROL,false);
        fixedRow(CarrierPanels.CLASSIC,true);
        nativeHundredPercent(CarrierPanels.NOTIFICATION,false);
        nativeHundredPercent(CarrierPanels.CONTROL,false);
        nativeHundredPercent(CarrierPanels.CLASSIC,true);
        replacementDoesNotResizeBaseline();
        cachedNativeTargets(CarrierPanels.NOTIFICATION,false);
        cachedNativeTargets(CarrierPanels.CONTROL,false);
        cachedNativeTargets(CarrierPanels.CLASSIC,true);
        replacementWidth(CarrierPanels.NOTIFICATION,false);
        replacementWidth(CarrierPanels.CONTROL,false);
        replacementWidth(CarrierPanels.CLASSIC,true);
        nativeCallbackText();
        PanelMode.query(key -> null);
        System.out.println(checks + " carrier native pixels, disabled pass-through, property ownership and latest OEM restoration checks passed");
    }
}
