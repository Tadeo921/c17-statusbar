// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RenderEffect;
import android.os.Handler;
import android.os.Looper;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Map;
import java.util.WeakHashMap;

/** OEM backdrop edge for external Live Alert buffers. Uses the same native TiltShift
 * container/effect contract as the shade, with module-owned passive overlay views only. */
final class NotificationSurfaceClockEdge {
    private static final String CONTAINER="com.oplus.systemui.notification.blur.OplusNotificationTiltShiftBlurContainer";
    private static final String FACTORY="com.oplus.graphics.OplusRenderEffect";
    private static final Map<View,State> states=new WeakHashMap<>();
    private static final Map<View,Pending> pending=new WeakHashMap<>();
    private static final Handler main=new Handler(Looper.getMainLooper());
    private static final Map<ClassLoader,Access> contracts=new WeakHashMap<>();
    private static final int MAX_SURFACES=4;
    private NotificationSurfaceClockEdge() { }
    /** Draw submits the newest geometry only. Never add, measure or change material while
     * SystemUI is traversing its child/overlay list. One main-loop job per stack is coalesced. */
    static synchronized void update(View stack,float edgeTop,float edgeBottom,float radius,boolean enabled) {
        if(stack==null)return;
        Pending work=pending.get(stack);
        if(work==null){work=new Pending(stack);pending.put(stack,work);}
        work.top=edgeTop;work.bottom=edgeBottom;work.radius=radius;work.enabled=enabled;
        if(!work.scheduled){work.scheduled=true;if(!main.post(work)){work.scheduled=false;pending.remove(stack);}}
    }
    private static void apply(View stack,float edgeTop,float edgeBottom,float radius,boolean enabled) {
        if(!enabled||stack==null||!stack.isAttachedToWindow()||!stack.isShown()||!(stack.getParent() instanceof ViewGroup)
                ||!finite(edgeTop)||!finite(edgeBottom)||!finite(radius)||edgeBottom<=edgeTop||radius<=0f) {release(stack);return;}
        ArrayList<SurfaceView> sources=NotificationRemoteClockEdge.unmappedSurfaces(stack);
        if(sources.isEmpty()){release(stack);return;}
        ViewGroup parent=(ViewGroup)stack.getParent();
        State state=states.get(stack);
        if(state==null) {
            Access access=access(stack.getClass().getClassLoader());
            if(access==null||!access.ready){
                if(access!=null&&!access.reported){access.reported=true;ModuleDiagnostics.info("notification-edge",
                        "External Live Alert edge: native backdrop contract unavailable; retaining native rendering");}
                return;
            }
            state=new State(access,parent);states.put(stack,state);
        }
        if(state.parent.get()!=parent){release(stack);apply(stack,edgeTop,edgeBottom,radius,enabled);return;}
        long generation=++state.generation;
        try {
            state.matrix.reset();stack.transformMatrixToGlobal(state.matrix);parent.transformMatrixToLocal(state.matrix);
            state.points[0]=0f;state.points[1]=edgeTop;state.points[2]=stack.getWidth();state.points[3]=edgeBottom;
            state.matrix.mapPoints(state.points);
            float left=state.points[0],top=state.points[1],right=state.points[2],bottom=state.points[3];
            if(!finite(left)||!finite(top)||!finite(right)||!finite(bottom)||right<=left||bottom<=top){release(stack);return;}
            int count=0;
            for(SurfaceView source:sources) {
                if(source==null||!source.isAttachedToWindow()||!source.isShown()||source.getAlpha()<=0f)continue;
                state.matrix.reset();source.transformMatrixToGlobal(state.matrix);parent.transformMatrixToLocal(state.matrix);
                state.points[0]=0f;state.points[1]=0f;state.points[2]=source.getWidth();state.points[3]=source.getHeight();
                state.matrix.mapPoints(state.points);
                if(!finite(state.points[0])||!finite(state.points[1])||!finite(state.points[2])||!finite(state.points[3]))continue;
                int l=(int)Math.ceil(Math.max(left,state.points[0])),r=(int)Math.floor(Math.min(right,state.points[2]));
                int t=(int)Math.ceil(Math.max(top,state.points[1])),b=(int)Math.floor(Math.min(bottom,state.points[3]));
                if(r<=l||b<=t)continue;
                if(count++>=MAX_SURFACES)break;
                float start=quantize(radius*(1f-clamp((t-top)/(bottom-top))));
                float end=quantize(radius*(1f-clamp((b-top)/(bottom-top))));
                Owned owned=null;
                for(Owned candidate:state.overlays)if(candidate.source.get()==source){owned=candidate;break;}
                if(owned==null){owned=new Owned(source);state.overlays.add(owned);}
                View effectView=owned.view.get();
                if(effectView==null) {
                    effectView=(View)state.access.constructor.newInstance(parent.getContext());
                    effectView.setClickable(false);effectView.setFocusable(false);
                    effectView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                    state.access.add.invoke(parent.getOverlay(),effectView);
                    owned.view=new WeakReference<>(effectView);
                }
                if(owned.left!=l||owned.top!=t||owned.right!=r||owned.bottom!=b) {
                    effectView.measure(View.MeasureSpec.makeMeasureSpec(r-l,View.MeasureSpec.EXACTLY),
                            View.MeasureSpec.makeMeasureSpec(b-t,View.MeasureSpec.EXACTLY));
                    effectView.layout(l,t,r,b);owned.left=l;owned.top=t;owned.right=r;owned.bottom=b;
                }
                if(owned.start!=start||owned.end!=end) {
                    Object effect=state.access.gradient.invoke(null,start,end,true,4f,1,0,0);
                    if(!(effect instanceof RenderEffect))throw new IllegalStateException("OEM gradient returned unsupported effect");
                    state.access.effect.set(effectView,effect);state.access.apply.invoke(effectView);
                    owned.start=start;owned.end=end;
                    if(!state.reported){state.reported=true;ModuleDiagnostics.info("notification-edge",
                            "External Live Alert edge registered: nativeTiltShift=true; bounded overlay=true");}
                }
                owned.generation=generation;
            }
            for(Owned owned:new ArrayList<>(state.overlays))
                if(owned.generation!=generation){remove(state,owned);state.overlays.remove(owned);}
        } catch(ReflectiveOperationException|RuntimeException|LinkageError unavailable) {
            release(stack);state.access.ready=false;
            try{ModuleDiagnostics.error("notification-edge","Native Live Alert backdrop contract unavailable; retaining native rendering",unavailable);}catch(Throwable ignored){ }
        }
    }
    static synchronized void release(View stack) {
        Pending work=pending.remove(stack);if(work!=null)main.removeCallbacks(work);
        State state=states.remove(stack);
        if(state!=null)for(Owned owned:new ArrayList<>(state.overlays))remove(state,owned);
    }
    static synchronized void releaseResources() {
        for(Pending work:new ArrayList<>(pending.values()))main.removeCallbacks(work);
        pending.clear();
        for(State state:new ArrayList<>(states.values()))for(Owned owned:new ArrayList<>(state.overlays))remove(state,owned);
        states.clear();
    }
    private static void remove(State state,Owned owned) {
        View view=owned.view.get();ViewGroup parent=state.parent.get();
        if(view==null)return;
        try{state.access.clear.invoke(view);}catch(ReflectiveOperationException|RuntimeException ignored){ }
        if(parent!=null)try{state.access.remove.invoke(parent.getOverlay(),view);}catch(ReflectiveOperationException|RuntimeException ignored){ }
        owned.view.clear();
    }
    private static Access access(ClassLoader loader) {
        Access access=contracts.get(loader);
        if(access==null){access=new Access(loader);contracts.put(loader,access);}return access;
    }
    private static float quantize(float value){return Math.round(Math.max(0f,Math.min(48f,value))*2f)/2f;}
    private static float clamp(float value){return Math.max(0f,Math.min(1f,value));}
    private static boolean finite(float value){return !Float.isNaN(value)&&!Float.isInfinite(value);}
    private static final class Pending implements Runnable {
        final WeakReference<View> stack;float top,bottom,radius;boolean enabled,scheduled;
        Pending(View stack){this.stack=new WeakReference<>(stack);}
        @Override public void run(){synchronized(NotificationSurfaceClockEdge.class){
            View view=stack.get();if(view==null||pending.get(view)!=this)return;
            scheduled=false;pending.remove(view);apply(view,top,bottom,radius,enabled);
        }}
    }
    private static final class State {
        final Access access;final WeakReference<ViewGroup> parent;
        // Keep the owned cleanup record even after an external Surface is collected.
        // A WeakHashMap entry would silently disappear and leave its native overlay behind.
        final ArrayList<Owned> overlays=new ArrayList<>();
        final Matrix matrix=new Matrix();final float[] points=new float[4];long generation;boolean reported;
        State(Access access,ViewGroup parent){this.access=access;this.parent=new WeakReference<>(parent);}
    }
    private static final class Owned {
        final WeakReference<SurfaceView> source;
        WeakReference<View> view=new WeakReference<>(null);long generation;
        int left=Integer.MIN_VALUE,top,right,bottom;float start=Float.NaN,end;
        Owned(SurfaceView source){this.source=new WeakReference<>(source);}
    }
    private static final class Access {
        Constructor<?> constructor;Method gradient,apply,clear,add,remove;Field effect;boolean ready,reported;
        Access(ClassLoader loader) {
            try {
                Class<?> type=Class.forName(CONTAINER,false,loader);
                if(!android.widget.FrameLayout.class.isAssignableFrom(type))return;
                constructor=type.getDeclaredConstructor(Context.class);
                apply=type.getDeclaredMethod("setMaterialBlur");clear=type.getDeclaredMethod("resetMaterialBlur");
                effect=type.getDeclaredField("blurRenderEffect");
                Class<?> factory=Class.forName(FACTORY,false,loader);
                gradient=factory.getDeclaredMethod("createGradientBlurEffect",float.class,float.class,boolean.class,float.class,int.class,int.class,int.class);
                Class<?> overlay=Class.forName("android.view.ViewGroupOverlay",false,loader);
                add=overlay.getDeclaredMethod("add",View.class);remove=overlay.getDeclaredMethod("remove",View.class);
                if(effect.getType()!=RenderEffect.class||apply.getReturnType()!=Void.TYPE||clear.getReturnType()!=Void.TYPE
                        ||gradient.getReturnType()!=RenderEffect.class||!Modifier.isStatic(gradient.getModifiers()))return;
                constructor.setAccessible(true);effect.setAccessible(true);apply.setAccessible(true);clear.setAccessible(true);gradient.setAccessible(true);
                ready=true;
            } catch(ReflectiveOperationException|RuntimeException|LinkageError ignored){ }
        }
    }
}
