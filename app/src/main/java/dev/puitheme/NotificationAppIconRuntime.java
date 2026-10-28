// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Build;
import android.database.ContentObserver;
import android.provider.Settings;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Bounded colored application artwork. All package-manager reads and rasterization run off draw. */
public final class NotificationAppIconRuntime {
    static final int MAX_ICONS=NotificationIconOverrides.MAX_RULES,MAX_SIDE=128;
    interface Loader { Bitmap load(Context context,String packageName)throws Exception; }
    private static final class Entry {Bitmap icon;int generation;boolean pending,removed;}
    private final Map<String,Entry> cache=new LinkedHashMap<>();
    private final Executor worker,completion;
    private final Loader loader;
    private final Runnable changed;
    private final ExecutorService ownedWorker;
    private final boolean launcherTheme;
    private final Handler main=new Handler(Looper.getMainLooper());
    private boolean packageRegistered,themeRegistered;
    private int themeObservers;
    private Context context,receiverContext;
    private final BroadcastReceiver themes=new BroadcastReceiver(){
        @Override public void onReceive(Context owner,Intent intent){themeChanged();}
    };
    private final ContentObserver themeObserver=new ContentObserver(main){
        @Override public void onChange(boolean selfChange){themeChanged();}
    };
    private boolean released;
    private final BroadcastReceiver receiver=new BroadcastReceiver(){
        @Override public void onReceive(Context owner,Intent intent){
            if(intent==null||intent.getData()==null)return;
            String pkg=intent.getData().getSchemeSpecificPart();
            packageChanged(pkg,Intent.ACTION_PACKAGE_REMOVED.equals(intent.getAction()));
        }
    };
    public NotificationAppIconRuntime(Runnable changed){
        this(changed,false);
    }
    public NotificationAppIconRuntime(Runnable changed,boolean launcherTheme){
        ExecutorService background=Executors.newSingleThreadExecutor(task->{Thread thread=new Thread(task,"C17-notification-app-icon");thread.setDaemon(true);return thread;});
        this.worker=background;this.ownedWorker=background;this.completion=task->main.post(task);
        this.loader=launcherTheme?LauncherNotificationIcons::load:NotificationAppIconRuntime::load;
        this.changed=changed;this.launcherTheme=launcherTheme;
    }
    /** Deterministic desktop execution uses the same cache and generation transaction. */
    NotificationAppIconRuntime(Executor worker,Executor completion,Loader loader,Runnable changed){
        this(worker,completion,loader,changed,false);
    }
    NotificationAppIconRuntime(Executor worker,Executor completion,Loader loader,Runnable changed,boolean launcherTheme){
        this.worker=worker;this.completion=completion;this.loader=loader;this.changed=changed;this.ownedWorker=null;this.launcherTheme=launcherTheme;
    }
    public synchronized void configure(Context owner,Collection<String> requested){
        if(released)return;
        if(owner!=null){Context application=owner.getApplicationContext();context=application==null?owner:application;}
        LinkedHashSet<String> sources=new LinkedHashSet<>();
        if(requested!=null)for(String pkg:requested){
            NotificationIconOverrides.validatePackage(pkg,"El formato del nombre de paquete de la aplicación de origen del icono no es válido.");
            sources.add(pkg);if(sources.size()>MAX_ICONS)throw new IllegalArgumentException("Se pueden almacenar en caché hasta 128 iconos de aplicaciones.");
        }
        cache.keySet().retainAll(sources);
        if(sources.isEmpty())unregister();else register();
        for(String pkg:sources){
            Entry entry=cache.get(pkg);
            if(entry==null){entry=new Entry();cache.put(pkg,entry);request(pkg,entry);}
            else if(context!=null&&!entry.pending&&entry.icon==null&&!entry.removed&&entry.generation==0)request(pkg,entry);
        }
    }
    public synchronized Bitmap icon(String packageName){Entry entry=cache.get(packageName);return released||entry==null?null:entry.icon;}
    /** Native package events revoke old pixels immediately, including already-recorded glyphs. */
    synchronized void packageChanged(String packageName,boolean removed){
        if(released)return;
        if(launcherTheme&&LauncherNotificationIcons.LAUNCHER.equals(packageName)){themeChanged();return;}
        Entry entry=cache.get(packageName);if(entry==null)return;
        entry.icon=null;entry.generation++;entry.removed=removed;
        // A pending load owns one worker slot. Its stale result reschedules the latest package
        // only once, even if a replacement emits added, changed and replaced together.
        if(!removed&&!entry.pending)request(packageName,entry);
        changed.run();
    }
    /** Theme/configuration events invalidate cached artwork once, never during drawing. */
    synchronized void themeChanged(){
        if(released)return;
        for(Map.Entry<String,Entry> item:cache.entrySet()){
            Entry entry=item.getValue();entry.icon=null;entry.generation++;entry.removed=false;
            if(!entry.pending)request(item.getKey(),entry);
        }
        changed.run();
    }
    private void request(String packageName,Entry entry){
        if(context==null||entry.pending||released)return;
        entry.pending=true;final int generation=++entry.generation;final Context owner=context;
        worker.execute(()->{
            synchronized(NotificationAppIconRuntime.this){if(released||cache.get(packageName)!=entry)return;}
            Bitmap icon=null;try{icon=loader.load(owner,packageName);}catch(Exception|LinkageError|OutOfMemoryError unavailable){ }
            final Bitmap ready=icon;
            completion.execute(()->{
                synchronized(NotificationAppIconRuntime.this){
                    if(released||cache.get(packageName)!=entry)return;
                    entry.pending=false;
                    if(entry.generation!=generation){if(!entry.removed)request(packageName,entry);return;}
                    entry.icon=ready;
                }
                changed.run();
            });
        });
    }
    private void register(){
        if(context==null)return;
        if(receiverContext!=null&&receiverContext!=context)unregister();
        receiverContext=context;
        IntentFilter filter=new IntentFilter();filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED);filter.addDataScheme("package");
        if(!packageRegistered)try{
            if(Build.VERSION.SDK_INT>=33)context.registerReceiver(receiver,filter,Context.RECEIVER_EXPORTED);
            else context.registerReceiver(receiver,filter);
            packageRegistered=true;
        }
        catch(RuntimeException unavailable){ModuleDiagnostics.info("hooks","Application icon package events unavailable; native artwork retained on load failure");}
        if(launcherTheme){
            if(!themeRegistered)try{
                IntentFilter themeFilter=new IntentFilter(Intent.ACTION_CONFIGURATION_CHANGED);
                if(Build.VERSION.SDK_INT>=33)context.registerReceiver(themes,themeFilter,Context.RECEIVER_NOT_EXPORTED);
                else context.registerReceiver(themes,themeFilter);
                themeRegistered=true;
            }
            catch(RuntimeException unavailable){ModuleDiagnostics.info("hooks","Launcher icon configuration events unavailable; retry on next configuration");}
            registerThemeObserver(1,Settings.Global.getUriFor("uxicon.config_version"));
            registerThemeObserver(2,Settings.System.getUriFor("color_theme_setting"));
            registerThemeObserver(4,Settings.Secure.getUriFor("persist.sys.oppo.theme_uuid"));
            // The system icon settings' corner radius and smoothing.
            registerThemeObserver(8,Settings.Secure.getUriFor("com.oplus.icons.icon_config"));
            registerThemeObserver(16,Settings.Global.getUriFor("smooth_corner_enabled"));
        }
    }
    private void registerThemeObserver(int bit,android.net.Uri uri){
        if((themeObservers&bit)!=0)return;
        try{context.getContentResolver().registerContentObserver(uri,false,themeObserver);themeObservers|=bit;}
        catch(RuntimeException unavailable){ModuleDiagnostics.info("hooks","Launcher icon theme observer unavailable; retry on next configuration");}
    }
    private void unregister(){
        if(receiverContext==null)return;
        if(themeRegistered){
            try{receiverContext.unregisterReceiver(themes);}catch(RuntimeException ignored){ }
            themeRegistered=false;
        }
        if(themeObservers!=0){
            try{receiverContext.getContentResolver().unregisterContentObserver(themeObserver);}catch(RuntimeException ignored){ }
            themeObservers=0;
        }
        if(packageRegistered){
            try{receiverContext.unregisterReceiver(receiver);}catch(RuntimeException alreadyDetached){ }
            packageRegistered=false;
        }
        receiverContext=null;
    }
    public synchronized void releaseRuntime(){
        if(released)return;released=true;unregister();cache.clear();context=null;
        if(ownedWorker!=null)ownedWorker.shutdownNow();
    }
    private static Bitmap load(Context context,String packageName)throws Exception{
        NotificationIconOverrides.validatePackage(packageName,"El formato del nombre de paquete de la aplicación de origen del icono no es válido.");
        Drawable source=context.getPackageManager().getApplicationIcon(packageName);
        if(source==null)return null;
        source=source.mutate();
        int width=source.getIntrinsicWidth(),height=source.getIntrinsicHeight();
        if(width<=0||height<=0){width=MAX_SIDE;height=MAX_SIDE;}
        int largest=Math.max(width,height);
        int targetWidth=Math.max(1,(int)((long)width*MAX_SIDE/largest));
        int targetHeight=Math.max(1,(int)((long)height*MAX_SIDE/largest));
        Bitmap result=Bitmap.createBitmap(targetWidth,targetHeight,Bitmap.Config.ARGB_8888);
        source.setBounds(0,0,targetWidth,targetHeight);source.draw(new Canvas(result));return result;
    }
}
