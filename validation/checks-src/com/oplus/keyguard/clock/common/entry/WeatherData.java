package com.oplus.keyguard.clock.common.entry;

/** Shape verified from the author's previously collected native clock APK.
 * Test coordination fields are not part of the production reflection contract. */
public final class WeatherData {
    private final int weatherId, dayTemperature, nightTemperature, resId;
    private final long cityId;
    private final String weatherDescription;
    public static java.util.concurrent.CountDownLatch entered, resume;
    public static int descriptionReads;
    public WeatherData(int weatherId, String description, int day, int night, int resId, long cityId) {
        this.weatherId=weatherId;weatherDescription=description;dayTemperature=day;nightTemperature=night;this.resId=resId;this.cityId=cityId;
    }
    public String getWeatherDescription() {
        descriptionReads++;
        java.util.concurrent.CountDownLatch waiting=entered, ready=resume;
        if(waiting!=null&&ready!=null)try{waiting.countDown();if(!ready.await(5,java.util.concurrent.TimeUnit.SECONDS))throw new IllegalStateException("fixture timeout");}
        catch(InterruptedException interrupted){Thread.currentThread().interrupt();throw new IllegalStateException(interrupted);}
        return weatherDescription;
    }
    public int getDayTemperature(){return dayTemperature;}
    public int getNightTemperature(){return nightTemperature;}
    public int getWeatherId(){return weatherId;}
    public int getResId(){return resId;}
    public long getCityId(){return cityId;}
}
