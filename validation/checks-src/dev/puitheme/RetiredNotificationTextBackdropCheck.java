package dev.puitheme;

import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.android.systemui.statusbar.notification.row.ExpandableNotificationRow;
import com.android.systemui.statusbar.notification.row.NotificationContentView;
import io.github.libxposed.api.XposedInterface;
import java.lang.reflect.*;
import java.util.*;

/** Retired notification glyph following has no owner or added common hooks;
 * existing native text/color/lockscreen hooks continue to work independently. */
public final class RetiredNotificationTextBackdropCheck {
    private static int checks;
    private static void check(boolean yes,String why){checks++;if(!yes)throw new AssertionError(why);}
    private interface Action{Object run()throws Throwable;}
    private static Field field(Class<?> type,String name)throws Exception{for(Class<?> c=type;c!=null;c=c.getSuperclass())try{Field f=c.getDeclaredField(name);f.setAccessible(true);return f;}catch(NoSuchFieldException missing){}throw new NoSuchFieldException(name);}
    private static Object invoke(Method method,Object target,Object...args)throws Throwable{try{return method.invoke(target,args);}catch(InvocationTargetException error){throw error.getCause();}}
    private static final class Harness {
        final Object framework;final Method run;final Class<?> action;
        final Map<Executable,?> hooks,modes;final Set<Executable> deoptimized;
        final StatusBarModule module=new StatusBarModule();
        @SuppressWarnings("unchecked") Harness()throws Exception {
            Class<?> type=Class.forName("dev.puitheme.ClockBackdropHooksCheck$Framework");Constructor<?> ctor=type.getDeclaredConstructor();ctor.setAccessible(true);framework=ctor.newInstance();
            action=Class.forName("dev.puitheme.ClockBackdropHooksCheck$Original");run=type.getDeclaredMethod("run",Executable.class,Object.class,Object[].class,action);run.setAccessible(true);
            hooks=(Map<Executable,?>)field(type,"hooks").get(framework);modes=(Map<Executable,?>)field(type,"modes").get(framework);deoptimized=(Set<Executable>)field(type,"deoptimized").get(framework);
            module.attachFramework((XposedInterface)field(type,"api").get(framework));
        }
        Object call(Method site,Object target,Object[] args,Action original)throws Throwable {
            int[] calls={0};Object callback=Proxy.newProxyInstance(action.getClassLoader(),new Class<?>[]{action},(owner,method,unused)->{calls[0]++;return original.run();});
            Object result=invoke(run,framework,site,target,args,callback);check(calls[0]==1,"retained hook proceeds exactly once: "+site.getName());return result;
        }
        void install()throws Throwable {
            Method installer=StatusBarModule.class.getDeclaredMethod("installTextControls",ClassLoader.class);installer.setAccessible(true);
            ClassLoader optionalAbsent=new ClassLoader(getClass().getClassLoader()){@Override protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException{if(name.startsWith("com.android.")||name.startsWith("com.oplus."))throw new ClassNotFoundException(name);return super.loadClass(name,resolve);}};
            check(Boolean.TRUE.equals(invoke(installer,module,optionalAbsent)),"existing TEXT installation still completes");
        }
    }
    @SuppressWarnings("unchecked") public static void main(String[] args)throws Throwable {
        int sdk=Build.VERSION.SDK_INT;Field installed=field(StatusBarModule.class,"INSTALLED_HOOKS"),features=field(StatusBarModule.class,"FEATURES");
        Map<Object,Set<Executable>> registry=(Map<Object,Set<Executable>>)installed.get(null);Map<Object,Set<Executable>> previous=new LinkedHashMap<>(registry);Object oldFeatures=features.get(null);
        try {
            for(int version:new int[]{30,33,37}) {
                Build.VERSION.SDK_INT=version;registry.clear();Harness harness=new Harness();harness.install();
                Method[] retired={View.class.getDeclaredMethod("setRenderEffect",RenderEffect.class),TextView.class.getDeclaredMethod("setCompoundDrawables",Drawable.class,Drawable.class,Drawable.class,Drawable.class),TextView.class.getDeclaredMethod("setCompoundDrawablesRelative",Drawable.class,Drawable.class,Drawable.class,Drawable.class),View.class.getDeclaredMethod("setBackgroundDrawable",Drawable.class),TextView.class.getDeclaredMethod("setShadowLayer",float.class,float.class,float.class,int.class)};
                for(Method site:retired){check(!harness.hooks.containsKey(site),"notification-only global hook removed on SDK"+version+": "+site.getName());check(!harness.deoptimized.contains(site),"notification-only entry no longer deoptimized: "+site.getName());}
                for(Field owner:StatusBarModule.class.getDeclaredFields())check(!owner.getName().equals("NOTIFICATION_TEXT_BACKDROP")&&!owner.getType().getName().equals("dev.puitheme.NotificationTextBackdrop"),"no resident notification glyph manager");
                for(boolean safe:new boolean[]{false,true}) {
                    Map<String,Object> values=new HashMap<>();values.put(StatusBarSettings.SAFE_MODE,safe);values.put(ClockBackdropSettings.NOTIFICATION_FOLLOW,true);values.put(ClockBackdropSettings.NOTIFICATION_RELIEF,true);features.set(null,FeatureOptions.from(values));
                    FrameLayout root=new FrameLayout(null);ExpandableNotificationRow row=new ExpandableNotificationRow(null,200,0,0);NotificationContentView content=new NotificationContentView(null);TextView text=new TextView(null);root.addView(row);row.addView(content);content.addView(text);text.setText("原生通知");
                    RenderEffect nativeEffect=RenderEffect.createBlurEffect(1,1,Shader.TileMode.CLAMP);text.setRenderEffect(nativeEffect);
                    Method attach=View.class.getDeclaredMethod("onAttachedToWindow"),detach=View.class.getDeclaredMethod("onDetachedFromWindow"),color=TextView.class.getDeclaredMethod("setTextColor",int.class),selector=TextView.class.getDeclaredMethod("setTextColor",ColorStateList.class),draw=View.class.getDeclaredMethod("draw",Canvas.class);
                    for(Method site:new Method[]{attach,detach,color,selector,draw})check(harness.hooks.containsKey(site)&&harness.modes.get(site)==XposedInterface.ExceptionMode.PROTECTIVE,"existing native text hook retained: "+site.getName());
                    Object marker=new Object();harness.call(attach,text,new Object[0],()->{text.attached=true;return marker;});
                    check(root.getViewTreeObserver().preDrawListeners.isEmpty(),"legacy notification keys cannot add a window preDraw listener");
                    Object result=harness.call(color,text,new Object[]{0xff2468ac},()->{text.setTextColor(0xff2468ac);return marker;});check(result==marker&&text.getCurrentTextColor()==0xff2468ac,"legacy keys do not recolor native notification glyphs");
                    ColorStateList chosen=new ColorStateList(new int[][]{{1},{}},new int[]{0xffabcdef,0xff987654});harness.call(selector,text,new Object[]{chosen},()->{text.setTextColor(chosen);return marker;});check(text.getTextColors()==chosen,"native selector object remains exact");
                    harness.call(draw,text,new Object[]{new Canvas()},()->marker);check(text.renderEffect==nativeEffect,"ordinary native draw keeps its own effect");
                    harness.call(detach,text,new Object[0],()->{text.attached=false;return marker;});check(root.getViewTreeObserver().preDrawListeners.isEmpty()&&text.renderEffect==nativeEffect,"detach has no notification material lifecycle");
                }
            }
        }finally{Build.VERSION.SDK_INT=sdk;features.set(null,oldFeatures);registry.clear();registry.putAll(previous);}
        System.out.println("RetiredNotificationTextBackdropCheck: "+checks+" checks passed");
    }
}
