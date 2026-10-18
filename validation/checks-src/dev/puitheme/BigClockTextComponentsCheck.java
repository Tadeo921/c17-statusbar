package dev.puitheme;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import java.util.TimeZone;

/** Production text components with desktop layout and native-weather fixtures. */
public final class BigClockTextComponentsCheck {
    private static int checks;
    private static void check(boolean result,String message){checks++;if(!result)throw new AssertionError(message);}
    private static TextView text(BigClockTextComponents row,int index){return (TextView)row.getChildAt(index);}
    private static android.text.style.ReplacementSpan icon(TextView view) {
        if(!(view.getText() instanceof android.text.Spanned))return null;
        WeatherTextIcon.Span[] spans=((android.text.Spanned)view.getText()).getSpans(0,view.getText().length(),WeatherTextIcon.Span.class);
        return spans.length==1?spans[0]:null;
    }
    private static Canvas drawText(TextView view)throws Exception {
        Canvas canvas=new Canvas();java.lang.reflect.Method draw=TextView.class.getDeclaredMethod("onDraw",Canvas.class);draw.setAccessible(true);draw.invoke(view,canvas);return canvas;
    }
    /** Borrow the backend's immutable frame by its real constructor, without
     * adding any production test hook or asking for another blur. */
    private static Object readyBackdrop(BigClockTextComponents row)throws Exception {
        FrameLayout root;
        if(row.getParent()==null){root=new FrameLayout(row.getContext());root.addView(row);}else root=(FrameLayout)row.getParent();
        View host=new View(row.getContext());root.addView(host);
        android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(512,512,android.graphics.Bitmap.Config.ARGB_8888);bitmap.eraseColor(0xff4b90d8);
        Class<?> type=Class.forName("dev.puitheme.ClockBackdropSource$Frame");
        java.lang.reflect.Constructor<?> ctor=type.getDeclaredConstructor(android.graphics.Bitmap.class,android.graphics.Matrix.class,View.class,android.graphics.Rect.class,long.class,android.graphics.Shader.TileMode.class);ctor.setAccessible(true);
        Object frame=ctor.newInstance(bitmap,new android.graphics.Matrix(),host,new android.graphics.Rect(0,0,512,512),1L,android.graphics.Shader.TileMode.MIRROR);
        java.lang.reflect.Method apply=BigClockTextComponents.class.getDeclaredMethod("backdrop",type);apply.setAccessible(true);apply.invoke(row,frame);return frame;
    }
    private static void followsBackdrop(TextView view,String message) {
        check(view.renderEffect!=null&&view.renderEffect.shader!=null&&"content".equals(view.renderEffect.inputName)
                &&view.renderEffect.shader.inputs.get("background") instanceof android.graphics.BitmapShader,message);
    }
    /** Phone reproduction: both 18dp slots are above the main time after
     * swapping, and own effects can only retain ink inside each TextView.
     * This fallback font reproduces the recorded 72px / baseline67 / height85
     * tight line, with ink extending into its complete top/bottom metrics. */
    private static final class FallbackFont extends android.text.TextPaint {
        @Override public int getFontMetricsInt(android.graphics.Paint.FontMetricsInt fm) {
            float scale=getTextSize()/72f;fm.ascent=-(int)Math.ceil(67f*scale);fm.descent=(int)Math.ceil(18f*scale);
            fm.top=-(int)Math.ceil(80f*scale);fm.bottom=(int)Math.ceil(22f*scale);fm.leading=0;return fm.descent-fm.ascent;
        }
        @Override public void getTextBounds(String value,int start,int end,android.graphics.Rect bounds) {
            super.getTextBounds(value,start,end,bounds);float scale=getTextSize()/72f;bounds.top=-(int)Math.ceil(76f*scale);bounds.bottom=(int)Math.ceil(20f*scale);
        }
    }
    private static void completeVerticalCoverage()throws Exception {
        float oldDensity=android.content.res.Resources.getSystem().getDisplayMetrics().density;
        boolean oldNativePadding=TextView.nativeFontPaddingForCheck;TextView.nativeFontPaddingForCheck=true;
        BigClockTextComponents row=new BigClockTextComponents(new Context());
        row.getResources().getDisplayMetrics().density=4f;
        try {
            Bundle values=new Bundle();values.putBoolean(BigClockWidgetsSettings.LEFT_ENABLED,true);values.putBoolean(BigClockWidgetsSettings.RIGHT_ENABLED,true);
            values.putString(BigClockWidgetsSettings.LEFT_TEXT,"天气暂无数据");values.putString(BigClockWidgetsSettings.RIGHT_TEXT,"29% 充电中");
            values.putFloat(BigClockWidgetsSettings.LEFT_SIZE,18f);values.putFloat(BigClockWidgetsSettings.RIGHT_SIZE,18f);
            row.configure(values);
            java.lang.reflect.Field paint=TextView.class.getDeclaredField("paint");paint.setAccessible(true);
            for(int i=0;i<2;i++){FallbackFont font=new FallbackFont();font.set(text(row,i).getPaint());paint.set(text(row,i),font);}
            row.refresh(0L,TimeZone.getTimeZone("UTC"));
            row.desiredHeight();
            android.graphics.Rect ink=new android.graphics.Rect();android.graphics.Paint.FontMetrics metrics=new android.graphics.Paint.FontMetrics();
            for(int i=0;i<2;i++) {
                TextView view=text(row,i);view.getPaint().getTextBounds(view.getText().toString(),0,view.getText().length(),ink);view.getPaint().getFontMetrics(metrics);
                float top=view.getBaseline()+Math.min(ink.top,metrics.top),bottom=view.getBaseline()+Math.max(ink.bottom,metrics.bottom);
                check(top>=0f&&bottom<=view.getMeasuredHeight(),"swapped component retains complete font ink inside its own material bounds, side="+i+", top="+top+", bottom="+bottom+", height="+view.getMeasuredHeight());
                check(view.getTextSize()==72f&&view.getEllipsize()==null,"complete ink coverage never shrinks or truncates the saved 18dp text");
                check(view.isFallbackLineSpacing(),"component line measurement includes fallback-script ascent and descent");
            }
            int height=(int)Math.ceil(row.desiredHeight()+row.topInset());row.onMeasure(View.MeasureSpec.makeMeasureSpec(1248,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));
            row.onLayout(true,0,0,1248,height);
            check(text(row,0).getBottom()<=height&&text(row,1).getBottom()<=height,"row reservation includes the complete protected text height");
            int rebuilds=text(row,0).layoutRebuilds+text(row,1).layoutRebuilds;
            for(int i=0;i<120;i++)row.desiredHeight();
            check(rebuilds==text(row,0).layoutRebuilds+text(row,1).layoutRebuilds,"unchanged swipe frames do not repeat ink measurement or layout");
        } finally {row.close();TextView.nativeFontPaddingForCheck=oldNativePadding;android.content.res.Resources.getSystem().getDisplayMetrics().density=oldDensity;}
    }
    private static void systemSlots()throws Exception {
        SystemWeatherText.clear();BigClockSystemText.clear();
        BigClockTextComponents row=new BigClockTextComponents(new Context());row.getResources().getDisplayMetrics().density=1f;
        try {
            com.oplus.keyguard.clock.common.entry.WeatherData sunny=new com.oplus.keyguard.clock.common.entry.WeatherData(54,"晴",30,20,com.oplus.keyguard.clock.common.R.drawable.ic_weather_sunny,123L);
            com.oplus.keyguard.clock.common.util.WeatherEffectController weather=new com.oplus.keyguard.clock.common.util.WeatherEffectController(true,sunny);WeatherNativeFixture.capture(weather);
            Bundle values=new Bundle();values.putBoolean(BigClockWidgetsSettings.LEFT_ENABLED,true);values.putBoolean(BigClockWidgetsSettings.RIGHT_ENABLED,true);
            values.putString(BigClockWidgetsSettings.LEFT_TEXT,"（天气）");values.putString(BigClockWidgetsSettings.RIGHT_TEXT,"右 （蜂窝信号） · (网络状态) · （电池）");
            row.configure(values);row.refresh(0L,TimeZone.getTimeZone("UTC"));row.desiredHeight();
            TextView left=text(row,0),right=text(row,1);CharSequence weatherText=left.getText();android.text.style.ReplacementSpan weatherIcon=icon(left);
            check(right.getText().toString().contains("蜂窝信号暂无数据")&&right.getText().toString().contains("电池暂无数据"),"new right-component tokens use explicit no-data text before native events");
            long token=BigClockSystemText.selectSubscription(7);BigClockSystemText.service(token,7,true);BigClockSystemText.signal(token,7,3);BigClockSystemText.dataNetwork(token,7,"5G");
            BigClockSystemText.network(BigClockSystemText.Transport.WIFI,true);BigClockSystemText.batteryRaw(85,100,2);
            row.refresh(0L,TimeZone.getTimeZone("UTC"));row.desiredHeight();BigClockSystemText.Snapshot snapshot=BigClockSystemText.snapshot();
            check(right.getText().toString().replace("\ufffc ","").equals("右 "+snapshot.cellularSignal+" · "+snapshot.networkStatus+" · "+snapshot.battery),"a system event updates all three right-component tokens and glyphs within the same minute");
            check(left.getText()==weatherText&&icon(left)==weatherIcon,"system events do not rebuild the unrelated weather text or span");
            int requests=row.layoutRequests,rebuilds=left.layoutRebuilds+right.layoutRebuilds;CharSequence status=right.getText();
            for(int i=0;i<100;i++){BigClockSystemText.batteryRaw(85,100,2);row.refresh(0L,TimeZone.getTimeZone("UTC"));row.desiredHeight();}
            check(requests==row.layoutRequests&&rebuilds==left.layoutRebuilds+right.layoutRebuilds&&right.getText()==status,"repeated unchanged battery events do not reformat or remeasure components");
            BigClockSystemText.batteryRaw(86,100,3);row.refresh(0L,TimeZone.getTimeZone("UTC"));
            check(right.getText()!=status&&right.getText().toString().contains("86%")&&left.getText()==weatherText&&icon(left)==weatherIcon,"changed battery text updates its dependent slot while preserving the weather span");
            status=right.getText();
            weather.setData(new com.oplus.keyguard.clock.common.entry.WeatherData(55,"晴",30,20,com.oplus.keyguard.clock.common.R.drawable.ic_weather_light_rain,123L));WeatherNativeFixture.capture(weather);
            row.refresh(0L,TimeZone.getTimeZone("UTC"));
            check(right.getText()==status&&((WeatherTextIcon.Span)icon(left)).kind==WeatherTextIcon.Kind.LIGHT_RAIN,"weather-only events do not replace the unrelated system-status text");
            values.putString(BigClockWidgetsSettings.RIGHT_TEXT,"(电池)");row.configure(values);row.refresh(0L,TimeZone.getTimeZone("UTC"));row.desiredHeight();
            status=right.getText();weatherText=left.getText();requests=row.layoutRequests;
            BigClockSystemText.network(BigClockSystemText.Transport.NONE,null);BigClockSystemText.signal(token,7,1);row.refresh(0L,TimeZone.getTimeZone("UTC"));
            check(right.getText()==status&&left.getText()==weatherText&&requests==row.layoutRequests,"a battery-only slot ignores independent network and signal revisions");
            row.refresh(60000L,TimeZone.getTimeZone("UTC"));
            check(right.getText()==status&&left.getText()==weatherText&&requests==row.layoutRequests,"a new minute does not reformat slots without time placeholders");
            values.putString(BigClockWidgetsSettings.RIGHT_TEXT,"（天气） (电池)");row.configure(values);row.refresh(60000L,TimeZone.getTimeZone("UTC"));row.desiredHeight();
            android.text.style.ReplacementSpan combinedIcon=icon(right);status=right.getText();
            BigClockSystemText.batteryRaw(87,100,2);row.refresh(60000L,TimeZone.getTimeZone("UTC"));
            check(right.getText()!=status&&right.getText().toString().contains("87%")&&icon(right)==combinedIcon,"battery changes in a combined weather/system slot reuse its existing weather span");
            weather.setData(new com.oplus.keyguard.clock.common.entry.WeatherData(54,"晴",30,20,com.oplus.keyguard.clock.common.R.drawable.ic_weather_clear_night,123L));WeatherNativeFixture.capture(weather);row.refresh(60000L,TimeZone.getTimeZone("UTC"));
            check(icon(right)!=combinedIcon&&((WeatherTextIcon.Span)icon(right)).kind==WeatherTextIcon.Kind.CLEAR_NIGHT,"a real weather icon change replaces the retained combined-slot span");
            row.appearance(0x806699cc,false,true,0xffffffff,.5f);readyBackdrop(row);row.desiredHeight();Canvas canvas=drawText(right);
            check(right.getCurrentTextColor()==0xffffffff&&canvas.pictureDraws>0&&canvas.textAlpha==255,
                    "combined system text and weather icon provide matching opaque glyph coverage");
            followsBackdrop(left,"weather slot borrows one ready background material");followsBackdrop(right,"combined slot borrows one ready background material");
            check(row.renderEffect==null&&left.renderEffect!=right.renderEffect&&canvas.layers==0&&canvas.nodeDraws==0,"each following slot owns one effect without adding a row effect or offscreen draw node");
        } finally {row.close();SystemWeatherText.clear();BigClockSystemText.clear();}
    }
    private static final class FontContext extends Context {
        final java.io.File font;
        final android.content.ContentResolver resolver=new android.content.ContentResolver(){
            @Override public android.os.ParcelFileDescriptor openFileDescriptor(android.net.Uri uri,String mode)throws java.io.FileNotFoundException {
                return android.os.ParcelFileDescriptor.open(font,android.os.ParcelFileDescriptor.MODE_READ_ONLY);
            }
        };
        FontContext(java.io.File font){this.font=font;}
        @Override public android.content.ContentResolver getContentResolver(){return resolver;}
    }
    private static void independentWeights()throws Exception {
        java.nio.file.Path font=java.nio.file.Files.createTempFile("c17-component-font-",".fixture");java.nio.file.Files.write(font,new byte[]{1});
        Context context=new FontContext(font.toFile());FontRepository.releaseRuntime();FontRepository.configure(context,"system","");
        BigClockTextComponents row=new BigClockTextComponents(context);row.getResources().getDisplayMetrics().density=1f;
        try {
            Bundle values=new Bundle();values.putBoolean(BigClockWidgetsSettings.LEFT_ENABLED,true);values.putBoolean(BigClockWidgetsSettings.RIGHT_ENABLED,true);
            values.putString(BigClockWidgetsSettings.LEFT_TEXT,"左 文本");values.putString(BigClockWidgetsSettings.RIGHT_TEXT,"右 文本");
            values.putFloat("notification_big_clock_widget_left_weight",900f);values.putFloat("notification_big_clock_widget_right_weight",200f);
            row.configure(values);row.refresh(0L,TimeZone.getTimeZone("UTC"));row.desiredHeight();TextView left=text(row,0),right=text(row,1);
            check(left.getPaint().effectiveWeight()==900f&&right.getPaint().effectiveWeight()==200f,"left and right apply their independent physical font weights");
            CharSequence previous=left.getText();int rightWrites=right.typefaceWrites,requests=row.layoutRequests;
            for(int i=0;i<120;i++){row.configure(values);row.refresh(0L,TimeZone.getTimeZone("UTC"));row.desiredHeight();}
            check(rightWrites==right.typefaceWrites&&requests==row.layoutRequests&&left.getText()==previous,"unchanged weights reuse faces, text and measurement during swipes");
            values.putFloat("notification_big_clock_widget_left_weight",1000f);row.configure(values);row.desiredHeight();
            check(left.getPaint().effectiveWeight()==1000f&&right.getPaint().effectiveWeight()==200f&&right.typefaceWrites==rightWrites,"one-side weight change preserves the opposite side");
            check(left.getIncludeFontPadding()&&left.isFallbackLineSpacing()&&left.getTextSize()==18f,"font weight preserves complete font coverage and the configured font size");
            row.appearance(0x806699cc,false,true,0xffeeeeee,.2f);readyBackdrop(row);android.graphics.RenderEffect effect=left.renderEffect;
            FontRepository.configure(context,"custom","component-font");row.refresh(0L,TimeZone.getTimeZone("UTC"));row.desiredHeight();
            check(!"custom".equals(left.getTypeface().sourceForCheck),"a saved custom family stays inactive while the global font switch is off");
            values.putBoolean("font_enabled",true);row.configure(values);row.refresh(0L,TimeZone.getTimeZone("UTC"));row.desiredHeight();
            check("custom".equals(left.getTypeface().sourceForCheck)&&left.getPaint().effectiveWeight()==1000f,"global font source changes within the same minute while preserving the selected component weight");
            check(left.renderEffect==effect&&left.getCurrentTextColor()==0xffffffff,"font changes keep the existing component material and opaque coverage");
            FontRepository.configure(context,"system","same-minute-native");row.refresh(0L,TimeZone.getTimeZone("UTC"));row.desiredHeight();
            check(!"custom".equals(left.getTypeface().sourceForCheck)&&left.getPaint().effectiveWeight()==1000f,"a global source revision refreshes an already enabled component within the same minute");
            FontRepository.configure(context,"custom","component-font-2");row.refresh(0L,TimeZone.getTimeZone("UTC"));row.desiredHeight();
            check("custom".equals(left.getTypeface().sourceForCheck)&&left.renderEffect==effect,"a later font revision restores the chosen family without replacing its material");
            values.putBoolean("font_enabled",false);row.configure(values);row.desiredHeight();
            check(!"custom".equals(left.getTypeface().sourceForCheck)&&left.getPaint().effectiveWeight()==1000f,"turning global font off restores the native family without erasing component weight");
        } finally {row.close();FontRepository.releaseRuntime();java.nio.file.Files.deleteIfExists(font);}
    }
    public static void main(String[] args)throws Exception {
        boolean oldPadding=View.keepPaddingForCheck,oldMeasure=TextView.measureTextWidth;
        View.keepPaddingForCheck=TextView.measureTextWidth=true;
        SystemWeatherText.clear();
        try {
            completeVerticalCoverage();
            independentWeights();
            Context context=new Context();BigClockTextComponents row=new BigClockTextComponents(context);row.getResources().getDisplayMetrics().density=1f;
            row.configure(new Bundle());row.refresh(0L,TimeZone.getTimeZone("UTC"));
            check(row.getChildCount()==0&&row.desiredHeight()==0f,"default-disabled text components allocate no child or reserved height");
            Bundle values=new Bundle();values.putBoolean(BigClockWidgetsSettings.LEFT_ENABLED,true);values.putBoolean(BigClockWidgetsSettings.RIGHT_ENABLED,true);
            values.putString(BigClockWidgetsSettings.LEFT_TEXT,"(时间)");values.putString(BigClockWidgetsSettings.RIGHT_TEXT,"右");
            values.putFloat(BigClockWidgetsSettings.RIGHT_SIZE,40f);values.putFloat(BigClockWidgetsSettings.LEFT_OFFSET_X,250f);
            values.putFloat(BigClockWidgetsSettings.RIGHT_OFFSET_X,-240f);values.putFloat(BigClockWidgetsSettings.LEFT_OFFSET_Y,-10f);
            values.putFloat(BigClockWidgetsSettings.RIGHT_OFFSET_Y,35f);
            row.configure(values);row.refresh(0L,TimeZone.getTimeZone("UTC"));
            check(row.getChildCount()==2,"each enabled side has exactly one real TextView");
            TextView left=text(row,0),right=text(row,1);
            check("00:00".contentEquals(left.getText())&&"右".contentEquals(right.getText()),"only explicit placeholders resolve; ordinary text stays literal");
            check(left.getTextSize()==18f&&right.getTextSize()==40f,"left and right retain independent sizes");
            check(row.desiredHeight()==75f,"positive Y includes the actual text bottom in notification reservation");
            row.onMeasure(View.MeasureSpec.makeMeasureSpec(400,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(80+row.topInset(),View.MeasureSpec.EXACTLY));
            row.onLayout(true,0,0,400,80+row.topInset());
            check(left.getLeft()==250&&right.getLeft()<200,"both text components may cross their original half-row");
            check(left.getTop()-row.topInset()==-10&&right.getTop()-row.topInset()==35,"negative and positive visual positions are retained after expanding the real parent touch geometry");
            int requests=row.layoutRequests,rebuilds=left.layoutRebuilds+right.layoutRebuilds;
            for(int i=0;i<100;i++){row.configure(values);row.refresh(1000L+i,TimeZone.getTimeZone("UTC"));row.desiredHeight();}
            check(requests==row.layoutRequests&&rebuilds==left.layoutRebuilds+right.layoutRebuilds,"unchanged event input does not reformat, request layout or remeasure text");
            row.refresh(60000L,TimeZone.getTimeZone("UTC"));check("00:01".contentEquals(left.getText()),"minute events refresh the time component");
            row.refresh(60000L,TimeZone.getTimeZone("GMT+08:00"));check("08:01".contentEquals(left.getText()),"time-zone changes refresh within the same minute");
            values.putString(BigClockWidgetsSettings.LEFT_TEXT,"(天气) / （温度）");row.configure(values);row.refresh(60000L,TimeZone.getTimeZone("UTC"));
            check(left.getText().toString().contains("天气暂无数据"),"unavailable native weather has an explicit text fallback");
            com.oplus.keyguard.clock.common.entry.WeatherData weather=new com.oplus.keyguard.clock.common.entry.WeatherData(54,"晴",30,20,com.oplus.keyguard.clock.common.R.drawable.ic_weather_sunny,123L);
            com.oplus.keyguard.clock.common.util.WeatherEffectController controller=new com.oplus.keyguard.clock.common.util.WeatherEffectController(true,weather);
            check(WeatherNativeFixture.capture(controller),"a genuine located native-weather callback changes the snapshot");
            row.refresh(60000L,TimeZone.getTimeZone("UTC"));
            check(left.getText().toString().contains("晴 ·30°")&&left.getText().toString().endsWith(" / 30°"),"weather revision refreshes both parenthesis token types within the same minute");
            check(icon(left)!=null,
                    "weather tokens include one single-colour inline icon instead of an emoji");
            check(((WeatherTextIcon.Span)icon(left)).kind==WeatherTextIcon.Kind.SUNNY,"the native widget supplies the actual sunny vector");
            row.desiredHeight();int weatherRequests=row.layoutRequests;
            check(!WeatherNativeFixture.capture(controller),"unchanged native-weather output retains its revision");
            row.refresh(60000L,TimeZone.getTimeZone("UTC"));check(weatherRequests==row.layoutRequests,"an unchanged weather event does not relayout the row");
            CharSequence sunnyText=left.getText();android.text.style.ReplacementSpan sunnyIcon=icon(left);
            row.refresh(120000L,TimeZone.getTimeZone("UTC"));
            check(left.getText()==sunnyText&&icon(left)==sunnyIcon,"a new minute with the same weather reuses the styled text and span");
            controller.setData(new com.oplus.keyguard.clock.common.entry.WeatherData(55,"晴",30,20,com.oplus.keyguard.clock.common.R.drawable.ic_weather_light_rain,123L));
            check(WeatherNativeFixture.capture(controller),"a changed native weather type advances revision despite identical description and temperature");
            row.refresh(120000L,TimeZone.getTimeZone("UTC"));
            check(left.getText().toString().equals(sunnyText.toString())&&icon(left)!=sunnyIcon&&((WeatherTextIcon.Span)icon(left)).kind==WeatherTextIcon.Kind.LIGHT_RAIN,
                    "identical visible weather text still replaces the inline icon for a new type");
            controller.setData(new com.oplus.keyguard.clock.common.entry.WeatherData(54,"晴",30,20,com.oplus.keyguard.clock.common.R.drawable.ic_weather_clear_night,123L));
            check(WeatherNativeFixture.capture(controller),"native day-to-night icon selection advances revision");
            row.refresh(120000L,TimeZone.getTimeZone("UTC"));
            check(((WeatherTextIcon.Span)icon(left)).kind==WeatherTextIcon.Kind.CLEAR_NIGHT,"the inline moon follows the native night resource without a local day/night timer");
            controller.setData(new com.oplus.keyguard.clock.common.entry.WeatherData(999,"晴",30,20,999999,123L));WeatherNativeFixture.capture(controller);
            row.refresh(120000L,TimeZone.getTimeZone("UTC"));
            check(icon(left)==null&&left.getText().toString().contains("晴 ·30°"),"an unknown native image retains readable text without a fabricated replacement icon");
            controller.setData(weather);WeatherNativeFixture.capture(controller);row.refresh(120000L,TimeZone.getTimeZone("UTC"));
            row.appearance(0x806699cc,false,true,0xffffffff,.5f);
            check(left.getCurrentTextColor()==0xffffffff&&right.getCurrentTextColor()==left.getCurrentTextColor(),"follow-time text provides opaque glyph coverage instead of colour interpolation");
            Object observedBackdrop=readyBackdrop(row);followsBackdrop(left,"following text receives the existing clean background input");followsBackdrop(right,"the other following slot gets its independent material");
            check((left.getCurrentTextColor()>>>24)==255,"backdrop glyph coverage ignores the old selected text alpha");
            row.desiredHeight();Canvas maskedIcon=drawText(left);
            check(maskedIcon.pictureDraws>0&&maskedIcon.layers==0&&maskedIcon.textColor==left.getCurrentTextColor()&&maskedIcon.textAlpha==255,
                    "inline icon and text share opaque coverage before their single parent material during actual Layout drawing");
            android.text.style.ReplacementSpan cachedIcon=icon(left);CharSequence cachedText=left.getText();
            row.appearance(0x806699cc,false,true,0xff0000ff,1f);row.desiredHeight();drawText(left);
            check(icon(left)==cachedIcon&&left.getText()==cachedText,"colour changes reuse the same cached inline span and styled text");
            values.putBoolean(BigClockWidgetsSettings.RIGHT_FOLLOW_TIME_COLOR,false);values.putInt(BigClockWidgetsSettings.RIGHT_COLOR_LIGHT,0xff00ff00);values.putInt(BigClockWidgetsSettings.RIGHT_COLOR_DARK,0xffee0000);
            row.configure(values);row.appearance(0x806699cc,false,true,0xff0000ff,1f);
            check(right.getCurrentTextColor()==0xff00ff00&&left.getCurrentTextColor()==0xffffffff&&right.renderEffect==null,"independent day colour does not inherit the time mask or its material");
            values.putString(BigClockWidgetsSettings.RIGHT_TEXT,"（天气）");row.configure(values);row.refresh(120000L,TimeZone.getTimeZone("UTC"));row.desiredHeight();
            Canvas independentIcon=drawText(right);
            check(independentIcon.pictureDraws>0&&independentIcon.lastRectColor==0xff00ff00&&independentIcon.textAlpha==255,
                    "independent weather icon follows its own text colour instead of the time mask");
            row.appearance(0x806699cc,true,true,0xff0000ff,1f);check(right.getCurrentTextColor()==0xffee0000,"independent night colour follows the background mode");
            ClockBackdropMaterial.Appearance look=new ClockBackdropMaterial.Appearance(true,.75f,0x80ff4000,.4f,90f,.3f,.1f,1.5f);
            java.lang.reflect.Method bodyLook=BigClockTextComponents.class.getDeclaredMethod("backdrop",observedBackdrop.getClass(),ClockBackdropMaterial.Appearance.class);bodyLook.setAccessible(true);bodyLook.invoke(row,observedBackdrop,look);
            check(left.renderEffect.shader.floats.get("edgeWidths")[0]==.75f&&left.renderEffect.shader.floats.get("edgeWidths")[1]==1.5f,"following text receives the main material's already converted pixel widths once");
            check(Math.abs(left.renderEffect.shader.floats.get("edgeColor")[3]-128f/255f*.4f)<.00001f&&left.renderEffect.shader.floats.get("relief")[2]==.3f,"following weather glyphs share the main material border and relief parameters");
            check(right.renderEffect==null&&right.getCurrentTextColor()==0xffee0000,"independent-colour text stays outside the main material appearance");
            int appearanceWrites=left.renderEffectWrites;
            for(int i=0;i<100;i++)bodyLook.invoke(row,observedBackdrop,new ClockBackdropMaterial.Appearance(true,.75f,0x80ff4000,.4f,90f,.3f,.1f,1.5f));
            check(left.renderEffectWrites==appearanceWrites,"equal appearance values reuse the following slot's shader effect without rebinding");
            row.appearance(0x806699cc,true,true,0xff0000ff,0f);
            check(left.getCurrentTextColor()==0xffffffff&&left.renderEffect.shader.floats.get("tint")[3]==0f&&left.renderEffect.shader.inputs.containsKey("background"),"zero tint retains opaque glyph coverage and its background input");
            android.graphics.RenderEffect stable=left.renderEffect;int writes=left.renderEffectWrites,rebuildsBefore=left.layoutRebuilds;
            int blurs=android.graphics.RenderEffect.blurCreations,reads=android.graphics.Bitmap.reads,copies=android.graphics.Bitmap.copies,scales=android.graphics.Bitmap.scales;
            for(int i=0;i<100;i++){row.appearance(0x806699cc,true,true,0xff0000ff,0f);row.desiredHeight();}
            check(left.renderEffect==stable&&left.renderEffectWrites==writes&&left.layoutRebuilds==rebuildsBefore,"stable appearance reuses the effect, weather span and text layout");
            check(android.graphics.RenderEffect.blurCreations==blurs&&android.graphics.Bitmap.reads==reads&&android.graphics.Bitmap.copies==copies&&android.graphics.Bitmap.scales==scales,"following text adds no blur, pixel read, bitmap copy or scale");
            row.appearance(0x806699cc,true,false,0xff0000ff,1f);check(left.getCurrentTextColor()==0x806699cc,"mask-off restores the follow-time base colour");
            check(row.renderEffect==null&&left.renderEffect==null&&right.renderEffect==null&&row.layerTypeWrites==0,"turning mode off clears both child materials without adding a row effect or special layer");
            row.setAlpha(.4f);check(left.getAlpha()==1f&&right.getAlpha()==1f,"component children inherit parent opacity without multiplying it again");
            FrameLayout root=(FrameLayout)row.getParent();row.layout(0,100-row.topInset(),400,180);Canvas canvas=new Canvas();row.dispatchDraw(canvas);
            check(canvas.clipTop==-100f+row.topInset()&&canvas.clipBottom==row.getHeight()&&canvas.clipLeft==0f&&canvas.clipRight==row.getWidth(),"compensated negative Y is clipped to the actual window top and positive Y to the reserved bottom");
            check(canvas.layers==0&&canvas.nodeDraws==0,"drawing adds no offscreen layer or material node");
            values.putFloat(BigClockWidgetsSettings.RIGHT_OFFSET_Y,-100f);row.configure(values);check(row.desiredHeight()==32f,"negative-only components keep the minimal row reservation");
            values.putBoolean(BigClockWidgetsSettings.LEFT_ENABLED,false);row.configure(values);check(row.getChildCount()==1&&row.getChildAt(0)==right,"disabling one side releases only that side");
            row.close();check(row.getChildCount()==0&&row.desiredHeight()==0f&&left.renderEffect==null&&right.renderEffect==null,"close releases all component views, owned materials and reservation");
            row.configure(values);row.refresh(120000L,TimeZone.getTimeZone("UTC"));check(row.getChildCount()==0,"a closed row cannot recreate detached children");
            systemSlots();
        }finally{SystemWeatherText.clear();View.keepPaddingForCheck=oldPadding;TextView.measureTextWidth=oldMeasure;}
        System.out.println("BigClockTextComponentsCheck: "+checks+" checks passed");
    }
}
