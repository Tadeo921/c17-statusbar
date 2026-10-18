package android.graphics.drawable;
public class InsetDrawable extends Drawable {
    private final Drawable drawable;
    public final float left,top,right,bottom;
    public InsetDrawable(Drawable drawable,float left,float top,float right,float bottom){this.drawable=drawable;this.left=left;this.top=top;this.right=right;this.bottom=bottom;}
    public Drawable getDrawable(){return drawable;}
    protected void onBoundsChange(android.graphics.Rect bounds){drawable.setBounds(
            bounds.left+(int)(bounds.width()*left),bounds.top+(int)(bounds.height()*top),
            bounds.right-(int)(bounds.width()*right),bounds.bottom-(int)(bounds.height()*bottom));}
    public boolean setLevel(int value){super.setLevel(value);drawable.setLevel(value);return true;}
    public void draw(android.graphics.Canvas canvas){drawable.draw(canvas);}
    public void setAlpha(int value){drawable.setAlpha(value);}
    public void setColorFilter(android.graphics.ColorFilter filter){drawable.setColorFilter(filter);}
    public int getOpacity(){return 0;}
    public void setTint(int color){drawable.setTint(color);}
}
