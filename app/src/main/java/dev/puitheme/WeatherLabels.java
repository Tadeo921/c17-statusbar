// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie

package dev.puitheme;

import java.util.HashMap;
import java.util.Map;

/** Chinese / English weather names for the left/right text components.
 * The system description stays authoritative for "follow system"; a fixed
 * language is resolved from the native icon category first (language
 * independent), then from a closed table of common descriptions. An
 * unrecognised description is shown as delivered, never guessed. */
final class WeatherLabels {
    static final int SYSTEM = 0, CHINESE = 1, ENGLISH = 2;
    private static final Map<WeatherTextIcon.Kind, String[]> KINDS = new HashMap<>();
    private static final Map<String, String[]> DESCRIPTIONS = new HashMap<>();
    private WeatherLabels() { }

    private static void kind(WeatherTextIcon.Kind kind, String zh, String en) { KINDS.put(kind, new String[]{zh, en}); }
    private static void words(String zh, String en) {
        String[] pair = {zh, en};
        DESCRIPTIONS.put(zh, pair); DESCRIPTIONS.put(en.toLowerCase(java.util.Locale.ROOT), pair);
    }

    static {
        kind(WeatherTextIcon.Kind.SUNNY, "晴", "Sunny");
        kind(WeatherTextIcon.Kind.CLEAR_NIGHT, "晴", "Clear");
        kind(WeatherTextIcon.Kind.PARTLY_CLOUDY, "多云", "Partly cloudy");
        kind(WeatherTextIcon.Kind.CLOUDY_NIGHT, "多云", "Partly cloudy");
        kind(WeatherTextIcon.Kind.CLOUDY, "阴", "Cloudy");
        kind(WeatherTextIcon.Kind.LIGHT_RAIN, "小雨", "Light rain");
        kind(WeatherTextIcon.Kind.MODERATE_RAIN, "中雨", "Moderate rain");
        kind(WeatherTextIcon.Kind.HEAVY_RAIN, "大雨", "Heavy rain");
        kind(WeatherTextIcon.Kind.ICE_RAIN, "冻雨", "Freezing rain");
        kind(WeatherTextIcon.Kind.HAILSTONE, "冰雹", "Hail");
        kind(WeatherTextIcon.Kind.THUNDERSHOWERS, "雷阵雨", "Thundershowers");
        kind(WeatherTextIcon.Kind.THUNDERSHOWERS_HAILSTONE, "雷阵雨伴冰雹", "Thunder and hail");
        kind(WeatherTextIcon.Kind.THUNDERSTORM, "雷暴", "Thunderstorm");
        kind(WeatherTextIcon.Kind.LIGHT_SNOW, "小雪", "Light snow");
        kind(WeatherTextIcon.Kind.MODERATE_SNOW, "中雪", "Moderate snow");
        kind(WeatherTextIcon.Kind.HEAVY_SNOW, "大雪", "Heavy snow");
        kind(WeatherTextIcon.Kind.SLEET, "雨夹雪", "Sleet");
        kind(WeatherTextIcon.Kind.FOG, "雾", "Fog");
        kind(WeatherTextIcon.Kind.SINK_FLOAT, "浮尘", "Dust");
        kind(WeatherTextIcon.Kind.DUST_DEVIL, "沙尘", "Sandstorm");
        kind(WeatherTextIcon.Kind.HAZE, "霾", "Haze");
        kind(WeatherTextIcon.Kind.WIND, "大风", "Windy");
        kind(WeatherTextIcon.Kind.STORM, "风暴", "Storm");
        kind(WeatherTextIcon.Kind.TORNADO, "龙卷风", "Tornado");
        kind(WeatherTextIcon.Kind.SUNRISE, "日出", "Sunrise");
        kind(WeatherTextIcon.Kind.SUNSET, "日落", "Sunset");

        words("晴", "Sunny"); words("晴朗", "Clear"); words("多云", "Partly cloudy"); words("少云", "Mostly sunny");
        words("阴", "Cloudy"); words("阴天", "Overcast"); words("小雨", "Light rain"); words("中雨", "Moderate rain");
        words("大雨", "Heavy rain"); words("暴雨", "Rainstorm"); words("大暴雨", "Heavy rainstorm"); words("毛毛雨", "Drizzle");
        words("阵雨", "Showers"); words("雨", "Rain"); words("冻雨", "Freezing rain"); words("雷阵雨", "Thundershowers");
        words("雷暴", "Thunderstorm"); words("冰雹", "Hail"); words("小雪", "Light snow"); words("中雪", "Moderate snow");
        words("大雪", "Heavy snow"); words("暴雪", "Blizzard"); words("阵雪", "Snow showers"); words("雪", "Snow");
        words("雨夹雪", "Sleet"); words("雾", "Fog"); words("大雾", "Dense fog"); words("霾", "Haze"); words("浮尘", "Dust");
        words("扬沙", "Blowing sand"); words("沙尘暴", "Sandstorm"); words("大风", "Windy"); words("龙卷风", "Tornado");
    }

    static String description(SystemWeatherText.Snapshot weather, int language) {
        String original = weather.description;
        if (language == SYSTEM || original.isEmpty()) return original;
        int index = language == CHINESE ? 0 : 1;
        String[] pair = KINDS.get(weather.weatherIconKind);
        boolean night = weather.weatherIconKind == WeatherTextIcon.Kind.CLEAR_NIGHT
                || weather.weatherIconKind == WeatherTextIcon.Kind.CLOUDY_NIGHT;
        // The native category already carries day/night; a closed text table
        // then refines the rest. Unrecognised text is kept as delivered.
        if (night && pair != null) return pair[index];
        String[] known = DESCRIPTIONS.get(original.toLowerCase(java.util.Locale.ROOT));
        if (known != null) return known[index];
        return pair != null ? pair[index] : original;
    }

    static String unavailable(int language) {
        return language == ENGLISH ? "Weather unavailable" : SystemWeatherText.UNAVAILABLE;
    }

    static String temperatureUnavailable(int language) {
        return language == ENGLISH ? "Temperature unavailable" : SystemWeatherText.TEMPERATURE_UNAVAILABLE;
    }
}
