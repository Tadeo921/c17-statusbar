package dev.puitheme;

import com.oplus.keyguard.clock.common.entry.WeatherData;
import com.oplus.keyguard.clock.common.util.WeatherEffectController;
import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Uses native model/controller shapes captured before this request. These
 * tests execute the real resolver/cache, not the OEM service or phone UI. */
public final class BigClockTextContentCheck {
    private static int checks;
    private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    private static String withoutIcons(CharSequence text){return text.toString().replace("\ufffc ","");}
    private static WeatherData weather(String description,int temperature){return new WeatherData(3,description,temperature,temperature,1,101010100L);}
    private static WeatherEffectController located(String description,int temperature){return new WeatherEffectController(true,weather(description,temperature));}
    private static long instant() {
        Calendar time=new GregorianCalendar(TimeZone.getTimeZone("UTC"),Locale.SIMPLIFIED_CHINESE);
        time.clear();time.set(2026,Calendar.OCTOBER,16,15,5,0);return time.getTimeInMillis();
    }

    private static void literalAndTime() {
        SystemWeatherText.clear();long now=instant();TimeZone utc=TimeZone.getTimeZone("UTC");
        check(BigClockTextContent.resolve(null,now,utc).isEmpty(),"null template is empty");
        check(BigClockTextContent.resolve("",now,utc).isEmpty(),"empty template has no placeholder fallback");
        String literal="Today HH:mm 'hello' {text} 😊 /（备注）/(todo)";
        check(BigClockTextContent.resolve(literal,now,utc).equals(literal),"English, date letters, quotes, braces, emoji and unknown parentheses stay literal");
        check(BigClockTextContent.resolve("（时间） (时间)",now,utc).equals("15:05 15:05"),"both parenthesis forms expand the same current time");
        check(BigClockTextContent.resolve("（日期）/(星期)",now,utc).equals("10月16日/星期五"),"date and weekday use the current event timestamp");
        TimeZone shanghai=TimeZone.getTimeZone("Asia/Shanghai");
        check(BigClockTextContent.resolve("(时间) (日期) （星期）",now,shanghai).equals("23:05 10月16日 星期五"),"all placeholders share the supplied timezone");
        check(BigClockTextContent.resolve("（日期） (星期)",now+12L*60L*60L*1000L,shanghai).equals("10月17日 星期六"),"date and weekday update at the same midnight boundary");
        check(BigClockTextContent.resolve("(天气） （天气) (unknown",now,utc).equals("(天气） （天气) (unknown"),"mismatched or unfinished parentheses remain ordinary text");
        check(BigClockTextContent.resolve("A（天气）B(温度)C",now,utc).equals("A天气暂无数据B温度暂无数据C"),"missing native weather is explicit and never a fabricated example");
        check(!BigClockTextContent.usesWeather(null)&&!BigClockTextContent.usesWeather("晴天 HH:mm"),"literal weather words do not enable weather handling");
        for(String token:new String[]{"（天气）","(天气)","（温度）","(温度)"})
            check(BigClockTextContent.usesWeather("prefix "+token+" suffix"),"only supported weather placeholders opt into native snapshots");
    }

    private static void nativeLocatedSnapshot() {
        SystemWeatherText.clear();long revision=SystemWeatherText.revision();
        WeatherEffectController source=located("小雨",18);
        check(SystemWeatherText.captureController(source),"the located native controller supplies its already resolved weather");
        SystemWeatherText.Snapshot first=SystemWeatherText.snapshot();
        check(first.description.equals("小雨")&&first.temperature.equals("18°")&&first.text.equals("小雨 18°"),"native degree display is preserved without inventing Celsius/Fahrenheit conversion");
        check(first.revision==revision+1&&SystemWeatherText.revision()==first.revision,"one actual displayed change advances the snapshot revision once");
        check(BigClockTextContent.resolve("出门：（天气） / (温度)",instant(),TimeZone.getTimeZone("UTC")).equals("出门：小雨 18° / 18°"),"weather and temperature templates use one immutable latest snapshot");
        for(int event=0;event<1000;event++)SystemWeatherText.captureController(source);
        check(SystemWeatherText.snapshot()==first&&SystemWeatherText.revision()==first.revision,"unchanged native events retain the same snapshot and revision");
        check(!SystemWeatherText.captureController(new WeatherEffectController(false,weather("常驻城市雪",-8))),"resident city weather cannot replace located weather");
        check(SystemWeatherText.snapshot()==first,"a dual-clock resident callback leaves the selected native weather intact");
        source.setDestroyed(true);source.setData(weather("旧控制器",99));
        check(!SystemWeatherText.captureController(source)&&SystemWeatherText.snapshot()==first,"a destroyed controller cannot revive an old weather snapshot");
        source.setDestroyed(false);source.setData(weather("多云",22));
        check(SystemWeatherText.captureController(source)&&SystemWeatherText.snapshot().text.equals("多云 22°"),"a later native event atomically replaces description and temperature");
        check(first.text.equals("小雨 18°"),"previous snapshots remain immutable after replacement");
        source.setData(null);
        check(SystemWeatherText.captureController(source)&&SystemWeatherText.snapshot().text.equals(SystemWeatherText.UNAVAILABLE),"native null data clears the old weather instead of persisting stale weather");
        source.setData(weather("",0));
        check(!SystemWeatherText.captureController(source)&&SystemWeatherText.snapshot().temperature.isEmpty(),"missing description plus numeric fallback never becomes invented zero degree weather");
        source.setData(weather("--",0));
        check(!SystemWeatherText.captureController(source)&&SystemWeatherText.snapshot().temperatureText().equals("温度暂无数据"),"native placeholder remains an explicit unavailable value");
        check(!SystemWeatherText.captureController(null)&&!SystemWeatherText.captureController(new Object()),"unknown objects are never treated as native weather controllers");
        source.setData(weather("晴\n天\u0000",Integer.MIN_VALUE));SystemWeatherText.captureController(source);
        check(SystemWeatherText.snapshot().text.equals("晴天")&&SystemWeatherText.snapshot().temperature.isEmpty(),"control characters and invalid temperature sentinels do not enter text");
        SystemWeatherText.clear();WeatherData cached=weather("原生缓存",19);
        source=new WeatherEffectController(true,cached);
        check(SystemWeatherText.captureRead(source,cached),"the first real native cache read can supply existing weather without another query");
        int reads=WeatherData.descriptionReads;SystemWeatherText.Snapshot retained=SystemWeatherText.snapshot();
        for(int frame=0;frame<1000;frame++)check(!SystemWeatherText.captureRead(source,cached),"unchanged immutable native cache reads are inactive");
        check(WeatherData.descriptionReads==reads&&SystemWeatherText.snapshot()==retained,"same-model reads do no getter/reflection-format path or snapshot replacement");
        cached=weather("新原生缓存",20);source.setData(cached);
        check(SystemWeatherText.captureRead(source,cached)&&WeatherData.descriptionReads==reads+1,"a new immutable native cache model is captured once");
        SystemWeatherText.clear();
        check(SystemWeatherText.captureRead(source,cached),"reset releases weak read identities so the new lifecycle can seed its native cache");
    }

    private static void systemTemplates() {
        BigClockSystemText.clear();
        long now=instant();TimeZone utc=TimeZone.getTimeZone("UTC");
        BigClockSystemText.Snapshot snapshot=BigClockSystemText.snapshot();
        String expected=snapshot.cellularSignal+" / "+snapshot.networkStatus+" / "+snapshot.battery;
        check(BigClockTextContent.resolve("（蜂窝信号） / (网络状态) / （电池）",now,utc).equals(expected),
                "full-width and ASCII system placeholders expand from one immutable snapshot");
        check(BigClockTextContent.resolve("(蜂窝信号) / （网络状态） / (电池)",now,utc).equals(expected),
                "both parenthesis forms have identical cellular/network/battery output");
        check(snapshot.cellularSignal.equals("蜂窝信号暂无数据")&&snapshot.networkStatus.equals("网络暂无数据")&&snapshot.battery.equals("电池暂无数据"),
                "a process without native events shows explicit system-data fallbacks");
        String literal="Today HH:mm 😊，电池100%，cellular [网络状态] /（蜂窝信号) /(电池）";
        check(BigClockTextContent.resolve(literal,now,utc).equals(literal),"natural words, emoji, preset labels and mismatched parentheses remain literal");
        check(BigClockTextContent.dependencies("（蜂窝信号）")==BigClockTextContent.CELLULAR,"cellular text declares only the cellular revision dependency");
        check(BigClockTextContent.dependencies("(网络状态)")==BigClockTextContent.NETWORK,"network text declares only the network revision dependency");
        check(BigClockTextContent.dependencies("（电池）")==BigClockTextContent.BATTERY,"battery text declares only the battery revision dependency");
        check(BigClockTextContent.dependencies("（天气） (电池) (时间)")==(BigClockTextContent.WEATHER|BigClockTextContent.BATTERY|BigClockTextContent.TIME),
                "mixed templates retain weather, system and time dependencies independently");
        check(BigClockTextContent.dependencies(literal)==0,"literal system words do not subscribe a slot to system revisions");
        long token=BigClockSystemText.selectSubscription(7);
        BigClockSystemText.service(token,7,true);BigClockSystemText.signal(token,7,3);BigClockSystemText.dataNetwork(token,7,"5G");
        BigClockSystemText.network(BigClockSystemText.Transport.WIFI,true);BigClockSystemText.batteryRaw(85,100,2);
        BigClockSystemText.Snapshot captured=BigClockSystemText.snapshot();
        String template="右侧😊 （蜂窝信号） | (网络状态) | （电池）";
        String formatted="右侧😊 "+captured.cellularSignal+" | "+captured.networkStatus+" | "+captured.battery;
        check(BigClockTextContent.resolve(template,now,utc).equals(formatted),"native-fed cellular, default route and battery text replace right-component placeholders");
        check(withoutIcons(BigClockTextContent.resolveStyled(template,now,utc)).equals(formatted),"styled rendering adds glyphs while retaining system text and natural emoji literally");
        BigClockSystemText.batteryRaw(86,100,3);BigClockSystemText.network(BigClockSystemText.Transport.NONE,null);BigClockSystemText.signal(token,7,1);
        check(withoutIcons(BigClockTextContent.resolveStyled(template,now,utc,SystemWeatherText.snapshot(),captured,null)).equals(formatted),
                "all tokens in a resolve use the one captured immutable snapshot even after a later event");
        BigClockSystemText.Snapshot latest=BigClockSystemText.snapshot();
        check(BigClockTextContent.resolve(template,now,utc).equals("右侧😊 "+latest.cellularSignal+" | "+latest.networkStatus+" | "+latest.battery),
                "the next resolve observes the latest atomic system snapshot");
        BigClockSystemText.clear();
    }

    private static void actualAospCallbackFallback() {
        SystemWeatherText.clear();
        com.android.systemui.plugins.keyguard.data.model.WeatherData aosp=
                new com.android.systemui.plugins.keyguard.data.model.WeatherData("Rain",54,false);
        check(SystemWeatherText.captureAosp(aosp)&&SystemWeatherText.snapshot().text.equals("Rain 54°F"),"a real AOSP weather callback can supply its explicitly declared Fahrenheit unit");
        check(SystemWeatherText.captureAosp(new com.android.systemui.plugins.keyguard.data.model.WeatherData("Sunny",20,true))
                &&SystemWeatherText.snapshot().text.equals("Sunny 20°C"),"AOSP Celsius data keeps its declared unit");
        check(!SystemWeatherText.captureAosp(new Object()),"lookalike/unknown weather data is ignored");
        WeatherEffectController source=located("系统定位天气",24);SystemWeatherText.captureController(source);
        check(!SystemWeatherText.captureAosp(aosp)&&SystemWeatherText.snapshot().text.equals("系统定位天气 24°"),"the located native pipeline has priority over another clock's fallback callback");
        check(SystemWeatherText.clear()&&SystemWeatherText.snapshot().text.equals(SystemWeatherText.UNAVAILABLE),"user switch or module teardown drops the old data and priority");
        check(SystemWeatherText.captureAosp(aosp),"a cleared process can accept its current real native fallback again");
    }

    private static void releaseGeneration() throws Exception {
        SystemWeatherText.clear();SystemWeatherText.captureController(located("旧用户天气",21));
        WeatherEffectController source=located("迟到的旧城市",32);
        CountDownLatch entered=new CountDownLatch(1),resume=new CountDownLatch(1);
        WeatherData.entered=entered;WeatherData.resume=resume;
        final boolean[] changed={true};
        Thread delivery=new Thread(()->changed[0]=SystemWeatherText.captureController(source),"weather-generation-fixture");
        try {
            delivery.start();check(entered.await(5,TimeUnit.SECONDS),"weather delivery reaches the controlled old generation");
            SystemWeatherText.clear();long cleared=SystemWeatherText.revision();resume.countDown();delivery.join(5000);
            check(!delivery.isAlive()&&!changed[0],"a delayed delivery is rejected after teardown/user switch");
            check(SystemWeatherText.revision()==cleared&&SystemWeatherText.snapshot().text.equals(SystemWeatherText.UNAVAILABLE),"old city data cannot repopulate the cleared snapshot");
        } finally {resume.countDown();delivery.join(5000);WeatherData.entered=WeatherData.resume=null;}
        check(SystemWeatherText.captureController(located("新用户天气",17)),"a fresh native event after reset can populate weather");
        for(Field field:SystemWeatherText.Snapshot.class.getDeclaredFields())
            check(field.getType()==String.class||field.getType().isPrimitive()||field.getType()==WeatherTextIcon.Kind.class
                    ||field.getType()==WeatherTextIcon.NativeImage.class,"the retained snapshot has no controller, View, Context, native data or callback");
        for(Field field:WeatherTextIcon.NativeImage.class.getDeclaredFields())
            check(field.getType().isPrimitive()||field.getType()==java.lang.ref.WeakReference.class
                    ||field.getType()==android.graphics.Picture.class||field.getType()==android.graphics.drawable.Drawable.ConstantState.class,
                    "native image owns a detached recording/factory and only weak source identities");
        SystemWeatherText.clear();
    }

    public static void main(String[] args) throws Exception {
        Locale original=Locale.getDefault();
        try {Locale.setDefault(Locale.SIMPLIFIED_CHINESE);literalAndTime();systemTemplates();nativeLocatedSnapshot();actualAospCallbackFallback();releaseGeneration();}
        finally {Locale.setDefault(original);SystemWeatherText.clear();BigClockSystemText.clear();}
        System.out.println("BigClockTextContentCheck: "+checks+" checks passed (captured native shapes; phone delivery not executed)");
    }
}
