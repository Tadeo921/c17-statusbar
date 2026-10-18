package com.oplus.keyguard.clock.common.util;

/** Only the two resident/located native fields. No service is constructed. */
public final class ExtraInfoManager {
    private WeatherEffectController locatedWeatherController;
    private WeatherEffectController residentWeatherController;
    public ExtraInfoManager(WeatherEffectController located,WeatherEffectController resident) {
        locatedWeatherController=located;residentWeatherController=resident;
    }
    public void setLocatedForCheck(WeatherEffectController value){locatedWeatherController=value;}
}
