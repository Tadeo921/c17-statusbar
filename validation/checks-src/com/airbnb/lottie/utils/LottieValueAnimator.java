package com.airbnb.lottie.utils;
import android.animation.Animator;
import java.util.*;
/** R8 retains listeners on the native animator, not addAnimatorListener on LottieDrawable. */
public final class LottieValueAnimator extends android.animation.ValueAnimator {
    private final List<Animator.AnimatorListener> listeners=new ArrayList<>();
    public void addListener(Animator.AnimatorListener listener){listeners.add(listener);}public void removeListener(Animator.AnimatorListener listener){listeners.remove(listener);}
    public void cancel(){super.cancel();for(Animator.AnimatorListener listener:new ArrayList<>(listeners)){listener.onAnimationCancel(null);listener.onAnimationEnd(null);}}
    public void finish(){for(Animator.AnimatorListener listener:new ArrayList<>(listeners))listener.onAnimationEnd(null);}
    public int listeners(){return listeners.size();}
}
