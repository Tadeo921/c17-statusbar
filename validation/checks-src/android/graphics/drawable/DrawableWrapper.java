package android.graphics.drawable;

/** Public Android wrapper contract: retain the parent's native draw operation
 * while alpha/filter ownership belongs to the wrapped drawable. */
public class DrawableWrapper extends Drawable {
    private Drawable drawable;
    public DrawableWrapper(Drawable value){drawable=value;}
    public Drawable getDrawable(){return drawable;}
    public void setDrawable(Drawable value){drawable=value;}
    @Override public void draw(android.graphics.Canvas canvas){if(drawable!=null)drawable.draw(canvas);}
    @Override public void setAlpha(int value){if(drawable!=null)drawable.setAlpha(value);}
    @Override public int getAlpha(){return drawable==null?255:drawable.getAlpha();}
    @Override public void setColorFilter(android.graphics.ColorFilter value){if(drawable!=null)drawable.setColorFilter(value);}
    @Override public android.graphics.ColorFilter getColorFilter(){return drawable==null?null:drawable.getColorFilter();}
    @Override public int getOpacity(){return -3;}
    @Override protected void onBoundsChange(android.graphics.Rect bounds){if(drawable!=null)drawable.setBounds(bounds);}
}
