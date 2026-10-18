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
        if(context==null||raw==null||!ModuleDiagnostics.PACKAGE_NAME.equals(context.getPackageName()))return new Result(false,false,0,"图标升级只能在本应用执行");
        SharedPreferences marker=prefs(context);Map<String,Object> marked=PreferenceWrites.values(marker);if(Integer.valueOf(3).equals(marked.get(VERSION)))return new Result(Boolean.TRUE.equals(marked.get(NOTICE)),false,0,"");int count=0;
        try{
            Map<String,Object> defaults=new LinkedHashMap<>();defaults.put("status_icon_pack_enabled",false);defaults.put("status_icon_pack_layers","[]");defaults.put("status_icon_pack_assignments","[]");if(!raw.contains(IconManagementSettings.KEY))defaults.put(IconManagementSettings.KEY,IconManagementSettings.DEFAULT);
            SharedPreferences.Editor editor=raw.edit();for(Map.Entry<String,Object> row:defaults.entrySet())StatusBarSettings.copyPreference(editor,row.getKey(),row.getValue());if(!PreferenceWrites.commit(raw,editor,defaults,false))return new Result(false,false,0,"图标升级尚未写入存储，下次进入时重试");
            File files=context.createDeviceProtectedStorageContext().getFilesDir().getCanonicalFile(),old=new File(files,"status-icon-packs"),retired=new File(files,"icon-management-v3-retired");requireChild(files,old);requireChild(files,retired);
            if(old.exists()){
                if(!old.isDirectory())throw new IOException("旧图标库路径不是目录");File[] children=old.listFiles();if(children==null)throw new IOException("旧图标库目录不可读");for(File child:children)if(child.isDirectory()&&IconManagementSettings.validPackId(child.getName()))count++;
                if(!retired.exists()&&!retired.mkdir()||!retired.isDirectory())throw new IOException("无法隔离旧图标库");File destination=new File(retired,"legacy-"+UUID.randomUUID());requireChild(retired,destination);android.system.Os.rename(old.getAbsolutePath(),destination.getAbsolutePath());
            }
            Map<String,Object> completed=new LinkedHashMap<>();completed.put(VERSION,3);completed.put(NOTICE,true);if(!PreferenceWrites.commit(marker,marker.edit().putInt(VERSION,3).putBoolean(NOTICE,true),completed,false))return new Result(false,false,count,"旧图标已停用，升级完成标记未保存，下次进入时重试");
            String warning="";if(retired.isDirectory())try{File[] children=retired.listFiles();if(children==null)throw new IOException("隔离目录不可读");for(File child:children)if(child.getName().matches("legacy-[0-9a-f-]{36}"))IconManagementRepository.deleteManaged(retired,child);}catch(IOException cleanup){warning="旧图标已隔离；部分旧文件尚未清理："+cleanup.getMessage();}
            return new Result(true,true,count,warning);
        }catch(Exception failure){return new Result(false,false,count,"图标升级尚未完成，旧资源不会用于 v3："+failure.getMessage());}
    }}
    private static void requireChild(File root,File child)throws IOException{if(!child.getCanonicalFile().equals(child.getAbsoluteFile())||!child.getCanonicalFile().getParentFile().equals(root.getCanonicalFile()))throw new IOException("图标迁移路径越界");}
    public static void acknowledge(Context context){synchronized(LOCK){if(context!=null&&ModuleDiagnostics.PACKAGE_NAME.equals(context.getPackageName())){SharedPreferences marker=prefs(context);if(!PreferenceWrites.commit(marker,marker.edit().putBoolean(NOTICE,false),Collections.singletonMap(NOTICE,false),false))throw new IllegalStateException("图标升级提示未能保存，请重试");}}}
    private IconManagementMigration(){ }
}
