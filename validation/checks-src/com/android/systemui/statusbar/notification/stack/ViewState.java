package com.android.systemui.statusbar.notification.stack;
/** Exact inspected state accessors; callers keep native state instances and animation ownership. */
public class ViewState {
    public static final int TAG_ANIMATOR_TRANSLATION_X=0x7f0a0f94;
    public static final int TAG_START_TRANSLATION_X=0x7f0a0f93;
    public static final int TAG_END_TRANSLATION_X=0x7f0a0f92;
    public static final AnimationProperties NO_NEW_ANIMATIONS=new AnimationProperties(){};
    private float mXTranslation,mYTranslation;
    private float mAlpha=1f;
    public int nativeXCallsForCheck;
    public Runnable nativeXCallbackForCheck;
    public float getXTranslation(){return mXTranslation;}
    public float getYTranslation(){return mYTranslation;}
    public float getAlpha(){return mAlpha;}
    public void setAlpha(float value,String reason){mAlpha=value;}
    public void setXTranslation(float value){mXTranslation=value;}
    public void setYTranslation(float value,String source){mYTranslation=value;}
    /** Exact DEX branch at 0x39af4c: retarget active X in place, retaining playtime. */
    public final void startXTranslationAnimation(android.view.View view,AnimationProperties properties){
        nativeXCallsForCheck++;
        if(properties.getAnimationFilter().animateX)throw new AssertionError("new animation requested");
        Float start=(Float)view.getTag(TAG_START_TRANSLATION_X),end=(Float)view.getTag(TAG_END_TRANSLATION_X);
        if(end!=null&&end.floatValue()==mXTranslation)return;
        android.animation.ObjectAnimator animator=(android.animation.ObjectAnimator)view.getTag(TAG_ANIMATOR_TRANSLATION_X);
        if(animator==null)view.setTranslationX(mXTranslation);
        else {
            float nextStart=start+(mXTranslation-end);
            animator.getValues()[0].setFloatValues(nextStart,mXTranslation);
            view.setTag(TAG_START_TRANSLATION_X,nextStart);view.setTag(TAG_END_TRANSLATION_X,mXTranslation);
            animator.setCurrentPlayTime(animator.getCurrentPlayTime());
        }
        if(nativeXCallbackForCheck!=null)nativeXCallbackForCheck.run();
    }
}
