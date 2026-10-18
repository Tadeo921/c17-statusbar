package dev.puitheme;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.StaticLayout;
import android.widget.TextView;
import android.view.View;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;

/** Exercises a measured, drawable two-row clock, including the lower row that an unmeasured fixture skips. */
public final class ClockSteadyDrawCheck {
    private static int checks;
    private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    private static final class ClockContext extends Context {
        final StatClockSizingCheck.ClockResources resources=new StatClockSizingCheck.ClockResources();
        ClockContext(){resources.getDisplayMetrics().density=resources.getDisplayMetrics().scaledDensity=4;}
        @Override public Resources getResources(){return resources;}
    }
    public static final class OwnershipResources extends Resources {
        @Override public int getIdentifier(String name,String type,String owner){return "stat_clock_size".equals(name)&&"dimen".equals(type)&&"com.android.systemui".equals(owner)?1:0;}
        public int getDimensionPixelSize(int id){return Math.round(13f*getDisplayMetrics().density);}
        @Override public String getResourceEntryName(int id){return id==502?"qs_footer_clock":"clock";}
        @Override public String getResourcePackageName(int id){return "com.android.systemui";}
    }
    private static final class OwnershipContext extends Context {
        final OwnershipResources resources=new OwnershipResources();
        OwnershipContext(){resources.getDisplayMetrics().density=resources.getDisplayMetrics().scaledDensity=4;}
        @Override public Resources getResources(){return resources;}
    }
    private static final class OwnedClock extends com.oplus.systemui.statusbar.widget.StatClock {
        int id=501;
        OwnedClock(Context context){super(context);stripHeight=80;}
        @Override public int getId(){return id;}
        @Override public Resources getResources(){return getContext().getResources();}
    }
    private static final class FontRecoveryContext extends Context {
        final StatClockSizingCheck.ClockResources resources=new StatClockSizingCheck.ClockResources();
        final java.io.File font;
        int providerReads;
        final android.content.ContentResolver resolver=new android.content.ContentResolver(){
            @Override public android.os.ParcelFileDescriptor openFileDescriptor(android.net.Uri uri,String mode)throws java.io.FileNotFoundException{
                providerReads++;return android.os.ParcelFileDescriptor.open(font,android.os.ParcelFileDescriptor.MODE_READ_ONLY);
            }
        };
        FontRecoveryContext(java.io.File font){this.font=font;resources.getDisplayMetrics().density=resources.getDisplayMetrics().scaledDensity=4;}
        @Override public Resources getResources(){return resources;}
        @Override public android.content.ContentResolver getContentResolver(){return resolver;}
    }
    private static Bundle settings(){
        Bundle b=new Bundle();b.putBoolean(ClockLayoutSettings.DUAL_ENABLED,true);b.putBoolean("clock_controls_enabled",false);
        b.putString(ClockLayoutSettings.DUAL_UPPER_PATTERN,"MM月dd日 EEE");b.putString(ClockLayoutSettings.DUAL_LOWER_PATTERN,"HH:mm:ss");
        b.putFloat(ClockLayoutSettings.DUAL_UPPER_SCALE,48.64f);b.putFloat(ClockLayoutSettings.DUAL_LOWER_SCALE,79.22f);
        b.putFloat(ClockLayoutSettings.DUAL_UPPER_WEIGHT,885);b.putFloat(ClockLayoutSettings.DUAL_LOWER_WEIGHT,750);
        b.putFloat(ClockLayoutSettings.DUAL_LINE_GAP,-3.48f);return b;
    }
    private static Object entry(TextControls controls,TextView clock)throws Exception{
        Field views=TextControls.class.getDeclaredField("views");views.setAccessible(true);return ((Map<?,?>)views.get(controls)).get(clock);
    }
    private static Field field(Object entry,String name)throws Exception{Field f=entry.getClass().getDeclaredField(name);f.setAccessible(true);return f;}
    private static void measure(TextControls controls,TextView clock){
        int spec=controls.prepareClockMeasure(clock,View.MeasureSpec.makeMeasureSpec(900,View.MeasureSpec.AT_MOST));
        clock.measure(spec,View.MeasureSpec.makeMeasureSpec(80,View.MeasureSpec.EXACTLY));controls.afterClockMeasure(clock);
    }
    private static void move(TextControls controls,OwnedClock clock,android.view.ViewGroup from,android.view.ViewGroup to,int id){
        from.removeView(clock);clock.id=id;to.addView(clock);controls.classificationChanged(clock);
    }
    private static void ownershipChanges(boolean modern)throws Exception{
        Paint.fontVariationOnPaint=modern;OwnershipContext context=new OwnershipContext();FontRepository.configure(context,"system","");
        TextControls controls=new TextControls(new Handler(Looper.getMainLooper()));OwnedClock clock=new OwnedClock(context);
        android.widget.LinearLayout status=new android.widget.LinearLayout(context);
        com.oplus.systemui.separate.OplusQSSimpleHeader shade=new com.oplus.systemui.separate.OplusQSSimpleHeader(context);
        status.addView(clock);clock.setText("native owner");
        Typeface family=Typeface.create(Typeface.DEFAULT,430,true);family.variableWeight=true;clock.setTypeface(family);
        String nativeAxes="'wght' 430, 'wdth' 91";clock.setFontVariationSettings(nativeAxes);Typeface nativeFace=clock.getTypeface();
        Bundle b=settings();b.putBoolean(StatusBarSettings.SHADE_CLOCK_CONTROLS_ENABLED,true);
        b.putBoolean("shade_clock_text_style_enabled",true);b.putFloat(StatusBarSettings.SHADE_CLOCK_WEIGHT,620);
        controls.configure(b,StatusBarSettings.COLOR_DEFAULTS,Collections.emptyMap());controls.attach(clock);measure(controls,clock);
        Object entry=entry(controls,clock);Field lower=field(entry,"lowerLayout");Field revision=field(entry,"appliedFontLayoutRevision");
        long configuredRevision=revision.getLong(entry);Canvas canvas=new Canvas();
        check("clock".equals(controls.group(clock)),"status owner starts in clock group");
        check(clock.getPaint().effectiveWeight()==885&&lower.get(entry)!=null,"status upper and lower fonts are applied");
        // Native ID/parent changes select a new style without another configure/revision event.
        move(controls,clock,status,shade,502);check("shade_clock".equals(controls.group(clock)),"native relocation selects independent shade owner");
        measure(controls,clock);controls.beforeDraw(clock,canvas);controls.afterDraw(clock,canvas);
        check(revision.getLong(entry)==configuredRevision,"ownership change does not publish a settings revision");
        check(clock.getPaint().effectiveWeight()==620,"shade ownership invalidates the cached upper font");
        check(lower.get(entry)==null,"shade owner releases the status lower row");
        check(clock.getParent()==shade&&shade.getChildAt(0)==clock,"styling retains the native parent and index");
        move(controls,clock,shade,status,501);check("clock".equals(controls.group(clock)),"native relocation restores status owner");measure(controls,clock);
        check(clock.getPaint().effectiveWeight()==885&&lower.get(entry)!=null,"returning to dual restores its own upper font and lower row");
        check(revision.getLong(entry)==configuredRevision,"returning ownership keeps the same settings revision");
        // A managed shade clock with no font effect must restore the exact native family/axes.
        b.putBoolean("shade_clock_text_style_enabled",false);controls.configure(b,StatusBarSettings.COLOR_DEFAULTS,Collections.emptyMap());
        configuredRevision=revision.getLong(entry);move(controls,clock,status,shade,502);controls.group(clock);measure(controls,clock);
        check(clock.getTypeface()==nativeFace&&nativeAxes.equals(clock.getFontVariationSettings()),"leaving dual restores the native upper family and axes");
        check(revision.getLong(entry)==configuredRevision,"native-font restoration is caused by ownership alone");
        // OEM configuration runs against native values and becomes the next restoration baseline.
        TextControls.NativeStyleScope scope=controls.beginNativeStyle(clock);check(scope!=null,"native style scope remains available for the new owner");
        Typeface changed=Typeface.create(Typeface.DEFAULT,510,false);changed.variableWeight=true;clock.setTypeface(changed);
        nativeAxes="'wght' 510, 'wdth' 87";clock.setFontVariationSettings(nativeAxes);nativeFace=clock.getTypeface();
        clock.setTextColor(0xff556677);scope.close();measure(controls,clock);
        check(clock.getTypeface()==nativeFace&&nativeAxes.equals(clock.getFontVariationSettings()),"native callback font changes survive an unstyled owner");
        move(controls,clock,shade,status,501);controls.group(clock);measure(controls,clock);
        check(clock.getPaint().effectiveWeight()==885&&lower.get(entry)!=null,"dual resumes from the latest OEM font baseline");
        b.putBoolean(ClockLayoutSettings.DUAL_ENABLED,false);controls.configure(b,StatusBarSettings.COLOR_DEFAULTS,Collections.emptyMap());
        check(clock.getTypeface()==nativeFace&&nativeAxes.equals(clock.getFontVariationSettings()),"disabling dual restores the latest native upper font");
        check(clock.getCurrentTextColor()==0xff556677&&lower.get(entry)==null,"disabling dual preserves native color and releases lower layout");
        FontRepository.releaseRuntime();controls.beforeDraw(clock,canvas);controls.afterDraw(clock,canvas);
        check(clock.getTypeface()==nativeFace&&nativeAxes.equals(clock.getFontVariationSettings()),"font release cannot reapply stale upper styling after disable");
    }
    private static boolean custom(TextView clock){return "custom".equals(clock.getTypeface().sourceForCheck);}
    private static boolean customLower(Object entry,Field lower)throws Exception{
        StaticLayout layout=(StaticLayout)lower.get(entry);return layout!=null&&"custom".equals(layout.paintForCheck().getTypeface().sourceForCheck);
    }
    private static void fontRecovery(boolean modern)throws Exception{
        Paint.fontVariationOnPaint=modern;FontRepository.releaseRuntime();long originalUptime=android.os.SystemClock.uptime;
        java.nio.file.Path directory=java.nio.file.Files.createTempDirectory("c17-font-retry-");java.nio.file.Path font=directory.resolve("provider.font");
        try{
            android.os.SystemClock.uptime=100000;FontRecoveryContext context=new FontRecoveryContext(font.toFile());
            FontRepository.configure(context,"custom","retry-fixture");TextControls controls=new TextControls(new Handler(Looper.getMainLooper()));
            StatClockSizingCheck.MeasuredClock clock=new StatClockSizingCheck.MeasuredClock(context);clock.setText("native");
            Bundle b=settings();b.putBoolean("font_enabled",true);controls.configure(b,StatusBarSettings.COLOR_DEFAULTS,Collections.emptyMap());
            controls.attach(clock);measure(controls,clock);Object entry=entry(controls,clock);Field lower=field(entry,"lowerLayout");Canvas canvas=new Canvas();
            check(context.providerReads==1,"the first missing custom font makes one provider attempt, not one per row");
            check(!custom(clock)&&!customLower(entry,lower),"a missing custom provider retains both native fallback rows");
            TextControls single=new TextControls(new Handler(Looper.getMainLooper()));StatClockSizingCheck.MeasuredClock singleClock=new StatClockSizingCheck.MeasuredClock(context);
            singleClock.setText("single");Bundle singleSettings=new Bundle();singleSettings.putBoolean("font_enabled",true);
            singleSettings.putBoolean("clock_controls_enabled",true);singleSettings.putFloat(StatusBarSettings.CLOCK_WEIGHT,640);
            single.configure(singleSettings,StatusBarSettings.COLOR_DEFAULTS,Collections.emptyMap());single.attach(singleClock);measure(single,singleClock);
            check(context.providerReads==1&&!custom(singleClock),"a single-row clock shares the same source backoff");
            Method apply=TextControls.class.getDeclaredMethod("apply",TextView.class,entry.getClass(),long.class,boolean.class);apply.setAccessible(true);
            long now=System.currentTimeMillis(),revision=FontRepository.styleRevision();
            for(int second=1;second<=4;second++){
                android.os.SystemClock.uptime=100000+second*1000;apply.invoke(controls,clock,entry,now+second*1000,false);measure(controls,clock);
                for(int i=0;i<1000;i++){controls.beforeDraw(clock,canvas);controls.afterDraw(clock,canvas);single.beforeDraw(singleClock,canvas);single.afterDraw(singleClock,canvas);}
            }
            check(context.providerReads==1,"draws and changed-second layouts never retry within the first five seconds");
            android.os.SystemClock.uptime=105000;controls.beforeDraw(clock,canvas);controls.afterDraw(clock,canvas);
            single.beforeDraw(singleClock,canvas);single.afterDraw(singleClock,canvas);
            check(context.providerReads==2,"an expired source failure makes exactly one further provider attempt");
            check(FontRepository.styleRevision()==revision,"another failure does not publish a false recovery");
            java.nio.file.Files.write(font,new byte[]{0,1,0,0,1});android.os.SystemClock.uptime=109999;
            apply.invoke(controls,clock,entry,now+9*1000,false);measure(controls,clock);measure(single,singleClock);
            controls.beforeDraw(clock,canvas);controls.afterDraw(clock,canvas);single.beforeDraw(singleClock,canvas);single.afterDraw(singleClock,canvas);
            check(context.providerReads==2&&!custom(clock)&&!custom(singleClock),"a recovered provider remains untouched until the new deadline");
            Object fallback=lower.get(entry);android.os.SystemClock.uptime=110000;
            controls.beforeDraw(clock,canvas);controls.afterDraw(clock,canvas);measure(controls,clock);
            single.beforeDraw(singleClock,canvas);single.afterDraw(singleClock,canvas);measure(single,singleClock);
            check(FontRepository.styleRevision()>revision,"successful source recovery publishes a new revision");
            check(custom(clock)&&customLower(entry,lower)&&fallback!=lower.get(entry),"the next drawn frame and its native measure restore both custom rows");
            check(custom(singleClock),"a stable single-row font memo sees the same recovery publication");
            int reads=context.providerReads;long[] hits=FontWeight.stats();
            for(int i=0;i<1000;i++){controls.beforeDraw(clock,canvas);controls.afterDraw(clock,canvas);single.beforeDraw(singleClock,canvas);single.afterDraw(singleClock,canvas);}
            check(context.providerReads==reads&&FontWeight.stats()[0]==hits[0],"recovered steady frames do no provider work or font lookup");
            // The failing weight can disappear; a still-visible, already cached weight must finish retry bookkeeping.
            java.nio.file.Files.delete(font);FontRepository.typeface(Typeface.DEFAULT,399);check(context.providerReads==reads+1,"a newly requested missing weight starts a source backoff");
            java.nio.file.Files.write(font,new byte[]{0,1,0,0,2});android.os.SystemClock.uptime=114999;
            controls.beforeDraw(clock,canvas);controls.afterDraw(clock,canvas);check(context.providerReads==reads+1,"an existing successful weight also respects the pending deadline");
            android.os.SystemClock.uptime=115000;controls.beforeDraw(clock,canvas);controls.afterDraw(clock,canvas);measure(controls,clock);
            check(context.providerReads==reads+2,"a cached visible weight retries the source once after the failing weight disappears");
            reads=context.providerReads;
            for(int i=0;i<1000;i++){controls.beforeDraw(clock,canvas);controls.afterDraw(clock,canvas);}
            check(context.providerReads==reads,"a cached successful retry clears the due state instead of retrying every frame");
            java.nio.file.Files.delete(font);FontRepository.typeface(Typeface.DEFAULT,398);reads=context.providerReads;
            FontRepository.configure(context,"custom","retry-fixture-new");controls.beforeDraw(clock,canvas);controls.afterDraw(clock,canvas);
            check(context.providerReads==reads+1,"a new configured font source clears the previous retry deadline");
            FontRepository.releaseRuntime();controls.beforeDraw(clock,canvas);controls.afterDraw(clock,canvas);measure(controls,clock);
            check(!custom(clock)&&!customLower(entry,lower),"release restores the system source and releases its retry state");
            reads=context.providerReads;android.os.SystemClock.uptime+=6000;controls.beforeDraw(clock,canvas);controls.afterDraw(clock,canvas);
            check(context.providerReads==reads,"a released source has no overdue retry");
        }finally{
            FontRepository.releaseRuntime();android.os.SystemClock.uptime=originalUptime;java.nio.file.Files.deleteIfExists(font);java.nio.file.Files.deleteIfExists(directory);
        }
    }
    private static void run(boolean modern)throws Exception{
        Paint.fontVariationOnPaint=modern;ClockContext context=new ClockContext();FontRepository.configure(context,"system","");
        Handler handler=new Handler(Looper.getMainLooper());TextControls controls=new TextControls(handler);
        StatClockSizingCheck.MeasuredClock clock=new StatClockSizingCheck.MeasuredClock(context);clock.setText("native");
        Bundle b=settings();controls.configure(b,StatusBarSettings.COLOR_DEFAULTS,Collections.emptyMap());controls.attach(clock);measure(controls,clock);
        Object entry=entry(controls,clock);Field lower=field(entry,"lowerLayout");StaticLayout layout=(StaticLayout)lower.get(entry);
        check(clock.getLayout()!=null&&layout!=null,"both rows have real layouts");Canvas canvas=new Canvas();
        for(int i=0;i<100;i++){controls.beforeDraw(clock,canvas);controls.afterDraw(clock,canvas);}
        int layouts=clock.layoutRequests,metrics=Paint.fontMetricsObjectsForCheck,scheduled=handler.scheduled,lowerDraws=layout.drawCalls;
        long[] fonts=FontWeight.stats();
        com.sun.management.ThreadMXBean memory=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();
        long allocated=memory.getThreadAllocatedBytes(Thread.currentThread().getId());
        for(int i=0;i<10000;i++){controls.beforeDraw(clock,canvas);controls.afterDraw(clock,canvas);}
        long bytes=memory.getThreadAllocatedBytes(Thread.currentThread().getId())-allocated;
        check(layout==lower.get(entry),"stable draw reuses lower layout");check(clock.layoutRequests==layouts,"stable draw requests no layout");
        check(layout.drawCalls==lowerDraws+10000,"lower row actually draws in every pass");check(handler.scheduled==scheduled,"stable draw creates no tick chain");
        check(FontWeight.stats()[0]==fonts[0],"stable draw performs no font plan lookup: "+(FontWeight.stats()[0]-fonts[0]));
        check(Paint.fontMetricsObjectsForCheck==metrics,"stable draw allocates no font metrics");
        check(bytes<2000000,"10000 stable draws allocate less than 2MB, saw "+bytes);
        System.out.println("ClockSteadyDrawCheck: 10000 draws; allocated="+bytes+" bytes; font lookups="+(FontWeight.stats()[0]-fonts[0]));
        // A changed second must rebuild the lower text, while its fixed digit width leaves the native upper layout alone.
        Method apply=TextControls.class.getDeclaredMethod("apply",TextView.class,entry.getClass(),long.class,boolean.class);apply.setAccessible(true);
        long now=System.currentTimeMillis();int rebuilds=0;Object previous=layout;
        for(int i=1;i<=60;i++){apply.invoke(controls,clock,entry,now+i*1000,false);Object next=lower.get(entry);if(next!=previous)rebuilds++;previous=next;}
        check(rebuilds==60,"every changed second updates lower layout");check(clock.layoutRequests==layouts,"fixed seconds never ask upper view to remeasure");
        // Native color remains live without a font lookup or a lower-layout rebuild.
        previous=lower.get(entry);long hits=FontWeight.stats()[0];controls.nativeColor(clock,0xff123456);clock.setTextColor(0xff123456);
        controls.beforeDraw(clock,canvas);controls.afterDraw(clock,canvas);
        check(previous==lower.get(entry),"color changes reuse geometry");check(((StaticLayout)previous).paintForCheck().getColor()==0xff123456,"native color reaches lower row");
        check(FontWeight.stats()[0]==hits,"color does not redo font work");
        // Configuration, a new OEM family/axes, source publication, density and remeasurement all invalidate real inputs.
        b.putFloat(ClockLayoutSettings.DUAL_LOWER_SCALE,90);controls.configure(b,StatusBarSettings.COLOR_DEFAULTS,Collections.emptyMap());
        check(previous!=lower.get(entry),"lower size/configuration invalidates layout");previous=lower.get(entry);
        Typeface family=Typeface.create(Typeface.DEFAULT,650,false);controls.nativeTypeface(clock,family);controls.refresh();
        check(previous!=lower.get(entry),"native font family invalidates layout");previous=lower.get(entry);
        clock.setFontVariationSettings("'wght' 650, 'wdth' 90");controls.nativeFontVariation(clock);controls.refresh();
        check(previous!=lower.get(entry),"OEM variation axes invalidate layout");previous=lower.get(entry);
        FontRepository.configure(context,"system","probe-font");controls.beforeDraw(clock,canvas);controls.afterDraw(clock,canvas);
        measure(controls,clock); // A native font/style setter requests the normal TextView measure before its next drawn frame.
        check(previous!=lower.get(entry),"a font source publication invalidates lower layout without new clock settings");previous=lower.get(entry);
        clock.getResources().getDisplayMetrics().density=5;controls.beforeDraw(clock,canvas);controls.afterDraw(clock,canvas);
        check(previous!=lower.get(entry),"density invalidates lower layout");
        b.putString(ClockLayoutSettings.DUAL_LOWER_PATTERN,"HH:mm");controls.configure(b,StatusBarSettings.COLOR_DEFAULTS,Collections.emptyMap());
        check(((StaticLayout)lower.get(entry)).textForCheck().toString().matches("\\d{2}:\\d{2}"),"new lower format reaches layout");
        b.putBoolean(ClockLayoutSettings.DUAL_ENABLED,false);controls.configure(b,StatusBarSettings.COLOR_DEFAULTS,Collections.emptyMap());
        check(lower.get(entry)==null,"disabling dual mode releases lower layout");FontRepository.configure(context,"system","");
    }
    public static void main(String[] args)throws Exception{
        boolean modern=Paint.fontVariationOnPaint;
        try{
            if(args.length>0&&"--font-recovery".equals(args[0])){fontRecovery(false);fontRecovery(true);}
            else{run(false);ownershipChanges(false);fontRecovery(false);run(true);ownershipChanges(true);fontRecovery(true);}
        }finally{Paint.fontVariationOnPaint=modern;}
        System.out.println("ClockSteadyDrawCheck: "+checks+" checks passed");
    }
}
