package android.graphics;
public class RenderEffect {
    public static boolean fail;
    public static int runtimeCreations,blurCreations,chainCreations,colorCreations;
    public final float radius;
    public final Shader.TileMode tileMode;
    public RuntimeShader shader;
    public String inputName;
    public RenderEffect outer,inner;
    public ColorFilter colorFilter;
    public static RenderEffect createColorFilterEffect(ColorFilter filter) {
        if(fail)throw new IllegalStateException("color effect unavailable");
        colorCreations++;RenderEffect effect=new RenderEffect(0f,null);effect.colorFilter=filter;return effect;
    }
    private RenderEffect(float radius,Shader.TileMode mode) {this.radius=radius;tileMode=mode;}
    public static RenderEffect createBlurEffect(float x,float y,Shader.TileMode mode) {
        if(fail)throw new IllegalStateException("blur unavailable");blurCreations++;return new RenderEffect(x,mode);
    }
    public static RenderEffect createRuntimeShaderEffect(RuntimeShader shader,String name) {
        if(fail)throw new IllegalStateException("shader effect unavailable");
        runtimeCreations++;RenderEffect effect=new RenderEffect(0f,null);effect.shader=shader;effect.inputName=name;return effect;
    }
    public static RenderEffect createChainEffect(RenderEffect outer,RenderEffect inner) {
        if(fail)throw new IllegalStateException("effect chain unavailable");
        chainCreations++;RenderEffect effect=new RenderEffect(0f,null);effect.outer=outer;effect.inner=inner;return effect;
    }
}
