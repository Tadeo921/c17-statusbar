// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.graphics.Matrix;
import android.graphics.RenderEffect;
import android.graphics.RuntimeShader;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Map;
import java.util.WeakHashMap;

/** Local content edge for native Surface-backed Live Alerts. A Canvas capture cannot sample
 * these independent buffers. Only a proven in-process SurfacePackage root is modified. */
public final class NotificationRemoteClockEdge {
    private static final String ROW="com.oplus.systemui.statusbar.notification.customcard.OplusCustomRow";
    private static final Object LOCK=new Object();
    private static final Map<View,RenderEffect> nativeEffects=new WeakHashMap<>();
    private static final Map<View,EffectState> effects=new WeakHashMap<>();
    private static final Map<View,StackState> stacks=new WeakHashMap<>();
    private static final Map<View,Pending> pending=new WeakHashMap<>();
    private static final Handler main=new Handler(Looper.getMainLooper());
    private static final Map<Class<?>,RowMethods> rowMethods=new WeakHashMap<>();
    private static final Map<Class<?>,Method> miniMethods=new WeakHashMap<>();
    private static boolean tracking;
    private NotificationRemoteClockEdge() { }

    /** Called after the early View.setRenderEffect hook has been installed, before native roots. */
    public static void trackingReady() { synchronized(LOCK){tracking=true;} }
    /** Native setter hook. Internal writes are excluded; a native writer always wins. */
    public static void nativeEffect(View view,RenderEffect effect) {
        if(view==null)return;
        synchronized(LOCK) {
            EffectState state=effects.get(view);
            if(state!=null&&state.writing)return;
            if(effect==null)nativeEffects.remove(view);else nativeEffects.put(view,effect);
            if(state!=null){state.original=effect;state.applied=null;state.baseChanged=true;}
        }
    }
    /** Release before the native host disappears. Retain any newer native effect. */
    public static void release(View root) {
        if(root==null||Build.VERSION.SDK_INT<33)return;
        synchronized(LOCK){restore(root,effects.remove(root));nativeEffects.remove(root);}
    }
    public static void releaseStack(View stack) {
        synchronized(LOCK) {
            Pending work=pending.remove(stack);if(work!=null)main.removeCallbacks(work);
            StackState state=stacks.remove(stack);
            if(state!=null)for(View root:new ArrayList<>(state.active.keySet()))restore(root,effects.remove(root));
        }
    }
    static ArrayList<SurfaceView> unmappedSurfaces(View stack) {
        ArrayList<SurfaceView> result=new ArrayList<>();
        synchronized(LOCK){StackState state=stacks.get(stack);if(state!=null)result.addAll(state.unmapped.keySet());}
        return result;
    }
    public static void releaseResources() {
        if(Build.VERSION.SDK_INT<33)return;
        synchronized(LOCK) {
            for(Pending work:new ArrayList<>(pending.values()))main.removeCallbacks(work);
            pending.clear();
            for(Map.Entry<View,EffectState> value:new ArrayList<>(effects.entrySet()))restore(value.getKey(),value.getValue());
            effects.clear();stacks.clear();
        }
    }

    /** The caller uses the same stack-local edge coordinates as its regular notification capture. */
    public static void update(View stack,float hardTop,float top,float bottom,float radius,boolean active) {
        if(Build.VERSION.SDK_INT<33||stack==null)return;
        synchronized(LOCK){
            Pending work=pending.get(stack);
            if(work==null){work=new Pending(stack);pending.put(stack,work);}
            work.hard=hardTop;work.top=top;work.bottom=bottom;work.radius=radius;work.active=active;
            if(!work.scheduled){work.scheduled=true;if(!main.post(work)){work.scheduled=false;pending.remove(stack);}}
        }
    }
    private static void applyLatest(View stack,float hardTop,float top,float bottom,float radius,boolean active) {
        if(!active||!(stack instanceof ViewGroup)||!finite(hardTop)||!finite(top)||!finite(bottom)
                ||!finite(radius)||bottom<=top||!stack.isAttachedToWindow()||!stack.isShown()){releaseStack(stack);return;}
        synchronized(LOCK) {
            StackState state=stacks.get(stack);
            if(state==null){state=new StackState();stacks.put(stack,state);}
            long generation=++state.generation;
            ViewGroup group=(ViewGroup)stack;
            for(int i=0,n=group.getChildCount();i<n;i++) {
                View row=group.getChildAt(i);
                if(row==null||row.getVisibility()!=View.VISIBLE||row.getAlpha()<=0f)continue;
                RowMethods methods=rowMethods.get(row.getClass());
                if(methods==null){methods=new RowMethods(row.getClass());rowMethods.put(row.getClass(),methods);}
                if(!methods.custom||methods.entry==null)continue;
                try {
                    Object entry=methods.entry.invoke(row);
                    if(entry==null)continue;
                    Method mini=miniMethods.get(entry.getClass());
                    if(!miniMethods.containsKey(entry.getClass())){mini=find(entry.getClass(),"getMiniCardView");miniMethods.put(entry.getClass(),mini);}
                    Object candidate=mini==null?null:mini.invoke(entry);
                    if(!(candidate instanceof View))continue;
                    View card=(View)candidate;
                    CardState cache=state.cards.get(row);
                    if(cache==null||cache.card.get()!=card){cache=new CardState(card);state.cards.put(row,cache);}
                    SurfaceView surface=cache.surface.get();
                    if(surface!=null&&!under(surface,card)){
                        cache.surface.clear();cache.searches=0;surface=null;
                    }
                    int childCount=card instanceof ViewGroup?((ViewGroup)card).getChildCount():0;
                    if(surface==null&&(cache.searches++<2||cache.childCount!=childCount)) {
                        cache.childCount=childCount;surface=findSurface(card,0,new int[]{0});cache.surface=new WeakReference<>(surface);
                    }
                    if(surface==null||surface.getVisibility()!=View.VISIBLE||!surface.isAttachedToWindow())continue;
                    View root=FluidCloudSurfaceRoots.contentOf(surface);
                    if(root==null||root==surface||!tracking||root.getClass().getName().equals(
                            "com.oplus.systemui.plugins.seedling.notification.surface.viewholder.d")) {
                        state.unmapped.put(surface,generation);continue;
                    }
                    if(!root.isAttachedToWindow()||root.getWidth()<=0||root.getHeight()<=0)continue;
                    state.matrix.reset();surface.transformMatrixToGlobal(state.matrix);stack.transformMatrixToLocal(state.matrix);
                    state.points[0]=0f;state.points[1]=0f;state.points[2]=surface.getWidth();state.points[3]=surface.getHeight();
                    state.matrix.mapPoints(state.points);
                    float surfaceY=state.points[1],height=state.points[3]-surfaceY;
                    if(!finite(surfaceY)||!finite(height)||height<=0f)continue;
                    float sx=(state.points[2]-state.points[0])/Math.max(1f,root.getWidth());
                    float sy=height/root.getHeight();
                    if(!finite(sx)||!finite(sy)||sx<=0f||sy<=0f)continue;
                    if(surfaceY>=bottom)continue; // Fully clear: remove our effect below the edge.
                    apply(root,(hardTop-surfaceY)/sy,(top-surfaceY)/sy,(bottom-surfaceY)/sy,
                            Math.max(0f,Math.min(48f,radius))/sx,Math.max(0f,Math.min(48f,radius))/sy);
                    state.active.put(root,generation);
                } catch(ReflectiveOperationException|RuntimeException|LinkageError ignored) { }
            }
            for(Map.Entry<View,Long> value:new ArrayList<>(state.active.entrySet()))if(value.getValue()!=generation) {
                restore(value.getKey(),effects.remove(value.getKey()));state.active.remove(value.getKey());
            }
            state.unmapped.entrySet().removeIf(value->value.getValue()!=generation);
        }
    }
    private static void apply(View root,float hard,float top,float bottom,float rx,float ry) {
        EffectState state=effects.get(root);
        if(state==null){state=new EffectState(nativeEffects.get(root));effects.put(root,state);}
        if(state.failed)return;
        try {
            if(state.shader==null) {
                state.shader=new RuntimeShader(SHADER);
                state.raw=RenderEffect.createRuntimeShaderEffect(state.shader,"content");
                state.baseChanged=true;
                ModuleDiagnostics.info("notification-edge","Hosted Live Alert edge registered: localRoot=true; native effect chained=true");
            }
            boolean changed=state.hard!=hard||state.top!=top||state.bottom!=bottom||state.rx!=rx||state.ry!=ry;
            if(changed) {
                state.shader.setFloatUniform("edge",hard,top,bottom);
                state.shader.setFloatUniform("radius",rx,ry);
                state.hard=hard;state.top=top;state.bottom=bottom;state.rx=rx;state.ry=ry;
            }
            if(state.baseChanged) {
                state.composed=state.original==null?state.raw:RenderEffect.createChainEffect(state.raw,state.original);
                state.baseChanged=false;
            }
            if(changed||state.applied!=state.composed){write(root,state,state.composed);state.applied=state.composed;}
        } catch(RuntimeException|LinkageError unavailable) {
            restore(root,state);state.failed=true;
            try{ModuleDiagnostics.error("notification-edge","Live Alert content edge unavailable; retaining native content",unavailable);}catch(Throwable ignored){ }
        }
    }
    private static void restore(View root,EffectState state) {
        if(root==null||state==null)return;
        if(state.applied!=null)try{write(root,state,state.original);}catch(RuntimeException|LinkageError ignored){ }
        state.applied=null;
    }
    private static void write(View root,EffectState state,RenderEffect value) {
        state.writing=true;try{root.setRenderEffect(value);}finally{state.writing=false;}
    }
    private static SurfaceView findSurface(View root,int depth,int[] visits) {
        if(root==null||depth>5||++visits[0]>48)return null;
        if(root instanceof SurfaceView)return (SurfaceView)root;
        if(root instanceof ViewGroup){ViewGroup group=(ViewGroup)root;for(int i=0;i<group.getChildCount();i++) {
            SurfaceView found=findSurface(group.getChildAt(i),depth+1,visits);if(found!=null)return found;
        }}return null;
    }
    private static boolean under(View view,View root){
        for(int i=0;view!=null&&i<32;i++){
            if(view==root)return true;
            android.view.ViewParent parent=view.getParent();view=parent instanceof View?(View)parent:null;
        }return false;
    }
    private static Method find(Class<?> type,String name) {
        for(Class<?> c=type;c!=null;c=c.getSuperclass())try{Method method=c.getDeclaredMethod(name);method.setAccessible(true);return method;}
        catch(ReflectiveOperationException|RuntimeException ignored){ }return null;
    }
    private static boolean finite(float value){return !Float.isNaN(value)&&!Float.isInfinite(value);}
    private static final class Pending implements Runnable {
        final WeakReference<View> stack;float hard,top,bottom,radius;boolean active,scheduled;
        Pending(View stack){this.stack=new WeakReference<>(stack);}
        @Override public void run(){synchronized(LOCK){
            View view=stack.get();if(view==null||pending.get(view)!=this)return;
            pending.remove(view);scheduled=false;applyLatest(view,hard,top,bottom,radius,active);
        }}
    }
    private static final class RowMethods {
        final boolean custom;final Method entry;
        RowMethods(Class<?> type){boolean known=false;for(Class<?> c=type;c!=null;c=c.getSuperclass())if(ROW.equals(c.getName())){known=true;break;}
            custom=known;entry=known?find(type,"getEntry"):null;}
    }
    private static final class CardState {
        final WeakReference<View> card;WeakReference<SurfaceView> surface=new WeakReference<>(null);int searches,childCount=-1;
        CardState(View card){this.card=new WeakReference<>(card);}
    }
    private static final class StackState {
        final WeakHashMap<View,CardState> cards=new WeakHashMap<>();
        final WeakHashMap<View,Long> active=new WeakHashMap<>();
        final WeakHashMap<SurfaceView,Long> unmapped=new WeakHashMap<>();
        final Matrix matrix=new Matrix();final float[] points=new float[4];long generation;
    }
    private static final class EffectState {
        RenderEffect original,raw,composed,applied;RuntimeShader shader;
        boolean writing,baseChanged,failed;float hard=Float.NaN,top,bottom,rx,ry;
        EffectState(RenderEffect original){this.original=original;}
    }
    // A fixed 3x3 kernel is evaluated only in the narrow transition band. Clear pixels bypass it;
    // no screenshots, bitmap reads, animation timers, parent alpha or SurfaceControl changes.
    private static final String SHADER="uniform shader content; uniform float3 edge; uniform float2 radius;"
        +"half4 main(float2 p){ if(p.y<edge.x)return half4(0); half4 sharp=content.eval(p);"
        +"if(p.y>=edge.z)return sharp; float t=clamp((p.y-edge.y)/max(1.0,edge.z-edge.y),0.0,1.0);"
        +"float a=t*t*(3.0-2.0*t); if(a<=0.0)return half4(0); if(radius.x+radius.y<=0.0)return sharp*half(a); float2 d=radius*0.6;"
        +"half4 b=content.eval(p+float2(-d.x,-d.y))+content.eval(p+float2(d.x,-d.y))"
        +"+content.eval(p+float2(-d.x,d.y))+content.eval(p+float2(d.x,d.y));"
        +"b+=2.0*(content.eval(p+float2(0.0,-d.y))+content.eval(p+float2(0.0,d.y))"
        +"+content.eval(p+float2(-d.x,0.0))+content.eval(p+float2(d.x,0.0)));"
        +"b+=4.0*sharp; return mix(b/16.0,sharp,half(a))*half(a);}";
}
