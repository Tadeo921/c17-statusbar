package dev.puitheme;

import android.graphics.Bitmap;
import android.view.View;
import android.widget.FrameLayout;
import com.android.systemui.scrim.ScrimView;
import io.github.libxposed.api.XposedInterface;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Executes the real installer and its guarded Xposed hookers against native
 * contract classes in a separate loader. Framework method interception itself,
 * ART optimisation and the device's GPU remain outside this JVM check. */
public final class ClockBackdropHooksCheck {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private interface Original {Object run() throws Throwable;}
    private static Field field(Class<?> type,String name) throws Exception {
        for(Class<?> current=type;current!=null;current=current.getSuperclass())try {
            Field result=current.getDeclaredField(name);result.setAccessible(true);return result;
        }catch(NoSuchFieldException missing){}
        throw new NoSuchFieldException(name);
    }
    private static Object invoke(Method method,Object receiver,Object...args) throws Throwable {
        try{return method.invoke(receiver,args);}catch(InvocationTargetException failure){throw failure.getCause();}
    }
    private static final class NativeLoader extends ClassLoader {
        final Set<String> requested=new LinkedHashSet<>();
        NativeLoader(){super(ClockBackdropHooksCheck.class.getClassLoader());}
        @Override protected Class<?> loadClass(String name,boolean resolve) throws ClassNotFoundException {
            synchronized(getClassLoadingLock(name)) {
                requested.add(name);
                if(!name.startsWith("com.oplus.posteffect.")&&!name.startsWith("com.oplusos.systemui.common.blurability.platformblur.")
                        &&!name.startsWith("kotlin.jvm.functions."))return super.loadClass(name,resolve);
                Class<?> found=findLoadedClass(name);
                if(found==null)try(InputStream source=getParent().getResourceAsStream(name.replace('.','/')+".class")) {
                    if(source==null)throw new ClassNotFoundException(name);
                    ByteArrayOutputStream output=new ByteArrayOutputStream();byte[] buffer=new byte[4096];int read;
                    while((read=source.read(buffer))!=-1)output.write(buffer,0,read);
                    byte[] bytes=output.toByteArray();found=defineClass(name,bytes,0,bytes.length);
                }catch(java.io.IOException failure){throw new ClassNotFoundException(name,failure);}
                if(resolve)resolveClass(found);return found;
            }
        }
    }
    private static final class Framework {
        final Map<Executable,XposedInterface.Hooker> hooks=new LinkedHashMap<>();
        final Set<Executable> deoptimized=new LinkedHashSet<>();
        final Map<Executable,XposedInterface.ExceptionMode> modes=new LinkedHashMap<>();
        final List<String> logs=new ArrayList<>();
        final XposedInterface api=(XposedInterface)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{XposedInterface.class},
                (owner,method,args)->{
                    switch(method.getName()) {
                        case "hook":return builder((Executable)args[0]);
                        case "deoptimize":deoptimized.add((Executable)args[0]);return true;
                        case "log":logs.add(Arrays.toString(args));return null;
                        case "getApiVersion":return 101;
                        case "getFrameworkName":return "ClockBackdropHooksCheck";
                        case "getFrameworkVersion":return "fixture";
                        case "getFrameworkVersionCode":case "getFrameworkProperties":return 0L;
                        case "toString":return "ClockBackdropHooksCheck framework";
                        default:throw new AssertionError("Unexpected framework operation "+method.getName());
                    }
                });
        private XposedInterface.HookBuilder builder(Executable executable) {
            return (XposedInterface.HookBuilder)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{XposedInterface.HookBuilder.class},
                    (owner,method,args)->{
                        if(method.getName().equals("setExceptionMode")){modes.put(executable,(XposedInterface.ExceptionMode)args[0]);return owner;}
                        if(method.getName().equals("setPriority"))return owner;
                        if(method.getName().equals("intercept")) {
                            check(hooks.put(executable,(XposedInterface.Hooker)args[0])==null,"installer never duplicates the same native executable");
                            return null;
                        }
                        throw new AssertionError(method.getName());
                    });
        }
        Object run(Executable executable,Object receiver,Object[]args,Original original) throws Throwable {
            XposedInterface.Hooker hook=hooks.get(executable);
            check(hook!=null,"real installer registered "+executable.getDeclaringClass().getSimpleName()+"."+executable.getName());
            XposedInterface.Chain chain=(XposedInterface.Chain)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{XposedInterface.Chain.class},
                    (owner,method,unused)->{
                        switch(method.getName()) {
                            case "getExecutable":return executable;
                            case "getThisObject":return receiver;
                            case "getArgs":return Arrays.asList(args);
                            case "getArg":return args[(Integer)unused[0]];
                            case "proceed":return original.run();
                            default:throw new AssertionError("Unexpected chain operation "+method.getName());
                        }
                    });
            return hook.intercept(chain);
        }
    }
    private static final class Fixture {
        final NativeLoader loader;
        final Framework framework;
        final FrameLayout root=new FrameLayout(null);
        final ScrimView host=new ScrimView();
        final View clock=new View(null);
        final Object helper,engine,wrapper,param;
        final Class<?> continuous,base,paramType;
        final Method ensure,add,drawn,encoded;
        final Constructor<?> construct;
        final Bitmap bitmap=Bitmap.createBitmap(3,2,Bitmap.Config.ARGB_8888);
        Fixture(NativeLoader loader,Framework framework) throws Exception {
            this.loader=loader;this.framework=framework;
            Class<?> helperType=loader.loadClass("com.oplusos.systemui.common.blurability.platformblur.PlatformBlurHelper");
            Class<?> wrapperType=loader.loadClass("com.oplusos.systemui.common.blurability.platformblur.PlatformBlurDrawable");
            Class<?> engineType=loader.loadClass("com.oplus.posteffect.BlurDrawable");
            continuous=loader.loadClass("com.oplus.posteffect.drawable.ContinuousBlurDrawable");
            base=loader.loadClass("com.oplus.posteffect.drawable.BaseDrawable");paramType=loader.loadClass("com.oplus.posteffect.BlurParam");
            host.setId(971);host.getResources().entryNames.put(971,"scrim_behind");root.addView(host);root.addView(clock);
            helper=helperType.getConstructor(View.class).newInstance(host);engine=engineType.getConstructor().newInstance();
            param=field(engineType,"params").get(engine);field(paramType,"radius").setInt(param,0);
            construct=wrapperType.getDeclaredConstructor(engineType,loader.loadClass("kotlin.jvm.functions.Function0"));
            wrapper=construct.newInstance(engine,null);((android.graphics.drawable.Drawable)engine).setBounds(0,0,30,20);
            ensure=helperType.getDeclaredMethod("ensureBlurDrawable");add=continuous.getDeclaredMethod("addBlurDrawable");
            drawn=continuous.getDeclaredMethod("preDrawContent");encoded=paramType.getDeclaredMethod("toFloatArray",boolean.class);
        }
        void constructAndRegister() throws Throwable {
            framework.run(ensure,helper,new Object[0],()->{
                final boolean[] originalRan={false};
                framework.run(construct,wrapper,new Object[]{engine,null},()->{
                    check(!((Map<?,?>)field(ClockBackdropSource.class,"ENGINES").get(null)).containsKey(engine),
                            "the constructor's original runs before source ownership is registered");
                    originalRan[0]=true;return null;
                });
                check(originalRan[0],"constructor observer proceeds before inspecting the native wrapper");
                field(paramType,"radius").setInt(param,40);
                framework.run(add,engine,new Object[0],()->{
                    check(ClockBackdropSource.snapshot(clock)==null,"BEFORE registration never publishes an unregistered image");
                    return invoke(add,engine);
                });
                return null;
            });
        }
        void image() throws Throwable {
            invoke(continuous.getDeclaredMethod("completeForCheck",Bitmap.class),engine,bitmap);
            framework.run(drawn,engine,new Object[0],()->invoke(drawn,engine));
        }
        ClockBackdropSource.Frame frame(){return ClockBackdropSource.snapshot(clock);}
    }
    @SuppressWarnings({"rawtypes","unchecked"})
    private static void safeModeConstructorPolicy() throws Exception {
        Class<?> group=Class.forName("dev.puitheme.StatusBarModule$HookGroup");
        Object bigClock=Enum.valueOf((Class)group,"BIG_CLOCK");
        Method policy=StatusBarModule.class.getDeclaredMethod("observesInSafeMode",group,Executable.class);
        policy.setAccessible(true);
        Class<?> wrapper=Class.forName("com.oplusos.systemui.common.blurability.platformblur.PlatformBlurDrawable");
        for(java.lang.reflect.Constructor<?> constructor:wrapper.getDeclaredConstructors())
            check((Boolean)policy.invoke(null,bigClock,constructor),
                    "native backdrop constructor must remain observable in safe mode before the first blur ID is registered");
    }
    @SuppressWarnings("unchecked")
    private static void installedCallbacks() throws Throwable {
        StatusBarModule module=new StatusBarModule();Framework framework=new Framework();module.attachFramework(framework.api);
        Field registryField=field(StatusBarModule.class,"INSTALLED_HOOKS");
        Map<Object,Set<Executable>> registry=(Map<Object,Set<Executable>>)registryField.get(null);
        Map<Object,Set<Executable>> previous=new LinkedHashMap<>(registry);
        Field features=field(StatusBarModule.class,"FEATURES");Object oldFeatures=features.get(null);
        NativeLoader loader=new NativeLoader();
        Method installer=StatusBarModule.class.getDeclaredMethod("installClockBackdropHooks",ClassLoader.class);installer.setAccessible(true);
        try {
            registry.clear();ClockBackdropSource.clear();
            invoke(installer,module,loader);
            check(framework.hooks.size()>=11,"actual installer resolves every captured native observer site: "+framework.logs);
            for(Executable site:framework.hooks.keySet()) {
                check(site.getDeclaringClass().getClassLoader()==loader,"every native hook uses the original supplied native class loader");
                check(framework.modes.get(site)==XposedInterface.ExceptionMode.PROTECTIVE,"all installed observers retain protective exception mode");
            }
            int installed=framework.hooks.size();invoke(installer,module,loader);
            check(framework.hooks.size()==installed,"a second installation is idempotent");
            Fixture first=new Fixture(loader,framework);
            check(framework.hooks.containsKey(first.drawn),"direct native image promotion is observed after the narrow Continuous override");
            Method largeBase=first.base.getDeclaredMethod("preDrawContent");
            check(!framework.hooks.containsKey(largeBase)&&!framework.deoptimized.contains(largeBase),
                    "the 705-unit Base draw body is neither hooked nor deoptimized");
            check(framework.deoptimized.contains(first.drawn),"the 20-unit Continuous dispatch shim is deoptimized to preserve its real callback");
            check(framework.deoptimized.contains(first.continuous.getDeclaredMethod("syncBlurParams",boolean.class,boolean.class)),
                    "the native update caller is deoptimized so AOT inlining cannot bypass the actual parameter-encoding observer");
            Class<?> manager=loader.loadClass("com.oplus.posteffect.manager.BlurDrawableManager");
            Class<?> stateListener=loader.loadClass("com.oplus.posteffect.manager.BlurStateListener");
            Method firstEncoding=manager.getDeclaredMethod("addBlurDrawable",android.view.SurfaceControl.class,int.class,
                    first.paramType,boolean.class,stateListener,boolean.class);
            check(framework.deoptimized.contains(firstEncoding),"the exact native first-add encoding caller is deoptimized with its original loader and parameter types");
            features.set(null,FeatureOptions.from(Collections.singletonMap(StatusBarSettings.SAFE_MODE,true)));
            final int[] notifications={0};Runnable listener=()->notifications[0]++;ClockBackdropSource.setInvalidationCallback(listener);
            first.constructAndRegister();first.image();
            ClockBackdropSource.Frame ready=first.frame();
            check(ready!=null&&ready.bitmap==first.bitmap&&ready.host==first.host,
                    "safe-mode construction plus first BEFORE/AFTER add captures a usable pure native frame");
            check(notifications[0]==1,"direct-field native image promotion produces one invalidation");
            features.set(null,FeatureOptions.from(Collections.emptyMap()));
            check(first.frame()==ready,"leaving safe mode reuses the proven existing native ID");
            for(int i=0;i<32;i++)framework.run(first.drawn,first.engine,new Object[0],()->invoke(first.drawn,first.engine));
            check(notifications[0]==1&&first.frame()==ready,"stable native draw hooks do not create a redraw or frame-allocation loop");
            android.graphics.Matrix matrix=(android.graphics.Matrix)invoke(first.continuous.getDeclaredMethod("getBitmapMatrix"),first.engine);
            matrix.postTranslate(2,3);framework.run(first.drawn,first.engine,new Object[0],()->invoke(first.drawn,first.engine));
            check(notifications[0]==2&&first.frame()!=ready,"matrix-only native draws notify after the actual transform has changed");
            float[] actual=(float[])invoke(first.encoded,first.param,true);actual[7]=.25f;
            framework.run(first.encoded,first.param,new Object[]{true},()->actual);
            check(first.frame()==null,"actual first parameter encoding is associated even without a preceding setter event");
            ClockBackdropSource.clear();
            Fixture second=new Fixture(loader,framework);second.constructAndRegister();second.image();
            check(second.frame()!=null,"a fresh native helper remains usable after source lifecycle clear");
            Method released=second.base.getDeclaredMethod("releaseDrawingBitmap");
            framework.run(released,second.engine,new Object[0],()->invoke(released,second.engine));
            check(second.frame()==null,"real release AFTER observes cleared native drawing fields");
            ClockBackdropSource.clear();
            Fixture detached=new Fixture(loader,framework);
            framework.run(detached.construct,detached.wrapper,new Object[]{detached.engine,null},()->null);
            field(detached.paramType,"radius").setInt(detached.param,40);
            framework.run(detached.add,detached.engine,new Object[0],()->invoke(detached.add,detached.engine));detached.image();
            check(detached.frame()==null,"constructor outside its helper scope cannot borrow a native behind image");
            ClockBackdropSource.clear();
            Fixture failedEnsure=new Fixture(loader,framework);
            try {
                framework.run(failedEnsure.ensure,failedEnsure.helper,new Object[0],()->{throw new IllegalStateException("native ensure failed");});
                throw new AssertionError("original ensure failure was swallowed");
            }catch(IllegalStateException expected){check("native ensure failed".equals(expected.getMessage()),"ensure observer preserves the original failure");}
            framework.run(failedEnsure.construct,failedEnsure.wrapper,new Object[]{failedEnsure.engine,null},()->null);
            field(failedEnsure.paramType,"radius").setInt(failedEnsure.param,40);
            framework.run(failedEnsure.add,failedEnsure.engine,new Object[0],()->invoke(failedEnsure.add,failedEnsure.engine));failedEnsure.image();
            check(failedEnsure.frame()==null,"exceptional ensure closes its thread-local scope before later unrelated construction");
            promotionCallbacks(loader,framework);
            setterCallbacks(loader,framework);
        } finally {
            features.set(null,oldFeatures);registry.clear();registry.putAll(previous);ClockBackdropSource.clear();
        }
    }
    private static void promotionCallbacks(NativeLoader loader,Framework framework) throws Throwable {
        ClockBackdropSource.clear();Fixture current=new Fixture(loader,framework);current.constructAndRegister();current.image();
        final int[] notifications={0};Runnable listener=()->notifications[0]++;ClockBackdropSource.setInvalidationCallback(listener);
        Method released=current.base.getDeclaredMethod("releaseDrawingBitmap");
        Field gap=current.base.getDeclaredField("afterReleaseForCheck");
        gap.set(current.engine,(Runnable)()->{
            try {
                check(field(current.continuous,"drawing").get(current.engine)==null,"real promotion has released the old image before its direct replacement");
                framework.run(released,current.engine,new Object[0],()->null);
                check(notifications[0]==0,"release inside native image promotion must not publish a temporary null image");
            }catch(Throwable failure){throw new AssertionError("promotion release observer",failure);}
        });
        current.image();gap.set(current.engine,null);
        check(notifications[0]==1&&current.frame()!=null,"completed native promotion publishes the final content once");
        framework.run(released,current.engine,new Object[0],()->invoke(released,current.engine));
        check(notifications[0]==2&&current.frame()==null,"standalone native release still invalidates immediately");
    }
    private static void setterCallbacks(NativeLoader loader,Framework framework) throws Throwable {
        for(boolean internal:new boolean[]{false,true}) {
            ClockBackdropSource.clear();Fixture current=new Fixture(loader,framework);current.constructAndRegister();current.image();
            Object upcoming=current.paramType.getConstructor().newInstance();field(current.paramType,"brightnessMaximum").setFloat(upcoming,.8f);
            Class<?> foreground=loader.loadClass("com.oplus.posteffect.ForegroundBlurParam");
            Method setter=internal?current.continuous.getDeclaredMethod("setBlurParamsInternal",current.paramType,foreground,boolean.class)
                    :current.base.getDeclaredMethod("setDrawingParam",current.paramType);
            Object[] args=internal?new Object[]{upcoming,null,false}:new Object[]{upcoming};
            framework.run(setter,current.engine,args,()->{
                check(current.frame()==null,"upcoming impure parameters are rejected BEFORE the native setter can submit them");
                check(field(current.engine.getClass(),"params").get(current.engine)==current.param,"parameter observer does not mutate the native current value");
                return invoke(setter,current.engine,args);
            });
            check(current.frame()==null,"native parameter copy/set cannot revive a contaminated blur ID");
        }
        for(String name:new String[]{"setEnableBlurShader","setEnableShader"}) {
            ClockBackdropSource.clear();Fixture current=new Fixture(loader,framework);current.constructAndRegister();current.image();
            Method setter=current.base.getDeclaredMethod(name,boolean.class);
            framework.run(setter,current.engine,new Object[]{false},()->{
                check(current.frame()==null,"renderer switch is observed BEFORE disabling the native shader");
                check(field(current.engine.getClass(),"enableShader").getBoolean(current.engine),"shader observer itself does not change the original renderer");
                return invoke(setter,current.engine,false);
            });
            check(current.frame()==null,"renderer false transition permanently discards the source");
        }
        ClockBackdropSource.clear();Fixture arrived=new Fixture(loader,framework);arrived.constructAndRegister();
        final int[] notified={0};Runnable listener=()->notified[0]++;ClockBackdropSource.setInvalidationCallback(listener);
        Class<?> image=loader.loadClass("com.oplus.posteffect.drawable.BaseDrawable$ImageInfo");
        Object content=loader.loadClass("com.oplus.posteffect.drawable.ContinuousBlurDrawable$ImageInfo").getConstructor(Bitmap.class).newInstance(arrived.bitmap);
        Method setter=arrived.base.getDeclaredMethod("setDrawingImageInfo",image);
        framework.run(setter,arrived.engine,new Object[]{content},()->{
            check(notified[0]==0,"image setter's original runs before the notification callback");return invoke(setter,arrived.engine,content);
        });
        check(notified[0]==1&&arrived.frame()!=null&&arrived.frame().bitmap==arrived.bitmap,"direct image setter AFTER exposes only the newly written native image");
        ClockBackdropSource.clear();Fixture debug=new Fixture(loader,framework);
        Field logging=field(loader.loadClass("com.oplus.posteffect.util.Log"),"DEBUG");
        Field serverMode=field(loader.loadClass("com.oplus.posteffect.manager.BlurDrawableManager"),"sBlendTypeValue");
        try {
            framework.run(debug.ensure,debug.helper,new Object[0],()->{
                framework.run(debug.construct,debug.wrapper,new Object[]{debug.engine,null},()->null);
                field(debug.paramType,"radius").setInt(debug.param,40);
                return framework.run(debug.add,debug.engine,new Object[0],()->{
                    logging.setBoolean(null,true);serverMode.setInt(null,2);return invoke(debug.add,debug.engine);
                });
            });
        } finally {logging.setBoolean(null,false);serverMode.setInt(null,-1);}
        debug.image();check(debug.frame()==null,"DEBUG server mixing enabled only inside first add is caught by registration AFTER and cannot revive later");
    }
    public static void main(String[] args) throws Throwable {
        safeModeConstructorPolicy();installedCallbacks();
        System.out.println("ClockBackdropHooksCheck: "+checks+" checks passed (real installer/guarded callbacks; ART/GPU/device pending)");
    }
}
