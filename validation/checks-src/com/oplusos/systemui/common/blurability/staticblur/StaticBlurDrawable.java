package com.oplusos.systemui.common.blurability.staticblur;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
/** The inspected static leaf draws its native background before its independent maskPaint. */
public class StaticBlurDrawable extends Drawable {
    public final Paint maskPaint=new Paint();
    public Drawable background;
    public int draws;
    public void draw(Canvas canvas){draws++;if(background!=null)background.draw(canvas);canvas.drawRect(getBounds(),maskPaint);}
    public void setAlpha(int value){maskPaint.setAlpha(value);}
    public int getAlpha(){return maskPaint.getAlpha();}
    public void setColorFilter(ColorFilter value){maskPaint.setColorFilter(value);}
    public int getOpacity(){return 0;}
}
