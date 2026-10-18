package dev.puitheme;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.RenderNode;
import android.os.Handler;
import android.view.Display;
import android.view.SurfaceControl;
import android.view.SurfaceControlViewHost;
import com.android.systemui.shade.NotificationShadeWindowView;
import com.oplus.app.OplusWindowInfo;
import com.oplus.graphics.OplusRenderEffect;
import com.oplus.systemui.keyguard.anim.OplusWallpaperAnimControllerImpl;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Structural/lifecycle tests only. Synthetic backdrop submission is not visual proof. */
public final class WallpaperSurfaceBlurCheck {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static void near(float expected,float actual,String message){check(Math.abs(expected-actual)<.001f,message+": "+actual);}
    private static final class WallpaperContext extends Context {
        final Resources resources=new Resources();final Display display=new Display();
        WallpaperContext(){resources.getDisplayMetrics().density=3f;resources.getConfiguration().orientation=1;}
        @Override public Resources getResources(){return resources;}
        @Override public Display getDisplay(){return display;}
    }
    private static final class Fixture {
        final WallpaperSurfaceBlur helper=new WallpaperSurfaceBlur();
        final WallpaperContext context=new WallpaperContext();
        final NotificationShadeWindowView shade=new NotificationShadeWindowView(context);
        final OplusWallpaperAnimControllerImpl controller=new OplusWallpaperAnimControllerImpl();
        final SurfaceControl wallpaper=new SurfaceControl("external wallpaper window",null);
        final OplusWindowInfo info=new OplusWindowInfo();
        Fixture(){
            wallpaper.alpha=.91f;info.mSurfaceControl=wallpaper;
            info.extension.putFloat("HScale",1.2f);info.extension.putFloat("VScale",1.2f);
            info.extension.putForWallpaperCheck("offset",new Point(-144,-317));
            controller._lockScreenWallpaperWindowInfoFlow.value=info;
        }
        void enable(){helper.onShade(shade);helper.onController(controller);helper.configure(true,true,.75f,16f,56f,32f,0x22000000);}
        void event(){helper.onController(controller);Handler.drainForCheck();}
        void retire(){helper.releaseRuntime();Handler.drainForCheck();}
    }
    private static void reset(){
        Handler.postedForCheck.clear();SurfaceControl.resetForCheck();SurfaceControlViewHost.hostsForCheck.clear();SurfaceControlViewHost.failForCheck=false;
        RenderNode.nodes.clear();RenderNode.rejectBackground=false;android.graphics.RenderEffect.fail=false;
        OplusRenderEffect.created=0;OplusRenderEffect.gradientsForCheck.clear();
    }
    private static SurfaceControl group(){for(SurfaceControl sc:SurfaceControl.createdForCheck)if(sc.name.equals("C17 external wallpaper status backdrop")&&sc.valid)return sc;throw new AssertionError("No external wallpaper group");}
    private static SurfaceControl effect(){for(SurfaceControl sc:SurfaceControl.createdForCheck)if(sc.name.equals("C17 wallpaper compositor feather")&&sc.valid)return sc;throw new AssertionError("No compositor wallpaper feather");}
    private static int regionWrites(){int count=0;for(SurfaceControl.Operation op:SurfaceControl.operationsForCheck)if(op.name.equals("regions"))count++;return count;}
    private static int commits(){int n=0;for(SurfaceControl.Transaction t:SurfaceControl.transactionsForCheck)n+=t.applyCalls;return n;}
    private static int groups(){int n=0;for(SurfaceControl sc:SurfaceControl.createdForCheck)if(sc.name.equals("C17 external wallpaper status backdrop"))n++;return n;}
    private static void borrowedUntouched(Fixture f){
        check(SurfaceControl.borrowedMutationAttemptsForCheck==0,"no borrowed wallpaper mutation attempt");
        near(.91f,f.wallpaper.alpha,"native wallpaper dim alpha is preserved");
        check(f.wallpaper.releaseCalls==0&&f.wallpaper.removeCalls==0&&f.wallpaper.valid,"borrowed parent never released or removed");
        for(SurfaceControl.Operation op:SurfaceControl.operationsForCheck)if(op.target!=null)check(op.target.owned,"transactions target owned surfaces exclusively");
        for(SurfaceControl.Transaction t:SurfaceControl.transactionsForCheck)check(t.closeCalls==1,"every transaction closes once");
        near(1f,f.shade.getAlpha(),"shade foreground alpha untouched");
        check(f.shade.renderEffectWrites==0&&f.shade.getChildCount()==0,"shade tree and effect untouched");
    }
    private static void allOwnedRetired(){
        for(SurfaceControl sc:SurfaceControl.createdForCheck)if(sc.owned)check(!sc.valid&&sc.releaseCalls==1,"each owned group/host/package handle releases once");
        for(SurfaceControlViewHost host:SurfaceControlViewHost.hostsForCheck)check(host.releasesForCheck==1,"each host retires once");
        for(RenderNode node:RenderNode.nodes)check(node.backgroundEffect==null,"owned backdrop effect cleared");
    }
    private static void bridgeAndStableNativeFrames() throws Exception {
        reset();Fixture f=new Fixture();f.enable();
        check(groups()==0,"creating a host is deferred outside native drawing/coroutine callback");check(Handler.postedForCheck.size()==1,"independent native events coalesce on main");
        Handler.drainForCheck();SurfaceControl g=group();
        check(g.parent==f.wallpaper,"only actual wallpaper window owns the backdrop");check(g.relativeTo==f.wallpaper&&g.relativeLayer==0,"below OEM foreground layers +1/+2");
        near(.75f,g.alpha,"native visibility applied once on owned group");check(!g.hidden,"external wallpaper effect shown");
        float[] matrix=new float[9];g.matrix.getValues(matrix);
        near(1f/1.2f,matrix[0],"native horizontal zoom cancelled");near(1f/1.2f,matrix[4],"native vertical zoom cancelled");
        near(120f,matrix[2],"native horizontal offset cancelled");near(317f/1.2f,matrix[5],"native vertical offset cancelled");
        near(0f,matrix[2]*1.2f-144f,"effect starts at physical screen x=0");near(0f,matrix[5]*1.2f-317f,"effect starts at physical screen y=0");
        check(g.crop.right==1440&&g.crop.bottom==264,"range and transition are physical display pixels");
        SurfaceControlViewHost host=SurfaceControlViewHost.hostsForCheck.get(0);
        check((host.layoutForCheck.flags&24)==24,"own window cannot take focus or intercept touches");
        check((host.layoutForCheck.flags&android.view.WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)!=0,"native backdrop opts in to hardware rendering");
        check(host.layoutForCheck.format==-3,"host buffer preserves alpha");
        SurfaceControl blur=effect();check(blur.parent==g&&blur.layer==0&&blur.kind==1,"owned compositor effect lives below the mask inside wallpaper group");
        check(blur.blurRegions.length>12&&blur.blurRegions.length<=255,"native alpha runs remove the old coarse eleven-strip transition within a fixed budget");
        float[] core=blur.blurRegions[0];near(48f,core[0],"core radius matches density-scaled setting");near(1f,core[1],"core remains fully blurred");check(core[5]>=168f,"opaque core includes the configured boundary");
        float previous=1f;int bottom=(int)core[5];
        for(int i=1;i<blur.blurRegions.length;i++){
            float[] region=blur.blurRegions[i];check(region.length==36,"audited local OEM wire format includes native parameters");
            near(48f,region[0],"every region reuses a single blur radius");near(bottom,region[3],"feather has no geometry gaps");bottom=(int)region[5];
            check(region[1]<previous&&region[1]>0f,"bounded feather opacity decreases toward sharp wallpaper");previous=region[1];
            near(0f,region[21],"compositor does not add a native material tint");near(1f,region[20],"native sampling zoom remains identity");
        }
        check(bottom<=264&&264-bottom<5,"only visually zero-alpha final pixels are omitted");
        check(OplusRenderEffect.created==0,"empty HWUI host never receives a wallpaper gradient effect");
        android.view.ViewGroup own=(android.view.ViewGroup)host.getView();
        Method layout=own.getClass().getDeclaredMethod("onLayout",boolean.class,int.class,int.class,int.class,int.class);layout.setAccessible(true);layout.invoke(own,true,0,0,1440,264);
        check(own.getChildCount()==0,"mask host contains no empty background-sampling child views");
        Field plan=WallpaperSurfaceBlur.class.getDeclaredField("currentPlan");plan.setAccessible(true);Object cached=plan.get(f.helper);
        int before=commits(),builds=groups(),effects=regionWrites();float[][] cachedRegions=blur.blurRegions;
        for(int i=0;i<100;i++)f.event();
        check(commits()==before&&groups()==builds,"unchanged native updates neither create surfaces nor recommit");check(effects==regionWrites()&&blur.blurRegions==cachedRegions&&plan.get(f.helper)==cached,"compositor payload and geometry reused across stable events");
        f.helper.configure(true,true,.6f,16f,56f,32f,0x55010203);Handler.drainForCheck();check(effect().blurRegions==cachedRegions&&regionWrites()==effects,"color/visibility-only change cannot rebuild or resubmit blur regions");before=commits();
        f.helper.onFlowChanged(new kotlinx.coroutines.flow.StateFlowImpl());check(Handler.postedForCheck.isEmpty(),"unrelated StateFlow cannot schedule capture/host updates");
        f.helper.onWindowInfoTask(new Object());check(Handler.postedForCheck.isEmpty(),"unrelated coroutine cannot become wallpaper source");
        f.helper.onWindowInfoTask(new com.oplus.systemui.keyguard.anim.OplusWallpaperAnimControllerImpl$onWallpaperSurfaceChanged$3(f.controller));
        check(!Handler.postedForCheck.isEmpty(),"audited wallpaper coroutine publishes its actual controller");Handler.drainForCheck();
        check(groups()==builds&&commits()==before,"unchanged directed callback has no GPU rebuild");
        f.helper.onWindowInfoTask(new com.oplus.systemui.keyguard.anim.OplusWallpaperAnimControllerImpl$onPictorialWallpaperSurfaceChanged$2(f.controller));
        check(!Handler.postedForCheck.isEmpty(),"audited pictorial coroutine publishes its actual controller");Handler.drainForCheck();
        borrowedUntouched(f);check(f.helper.summary().contains("externalBackdropVisual=unverified"),"synthetic submission never claims visual success");f.retire();allOwnedRetired();
    }
    private static void visibilityDisableAndSurfaceReplacement(){
        reset();Fixture f=new Fixture();f.enable();Handler.drainForCheck();SurfaceControl first=group();int effects=regionWrites();float[][] cached=effect().blurRegions;
        f.controller.wallpaperVisiable=false;f.event();check(first.hidden,"native invisible wallpaper hides own effect");
        f.controller.wallpaperVisiable=true;f.event();check(!first.hidden&&effects==regionWrites()&&effect().blurRegions==cached,"native visibility restores cached live compositor payload");
        f.helper.configure(true,true,0f,16f,56f,32f,0x22000000);Handler.drainForCheck();check(first.hidden,"dismiss/AOD native scrim blackout hides owned effect");
        f.helper.configure(true,true,.4f,16f,56f,32f,0x22000000);Handler.drainForCheck();near(.4f,first.alpha,"native scrim transparency drives own alpha");
        SurfaceControl copiedParent=new SurfaceControl("copied native wallpaper handle",null);copiedParent.sameLayerForCheck=f.wallpaper;f.info.mSurfaceControl=copiedParent;f.event();check(group()==first,"copied native handle for same surface does not rebuild");
        SurfaceControl replacement=new SurfaceControl("new wallpaper window",null);f.info.mSurfaceControl=replacement;f.event();check(!first.valid&&group().parent==replacement,"wallpaper replacement retires old subtree and adopts new exact parent");
        f.helper.configure(false,true,.4f,16f,56f,32f,0x22000000);check(group().valid,"disable is safely deferred");Handler.drainForCheck();allOwnedRetired();
        f.helper.configure(true,true,.4f,16f,56f,32f,0x22000000);Handler.drainForCheck();check(group().valid,"re-enable rebuilds owned resources");
        f.helper.onShadeDetached(f.shade);Handler.drainForCheck();allOwnedRetired();borrowedUntouched(f);f.retire();
    }
    private static void scopedFallbackAndRecovery(){
        reset();Fixture f=new Fixture();f.info.type=2000;f.enable();Handler.drainForCheck();check(groups()==0,"status bar/application window cannot become wallpaper parent");
        f.info.type=2013;f.info.packageName="com.android.systemui";f.event();check(groups()==0,"in-process static wallpaper retains existing Engine path");
        f.info.packageName="com.heytap.colorfulengine";f.info.extension.putFloat("HScale",Float.NaN);f.event();check(groups()==0,"unknown transform cannot guess screen geometry");
        f.info.extension.putFloat("HScale",1.2f);f.event();check(group().valid,"native valid mapping recovers without reloading module");
        f.context.display.realWidth=3168;f.context.display.realHeight=1440;f.event();check(group().hidden,"transient rotated mapping is safely hidden");
        f.context.display.realWidth=1440;f.context.display.realHeight=3168;f.event();check(!group().hidden,"portrait mapping restores own effect");
        f.info.mSurfaceControl.valid=false;f.event();allOwnedRetired();f.retire();
        reset();Fixture failing=new Fixture();SurfaceControlViewHost.failForCheck=true;failing.enable();Handler.drainForCheck();check(groups()==1,"failed host attempted once");
        int builds=groups();for(int i=0;i<50;i++)failing.event();check(groups()==builds,"same failed surface does not retry on each native event");
        check(failing.wallpaper.valid&&failing.wallpaper.releaseCalls==0,"host failure preserves native wallpaper");SurfaceControlViewHost.failForCheck=false;
        failing.info.mSurfaceControl=new SurfaceControl("fresh native source",null);failing.event();check(group().valid,"new source generation retries native contract");failing.retire();allOwnedRetired();
        reset();Fixture rejected=new Fixture();SurfaceControl.rejectBlurRegionsForCheck=true;rejected.enable();Handler.drainForCheck();check(group().valid,"rejected optional native blur retains wallpaper-only mask host");
        check(rejected.helper.summary().contains("externalGradientBlur=unavailable-mask-only"),"blur submission failure is explicit in diagnostics");
        check(rejected.helper.summary().contains("externalMaskAlpha=34"),"configured ARGB alpha remains independently observable");
        int effects=regionWrites();for(int i=0;i<50;i++)rejected.event();check(regionWrites()==effects,"failed optional blur is not retried on every wallpaper event");
        SurfaceControl.rejectBlurRegionsForCheck=false;rejected.retire();allOwnedRetired();
    }
    private static android.graphics.Canvas drawMask() throws Exception {
        Object view=SurfaceControlViewHost.hostsForCheck.get(SurfaceControlViewHost.hostsForCheck.size()-1).getView();
        Method draw=view.getClass().getDeclaredMethod("dispatchDraw",android.graphics.Canvas.class);draw.setAccessible(true);
        android.graphics.Canvas canvas=new android.graphics.Canvas();draw.invoke(view,canvas);return canvas;
    }
    private static void resizeKeepsEffectCropInSync() {
        reset();Fixture f=new Fixture();f.enable();Handler.drainForCheck();SurfaceControl old=effect();
        f.context.display.realWidth=1536;f.event();SurfaceControl wider=effect();
        check(wider!=old&&!old.valid,"wallpaper width change retires the old cropped effect");
        check(wider.crop.right==1536&&wider.crop.bottom==264&&wider.blurRegions[0][4]==1536f,"new effect crop and region width expand together");
        f.helper.configure(true,true,.75f,16f,80f,40f,0x22000000);Handler.drainForCheck();SurfaceControl taller=effect();
        check(taller!=wider&&!wider.valid,"larger output height rebuilds the exact effect crop");
        check(taller.crop.right==1536&&taller.crop.bottom==360,"effect crop includes the expanded transition");
        f.helper.configure(true,true,.75f,16f,70f,50f,0x22000000);Handler.drainForCheck();
        check(effect()==taller&&taller.crop.bottom==360&&taller.blurRegions[0][5]>=210f&&taller.blurRegions[0][5]<216f,"core-only geometry change reuses a correctly sized effect and updates its runs");
        f.retire();allOwnedRetired();
    }
    private static void independentMaskAndOptionalBlur() throws Exception {
        reset();Fixture f=new Fixture();SurfaceControl.rejectBlurRegionsForCheck=true;f.enable();Handler.drainForCheck();
        check(group().valid&&!group().hidden,"native effect creation failure cannot suppress color mask");
        android.graphics.Canvas canvas=drawMask();check(canvas.rects==1&&canvas.lastShader instanceof android.graphics.LinearGradient,"mask draws with native continuous shader even without blur");
        android.graphics.LinearGradient shader=(android.graphics.LinearGradient)canvas.lastShader;
        near(34f/255f,shader.alpha(0,0),"mask top keeps configured opacity");near(0f,shader.alpha(0,264),"mask ends fully transparent");
        float previous=shader.alpha(0,0);for(int y=1;y<=264;y++){
            float next=shader.alpha(0,y);check(next<=previous+.0001f,"mask gradually becomes clear downward");previous=next;
        }
        check(f.helper.summary().contains("unavailable-mask-only"),"effect creation failure reports mask-only state");
        SurfaceControl.rejectBlurRegionsForCheck=false;
        f.helper.configure(true,false,.75f,16f,56f,32f,0x88402010);Handler.drainForCheck();canvas=drawMask();
        near(136f/255f,((android.graphics.LinearGradient)canvas.lastShader).alpha(0,0),"mask stays adjustable when cross-window blur is disabled");
        check(effect().blurRegions.length==0&&effect().hidden,"power policy disable removes and hides compositor blur even after a rejection");
        check(f.helper.summary().contains("externalGradientBlur=disabled"),"power policy disable is explicit even after optional API failure");
        SurfaceControl keep=group();f.helper.configure(true,false,.75f,16f,56f,32f,0);Handler.drainForCheck();canvas=drawMask();
        check(canvas.rects==0&&group()==keep,"explicit transparent color is preserved and suppresses only mask painting");
        check(f.helper.summary().contains("externalMaskAlpha=0"),"transparent user setting is not rewritten or hidden in diagnostics");
        f.retire();allOwnedRetired();
        reset();
        ClassLoader unavailable=new ClassLoader(WallpaperSurfaceBlurCheck.class.getClassLoader()){
            @Override protected Class<?> loadClass(String name,boolean resolve) throws ClassNotFoundException {
                if(name.equals("com.oplus.graphics.OplusBlurParam"))throw new ClassNotFoundException("optional OEM parameter contract absent");
                return super.loadClass(name,resolve);
            }
        };
        Class<?> access=Class.forName("dev.puitheme.WallpaperSurfaceBlur$NativeAccess");
        java.lang.reflect.Constructor<?> constructor=access.getDeclaredConstructor(ClassLoader.class);constructor.setAccessible(true);
        Object nativeApi=constructor.newInstance(unavailable);Method canBlur=access.getDeclaredMethod("canBlur");canBlur.setAccessible(true);
        check(!((Boolean)canBlur.invoke(nativeApi)),"optional OEM API absence does not reject required surface host contract");
    }
    public static void main(String[] args) throws Exception {
        bridgeAndStableNativeFrames();visibilityDisableAndSurfaceReplacement();scopedFallbackAndRecovery();independentMaskAndOptionalBlur();resizeKeepsEffectCropInSync();
        System.out.println("WallpaperSurfaceBlurCheck: "+checks+" structural/lifecycle assertions passed; visual backdrop unverified");
    }
}
