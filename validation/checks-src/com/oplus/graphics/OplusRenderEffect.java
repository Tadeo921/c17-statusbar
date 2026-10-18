package com.oplus.graphics;
/** Audited OEM gradient signature and inputs, with synthetic RenderEffect objects. */
public final class OplusRenderEffect {
    public static int created;
    public static float start,end,speed;
    public static boolean vertical;
    public static final java.util.List<float[]> gradientsForCheck=new java.util.ArrayList<>();
    public static android.graphics.RenderEffect createGradientBlurEffect(float from,float to,boolean axis,float velocity){
        start=from;end=to;speed=velocity;vertical=axis;created++;gradientsForCheck.add(new float[]{from,to,velocity});
        return android.graphics.RenderEffect.createBlurEffect(from,to,android.graphics.Shader.TileMode.CLAMP);
    }
    public static android.graphics.RenderEffect createGradientBlurEffect(float from,float to,boolean axis,float velocity,int blend,int blendC,int mixC){
        if(blend!=1||blendC!=0||mixC!=0)throw new AssertionError("native material arguments changed");
        start=from;end=to;speed=velocity;vertical=axis;created++;
        return android.graphics.RenderEffect.createBlurEffect(from,to,android.graphics.Shader.TileMode.CLAMP);
    }
}
