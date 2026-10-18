package com.oplusos.systemui.common.blurability.platformblur;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import com.oplus.posteffect.BlurDrawable;
public class PlatformBlurDrawable extends Drawable {
    public BlurDrawable blurDrawable=new BlurDrawable();
    public final Paint maskPaint=new Paint();
    public boolean enableBlurMaskColor;
    public int blurMaskColor;
    public PlatformBlurDrawable(){}
    public PlatformBlurDrawable(BlurDrawable engine,kotlin.jvm.functions.Function0<?> hostName){blurDrawable=engine;}
    public BlurDrawable getBlurDrawable(){return blurDrawable;}
    public void draw(Canvas canvas){blurDrawable.draw(canvas);if(enableBlurMaskColor&&blurMaskColor!=0&&maskPaint.getAlpha()!=0)canvas.drawRect(getBounds(),maskPaint);}
    public void setAlpha(int value){}
    public void setColorFilter(ColorFilter value){}
    public int getOpacity(){return 0;}
}
