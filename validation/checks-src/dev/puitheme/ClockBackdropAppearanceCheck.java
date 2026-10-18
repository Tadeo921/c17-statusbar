package dev.puitheme;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RenderEffect;
import android.graphics.RuntimeShader;
import android.graphics.Shader;
import android.view.View;
import android.widget.FrameLayout;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

/** Real apply/uniform/cache checks plus an independent analytic square-glyph
 * compositing model. This does not execute AGSL or establish device GPU quality. */
public final class ClockBackdropAppearanceCheck {
    private static int checks;
    private static Class<?> appearance;
    private static Constructor<?> constructor;
    private static Method apply;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static void near(float actual,float expected,String message){check(Math.abs(actual-expected)<.00002f,message+" actual="+actual+" expected="+expected);}
    private static Object value(Object owner,String name) throws Exception {Field f=appearance.getDeclaredField(name);f.setAccessible(true);return f.get(owner);}
    private static Object look(boolean border,float width,int color,float opacity,float angle,float high,float shade,float relief) throws Exception {
        return constructor.newInstance(border,width,color,opacity,angle,high,shade,relief);
    }
    private static boolean apply(ClockBackdropMaterial material,View view,ClockBackdropSource.Frame frame,Object look) throws Exception {
        return (Boolean)apply.invoke(material,view,frame,0xffffffff,.2f,look);
    }
    private static final class Scene {
        final FrameLayout root=new FrameLayout(null);
        final View host=new View(null),text=new View(null);
        final Bitmap bitmap=Bitmap.createBitmap(new int[]{0xff2478d4,0xffffffff,0xfff48a24},3,1,Bitmap.Config.ARGB_8888);
        final ClockBackdropSource.Frame frame;
        Scene() throws Exception {
            root.addView(host);root.addView(text);
            Constructor<ClockBackdropSource.Frame> c=ClockBackdropSource.Frame.class.getDeclaredConstructor(
                    Bitmap.class,Matrix.class,View.class,Rect.class,long.class,Shader.TileMode.class);c.setAccessible(true);
            frame=c.newInstance(bitmap,new Matrix(),host,new Rect(0,0,100,100),1L,Shader.TileMode.MIRROR);
        }
        RuntimeShader shader(){return text.renderEffect.shader;}
    }
    private static void initialize() throws Exception {
        try {appearance=Class.forName("dev.puitheme.ClockBackdropMaterial$Appearance");}
        catch(ClassNotFoundException missing){throw new AssertionError("material has no configurable inward edge appearance; real apply cannot yet bind border or relief",missing);}
        constructor=appearance.getDeclaredConstructor(boolean.class,float.class,int.class,float.class,float.class,float.class,float.class,float.class);
        constructor.setAccessible(true);
        apply=ClockBackdropMaterial.class.getDeclaredMethod("apply",View.class,ClockBackdropSource.Frame.class,int.class,float.class,appearance);
        apply.setAccessible(true);
    }
    private static void defaultsAndBounds() throws Exception {
        Constructor<?> defaults=appearance.getDeclaredConstructor();defaults.setAccessible(true);Object chosen=defaults.newInstance();
        check(Boolean.TRUE.equals(value(chosen,"borderEnabled")),"readability border defaults on");
        near((Float)value(chosen,"borderWidthPx"),.6f,"default density-one border is .6 pixels");
        check((Integer)value(chosen,"borderColor")==0xffffffff,"default inner border is white");
        near((Float)value(chosen,"borderOpacity"),.6f,"default border opacity");
        near((Float)value(chosen,"angleDeg"),315f,"default light angle");
        near((Float)value(chosen,"highlight"),.4f,"default highlight strength");
        near((Float)value(chosen,"shade"),0f,"default dark-edge strength is zero");
        near((Float)value(chosen,"reliefWidthPx"),1.5f,"default density-one relief width");
        Object invalid=look(true,Float.POSITIVE_INFINITY,0x80336699,Float.NaN,Float.NaN,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY,-8);
        near((Float)value(invalid,"borderWidthPx"),0,"nonfinite width is inactive");near((Float)value(invalid,"reliefWidthPx"),0,"negative relief width clips to zero");
        near((Float)value(invalid,"borderOpacity"),0,"nonfinite border opacity is inactive");near((Float)value(invalid,"highlight"),0,"nonfinite highlight is inactive");
        near((Float)value(invalid,"shade"),0,"nonfinite shade is inactive");near((Float)value(invalid,"angleDeg"),315,"invalid angle retains the documented direction");
        Object bounded=look(true,99,0xffffffff,2,-45,2,-1,99);
        near((Float)value(bounded,"borderWidthPx"),32,"border width is bounded before GPU sampling");
        near((Float)value(bounded,"reliefWidthPx"),32,"relief width is bounded before GPU sampling");
        near((Float)value(bounded,"borderOpacity"),1,"border opacity clamps to one");near((Float)value(bounded,"highlight"),1,"highlight clamps to one");
        near((Float)value(bounded,"shade"),0,"negative shade is inactive");near((Float)value(bounded,"angleDeg"),315,"equivalent negative angles normalize");
        Object equal=look(true,32,0xffffffff,1,675,1,0,32);
        check(bounded.equals(equal)&&equal.equals(bounded)&&bounded.hashCode()==equal.hashCode(),"normalised appearance uses value equality and a matching hash code");
        check(!bounded.equals(look(false,32,0xffffffff,1,315,1,0,32)),"border enablement is part of appearance identity");
        for(Field field:appearance.getDeclaredFields())if(!Modifier.isStatic(field.getModifiers()))
            check(Modifier.isFinal(field.getModifiers())&&field.getType().isPrimitive(),"appearance is immutable primitive data, retaining no renderer or view");
    }
    private static void bindingsAndCache() throws Exception {
        Scene scene=new Scene();ClockBackdropMaterial material=new ClockBackdropMaterial();
        int reads=Bitmap.reads,copies=Bitmap.copies,bitmaps=Bitmap.creations,blur=RenderEffect.blurCreations,chains=RenderEffect.chainCreations;
        check(apply(material,scene.text,scene.frame,null),"null new appearance retains the original flat material");
        RuntimeShader shader=scene.shader();check(shader.floats.size()==1&&shader.floats.containsKey("tint"),"NONE has exactly the old single tint upload");
        check(!material.apply(scene.text,scene.frame,0xffffffff,.2f),"old four-argument apply delegates to the same cached NONE state");
        int shaders=RuntimeShader.creations,inputs=BitmapShader.creations;
        Object chosen=look(true,2,0x80336699,.25f,90,.4f,0,3);
        check(apply(material,scene.text,scene.frame,chosen),"enhanced appearance changes actual material state");
        check(scene.shader()==shader,"appearance uses the original single-pass runtime program");
        float[] border=shader.floats.get("edgeColor"),light=shader.floats.get("relief"),widths=shader.floats.get("edgeWidths");
        near(border[0],.2f,"border red");near(border[1],.4f,"border green");near(border[2],.6f,"border blue");near(border[3],128/255f*.25f,"border color alpha and border opacity multiply once");
        near(light[0],0,"90 degrees has no horizontal light component");near(light[1],-1,"90 degrees points upwards in screen coordinates");
        near(light[2],.4f,"highlight uniform");near(light[3],0,"default dark-side uniform is zero");near(widths[0],2,"border width stays in caller-supplied pixels");near(widths[1],3,"relief width stays in pixels");
        int writes=scene.text.renderEffectWrites,uploads=shader.uploadCount,effects=RenderEffect.runtimeCreations;
        for(int i=0;i<1000;i++)check(!apply(material,scene.text,scene.frame,look(true,2,0x80336699,.25f,90,.4f,0,3)),"equivalent appearance objects do not rebuild stable frames");
        check(scene.text.renderEffectWrites==writes&&shader.uploadCount==uploads&&RenderEffect.runtimeCreations==effects,"stable enhanced state does no effect writes or uploads");
        check(RuntimeShader.creations==shaders&&BitmapShader.creations==inputs,"changing or repeating appearance creates neither extra runtime program nor background sampler");
        check(Bitmap.reads==reads&&Bitmap.copies==copies&&Bitmap.creations==bitmaps&&RenderEffect.blurCreations==blur&&RenderEffect.chainCreations==chains,
                "edge enhancement creates no bitmap, readback, copy, extra blur job or effect chain");
        check(apply(material,scene.text,scene.frame,null),"returning to NONE restores original body appearance");
        near(shader.floats.get("edgeColor")[3],0,"NONE resets the previous border weight");near(shader.floats.get("relief")[2],0,"NONE resets highlight");near(shader.floats.get("relief")[3],0,"NONE resets shade");
        check(!material.apply(scene.text,scene.frame,0xffffffff,.2f),"legacy delegate remains stable after enhancement reset");
        apply(material,scene.text,scene.frame,chosen);apply(material,scene.text,null,chosen);apply(material,scene.text,null,null);apply(material,scene.text,scene.frame,null);
        near(scene.shader().floats.get("edgeColor")[3],0,"enhanced-ready through pending then NONE cannot leak the old border on resume");
        near(scene.shader().floats.get("relief")[2],0,"pending-to-ready reset cannot leak old highlight");
        check(scene.bitmap.recycleCalls==0,"enhancement never takes ownership of the native image");material.clear(scene.text);
        check(scene.text.renderEffect==null,"closing enhancement removes its single owned effect");
    }
    private static float coverage(float x,float y) {
        return Math.max(0,Math.min(1,Math.min(Math.min(x+.5f,100.5f-x),Math.min(y+.5f,100.5f-y))));
    }
    /** Analytic square-glyph reference driven by the real bound uniforms. */
    private static float[] pixel(RuntimeShader shader,float x,float y,float[] body) {
        float a=coverage(x,y);float[] rgb=body.clone();
        float[] light=shader.floats.get("relief"),border=shader.floats.get("edgeColor"),width=shader.floats.get("edgeWidths");
        if(light!=null&&width!=null&&width[1]>0) {
            float along=coverage(x-light[0]*width[1],y-light[1]*width[1])-coverage(x+light[0]*width[1],y+light[1]*width[1]);
            for(int c=0;c<3;c++)rgb[c]=along>0?rgb[c]+(1-rgb[c])*along*light[2]:rgb[c]*(1+along*light[3]);
        }
        if(border!=null&&width!=null&&width[0]>0) {
            float distance=1-Math.min(Math.min(coverage(x-width[0],y),coverage(x+width[0],y)),
                    Math.min(coverage(x,y-width[0]),coverage(x,y+width[0])));
            float strength=distance*border[3];for(int c=0;c<3;c++)rgb[c]=rgb[c]+(border[c]-rgb[c])*strength;
        }
        return new float[]{rgb[0]*a,rgb[1]*a,rgb[2]*a,a};
    }
    private static void directionalModel() throws Exception {
        Scene scene=new Scene();ClockBackdropMaterial material=new ClockBackdropMaterial();float[] blue={.12f,.38f,.72f};
        float[][] points={{100,50},{50,0},{0,50},{50,100}};float[] angles={0,90,180,270};
        for(int i=0;i<angles.length;i++) {
            apply(material,scene.text,scene.frame,look(false,0,0xffffffff,0,angles[i],.4f,0,2));RuntimeShader shader=scene.shader();
            float[] lit=pixel(shader,points[i][0],points[i][1],blue),back=pixel(shader,points[(i+2)%4][0],points[(i+2)%4][1],blue);
            near(lit[3],.5f,"highlight retains the original antialias coverage");
            for(int c=0;c<3;c++){near(lit[c],(blue[c]+(1-blue[c])*.4f)*.5f,"selected light direction only brightens the facing inner edge");near(back[c],blue[c]*.5f,"shade zero never dims the opposite edge");}
            float[] center=pixel(shader,50,50,blue);for(int c=0;c<3;c++)near(center[c],blue[c],"glyph interior keeps the clean background material");
        }
        apply(material,scene.text,scene.frame,look(false,0,0xffffffff,0,0,0,.5f,2));
        float[] shaded=pixel(scene.shader(),0,50,blue),facing=pixel(scene.shader(),100,50,blue);
        for(int c=0;c<3;c++){near(shaded[c],blue[c]*.5f*.5f,"explicit dark-side strength changes only the back-facing edge");near(facing[c],blue[c]*.5f,"shade cannot darken the facing edge or the whole glyph");}
        apply(material,scene.text,scene.frame,look(true,2,0xffff0000,.6f,315,0,0,2));
        float[] edge=pixel(scene.shader(),100,50,blue),inside=pixel(scene.shader(),50,50,blue);
        near(edge[0],(blue[0]+(1-blue[0])*.6f)*.5f,"custom border color is mixed only inside glyph coverage");
        near(edge[1],blue[1]*.4f*.5f,"custom border green");near(edge[2],blue[2]*.4f*.5f,"custom border blue");
        check(Arrays.equals(inside,new float[]{blue[0],blue[1],blue[2],1}),"border never colors the interior background");
        Constructor<?> defaults=appearance.getDeclaredConstructor();defaults.setAccessible(true);apply(material,scene.text,scene.frame,defaults.newInstance());
        for(float x:new float[]{-10,-1,0,.25f,1,2,50,98,99,99.75f,100,101,110})for(float y:new float[]{-1,0,1,50,99,100,101}) {
            float[] actual=pixel(scene.shader(),x,y,blue);near(actual[3],coverage(x,y),"enhancement never expands the original glyph domain");
            for(int c=0;c<3;c++)check(actual[c]+.00001f>=blue[c]*actual[3],"default white border/highlight and shade zero cannot reintroduce black dimming");
            if(coverage(x,y)==0)check(actual[0]==0&&actual[1]==0&&actual[2]==0,"fully outside pixels remain transparent despite nearby edge samples");
        }
        String source=scene.shader().source;
        check(!source.contains("for(")&&!source.contains("for (")&&!source.contains("while"),"edge program has no sampling kernel loops");
        int taps=0,at=-1;while((at=source.indexOf("content.eval(",at+1))>=0)taps++;
        check(taps<=7,"single pass uses at most one center plus six fixed alpha samples");
        check(source.contains("if(edgeColor.a>0.0)")&&source.contains("if(relief.z>0.0||relief.w>0.0)"),
                "flat/disabled controls skip their neighbour samples in the actual AGSL program");
        material.clear(scene.text);
    }
    public static void main(String[] args) throws Exception {
        initialize();defaultsAndBounds();bindingsAndCache();directionalModel();
        System.out.println("ClockBackdropAppearanceCheck: "+checks+" checks passed (actual material bindings + analytic glyph model; AGSL/GPU/device pending)");
    }
}
