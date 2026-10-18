package com.oplusos.systemui.common.blurability.drawable;
import android.graphics.*;
import android.graphics.drawable.Drawable;
/** Current ROM WindowBg leaf has only this colour plane. */
public final class MaskDrawable extends Drawable {
    private final Paint maskPaint=new Paint();
    public int colorForCheck=0x80000000,drawsForCheck;
    public void draw(Canvas canvas){drawsForCheck++;maskPaint.setColor(colorForCheck);if(maskPaint.getAlpha()!=0)canvas.drawRect(getBounds(),maskPaint);}
    public void setAlpha(int value){}
    public void setColorFilter(ColorFilter value){}
    public int getOpacity(){return 0;}
}
