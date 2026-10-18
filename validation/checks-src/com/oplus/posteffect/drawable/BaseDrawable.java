package com.oplus.posteffect.drawable;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import com.oplus.posteffect.BlurParam;

/** Captured native declaration sites used by the production hook installer.
 * The existing Continuous fixture retains its externally visible state. */
public class BaseDrawable extends Drawable {
    /** The real native body releases old drawing content before assigning the
     * pending image directly. A check callback stands at that exact gap. */
    public Runnable afterReleaseForCheck;
    public static void access$drawContent(BaseDrawable drawable){drawable.preDrawContent();}
    public static class ImageInfo {
        final com.oplus.posteffect.image.ContentImage image;
        public ImageInfo(Bitmap bitmap){image=new com.oplus.posteffect.image.ContentImage(bitmap);}
        public com.oplus.posteffect.image.ContentImage getImage(){return image;}
    }
    public void setDrawingParam(BlurParam value){((ContinuousBlurDrawable)this).params=value;}
    public void setEnableBlurShader(boolean value){((BlendDrawable)this).enableShader=value;}
    public void setEnableShader(boolean value){((BlendDrawable)this).enableShader=value;}
    public void setDrawingImageInfo(ImageInfo value){((ContinuousBlurDrawable)this).drawing=(ContinuousBlurDrawable.ImageInfo)value;}
    public boolean preDrawContent(){
        ContinuousBlurDrawable current=(ContinuousBlurDrawable)this;
        if(current.pending!=null){
            ContinuousBlurDrawable.ImageInfo next=current.pending;current.pending=null;
            releaseDrawingBitmap();
            if(afterReleaseForCheck!=null)afterReleaseForCheck.run();
            current.drawing=next;
        }
        return true;
    }
    public void releaseDrawingBitmap(){((ContinuousBlurDrawable)this).drawing=null;}
    public void draw(Canvas canvas){}
    public void setAlpha(int value){}
    public void setColorFilter(ColorFilter value){}
    public int getOpacity(){return 0;}
}
