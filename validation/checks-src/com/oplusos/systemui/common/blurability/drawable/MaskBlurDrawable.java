package com.oplusos.systemui.common.blurability.drawable;
import android.graphics.*;
import android.graphics.drawable.Drawable;
/** Current-ROM body: one selector call, selected blur then its independent colour;
 * the null-selection fallback has no additional colour plane. */
public class MaskBlurDrawable extends Drawable {
    public final com.oplusos.systemui.common.blurability.ViewBlurProxy viewBlurProxy;
    private Drawable defaultDrawable;
    public int maskColor;
    public int drawsForCheck,selectionsForCheck;
    public java.util.function.BiConsumer<Object,Object> afterSelectionForCheck;
    public MaskBlurDrawable(com.oplusos.systemui.common.blurability.ViewBlurProxy p){viewBlurProxy=p;}
    public MaskBlurDrawable(com.oplusos.systemui.common.blurability.ViewBlurProxy p,Drawable fallback,int color){viewBlurProxy=p;defaultDrawable=fallback;maskColor=color;}
    private Drawable getDrawable(){selectionsForCheck++;Drawable selected=viewBlurProxy.getBlurDrawable(null);
        if(afterSelectionForCheck!=null)afterSelectionForCheck.accept(this,selected);return selected;}
    public void draw(Canvas c){drawsForCheck++;Drawable d=getDrawable();if(d!=null){d.setBounds(getBounds());d.draw(c);if(maskColor!=0)c.drawColor(maskColor);}
        else if(defaultDrawable!=null){defaultDrawable.setBounds(getBounds());defaultDrawable.draw(c);}}
    public void setAlpha(int v){}
    public void setColorFilter(ColorFilter c){}
    public int getOpacity(){return 0;}
}
