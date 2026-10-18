package com.android.systemui.plugins.keyguard.data.model;

/** Public getter shape from the captured SystemUI WeatherData model. */
public final class WeatherData {
    private final String description;
    private final int temperature;
    private final boolean useCelsius;
    public WeatherData(String description,int temperature,boolean useCelsius){this.description=description;this.temperature=temperature;this.useCelsius=useCelsius;}
    public String getDescription(){return description;}
    public int getTemperature(){return temperature;}
    public boolean getUseCelsius(){return useCelsius;}
}
