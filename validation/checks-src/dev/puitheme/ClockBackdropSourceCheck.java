package dev.puitheme;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Shader;
import android.view.View;
import android.widget.FrameLayout;
import com.android.systemui.scrim.ScrimView;
import com.oplus.posteffect.BlurDrawable;
import com.oplus.posteffect.BlurParam;
import com.oplus.posteffect.drawable.ContinuousBlurDrawable;
import com.oplus.posteffect.manager.BlurDrawableManager;
import com.oplus.posteffect.util.Log;
import com.oplusos.systemui.common.blurability.platformblur.PlatformBlurDrawable;
import com.oplusos.systemui.common.blurability.platformblur.PlatformBlurHelper;

/** Real observer events against the saved ROM's transport and ownership shape.
 * No private source is installed, submitted, sampled on CPU or released. */
public final class ClockBackdropSourceCheck {
    private static int checks;
    private static void check(boolean yes,String why){checks++;if(!yes)throw new AssertionError(why+" ["+ClockBackdropSource.lastFailure()+"]");}
    private static void reset(){ClockBackdropSource.clear();Log.DEBUG=false;BlurDrawableManager.sBlendTypeValue=-1;}
    private static final class Scene {
        final FrameLayout root=new FrameLayout(null);
        final ScrimView host=new ScrimView();
        final View clock=new View(null);
        final PlatformBlurHelper helper;
        final PlatformBlurDrawable wrapper=new PlatformBlurDrawable();
        final BlurDrawable engine=wrapper.blurDrawable;
        final Bitmap bitmap=Bitmap.createBitmap(120,80,Bitmap.Config.ARGB_8888);
        Scene(){host.setId(903);host.getResources().entryNames.put(903,"scrim_behind");root.addView(host);root.addView(clock);helper=new PlatformBlurHelper(host);engine.params.radius=0;engine.setBounds(0,0,240,160);}
        void observe(){try(ClockBackdropSource.Scope scope=ClockBackdropSource.beginHelper(helper)){ClockBackdropSource.created(wrapper);}}
        void add(){engine.params.radius=40;ClockBackdropSource.onParams(engine,engine.params);ClockBackdropSource.onRegistration(engine);engine.addBlurDrawable();ClockBackdropSource.onRegistration(engine);}
        void image(){engine.completeForCheck(bitmap);engine.preDrawContent();ClockBackdropSource.onDrawn(engine);}
        ClockBackdropSource.Frame frame(){return ClockBackdropSource.snapshot(clock);}
        void ready(){observe();add();image();}
    }
    public static void main(String[] args)throws Exception {
        provenance();transport();frames();events();mutableContent();promotion();scopes();ownership();
        reset();System.out.println("ClockBackdropSourceCheck: "+checks+" checks passed (captured native shape; device shader pending)");
    }
    private static void provenance(){
        reset();Scene s=new Scene();s.add();s.image();check(s.frame()==null,"unobserved existing ID is never adopted");s.observe();check(s.frame()==null,"registered ID cannot be adopted after its first add");
        reset();s=new Scene();s.engine.params.radius=1;s.observe();s.add();s.image();check(s.frame()==null,"nonzero constructor radius cannot prove pristine ID");
        reset();s=new Scene();s.engine.enableShader=false;s.observe();s.engine.enableShader=true;s.add();s.image();check(s.frame()==null,"constructor renderer must already be shader mode");
        reset();s=new Scene();s.engine.params.brightnessMinimum=.1f;s.observe();s.engine.params.brightnessMinimum=0;s.add();s.image();check(s.frame()==null,"unknown constructor brightness is never accepted");
        reset();s=new Scene();s.engine.completeForCheck(s.bitmap);s.engine.preDrawContent();s.observe();s.add();check(s.frame()==null,"a constructor carrying a previously promoted image is rejected");
        reset();s=new Scene();s.host.getResources().entryNames.put(903,"scrim_front");s.ready();check(s.frame()==null,"front scrim is outside exact behind scope");
        reset();s=new Scene();s.host.setId(View.NO_ID);s.ready();check(s.frame()==null,"unnamed native view cannot become a source");
        reset();s=new Scene();s.observe();s.engine.params.radius=40;s.engine.isRegistered=true;ClockBackdropSource.onRegistration(s.engine);s.image();check(s.frame()==null,"registration with missing BEFORE event is rejected");
        reset();s=new Scene();s.observe();check(s.frame()==null,"constructor alone does not publish a buffer");s.add();check(s.frame()==null,"registration alone does not publish pixels");s.image();check(s.frame()!=null,"first observed pure native image becomes available");
        reset();s=new Scene();s.engine.params.blend.blendMode=0x55000000;s.engine.params.blend.blendColorA=0x2299aabb;s.engine.params.blend.fgBlendColorB=0x33aabbcc;
        s.observe();s.engine.isRegistered=true;ClockBackdropSource.onRegistration(s.engine);check(s.frame()==null,"Java blend values cannot bypass missing registration history");
        reset();s=new Scene();s.engine.params.blend.blendMode=0x55000000;s.engine.params.blend.fgBlendColorB=0x33aabbcc;s.observe();s.engine.params.radius=40;ClockBackdropSource.onParams(s.engine,s.engine.params);ClockBackdropSource.onRegistration(s.engine);s.engine.isRegistered=true;ClockBackdropSource.onRegistration(s.engine);s.image();
        check(s.frame()!=null,"original Java shader mix is allowed when actual service slots are zero");check(s.engine.params.blend.blendMode==0x55000000&&s.engine.params.blend.fgBlendColorB==0x33aabbcc,"source never clears original background Java mix");
    }
    private static void transport(){
        for(int index=7;index<=27;index++)if(index<=15||index>=19){
            reset();Scene s=new Scene();s.ready();float[] actual=s.engine.params.toFloatArray(true);actual[index]=.25f;ClockBackdropSource.onEncoded(s.engine.params,actual);
            check(s.frame()==null,"actual native mix slot "+index+" poisons ID");ClockBackdropSource.onEncoded(s.engine.params,s.engine.params.toFloatArray(true));check(s.frame()==null,"clean later transport never adopts contaminated ID");
        }
        for(int index:new int[]{0,2,17,18,28,29}){
            reset();Scene s=new Scene();s.ready();float[] actual=s.engine.params.toFloatArray(true);actual[index]=Float.NaN;ClockBackdropSource.onEncoded(s.engine.params,actual);check(s.frame()==null,"nonfinite transport "+index+" is rejected");
        }
        for(float[] malformed:new float[][]{null,new float[0],new float[29],new float[31]}){
            reset();Scene s=new Scene();s.ready();ClockBackdropSource.onEncoded(s.engine.params,malformed);check(s.frame()==null,"unknown transport length is never treated as pure");
        }
        for(float[] brightness:new float[][]{{.1f,1},{0,.9f},{-1,1},{0,2}}){
            reset();Scene s=new Scene();s.ready();float[] actual=s.engine.params.toFloatArray(true);actual[28]=brightness[0];actual[29]=brightness[1];ClockBackdropSource.onEncoded(s.engine.params,actual);check(s.frame()==null,"nondefault brightness scope poisons ID");
        }
        reset();Scene s=new Scene();s.ready();Log.DEBUG=true;BlurDrawableManager.sBlendTypeValue=2;ClockBackdropSource.onEncoded(s.engine.params,s.engine.params.toFloatArray(true));Log.DEBUG=false;BlurDrawableManager.sBlendTypeValue=-1;
        check(s.frame()==null,"actual encoding catches transient DEBUG2 even when reset before snapshot");
        reset();s=new Scene();s.observe();ClockBackdropSource.onRegistration(s.engine);Log.DEBUG=true;BlurDrawableManager.sBlendTypeValue=2;s.engine.isRegistered=true;ClockBackdropSource.onRegistration(s.engine);Log.DEBUG=false;BlurDrawableManager.sBlendTypeValue=-1;s.image();check(s.frame()==null,"DEBUG initialised inside first add is checked after registration");
        reset();s=new Scene();s.ready();Log.DEBUG=true;BlurDrawableManager.sBlendTypeValue=2;check(s.frame()==null,"unannounced DEBUG2 change rejects source at snapshot");Log.DEBUG=false;BlurDrawableManager.sBlendTypeValue=-1;check(s.frame()==null,"snapshot DEBUG contamination is permanent");
        reset();s=new Scene();s.ready();BlurParam upcoming=new BlurParam();upcoming.brightnessMaximum=.8f;ClockBackdropSource.onParams(s.engine,upcoming);check(s.frame()==null,"upcoming params are checked before native copy");
        reset();s=new Scene();s.ready();BlurParam replacement=new BlurParam();s.engine.params=replacement;ClockBackdropSource.onParams(s.engine,replacement);float[] actual=replacement.toFloatArray(true);actual[19]=1;ClockBackdropSource.onEncoded(replacement,actual);check(s.frame()==null,"new drawing param identity stays associated with the existing source");
        reset();s=new Scene();s.ready();s.engine.params.radius=41;actual=s.engine.params.toFloatArray(true);actual[19]=1;ClockBackdropSource.onEncoded(s.engine.params,actual);check(s.frame()==null,"native copyFrom changing parameter content/hash cannot evade actual encoding observer");
        reset();s=new Scene();s.ready();ClockBackdropSource.onParams(s.engine,new Object());check(s.frame()==null,"unknown param type is rejected");
        reset();s=new Scene();s.ready();ClockBackdropSource.onShaderChanged(s.engine,false);s.engine.enableShader=true;check(s.frame()==null,"renderer false then true cannot reuse an ID with mixed history");
        reset();Scene a=new Scene(),b=new Scene();a.ready();b.ready();BlurParam shared=new BlurParam();ClockBackdropSource.onParams(a.engine,shared);ClockBackdropSource.onParams(b.engine,shared);float[] bad=shared.toFloatArray(true);bad[7]=1;ClockBackdropSource.onEncoded(shared,bad);check(a.frame()==null&&b.frame()==null,"a shared parameter poisons every associated source, not an arbitrary last one");
        reset();s=new Scene();s.ready();float[] foreign=s.engine.params.toFloatArray(true);foreign[7]=1;ClockBackdropSource.onEncoded(new BlurParam(),foreign);check(s.frame()!=null,"unrelated system blur transport cannot poison behind source");
    }
    private static void frames(){
        reset();Scene s=new Scene();s.ready();ClockBackdropSource.Frame first=s.frame();check(first!=null&&first.bitmap==s.bitmap&&first.host==s.host,"frame borrows existing bitmap and exact source host");
        check(first.bounds.left==0&&first.bounds.bottom==160,"source bounds are drawable bounds");
        check(first.tileMode==Shader.TileMode.MIRROR,"native default wrapping is transported instead of guessing CLAMP");
        for(int i=0;i<512;i++)check(s.frame()==first,"stable source reuses immutable Frame");
        s.clock.layout(0,0,2000,2000);check(s.frame()==first,"large TextView outer bounds are not mistaken for actual glyph coverage");
        s.engine.bitmapMatrix.setScale(2,3);s.engine.bitmapMatrix.postTranslate(-12,27);ClockBackdropSource.onDrawn(s.engine);ClockBackdropSource.Frame transformed=s.frame();
        check(transformed!=first&&transformed.revision>first.revision,"changed native transform refreshes frame");float[] point={5,4};transformed.bitmapToDrawable.mapPoints(point);check(point[0]==-2&&point[1]==39,"native forward scale and drawable translation are retained exactly once");
        point=new float[]{5,4};first.bitmapToDrawable.mapPoints(point);check(point[0]==5&&point[1]==4,"previous frame matrix is an owned snapshot, not mutable native matrix");
        s.engine.setBounds(7,11,220,155);ClockBackdropSource.onDrawn(s.engine);ClockBackdropSource.Frame bounded=s.frame();check(bounded!=transformed&&bounded.bounds.left==7&&transformed.bounds.left==0,"new bounds do not mutate cached frames");
        s.engine.viewAxisRemapEnabled=true;s.engine.viewAxisRemapRotation=1;check(s.frame()==null,"unknown native view-axis remap cannot silently misalign glyphs");s.engine.viewAxisRemapRotation=0;check(s.frame()!=null,"ordinary portrait axis remains available");s.engine.viewAxisRemapEnabled=false;
        s.engine.bitmapMatrix.setScale(0,1);check(s.frame()==null,"singular transform is rejected");s.engine.bitmapMatrix.setTranslate(3,4);check(s.frame()!=null,"transient geometry failure does not poison pure blur history");
        s.engine.setBounds(0,0,0,0);check(s.frame()==null,"empty drawable bounds are unavailable");s.engine.setBounds(0,0,240,160);
        ClockBackdropSource.Frame previous=s.frame();for(BlurParam.Wrapping wrapping:BlurParam.Wrapping.values()){
            s.engine.params.base.wrapping=wrapping;ClockBackdropSource.Frame wrapped=s.frame();check(wrapped!=null&&wrapped.tileMode==wrapping.toShaderTileMode(),"actual current native wrapping is used: "+wrapping);
            if(previous.tileMode!=wrapped.tileMode)check(previous!=wrapped&&previous.revision<wrapped.revision,"wrapping-only change refreshes cached frame");
            check(s.frame()==wrapped,"stable wrapping keeps cached frame identity");previous=wrapped;
        }
        s.engine.params.base.wrapping=null;check(s.frame()==null,"unknown current wrapping is rejected instead of guessing a shader mode");s.engine.params.base.wrapping=BlurParam.Wrapping.MIRRORED_REPEAT;
        s.engine.params.radius=0;check(s.frame()==null,"zero active native radius cannot lend a stale application blur");s.engine.params.radius=40;check(s.frame()!=null,"resuming original native blur needs no private refresh request");
        s.clock.attached=false;check(s.frame()==null,"detached clock cannot borrow a frame");s.clock.attached=true;s.host.attached=false;check(s.frame()==null,"detached native source is unavailable");s.host.attached=true;
        View foreign=new View(null);check(ClockBackdropSource.snapshot(foreign)==null,"source cannot cross a root/window boundary");
        Scene other=new Scene();other.ready();check(s.frame()!=null&&other.frame()!=null,"distinct roots do not make sources ambiguous");s.root.addView(other.host);check(s.frame()==null,"multiple native behind IDs in same root are rejected");other.host.attached=false;check(s.frame()!=null,"detaching duplicate source resolves ambiguity without recreating original");
        s.engine.isRegistered=false;check(s.frame()==null,"removed native source cannot yield stale cached pixels");
    }
    private static void events(){
        reset();Scene s=new Scene();final int[] notifications={0};Runnable listener=()->notifications[0]++;ClockBackdropSource.setInvalidationCallback(listener);s.ready();check(notifications[0]==1,"first promoted frame notifies once");
        int stable=notifications[0];for(int i=0;i<1000;i++){s.engine.preDrawContent();ClockBackdropSource.onDrawn(s.engine);ClockBackdropSource.onImageChanged(s.engine);}check(notifications[0]==stable,"stable preDraw and setter events never create a redraw feedback loop");
        s.engine.bitmapMatrix.postTranslate(0,1);ClockBackdropSource.onDrawn(s.engine);check(notifications[0]==stable+1,"geometry-only native movement invalidates clock exactly once");ClockBackdropSource.onDrawn(s.engine);check(notifications[0]==stable+1,"unchanged matrix does not notify again");
        s.engine.completeForCheck(s.bitmap);s.engine.preDrawContent();ClockBackdropSource.onDrawn(s.engine);check(notifications[0]==stable+2,"new native ImageInfo using same Bitmap invalidates material");
        s.engine.drawing=null;ClockBackdropSource.onImageChanged(s.engine);check(notifications[0]==stable+3&&s.frame()==null,"released drawing image clears visible material");ClockBackdropSource.onImageChanged(s.engine);check(notifications[0]==stable+3,"repeated null release is stable");
        ClockBackdropSource.onShaderChanged(s.engine,false);check(notifications[0]==stable+4,"permanent source contamination schedules restoration");ClockBackdropSource.onShaderChanged(s.engine,false);check(notifications[0]==stable+4,"repeated poison does not loop");
        ClockBackdropSource.clear();check(s.frame()==null,"lifecycle clear drops every observed ID");ClockBackdropSource.onDrawn(s.engine);check(notifications[0]==stable+4,"cleared source cannot retain or invoke old listener");
        reset();Scene active=new Scene();final int[] availability={0};Runnable availabilityListener=()->availability[0]++;ClockBackdropSource.setInvalidationCallback(availabilityListener);active.ready();
        active.engine.params.radius=0;ClockBackdropSource.onDrawn(active.engine);check(availability[0]==2&&active.frame()==null,"native blur stopping invalidates glyph even when old image identity remains");
        for(int i=0;i<30;i++)ClockBackdropSource.onDrawn(active.engine);check(availability[0]==2,"stable inactive blur never repeats restoration");
        active.engine.params.radius=40;ClockBackdropSource.onDrawn(active.engine);check(availability[0]==3&&active.frame()!=null,"native blur resuming invalidates once without requesting capture");
        active.engine.params.base.wrapping=BlurParam.Wrapping.CLAMP;ClockBackdropSource.onDrawn(active.engine);check(availability[0]==4,"native wrapping-only change invalidates material exactly once");ClockBackdropSource.onDrawn(active.engine);check(availability[0]==4,"stable wrapping generates no redraw loop");
    }
    private static void mutableContent(){
        reset();Scene s=new Scene();final int[] notifications={0};Runnable listener=()->notifications[0]++;
        ClockBackdropSource.setInvalidationCallback(listener);s.ready();ClockBackdropSource.Frame first=s.frame();
        Object image=s.engine.drawing;int before=notifications[0];
        s.bitmap.eraseColor(0xff2764b8);ClockBackdropSource.onDrawn(s.engine);
        check(s.engine.drawing==image,"mutable update keeps the same real image wrapper");
        check(notifications[0]==before+1,"same native image with changed bitmap generation invalidates its borrowers once");
        ClockBackdropSource.Frame changed=s.frame();
        check(changed!=first&&changed.revision>first.revision,"mutable native bitmap content cannot reuse the old immutable frame");
        check(changed.contentRevision>first.contentRevision&&changed.bitmapGeneration!=first.bitmapGeneration,
                "immutable frames retain separate native content and bitmap generation markers");
        for(int i=0;i<30;i++)ClockBackdropSource.onDrawn(s.engine);
        check(notifications[0]==before+1&&s.frame()==changed,"stable bitmap generation never creates continuous invalidation");
        s.engine.bitmapMatrix.postTranslate(1,0);ClockBackdropSource.onDrawn(s.engine);ClockBackdropSource.Frame moved=s.frame();
        check(moved!=changed&&moved.contentRevision==changed.contentRevision,"native geometry movement does not create a new image delivery lifetime");
        s.bitmap.eraseColor(0xffd4a147);ClockBackdropSource.Frame polled=s.frame();
        check(polled!=moved&&polled.contentRevision>moved.contentRevision,"snapshot also detects bitmap generation when the promotion callback is not delivered");
        java.util.Map<String,Object> diagnostics=ClockBackdropSource.diagnostics();
        check(diagnostics.size()==28&&((Number)diagnostics.get("generation_changes")).longValue()==2,
                "probe includes bounded generation counters from actual source events");
        for(Object value:diagnostics.values())check(value instanceof Number||value instanceof String,"source diagnostics expose only scalar metadata");
    }
    private static void promotion()throws Exception {
        reset();Scene s=new Scene();s.ready();ClockBackdropMaterial material=new ClockBackdropMaterial();
        ClockBackdropSource.Frame beforePromotion=s.frame();material.apply(s.clock,beforePromotion,0xffffffff,.2f);
        final java.util.concurrent.atomic.AtomicInteger notifications=new java.util.concurrent.atomic.AtomicInteger();
        Runnable listener=notifications::incrementAndGet;ClockBackdropSource.setInvalidationCallback(listener);
        Bitmap next=Bitmap.createBitmap(120,80,Bitmap.Config.ARGB_8888);s.engine.completeForCheck(next);
        java.util.concurrent.CountDownLatch released=new java.util.concurrent.CountDownLatch(1),resume=new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<Throwable> failure=new java.util.concurrent.atomic.AtomicReference<>();
        s.engine.afterReleaseForCheck=()->{
            ClockBackdropSource.onImageChanged(s.engine);s.bitmap.recycle();released.countDown();
            try {if(!resume.await(5,java.util.concurrent.TimeUnit.SECONDS))throw new AssertionError("promotion reader did not finish");}
            catch(InterruptedException interrupted){Thread.currentThread().interrupt();throw new AssertionError(interrupted);}
            s.engine.bitmapMatrix.postTranslate(7,9);
        };
        Thread render=new Thread(()->{
            try(ClockBackdropSource.DrawScope scope=ClockBackdropSource.beginDraw(s.engine)) {
                s.engine.preDrawContent();scope.complete();
            }catch(Throwable problem){failure.set(problem);}
        },"native-promotion-check");render.setDaemon(true);render.start();
        check(released.await(2,java.util.concurrent.TimeUnit.SECONDS),"real release-to-assignment gap is reached on the render thread");
        java.util.concurrent.FutureTask<Boolean> reader=new java.util.concurrent.FutureTask<>(()->{
            ClockBackdropSource.Frame frame=s.frame();
            int writes=s.clock.renderEffectWrites,programs=android.graphics.RuntimeShader.creations,samplers=android.graphics.BitmapShader.creations;
            boolean result=frame!=null&&frame.deferred()&&frame.bitmap==null&&frame.host==null;
            boolean raced=material.apply(s.clock,beforePromotion,0xffffffff,.2f);
            result&=!raced&&writes==s.clock.renderEffectWrites;
            boolean applied=material.apply(s.clock,frame,0xffffffff,.2f);
            return result&&!applied&&writes==s.clock.renderEffectWrites&&programs==android.graphics.RuntimeShader.creations&&samplers==android.graphics.BitmapShader.creations;
        });
        Thread ui=new Thread(reader,"ui-promotion-check");ui.setDaemon(true);ui.start();
        try {
            check(reader.get(2,java.util.concurrent.TimeUnit.SECONDS),"UI read returns without waiting for RT, sampling released pixels, submitting an old effect or switching to white");
            check(notifications.get()==0,"internal release does not schedule an intermediate unavailable image");
        }finally {resume.countDown();render.join(3000);s.engine.afterReleaseForCheck=null;}
        check(!render.isAlive()&&failure.get()==null,"native promotion completes without a cross-thread observation lock");
        check(notifications.get()==1,"completion schedules exactly one final refresh for all deferred borrowers");
        ClockBackdropSource.Frame completed=s.frame();
        check(completed!=null&&!completed.deferred()&&completed.bitmap==next,"final snapshot contains only the newly promoted native image");
        float[] point={0,0};completed.bitmapToDrawable.mapPoints(point);
        check(point[0]==7&&point[1]==9,"source geometry and content are promoted together");
        check(material.apply(s.clock,completed,0xffffffff,.2f),"final callback can install the completed image after a deferred update");
        check(((android.graphics.BitmapShader)s.clock.renderEffect.shader.inputs.get("background")).bitmap==next,"completed material no longer binds the released image");
        s.engine.releaseDrawingBitmap();ClockBackdropSource.onImageChanged(s.engine);
        check(notifications.get()==2&&s.frame()==null,"standalone release remains immediately unavailable");
        material.clear(s.clock);

        reset();Scene raced=new Scene();raced.ready();final java.util.concurrent.atomic.AtomicInteger delivered=new java.util.concurrent.atomic.AtomicInteger();
        Runnable racedListener=delivered::incrementAndGet;ClockBackdropSource.setInvalidationCallback(racedListener);
        raced.engine.afterReleaseForCheck=()->ClockBackdropSource.onImageChanged(raced.engine);
        raced.engine.completeForCheck(Bitmap.createBitmap(120,80,Bitmap.Config.ARGB_8888));
        java.util.concurrent.CountDownLatch bodyFinished=new java.util.concurrent.CountDownLatch(1);
        Thread racingRender=new Thread(()->{
            try(ClockBackdropSource.DrawScope scope=ClockBackdropSource.beginDraw(raced.engine)) {
                raced.engine.preDrawContent();scope.complete();bodyFinished.countDown();
            }catch(Throwable problem){failure.set(problem);}
        },"native-final-observer-check");racingRender.setDaemon(true);
        synchronized(raced.engine.getDataLock()) {
            racingRender.start();check(bodyFinished.await(2,java.util.concurrent.TimeUnit.SECONDS),"native body can complete before its final observation obtains dataLock");
            long until=System.nanoTime()+2_000_000_000L;
            while(racingRender.getState()!=Thread.State.BLOCKED&&racingRender.isAlive()&&System.nanoTime()<until)Thread.yield();
            check(racingRender.getState()==Thread.State.BLOCKED,"final observer is parked only on the existing native dataLock");
            ClockBackdropSource.Frame early=raced.frame();
            check(early!=null&&!early.deferred(),"UI may read the final promoted image before the final observer runs");
            check(delivered.get()==0,"reading the completed image itself never manufactures a second intermediate callback");
        }
        racingRender.join(3000);raced.engine.afterReleaseForCheck=null;
        check(!racingRender.isAlive()&&failure.get()==null&&delivered.get()==1,
                "coalesced internal release guarantees one final callback even when another borrower read the new image first");

        reset();Scene failed=new Scene();failed.ready();final int[] finalEvents={0};Runnable observer=()->finalEvents[0]++;
        ClockBackdropSource.setInvalidationCallback(observer);
        try(ClockBackdropSource.DrawScope scope=ClockBackdropSource.beginDraw(failed.engine)) {
            failed.engine.releaseDrawingBitmap();ClockBackdropSource.onImageChanged(failed.engine);
            // No complete(): an exceptional native return must not leave a live promotion marker.
        }
        check(finalEvents[0]==1&&failed.frame()==null,"exceptional promotion restores unavailable state instead of indefinitely deferring");
        failed.engine.completeForCheck(failed.bitmap);
        try(ClockBackdropSource.DrawScope scope=ClockBackdropSource.beginDraw(failed.engine)){failed.engine.preDrawContent();scope.complete();}
        check(failed.frame()!=null&&finalEvents[0]==2,"next successful native draw recovers from an incomplete promotion");
        ClockBackdropSource.Frame stale;
        try(ClockBackdropSource.DrawScope scope=ClockBackdropSource.beginDraw(failed.engine)) {
            stale=failed.frame();ClockBackdropSource.onShaderChanged(failed.engine,false);
            check(stale!=null&&!stale.deferred()&&failed.frame()==null,"poison invalidates even an already returned promotion marker immediately");scope.complete();
        }
        reset();Scene cleared=new Scene();cleared.ready();
        try(ClockBackdropSource.DrawScope scope=ClockBackdropSource.beginDraw(cleared.engine)) {
            stale=cleared.frame();ClockBackdropSource.clear();
            check(stale!=null&&!stale.deferred()&&cleared.frame()==null,"lifecycle clear invalidates promotion markers across threads/epochs");scope.complete();
        }
    }
    private static void scopes()throws Exception {
        reset();Scene good=new Scene(),nested=new Scene();nested.host.getResources().entryNames.put(903,"scrim_front");
        try(ClockBackdropSource.Scope outer=ClockBackdropSource.beginHelper(good.helper)){
            try(ClockBackdropSource.Scope unrelated=ClockBackdropSource.beginHelper(nested.helper)){ClockBackdropSource.created(nested.wrapper);}
            Thread worker=new Thread(()->ClockBackdropSource.created(good.wrapper));worker.start();worker.join();
            check(good.frame()==null,"helper context does not leak across native threads");ClockBackdropSource.created(good.wrapper);
        }
        good.add();good.image();nested.add();nested.image();check(good.frame()!=null&&nested.frame()==null,"nested unrelated helper masks outer host and restores correct scope");
        reset();Scene s=new Scene();ClockBackdropSource.Scope scope=ClockBackdropSource.beginHelper(s.helper);scope.close();scope.close();ClockBackdropSource.created(s.wrapper);s.add();s.image();check(s.frame()==null,"closed scope cannot authorise a later wrapper");
        reset();s=new Scene();try(ClockBackdropSource.Scope stale=ClockBackdropSource.beginHelper(s.helper)){
            Thread clear=new Thread(ClockBackdropSource::clear);clear.start();clear.join();ClockBackdropSource.created(s.wrapper);
        }
        s.add();s.image();check(s.frame()==null,"a helper active on another thread cannot survive lifecycle clear");
    }
    private static void ownership(){
        reset();Scene s=new Scene();s.ready();int ids=ContinuousBlurDrawable.nextId,submissions=s.engine.submissionsForCheck.size(),removes=ContinuousBlurDrawable.removeCalls;
        int creates=Bitmap.creations,reads=Bitmap.reads,copies=Bitmap.copies,scales=Bitmap.scales;Matrix nativeMatrix=s.engine.bitmapMatrix;Object nativeShader=s.engine.drawableShader.shader;
        for(int i=0;i<256;i++){ClockBackdropSource.Frame frame=s.frame();check(frame!=null&&frame.bitmap==s.bitmap,"every frame keeps borrowed bitmap identity");ClockBackdropSource.onDrawn(s.engine);}
        ClockBackdropSource.clear();check(ContinuousBlurDrawable.nextId==ids&&ContinuousBlurDrawable.removeCalls==removes&&s.engine.submissionsForCheck.size()==submissions,"source never creates, adds, updates, removes or releases native IDs");
        check(Bitmap.creations==creates&&Bitmap.reads==reads&&Bitmap.copies==copies&&Bitmap.scales==scales&&s.bitmap.recycleCalls==0,"source never creates/copies/scales/reads/recycles bitmap pixels");
        check(s.engine.bitmapMatrix==nativeMatrix&&s.engine.drawableShader.shader==nativeShader&&s.engine.enableShader,"source never replaces shared shader/matrix/renderer");
        check(s.engine.isRegistered&&s.engine.drawing!=null,"clear releases only observation state, preserving original native resources");
    }
}
