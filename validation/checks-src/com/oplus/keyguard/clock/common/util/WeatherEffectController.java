package com.oplus.keyguard.clock.common.util;

import com.oplus.keyguard.clock.common.entry.WeatherData;

/** Only native state shape; the fixture never queries a weather service. */
public final class WeatherEffectController {
    private WeatherData currentData;
    private final boolean isLocated;
    private volatile boolean isDestroyed;
    private java.util.function.Consumer<WeatherData> completeAction;
    private final java.util.List<Runnable> queuedForCheck=new java.util.ArrayList<>();
    public int getterReads;
    public WeatherEffectController(boolean located,WeatherData data){isLocated=located;currentData=data;}
    public void setData(WeatherData value){currentData=value;}
    public void setDestroyed(boolean value){isDestroyed=value;}
    public WeatherData getCurrentData(){getterReads++;return currentData;}
    public void setCompleteActionForCheck(java.util.function.Consumer<WeatherData> value){completeAction=value;}
    public void sendForCheck(){sendWeatherData();}
    /** Native delivery is queued, so an observer on send alone does not cover
     * an already queued callback when its loader hook is installed late. */
    private void sendWeatherData(){if(!isDestroyed)queuedForCheck.add(()->sendWeatherData$lambda$15(this));}
    private static void sendWeatherData$lambda$15(WeatherEffectController owner) {
        if(!owner.isDestroyed&&owner.completeAction!=null)owner.completeAction.accept(owner.currentData);
    }
    public void drainForCheck(Runnable afterNativeDelivery) {
        java.util.List<Runnable> ready=new java.util.ArrayList<>(queuedForCheck);queuedForCheck.clear();
        for(Runnable delivery:ready){delivery.run();afterNativeDelivery.run();}
    }
}
