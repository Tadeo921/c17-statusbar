package dev.puitheme;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextPaint;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.IdentityHashMap;

/** Runs the owned OEM digit bridge's actual measurements and switch-animation calls.
 * Only plugin XML creation is replaced: slots are assembled from independent layers. */
public final class NotificationBigClockOptionsCheck {
    private static int checks;
    private static final sun.misc.Unsafe UNSAFE;
    static {try {Field field=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");field.setAccessible(true);UNSAFE=(sun.misc.Unsafe)field.get(null);}
        catch(Exception error){throw new AssertionError(error);}}
    private static void check(boolean condition,String message){checks++;if(!condition)throw new AssertionError(message);}
    private static Field field(Class<?> type,String name)throws Exception {
        for(Class<?> c=type;c!=null;c=c.getSuperclass())try{Field f=c.getDeclaredField(name);f.setAccessible(true);return f;}catch(NoSuchFieldException ignored){}
        throw new NoSuchFieldException(name);
    }
    private static void set(Object owner,String name,Object value)throws Exception{field(owner.getClass(),name).set(owner,value);}
    private static Object get(Object owner,String name)throws Exception{return field(owner.getClass(),name).get(owner);}
    private static Object allocate(String name)throws Exception{return UNSAFE.allocateInstance(Class.forName(name));}
    private static Method method(Class<?> type,String name,Class<?>...args)throws Exception{Method m=type.getDeclaredMethod(name,args);m.setAccessible(true);return m;}
    private static void copyView(Object target,Context context)throws Exception {
        FrameLayout original=new FrameLayout(context);
        for(Class<?> c=FrameLayout.class;c!=null&&c!=Object.class;c=c.getSuperclass())
            for(Field f:c.getDeclaredFields())if(!Modifier.isStatic(f.getModifiers())){f.setAccessible(true);f.set(target,f.get(original));}
    }
    public static final class Transition {boolean running;int reads;public boolean isAnimating(){reads++;return running;}}
    public static final class Layer extends TextView {
        final TextPaint proportionalPaint=new TextPaint();final Paint rt=new Paint();final Transition transition=new Transition();
        public float fontMetricsDrawOffsetY;
        Layer(Context context){super(context);proportionalPaint.setTypeface(Typeface.DEFAULT);}
        @Override public TextPaint getPaint(){return proportionalPaint;}
        @Override public void setTextSize(int unit,float size){super.setTextSize(unit,size);proportionalPaint.setTextSize(size);rt.setTextSize(size);}
        @Override public void setTypeface(Typeface face){super.setTypeface(face);proportionalPaint.setTypeface(face);}
        @Override public void setFontFeatureSettings(String features){super.setFontFeatureSettings(features);proportionalPaint.setFontFeatureSettings(features);}
        @Override public String getFontVariationSettings(){return proportionalPaint.getFontVariationSettings();}
        @Override public boolean setFontVariationSettings(String value){boolean applied=super.setFontVariationSettings(value);proportionalPaint.setFontVariationSettings(value);rt.setFontVariationSettings(value);return applied;}
        public void setFontStyle(Typeface face,boolean one,boolean two){setTypeface(face);}
        public void setBlurRatio(float amount){}
        public Paint getRtTextPaint(){return rt;}
        public Transition getRenderSpringTransition(){return transition;}
    }
    /** 1 and 8 have different natural advances, as in a proportional custom font. */
    public static final class GlyphPaint extends TextPaint {
        @Override public float measureText(String text){return getTextSize()*("1".equals(text)?3f:7f)/10f;}
        @Override public void getTextBounds(String text,int start,int end,Rect bounds){
            float height=getTextSize();String axes=getFontVariationSettings();
            if(axes!=null)for(android.graphics.fonts.FontVariationAxis axis:android.graphics.fonts.FontVariationAxis.fromFontVariationSettings(axes))
                if("HGHT".equals(axis.getTag()))height=axis.getStyleValue();
            bounds.set(0,-(int)height,(int)measureText(text),0);
        }
    }
    public static final class Controller {
        int switches,aborts;
        public void runDigitSwitchAnimation(View digit,boolean hour,boolean stagger){switches++;}
        public void abortIfAnimating(Transition transition,boolean finish){if(transition.running){aborts++;transition.running=false;}}
    }
    private static final class Fixture {
        final NotificationNativeClock clock;final Controller controller=new Controller();final Object slot;
        final Layer visible=new Layer(new Context()),outgoing=new Layer(new Context());
        Fixture(String numeral)throws Exception {
            Context context=new Context();
            clock=(NotificationNativeClock)allocate("dev.puitheme.NotificationNativeClock");copyView(clock,context);
            Object access=allocate("dev.puitheme.NotificationNativeClock$NativeAccess");
            set(access,"controller",controller);set(access,"switchDigit",method(Controller.class,"runDigitSwitchAnimation",View.class,boolean.class,boolean.class));
            set(access,"abort",method(Controller.class,"abortIfAnimating",Transition.class,boolean.class));
            set(access,"getTransition",method(Layer.class,"getRenderSpringTransition"));set(access,"isAnimating",method(Transition.class,"isAnimating"));
            set(access,"setFont",method(Layer.class,"setFontStyle",Typeface.class,boolean.class,boolean.class));
            set(access,"setBlur",method(Layer.class,"setBlurRatio",float.class));set(access,"getRtPaint",method(Layer.class,"getRtTextPaint"));
            set(clock,"nativeAccess",access);set(clock,"available",true);set(clock,"time",numeral);set(clock,"typeface",Typeface.DEFAULT);
            set(clock,"size",100f);set(clock,"originalShaders",new IdentityHashMap<Paint,android.graphics.Shader>());
            slot=allocate("dev.puitheme.NotificationNativeClock$Slot");copyView(slot,context);
            set(slot,"this$0",clock);set(slot,"digit",true);set(slot,"nativeDigit",new FrameLayout(context));
            set(slot,"visible",visible);set(slot,"outgoing",outgoing);set(slot,"ink",new Rect());set(slot,"token",numeral);set(slot,"envelopeSize",-1f);
            // Keep the real layer APIs while supplying glyph metrics independent of OEM XML.
            Field paint=Layer.class.getDeclaredField("proportionalPaint");paint.setAccessible(true);paint.set(visible,new GlyphPaint());paint.set(outgoing,new GlyphPaint());
            visible.setText(numeral);outgoing.setText("");
            visible.setTextSize(0,100f);outgoing.setTextSize(0,100f);
            ArrayList<Object> slots=new ArrayList<>();slots.add(slot);set(clock,"slots",slots);
        }
        int measure() {clock.measure(0,0);return clock.getMeasuredWidth();}
        void proportional(boolean enabled)throws Exception {
            // The old code is a valid red baseline: the missing option leaves its old constraint.
            try{method(NotificationNativeClock.class,"setProportionalDigits",boolean.class).invoke(clock,enabled);}
            catch(NoSuchMethodException oldCode){}
        }
    }
    private static void widths()throws Exception {
        Fixture fixture=new Fixture("1");fixture.clock.setTypography(Typeface.DEFAULT,100f,0xffffffff,.1f);
        int fixed=fixture.measure();check(fixed==70,"fixture must detect the existing 0–9 envelope");
        fixture.proportional(true);int one=fixture.measure();
        check(one==30,"解除等宽后数字 1 应使用自身 30 px 宽度，实际 "+one);
        check(fixture.clock.setTime("8",false,60000L),"native bridge stays available");
        check(fixture.measure()==70,"8 keeps its own 70 px advance");
        fixture.clock.setTime("1",false,120000L);check(fixture.measure()==30,"a new narrow digit is remeasured");
        check((Float)get(fixture.clock,"spacing")==.1f,"解除等宽保留用户字距");
        check(fixture.visible.getFontFeatureSettings().contains("'tnum' 0"),"visible font disables tabular figures");
        check(fixture.visible.rt.getFontFeatureSettings().equals(fixture.visible.getFontFeatureSettings()),"native RT paint shares numeral features");
        Fixture second=new Fixture("1");set(second.slot,"this$0",fixture.clock);
        ((java.util.List<Object>)get(fixture.clock,"slots")).add(second.slot);
        check(fixture.measure()==70,"两个 30 px 数字仍保留用户 10 px 字距");
        fixture.clock.setTypography(Typeface.DEFAULT,100f,0xffffffff,-.1f);
        check(fixture.measure()==50,"负字距仍按用户选择收紧");
        ((java.util.List<Object>)get(fixture.clock,"slots")).remove(second.slot);
        fixture.proportional(false);check(fixture.measure()==fixed,"switching back restores fixed digit slots");
        check("tnum".equals(fixture.visible.getFontFeatureSettings()),"default feature recipe is restored");
        for(int i=0;i<500;i++)check(fixture.measure()==fixed,"stable fixed frames");
    }
    private static void settingsAndFallback()throws Exception {
        Context context=new Context();Bundle bundle=new Bundle();
        Class<?> type=Class.forName("dev.puitheme.NotificationBigClock$Settings");
        Constructor<?> settingsCtor=type.getDeclaredConstructor(Bundle.class,String.class);settingsCtor.setAccessible(true);
        for(String prefix:new String[]{"notification_big_clock_","notification_big_clock_landscape_"}) {
            check(Boolean.FALSE.equals(NotificationBigClockSettings.BOOLEANS.get(prefix+"proportional_digits")),"等宽解除默认关闭");
            check(Boolean.FALSE.equals(NotificationBigClockSettings.BOOLEANS.get(prefix+"time_animation_disabled")),"动画关闭默认不启用");
            Object defaults=settingsCtor.newInstance(bundle,prefix);
            check(Boolean.FALSE.equals(get(defaults,"proportionalDigits")),"runtime numeral default");
            check(Boolean.FALSE.equals(get(defaults,"timeAnimationDisabled")),"runtime animation default");
        }
        bundle.putBoolean(NotificationBigClockSettings.PROPORTIONAL_DIGITS,true);
        bundle.putBoolean(NotificationBigClockSettings.TIME_ANIMATION_DISABLED,true);
        bundle.putString(NotificationBigClockSettings.FONT,"system");
        Object portrait=settingsCtor.newInstance(bundle,NotificationBigClockSettings.PORTRAIT_PREFIX);
        Object landscape=settingsCtor.newInstance(bundle,NotificationBigClockSettings.LANDSCAPE_PREFIX);
        check(Boolean.TRUE.equals(get(portrait,"proportionalDigits"))&&Boolean.FALSE.equals(get(landscape,"proportionalDigits")),"横竖屏各自保存等宽选项");
        check(Boolean.TRUE.equals(get(portrait,"timeAnimationDisabled"))&&Boolean.FALSE.equals(get(landscape,"timeAnimationDisabled")),"横竖屏各自保存动画选项");
        check("'kern' 1, 'tnum' 0, 'pnum' 1".equals(NotificationBigClockSettings.timeFontFeatures("'kern' 1, tnum, 'pnum' 0",true)),"只移除数字宽度限制，保留其它字体特性");
        check("'kern' 1, tnum".equals(NotificationBigClockSettings.timeFontFeatures("'kern' 1, tnum",false)),"未解除时保留原配方");
        NotificationBigClock parent=new NotificationBigClock();set(parent,"settings",portrait);
        Object overlay=allocate("dev.puitheme.NotificationBigClock$ClockView");copyView(overlay,context);set(overlay,"this$0",parent);
        set(overlay,"faces",new java.util.HashMap<Integer,Typeface>());set(overlay,"typography",new java.util.WeakHashMap<TextView,Object>());
        Constructor<?> style=NotificationClockStyle.Style.class.getDeclaredConstructor(Context.class,Typeface.class);style.setAccessible(true);
        set(overlay,"nativeStyle",style.newInstance(context,Typeface.DEFAULT));
        Constructor<?> clockCtor=Class.forName("dev.puitheme.NotificationBigClock$ClockText").getDeclaredConstructor(NotificationBigClock.class,Context.class);clockCtor.setAccessible(true);
        TextView fallback=(TextView)clockCtor.newInstance(parent,context),date=new TextView(context);
        Method typography=method(overlay.getClass(),"textStyle",TextView.class,String.class,float.class,int.class,float.class,float.class);
        typography.invoke(overlay,fallback,"11:18",100f,600,.12f,1f);
        check(fallback.getFontFeatureSettings().contains("'tnum' 0"),"无 OEM 插件时 fallback 也解除等宽");
        check(fallback.getLetterSpacing()==.12f,"fallback保留字距值");
        typography.invoke(overlay,date,"10月13日",18f,600,0f,1f);
        check("tnum".equals(date.getFontFeatureSettings()),"大时钟解除不改日期/底部标签的原生特性");
    }
    private static void ticks(String prefix,boolean disabled)throws Exception {
        Bundle bundle=new Bundle();bundle.putBoolean(prefix+"enabled",true);bundle.putBoolean(prefix+"time_animation_disabled",disabled);
        bundle.putBoolean(prefix+"date_enabled",false);bundle.putString(prefix+"pattern","m");
        Class<?> settings=Class.forName("dev.puitheme.NotificationBigClock$Settings");
        Constructor<?> ctor=settings.getDeclaredConstructor(Bundle.class,String.class);ctor.setAccessible(true);Object value=ctor.newInstance(bundle,prefix);
        NotificationBigClock parent=new NotificationBigClock();set(parent,"settings",value);set(parent,"active",true);set(parent,"fraction",1f);set(parent,"barState",0);
        FrameLayout host=new FrameLayout(new Context());host.getResources().getConfiguration().orientation=prefix.contains("landscape")?2:1;
        set(parent,"panel",new WeakReference<View>(host));
        ((java.util.Map<Object,Object>)get(parent,"headers")).put(host,null);
        Object overlay=allocate("dev.puitheme.NotificationBigClock$ClockView");copyView(overlay,new Context());set(overlay,"this$0",parent);
        Fixture fixture=new Fixture("1");set(overlay,"nativeClock",fixture.clock);
        Constructor<?> clockCtor=Class.forName("dev.puitheme.NotificationBigClock$ClockText").getDeclaredConstructor(NotificationBigClock.class,Context.class);
        clockCtor.setAccessible(true);set(overlay,"clock",clockCtor.newInstance(parent,new Context()));
        set(overlay,"dateLabel",new TextView(new Context()));set(overlay,"footerLabel",new TextView(new Context()));
        set(overlay,"time","1");set(overlay,"date","");set(overlay,"footer","");set(overlay,"textSettings",value);set(overlay,"textBucket",1L);
        check(parent.hasVisibleTime(),"the minute tick occurs on a visible clock");
        method(overlay.getClass(),"refreshText",boolean.class,long.class).invoke(overlay,false,120000L);
        check("2".equals(fixture.visible.getText()),"minute text changes immediately");
        check(fixture.controller.switches==(disabled?0:1),"关闭时间变动动画仅阻止 digit switch："+prefix+disabled+" calls="+fixture.controller.switches);
        check(fixture.visible.getAlpha()==1f&&fixture.outgoing.getVisibility()==View.GONE,"immediate update settles native layers");
        if(!disabled) {
            fixture.visible.transition.running=true;fixture.outgoing.transition.running=true;
            fixture.visible.setAlpha(.4f);fixture.outgoing.setVisibility(View.VISIBLE);
            bundle.putBoolean(prefix+"time_animation_disabled",true);Object stopped=ctor.newInstance(bundle,prefix);
            set(parent,"settings",stopped);set(overlay,"textSettings",stopped);
            // Same time bucket: no new text is needed, but an in-flight switch must settle.
            method(overlay.getClass(),"refreshText",boolean.class,long.class).invoke(overlay,false,120000L);
            check(fixture.controller.aborts==2,"disabling aborts already running independent digit layers");
            check(fixture.visible.getAlpha()==1f&&fixture.outgoing.getVisibility()==View.GONE,"same-minute disable settles immediately");
            check(fixture.controller.switches==1,"same-minute disable starts no replacement animation");
        }
    }
    private static void clippingGeometry()throws Exception {
        boolean padding=View.keepPaddingForCheck;View.keepPaddingForCheck=true;
        try {
        NotificationBigClock owner=new NotificationBigClock();
        Constructor<?> ctor=Class.forName("dev.puitheme.NotificationBigClock$ClockText").getDeclaredConstructor(NotificationBigClock.class,Context.class);
        ctor.setAccessible(true);TextView fallback=(TextView)ctor.newInstance(owner,new Context());
        fallback.setText("12:34");fallback.setTextSize(0,100f);set(fallback,"glass",true);set(fallback,"glassBorder",true);set(fallback,"borderWidth",40f);
        fallback.measure(0,0);Rect ink=new Rect();fallback.getPaint().getTextBounds("12:34",0,5,ink);
        check(fallback.getMeasuredHeight()>=ink.height()+42,"owned text must allocate half the 40px border plus antialias room on both edges: height="+fallback.getMeasuredHeight());
        } finally {View.keepPaddingForCheck=padding;}
    }
    private static void fallbackSideBearings()throws Exception {
        boolean padding=View.keepPaddingForCheck,measure=TextView.measureTextWidth;View.keepPaddingForCheck=TextView.measureTextWidth=true;
        try {
            NotificationBigClock owner=new NotificationBigClock();Constructor<?> ctor=Class.forName("dev.puitheme.NotificationBigClock$ClockText").getDeclaredConstructor(NotificationBigClock.class,Context.class);ctor.setAccessible(true);
            TextView text=(TextView)ctor.newInstance(owner,new Context());TextPaint slanted=new TextPaint(){
                @Override public void getTextBounds(String source,int start,int end,Rect out){out.set(-20,-80,145,20);}
                @Override public float measureText(CharSequence source,int start,int end){return 125f;}};
            field(TextView.class,"paint").set(text,slanted);text.setText("clock");text.setTextSize(0,100f);text.measure(0,0);
            check(text.getPaddingLeft()>=20&&text.getMeasuredWidth()>=text.getPaddingLeft()+145,"private fallback allocates the actual font's left/right ink overhang instead of clipping italic glyphs");
        }finally{View.keepPaddingForCheck=padding;TextView.measureTextWidth=measure;}
    }
    private static void allTextAppearance()throws Exception {
        Context context=new Context();NotificationBigClock owner=new NotificationBigClock();
        Object overlay=allocate("dev.puitheme.NotificationBigClock$ClockView");copyView(overlay,context);set(overlay,"this$0",owner);
        Class<?> textType=Class.forName("dev.puitheme.NotificationBigClock$ClockText"),labelType=Class.forName("dev.puitheme.NotificationBigClock$ClockLabel");
        Constructor<?> textCtor=textType.getDeclaredConstructor(NotificationBigClock.class,Context.class),labelCtor=labelType.getDeclaredConstructor(NotificationBigClock.class,Context.class);
        textCtor.setAccessible(true);labelCtor.setAccessible(true);
        TextView digits=(TextView)textCtor.newInstance(owner,context),date=(TextView)labelCtor.newInstance(owner,context),footer=(TextView)labelCtor.newInstance(owner,context);
        digits.setText("12:34");date.setText("日期");footer.setText("附文");
        digits.setTextSize(0,100f);date.setTextSize(0,18f);footer.setTextSize(0,24f);
        set(overlay,"clock",digits);set(overlay,"timeProbe",textCtor.newInstance(owner,context));set(overlay,"dateLabel",date);set(overlay,"footerLabel",footer);set(overlay,"probe",labelCtor.newInstance(owner,context));
        FrameLayout root=new FrameLayout(context);root.addView((View)overlay);((FrameLayout)overlay).addView(digits);((FrameLayout)overlay).addView(date);((FrameLayout)overlay).addView(footer);
        Constructor<?> settingsCtor=Class.forName("dev.puitheme.NotificationBigClock$Settings").getDeclaredConstructor(Bundle.class,String.class);settingsCtor.setAccessible(true);
        Bundle saved=new Bundle();saved.putInt(NotificationBigClockSettings.COLOR_LIGHT,0x806699cc);saved.putInt(NotificationBigClockSettings.COLOR_DARK,0x90445566);
        saved.putBoolean(StatusBarSettings.alphaKey(NotificationBigClockSettings.COLOR_LIGHT),true);saved.putBoolean(StatusBarSettings.alphaKey(NotificationBigClockSettings.COLOR_DARK),true);
        saved.putInt(NotificationBigClockSettings.DATE_COLOR_LIGHT,0xff00ff00);saved.putInt(NotificationBigClockSettings.FOOTER_COLOR_LIGHT,0xffff0000);
        saved.putFloat(NotificationBigClockSettings.GLASS_BORDER_WIDTH,12f);
        Object settings=settingsCtor.newInstance(saved,NotificationBigClockSettings.PORTRAIT_PREFIX);
        Method colors=method(overlay.getClass(),"applyTextColors",settings.getClass(),int.class,boolean.class),appearance=method(overlay.getClass(),"prepareAppearance",settings.getClass());
        colors.invoke(overlay,settings,0xffffffff,false);appearance.invoke(overlay,settings);
        check(date.getCurrentTextColor()==0xff00ff00&&footer.getCurrentTextColor()==0xffff0000,"default independent date/footer retain saved colours");
        int baseColor=digits.getCurrentTextColor();check(baseColor==0x806699cc,"baseline includes the saved font alpha");
        saved.putBoolean(NotificationBigClockSettings.GLASS,true);saved.putBoolean(NotificationBigClockSettings.GLASS_BORDER_ENABLED,true);
        saved.putBoolean(NotificationBigClockSettings.LOCK_STYLE,true);saved.putBoolean(NotificationBigClockSettings.LOCK_STYLE_NATIVE,true);
        saved.putBoolean(NotificationBigClockSettings.DIGIT_REVEAL_ENABLED,true);saved.putFloat(NotificationBigClockSettings.DIGIT_REVEAL_AMOUNT,0f);
        saved.putInt(NotificationBigClockSettings.REVEAL_MASK_COLOR,0x80336699);saved.putBoolean(StatusBarSettings.alphaKey(NotificationBigClockSettings.REVEAL_MASK_COLOR),true);
        saved.putFloat(NotificationBigClockSettings.REVEAL_MASK_OPACITY,50f);
        settings=settingsCtor.newInstance(saved,NotificationBigClockSettings.PORTRAIT_PREFIX);set(owner,"settings",settings);
        colors.invoke(overlay,settings,0xffffffff,false);appearance.invoke(overlay,settings);
        check(digits.getCurrentTextColor()==0xffffffff,"background material receives opaque glyph coverage, regardless of retired amount, selected font alpha or tint alpha");
        check(Boolean.FALSE.equals(get(settings,"glass"))&&Boolean.FALSE.equals(get(settings,"lockStyle"))&&Boolean.FALSE.equals(get(settings,"lockNative")),"dedicated backdrop mode bypasses all saved native/glass materials");
        check(Boolean.FALSE.equals(get(digits,"glass"))&&Boolean.FALSE.equals(get(date,"glass")),"actual text appearance does not leave a gradient beneath backdrop mode");
        check(date.getCurrentTextColor()==0xff00ff00&&footer.getCurrentTextColor()==0xffff0000,"backdrop digits do not recolour independent labels");
        for(String name:new String[]{"clockBackdrop","nativeBackdrop","dateBackdrop","footerBackdrop"})set(overlay,name,new ClockBackdropMaterial());
        set(overlay,"borrowedColour",new ClockMaterialMask());
        ClockBackdropSource.clear();com.android.systemui.scrim.ScrimView scrim=new com.android.systemui.scrim.ScrimView();scrim.setId(1);
        scrim.getResources().entryNames.put(1,"scrim_behind");root.addView(scrim);
        com.oplusos.systemui.common.blurability.platformblur.PlatformBlurHelper helper=new com.oplusos.systemui.common.blurability.platformblur.PlatformBlurHelper(scrim);
        com.oplusos.systemui.common.blurability.platformblur.PlatformBlurDrawable wrapper=new com.oplusos.systemui.common.blurability.platformblur.PlatformBlurDrawable();
        wrapper.blurDrawable.params.radius=0;
        try(ClockBackdropSource.Scope scope=ClockBackdropSource.beginHelper(helper)){ClockBackdropSource.created(wrapper);}
        com.oplus.posteffect.BlurDrawable engine=wrapper.blurDrawable;engine.setBounds(0,0,800,800);engine.params.radius=40;
        ClockBackdropSource.onParams(engine,engine.params);ClockBackdropSource.onRegistration(engine);engine.addBlurDrawable();ClockBackdropSource.onRegistration(engine);
        android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(800,800,android.graphics.Bitmap.Config.ARGB_8888);bitmap.eraseColor(0xff4b90d8);
        engine.completeForCheck(bitmap);engine.preDrawContent();ClockBackdropSource.onDrawn(engine);
        check(ClockBackdropSource.snapshot((View)overlay)!=null,"callsite uses a real observed existing system frame");
        Method update=method(overlay.getClass(),"updateBackdrops"),clear=method(overlay.getClass(),"clearMaterials");
        update.invoke(overlay);
        check(digits.renderEffect!=null&&digits.renderEffect.shader.inputs.get("background") instanceof android.graphics.BitmapShader,"actual ClockView callsite binds the observed clean background shader");
        check(((android.graphics.BitmapShader)digits.renderEffect.shader.inputs.get("background")).bitmap==bitmap,"ClockView borrows the existing source bitmap by identity");
        check(date.renderEffect==null&&footer.renderEffect==null,"independent date/footer have no backdrop effect");
        float[] tint=digits.renderEffect.shader.floats.get("tint");check(Math.abs(tint[3]-128f/255f*.5f)<.00001f,"colour alpha and tint strength are applied once at the material, independently of glyph alpha");
        Fixture nativeMask=new Fixture("1");set(overlay,"nativeClock",nativeMask.clock);((FrameLayout)overlay).addView(nativeMask.clock);
        appearance.invoke(overlay,settings);nativeMask.clock.setTypography(Typeface.DEFAULT,100f,digits.getCurrentTextColor(),0f);update.invoke(overlay);
        check(Boolean.TRUE.equals(get(nativeMask.clock,"colorMaskEnabled"))&&nativeMask.clock.renderEffect!=null,"native row receives one parent backdrop and disables native paint shaders");
        check(nativeMask.visible.getPaint().getColor()==0xffffffff&&nativeMask.visible.rt.getColor()==0xffffffff,"actual resolved coverage reaches both ordinary and RT native paints");
        saved.putFloat(NotificationBigClockSettings.REVEAL_MASK_OPACITY,0f);settings=settingsCtor.newInstance(saved,NotificationBigClockSettings.PORTRAIT_PREFIX);set(owner,"settings",settings);
        colors.invoke(overlay,settings,0xffffffff,false);appearance.invoke(overlay,settings);update.invoke(overlay);
        check(digits.getCurrentTextColor()==0xffffffff&&Boolean.TRUE.equals(get(nativeMask.clock,"colorMaskEnabled")),"zero tint strength keeps opaque glyph coverage and the native backdrop mode");
        check(digits.renderEffect.shader.inputs.containsKey("background")&&digits.renderEffect.shader.floats.get("tint")[3]==0f,"zero tint strength still binds clean blur rather than restoring transparent text");
        saved.putFloat(NotificationBigClockSettings.REVEAL_MASK_OPACITY,100f);saved.putInt(NotificationBigClockSettings.REVEAL_MASK_COLOR,0x00336699);
        settings=settingsCtor.newInstance(saved,NotificationBigClockSettings.PORTRAIT_PREFIX);set(owner,"settings",settings);appearance.invoke(overlay,settings);update.invoke(overlay);
        check(Boolean.TRUE.equals(get(nativeMask.clock,"colorMaskEnabled"))&&digits.renderEffect.shader.floats.get("tint")[3]==0f,"transparent mask colour also preserves the clean backdrop");
        saved.putBoolean(NotificationBigClockSettings.FOLLOW_STYLE,true);saved.putInt(NotificationBigClockSettings.REVEAL_MASK_COLOR,0xffffffff);saved.putFloat(NotificationBigClockSettings.REVEAL_MASK_OPACITY,20f);
        settings=settingsCtor.newInstance(saved,NotificationBigClockSettings.PORTRAIT_PREFIX);set(owner,"settings",settings);
        for(boolean dark:new boolean[]{false,true}) {
            colors.invoke(overlay,settings,0xffffffff,dark);appearance.invoke(overlay,settings);update.invoke(overlay);
            check(date.getCurrentTextColor()==0xffffffff&&footer.getCurrentTextColor()==0xffffffff,"following labels use opaque coverage in both background modes");
            check(date.renderEffect!=null&&footer.renderEffect!=null&&date.renderEffect!=footer.renderEffect&&date.renderEffect!=digits.renderEffect,"date/footer own exactly one backdrop each, independently of the digit effect");
        }
        int writes=digits.renderEffectWrites+date.renderEffectWrites+footer.renderEffectWrites+nativeMask.clock.renderEffectWrites;
        for(int i=0;i<100;i++)update.invoke(overlay);
        check(writes==digits.renderEffectWrites+date.renderEffectWrites+footer.renderEffectWrites+nativeMask.clock.renderEffectWrites,"stable actual callsite reuses all material bindings");
        check(digits.getTextSize()==100f&&date.getTextSize()==18f&&footer.getTextSize()==24f,"material changes preserve independent font sizes");
        clear.invoke(overlay);check(digits.renderEffect==null&&date.renderEffect==null&&footer.renderEffect==null&&nativeMask.clock.renderEffect==null,"configuration clearing releases every owned backdrop");
        saved.putBoolean(NotificationBigClockSettings.DIGIT_REVEAL_ENABLED,false);saved.putBoolean(NotificationBigClockSettings.LOCK_STYLE,false);
        settings=settingsCtor.newInstance(saved,NotificationBigClockSettings.PORTRAIT_PREFIX);set(owner,"settings",settings);colors.invoke(overlay,settings,0xffffffff,false);appearance.invoke(overlay,settings);update.invoke(overlay);
        check(digits.getCurrentTextColor()==baseColor&&Boolean.TRUE.equals(get(digits,"glass"))&&Boolean.FALSE.equals(get(nativeMask.clock,"colorMaskEnabled")),"turning mode off restores saved font alpha/glass selection and native shader choice");
        check(date.getCurrentTextColor()==0xff00ff00&&footer.getCurrentTextColor()==0xffff0000&&date.renderEffect==null&&footer.renderEffect==null,"mode off restores independent label colours without stale backdrop");
        saved.putBoolean(NotificationBigClockSettings.DIGIT_REVEAL_ENABLED,true);saved.putBoolean(StatusBarSettings.SAFE_MODE,true);
        settings=settingsCtor.newInstance(saved,NotificationBigClockSettings.PORTRAIT_PREFIX);set(owner,"settings",settings);colors.invoke(overlay,settings,0xffffffff,false);appearance.invoke(overlay,settings);update.invoke(overlay);
        check(digits.getCurrentTextColor()==baseColor&&Boolean.FALSE.equals(get(nativeMask.clock,"colorMaskEnabled"))&&digits.renderEffect==null,"safety mode bypasses backdrop without discarding the selected base colour");
        check(date.getCurrentTextColor()==0xff00ff00&&footer.getCurrentTextColor()==0xffff0000,"safety mode also suspends saved appearance following");
        check(saved.getBoolean(NotificationBigClockSettings.GLASS)&&saved.getBoolean(NotificationBigClockSettings.GLASS_BORDER_ENABLED)&&saved.getBoolean(NotificationBigClockSettings.LOCK_STYLE_NATIVE)&&saved.getBoolean(NotificationBigClockSettings.FOLLOW_STYLE),"runtime bypass leaves every saved material/follow preference intact");
        check(saved.getInt(NotificationBigClockSettings.COLOR_LIGHT)==0x806699cc&&saved.getInt(NotificationBigClockSettings.DATE_COLOR_LIGHT)==0xff00ff00,"runtime bypass never rewrites independent colour preferences");
        clear.invoke(overlay);set(overlay,"nativeClock",null);nativeMask.clock.detach();ClockBackdropSource.clear();
    }
    private static void nativeVisualSpace()throws Exception {
        Fixture fixture=new Fixture("1");fixture.clock.setTypography(Typeface.DEFAULT,100f,0xffffffff,0f);fixture.measure();int width=fixture.clock.getMeasuredWidth(),height=fixture.clock.getMeasuredHeight();
        fixture.clock.setVisualOutset(21f);fixture.measure();
        check(fixture.clock.getMeasuredWidth()==width+42&&fixture.clock.getMeasuredHeight()==height+42,"native row reserves both border/glow edges without resizing the digit");
        method(NotificationNativeClock.class,"layoutSlots").invoke(fixture.clock);
        check(((View)fixture.slot).getLeft()==21&&((View)fixture.slot).getTop()==21,"native animation slots stay inside the row's reserved visual edges");
        fixture.clock.setVisualOutset(0);fixture.measure();
        check(fixture.clock.getMeasuredWidth()==width&&fixture.clock.getMeasuredHeight()==height,"plain appearance returns to the original native geometry");
        set(get(fixture.clock,"nativeAccess"),"drawOffset",Layer.class.getDeclaredField("fontMetricsDrawOffsetY"));fixture.visible.fontMetricsDrawOffsetY=17.5f;
        method(fixture.slot.getClass(),"registerBorderLayers").invoke(fixture.slot);
        try {check(NotificationBigClock.maskOriginY(fixture.visible,fixture.visible.getLayout())==17.5f,"mask origin includes the current ROM native text draw offset exactly once");}
        finally {method(NotificationNativeClock.class,"unregisterBorderLayers").invoke(fixture.clock);}
    }
    private static void nativePaintAxesInvalidateEnvelope()throws Exception {
        boolean old=Paint.fontVariationOnPaint;Paint.fontVariationOnPaint=true;
        try {
            Fixture fixture=new Fixture("1");
            fixture.clock.setTypography(Typeface.DEFAULT,100f,0xffffffff,0f,"'HGHT' 80");fixture.measure();
            int before=fixture.clock.getMeasuredHeight();Typeface same=fixture.visible.getPaint().getTypeface();
            check(before==80,"the real envelope reads the first Paint-only HGHT value");
            fixture.clock.setTypography(Typeface.DEFAULT,100f,0xffffffff,0f,"'HGHT' 140");fixture.measure();
            check(fixture.visible.getPaint().getTypeface()==same,"Android36 fixture keeps native face identity while changing Paint axes");
            check(fixture.clock.getMeasuredHeight()>before&&fixture.clock.getMeasuredHeight()>=140,
                    "native digit envelope must follow paint-only HGHT changes: "+before+" -> "+fixture.clock.getMeasuredHeight());
        } finally {Paint.fontVariationOnPaint=old;}
    }
    private static void nativeBodyLifecycle()throws Exception {
        int base=0x806699cc,mixed=0x8088aadd;
        Fixture fixture=new Fixture("1");fixture.clock.setTypography(Typeface.DEFAULT,100f,mixed,0f);fixture.measure();
        method(fixture.slot.getClass(),"registerBorderLayers").invoke(fixture.slot);
        try {
            check(NotificationNativeClock.ownsLayer(fixture.visible)&&!NotificationNativeClock.ownsLayer(new TextView(new Context())),"only independently registered native layers enter the existing draw seam");
            check(fixture.visible.rt.getColor()==mixed&&fixture.outgoing.rt.getColor()==mixed,
                    "precomputed mask colour reaches the separate visible and outgoing RT paints");
            check(fixture.visible.getPaint().getColor()==mixed&&fixture.outgoing.getPaint().getColor()==mixed,
                    "ordinary native paints receive the same precomputed mask colour");
            android.graphics.Shader supplied=new android.graphics.LinearGradient(0f,0f,0f,100f,0xffffffff,0xff000000,android.graphics.Shader.TileMode.CLAMP);
            fixture.clock.setPaintShader(supplied);fixture.clock.setGlass(true,1f);
            check(fixture.visible.getPaint().getShader()==supplied&&fixture.visible.rt.getShader()==supplied,"unmasked native glass retains its supplied shader");
            fixture.clock.setColorMaskEnabled(true);android.graphics.Shader masked=fixture.visible.getPaint().getShader();
            check(masked==null&&fixture.visible.rt.getShader()==null,"backdrop mode clears every native paint shader instead of adding a second dim gradient");
            fixture.clock.setColorMaskEnabled(true);
            check(fixture.visible.getPaint().getShader()==masked,"stable backdrop state keeps native paint shaders cleared");
            fixture.clock.setColorMaskEnabled(false);
            check(fixture.visible.getPaint().getShader()==supplied&&fixture.visible.rt.getShader()==supplied,"disabling the mask restores the supplied glass shader");
            fixture.clock.setGlass(false,1f);
            android.graphics.Canvas canvas=new android.graphics.Canvas();canvas.hardware=true;int[] nativeCalls={0};
            int originalAlpha=fixture.visible.getPaint().getAlpha(),originalRtAlpha=fixture.visible.rt.getAlpha();
            try{NotificationNativeClock.drawOwned(fixture.visible,canvas,target->{nativeCalls[0]++;check(target==canvas,"native draw retains its original Canvas");target.drawText("1",0,1,0,80,fixture.visible.getPaint());});}
            catch(Throwable failed){throw new AssertionError(failed);}
            check(nativeCalls[0]==1,"colour mask never duplicates native glyph drawing");
            check(originalAlpha==0x80&&originalRtAlpha==0x80&&fixture.visible.getPaint().getAlpha()==originalAlpha&&fixture.visible.rt.getAlpha()==originalRtAlpha,
                    "selected text alpha is applied exactly once and drawing leaves both paints untouched");
            fixture.clock.setTimeAnimationEnabled(true);fixture.clock.setTime("2",true,1L);fixture.visible.transition.running=true;
            fixture.outgoing.transition.running=true;fixture.visible.setAlpha(.4f);fixture.outgoing.setAlpha(.25f);
            int switches=fixture.controller.switches,aborts=fixture.controller.aborts;
            check(switches==1,"minute change retains the native switch animation");
            int transitionReads=fixture.visible.transition.reads+fixture.outgoing.transition.reads;
            for(int i=0;i<100;i++)try{NotificationNativeClock.drawOwned(fixture.visible,canvas,target->{check(target==canvas,"animated glyph keeps its original Canvas");nativeCalls[0]++;});}
            catch(Throwable failed){throw new AssertionError(failed);}
            check(nativeCalls[0]==101&&transitionReads==fixture.visible.transition.reads+fixture.outgoing.transition.reads,
                    "steady drawing adds no background work or transition polling");
            fixture.clock.setTypography(Typeface.DEFAULT,100f,base,0f);
            check(fixture.visible.getPaint().getColor()==base&&fixture.visible.rt.getColor()==base&&fixture.outgoing.rt.getColor()==base,
                    "turning off the mask restores the base colour on both native paint paths");
            check(fixture.visible.getAlpha()==.4f&&fixture.outgoing.getAlpha()==.25f&&fixture.controller.switches==switches&&fixture.controller.aborts==aborts,
                    "colour changes preserve the native animated layer alpha and do not restart or cancel transitions");
            fixture.clock.setTypography(Typeface.DEFAULT,100f,mixed,0f);
            check(fixture.visible.getPaint().getColor()==mixed&&fixture.visible.rt.getColor()==mixed,"mask colour can be reapplied without stale native paint");
            java.util.Map<?,?> owned=(java.util.Map<?,?>)getStatic(NotificationNativeClock.class,"OWNED_LAYERS");
            Object layer=owned.get(fixture.visible);
            for(Field field:layer.getClass().getDeclaredFields())check(!field.getType().getName().contains("ShadeClockReveal")&&!field.getType().getName().equals("android.graphics.RenderNode"),
                    "native layer retains no background material body or offscreen draw commands");
            fixture.clock.detach();check(!NotificationNativeClock.ownsLayer(fixture.visible)&&!NotificationNativeClock.ownsLayer(fixture.outgoing),"native detach unregisters both independent layers");
        }finally{fixture.clock.detach();}
    }
    private static Object getStatic(Class<?> type,String name)throws Exception {Field field=field(type,name);return field.get(null);}
    private static void componentClickVisibility()throws Exception {
        Context context=new Context();FrameLayout host=new FrameLayout(context);
        int orientation=host.getResources().getConfiguration().orientation;host.getResources().getConfiguration().orientation=1;
        NotificationBigClock owner=new NotificationBigClock();
        Constructor<?> ctor=Class.forName("dev.puitheme.NotificationBigClock$ClockView").getDeclaredConstructor(NotificationBigClock.class,Context.class);ctor.setAccessible(true);
        View overlay=(View)ctor.newInstance(owner,context);host.addView(overlay);
        FrameLayout header=(FrameLayout)get(overlay,"headerMotion");View target=new TextView(context);header.addView(target);
        Bundle saved=new Bundle();saved.putBoolean(NotificationBigClockSettings.MASTER,true);
        Constructor<?> settings=Class.forName("dev.puitheme.NotificationBigClock$Settings").getDeclaredConstructor(Bundle.class,String.class);settings.setAccessible(true);
        set(owner,"settings",settings.newInstance(saved,NotificationBigClockSettings.PORTRAIT_PREFIX));set(owner,"panel",new WeakReference<View>(host));
        set(owner,"active",true);set(owner,"barState",0);set(owner,"fraction",1f);
        ((java.util.Map<Object,Object>)get(owner,"headers")).put(host,null);
        ClockLaunchHelper launch=(ClockLaunchHelper)get(owner,"clockLauncher");
        launch.setActivityStarter(new com.android.systemui.plugins.ActivityStarter(){
            public void postStartActivityDismissingKeyguard(android.content.Intent intent,int delay){}
            public void startActivity(android.content.Intent intent,boolean dismiss){}
        });
        Method allowed=method(overlay.getClass(),"componentClickAllowed",View.class),legacy=method(overlay.getClass(),"clickAllowed",View.class,boolean.class);
        try {
            check(Boolean.TRUE.equals(allowed.invoke(overlay,target)),"actual owner allows a visible component only on the fully expanded portrait notification page");
            for(float fraction:new float[]{0f,.25f,.5f,.998f,Float.NaN}){set(owner,"fraction",fraction);check(Boolean.FALSE.equals(allowed.invoke(overlay,target)),"actual component policy rejects closing/partially expanded state: "+fraction);}
            set(owner,"fraction",.5f);check(Boolean.TRUE.equals(legacy.invoke(overlay,target,false)),"component-only expansion guard preserves the existing visible clock/date click policy");
            set(owner,"fraction",1f);header.setTranslationX(1f);check(Boolean.FALSE.equals(allowed.invoke(overlay,target)),"actual owner blocks component clicks during horizontal page displacement");
            header.setTranslationX(0f);set(owner,"pageMotion",new NotificationBigClock.PageMotion(){public void register(View view){}public void unregister(View view){}public boolean isRunning(){return true;}});
            check(Boolean.FALSE.equals(allowed.invoke(overlay,target)),"actual owner blocks component clicks while native page motion is running");set(owner,"pageMotion",null);
            header.setAlpha(0f);check(Boolean.FALSE.equals(allowed.invoke(overlay,target)),"actual owner respects inherited animated alpha");header.setAlpha(1f);
            host.getResources().getConfiguration().orientation=2;check(Boolean.FALSE.equals(allowed.invoke(overlay,target)),"actual owner never activates portrait component clicks in landscape");host.getResources().getConfiguration().orientation=1;
            saved.putBoolean(StatusBarSettings.SAFE_MODE,true);set(owner,"settings",settings.newInstance(saved,NotificationBigClockSettings.PORTRAIT_PREFIX));check(Boolean.FALSE.equals(allowed.invoke(overlay,target)),"actual owner disables component clicks in safety mode");
            saved.putBoolean(StatusBarSettings.SAFE_MODE,false);set(owner,"settings",settings.newInstance(saved,NotificationBigClockSettings.PORTRAIT_PREFIX));set(owner,"active",false);check(Boolean.FALSE.equals(allowed.invoke(overlay,target)),"departing owners cannot open component apps");
        } finally {launch.clear();host.getResources().getConfiguration().orientation=orientation;}
    }
    public static void main(String[] args)throws Exception {
        if(args.length>0&&"bearings".equals(args[0])){fallbackSideBearings();return;}
        if(args.length>0&&"clip".equals(args[0])){clippingGeometry();return;}
        if(args.length>0&&"axes".equals(args[0])){nativePaintAxesInvalidateEnvelope();return;}
        if(args.length==0||!"animation".equals(args[0]))widths();
        for(String prefix:new String[]{"notification_big_clock_","notification_big_clock_landscape_"}){ticks(prefix,false);ticks(prefix,true);}
        if(args.length==0){settingsAndFallback();clippingGeometry();nativePaintAxesInvalidateEnvelope();nativeVisualSpace();allTextAppearance();fallbackSideBearings();nativeBodyLifecycle();componentClickVisibility();}
        System.out.println("Notification big-clock options checks passed: "+checks);
    }
}
