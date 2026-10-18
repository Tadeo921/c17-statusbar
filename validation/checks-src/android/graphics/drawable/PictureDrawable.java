package android.graphics.drawable;
public class PictureDrawable extends Drawable {
    private android.graphics.Picture picture;
    public PictureDrawable(android.graphics.Picture value){picture=value;}
    public android.graphics.Picture getPicture(){return picture;}
    public void setPicture(android.graphics.Picture value){picture=value;}
    @Override public int getIntrinsicWidth(){return picture==null?-1:picture.getWidth();}
    @Override public int getIntrinsicHeight(){return picture==null?-1:picture.getHeight();}
    @Override public void draw(android.graphics.Canvas canvas){if(picture!=null){int saved=canvas.save();try{android.graphics.Rect bounds=getBounds();canvas.clipRect(bounds.left,bounds.top,bounds.right,bounds.bottom);canvas.translate(bounds.left,bounds.top);canvas.drawPicture(picture);}finally{canvas.restoreToCount(saved);}}}
    @Override public void setAlpha(int alpha){}@Override public void setColorFilter(android.graphics.ColorFilter filter){}
    @Override public int getOpacity(){return -3;}
}
