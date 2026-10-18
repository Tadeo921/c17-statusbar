package com.oplus.systemui.notification.blur;
import com.oplusos.systemui.common.blurability.BlurMixConfig;
import com.oplus.systemui.notification.row.material.NotificationBlurMode;
public class ViewBlurManager {
    public enum CardType {NOTIFICATION,CUSTOM_CARD}
    public NotificationBlurMode mode=NotificationBlurMode.SHADE_LIGHT;
    public int mixes;
    public int motionMixes;
    public boolean noMotionMixForCheck;
    public com.oplusos.systemui.common.blurability.MixColor motionColorForCheck;
    public boolean motionForCheck;
    public boolean shaderForCheck;
    public BlurMixConfig lastMixForCheck;
    public boolean useMotionBlur(){return motionForCheck;}
    public void updatePlatformBlurParams(com.oplusos.systemui.common.blurability.ViewBlurProxy proxy,boolean refresh){ }
    public NotificationBlurMode globalBlurMode(){return mode;}
    public BlurMixConfig getMixConfig(CardType type,NotificationBlurMode mode){mixes++;lastMixForCheck=shaderForCheck?new BlurMixConfig.BlurMixMultiWithShader(mode):new BlurMixConfig.BlurMixMulti(mode);lastMixForCheck.setAlphaWithBlurAmount(true);lastMixForCheck.setPlaceHolderColor(0x60403020);return lastMixForCheck;}
    public BlurMixConfig.BlurMixSingle headsupCardMotionMixConfig(CardType type,NotificationBlurMode mode){motionMixes++;return noMotionMixForCheck?null:motionColorForCheck==null?new BlurMixConfig.BlurMixSingle(mode):new BlurMixConfig.BlurMixSingle(motionColorForCheck);}
}
