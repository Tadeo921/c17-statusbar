package dev.puitheme;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import com.android.systemui.plugins.ActivityStarter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

/** Real launch routing, owner visibility and portrait-only settings, without touching device apps. */
public final class NotificationBigClockLaunchCheck {
    private static int checks;
    private static void check(boolean yes,String why){checks++;if(!yes)throw new AssertionError(why);}
    private static ResolveInfo app(String name,int flags){
        ResolveInfo resolved=new ResolveInfo();ActivityInfo activity=new ActivityInfo();
        activity.packageName=name;activity.name=name+".Main";activity.applicationInfo=new ApplicationInfo();
        activity.applicationInfo.flags=flags;resolved.activityInfo=activity;return resolved;
    }
    private static final class Packages extends PackageManager {
        ResolveInfo clock,calendar,alarm,viewer;List<ResolveInfo> handlers=Collections.emptyList();
        final Map<String,ResolveInfo> launchers=new LinkedHashMap<>();
        int componentState=COMPONENT_ENABLED_STATE_DEFAULT,viewQueries;
        @Override public ResolveInfo resolveActivity(Intent intent,int flags){
            check(flags==MATCH_DEFAULT_ONLY,"matches exported default intent handlers");
            if(Intent.ACTION_VIEW.equals(intent.getAction())){viewQueries++;return viewer;}
            ResolveInfo target="native.clock.action".equals(intent.getAction())?clock:
                    "android.intent.action.SHOW_ALARMS".equals(intent.getAction())?alarm:
                    intent.hasCategory(Intent.CATEGORY_APP_CALENDAR)?calendar:null;
            return target!=null&&(intent.getPackage()==null||intent.getPackage().equals(target.activityInfo.packageName))?target:null;
        }
        @Override public List<ResolveInfo> queryIntentActivities(Intent intent,int flags){
            return handlers;
        }
        @Override public Intent getLaunchIntentForPackage(String name){
            ResolveInfo launcher=launchers.get(name);
            return launcher==null?null:new Intent(Intent.ACTION_MAIN).setComponent(new ComponentName(name,launcher.activityInfo.name));
        }
        @Override public ActivityInfo getActivityInfo(ComponentName name,int flags)throws NameNotFoundException{
            ResolveInfo launcher=launchers.get(name.getPackageName());
            if(launcher==null||!launcher.activityInfo.name.equals(name.getClassName()))throw new NameNotFoundException();
            return launcher.activityInfo;
        }
        @Override public int getComponentEnabledSetting(ComponentName name){return componentState;}
        @Override public void setComponentEnabledSetting(ComponentName name,int state,int flags){}
    }
    private static final class Starter implements ActivityStarter {
        int posted,raw,delay=-1;Intent intent;boolean fail;
        @Override public void postStartActivityDismissingKeyguard(Intent intent,int delay){
            if(fail)throw new IllegalStateException("native launch unavailable");posted++;this.intent=intent;this.delay=delay;
        }
        @Override public void startActivity(Intent intent,boolean collapse){raw++;}
    }
    private static void launchRouting(){
        Packages packages=new Packages();ResolveInfo system=app("system.clock",ApplicationInfo.FLAG_SYSTEM);
        packages.clock=system;packages.launchers.put("system.clock",system);
        ResolveInfo calendar=app("system.calendar",ApplicationInfo.FLAG_SYSTEM);packages.calendar=calendar;
        packages.viewer=app("system.search",ApplicationInfo.FLAG_SYSTEM);
        Intent clock=ClockLaunchHelper.systemIntent(packages,false,"native.clock.action","system.calendar");
        check("native.clock.action".equals(clock.getAction()),"uses native SystemUI clock action instead of CTS API");
        check(clock.getComponent().getPackageName().equals("system.clock"),"native resolved system app retained");
        check((clock.getFlags()&Intent.FLAG_ACTIVITY_NEW_TASK)!=0,"SystemUI non-Activity launch flag");
        check((clock.getFlags()&(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP))
                ==(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP),"native clock flags retained");
        Intent date=ClockLaunchHelper.systemIntent(packages,true,"native.clock.action","system.calendar");
        check(Intent.ACTION_MAIN.equals(date.getAction())&&date.hasCategory(Intent.CATEGORY_APP_CALENDAR),"native calendar category");
        check(date.getComponent().getPackageName().equals("system.calendar")&&"system.calendar".equals(date.getPackage()),"native calendar package retained");
        check(date.getBooleanExtra("KEY_CHECK_PASSWORD",false)&&date.getIntExtra("calendar_enter_type",0)==2,"native calendar entry metadata");
        check((date.getFlags()&Intent.FLAG_ACTIVITY_CLEAR_TASK)!=0,"native calendar flags");
        check(packages.viewQueries==0,"calendar content URI is never routed to unrelated search viewer");
        ResolveInfo user=app("user.app",0),updated=app("system.updated",ApplicationInfo.FLAG_UPDATED_SYSTEM_APP);
        packages.clock=user;packages.handlers=Arrays.asList(user,updated);
        check(ClockLaunchHelper.systemIntent(packages,false,"native.clock.action","").getComponent().getPackageName().equals("system.updated"),"only unique system handler replaces unrelated resolver");
        packages.handlers=Arrays.asList(updated,system);
        check(ClockLaunchHelper.systemIntent(packages,false,"native.clock.action","")==null,"multiple system handlers cannot be chosen alphabetically");
        packages.handlers=Arrays.asList(system,updated);
        check(ClockLaunchHelper.systemIntent(packages,false,"native.clock.action","")==null,"ambiguous query order is irrelevant");
        packages.clock=app("android",ApplicationInfo.FLAG_SYSTEM);
        check(ClockLaunchHelper.systemIntent(packages,false,"native.clock.action","")==null,"resolver activity is not a clock application");
        packages.clock=user;
        packages.handlers=Arrays.asList(updated);
        updated.activityInfo.enabled=false;check(ClockLaunchHelper.systemIntent(packages,false,"native.clock.action","")==null,"disabled system target safely unsupported");
        updated.activityInfo.enabled=true;updated.activityInfo.exported=false;check(ClockLaunchHelper.systemIntent(packages,false,"native.clock.action","")==null,"private activity unsupported");
        updated.activityInfo.exported=true;updated.activityInfo.applicationInfo.enabled=false;
        packages.calendar=updated;
        check(ClockLaunchHelper.systemIntent(packages,true,"","")==null,"disabled application unsupported");
        updated.activityInfo.applicationInfo.enabled=true;packages.calendar=calendar;packages.handlers=Collections.emptyList();
        Intent fallback=ClockLaunchHelper.systemIntent(packages,true,"","uninstalled.calendar");
        check(Intent.ACTION_MAIN.equals(fallback.getAction())&&fallback.hasCategory(Intent.CATEGORY_APP_CALENDAR),"missing regional package uses resolved calendar category");
        ResolveInfo api=app("system.clock",ApplicationInfo.FLAG_SYSTEM);api.activityInfo.name="system.clock.cts.HandleApiActivity";
        packages.alarm=api;packages.clock=null;
        Intent apiFallback=ClockLaunchHelper.systemIntent(packages,false,"","");
        check(apiFallback.getComponent().getClassName().equals("system.clock.Main")&&Intent.ACTION_MAIN.equals(apiFallback.getAction()),"missing native resource opens app main instead of CTS handler");
        packages.clock=system;
        Context context=new Context(){
            @Override public PackageManager getPackageManager(){return packages;}
            @Override public android.content.res.Resources getResources(){return new android.content.res.Resources(){
                @Override public int getIdentifier(String name,String type,String pkg){
                    check("string".equals(type)&&"com.android.systemui".equals(pkg),"native resource namespace");
                    return "alarmclock_action".equals(name)?1:"calendar_cn_package".equals(name)?2:0;
                }
                @Override public String getString(int id){return id==1?"native.clock.action":"system.calendar";}
            };}
        };
        Intent nativeResource=ClockLaunchHelper.systemIntent(context,false,0);
        check("native.clock.action".equals(nativeResource.getAction()),"reads native action dynamically without baked component");
        final int[] nativeLookups={0};
        Context settingsContext=new Context(){
            @Override public PackageManager getPackageManager(){return packages;}
            @Override public android.content.res.Resources getResources(){return new android.content.res.Resources(){
                @Override public int getIdentifier(String name,String type,String pkg){return 0;}
            };}
            @Override public Context createPackageContext(String name,int flags){
                check("com.android.systemui".equals(name)&&flags==0,"settings preview uses installed SystemUI resources without code loading");
                nativeLookups[0]++;return context;
            }
        };
        check("system.clock".equals(ClockLaunchHelper.systemIntent(settingsContext,false,0).getComponent().getPackageName()),
                "app picker default preview resolves same native clock as SystemUI");
        check("system.calendar".equals(ClockLaunchHelper.systemIntent(settingsContext,true,0).getComponent().getPackageName()),
                "app picker default preview resolves same native calendar as SystemUI");
        check(nativeLookups[0]==4,"native resource lookup is event driven for two values per default resolution");
        packages.launchers.put("user.app",user);
        check(ClockLaunchHelper.customIntent(packages,"user.app").getComponent().getPackageName().equals("user.app"),"custom third-party launch app allowed");
        packages.launchers.put("redirected.app",user);
        check(ClockLaunchHelper.customIntent(packages,"redirected.app")==null,"selected package cannot launch a different app");
        check(ClockLaunchHelper.customIntent(packages,"not.installed")==null,"uninstalled custom app safely falls back");
        user.activityInfo.exported=false;check(ClockLaunchHelper.customIntent(packages,"user.app")==null,"private custom activity rejected");
        user.activityInfo.exported=true;user.activityInfo.enabled=false;check(ClockLaunchHelper.customIntent(packages,"user.app")==null,"disabled custom activity rejected");
        user.activityInfo.enabled=true;user.activityInfo.applicationInfo.enabled=false;
        check(ClockLaunchHelper.customIntent(packages,"user.app")==null,"disabled custom app rejected");
        user.activityInfo.applicationInfo.enabled=true;packages.componentState=PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER;
        check(ClockLaunchHelper.customIntent(packages,"user.app")==null,"explicitly disabled component rejected");
        packages.componentState=PackageManager.COMPONENT_ENABLED_STATE_DEFAULT;
        ClockLaunchHelper helper=new ClockLaunchHelper();check(!helper.launch(context,false,0),"no unsafe direct launch while starter is absent");
        helper.setActivityStarter(new Object());check(!helper.ready(),"only real ActivityStarter contract accepted");
        Starter starter=new Starter();helper.setActivityStarter(starter);check(helper.ready(),"injected native starter ready");
        check(helper.launch(context,false,0)&&starter.posted==1&&starter.raw==0&&starter.delay==0,"native dismissal route called once, not direct activity start");
        check(helper.launch(context,false,0,"user.app")&&starter.intent.getComponent().getPackageName().equals("user.app"),"custom clock uses selected app via same dismissal route");
        check(helper.launch(context,true,0,"not.installed")&&starter.intent.getComponent().getPackageName().equals("system.calendar"),"deleted custom calendar falls back to native default");
        starter.fail=true;check(!helper.launch(context,true,0),"native failure contained without retrying launch");
        helper.clear();check(!helper.ready()&&!helper.launch(context,false,0),"release clears native starter");
    }
    private static boolean allowed(View target,View overlay,View host,boolean active,boolean eligible,
            boolean landscape,boolean enabled,boolean moving,float fraction){
        return NotificationBigClock.clickVisible(target,overlay,host,active,eligible,landscape,enabled,moving,fraction,new Rect());
    }
    private static void visibility(){
        Context context=new Context();FrameLayout host=new FrameLayout(context),overlay=new FrameLayout(context),header=new FrameLayout(context);
        View target=new View(context);host.addView(overlay);overlay.addView(header);header.addView(target);
        check(allowed(target,overlay,host,true,true,false,true,false,1f),"visible portrait glyph is clickable");
        check(!allowed(target,overlay,host,false,true,false,true,false,1f),"inactive owner cannot launch");
        check(!allowed(target,overlay,host,true,false,false,true,false,1f),"wrong scene cannot launch");
        check(!allowed(target,overlay,host,true,true,true,true,false,1f),"landscape never borrows portrait click");
        check(!allowed(target,overlay,host,true,true,false,false,false,1f),"independent switch off");
        check(!allowed(target,overlay,host,true,true,false,true,true,1f),"horizontal animation cannot launch");
        check(!allowed(target,overlay,host,true,true,false,true,false,0f),"closed panel cannot launch");
        check(!allowed(target,overlay,host,true,true,false,true,false,Float.NaN),"invalid transition retained native");
        header.setAlpha(0f);check(!allowed(target,overlay,host,true,true,false,true,false,1f),"invisible header alpha");
        header.setAlpha(1f);target.setVisibility(View.GONE);check(!allowed(target,overlay,host,true,true,false,true,false,1f),"fallback clock hidden for native glyphs");
        target.setVisibility(View.VISIBLE);target.attached=false;check(!allowed(target,overlay,host,true,true,false,true,false,1f),"detached glyph");
        target.attached=true;header.setVisibility(View.INVISIBLE);check(!allowed(target,overlay,host,true,true,false,true,false,1f),"hidden parent");
        header.setVisibility(View.VISIBLE);overlay.parent=new FrameLayout(context);
        check(!allowed(target,overlay,host,true,true,false,true,false,1f),"stale overlay cannot use departing host");
        host.addView(overlay);FrameLayout window=new FrameLayout(context);window.addView(host);window.setAlpha(0f);
        check(!allowed(target,overlay,host,true,true,false,true,false,1f),"outer window invisibility included");
    }
    private static Object setting(Bundle source,String prefix,String field)throws Exception{
        Class<?> type=Class.forName("dev.puitheme.NotificationBigClock$Settings");
        Constructor<?> constructor=type.getDeclaredConstructor(Bundle.class,String.class);constructor.setAccessible(true);
        Field value=type.getDeclaredField(field);value.setAccessible(true);return value.get(constructor.newInstance(source,prefix));
    }
    private static void settings()throws Exception{
        check(Boolean.TRUE.equals(NotificationBigClockSettings.BOOLEANS.get(NotificationBigClockSettings.CLOCK_CLICK_ENABLED)),"clock launch defaults enabled");
        check(Boolean.TRUE.equals(NotificationBigClockSettings.BOOLEANS.get(NotificationBigClockSettings.DATE_CLICK_ENABLED)),"calendar launch defaults enabled");
        check(!NotificationBigClockSettings.BOOLEANS.containsKey(NotificationBigClockSettings.landscapeKey(NotificationBigClockSettings.CLOCK_CLICK_ENABLED)),"no unintended landscape setting");
        check((Boolean)setting(null,NotificationBigClockSettings.PORTRAIT_PREFIX,"clockClickEnabled"),"missing old config adopts clock default");
        check((Boolean)setting(null,NotificationBigClockSettings.PORTRAIT_PREFIX,"dateClickEnabled"),"missing old config adopts date default");
        Bundle source=new Bundle();source.putBoolean(NotificationBigClockSettings.CLOCK_CLICK_ENABLED,false);
        check(!(Boolean)setting(source,NotificationBigClockSettings.PORTRAIT_PREFIX,"clockClickEnabled"),"saved clock switch retained");
        check((Boolean)setting(source,NotificationBigClockSettings.PORTRAIT_PREFIX,"dateClickEnabled"),"date switch independent");
        source.putBoolean(NotificationBigClockSettings.landscapeKey(NotificationBigClockSettings.DATE_CLICK_ENABLED),true);
        check(!(Boolean)setting(source,NotificationBigClockSettings.LANDSCAPE_PREFIX,"dateClickEnabled"),"imported landscape alias cannot enable launch");
        for(String key:new String[]{NotificationBigClockSettings.CLOCK_LAUNCH_APP,NotificationBigClockSettings.DATE_LAUNCH_APP}) {
            check("".equals(NotificationBigClockSettings.STRINGS.get(key)),"empty target keeps system default "+key);
            check(!NotificationBigClockSettings.STRINGS.containsKey(NotificationBigClockSettings.landscapeKey(key)),"no launch-app alias on landscape "+key);
        }
        source.putString(NotificationBigClockSettings.CLOCK_LAUNCH_APP,"selected.clock");
        source.putString(NotificationBigClockSettings.DATE_LAUNCH_APP,"selected.calendar");
        check("selected.clock".equals(setting(source,NotificationBigClockSettings.PORTRAIT_PREFIX,"clockLaunchApp")),"clock target retained independently");
        check("selected.calendar".equals(setting(source,NotificationBigClockSettings.PORTRAIT_PREFIX,"dateLaunchApp")),"date target retained independently");
        check("".equals(setting(source,NotificationBigClockSettings.LANDSCAPE_PREFIX,"clockLaunchApp")),"landscape cannot use portrait clock app");
        check("".equals(setting(source,NotificationBigClockSettings.LANDSCAPE_PREFIX,"dateLaunchApp")),"landscape cannot use portrait calendar app");
    }
    private static void widgets()throws Exception{
        NotificationBigClock owner=new NotificationBigClock();
        Class<?> type=Class.forName("dev.puitheme.NotificationBigClock$ClockView");
        Constructor<?> constructor=type.getDeclaredConstructor(NotificationBigClock.class,Context.class);constructor.setAccessible(true);
        View widget=(View)constructor.newInstance(owner,new Context());
        check(!widget.isClickable(),"full-screen overlay stays transparent to notification touch");
        for(String name:new String[]{"headerMotion","footerMotion","clock","dateLabel","footerLabel"}){
            Field field=type.getDeclaredField(name);field.setAccessible(true);View child=(View)field.get(widget);
            check(!child.isClickable(),"unbound or disabled widget never steals touch: "+name);
        }
        java.lang.reflect.Method disable=type.getDeclaredMethod("disableClickTargets");disable.setAccessible(true);
        Field clock=type.getDeclaredField("clock");clock.setAccessible(true);View child=(View)clock.get(widget);
        child.setClickable(true);disable.invoke(widget);check(!child.isClickable(),"exit releases only child click regions");
        child.performClick();check(!child.isClickable(),"programmatic hidden click cannot enable or launch a departing clock");
    }
    public static void main(String[] args)throws Exception{
        launchRouting();visibility();settings();widgets();
        System.out.println("NotificationBigClockLaunchCheck: "+checks+" checks passed");
    }
}
