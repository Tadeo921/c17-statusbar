package dev.puitheme;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.TextView;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

/** The actual owned ClockView traversal, including retained display lists:
 * property-only native motion must not need a child dispatchDraw or a new blur
 * image. These are synchronization/work assertions, not device FPS claims. */
public final class ClockBackdropFrameSyncCheck {
    private static int checks;
    private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    private static Field field(Class<?> type,String name)throws Exception{Field f=type.getDeclaredField(name);f.setAccessible(true);return f;}
    private static Object get(Object target,String name)throws Exception{return field(target.getClass(),name).get(target);}
    private static void set(Object target,String name,Object value)throws Exception{field(target.getClass(),name).set(target,value);}
    private static Method method(Class<?> type,String name,Class<?>... args)throws Exception{Method m=type.getDeclaredMethod(name,args);m.setAccessible(true);return m;}
    private static final class Scene implements AutoCloseable {
        final ClockBackdropCheckScene nativeScene=new ClockBackdropCheckScene();
        final NotificationBigClock owner=new NotificationBigClock();
        final FrameLayout root=new FrameLayout(new Context()),overlay,header;
        final TextView clock;
        final Class<?> viewType,settingsType;
        final Constructor<?> settingsConstructor;
        final Bundle values=new Bundle();
        @SuppressWarnings("unchecked") Scene()throws Exception {
            root.addView(nativeScene.root);
            root.getResources().getConfiguration().orientation=1;
            viewType=Class.forName("dev.puitheme.NotificationBigClock$ClockView");
            settingsType=Class.forName("dev.puitheme.NotificationBigClock$Settings");
            Constructor<?> ctor=viewType.getDeclaredConstructor(NotificationBigClock.class,Context.class);ctor.setAccessible(true);
            overlay=(FrameLayout)ctor.newInstance(owner,new Context());root.addView(overlay);
            set(owner,"panel",new WeakReference<View>(root));set(owner,"overlay",overlay);set(owner,"active",true);
            set(owner,"barState",0);set(owner,"fraction",1f);set(owner,"panelSettledClosed",false);
            Constructor<?> h=Class.forName("dev.puitheme.NotificationBigClock$HeaderState").getDeclaredConstructor(View.class);h.setAccessible(true);
            ((java.util.Map<Object,Object>)get(owner,"headers")).put(root,h.newInstance(new FrameLayout(new Context())));
            header=(FrameLayout)get(overlay,"headerMotion");clock=(TextView)get(overlay,"clock");clock.setText("12:34");
            ((View)get(overlay,"dateLabel")).setVisibility(View.GONE);((View)get(overlay,"footerLabel")).setVisibility(View.GONE);
            values.putBoolean(NotificationBigClockSettings.MASTER,true);values.putBoolean(NotificationBigClockSettings.DIGIT_REVEAL_ENABLED,true);
            settingsConstructor=settingsType.getDeclaredConstructor(Bundle.class,String.class);settingsConstructor.setAccessible(true);applySettings();
            method(viewType,"updateBackdrops").invoke(overlay);method(viewType,"observeWidgets").invoke(overlay);
            check(clock.renderEffect!=null,"real ClockView binds the observed native image before motion");
            check(overlay.getViewTreeObserver().preDrawListeners.size()==1,"the existing owned traversal observer is registered once");
            overlay.queuePostedForCheck=true;
        }
        void applySettings()throws Exception {set(owner,"settings",settingsConstructor.newInstance(values,NotificationBigClockSettings.PORTRAIT_PREFIX));}
        void traversal(){for(ViewTreeObserver.OnPreDrawListener listener:new ArrayList<>(overlay.getViewTreeObserver().preDrawListeners))listener.onPreDraw();}
        void event()throws Exception {method(NotificationBigClock.class,"onBackdropChanged").invoke(owner);}
        float sampleX(){float[] m=new float[9];((BitmapShader)clock.renderEffect.shader.inputs.get("background")).matrix.getValues(m);return m[2];}
        public void close()throws Exception{method(viewType,"unscheduleWidgets").invoke(overlay);method(viewType,"clearMaterials").invoke(overlay);nativeScene.close();}
    }
    private static void retainedDisplayListMotion()throws Exception {
        try(Scene s=new Scene()) {
            int writes=s.clock.renderEffectWrites,invalidations=s.overlay.postedInvalidations;
            for(int frame=1;frame<=120;frame++) {
                // Native progressive animation's real UI callsite uses
                // View.setTranslationX. A retained child list need not draw.
                s.header.setTranslationX(frame*2f);s.traversal();
                check(Math.abs(s.sampleX()+frame*2f)<.001f,"property-only frame must sample final geometry before display-list reuse: frame "+frame+", sample="+s.sampleX());
            }
            check(s.clock.renderEffectWrites==writes+120,"only one changed matrix is submitted per actual traversal");
            int stable=s.clock.renderEffectWrites;
            for(int frame=0;frame<120;frame++)s.traversal();
            check(s.clock.renderEffectWrites==stable,"unchanged traversals submit no effects");
            check(s.overlay.postedInvalidations==invalidations,"observing native motion never starts a continuous invalidate loop");
            check(s.overlay.postedTasksForCheck.isEmpty(),"property-only motion creates no refresh callback queue");
            set(s.owner,"qsExpanded",true);s.header.setTranslationX(311f);s.traversal();
            check(s.sampleX()==-311f,"a QS transition still aligns the last visible glyph after clock eligibility changes");
        }
    }
    private static void deliveryDuringTraversal()throws Exception {
        try(Scene s=new Scene()) {
            java.util.concurrent.CountDownLatch gap=new java.util.concurrent.CountDownLatch(1),finish=new java.util.concurrent.CountDownLatch(1);
            java.util.concurrent.atomic.AtomicReference<Throwable> failure=new java.util.concurrent.atomic.AtomicReference<>();
            Bitmap latest=Bitmap.createBitmap(3,1,Bitmap.Config.ARGB_8888);
            s.nativeScene.engine.afterReleaseForCheck=()->{
                gap.countDown();
                try {if(!finish.await(2,java.util.concurrent.TimeUnit.SECONDS))throw new AssertionError("bounded native promotion was not released");}
                catch(InterruptedException interrupted){throw new AssertionError(interrupted);}
            };
            int writes=s.clock.renderEffectWrites,invalidations=s.overlay.postedInvalidations;
            Thread nativeThread=new Thread(()->{
                try(ClockBackdropSource.DrawScope scope=ClockBackdropSource.beginDraw(s.nativeScene.engine)) {
                    s.nativeScene.engine.completeForCheck(latest);s.nativeScene.engine.preDrawContent();scope.complete();
                } catch(Throwable error){failure.set(error);}
            },"native-promotion-frame-check");
            nativeThread.start();
            try {
                check(gap.await(2,java.util.concurrent.TimeUnit.SECONDS),"native promotion reaches its real release/direct-assignment gap");
                s.header.setTranslationX(49f);s.traversal();
                check(s.clock.renderEffectWrites==writes,"a traversal during promotion neither samples released input nor submits white pending");
            } finally {finish.countDown();nativeThread.join(2500);}
            check(!nativeThread.isAlive()&&failure.get()==null,"native promotion finishes without UI/RT blocking or failure");
            check(s.overlay.postedInvalidations==invalidations+1,"deferred traversal guarantees one final source-completion frame");
            s.traversal();
            check(s.clock.renderEffectWrites==writes+1&&s.sampleX()==-49f,"the final traversal submits the promoted image at the final native position once");
            check(((BitmapShader)s.clock.renderEffect.shader.inputs.get("background")).bitmap==latest,"final traversal replaces the released native content");
        }
    }
    private static void simultaneousHints()throws Exception {
        try(Scene s=new Scene()) {
            int invalidations=s.overlay.postedInvalidations;
            java.util.concurrent.CountDownLatch start=new java.util.concurrent.CountDownLatch(1);
            java.util.concurrent.atomic.AtomicReference<Throwable> failure=new java.util.concurrent.atomic.AtomicReference<>();
            Thread[] producers=new Thread[8];
            for(int i=0;i<producers.length;i++) {
                producers[i]=new Thread(()->{try{start.await();for(int n=0;n<10;n++)s.event();}catch(Throwable error){failure.set(error);}},"source-hint-"+i);
                producers[i].start();
            }
            start.countDown();for(Thread producer:producers)producer.join(2500);
            for(Thread producer:producers)check(!producer.isAlive(),"source hint producer never waits on a rendering frame");
            check(failure.get()==null&&s.overlay.postedInvalidations==invalidations+1,"concurrent native hints share one atomic traversal request");
            s.traversal();s.event();check(s.overlay.postedInvalidations==invalidations+2,"consuming a hint permits the next real delivery");
            s.overlay.attached=false;int detached=s.overlay.postedInvalidations;s.event();
            check(s.overlay.postedInvalidations==detached,"a detached owner is never resurrected by a source hint");
            s.overlay.attached=true;
        }
    }
    private static void burstDelivery()throws Exception {
        try(Scene s=new Scene()) {
            int posts=s.overlay.postCallsForCheck,invalidations=s.overlay.postedInvalidations,writes=s.clock.renderEffectWrites;
            Bitmap latest=Bitmap.createBitmap(3,1,Bitmap.Config.ARGB_8888);
            for(int i=0;i<80;i++) {
                s.nativeScene.engine.completeForCheck(i==79?latest:s.nativeScene.bitmap);
                s.nativeScene.engine.preDrawContent();ClockBackdropSource.onDrawn(s.nativeScene.engine);
            }
            check(s.overlay.postCallsForCheck==posts&&s.overlay.postedTasksForCheck.isEmpty(),"native delivery bursts must not repeatedly remove/post a late UI material refresh");
            check(s.overlay.postedInvalidations==invalidations+1,"a burst requests one traversal, without sliding its queue position");
            check(s.clock.renderEffectWrites==writes,"native callbacks do not submit partial geometry before traversal");
            s.header.setTranslationX(57f);s.traversal();
            check(((BitmapShader)s.clock.renderEffect.shader.inputs.get("background")).bitmap==latest,"the traversal consumes the latest completed native frame");
            check(s.sampleX()==-57f,"source and final target geometry are consumed together");
            check(s.clock.renderEffectWrites==writes+1,"one burst produces one material submission");
            s.event();check(s.overlay.postedInvalidations==invalidations+2,"the completed traversal releases the next source invalidation");
            s.traversal();check(s.clock.renderEffectWrites==writes+1,"a source hint without changed content or geometry creates no effect");
            s.values.putBoolean(StatusBarSettings.SAFE_MODE,true);s.applySettings();
            int safe=s.overlay.postedInvalidations;s.event();s.traversal();
            check(s.overlay.postedInvalidations==safe,"safety mode never queues a backdrop-only traversal");
        }
    }
    private static void scheduledLayoutReadsOnce()throws Exception {
        try(Scene s=new Scene()) {
            set(s.overlay,"time","12:34");
            final int[] registered={0};
            set(s.owner,"pageMotion",new NotificationBigClock.PageMotion(){
                public void register(View target){if(target==s.header){registered[0]++;target.setTranslationX(83f);}}
                public void unregister(View target){}
                public boolean isRunning(){return false;}
            });
            set(s.overlay,"widgetsScheduled",true);
            long reads=((Number)ClockBackdropSource.diagnostics().get("snapshot_calls")).longValue();
            int effects=s.clock.renderEffectWrites;
            s.traversal();
            long after=((Number)ClockBackdropSource.diagnostics().get("snapshot_calls")).longValue();
            check(after-reads==1,"a scheduled layout/rebound traversal takes one final source snapshot, not a second redundant read: "+(after-reads));
            check(!(Boolean)get(s.overlay,"widgetsScheduled"),"the real layout branch was consumed before counting snapshots");
            check(registered[0]==1&&s.header.getTranslationX()==83f,"native motion registration can write the final position after widget layout");
            check(Math.abs(s.sampleX()+83f+s.clock.getTranslationX()+s.clock.getLeft())<.001f,"one final snapshot submits geometry after native motion registration");
            check(s.clock.renderEffectWrites==effects+1,"layout plus native registration submits one final effect rather than an intermediate one");
            set(s.owner,"qsExpanded",true);s.header.setTranslationX(97f);
            long beforeQs=((Number)ClockBackdropSource.diagnostics().get("snapshot_calls")).longValue();s.traversal();
            check(((Number)ClockBackdropSource.diagnostics().get("snapshot_calls")).longValue()==beforeQs+1,"the QS transition retains its one final source read outside eligible layout");
            check(Math.abs(s.sampleX()+97f+s.clock.getTranslationX()+s.clock.getLeft())<.001f,"the last visible glyph retains its final QS geometry");
        }
    }
    public static void main(String[] args)throws Exception {
        if(args.length==0||args[0].equals("motion"))retainedDisplayListMotion();
        if(args.length==0||args[0].equals("burst"))burstDelivery();
        if(args.length==0||args[0].equals("promotion"))deliveryDuringTraversal();
        if(args.length==0||args[0].equals("concurrent"))simultaneousHints();
        if(args.length==0||args[0].equals("layout"))scheduledLayoutReadsOnce();
        System.out.println("ClockBackdropFrameSyncCheck: "+checks+" checks passed (actual preDraw/property-only and queued-delivery seams; device sync pending)");
    }
}
