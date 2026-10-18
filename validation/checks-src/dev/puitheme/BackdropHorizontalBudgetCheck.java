package dev.puitheme;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Matrix;
import android.graphics.RenderEffect;
import android.graphics.RuntimeShader;
import android.graphics.Shader;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.android.systemui.scrim.ScrimView;
import com.android.systemui.statusbar.notification.row.ExpandableNotificationRow;
import com.android.systemui.statusbar.notification.row.NotificationContentView;
import com.oplus.posteffect.BlurDrawable;
import com.oplusos.systemui.common.blurability.platformblur.PlatformBlurDrawable;
import com.oplusos.systemui.common.blurability.platformblur.PlatformBlurHelper;
import java.lang.reflect.Field;

/** Clock-owned source/material movement work counts, rather than a device FPS
 * estimate. No notification manager or window traversal observer is used. */
public final class BackdropHorizontalBudgetCheck {
    private static final int TEXTS=12,FRAMES=120;
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static final class Scene {
        final ClockBackdropCheckScene source=new ClockBackdropCheckScene();
        final FrameLayout root=source.root;
        final FrameLayout row=new FrameLayout(null);
        final TextView[] texts=new TextView[TEXTS];
        final ClockBackdropMaterial[] materials=new ClockBackdropMaterial[TEXTS];
        final BlurDrawable engine=source.engine;
        final Bitmap bitmap=source.bitmap;
        final ClockBackdropSource.Frame held=source.held;
        final ClockBackdropMaterial.Appearance look=new ClockBackdropMaterial.Appearance();
        Scene() {
            root.addView(row);check(held!=null,"baseline uses a real proven native source, as the visible clock would retain it");
            for(int i=0;i<TEXTS;i++){texts[i]=new TextView(null);texts[i].setText("时钟字形 "+i);row.addView(texts[i]);materials[i]=new ClockBackdropMaterial();}
            updateMaterials();
        }
        void updateMaterials(){ClockBackdropSource.Frame frame=ClockBackdropSource.snapshot(root);for(int i=0;i<TEXTS;i++)materials[i].apply(texts[i],frame,0xffffffff,.2f,look);}
        void close(){for(int i=0;i<TEXTS;i++)materials[i].clear(texts[i]);source.close();}
    }
    private static final class Counts {
        int effects,tint,uploads,ownerChanges,inversions,nativeReads,samplers,programs,bitmaps,pixels;
        @Override public String toString(){return "effects="+effects+",tintUploads="+tint+",allUploads="+uploads+",observedOwnerReplacements="+ownerChanges
                +",sourceInversions="+inversions+",nativeGetterReads="+nativeReads+",newSamplers="+samplers+",newPrograms="+programs+",newBitmaps="+bitmaps+",pixelReads="+pixels;}
    }
    private static int total(Scene scene,boolean tint) {
        int result=0;for(TextView text:scene.texts){RuntimeShader shader=text.renderEffect.shader;
            result+=tint?shader.floatWritesForCheck.getOrDefault("tint",0):shader.uploadCount;}return result;
    }
    private static int nativeReads(Scene scene){return scene.engine.paramReadsForCheck+scene.engine.imageReadsForCheck+scene.engine.matrixReadsForCheck+scene.engine.enabledReadsForCheck;}
    private static Counts replay(boolean nativeChanges) throws Exception {
        Scene s=new Scene();ClockBackdropMaterial[] materials=s.materials;Object[] owners=new Object[TEXTS];
        Field owner=ClockBackdropMaterial.class.getDeclaredField("owner");owner.setAccessible(true);
        for(int i=0;i<TEXTS;i++)owners[i]=owner.get(materials[i]);
        Counts c=new Counts();int effects=RenderEffect.runtimeCreations,tint=total(s,true),uploads=total(s,false),inversions=s.engine.bitmapMatrix.inversionsForCheck,reads=nativeReads(s);
        int samplers=BitmapShader.creations,programs=RuntimeShader.creations,bitmaps=Bitmap.creations,pixels=Bitmap.reads;
        for(int frame=1;frame<=FRAMES;frame++) {
            if(nativeChanges)s.engine.bitmapMatrix.postTranslate(.25f,0);
            s.engine.preDrawContent();ClockBackdropSource.onDrawn(s.engine);
            // Apply clock-owned materials once at the final geometry boundary.
            // There is no notification preDraw or posted refresh path anymore.
            s.row.setTranslationX(frame*2f);s.updateMaterials();
            for(int i=0;i<TEXTS;i++){Object next=owner.get(materials[i]);if(next!=owners[i])c.ownerChanges++;owners[i]=next;}
        }
        c.effects=RenderEffect.runtimeCreations-effects;c.tint=total(s,true)-tint;c.uploads=total(s,false)-uploads;
        c.inversions=s.engine.bitmapMatrix.inversionsForCheck-inversions;c.nativeReads=nativeReads(s)-reads;
        c.samplers=BitmapShader.creations-samplers;c.programs=RuntimeShader.creations-programs;c.bitmaps=Bitmap.creations-bitmaps;c.pixels=Bitmap.reads-pixels;
        for(TextView text:s.texts) {
            check(text.renderEffect!=null&&text.renderEffect.shader.floats.get("edgeColor")[3]>.5f,"appearance and actual backdrop stay enabled throughout horizontal movement");
            check(((BitmapShader)text.renderEffect.shader.inputs.get("background")).bitmap==s.bitmap,"moving text keeps the real native image");
        }
        float[] matrix=new float[9];((BitmapShader)s.texts[0].renderEffect.shader.inputs.get("background")).matrix.getValues(matrix);
        check(Math.abs(matrix[2]-((nativeChanges?FRAMES*.25f:0)-FRAMES*2))<.001f,"final sampling tracks both source movement and the native row translation");
        check(c.samplers==0&&c.programs==0&&c.bitmaps==0&&c.pixels==0,"replay never replaces native animation with bitmap/blur preparation");
        s.close();return c;
    }
    private static void tintCacheBoundaries() {
        Scene s=new Scene();View target=new View(null);s.root.addView(target);ClockBackdropMaterial material=new ClockBackdropMaterial(false);
        material.apply(target,s.held,0x80ffffff,.25f);RuntimeShader first=target.renderEffect.shader;
        check(first.floatWritesForCheck.get("tint")==1,"a fresh body initializes tint once");
        material.apply(target,null,0xffff0000,.5f);
        check(target.renderEffect.shader.floats.isEmpty(),"pending fallback never receives tint uniforms");
        material.apply(target,s.held,0xffff0000,.5f);float[] red=target.renderEffect.shader.floats.get("tint");
        check(target.renderEffect.shader==first&&red[0]==1&&red[1]==0&&red[2]==0&&red[3]==.5f,
                "pending mask changes update the retained body's tint rather than trusting pending logical state");
        material.clear(target);material.apply(target,s.held,0xffff0000,.5f);
        check(target.renderEffect.shader!=first&&target.renderEffect.shader.floatWritesForCheck.get("tint")==1,
                "a cleared/rebuilt body initializes even the same mask once");
        material.clear(target);s.close();
    }
    public static void main(String[] args)throws Exception {
        String phase=args.length==0?"all":args[0];Counts row=replay(false),both=replay(true);
        System.out.println("ROW_ONLY 12texts/120frames "+row);System.out.println("SOURCE_AND_ROW 12texts/120frames "+both);
        if(!phase.equals("baseline")) {
            if(phase.equals("all")||phase.equals("material")) {
                check(row.tint==0&&both.tint==0,"geometry-only sliding must not upload an unchanged tint for every text/frame");
                check(row.ownerChanges==0&&both.ownerChanges==0,"same-owner sliding must not allocate a replacement weak owner per frame");
            }
            if(phase.equals("all")||phase.equals("source"))check(row.inversions==0,"unchanged native bitmap mapping must return its validated cache before repeated matrix inversion");
            if(phase.equals("all")||phase.equals("consumer"))check(both.effects==TEXTS*FRAMES,"clock-owned materials submit once at each final geometry boundary");
        }
        if(!phase.equals("baseline"))tintCacheBoundaries();
        System.out.println("BackdropHorizontalBudgetCheck: "+checks+" checks passed (deterministic callback/work counts; GPU/FPS/device pending)");
    }
}
