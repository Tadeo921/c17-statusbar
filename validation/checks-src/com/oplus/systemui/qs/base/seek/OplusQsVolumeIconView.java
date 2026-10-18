package com.oplus.systemui.qs.base.seek;
public class OplusQsVolumeIconView extends android.widget.ImageView {
    private com.android.systemui.plugins.qs.QSVolume.VolumeState qsVolumeState;
    public boolean animating;
    public final java.util.List<android.animation.Animator.AnimatorListener> listeners=new java.util.ArrayList<>();
    public OplusQsVolumeIconView(){super(new android.content.Context());}
    public void nativeState(com.android.systemui.plugins.qs.QSVolume.VolumeState state){qsVolumeState=state;}
    public boolean isAnimating(){return animating;}
    public void addAnimatorListener(android.animation.Animator.AnimatorListener listener){listeners.add(listener);}
    public void removeAnimatorListener(android.animation.Animator.AnimatorListener listener){listeners.remove(listener);}
    public void finish(){animating=false;for(android.animation.Animator.AnimatorListener listener:new java.util.ArrayList<>(listeners))listener.onAnimationEnd(null);}
    public void updateIconState(com.android.systemui.plugins.qs.QSVolume.VolumeState state){qsVolumeState=state;}
    public void updateIconState(com.android.systemui.plugins.qs.QSVolume.VolumeState state,boolean animate){qsVolumeState=state;}
    public void updateIconColor(boolean first,boolean second){}
    public void disableTheme(boolean value){}
}
