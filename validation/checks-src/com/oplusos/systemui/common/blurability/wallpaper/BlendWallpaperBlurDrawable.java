package com.oplusos.systemui.common.blurability.wallpaper;
import android.view.View;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import com.oplusos.systemui.common.blurability.BlurMixConfig;
/** Native apply retains the submitted mix identity and its exact wallpaper host. */
public class BlendWallpaperBlurDrawable extends Drawable {
    public com.oplus.posteffect.drawable.BlendDrawable blendDrawable=new com.oplus.posteffect.drawable.BlendDrawable();
    public BlurMixConfig currentBlurMixConfig;
    public View hostView;
    public com.oplus.posteffect.drawable.BlendDrawable getBlendDrawable(){return blendDrawable;}
    public BlurMixConfig getCurrentBlurMixConfig(){return currentBlurMixConfig;}
    public View getHostView(){return hostView;}
    public void draw(Canvas c){blendDrawable.draw(c);}
    public void setAlpha(int v){}
    public void setColorFilter(ColorFilter c){}
    public int getOpacity(){return 0;}
}
