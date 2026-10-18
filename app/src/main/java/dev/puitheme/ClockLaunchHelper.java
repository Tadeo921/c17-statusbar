// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.provider.AlarmClock;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.List;

/** Use SystemUI's real unlock/collapse route; never launch directly over the keyguard. */
public final class ClockLaunchHelper {
    private static final String STARTER="com.android.systemui.plugins.ActivityStarter";
    private WeakReference<Object> starter=new WeakReference<>(null);
    private Method postStart;
    private boolean usesPostStart;
    private boolean warned;

    public void setActivityStarter(Object value) {
        if(starter.get()==value&&postStart!=null)return;
        starter=new WeakReference<>(null);postStart=null;warned=false;
        if(value==null)return;
        try {
            Class<?> contract=value.getClass().getClassLoader().loadClass(STARTER);
            if(!contract.isInstance(value))return;
            Method method;
            try {
                method=contract.getMethod("postStartActivityDismissingKeyguard",Intent.class,Integer.TYPE);
                usesPostStart=true;
            } catch(NoSuchMethodException missingPost) {
                method=contract.getMethod("startActivity",Intent.class,Boolean.TYPE);
                usesPostStart=false;
            }
            if(method.getReturnType()!=Void.TYPE)return;
            method.setAccessible(true);postStart=method;starter=new WeakReference<>(value);
        } catch(ReflectiveOperationException|RuntimeException|LinkageError unsupported) { report(unsupported); }
    }
    public void clear(){starter.clear();postStart=null;}
    public boolean ready(){return starter.get()!=null&&postStart!=null;}
    public boolean launch(Context context,boolean calendar,long now) {
        return launch(context,calendar,now,"");
    }
    public boolean launch(Context context,boolean calendar,long now,String chosenPackage) {
        Object owner=starter.get();Method launch=postStart;
        if(context==null||owner==null||launch==null)return false;
        try {
            Intent intent=customIntent(context.getPackageManager(),chosenPackage);
            if(intent==null)intent=systemIntent(context,calendar,now);
            if(intent==null)return false;
            launch.invoke(owner,intent,usesPostStart?Integer.valueOf(0):Boolean.TRUE);return true;
        } catch(ReflectiveOperationException|RuntimeException|LinkageError unavailable) { report(unavailable);return false; }
    }
    /** Text components have no default clock/calendar action. A selected app
     * disappearing must fail rather than open an unrelated system target. */
    public boolean launchApp(Context context,String chosenPackage) {
        Object owner=starter.get();Method launch=postStart;
        if(context==null||owner==null||launch==null||chosenPackage==null||chosenPackage.isEmpty()
                ||BigClockWidgetsSettings.launchAppValidationError(chosenPackage)!=null)return false;
        try {
            Intent intent=customIntent(context.getPackageManager(),chosenPackage);
            if(intent==null)return false;
            launch.invoke(owner,intent,usesPostStart?Integer.valueOf(0):Boolean.TRUE);return true;
        } catch(ReflectiveOperationException|RuntimeException|LinkageError unavailable) {report(unavailable);return false;}
    }
    /** Read the same native resource contracts used by OplusQSSimpleHeader's click listener. */
    static Intent systemIntent(Context context,boolean calendar,long now) {
        if(context==null)return null;
        return systemIntent(context.getPackageManager(),calendar,
                nativeString(context,"alarmclock_action"),nativeString(context,"calendar_cn_package"));
    }
    private static String nativeString(Context context,String name) {
        try {
            Resources resources=context.getResources();
            int resource=resources.getIdentifier(name,"string","com.android.systemui");
            // The settings UI has its own Resources. Resolve the same installed
            // SystemUI contract for its default-app label, without baking package names.
            if(resource==0) {
                Context system=context.createPackageContext("com.android.systemui",0);
                resources=system.getResources();
                resource=resources.getIdentifier(name,"string","com.android.systemui");
            }
            return resource==0?"":resources.getString(resource).trim();
        } catch(PackageManager.NameNotFoundException|RuntimeException unavailable){return "";}
    }
    /** No generic calendar VIEW URI or alphabetical choice: these can resolve to unrelated apps. */
    static Intent systemIntent(PackageManager packages,boolean calendar,String nativeClockAction,String nativeCalendarPackage) {
        if(packages==null)return null;
        if(calendar) {
            Intent calendarIntent=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALENDAR);
            if(nativeCalendarPackage!=null&&!nativeCalendarPackage.isEmpty()) {
                calendarIntent.setPackage(nativeCalendarPackage);
                ActivityInfo nativeTarget=systemTarget(packages,calendarIntent);
                if(nativeTarget!=null)return pin(calendarIntent.putExtra("KEY_CHECK_PASSWORD",true)
                        .putExtra("calendar_enter_type",2).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK),nativeTarget);
                calendarIntent.setPackage(null);
            }
            ActivityInfo target=systemTarget(packages,calendarIntent);
            return target==null?null:pin(calendarIntent,target);
        }
        if(nativeClockAction!=null&&!nativeClockAction.isEmpty()) {
            Intent clock=new Intent(nativeClockAction).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
            ActivityInfo target=systemTarget(packages,clock);
            if(target!=null)return pin(clock,target);
        }
        // Alarm API handlers can be CTS activities. Use their app's main entry, never invoke that handler.
        ActivityInfo alarm=systemTarget(packages,new Intent(AlarmClock.ACTION_SHOW_ALARMS));
        if(alarm==null)return null;
        Intent launcher=customIntent(packages,alarm.packageName);
        if(launcher==null)return null;
        try {return systemActivity(packages.getActivityInfo(launcher.getComponent(),0))?launcher:null;}
        catch(PackageManager.NameNotFoundException unavailable){return null;}
    }
    /** User-selected packages must have an enabled, exported launch entry. Missing apps fall back upstream. */
    public static Intent customIntent(PackageManager packages,String packageName) {
        if(packages==null||packageName==null||packageName.isEmpty())return null;
        try {
            Intent launcher=packages.getLaunchIntentForPackage(packageName);
            ComponentName component=launcher==null?null:launcher.getComponent();
            if(component==null||!packageName.equals(component.getPackageName()))return null;
            ActivityInfo activity=packages.getActivityInfo(component,0);
            int state=packages.getComponentEnabledSetting(component);
            if(!enabledActivity(activity)||!packageName.equals(activity.packageName)
                    ||state!=PackageManager.COMPONENT_ENABLED_STATE_DEFAULT&&state!=PackageManager.COMPONENT_ENABLED_STATE_ENABLED)return null;
            return new Intent(launcher).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        } catch(PackageManager.NameNotFoundException|RuntimeException unavailable){return null;}
    }
    private static Intent pin(Intent intent,ActivityInfo target) {
        return intent.setComponent(new ComponentName(target.packageName,target.name)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }
    private static ActivityInfo systemTarget(PackageManager packages,Intent intent) {
        ResolveInfo resolved=packages.resolveActivity(intent,PackageManager.MATCH_DEFAULT_ONLY);
        if(resolved!=null&&systemActivity(resolved.activityInfo))return resolved.activityInfo;
        List<ResolveInfo> handlers=packages.queryIntentActivities(intent,PackageManager.MATCH_DEFAULT_ONLY);
        ActivityInfo chosen=null;
        if(handlers!=null)for(ResolveInfo handler:handlers){
            ActivityInfo candidate=handler==null?null:handler.activityInfo;
            if(!systemActivity(candidate))continue;
            if(chosen!=null&&(!candidate.packageName.equals(chosen.packageName)||!candidate.name.equals(chosen.name)))return null;
            chosen=candidate;
        }
        return chosen;
    }
    private static boolean systemActivity(ActivityInfo info) {
        return enabledActivity(info)&&!"android".equals(info.packageName)
                &&(info.applicationInfo.flags&(ApplicationInfo.FLAG_SYSTEM|ApplicationInfo.FLAG_UPDATED_SYSTEM_APP))!=0;
    }
    private static boolean enabledActivity(ActivityInfo info) {
        return info!=null&&info.enabled&&info.exported&&info.name!=null&&info.packageName!=null
                &&info.applicationInfo!=null&&info.applicationInfo.enabled;
    }
    private void report(Throwable error){
        if(warned)return;warned=true;
        ModuleDiagnostics.error("bigclock","System calendar/clock launch unavailable; notification gestures retained",error);
    }
}
