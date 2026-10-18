package dev.puitheme;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import com.android.systemui.statusbar.StatusBarIconView;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/** Native launcher-context artwork, event generations and real notification-glyph integration. */
public final class LauncherNotificationIconsCheck {
    static int checks;
    static void eq(Object expected,Object actual){checks++;if(!Objects.equals(expected,actual))throw new AssertionError(expected+" != "+actual);}
    static void near(float expected,float actual){checks++;if(Math.abs(expected-actual)>.001f)throw new AssertionError(expected+" != "+actual);}
    static ResolveInfo activity(String pkg,String name){
        ResolveInfo result=new ResolveInfo();result.activityInfo=new ActivityInfo();
        result.activityInfo.packageName=pkg;result.activityInfo.name=name;result.activityInfo.enabled=true;result.activityInfo.exported=true;
        result.activityInfo.applicationInfo=new ApplicationInfo();result.activityInfo.applicationInfo.enabled=true;return result;
    }
    static final class Source extends Drawable {
        final int width,height,color;final int[] draws,clones;
        Source(int width,int height,int color,int[] draws,int[] clones){this.width=width;this.height=height;this.color=color;this.draws=draws;this.clones=clones;}
        @Override public int getIntrinsicWidth(){return width;}@Override public int getIntrinsicHeight(){return height;}
        @Override public void draw(Canvas canvas){
            draws[0]++;Bitmap texture=Bitmap.createBitmap(2,1,Bitmap.Config.ARGB_8888);texture.pixels[0]=color;texture.pixels[1]=0xff365fe8;
            android.graphics.Rect bounds=getBounds();canvas.drawBitmap(texture,null,new RectF(bounds.left,bounds.top,bounds.right,bounds.bottom),new Paint(0));
        }
        @Override public ConstantState getConstantState(){return new ConstantState(){@Override public Drawable newDrawable(){clones[0]++;return new Source(width,height,color,draws,clones);}};}
        @Override public void setAlpha(int alpha){}@Override public void setColorFilter(android.graphics.ColorFilter filter){}@Override public int getOpacity(){return -3;}
    }
    static final class Manager extends PackageManager {
        ResolveInfo preferred=activity("com.source","com.source.Launcher");List<ResolveInfo> matches=Collections.emptyList();
        final int[] draws={0},clones={0};Drawable drawable=new Source(512,256,0xfff03870,draws,clones);
        int applicationReads,activityReads,resolves,queries;Intent resolvedRequest,iconRequest;
        @Override public ResolveInfo resolveActivity(Intent intent,int flags){resolves++;resolvedRequest=intent;return preferred;}
        @Override public List<ResolveInfo> queryIntentActivities(Intent intent,int flags){queries++;return matches;}
        @Override public Drawable getActivityIcon(Intent intent){activityReads++;iconRequest=intent;return drawable;}
        @Override public Drawable getApplicationIcon(String pkg){applicationReads++;throw new AssertionError("Native UX icon must use launch activity, not application icon");}
        @Override public ActivityInfo getActivityInfo(ComponentName component,int flags){return preferred==null?null:preferred.activityInfo;}
        @Override public int getComponentEnabledSetting(ComponentName component){return 0;}
        @Override public void setComponentEnabledSetting(ComponentName component,int state,int flags){}
    }
    static final class Resolver extends ContentResolver {
        final Map<String,ContentObserver> registered=new LinkedHashMap<>();int registrations,unregistrations,failures;
        String failUri;
        @Override public void registerContentObserver(android.net.Uri uri,boolean descendants,ContentObserver observer){
            registrations++;eq(false,descendants);
            if(failures>0&&uri.toString().equals(failUri)){failures--;throw new IllegalStateException("provider temporarily unavailable");}
            registered.put(uri.toString(),observer);
        }
        @Override public void unregisterContentObserver(ContentObserver observer){unregistrations++;registered.values().removeIf(value->value==observer);}
        void changed(){if(!registered.isEmpty())registered.values().iterator().next().onChange(false);}
    }
    static final class Owner extends Context {
        final Manager manager=new Manager();final Resolver resolver=new Resolver();
        final Map<BroadcastReceiver,IntentFilter> broadcasts=new LinkedHashMap<>();final List<Integer> receiverFlags=new ArrayList<>();
        int contexts,ownerPmReads,receiverAttempts,unregistrations,failTheme,failPackage;
        String contextPackage;int contextFlags;
        final Context launcher=new Context(){@Override public PackageManager getPackageManager(){return manager;}};
        @Override public Context createPackageContext(String name,int flags){contexts++;contextPackage=name;contextFlags=flags;return launcher;}
        @Override public PackageManager getPackageManager(){ownerPmReads++;throw new AssertionError("Owner SystemUI PackageManager is not launcher UX context");}
        @Override public ContentResolver getContentResolver(){return resolver;}
        @Override public Intent registerReceiver(BroadcastReceiver receiver,IntentFilter filter,int flags){
            receiverAttempts++;
            if(filter.actions.contains(Intent.ACTION_CONFIGURATION_CHANGED)&&failTheme>0){failTheme--;throw new IllegalStateException("theme receiver temporarily unavailable");}
            if(filter.actions.contains(Intent.ACTION_PACKAGE_ADDED)&&failPackage>0){failPackage--;throw new IllegalStateException("package receiver temporarily unavailable");}
            broadcasts.put(receiver,filter);receiverFlags.add(flags);return null;
        }
        @Override public void unregisterReceiver(BroadcastReceiver receiver){unregistrations++;broadcasts.remove(receiver);}
        void configuration(){for(Map.Entry<BroadcastReceiver,IntentFilter> item:new ArrayList<>(broadcasts.entrySet()))
            if(item.getValue().actions.contains(Intent.ACTION_CONFIGURATION_CHANGED))item.getKey().onReceive(this,new Intent(Intent.ACTION_CONFIGURATION_CHANGED));}
        void packageEvent(String pkg,String action){for(Map.Entry<BroadcastReceiver,IntentFilter> item:new ArrayList<>(broadcasts.entrySet()))
            if(item.getValue().actions.contains(action))item.getKey().onReceive(this,new Intent(action,android.net.Uri.parse("package:"+pkg)));}
    }
    static void raster()throws Exception{
        Owner owner=new Owner();owner.manager.drawable.setBounds(7,8,90,91);
        Bitmap image=LauncherNotificationIcons.load(owner,"com.source");eq(1,owner.contexts);eq("com.android.launcher",owner.contextPackage);eq(0,owner.contextFlags);
        eq(0,owner.ownerPmReads);eq(1,owner.manager.activityReads);eq(0,owner.manager.applicationReads);
        eq(Intent.ACTION_MAIN,owner.manager.resolvedRequest.getAction());eq(true,owner.manager.resolvedRequest.hasCategory(Intent.CATEGORY_LAUNCHER));
        eq("com.source",owner.manager.resolvedRequest.getPackage());eq("com.source.Launcher",owner.manager.iconRequest.getComponent().getClassName());
        eq(128,image.getWidth());eq(64,image.getHeight());eq(Bitmap.Config.ARGB_8888,image.getConfig());
        eq(0xfff03870,image.pixels[0]);eq(0xff365fe8,image.pixels[image.pixels.length-1]);eq(1,owner.manager.draws[0]);eq(1,owner.manager.clones[0]);
        eq(7,owner.manager.drawable.getBounds().left);eq(90,owner.manager.drawable.getBounds().right); // PM-owned pixels are not resized in-place.
        owner.manager.drawable=new Source(80,320,0xffabcdef,owner.manager.draws,owner.manager.clones);
        image=LauncherNotificationIcons.load(owner,"com.source");eq(32,image.getWidth());eq(128,image.getHeight());
        owner.manager.drawable=new Source(-1,0,0xff123456,owner.manager.draws,owner.manager.clones);
        image=LauncherNotificationIcons.load(owner,"com.source");eq(128,image.getWidth());eq(128,image.getHeight());
        owner.manager.drawable=null;eq(null,LauncherNotificationIcons.load(owner,"com.source"));
        eq(null,LauncherNotificationIcons.load(null,"com.source"));
        boolean malformed=false;try{LauncherNotificationIcons.load(owner,"../com.source");}catch(IllegalArgumentException expected){malformed=true;}eq(true,malformed);
        Manager manager=new Manager();manager.preferred=activity("com.foreign","com.foreign.Home");
        ResolveInfo disabled=activity("com.source","com.source.Disabled");disabled.activityInfo.enabled=false;
        ResolveInfo privateActivity=activity("com.source","com.source.Private");privateActivity.activityInfo.exported=false;
        ResolveInfo disabledApp=activity("com.source","com.source.DisabledApp");disabledApp.activityInfo.applicationInfo.enabled=false;
        manager.matches=Arrays.asList(null,manager.preferred,disabled,privateActivity,disabledApp,activity("com.source","com.source.Valid"));
        Intent selected=LauncherNotificationIcons.launcherIntent(manager,"com.source");eq("com.source.Valid",selected.getComponent().getClassName());eq(1,manager.queries);
        manager.matches=Arrays.asList(disabled,privateActivity,disabledApp);eq(null,LauncherNotificationIcons.launcherIntent(manager,"com.source"));
        manager.preferred=activity("com.source",null);manager.matches=Collections.emptyList();eq(null,LauncherNotificationIcons.launcherIntent(manager,"com.source"));
        manager.preferred=activity("com.source","com.source.NoApplication");manager.preferred.activityInfo.applicationInfo=null;eq(null,LauncherNotificationIcons.launcherIntent(manager,"com.source"));
    }
    static void drain(ArrayDeque<Runnable> queue){while(!queue.isEmpty())queue.remove().run();}
    static Bitmap pixels(int color){Bitmap bitmap=Bitmap.createBitmap(128,64,Bitmap.Config.ARGB_8888);bitmap.eraseColor(color);return bitmap;}
    static void runtime()throws Exception{
        ArrayDeque<Runnable> work=new ArrayDeque<>(),done=new ArrayDeque<>();int[] reads={0},updates={0};Bitmap[] current={pixels(0xffff0055)};
        Owner owner=new Owner();NotificationAppIconRuntime runtime=new NotificationAppIconRuntime(work::add,done::add,
                (ctx,pkg)->{reads[0]++;return "com.missing".equals(pkg)?null:current[0];},()->updates[0]++,true);
        List<String> sources=Arrays.asList("com.source","com.missing");runtime.configure(owner,sources);
        eq(2,owner.broadcasts.size());eq(Arrays.asList(Context.RECEIVER_EXPORTED,Context.RECEIVER_NOT_EXPORTED),owner.receiverFlags);
        eq(new LinkedHashSet<>(Arrays.asList("content://settings/global/uxicon.config_version","content://settings/system/color_theme_setting",
                "content://settings/secure/persist.sys.oppo.theme_uuid","content://settings/secure/com.oplus.icons.icon_config",
                "content://settings/global/smooth_corner_enabled")),owner.resolver.registered.keySet());
        eq(2,work.size());drain(work);drain(done);eq(2,reads[0]);eq(current[0],runtime.icon("com.source"));eq(null,runtime.icon("com.missing"));
        for(int i=0;i<1000;i++){runtime.configure(owner,sources);runtime.icon("com.source");runtime.icon("com.missing");}
        eq(2,reads[0]);eq(2,owner.receiverAttempts);eq(5,owner.resolver.registrations);eq(0,work.size());
        int oldUpdates=updates[0];owner.resolver.changed();eq(oldUpdates+1,updates[0]);eq(null,runtime.icon("com.source"));eq(2,work.size());
        drain(work);Bitmap blue=pixels(0xff0055ff);current[0]=blue;
        owner.configuration();owner.resolver.changed();eq(0,work.size()); // One in-flight load per source despite several theme events.
        drain(done);eq(2,work.size());eq(null,runtime.icon("com.source"));drain(work);drain(done);eq(blue,runtime.icon("com.source"));eq(6,reads[0]);
        owner.packageEvent("com.unrelated",Intent.ACTION_PACKAGE_CHANGED);eq(0,work.size());
        owner.packageEvent("com.android.launcher",Intent.ACTION_PACKAGE_REPLACED);eq(null,runtime.icon("com.source"));eq(2,work.size());drain(work);drain(done);eq(8,reads[0]);
        owner.packageEvent("com.source",Intent.ACTION_PACKAGE_REMOVED);eq(null,runtime.icon("com.source"));eq(0,work.size());
        runtime.configure(owner,sources);eq(0,work.size());owner.packageEvent("com.source",Intent.ACTION_PACKAGE_ADDED);eq(1,work.size());
        drain(work);drain(done);eq(9,reads[0]);eq(blue,runtime.icon("com.source"));
        owner.configuration();drain(work);oldUpdates=updates[0];runtime.releaseRuntime();drain(done);
        eq(oldUpdates,updates[0]);eq(null,runtime.icon("com.source"));eq(0,owner.broadcasts.size());eq(0,owner.resolver.registered.size());eq(2,owner.unregistrations);eq(1,owner.resolver.unregistrations);
        owner.configuration();runtime.themeChanged();runtime.configure(owner,sources);eq(0,work.size());

        Owner partial=new Owner();partial.failTheme=1;partial.failPackage=1;partial.resolver.failUri="content://settings/secure/persist.sys.oppo.theme_uuid";partial.resolver.failures=1;
        NotificationAppIconRuntime retried=new NotificationAppIconRuntime(work::add,done::add,(ctx,pkg)->blue,()->{},true);
        retried.configure(partial,Collections.singletonList("com.source"));eq(0,partial.broadcasts.size());eq(4,partial.resolver.registered.size());
        retried.configure(partial,Collections.singletonList("com.source"));eq(2,partial.broadcasts.size());eq(5,partial.resolver.registered.size());
        eq(4,partial.receiverAttempts);eq(6,partial.resolver.registrations);retried.configure(partial,Collections.singletonList("com.source"));
        eq(4,partial.receiverAttempts);eq(6,partial.resolver.registrations);eq(1,work.size());drain(work);drain(done);
        retried.configure(partial,Collections.emptyList());eq(0,partial.broadcasts.size());eq(0,partial.resolver.registered.size());
        eq(2,partial.unregistrations);eq(1,partial.resolver.unregistrations);retried.releaseRuntime();
        // A failed theme receiver cannot leak observers on disabling, nor affect the app-icon-only path.
        partial=new Owner();partial.failTheme=1;
        retried=new NotificationAppIconRuntime(work::add,done::add,(ctx,pkg)->blue,()->{},true);retried.configure(partial,Collections.singletonList("com.source"));
        eq(1,partial.broadcasts.size());eq(5,partial.resolver.registered.size());retried.releaseRuntime();eq(0,partial.broadcasts.size());eq(0,partial.resolver.registered.size());drain(work);drain(done);
        Owner appOwner=new Owner();retried=new NotificationAppIconRuntime(work::add,done::add,(ctx,pkg)->blue,()->{});
        retried.configure(appOwner,Collections.singletonList("com.source"));eq(1,appOwner.broadcasts.size());eq(0,appOwner.resolver.registered.size());retried.releaseRuntime();drain(work);drain(done);
    }
    static void install(NotificationIconArea area,String name,NotificationAppIconRuntime runtime)throws Exception{
        Field field=NotificationIconArea.class.getDeclaredField(name);field.setAccessible(true);((NotificationAppIconRuntime)field.get(area)).releaseRuntime();field.set(area,runtime);
    }
    static Runnable changed(NotificationIconArea area)throws Exception{
        Method method=NotificationIconArea.class.getDeclaredMethod("applicationIconsChanged");method.setAccessible(true);
        return ()->{try{method.invoke(area);}catch(Exception failure){throw new AssertionError(failure);}};
    }
    static Bundle options(){Bundle bundle=new Bundle();bundle.putBoolean(NotificationIconArea.MASTER,true);bundle.putBoolean(NotificationIconArea.FOLLOW_LAUNCHER,true);
        bundle.putString(NotificationIconArea.MODE,"native");bundle.putBoolean(NotificationIconArea.SIZE_ENABLED,false);return bundle;}
    static NotificationIconOverridesCheck.NativeCanvas glyph(NotificationIconArea area,StatusBarIconView icon)throws Throwable{
        NotificationIconOverridesCheck.NativeCanvas canvas=new NotificationIconOverridesCheck.NativeCanvas();NotificationIconOverridesCheck.nativeIcon(area,icon,canvas);return canvas;
    }
    static void integration()throws Throwable{
        Handler.postedForCheck.clear();NotificationIconArea area=new NotificationIconArea();area.resolve(LauncherNotificationIconsCheck.class.getClassLoader());
        ArrayDeque<Runnable> work=new ArrayDeque<>(),done=new ArrayDeque<>(),appWork=new ArrayDeque<>(),appDone=new ArrayDeque<>();int[] launcherReads={0},appReads={0};
        Owner context=new Owner();Bitmap appBitmap=pixels(0xff88aa22);
        NotificationAppIconRuntime launcher=new NotificationAppIconRuntime(work::add,done::add,(ctx,pkg)->{launcherReads[0]++;return LauncherNotificationIcons.load(ctx,pkg);},changed(area),true);
        NotificationAppIconRuntime app=new NotificationAppIconRuntime(appWork::add,appDone::add,(ctx,pkg)->{appReads[0]++;return "com.missing".equals(pkg)?null:appBitmap;},changed(area));
        install(area,"launcherIcons",launcher);install(area,"applicationIcons",app);
        NotificationIconOverridesCheck.Owner owner=new NotificationIconOverridesCheck.Owner();new NotificationIconOverridesCheck.Host(202).addView(owner);
        StatusBarIconView icon=NotificationIconOverridesCheck.icon(owner,"com.source");area.changed(owner);Bundle settings=options();
        settings.putBoolean(NotificationIconArea.COLOR_ENABLED,true);settings.putInt("notification_icons_color_light",0xff00ff00);
        area.configure(context,settings);area.draw(owner,new Canvas(),()->null);eq(0,context.manager.activityReads);eq(0,work.size());
        NotificationIconOverridesCheck.NativeCanvas canvas=glyph(area,icon);eq(0,canvas.bitmapDraws);near(32,canvas.glyphHeight);
        Handler.drainForCheck();eq(1,work.size());eq(0,context.manager.activityReads);drain(work);drain(done);area.draw(owner,new Canvas(),()->null);
        Bitmap themed=launcher.icon("com.source");eq(0xfff03870,themed.pixels[0]);canvas=glyph(area,icon);
        eq(1,canvas.bitmapDraws);eq(themed,canvas.drawnBitmapsForCheck.get(0));eq(0,canvas.layers);near(0,canvas.glyphHeight);near(6,canvas.dotSize);
        android.service.notification.StatusBarNotification metadata=icon.getNotification();int metadataReads=metadata.packageReads,getterReads=icon.getterCalls;
        for(int i=0;i<1000;i++){canvas=glyph(area,icon);eq(1,canvas.bitmapDraws);near(6,canvas.dotSize);}
        eq(1,launcherReads[0]);eq(1,context.manager.activityReads);eq(0,context.ownerPmReads);eq(metadataReads,metadata.packageReads);eq(getterReads,icon.getterCalls);
        icon.iconAppearAmount=.1f;canvas=glyph(area,icon);eq(1,canvas.bitmapDraws);near(6,canvas.dotSize);icon.iconAppearAmount=1f;
        // Explicit text and app artwork overrides take priority over launcher colors.
        settings.putBoolean(NotificationIconOverrides.MASTER,true);settings.putString(NotificationIconOverrides.RULES,NotificationIconOverrides.put("[]","com.source","X",true));
        area.configure(context,settings);area.draw(owner,new Canvas(),()->null);canvas=glyph(area,icon);eq("X",canvas.text);eq(0,canvas.bitmapDraws);
        settings.putString(NotificationIconOverrides.RULES,NotificationIconOverrides.putAppIcon("[]","com.source","com.custom",true));
        area.configure(context,settings);area.draw(owner,new Canvas(),()->null);canvas=glyph(area,icon);eq(0,canvas.bitmapDraws);near(32,canvas.glyphHeight);
        drain(appWork);drain(appDone);area.draw(owner,new Canvas(),()->null);canvas=glyph(area,icon);eq(1,canvas.bitmapDraws);eq(appBitmap,canvas.drawnBitmapsForCheck.get(0));eq(1,appReads[0]);
        settings.putString(NotificationIconOverrides.RULES,NotificationIconOverrides.putAppIcon("[]","com.source","com.missing",true));
        area.configure(context,settings);drain(appWork);drain(appDone);area.draw(owner,new Canvas(),()->null);canvas=glyph(area,icon);
        eq(0,canvas.bitmapDraws);near(32,canvas.glyphHeight); // Missing explicit override keeps native, does not silently use launcher.
        settings.putBoolean(NotificationIconOverrides.MASTER,false);area.configure(context,settings);area.draw(owner,new Canvas(),()->null);canvas=glyph(area,icon);eq(1,canvas.bitmapDraws);
        // A theme generation revokes glyph pixels immediately, before the container is re-recorded.
        launcher.themeChanged();canvas=glyph(area,icon);eq(0,canvas.bitmapDraws);near(32,canvas.glyphHeight);
        context.manager.drawable=new Source(512,256,0xff119955,context.manager.draws,context.manager.clones);
        drain(work);drain(done);area.draw(owner,new Canvas(),()->null);canvas=glyph(area,icon);eq(1,canvas.bitmapDraws);
        eq(0xff119955,canvas.drawnBitmapsForCheck.get(0).pixels[0]);eq(2,launcherReads[0]);
        settings.putBoolean(NotificationIconArea.FOLLOW_LAUNCHER,false);area.configure(context,settings);canvas=glyph(area,icon);eq(0,canvas.bitmapDraws);eq(null,launcher.icon("com.source"));
        settings.putBoolean(NotificationIconArea.FOLLOW_LAUNCHER,true);area.configure(context,settings);area.draw(owner,new Canvas(),()->null);Handler.drainForCheck();drain(work);drain(done);area.draw(owner,new Canvas(),()->null);
        settings.putBoolean(StatusBarSettings.SAFE_MODE,true);area.configure(context,settings);canvas=glyph(area,icon);eq(0,canvas.bitmapDraws);near(32,canvas.glyphHeight);
        settings.putBoolean(StatusBarSettings.SAFE_MODE,false);area.configure(context,settings);area.draw(owner,new Canvas(),()->null);Handler.drainForCheck();drain(work);drain(done);area.draw(owner,new Canvas(),()->null);
        new NotificationIconOverridesCheck.Host(404).addView(owner);area.nativeMaxIcons(owner,4);canvas=glyph(area,icon);eq(0,canvas.bitmapDraws);near(32,canvas.glyphHeight);
        new NotificationIconOverridesCheck.Host(202).addView(owner);area.changed(owner);area.draw(owner,new Canvas(),()->null);Handler.drainForCheck();drain(work);drain(done);area.draw(owner,new Canvas(),()->null);
        settings.putString(NotificationIconArea.MODE,"heart");area.configure(context,settings);area.draw(owner,new Canvas(),()->null);eq(null,launcher.icon("com.source"));eq(0,work.size());
        area.releaseRuntime();Handler.drainForCheck();drain(work);drain(done);canvas=glyph(area,icon);eq(0,canvas.bitmapDraws);near(32,canvas.glyphHeight);
        eq(0,context.broadcasts.size());eq(0,context.resolver.registered.size());
    }
    public static void main(String[] args)throws Throwable{raster();runtime();integration();System.out.println(checks+" checks passed (launcher UX context, bounded native colors, theme generations, failure retry, override priority and draw-only cache)");}
}
