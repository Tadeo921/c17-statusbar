package dev.puitheme;

import com.oplus.keyguard.clock.common.entry.WeatherData;
import com.oplus.keyguard.clock.common.util.WeatherEffectController;
import java.lang.reflect.Method;
import java.util.TimeZone;

/** Real native cache shapes, including already initialized plugin lazy values.
 * The desktop fixture provides no provider, Context or weather query API. */
public final class SystemWeatherDeliveryCheck {
    private static int checks;
    private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    private static WeatherData data(String text,int value){return new WeatherData(54,text,value,value,com.oplus.keyguard.clock.common.R.drawable.ic_weather_sunny,123L);}
    /** Missing new seam on delivered code169 is a semantic no-update baseline. */
    private static boolean invoke(String name,Object value)throws Exception {
        try {Method method=SystemWeatherText.class.getDeclaredMethod(name,Object.class);method.setAccessible(true);return Boolean.TRUE.equals(method.invoke(null,value));}
        catch(NoSuchMethodException old169){return false;}
    }
    public static void main(String[] args)throws Exception {
        SystemWeatherText.clear();
        try {
            WeatherEffectController located=new WeatherEffectController(true,data("原生已缓存",26));
            WeatherEffectController resident=new WeatherEffectController(false,data("另一个城市",-5));
            com.oplus.keyguard.clock.common.util.ExtraInfoManager common=new com.oplus.keyguard.clock.common.util.ExtraInfoManager(located,resident);
            check(invoke("captureManager",common)&&SystemWeatherText.snapshot().text.equals("原生已缓存 26°"),"a pre-existing common native manager seeds its located cache immediately, without waiting for another getter or weather delivery");
            SystemWeatherText.Snapshot retained=SystemWeatherText.snapshot();int reads=WeatherData.descriptionReads;
            for(int i=0;i<1000;i++)check(!invoke("captureManager",common),"unchanged native manager events retain the existing snapshot");
            check(SystemWeatherText.snapshot()==retained&&WeatherData.descriptionReads==reads,"repeat seeding skips immutable weather getters and formatting");
            common.setLocatedForCheck(null);check(!invoke("captureManager",common)&&SystemWeatherText.snapshot()==retained,"an absent located controller never borrows the resident city's data");
            common.setLocatedForCheck(resident);check(!invoke("captureManager",common)&&SystemWeatherText.snapshot()==retained,"even a misbound resident controller is rejected by its native located flag");
            WeatherEffectController current=new WeatherEffectController(true,data("系统锁屏当前天气",19));
            com.oplus.keyguard.clock.base.util.ExtraInfoManager base=new com.oplus.keyguard.clock.base.util.ExtraInfoManager(current);
            check(invoke("captureManager",base)&&SystemWeatherText.snapshot().text.equals("系统锁屏当前天气 19°"),"base manager uses its genuine common located controller cache");
            retained=SystemWeatherText.snapshot();
            WeatherEffectController warming=new WeatherEffectController(true,null);
            check(!invoke("captureManager",new com.oplus.keyguard.clock.common.util.ExtraInfoManager(warming,null))&&SystemWeatherText.snapshot()==retained,"a second located model with an unready null cache cannot erase already delivered weather");
            check(!invoke("captureManager",new com.oplus.keyguard.clock.base.util.ExtraInfoManager(warming))&&SystemWeatherText.snapshot()==retained,"base manager passive seeding also ignores a prewarming empty cache");
            check(BigClockTextContent.resolve("左（天气） 右(天气)/(温度)",0L,TimeZone.getTimeZone("UTC")).equals("左系统锁屏当前天气 19° 右系统锁屏当前天气 19°/19°"),"both text components consume the seeded native cache in the same event");
            common.setLocatedForCheck(located);
            check(invoke("captureManager",common)&&SystemWeatherText.snapshot().text.equals("原生已缓存 26°"),"switching back to another active model reselects its unchanged cached data instead of incorrectly deduplicating it");
            check(invoke("captureManager",base),"selecting the base model again restores its current located data");
            current.setDestroyed(true);current.setData(data("已销毁",99));retained=SystemWeatherText.snapshot();
            check(!invoke("captureManager",base)&&SystemWeatherText.snapshot()==retained,"destroyed manager controllers cannot republish stale weather");
            current.setDestroyed(false);current.setData(data("刷新后的缓存",20));check(invoke("captureManager",base),"a real changed located cache refreshes after the native manager event");
            current.setData(null);retained=SystemWeatherText.snapshot();
            check(!invoke("captureManager",base)&&SystemWeatherText.snapshot()==retained,"passive manager seeding never interprets null as a genuine empty delivery");
            check(SystemWeatherText.captureController(current)&&SystemWeatherText.snapshot().text.equals(SystemWeatherText.UNAVAILABLE),"a real native null-data delivery still clears old weather");
            SystemWeatherText.clear();
            j6.f uninitialized=new j6.f(()->{throw new AssertionError("a new native manager must not be constructed");});
            com.oplus.keyguard.clock.base.domain.model.DateMessageViewModel baseModel=new com.oplus.keyguard.clock.base.domain.model.DateMessageViewModel(uninitialized);
            check(!invoke("captureViewModel",baseModel)&&uninitialized.getterCalls==0&&uninitialized.initializations==0,"uninitialized native lazy values are inspected without invoking their getter/initializer");
            current.setData(data("初始化前已存在",21));uninitialized.b=base;
            check(invoke("captureViewModel",baseModel)&&SystemWeatherText.snapshot().text.equals("初始化前已存在 21°")&&uninitialized.getterCalls==0,"an already initialized active base model seeds its exact cached manager without calling lazy getValue");
            located.setData(data("相册锁屏缓存",28));common.setLocatedForCheck(located);j6.f galleryLazy=new j6.f(common);
            com.oplus.keyguard.clock.gallery.domain.model.ExtraMessageViewModel gallery=new com.oplus.keyguard.clock.gallery.domain.model.ExtraMessageViewModel(galleryLazy);
            check(invoke("captureViewModel",gallery)&&SystemWeatherText.snapshot().text.equals("相册锁屏缓存 28°")&&galleryLazy.getterCalls==0,"gallery model selects its common manager's located data");
            retained=SystemWeatherText.snapshot();j6.f wrong=new j6.f(common);
            check(!invoke("captureViewModel",new com.oplus.keyguard.clock.base.domain.model.DateMessageViewModel(wrong))&&SystemWeatherText.snapshot()==retained,"a base model cannot accidentally seed another manager family");
            check(!invoke("captureManager",new Object())&&!invoke("captureViewModel",new Object())&&!invoke("captureManager",null)&&!invoke("captureViewModel",null),"unrelated and missing objects leave native weather untouched");
            SystemWeatherText.clear();
            WeatherEffectController queued=new WeatherEffectController(true,data("排队前",22));
            int[] deliveries={0},changes={0};queued.setCompleteActionForCheck(value->deliveries[0]++);
            queued.sendForCheck();queued.setData(data("真正交付的天气",24));
            check(deliveries[0]==0,"native send only queues its completion callback");
            queued.drainForCheck(()->{if(SystemWeatherText.captureController(queued))changes[0]++;});
            check(deliveries[0]==1&&changes[0]==1&&SystemWeatherText.snapshot().text.equals("真正交付的天气 24°"),"observing the real queued native completion captures its current delivered model");
            check(queued.getterReads==0,"manager/model/delivery seeding never calls the native cache getter");
            queued.sendForCheck();queued.drainForCheck(()->{if(SystemWeatherText.captureController(queued))changes[0]++;});
            check(deliveries[0]==2&&changes[0]==1,"duplicate delivery retains the same weather revision");
            queued.setData(data("已销毁的排队天气",99));queued.sendForCheck();queued.setDestroyed(true);
            retained=SystemWeatherText.snapshot();queued.drainForCheck(()->SystemWeatherText.captureController(queued));
            check(deliveries[0]==2&&SystemWeatherText.snapshot()==retained,"a destroyed controller's queued completion cannot publish old data");
            SystemWeatherText.clear();check(SystemWeatherText.snapshot().text.equals(SystemWeatherText.UNAVAILABLE),"module reset releases the previous native weather cache");
        } finally {SystemWeatherText.clear();}
        System.out.println("SystemWeatherDeliveryCheck: "+checks+" checks passed");
    }
}
