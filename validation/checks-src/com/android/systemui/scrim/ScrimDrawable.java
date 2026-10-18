package com.android.systemui.scrim;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
/** The inspected native leaf owns only its dim colour, never a blur registration. */
public class ScrimDrawable extends Drawable {
    public final Paint paint=new Paint();
    public int draws;
    public void draw(Canvas canvas){draws++;canvas.drawRect(getBounds(),paint);}
    public void setAlpha(int value){paint.setAlpha(value);}
    public int getAlpha(){return paint.getAlpha();}
    public void setColorFilter(ColorFilter value){paint.setColorFilter(value);}
    public int getOpacity(){return 0;}
}
