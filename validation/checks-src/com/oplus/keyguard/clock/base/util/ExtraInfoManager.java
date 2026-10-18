package com.oplus.keyguard.clock.base.util;

/** Captured base manager also uses the common located controller class. */
public final class ExtraInfoManager {
    private com.oplus.keyguard.clock.common.util.WeatherEffectController weatherEffectController;
    public ExtraInfoManager(com.oplus.keyguard.clock.common.util.WeatherEffectController value){weatherEffectController=value;}
    public void setControllerForCheck(com.oplus.keyguard.clock.common.util.WeatherEffectController value){weatherEffectController=value;}
}
