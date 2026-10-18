// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RenderEffect;
import android.graphics.RuntimeShader;
import android.graphics.Shader;
import android.os.Build;
import android.view.View;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLongArray;

/** Uses an existing, proven unmixed system blur as the glyph body. No blur job
 * or bitmap copy is made here. Colour opacity is tint strength, never a second
 * alpha on the backdrop (which would expose the black scrim again). */
final class ClockBackdropMaterial {
    private static final String[] STAT_NAMES={"apply_calls","unchanged_calls","ready_to_pending","pending_to_ready","sampler_rebuilds",
            "effect_rebuilds","content_updates","geometry_updates","style_updates","material_failures","material_clears",
            "pending_visible","pending_hidden","pending_no_frame","pending_recycled","pending_root_mismatch","pending_other","deferred_updates"};
    private static final AtomicLongArray STATS=new AtomicLongArray(STAT_NAMES.length);
    private static volatile String lastState="inactive";
    private static volatile String lastPendingReason="none";
    private static final String BODY="uniform shader content; uniform shader background; uniform float4 tint;"
            +" uniform float4 edgeColor; uniform float4 relief; uniform float2 edgeWidths;"
            +" half4 main(float2 p) { half4 glyph=content.eval(p); half4 bg=background.eval(p);"
            +" float a=float(glyph.a); float3 base=float3(bg.rgb);"
            +" float3 rgb=mix(base,tint.rgb,tint.a);"
            +" if(a>0.0) {"
            +" if(relief.z>0.0||relief.w>0.0) { float2 ray=relief.xy*edgeWidths.y;"
            +" float d=float(content.eval(p-ray).a)-float(content.eval(p+ray).a);"
            +" if(d>0.0) rgb=mix(rgb,float3(1.0),clamp(d,0.0,1.0)*relief.z);"
            +" else rgb*=1.0-clamp(-d,0.0,1.0)*relief.w; }"
            +" if(edgeColor.a>0.0) { float2 dx=float2(edgeWidths.x,0.0); float2 dy=float2(0.0,edgeWidths.x);"
            +" float nearA=min(min(float(content.eval(p-dx).a),float(content.eval(p+dx).a)),"
            +" min(float(content.eval(p-dy).a),float(content.eval(p+dy).a)));"
            +" rgb=mix(rgb,edgeColor.rgb,clamp(1.0-nearA,0.0,1.0)*edgeColor.a); } }"
            +" return half4(half3(rgb*a),half(a)); }";
    private static final String PENDING="uniform shader content;"
            +" half4 main(float2 p) { float a=float(content.eval(p).a);"
            +" return half4(half3(a),half(a)); }";
    private final Matrix matrix=new Matrix(),mapping=new Matrix();
    private final Rect visibleBounds=new Rect();
    private final float[] values=new float[9],appliedValues=new float[9];
    private RuntimeShader body,pending;
    private Bitmap bitmap;
    private ClockBackdropSource.Frame retained;
    private BitmapShader input;
    private Shader.TileMode tileMode;
    private RenderEffect effect;
    private WeakReference<View> owner=new WeakReference<>(null);
    private boolean active,hasSource,failed;
    private int color;
    private float opacity=Float.NaN;
    private int bodyMask;
    private float bodyOpacity;
    private boolean bodyTintSet;
    private long revision=Long.MIN_VALUE;
    private long contentRevision=Long.MIN_VALUE;
    private int bitmapGeneration;
    private Appearance appearance=Appearance.NONE,bodyLook=Appearance.NONE;
    private final boolean retainSource;

    ClockBackdropMaterial(){this(true);}
    /** Weak-key notification owners must not retain Frame.host through values.
     * Clocks retain their immutable frame to keep the shared source cache warm. */
    ClockBackdropMaterial(boolean retainSource){this.retainSource=retainSource;}

    /** Pixel dimensions are supplied by the caller after density conversion.
     * Both effects stay inside the original glyph, so no larger layer is needed. */
    static final class Appearance {
        static final Appearance NONE=new Appearance(false,0f,0xffffffff,0f,315f,0f,0f,0f);
        final boolean borderEnabled;
        final float borderWidthPx,borderOpacity,angleDeg,highlight,shade,reliefWidthPx;
        final int borderColor;
        Appearance(){this(true,.6f,0xffffffff,.6f,315f,.4f,0f,1.5f);}
        Appearance(boolean borderEnabled,float borderWidthPx,int borderColor,float borderOpacity,
                float angleDeg,float highlight,float shade,float reliefWidthPx) {
            this.borderEnabled=borderEnabled;this.borderWidthPx=clip(borderWidthPx,32f);this.borderColor=borderColor;
            this.borderOpacity=clip(borderOpacity,1f);this.highlight=clip(highlight,1f);this.shade=clip(shade,1f);
            this.reliefWidthPx=clip(reliefWidthPx,32f);
            float angle=Float.isFinite(angleDeg)?angleDeg%360f:315f;
            this.angleDeg=angle==0f?0f:angle<0f?angle+360f:angle;
        }
        private static float clip(float value,float maximum){return Float.isFinite(value)?Math.max(0f,Math.min(maximum,value)):0f;}
        private boolean border(){return borderEnabled&&borderWidthPx>0f&&borderOpacity>0f&&(borderColor>>>24)!=0;}
        private boolean relief(){return reliefWidthPx>0f&&(highlight>0f||shade>0f);}
        private boolean flat(){return !border()&&!relief();}
        @Override public boolean equals(Object value) {
            if(this==value)return true;if(!(value instanceof Appearance))return false;Appearance other=(Appearance)value;
            return borderEnabled==other.borderEnabled&&borderColor==other.borderColor
                    &&borderWidthPx==other.borderWidthPx&&borderOpacity==other.borderOpacity&&angleDeg==other.angleDeg
                    &&highlight==other.highlight&&shade==other.shade&&reliefWidthPx==other.reliefWidthPx;
        }
        @Override public int hashCode() {
            int result=borderEnabled?1:0;result=31*result+borderColor;
            result=31*result+Float.floatToIntBits(borderWidthPx);result=31*result+Float.floatToIntBits(borderOpacity);
            result=31*result+Float.floatToIntBits(angleDeg);result=31*result+Float.floatToIntBits(highlight);
            result=31*result+Float.floatToIntBits(shade);return 31*result+Float.floatToIntBits(reliefWidthPx);
        }
    }

    boolean apply(View view,ClockBackdropSource.Frame frame,int mask,float amount) {
        return apply(view,frame,mask,amount,Appearance.NONE);
    }

    boolean apply(View view,ClockBackdropSource.Frame frame,int mask,float amount,Appearance requested) {
        STATS.incrementAndGet(0);
        if(view==null||Build.VERSION.SDK_INT<33||failed)return false;
        // This marker contains no bitmap. Native promotion is still completing;
        // do not sample/re-submit its released image or briefly switch to white.
        if(frame!=null&&frame.deferUpdate(view)){STATS.incrementAndGet(17);return false;}
        Appearance look=requested==null?Appearance.NONE:requested;
        float strength=Float.isFinite(amount)?Math.max(0f,Math.min(1f,amount)):0f;
        boolean ready=frame!=null&&frame.bitmap!=null&&frame.tileMode!=null&&!frame.bitmap.isRecycled()
                &&frame.host!=null&&frame.host.getRootView()==view.getRootView();
        try {
            boolean geometry=false;
            if(ready) {
                matrix.set(frame.bitmapToDrawable);
                mapping.reset();frame.host.transformMatrixToGlobal(mapping);view.transformMatrixToLocal(mapping);
                matrix.postConcat(mapping);matrix.getValues(values);
                for(float value:values)if(!Float.isFinite(value))return clear(view);
                geometry=!Arrays.equals(values,appliedValues);
            }
            boolean source=ready&&(bitmap!=frame.bitmap||revision!=frame.revision||tileMode!=frame.tileMode);
            boolean content=ready&&(bitmap!=frame.bitmap||contentRevision!=frame.contentRevision||bitmapGeneration!=frame.bitmapGeneration);
            boolean changed=!active||owner.get()!=view||hasSource!=ready||source||content||geometry||color!=mask||opacity!=strength
                    ||!appearance.equals(look);
            if(!changed){STATS.incrementAndGet(1);return false;}
            if(active&&hasSource&&!ready) {
                STATS.incrementAndGet(2);
                boolean visible=view.getWindowVisibility()==View.VISIBLE&&view.isShown()&&view.getGlobalVisibleRect(visibleBounds);
                for(View node=view;visible&&node!=null;) {
                    visible=node.getAlpha()>.01f&&node.getTransitionAlpha()>.01f;
                    android.view.ViewParent parent=node.getParent();node=parent instanceof View?(View)parent:null;
                }
                STATS.incrementAndGet(visible?11:12);
                if(frame==null){STATS.incrementAndGet(13);lastPendingReason="frame_missing";}
                else if(frame.bitmap!=null&&frame.bitmap.isRecycled()){STATS.incrementAndGet(14);lastPendingReason="bitmap_recycled";}
                else if(frame.host!=null&&frame.host.getRootView()!=view.getRootView()){STATS.incrementAndGet(15);lastPendingReason="root_mismatch";}
                else{STATS.incrementAndGet(16);lastPendingReason="other";}
            }
            if(active&&!hasSource&&ready)STATS.incrementAndGet(3);
            if(content)STATS.incrementAndGet(6);if(geometry)STATS.incrementAndGet(7);
            if(color!=mask||opacity!=strength||!appearance.equals(look))STATS.incrementAndGet(8);
            RuntimeShader shader;
            if(ready) {
                if(body==null){body=new RuntimeShader(BODY);bodyLook=Appearance.NONE;bodyTintSet=false;}
                if(content||input==null||tileMode!=frame.tileMode) {
                    input=new BitmapShader(frame.bitmap,frame.tileMode,frame.tileMode);
                    input.setFilterMode(BitmapShader.FILTER_MODE_LINEAR);
                    STATS.incrementAndGet(4);
                }
                if(source||content||geometry||!hasSource) {
                    input.setLocalMatrix(matrix);body.setInputShader("background",input);
                    System.arraycopy(values,0,appliedValues,0,9);
                }
                shader=body;bitmap=frame.bitmap;revision=frame.revision;contentRevision=frame.contentRevision;bitmapGeneration=frame.bitmapGeneration;
                retained=retainSource?frame:null;tileMode=frame.tileMode;
                applyAppearance(look);
            } else {
                if(pending==null)pending=new RuntimeShader(PENDING);
                shader=pending;bitmap=null;input=null;retained=null;tileMode=null;revision=Long.MIN_VALUE;
            }
            if(ready&&(!bodyTintSet||bodyMask!=mask||bodyOpacity!=strength)) {
                shader.setFloatUniform("tint",((mask>>>16)&255)/255f,((mask>>>8)&255)/255f,
                        (mask&255)/255f,(mask>>>24)/255f*strength);
                bodyMask=mask;bodyOpacity=strength;bodyTintSet=true;
            }
            RenderEffect next=RenderEffect.createRuntimeShaderEffect(shader,"content");
            STATS.incrementAndGet(5);lastState=ready?"ready":"pending";
            View old=owner.get();if(old!=null&&old!=view)old.setRenderEffect(null);
            view.setRenderEffect(next);effect=next;if(old!=view)owner=new WeakReference<>(view);
            active=true;hasSource=ready;color=mask;opacity=strength;appearance=look;return true;
        } catch(RuntimeException unavailable) {
            clear(view);failed=true;
            STATS.incrementAndGet(9);lastState="failed";
            ModuleDiagnostics.error("bigclock","Existing clock backdrop material unavailable",unavailable);
            return false;
        }
    }

    private void applyAppearance(Appearance look) {
        if(bodyLook.equals(look))return;
        // Runtime uniforms start at zero. Preserve the exact legacy upload path
        // when both old and new appearances have no visible edge treatment.
        if(!look.flat()||!bodyLook.flat()) {
            int border=look.borderColor;
            body.setFloatUniform("edgeColor",((border>>>16)&255)/255f,((border>>>8)&255)/255f,(border&255)/255f,
                    look.border()?(border>>>24)/255f*look.borderOpacity:0f);
            double radians=Math.toRadians(look.angleDeg);
            body.setFloatUniform("relief",(float)Math.cos(radians),(float)-Math.sin(radians),
                    look.relief()?look.highlight:0f,look.relief()?look.shade:0f);
            body.setFloatUniform("edgeWidths",look.borderWidthPx,look.reliefWidthPx);
        }
        bodyLook=look;
    }

    /** Distinguishes a cached no-op from an unavailable effect for borrowers. */
    boolean isAppliedTo(View view){return active&&hasSource&&!failed&&owner.get()==view;}

    boolean clear(View view) {
        View old=owner.get();
        if(active&&old!=null)old.setRenderEffect(null);
        if(active&&view!=null&&view!=old)view.setRenderEffect(null);
        boolean changed=active;if(changed){STATS.incrementAndGet(10);lastState="cleared";}
        active=false;hasSource=false;bitmap=null;input=null;effect=null;retained=null;tileMode=null;
        contentRevision=Long.MIN_VALUE;bitmapGeneration=0;
        body=pending=null;bodyTintSet=false;revision=Long.MIN_VALUE;opacity=Float.NaN;appearance=bodyLook=Appearance.NONE;owner.clear();return changed;
    }
    /** Fixed scalar metadata, collected only by an explicit runtime probe. */
    static Map<String,Object> diagnostics() {
        Map<String,Object> result=new LinkedHashMap<>();for(int i=0;i<STAT_NAMES.length;i++)result.put(STAT_NAMES[i],STATS.get(i));
        result.put("material_state",lastState);result.put("pending_reason",lastPendingReason);return result;
    }
}
