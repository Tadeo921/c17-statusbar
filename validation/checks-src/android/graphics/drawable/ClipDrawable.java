package android.graphics.drawable;
public class ClipDrawable extends Drawable {
    public static final int HORIZONTAL=1;
    private final Drawable drawable;
    public ClipDrawable(Drawable drawable,int gravity,int orientation){this.drawable=drawable;}
    public Drawable getWrapped(){return drawable;}
    protected void onBoundsChange(android.graphics.Rect bounds){drawable.setBounds(bounds);}
    public boolean setLevel(int value){super.setLevel(value);drawable.setLevel(value);return true;}
    public void draw(android.graphics.Canvas canvas){
        if(getLevel()==0)return;
        android.graphics.Rect b=getBounds();int width=b.width()-(b.width()*(10000-getLevel())/10000);
        int save=canvas.save();
        try{canvas.clipRect(b.left,b.top,b.left+width,b.bottom);drawable.draw(canvas);}
        finally{canvas.restoreToCount(save);}
    }
    public void setAlpha(int value){drawable.setAlpha(value);}
    public void setColorFilter(android.graphics.ColorFilter filter){drawable.setColorFilter(filter);}
    public int getOpacity(){return 0;}
    public void setTint(int color){drawable.setTint(color);}
}
