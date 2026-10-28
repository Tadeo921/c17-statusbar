// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;

/** App entry visibility is PackageManager state, independent of runtime/exported settings. The launcher entry exists twice, under
 * the current name (Status C17) and the old one; at most one is enabled. Hidden means both are off; the chosen name is remembered
 * so that showing the icon again brings back the same name. The application's own label (system settings, LSPosed) cannot change at
 * run time and stays the current name. */
public final class LauncherIcon {
    public static final String LAUNCHER_ALIAS="dev.puitheme.MainActivity";
    public static final String LEGACY_ALIAS="dev.puitheme.LegacyLauncher";
    public static final String LEGACY_NAME="C17 Statusbar mejorado", NAME="Status C17";
    private static final String PREFS="launcher_name", LEGACY_KEY="legacy";
    public static final String SETTINGS_ALIAS="dev.puitheme.SettingsActivity";
    public static final String SETTINGS_CATEGORY="de.robv.android.xposed.category.MODULE_SETTINGS";
    private static final String ACTIVITY="dev.puitheme.ModernMainActivity";
    private LauncherIcon(){}
    public static final class Result {
        public final boolean success,hidden,legacyName;
        public final String message;
        private Result(boolean success,boolean hidden,String message){this(success,hidden,false,message);}
        private Result(boolean success,boolean hidden,boolean legacyName,String message){
            this.success=success;this.hidden=hidden;this.legacyName=legacyName;this.message=message;
        }
    }
    public static Result read(Context context) {
        try {
            requireOwn(context);PackageManager manager=context.getPackageManager();
            ComponentName launcher=new ComponentName(ModuleDiagnostics.PACKAGE_NAME,LAUNCHER_ALIAS);
            ActivityInfo info=manager.getActivityInfo(launcher,PackageManager.MATCH_DISABLED_COMPONENTS);
            if(!ACTIVITY.equals(info.targetActivity))throw new IllegalStateException("Unexpected launcher alias");
            ComponentName legacy=new ComponentName(ModuleDiagnostics.PACKAGE_NAME,LEGACY_ALIAS);
            ActivityInfo legacyInfo=manager.getActivityInfo(legacy,PackageManager.MATCH_DISABLED_COMPONENTS);
            if(!ACTIVITY.equals(legacyInfo.targetActivity))throw new IllegalStateException("Unexpected launcher alias");
            boolean current=enabled(manager,launcher,info),old=enabled(manager,legacy,legacyInfo);
            boolean hidden=!current&&!old;
            return new Result(true,hidden,old||hidden&&chosenLegacy(context),"");
        }catch(RuntimeException | PackageManager.NameNotFoundException unavailable){return new Result(false,false,"No se pudo leer el estado del icono del lanzador. Inténtalo de nuevo.");}
    }
    /** The name the settings page shows for this app: the chosen launcher name. */
    public static String displayName(Context context) {
        Result state=read(context);
        return state.success&&state.legacyName?LEGACY_NAME:NAME;
    }
    private static boolean chosenLegacy(Context context) {
        return context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getBoolean(LEGACY_KEY,false);
    }
    private static void remember(Context context,boolean legacy) {
        context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putBoolean(LEGACY_KEY,legacy).apply();
    }
    /** Switches the launcher entry to the old name (or back). The new entry is enabled before the old one is disabled, so there is
     * always an icon; a hidden icon stays hidden and only remembers the choice. */
    public static synchronized Result setLegacyName(Context context,boolean legacy) {
        Result before=read(context);if(!before.success)return before;
        if(before.hidden){remember(context,legacy);return new Result(true,true,legacy,"Nombre guardado; se usará al mostrar el icono: «"+(legacy?LEGACY_NAME:NAME)+"»");}
        if(before.legacyName==legacy){remember(context,legacy);return new Result(true,false,legacy,"El nombre del lanzador ya es «"+(legacy?LEGACY_NAME:NAME)+"»");}
        try {
            PackageManager manager=context.getPackageManager();
            manager.setComponentEnabledSetting(new ComponentName(ModuleDiagnostics.PACKAGE_NAME,legacy?LEGACY_ALIAS:LAUNCHER_ALIAS),
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,PackageManager.DONT_KILL_APP);
            manager.setComponentEnabledSetting(new ComponentName(ModuleDiagnostics.PACKAGE_NAME,legacy?LAUNCHER_ALIAS:LEGACY_ALIAS),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,PackageManager.DONT_KILL_APP);
            Result after=read(context);
            if(!after.success||after.legacyName!=legacy||after.hidden)return new Result(false,after.hidden,after.legacyName,"No se ha confirmado el nombre del lanzador. Inténtalo de nuevo.");
            // Remembered only once the launcher shows it, so a failed switch never comes back later when the icon is shown again.
            remember(context,legacy);
            return new Result(true,false,legacy,"El nombre del lanzador se cambió a «"+(legacy?LEGACY_NAME:NAME)+"». Es posible que el lanzador tarde en actualizarse.");
        }catch(RuntimeException unavailable){
            Result after=read(context);return new Result(false,after.hidden,after.legacyName,"No se pudo cambiar el nombre del lanzador. Inténtalo de nuevo.");
        }
    }
    public static boolean isHidden(Context context) {
        Result result=read(context);if(!result.success)throw new IllegalStateException(result.message);return result.hidden;
    }
    /** Hiding is called only after the UI confirmation. Never kills/finishes the current settings page. */
    public static synchronized Result setHidden(Context context,boolean hidden) {
        Result before=read(context);if(!before.success)return before;
        try {
            PackageManager manager=context.getPackageManager();
            if(hidden) {
                ComponentName settings=new ComponentName(ModuleDiagnostics.PACKAGE_NAME,SETTINGS_ALIAS);
                ActivityInfo info=manager.getActivityInfo(settings,PackageManager.MATCH_DISABLED_COMPONENTS);
                if(!info.exported||!ACTIVITY.equals(info.targetActivity)||!enabled(manager,settings,info))
                    return new Result(false,before.hidden,"El acceso a los ajustes del módulo no está disponible; el icono no se modificó.");
            }
            if(before.hidden==hidden)return new Result(true,hidden,before.legacyName,hidden?"El icono del lanzador está oculto.":"El icono del lanzador está visible.");
            boolean legacy=before.legacyName;
            if(hidden) {
                for(String alias:new String[]{LAUNCHER_ALIAS,LEGACY_ALIAS})
                    manager.setComponentEnabledSetting(new ComponentName(ModuleDiagnostics.PACKAGE_NAME,alias),
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,PackageManager.DONT_KILL_APP);
            } else manager.setComponentEnabledSetting(new ComponentName(ModuleDiagnostics.PACKAGE_NAME,legacy?LEGACY_ALIAS:LAUNCHER_ALIAS),
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,PackageManager.DONT_KILL_APP);
            Result after=read(context);
            if(!after.success||after.hidden!=hidden)return new Result(false,after.success?after.hidden:before.hidden,"No se ha confirmado el estado del icono del lanzador. Inténtalo de nuevo.");
            return new Result(true,hidden,after.legacyName,hidden?"El icono está oculto. Puedes recuperarlo desde los ajustes del módulo en LSPosed.":"El icono se restauró. Es posible que el lanzador tarde en actualizarse.");
        }catch(RuntimeException | PackageManager.NameNotFoundException unavailable){
            Result after=read(context);return new Result(false,after.success?after.hidden:before.hidden,"No se pudo cambiar el icono del lanzador. Inténtalo de nuevo.");
        }
    }
    private static boolean enabled(PackageManager manager,ComponentName component,ActivityInfo info) {
        switch(manager.getComponentEnabledSetting(component)) {
            case PackageManager.COMPONENT_ENABLED_STATE_DEFAULT:return info.enabled;
            case PackageManager.COMPONENT_ENABLED_STATE_ENABLED:return true;
            case PackageManager.COMPONENT_ENABLED_STATE_DISABLED:
            case PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER:
            case PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED:return false;
            default:throw new IllegalStateException("Unknown component state");
        }
    }
    private static void requireOwn(Context context) {
        if(context==null||!ModuleDiagnostics.PACKAGE_NAME.equals(context.getPackageName()))throw new SecurityException("Own app context required");
    }
}
