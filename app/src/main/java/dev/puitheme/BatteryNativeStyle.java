// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;

/** The OEM percentage setting is independent of the battery's icon orientation. */
public final class BatteryNativeStyle {
    public static final String PERCENT_SETTING="display_power_percent";
    private static final Method CURRENT_USER_READ=method(Settings.System.class,"getIntForUser",
            ContentResolver.class,String.class,Integer.TYPE,Integer.TYPE);
    private static final Method ALL_USER_OBSERVE=method(ContentResolver.class,"registerContentObserver",
            android.net.Uri.class,Boolean.TYPE,ContentObserver.class,Integer.TYPE);
    private BatteryNativeStyle() { }
    private static Method method(Class<?> owner,String name,Class<?>... args) {
        try {return owner.getMethod(name,args);}
        catch(ReflectiveOperationException|RuntimeException unavailable){return null;}
    }
    public static int percentStyle(Context context) {
        if(context==null)return 0;
        ContentResolver resolver;
        try {resolver=context.getContentResolver();}
        catch(RuntimeException unavailable){return 0;}
        if(resolver==null)return 0;
        if(CURRENT_USER_READ!=null)try {
            // SystemUI's process context may belong to user 0 while its native binder follows
            // another active user. Match BatteryRepositoryImpl rather than that process user.
            return normalize(((Number)CURRENT_USER_READ.invoke(null,resolver,PERCENT_SETTING,0,-2)).intValue());
        } catch(ReflectiveOperationException|RuntimeException unavailable) { }
        try {return normalize(Settings.System.getInt(resolver,PERCENT_SETTING,0));}
        catch(RuntimeException unavailable){return 0;}
    }
    private static int normalize(int style) {return style==1||style==2?style:0;}
    public static boolean percentEnabled(Context context) {return percentStyle(context)!=0;}

    /** Read at native binding/setting events; never issue Settings queries on each draw. */
    static final class Monitor implements AutoCloseable {
        private final WeakReference<Context> context;
        private final WeakReference<ContentResolver> resolver;
        private final ContentObserver observer;
        private int style;
        private boolean closed;
        Monitor(Context context,Handler handler,Runnable changed) {
            this.context=new WeakReference<>(context);
            ContentResolver source=null;
            try {if(context!=null)source=context.getContentResolver();}
            catch(RuntimeException unavailable) { }
            resolver=new WeakReference<>(source);style=percentStyle(context);
            observer=new ContentObserver(handler) {
                @Override public void onChange(boolean selfChange) {
                    if(!closed&&refresh())changed.run();
                }
            };
            if(source!=null)try {
                if(ALL_USER_OBSERVE!=null)ALL_USER_OBSERVE.invoke(source,
                        Settings.System.getUriFor(PERCENT_SETTING),false,observer,-1);
                else source.registerContentObserver(Settings.System.getUriFor(PERCENT_SETTING),false,observer);
            } catch(ReflectiveOperationException|RuntimeException unavailable) {
                try {source.registerContentObserver(Settings.System.getUriFor(PERCENT_SETTING),false,observer);}
                catch(RuntimeException ignored) { }
                // OEM binding callbacks also refresh this state after settings/user changes.
            }
        }
        boolean refresh() {
            int next=percentStyle(context.get());boolean changed=style!=next;style=next;return changed;
        }
        boolean enabled() {return !closed&&style!=0;}
        @Override public void close() {
            if(closed)return;closed=true;
            ContentResolver source=resolver.get();
            if(source!=null)try {source.unregisterContentObserver(observer);}
            catch(RuntimeException unavailable) { }
        }
    }
}
