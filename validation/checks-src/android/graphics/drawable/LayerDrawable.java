package android.graphics.drawable;
public class LayerDrawable extends Drawable {
    private final Drawable[] drawables;
    private final int[][] insets;
    private final ConstantState constantState=new ConstantState(){
        public Drawable newDrawable(){
            Drawable[] copies=new Drawable[drawables.length];
            for(int i=0;i<copies.length;i++)copies[i]=drawables[i].getConstantState().newDrawable();
            LayerDrawable copy=new LayerDrawable(copies);
            for(int i=0;i<copies.length;i++)copy.setLayerInset(i,insets[i][0],insets[i][1],insets[i][2],insets[i][3]);
            return copy;
        }
    };
    public LayerDrawable(Drawable[] drawables){this.drawables=drawables;insets=new int[drawables.length][4];}
    public void setDrawable(int index,Drawable drawable){drawable.setBounds(drawables[index].getBounds());drawables[index]=drawable;}
    public void setLayerInset(int index,int left,int top,int right,int bottom){insets[index]=new int[]{left,top,right,bottom};onBoundsChange(getBounds());}
    public int getLayerInsetLeft(int index){return insets[index][0];}
    public int getLayerInsetTop(int index){return insets[index][1];}
    public int getLayerInsetRight(int index){return insets[index][2];}
    public int getLayerInsetBottom(int index){return insets[index][3];}
    protected void onBoundsChange(android.graphics.Rect bounds){
        for(int i=0;i<drawables.length;i++)drawables[i].setBounds(bounds.left+insets[i][0],bounds.top+insets[i][1],bounds.right-insets[i][2],bounds.bottom-insets[i][3]);
    }
    public void setId(int index,int id){ }
    public Drawable getDrawable(int index){return drawables[index];}
    public int getNumberOfLayers(){return drawables.length;}
    public ConstantState getConstantState(){
        for(Drawable value:drawables)if(value.getConstantState()==null)return null;
        return constantState;
    }
    public void draw(android.graphics.Canvas canvas){for(Drawable value:drawables)value.draw(canvas);}
    public void setAlpha(int value){for(Drawable drawable:drawables)drawable.setAlpha(value);}
    public void setColorFilter(android.graphics.ColorFilter filter){for(Drawable drawable:drawables)drawable.setColorFilter(filter);}
    public int getOpacity(){return 0;}
    public void setTint(int color){for(Drawable drawable:drawables)drawable.setTint(color);}
    public boolean setLevel(int level){super.setLevel(level);for(Drawable drawable:drawables)drawable.setLevel(level);return true;}
}
