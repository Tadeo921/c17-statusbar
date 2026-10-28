// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.File;
import java.io.IOException;
import java.util.*;

/** App-only migration. No SystemUI callsite is allowed to move or delete legacy libraries. */
public final class IconManagementMigration {
    private static final String PREFS="icon_management_v3_migration",VERSION="version",NOTICE="notice_pending";private static final Object LOCK=new Object();
    public static final class Result{public final boolean noticePending,migrated;public final int retiredLibraries;public final String warning;private Result(boolean notice,boolean migrated,int count,String warning){noticePending=notice;this.migrated=migrated;retiredLibraries=count;this.warning=warning;}}
    private static SharedPreferences prefs(Context context){return context.createDeviceProtectedStorageContext().getSharedPreferences(PREFS,Context.MODE_PRIVATE);}
    public static Result ensure(Context context,SharedPreferences raw){synchronized(LOCK){
        if(context==null||raw==null||!ModuleDiagnostics.PACKAGE_NAME.equals(context.getPackageName()))return new Result(false,false,0,"La migración de iconos solo puede ejecutarse desde esta aplicación.");
        SharedPreferences marker=prefs(context);Map<String,Object> marked=PreferenceWrites.values(marker);if(Integer.valueOf(3).equals(marked.get(VERSION)))return new Result(Boolean.TRUE.equals(marked.get(NOTICE)),false,0,"");int count=0;
        try{
            Map<String,Object> defaults=new LinkedHashMap<>();defaults.put("status_icon_pack_enabled",false);defaults.put("status_icon_pack_layers","[]");defaults.put("status_icon_pack_assignments","[]");if(!raw.contains(IconManagementSettings.KEY))defaults.put(IconManagementSettings.KEY,IconManagementSettings.DEFAULT);
            SharedPreferences.Editor editor=raw.edit();for(Map.Entry<String,Object> row:defaults.entrySet())StatusBarSettings.copyPreference(editor,row.getKey(),row.getValue());if(!PreferenceWrites.commit(raw,editor,defaults,false))return new Result(false,false,0,"La migración de iconos no se guardó. Se volverá a intentar al entrar de nuevo.");
            File files=context.createDeviceProtectedStorageContext().getFilesDir().getCanonicalFile(),old=new File(files,"status-icon-packs"),retired=new File(files,"icon-management-v3-retired");requireChild(files,old);requireChild(files,retired);
            if(old.exists()){
                if(!old.isDirectory())throw new IOException("La ruta de la biblioteca de iconos antigua no es una carpeta.");File[] children=old.listFiles();if(children==null)throw new IOException("No se puede leer la carpeta de la biblioteca de iconos antigua.");for(File child:children)if(child.isDirectory()&&IconManagementSettings.validPackId(child.getName()))count++;
                if(!retired.exists()&&!retired.mkdir()||!retired.isDirectory())throw new IOException("No se pudo poner en cuarentena la biblioteca de iconos antigua.");File destination=new File(retired,"legacy-"+UUID.randomUUID());requireChild(retired,destination);android.system.Os.rename(old.getAbsolutePath(),destination.getAbsolutePath());
            }
            Map<String,Object> completed=new LinkedHashMap<>();completed.put(VERSION,3);completed.put(NOTICE,true);if(!PreferenceWrites.commit(marker,marker.edit().putInt(VERSION,3).putBoolean(NOTICE,true),completed,false))return new Result(false,false,count,"Los iconos antiguos se desactivaron, pero no se guardó la marca de migración. Se volverá a intentar al entrar de nuevo.");
            String warning="";if(retired.isDirectory())try{File[] children=retired.listFiles();if(children==null)throw new IOException("No se puede leer la carpeta de cuarentena.");for(File child:children)if(child.getName().matches("legacy-[0-9a-f-]{36}"))IconManagementRepository.deleteManaged(retired,child);}catch(IOException cleanup){warning="Los iconos antiguos están en cuarentena, pero no se pudieron limpiar algunos archivos:"+cleanup.getMessage();}
            return new Result(true,true,count,warning);
        }catch(Exception failure){return new Result(false,false,count,"La migración aún no terminó. Los recursos antiguos no se usarán en v3:"+failure.getMessage());}
    }}
    private static void requireChild(File root,File child)throws IOException{if(!child.getCanonicalFile().equals(child.getAbsoluteFile())||!child.getCanonicalFile().getParentFile().equals(root.getCanonicalFile()))throw new IOException("La ruta de migración de iconos está fuera de los límites permitidos.");}
    public static void acknowledge(Context context){synchronized(LOCK){if(context!=null&&ModuleDiagnostics.PACKAGE_NAME.equals(context.getPackageName())){SharedPreferences marker=prefs(context);if(!PreferenceWrites.commit(marker,marker.edit().putBoolean(NOTICE,false),Collections.singletonMap(NOTICE,false),false))throw new IllegalStateException("No se pudo guardar el aviso de migración de iconos. Inténtalo de nuevo.");}}}
    private IconManagementMigration(){ }
}
