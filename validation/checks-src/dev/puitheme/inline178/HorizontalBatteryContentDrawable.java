package dev.puitheme.inline178;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import dev.puitheme.PuiBatteryStyleCheck;

/** Audited OEM seam: native coordinates include density padding and native
 * percentage typography; callers must scale this complete draw, not its slots. */
public final class HorizontalBatteryContentDrawable extends PuiBatteryStyleCheck.NativeBar {
    private final Context source;
    public Drawable bgDrawable=new PuiBatteryStyleCheck.Graphic();
    public LayerDrawable progressDrawable=new LayerDrawable(new Drawable[]{new PuiBatteryStyleCheck.Graphic()});
    public final RectF noPercentRectF=new RectF();
    public final Paint percentInPaint=new Paint(1);
    public boolean isShowPercentIn=true;
    public int nativeDraws,destroyed,measurements;
    public HorizontalBatteryContentDrawable(Context source,int flags) {
        this.source=source;
        percentInPaint.setTypeface(Typeface.create(Typeface.DEFAULT,750,false));
        percentInPaint.setTextSize(37);
    }
    public Context getContext(){return source;}
    public void setShowPercentIn(boolean value){isShowPercentIn=value;}
    public void setBatteryLevel(int value){batteryLevel=value;}
    public void setChargeIconId(int value){chargeIconId=value;}
    public void updatePercentInTextBounds(){measurements++;}
    public void onDestroy(){destroyed++;}
    protected void onBoundsChange(Rect bounds) {
        rect.set(bounds.left+8,bounds.top+8,bounds.right-12,bounds.bottom-8);
        noPercentRectF.set(bounds.left+4,bounds.top+4,bounds.right-8,bounds.bottom-4);
    }
    public void draw(Canvas canvas) {
        nativeDraws++;
        if(isShowPercentIn) {
            outsideDrawable.draw(canvas);insideDrawable.draw(canvas);frameDrawable.draw(canvas);
            canvas.drawText(Integer.toString(batteryLevel),rect.centerX(),rect.centerY(),percentInPaint);
        } else {bgDrawable.draw(canvas);progressDrawable.draw(canvas);}
    }
}
