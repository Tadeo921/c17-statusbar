package dev.puitheme;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RenderEffect;
import android.graphics.RuntimeShader;
import android.graphics.Shader;
import android.os.Build;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import java.lang.reflect.Constructor;
import java.util.Arrays;

/** Executes the real material lifecycle and bindings against observable Android
 * fixtures. The numeric image model below is a separate compositing oracle;
 * it does not execute AGSL, Android RenderNode, a GPU or a phone screenshot. */
public final class ClockBackdropMaterialCheck {
    private static int checks;
    private static void check(boolean value,String message) {
        checks++;if(!value)throw new AssertionError(message);
    }
    private static void near(float actual,float expected,String message) {
        check(Math.abs(actual-expected)<.00001f,message+" actual="+actual+" expected="+expected);
    }
    private static final class Scene {
        final Context context=new Context();
        final FrameLayout root=new FrameLayout(context);
        final View host=new View(context);
        final TextView text=new TextView(context);
        Scene(){root.addView(host);root.addView(text);}
    }
    /** Frame has no public testing factory; do not widen production visibility. */
    private static ClockBackdropSource.Frame frame(Bitmap bitmap,Matrix matrix,View host,long revision) throws Exception {
        return frame(bitmap,matrix,host,revision,Shader.TileMode.MIRROR);
    }
    private static ClockBackdropSource.Frame frame(Bitmap bitmap,Matrix matrix,View host,long revision,Shader.TileMode mode) throws Exception {
        Constructor<ClockBackdropSource.Frame> constructor=ClockBackdropSource.Frame.class.getDeclaredConstructor(
                Bitmap.class,Matrix.class,View.class,Rect.class,long.class,Shader.TileMode.class);
        constructor.setAccessible(true);
        return constructor.newInstance(bitmap,matrix,host,new Rect(0,0,3,1),revision,mode);
    }
    private static Bitmap sample() {
        return Bitmap.createBitmap(new int[]{0xff2478d4,0xffffffff,0xfff48a24},3,1,Bitmap.Config.ARGB_8888);
    }
    private static RuntimeShader shader(View view) {
        check(view.renderEffect!=null,"an enabled material has a view effect");
        check("content".equals(view.renderEffect.inputName),"only the original glyph content is the runtime-effect child");
        check(view.renderEffect.outer==null&&view.renderEffect.inner==null&&view.renderEffect.colorFilter==null,
                "material adds no effect chain, color-filter pass or extra blur pass");
        return view.renderEffect.shader;
    }
    private static BitmapShader input(View view) {
        Shader value=shader(view).inputs.get("background");
        check(value instanceof BitmapShader,"proven source bitmap is independently bound as the background shader");
        return (BitmapShader)value;
    }
    private static float[] values(Matrix matrix) {
        float[] values=new float[9];matrix.getValues(values);return values;
    }
    private static void initialAndCache() throws Exception {
        Scene scene=new Scene();Bitmap bitmap=sample();Matrix mapping=new Matrix();mapping.setTranslate(6,8);
        scene.host.layout(30,50,33,51);scene.text.layout(10,20,110,60);
        ClockBackdropSource.Frame frame=frame(bitmap,mapping,scene.host,11);
        scene.text.setTextColor(0x12345678);
        scene.text.getPaint().setAlpha(7);
        scene.text.getPaint().setShader(new Shader());
        scene.text.getPaint().setShadowLayer(4,1,2,0xff000000);
        int allocations=Bitmap.creations,reads=Bitmap.reads,copies=Bitmap.copies,scales=Bitmap.scales;
        int blur=RenderEffect.blurCreations,chain=RenderEffect.chainCreations;
        ClockBackdropMaterial material=new ClockBackdropMaterial();
        check(material.apply(scene.text,frame,0xffffffff,0f),"zero tint still enables the unmodified background body");
        RuntimeShader shader=shader(scene.text);BitmapShader input=input(scene.text);
        check(input.bitmap==bitmap,"source bitmap is borrowed by identity rather than copied");
        check(input.tileX==Shader.TileMode.MIRROR&&input.tileY==Shader.TileMode.MIRROR,"bitmap edge sampling follows the proven native frame wrapping instead of assuming clamp");
        check(input.filterMode==BitmapShader.FILTER_MODE_LINEAR&&input.filterModeWrites==1,"low resolution native blur uses the same explicit linear filtering as ContentImage.requireShader");
        check(input.matrix!=mapping,"the native source matrix is never borrowed as mutable shader state");
        near(values(input.matrix)[2],26,"source coordinates include host-to-glyph horizontal mapping");
        near(values(input.matrix)[5],38,"source coordinates include host-to-glyph vertical mapping");
        check(Arrays.equals(values(mapping),new float[]{1,0,6,0,1,8,0,0,1}),"applying material never modifies the frame's owned mapping");
        float[] tint=shader.floats.get("tint");
        near(tint[0],1f,"default tint red");near(tint[1],1f,"default tint green");near(tint[2],1f,"default tint blue");
        near(tint[3],0f,"zero selected opacity is tint weight zero, not body opacity zero");
        check(shader.floats.size()==1&&shader.ints.isEmpty(),"base text alpha, selected text color and shadow are not material uniforms");
        check(scene.text.getCurrentTextColor()==0x12345678&&scene.text.getPaint().getAlpha()==7,
                "material application does not modify the caller's original text properties");
        check(scene.text.getPaint().shadowRadius==4&&scene.text.getPaint().getShader()!=null,
                "original paint restoration belongs to the caller; material itself neither adds nor clears a paint effect");
        int writes=scene.text.renderEffectWrites,uploads=shader.uploadCount;
        int runtimes=RuntimeShader.creations,bitmapShaders=BitmapShader.creations,effects=RenderEffect.runtimeCreations;
        RenderEffect effect=scene.text.renderEffect;
        for(int i=0;i<1000;i++)check(!material.apply(scene.text,frame,0xffffffff,0f),"stable frames report no state change");
        check(scene.text.renderEffect==effect&&scene.text.renderEffectWrites==writes,"stable state performs no view-effect writes");
        check(shader.uploadCount==uploads,"stable state performs no uniform or input uploads");
        check(RuntimeShader.creations==runtimes&&BitmapShader.creations==bitmapShaders
                &&RenderEffect.runtimeCreations==effects,"stable state creates no runtime shader, bitmap shader or render effect");
        check(Bitmap.creations==allocations&&Bitmap.reads==reads&&Bitmap.copies==copies&&Bitmap.scales==scales,
                "real apply performs no bitmap allocation, readback, copying or scaling");
        check(RenderEffect.blurCreations==blur&&RenderEffect.chainCreations==chain,"real apply never creates a blur job or effect chain");
        check(bitmap.recycleCalls==0,"native-owned bitmap is never recycled by the material");
        check(material.clear(scene.text)&&scene.text.renderEffect==null,"closing restores the original plain drawing path");
        writes=scene.text.renderEffectWrites;
        check(!material.clear(scene.text)&&scene.text.renderEffectWrites==writes,"repeated clear adds no view-effect write");
        check(bitmap.recycleCalls==0,"clearing releases references without recycling native ownership");
    }
    private static void independentViewsAndMovement() throws Exception {
        Scene scene=new Scene();TextView other=new TextView(scene.context);scene.root.addView(other);
        other.layout(40,30,140,70);
        Bitmap bitmap=sample();Matrix matrix=new Matrix();matrix.setTranslate(10,15);
        ClockBackdropMaterial first=new ClockBackdropMaterial(),second=new ClockBackdropMaterial();
        ClockBackdropSource.Frame frame=frame(bitmap,matrix,scene.host,1);
        first.apply(scene.text,frame,0xffffffff,.2f);second.apply(other,frame,0xff884422,.5f);
        BitmapShader left=input(scene.text),right=input(other);
        check(left!=right&&left.matrix!=right.matrix,"two views own separate bitmap shader and mutable mapping state");
        check(shader(scene.text)!=shader(other),"two views do not share mutable tint uniforms");
        float[] rightBefore=values(right.matrix),sourceBefore=values(matrix);
        int bitmaps=BitmapShader.creations,runtimes=RuntimeShader.creations;
        scene.text.setTranslationY(9);
        check(first.apply(scene.text,frame,0xffffffff,.2f),"view animation movement updates source alignment");
        near(values(left.matrix)[5],6,"animated glyph matrix follows the moved view");
        check(Arrays.equals(values(right.matrix),rightBefore)&&Arrays.equals(values(matrix),sourceBefore),
                "updating one view cannot shift the other view or mutate the native frame");
        check(BitmapShader.creations==bitmaps&&RuntimeShader.creations==runtimes,"matrix-only movement reuses both shader objects");
        scene.host.setTranslationX(5);
        check(first.apply(scene.text,frame,0xffffffff,.2f),"native source host movement also updates alignment");
        near(values(left.matrix)[2],15,"source host movement affects glyph-local mapping");
        matrix.postTranslate(3,2);
        check(first.apply(scene.text,frame,0xffffffff,.2f),"a new native bitmap mapping is detected without relying on revision");
        check(BitmapShader.creations==bitmaps,"native matrix updates do not allocate a replacement bitmap shader");
        int uploads=shader(scene.text).uploadCount;
        check(first.apply(scene.text,frame(bitmap,matrix,scene.host,2),0xffffffff,.2f),"same bitmap with new source revision uploads the changed source state");
        check(shader(scene.text).uploadCount>uploads&&input(scene.text)==left,"source revision keeps the same native bitmap shader");
        Bitmap next=sample();
        check(first.apply(scene.text,frame(next,matrix,scene.host,3),0xffffffff,.2f),"native image replacement is detected");
        check(input(scene.text)!=left&&input(scene.text).bitmap==next,"only an image identity replacement creates its new bitmap shader");
        check(bitmap.recycleCalls==0&&next.recycleCalls==0,"native images remain owned by their producer across replacements");
        first.clear(scene.text);second.clear(other);
    }
    private static void alphaAndBounds() throws Exception {
        Scene scene=new Scene();ClockBackdropMaterial material=new ClockBackdropMaterial();
        ClockBackdropSource.Frame frame=frame(sample(),new Matrix(),scene.host,1);
        int[] alphas={0,1,64,128,255};
        float[] amounts={-2,0,.2f,.5f,1,2,Float.NaN,Float.POSITIVE_INFINITY};
        for(int alpha:alphas)for(float amount:amounts) {
            int color=(alpha<<24)|0x2468ac;
            material.apply(scene.text,frame,color,amount);
            float[] tint=shader(scene.text).floats.get("tint");
            float strength=Float.isFinite(amount)?Math.max(0,Math.min(1,amount)):0;
            near(tint[0],0x24/255f,"custom red is straight RGB");
            near(tint[1],0x68/255f,"custom green is straight RGB");
            near(tint[2],0xac/255f,"custom blue is straight RGB");
            near(tint[3],alpha/255f*strength,"color alpha and clamped opacity multiply exactly once as tint weight");
            check(shader(scene.text).inputs.containsKey("background"),"every tint weight including zero retains the background body");
        }
        material.clear(scene.text);
    }
    private static void pendingAndFailures() throws Exception {
        Scene scene=new Scene();ClockBackdropMaterial material=new ClockBackdropMaterial();
        Bitmap bitmap=sample();ClockBackdropSource.Frame ready=frame(bitmap,new Matrix(),scene.host,1);
        material.apply(scene.text,ready,0xffffffff,0f);
        RuntimeShader body=shader(scene.text);
        check(material.apply(scene.text,null,0x00ffffff,0f),"source loss switches the active material to pending");
        RuntimeShader pending=shader(scene.text);
        check(pending!=body&&pending.inputs.isEmpty(),"pending cannot sample an old or missing source image");
        check(pending.source.contains("float a=float(content.eval(p).a);")
                &&!pending.source.contains("content.eval(p).a)*tint.a"),
                "pending fallback keeps full glyph coverage even with zero tint alpha/opacity");
        check(pending.floats.isEmpty()&&!pending.source.contains("tint"),"pending program has no unused tint uniform or upload");
        check(pending.source.contains("return half4(half3(a),half(a));"),"unknown source uses white opaque glyph coverage rather than a possibly black or transparent custom mask");
        material.apply(scene.text,null,0x00000000,0f);
        check(shader(scene.text)==pending&&pending.floats.isEmpty(),"zero transparent-black mask also keeps the readable white pending fallback");
        int writes=scene.text.renderEffectWrites;
        check(!material.apply(scene.text,null,0x00000000,0f)&&scene.text.renderEffectWrites==writes,
                "stable pending state has no effect writes");
        check(material.apply(scene.text,ready,0xffffffff,0f)&&shader(scene.text)==body,"ready after pending resumes the real background program");
        View foreignHost=new View(new Context());
        check(material.apply(scene.text,frame(bitmap,new Matrix(),foreignHost,2),0xffffffff,.2f)
                &&shader(scene.text).inputs.isEmpty(),"a foreign window/root is treated as unavailable, never sampled");
        Bitmap recycled=sample();recycled.recycle();
        material.apply(scene.text,frame(recycled,new Matrix(),scene.host,3),0xffffffff,.3f);
        check(shader(scene.text).inputs.isEmpty()&&recycled.recycleCalls==1,"recycled native image falls back and is not recycled again");
        material.apply(scene.text,ready,0xffffffff,.2f);
        Matrix invalid=new Matrix();invalid.postTranslate(Float.NaN,0);
        check(material.apply(scene.text,frame(bitmap,invalid,scene.host,4),0xffffffff,.2f)
                &&scene.text.renderEffect==null,"invalid geometry clears the effect to a readable caller-owned text path");
        check(material.apply(scene.text,ready,0xffffffff,.2f),"valid source can recover after invalid geometry");
        RenderEffect.fail=true;
        try {check(!material.apply(scene.text,ready,0xffffffff,.4f),"unavailable runtime effect fails without propagating to SystemUI");}
        finally {RenderEffect.fail=false;}
        check(scene.text.renderEffect==null,"runtime failure restores caller text instead of retaining a stale background");
        writes=scene.text.renderEffectWrites;
        check(!material.apply(scene.text,ready,0xffffffff,.2f)&&scene.text.renderEffectWrites==writes,
                "failed instance stops retrying shader work on animation frames");
        check(bitmap.recycleCalls==0,"pending, invalid geometry and runtime failure never recycle the native image");
        int sdk=Build.VERSION.SDK_INT;
        try {
            Build.VERSION.SDK_INT=32;ClockBackdropMaterial unsupported=new ClockBackdropMaterial();
            writes=scene.text.renderEffectWrites;
            check(!unsupported.apply(scene.text,ready,0xffffffff,.2f)&&scene.text.renderEffectWrites==writes,
                    "unsupported SDK keeps plain caller text without creating an effect");
        } finally {Build.VERSION.SDK_INT=sdk;}
        ClockBackdropMaterial migrating=new ClockBackdropMaterial();TextView replacement=new TextView(scene.context);scene.root.addView(replacement);
        migrating.apply(scene.text,ready,0xffffffff,.2f);
        check(migrating.apply(replacement,ready,0xffffffff,.2f)&&scene.text.renderEffect==null&&replacement.renderEffect!=null,
                "moving an instance to another view removes its previous owner's effect");
        migrating.clear(replacement);
        check(replacement.renderEffect==null,"clear restores the replacement owner too");
    }
    /** Independent premultiplied source-over model. The real shader's contract
     * and bound uniforms are checked separately; this is not a GPU interpreter. */
    private static float[] compose(int background,float[] tint,float coverage,int oldBackdrop) {
        float weight=tint[3];float[] result=new float[4];
        for(int channel=0;channel<3;channel++) {
            int shift=(2-channel)*8;
            float clean=((background>>>shift)&255)/255f;
            float rgb=clean*(1-weight)+tint[channel]*weight;
            float underneath=((oldBackdrop>>>shift)&255)/255f;
            result[channel]=rgb*coverage+underneath*(1-coverage);
        }
        result[3]=coverage;return result;
    }
    private static void imageCompositionModel() throws Exception {
        Scene scene=new Scene();Bitmap bitmap=sample();ClockBackdropMaterial material=new ClockBackdropMaterial();
        ClockBackdropSource.Frame frame=frame(bitmap,new Matrix(),scene.host,1);
        material.apply(scene.text,frame,0xffffffff,0f);RuntimeShader shader=shader(scene.text);
        check(shader.source.contains("float a=float(glyph.a);")
                &&shader.source.contains("mix(base,tint.rgb,tint.a)")
                &&shader.source.contains("half3(rgb*a),half(a)"),
                "actual body program separates tint interpolation from the one glyph coverage multiplication");
        check(!shader.source.contains("glyph.rgb")&&!shader.source.contains("shadow")
                &&!shader.source.contains("blur(")&&!shader.source.contains("baseText"),
                "body uses clean background RGB rather than old text, shadow, base-text alpha or a second blur");
        check(shader.source.contains("float3 base=float3(bg.rgb);")&&!shader.source.contains("/float(bg.a)")
                &&!shader.source.contains("bg.a>"),"known native image RGB follows the original AGSL program without a second unpremultiply or synthetic white alpha fallback");
        float[] tint=shader.floats.get("tint");
        for(int pixel:bitmap.pixels) {
            float[] output=compose(pixel,tint,1,0xff000000);
            near(output[0],((pixel>>>16)&255)/255f,"zero tint reveals clean source red");
            near(output[1],((pixel>>>8)&255)/255f,"zero tint reveals clean source green");
            near(output[2],(pixel&255)/255f,"zero tint reveals clean source blue");
            near(output[3],1,"zero tint still has an opaque glyph body over black scrim");
        }
        check(bitmap.pixels[0]!=bitmap.pixels[1]&&bitmap.pixels[1]!=bitmap.pixels[2],
                "blue, white and orange native regions stay distinct instead of one white text interpolation");
        material.apply(scene.text,frame,0x80ffffff,.4f);tint=shader(scene.text).floats.get("tint");
        for(float aa:new float[]{0,.125f,.5f,.875f,1})for(int pixel:bitmap.pixels) {
            float[] black=compose(pixel,tint,aa,0xff000000),white=compose(pixel,tint,aa,0xffffffff);
            near(black[3],aa,"antialias coverage is independent of mask weight");
            for(int c=0;c<3;c++) {
                near(white[c]-black[c],1-aa,"backdrop contributes only outside the glyph's AA coverage");
                float clean=((pixel>>>((2-c)*8))&255)/255f;
                near(black[c],(clean*(1-tint[3])+tint[c]*tint[3])*aa,
                        "black scrim does not re-enter the opaque body through tint opacity");
            }
        }
        material.clear(scene.text);
    }
    private static void nativeWrapping() throws Exception {
        Scene scene=new Scene();ClockBackdropMaterial material=new ClockBackdropMaterial();Bitmap bitmap=sample();Matrix matrix=new Matrix();
        long revision=20;
        for(Shader.TileMode mode:new Shader.TileMode[]{Shader.TileMode.CLAMP,Shader.TileMode.MIRROR,Shader.TileMode.REPEAT,Shader.TileMode.DECAL}) {
            ClockBackdropSource.Frame next=frame(bitmap,matrix,scene.host,revision++,mode);
            check(material.apply(scene.text,next,0xffffffff,.2f),"native wrapping change updates the material");
            BitmapShader actual=input(scene.text);
            check(actual.tileX==mode&&actual.tileY==mode,"native frame wrapping reaches both source axes without hardcoding");
            check(actual.filterMode==BitmapShader.FILTER_MODE_LINEAR,"native wrapping retains explicit linear filtering");
            int shaders=BitmapShader.creations;
            check(material.apply(scene.text,frame(bitmap,matrix,scene.host,revision++,mode),0xffffffff,.2f),"frame geometry revision can update with stable wrapping");
            check(input(scene.text)==actual&&BitmapShader.creations==shaders,"geometry revision without a new content delivery reuses the independent sampler");
        }
        check(bitmap.recycleCalls==0,"changing native wrap state never recycles the borrowed bitmap");
        material.clear(scene.text);
    }
    private static void sourceRetention() throws Exception {
        java.lang.reflect.Field retained=ClockBackdropMaterial.class.getDeclaredField("retained");retained.setAccessible(true);
        java.lang.reflect.Field owner=ClockBackdropMaterial.class.getDeclaredField("owner");owner.setAccessible(true);
        Scene scene=new Scene();Bitmap bitmap=sample();Matrix matrix=new Matrix();
        ClockBackdropSource.Frame frame=frame(bitmap,matrix,scene.host,1);
        ClockBackdropMaterial clock=new ClockBackdropMaterial();clock.apply(scene.text,frame,0xffffffff,.2f);
        check(retained.get(clock)==frame,"default clock ownership keeps the immutable shared source frame cached");
        clock.clear(scene.text);check(retained.get(clock)==null,"clearing default clock ownership releases its source frame");
        ClockBackdropMaterial explicit=new ClockBackdropMaterial(true);explicit.apply(scene.text,frame,0xffffffff,.2f);
        check(retained.get(explicit)==frame,"explicit retained source matches the default clock constructor");explicit.clear(scene.text);
        ClockBackdropMaterial notification=new ClockBackdropMaterial(false);notification.apply(scene.text,frame,0xffffffff,.2f);
        check(retained.get(notification)==null,"weak-key notification ownership never retains Frame.host or its window tree");
        check(input(scene.text).bitmap==bitmap,"nonretaining ownership still borrows the existing bitmap without copying");
        check(owner.get(notification) instanceof java.lang.ref.WeakReference
                &&((java.lang.ref.WeakReference<?>)owner.get(notification)).get()==scene.text,"notification material stores its drawing owner only weakly");
        int writes=scene.text.renderEffectWrites,shaders=BitmapShader.creations,programs=RuntimeShader.creations;
        check(!notification.apply(scene.text,frame,0xffffffff,.2f)&&scene.text.renderEffectWrites==writes,
                "nonretaining ownership preserves stable-state effect caching");
        scene.text.setTranslationX(3);notification.apply(scene.text,frame,0xffffffff,.2f);
        check(retained.get(notification)==null&&BitmapShader.creations==shaders&&RuntimeShader.creations==programs,
                "geometry movement updates the borrowed shader without capturing a host-bearing frame");
        notification.apply(scene.text,null,0xffffffff,.2f);notification.apply(scene.text,frame,0xffffffff,.2f);
        check(retained.get(notification)==null,"pending-to-ready recovery preserves nonretaining ownership");
        notification.clear(scene.text);
        check(retained.get(notification)==null&&scene.text.renderEffect==null,"clearing a notification material leaves no source frame or owned view effect");
        check(bitmap.recycleCalls==0,"neither retention policy recycles the source producer's bitmap");
    }
    /** The ROM reuses HardwareBuffer-backed Bitmap identity but gives each
     * native delivery a new ContentImage and independent BitmapShader. */
    private static void deliveredContentAndGeometry() throws Exception {
        try(ClockBackdropCheckScene source=new ClockBackdropCheckScene()) {
            TextView text=new TextView(null);source.root.addView(text);ClockBackdropMaterial material=new ClockBackdropMaterial();
            ClockBackdropSource.Frame initial=ClockBackdropSource.snapshot(source.root);
            material.apply(text,initial,0xffffffff,.2f);BitmapShader first=input(text);RuntimeShader program=shader(text);
            source.engine.completeForCheck(source.bitmap);source.engine.preDrawContent();ClockBackdropSource.onDrawn(source.engine);
            ClockBackdropSource.Frame delivered=ClockBackdropSource.snapshot(source.root);
            check(delivered!=initial&&delivered.bitmap==initial.bitmap,"real native delivery can replace ImageInfo while keeping the same Bitmap");
            check(delivered.contentRevision>initial.contentRevision,"real promotion marks new image content separately from geometry");
            material.apply(text,delivered,0xffffffff,.2f);BitmapShader fresh=input(text);
            check(fresh!=first,"new native content delivery must refresh its independent sampler even when Bitmap identity is reused");
            check(shader(text)==program,"content refresh does not compile a new glyph program");
            source.engine.bitmapMatrix.postTranslate(2,0);ClockBackdropSource.onDrawn(source.engine);
            material.apply(text,ClockBackdropSource.snapshot(source.root),0xffffffff,.2f);
            check(input(text)==fresh,"geometry-only native movement must reuse the refreshed sampler");
            source.bitmap.eraseColor(0xff7a4f2b);ClockBackdropSource.onDrawn(source.engine);
            material.apply(text,ClockBackdropSource.snapshot(source.root),0xffffffff,.2f);
            check(input(text)!=fresh,"mutable content generation also refreshes its sampler without waiting for another wrapper");
            check(shader(text)==program,"mutable generation still reuses the same glyph program");
            java.util.Map<String,Object> diagnostics=ClockBackdropMaterial.diagnostics();
            check(diagnostics.size()==20&&((Number)diagnostics.get("sampler_rebuilds")).longValue()>=3,
                    "verified probe reports bounded sampler refresh metadata");
            for(Object value:diagnostics.values())check(value instanceof Number||value instanceof String,"material diagnostics contain no view, image, pixel or text");
            material.clear(text);
        }
    }
    public static void main(String[] args) throws Exception {
        deliveredContentAndGeometry();
        int sdk=Build.VERSION.SDK_INT;Build.VERSION.SDK_INT=35;
        try {initialAndCache();independentViewsAndMovement();alphaAndBounds();pendingAndFailures();imageCompositionModel();nativeWrapping();sourceRetention();}
        finally {Build.VERSION.SDK_INT=sdk;RenderEffect.fail=false;}
        System.out.println("ClockBackdropMaterialCheck: "+checks+" checks passed (real apply/bindings + CPU compositing model; GPU/device pending)");
    }
}
