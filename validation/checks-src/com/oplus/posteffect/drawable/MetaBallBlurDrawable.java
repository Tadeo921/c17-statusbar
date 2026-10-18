package com.oplus.posteffect.drawable;
/** Native setter contract used by NotificationBackgroundCustomRowHandler. */
public class MetaBallBlurDrawable extends android.graphics.drawable.Drawable {
    public int metaColor,changes;
    public int[] gradientColors;
    public android.graphics.drawable.GradientDrawable.Orientation orientation;
    public void setMetaColor(int color){metaColor=color;gradientColors=null;changes++;}
    public void setGradientColors(android.graphics.drawable.GradientDrawable.Orientation direction,int[] colors){orientation=direction;gradientColors=colors.clone();changes++;}
    public void draw(android.graphics.Canvas canvas){ }
    public void setAlpha(int alpha){ }
    public void setColorFilter(android.graphics.ColorFilter filter){ }
    public int getOpacity(){return android.graphics.PixelFormat.TRANSLUCENT;}
}
