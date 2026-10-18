package dev.puitheme;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/** Click the actual slot TextViews and route their selected apps through the
 * native ActivityStarter contract. No direct activity or device access. */
public final class BigClockComponentClickCheck {
    private static int checks;
    private static final String LEFT_CLICK="notification_big_clock_widget_left_click_enabled",RIGHT_CLICK="notification_big_clock_widget_right_click_enabled";
    private static final String LEFT_APP="notification_big_clock_widget_left_launch_app",RIGHT_APP="notification_big_clock_widget_right_launch_app";
    private static void check(boolean result,String why){checks++;if(!result)throw new AssertionError(why);}
    private static final class Packages extends PackageManager {
        final java.util.Map<String,ActivityInfo> apps=new java.util.HashMap<>();int queries,resolves;
        ActivityInfo add(String name){ActivityInfo info=new ActivityInfo();info.packageName=name;info.name=name+".Main";info.enabled=info.exported=true;info.applicationInfo=new ApplicationInfo();info.applicationInfo.enabled=true;apps.put(name,info);return info;}
        public Intent getLaunchIntentForPackage(String name){queries++;ActivityInfo info=apps.get(name);return info==null?null:new Intent(Intent.ACTION_MAIN).setComponent(new ComponentName(name,info.name));}
        public ActivityInfo getActivityInfo(ComponentName name,int flags)throws NameNotFoundException {ActivityInfo info=apps.get(name.getPackageName());if(info==null)throw new NameNotFoundException();return info;}
        public int getComponentEnabledSetting(ComponentName name){return COMPONENT_ENABLED_STATE_DEFAULT;}
        public void setComponentEnabledSetting(ComponentName name,int state,int flags){}
        public android.content.pm.ResolveInfo resolveActivity(Intent intent,int flags){resolves++;return null;}
    }
    public static final class Starter implements com.android.systemui.plugins.ActivityStarter {
        int posted,raw;Intent intent;
        public void postStartActivityDismissingKeyguard(Intent target,int delay){check(delay==0,"selected component uses native dismissal scheduling");posted++;intent=target;}
        public void startActivity(Intent target,boolean dismissShade){raw++;}
    }
    private static Method method(Class<?> type,String name,Class<?>...args)throws Exception{Method m=type.getDeclaredMethod(name,args);m.setAccessible(true);return m;}
    private static boolean launch(ClockLaunchHelper helper,Context context,String pkg)throws Exception {
        return Boolean.TRUE.equals(method(ClockLaunchHelper.class,"launchApp",Context.class,String.class).invoke(helper,context,pkg));
    }
    public static void main(String[] args)throws Exception {
        check(Boolean.FALSE.equals(BigClockWidgetsSettings.BOOLEANS.get(LEFT_CLICK))&&Boolean.FALSE.equals(BigClockWidgetsSettings.BOOLEANS.get(RIGHT_CLICK)),"component click switches are registered and default off on both sides");
        check("".equals(BigClockWidgetsSettings.STRINGS.get(LEFT_APP))&&"".equals(BigClockWidgetsSettings.STRINGS.get(RIGHT_APP)),"unselected component apps default empty rather than inheriting clock/calendar apps");
        for(int side=0;side<2;side++) {
            String click=(String)method(BigClockWidgetsSettings.class,"clickEnabledKey",int.class).invoke(null,side),app=(String)method(BigClockWidgetsSettings.class,"launchAppKey",int.class).invoke(null,side);
            check(BigClockWidgetsSettings.textSide(click)==side&&BigClockWidgetsSettings.textSide(app)==side,"each new setting stays in its own existing text component group");
        }
        for(String value:new String[]{"","com.example.app","android"})check(method(BigClockWidgetsSettings.class,"launchAppValidationError",String.class).invoke(null,value)==null,"selected app stores only a package name or empty value");
        for(String value:new String[]{"intent:#Intent;end","com.example/.Main","https://example.com","bad package",".bad","bad..name","bad\nname"})check(method(BigClockWidgetsSettings.class,"launchAppValidationError",String.class).invoke(null,value)!=null,"Intent/component/URI or malformed package cannot enter component configuration");
        Packages packages=new Packages();ActivityInfo leftApp=packages.add("selected.left"),rightApp=packages.add("selected.right");
        Context context=new Context(){@Override public PackageManager getPackageManager(){return packages;}};
        ClockLaunchHelper helper=new ClockLaunchHelper();Starter starter=new Starter();helper.setActivityStarter(starter);
        BigClockTextComponents row=new BigClockTextComponents(context);Bundle values=new Bundle();
        values.putBoolean(BigClockWidgetsSettings.LEFT_ENABLED,true);values.putBoolean(BigClockWidgetsSettings.RIGHT_ENABLED,true);
        values.putString(BigClockWidgetsSettings.LEFT_TEXT,"左侧文字");values.putString(BigClockWidgetsSettings.RIGHT_TEXT,"右侧天气");row.configure(values);row.refresh(0L,java.util.TimeZone.getTimeZone("UTC"));
        TextView left=(TextView)row.getChildAt(0),right=(TextView)row.getChildAt(1);
        boolean[] policy={true};int[] callbacks={0};String[] selected={""};View[] clicked={null};
        Class<?> listener=Class.forName("dev.puitheme.BigClockTextComponents$OnComponentClickListener");
        Object callback=Proxy.newProxyInstance(listener.getClassLoader(),new Class<?>[]{listener},(proxy,m,argv)->{
            if(m.getName().equals("isComponentClickAllowed"))return policy[0];
            if(m.getName().equals("onComponentClick")){callbacks[0]++;clicked[0]=(View)argv[0];selected[0]=argv[1]+":"+argv[2];launch(helper,context,(String)argv[2]);return null;}
            return null;
        });
        method(BigClockTextComponents.class,"setOnComponentClickListener",listener).invoke(row,callback);
        Method update=method(BigClockTextComponents.class,"updateClickTargets"),disable=method(BigClockTextComponents.class,"disableClickTargets");update.invoke(row);
        check(!row.isClickable()&&!left.isClickable()&&!right.isClickable()&&!left.hasOnClickListeners(),"defaults leave the row and both texts transparent to native gestures");
        values.putBoolean(LEFT_CLICK,true);row.configure(values);update.invoke(row);
        check(!left.isClickable()&&!left.hasOnClickListeners(),"enabling an unselected slot does not steal notification touches");
        values.putString(LEFT_APP,"selected.left");row.configure(values);update.invoke(row);
        check(left.isClickable()&&left.isFocusable()&&!right.isClickable()&&row.getChildCount()==2&&!row.isClickable(),"only the configured real text view becomes a click target; no transparent covering view is added");
        check(left.getImportantForAccessibility()==1 /* IMPORTANT_FOR_ACCESSIBILITY_YES */&&row.getImportantForAccessibility()!=View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS,"enabled text remains available to accessibility instead of being hidden by its row");
        int queries=packages.queries;for(int i=0;i<100;i++)update.invoke(row);
        check(queries==packages.queries,"frame visibility policy never queries PackageManager");
        left.performClick();check(callbacks[0]==1&&clicked[0]==left&&"0:selected.left".equals(selected[0])&&starter.posted==1&&starter.raw==0&&starter.intent.getComponent().getPackageName().equals("selected.left"),"actual left text opens its selected exported app once via ActivityStarter");
        values.putBoolean(RIGHT_CLICK,true);values.putString(RIGHT_APP,"selected.right");row.configure(values);update.invoke(row);right.performClick();
        check(callbacks[0]==2&&"1:selected.right".equals(selected[0])&&starter.intent.getComponent().getPackageName().equals("selected.right"),"right text independently uses its saved package");
        policy[0]=false;right.performClick();check(callbacks[0]==2,"a gesture/panel state change is checked again at click time before a launch");update.invoke(row);
        check(!left.isClickable()&&!right.isClickable()&&!left.hasOnClickListeners()&&!right.hasOnClickListeners(),"closing/moving/locked policy returns both text regions to native gestures");
        policy[0]=true;update.invoke(row);disable.invoke(row);left.performClick();check(!left.isClickable()&&callbacks[0]==2,"explicit owner disable rejects even a stale programmatic click");
        update.invoke(row);values.putBoolean(LEFT_CLICK,false);row.configure(values);left.performClick();check(callbacks[0]==2&&!left.isClickable(),"turning one click switch off releases only that text target");
        rightApp.exported=false;int launches=starter.posted;right.performClick();check(starter.posted==launches&&packages.resolves==0,"an unavailable selected app never falls back to a clock/calendar or unrelated activity");
        rightApp.exported=true;packages.apps.remove("selected.right");check(!launch(helper,context,"selected.right")&&starter.posted==launches,"uninstalled selected app fails safely at launch time");
        check(!launch(helper,context,"")&&!launch(helper,context,"intent:#Intent;end")&&packages.resolves==0,"empty/invalid component targets never trigger a system default");
        row.close();right.performClick();check(callbacks[0]==3&&!right.isClickable()&&!right.hasOnClickListeners(),"close releases child listeners and stale references cannot relaunch");
        helper.clear();check(!launch(helper,context,"selected.left"),"missing native ActivityStarter never launches directly over the keyguard");
        System.out.println("BigClockComponentClickCheck: "+checks+" checks passed");
    }
}
