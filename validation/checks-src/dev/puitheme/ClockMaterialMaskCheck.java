package dev.puitheme;
import android.graphics.*;
public final class ClockMaterialMaskCheck {
    private static int checks;
    private static void check(boolean yes,String message){checks++;if(!yes)throw new AssertionError(message);}
    public static void main(String[] args) {
        ClockMaterialMask mask=new ClockMaterialMask();
        RenderEffect source=RenderEffect.createBlurEffect(2,2,Shader.TileMode.CLAMP);
        check(mask.apply(null,0xffffffff,1,1)==null,"plain text cannot acquire a material effect");
        check(mask.apply(source,0xffffffff,0,1)==source,"zero mask keeps the exact existing material");
        for(int a:new int[]{0,1,64,128,255})for(float opacity:new float[]{0,.2f,.5f,1})for(float textAlpha:new float[]{0,.25f,.5f,1}) {
            int color=(a<<24)|0x336699;
            RenderEffect result=mask.apply(source,color,opacity,textAlpha);
            float w=a/255f*opacity;
            check(mask.apply(source,color,opacity,textAlpha)==result,"stable material is cached");
            if(w==0&&textAlpha==1){check(result==source,"identity does not allocate another effect");continue;}
            check(result.inner==source,"the existing material is used once without background sampling");
            ColorMatrixColorFilter filter=(ColorMatrixColorFilter)result.outer.colorFilter;
            float[] m=filter.matrix.getArray();
            check(Math.abs(m[0]-(1-w))<.00001f&&m[4]==0x33*w&&m[9]==0x66*w&&m[14]==0x99*w,"mask RGB applies once at material outlet");
            check(m[18]==textAlpha&&m[19]==0,"selected text alpha is preserved once");
        }
        mask.clear();check(mask.apply(source,0,0,1)==source,"clear cannot leave a tint on the source");
        System.out.println("ClockMaterialMaskCheck: "+checks+" checks passed (effect model, hardware pending)");
    }
}
