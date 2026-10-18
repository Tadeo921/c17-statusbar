package com.android.systemui.scrim;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.View;
/** The inspected ScrimView skips its native leaf when drawable alpha is zero. */
public class ScrimView extends View {
    public Drawable mDrawable;
    public ScrimView(){super(null);}
    public void onDraw(Canvas canvas){if(mDrawable!=null&&mDrawable.getAlpha()>0)mDrawable.draw(canvas);}
}
