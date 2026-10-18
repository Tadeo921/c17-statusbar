package com.airbnb.lottie;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import com.airbnb.lottie.model.KeyPath;
import com.airbnb.lottie.value.LottieValueCallback;
import java.util.*;
/** Real retained method/field shape. It intentionally offers no addAnimatorListener/cancelAnimation. */
public final class LottieDrawable extends Drawable {
    public final com.airbnb.lottie.utils.LottieValueAnimator animator=new com.airbnb.lottie.utils.LottieValueAnimator();
    public LottieComposition composition;public float progress,speed;public int plays,clears,alpha=255,repeats,resumes;public RenderMode renderMode;
    private boolean hidden;private final List<KeyPath> paths=Arrays.asList(new KeyPath(new com.airbnb.lottie.animation.content.FillContent()),new KeyPath(new com.airbnb.lottie.animation.content.StrokeContent()));
    public final Map<Object,LottieValueCallback> callbacks=new IdentityHashMap<>();public final Set<Integer> properties=new LinkedHashSet<>();
    public boolean setComposition(LottieComposition value){composition=value;return true;}public void clearComposition(){clears++;composition=null;animator.cancel();}
    public void playAnimation(){plays++;animator.start();}public void setRepeatCount(int value){repeats=value;}public void setSpeed(float value){speed=value;}public void setProgress(float value){progress=value;}public float getProgress(){return progress;}
    public void setRenderMode(RenderMode mode){renderMode=mode;}public List<KeyPath> resolveKeyPath(KeyPath query){return paths;}
    public void addValueCallback(KeyPath path,Object property,LottieValueCallback callback){properties.add((Integer)property);if(callback==null)callbacks.remove(path.element);else callbacks.put(path.element,callback);}
    public boolean setVisible(boolean visible,boolean restart){if(visible&&hidden&&composition!=null)resumes++;hidden=!visible;return true;}
    public void draw(Canvas canvas){}public void setAlpha(int value){alpha=value;}public int getAlpha(){return alpha;}public void setColorFilter(ColorFilter value){}public int getOpacity(){return -3;}
}
