package dev.puitheme;

import com.oplus.keyguard.clock.common.R;
import com.oplus.keyguard.clock.common.entry.WeatherData;
import com.oplus.keyguard.clock.common.util.WeatherEffectController;

/** Why the lock-screen card weather used to be dropped or stuck: unit/sign
 * spellings the template may use, and a quiet higher-priority source. */
public final class SystemWeatherReliabilityCheck {
    private static int checks;
    private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    private static void temperature(String shown,String expected){
        SystemWeatherText.clear();WeatherNativeFixture fixture=new WeatherNativeFixture();
        boolean ok=fixture.update("晴",shown,"sunny.svg",true);
        if(expected==null){
            check(!ok&&SystemWeatherText.snapshot().description.isEmpty(),"rejected, never guessed: ["+shown+"]");
        } else {
            check(ok&&SystemWeatherText.snapshot().temperature.equals(expected)&&SystemWeatherText.snapshot().description.equals("晴"),
                    "["+shown+"] reads as "+expected+" but was ["+SystemWeatherText.snapshot().temperature+"]");
        }
    }

    private static void units() {
        temperature("23°","23°");
        temperature("°23","23°");
        temperature("23℃","23°C");
        temperature("23 ℃","23°C");
        temperature("23°C","23°C");
        temperature("72°F","72°F");
        temperature("72℉","72°F");
        temperature("23C","23°C");
        temperature("-5°","-5°");
        temperature("−5°","-5°");
        temperature("–5°","-5°");
        temperature("  18.5° ","18.5°");
        temperature("23","23°");
        temperature("",null);
        temperature("--",null);
        temperature("abc",null);
        temperature("°23°",null);
        temperature("1234°",null);
        temperature("23°°",null);
    }

    private static WeatherData data(String text,int id,int resource){return new WeatherData(id,text,19,12,resource,1L);}

    private static void staleSource() {
        SystemWeatherText.clear();SystemWeatherText.skewForCheck=0L;
        try {
            WeatherNativeFixture fixture=new WeatherNativeFixture();fixture.update("晴","23°","sunny.svg",true);
            check(SystemWeatherText.snapshot().text.equals("晴 23°"),"card weather published");
            WeatherEffectController controller=new WeatherEffectController(true,data("多云",3,R.drawable.ic_weather_partly_cloudy));
            check(!SystemWeatherText.captureController(controller)&&SystemWeatherText.snapshot().text.equals("晴 23°"),
                    "a recent card result still outranks the controller");
            SystemWeatherText.skewForCheck=30L*60L*1000L;
            check(!SystemWeatherText.captureController(new WeatherEffectController(true,data("阴",4,R.drawable.ic_weather_cloudy)))
                    &&SystemWeatherText.snapshot().text.equals("晴 23°"),"half an hour of silence is not stale");
            SystemWeatherText.skewForCheck=61L*60L*1000L;
            WeatherEffectController empty=new WeatherEffectController(true,null);
            long revision=SystemWeatherText.revision();
            check(!SystemWeatherText.captureController(empty)&&SystemWeatherText.snapshot().text.equals("晴 23°")&&SystemWeatherText.revision()==revision,
                    "an empty controller delivery never wipes stale but valid weather");
            check(SystemWeatherText.captureController(controller)&&SystemWeatherText.snapshot().text.equals("多云 19°"),
                    "a fresh controller result replaces a card that stopped updating for an hour");
            check(SystemWeatherText.snapshot().weatherIcon==null,"the controller result carries no widget picture, so none is fabricated");
            long after=SystemWeatherText.revision();
            check(fixture.update("小雨","16°","light_rain.svg",true)&&SystemWeatherText.snapshot().text.equals("小雨 16°")
                    &&SystemWeatherText.revision()==after+1,"the card takes over again as soon as it delivers");
            SystemWeatherText.skewForCheck=0L;
            check(!SystemWeatherText.captureController(new WeatherEffectController(true,data("晴",1,R.drawable.ic_weather_sunny))),
                    "after takeover back to the card, the controller is blocked again until it goes quiet");
        } finally {SystemWeatherText.skewForCheck=0L;SystemWeatherText.clear();}
    }

    private static void reConfirm() {
        SystemWeatherText.clear();SystemWeatherText.skewForCheck=0L;
        try {
            WeatherNativeFixture fixture=new WeatherNativeFixture();fixture.update("晴","23°","sunny.svg",true);
            SystemWeatherText.skewForCheck=50L*60L*1000L;
            SystemWeatherText.captureWidgetCard(fixture.card);
            fixture.update("晴","23°","sunny.svg",true);
            SystemWeatherText.skewForCheck=100L*60L*1000L;
            check(!SystemWeatherText.captureController(new WeatherEffectController(true,data("阴",4,R.drawable.ic_weather_cloudy))),
                    "an unchanged card read counts as a fresh confirmation");
        } finally {SystemWeatherText.skewForCheck=0L;SystemWeatherText.clear();}
    }

    public static void main(String[] args){
        units();staleSource();reConfirm();
        System.out.println("SystemWeatherReliabilityCheck: "+checks+" checks passed");
    }
}
