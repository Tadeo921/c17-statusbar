package com.oplus.posteffect;
import android.graphics.Rect;
public class BlurParam {
    public enum Wrapping {
        MIRRORED_REPEAT(android.graphics.Shader.TileMode.MIRROR),CLAMP(android.graphics.Shader.TileMode.CLAMP),REPEAT(android.graphics.Shader.TileMode.REPEAT),DECAL(android.graphics.Shader.TileMode.DECAL);
        final android.graphics.Shader.TileMode mode;Wrapping(android.graphics.Shader.TileMode mode){this.mode=mode;}
        public android.graphics.Shader.TileMode toShaderTileMode(){return mode;}
    }
    public static final class Base {
        public float zoom=1f;public int rotation;
        public Wrapping wrapping=Wrapping.MIRRORED_REPEAT;
        public Base copy(){Base b=new Base();b.zoom=zoom;b.rotation=rotation;b.wrapping=wrapping;return b;}
        public int getRotation(){return rotation;}
        public Wrapping getWrappingType(){return wrapping;}
        @Override public boolean equals(Object other){return other instanceof Base&&((Base)other).zoom==zoom&&((Base)other).rotation==rotation;}
    }
    public Base base=new Base();public BlendParam blend=new BlendParam();public int radius=40;
    public float brightnessMinimum=0f,brightnessMaximum=1f;
    public Rect area=new Rect(0,0,240,100);
    public Base getBaseParam(){return base;}public BlendParam getBlendParam(){return blend;}public int getBlurRadius(){return radius;}
    public Rect getBlurArea(){return area;}public int getBlurType(){return 0;}
    /** Captured Kotlin parameter hash depends on mutable content. */
    @Override public int hashCode(){return 31*radius+area.left+area.top+area.right+area.bottom+blend.blendColorA;}
    public boolean isDiffBlur(BlurParam other){return radius!=other.radius||!area.equals(other.area);}
    public BlurParam detachedCopy$app_sdk_release(){BlurParam p=new BlurParam();p.base=base.copy();p.blend=blend.copy();p.radius=radius;p.brightnessMinimum=brightnessMinimum;p.brightnessMaximum=brightnessMaximum;p.area=new Rect(area.left,area.top,area.right,area.bottom);return p;}
    /** The captured ROM's 30-float transport: enableShader keeps all six
     * native-service blend slots empty while Java draws their original mix. */
    public float[] toFloatArray(boolean enableShader){
        boolean includeBlend=!enableShader;
        if(com.oplus.posteffect.util.Log.DEBUG&&com.oplus.posteffect.manager.BlurDrawableManager.sBlendTypeValue!=-1)
            includeBlend=com.oplus.posteffect.manager.BlurDrawableManager.sBlendTypeValue==2;
        float[] out=new float[30];out[1]=1;out[2]=radius;out[3]=area.left;out[4]=area.top;out[5]=area.right;out[6]=area.bottom;out[16]=base.wrapping==null?0:base.wrapping.ordinal();out[17]=includeBlend?base.zoom:1f;out[18]=base.rotation;
        if(includeBlend){out[7]=blend.blendMode;writeColor(out,8,blend.blendColorA);writeColor(out,12,blend.blendColorB);out[19]=blend.fgBlendMode;writeColor(out,20,blend.fgBlendColorA);writeColor(out,24,blend.fgBlendColorB);}
        out[28]=brightnessMinimum;out[29]=brightnessMaximum;return out;
    }
    private static void writeColor(float[] out,int start,int color){out[start]=(color>>>16&255)/255f;out[start+1]=(color>>>8&255)/255f;out[start+2]=(color&255)/255f;out[start+3]=(color>>>24)/255f;}
}
