package android.graphics.drawable;
public class ColorDrawable extends Drawable {
 private int color; public ColorDrawable(){} public ColorDrawable(int color){this.color=color;}
 public void setColor(int value){color=value;}public int getColor(){return color;}
 @Override public void draw(android.graphics.Canvas canvas){}
 @Override public void setAlpha(int alpha){color=(color&0xffffff)|(alpha<<24);}
 @Override public int getAlpha(){return color>>>24;}
 @Override public void setColorFilter(android.graphics.ColorFilter filter){}
 @Override public int getOpacity(){return android.graphics.PixelFormat.TRANSLUCENT;}
}
