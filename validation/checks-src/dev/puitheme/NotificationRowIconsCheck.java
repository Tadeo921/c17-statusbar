package dev.puitheme;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.android.internal.widget.CachingIconView;
import com.android.systemui.statusbar.StatusBarIconView;
import com.android.systemui.statusbar.notification.row.ExpandableNotificationRow;
import com.android.systemui.statusbar.notification.row.wrapper.NotificationHeaderViewWrapper;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/** Real header-only binding, shared Phone/cache transactions and reversible glyph drawing. */
public final class NotificationRowIconsCheck {
    static int checks;
    static void eq(Object expected,Object actual){checks++;if(!Objects.equals(expected,actual))throw new AssertionError("row icon "+checks+": "+expected+" != "+actual);}
    static Object field(Object owner,String name)throws Exception{Field field=owner.getClass().getDeclaredField(name);field.setAccessible(true);return field.get(owner);}
    static void drain(ArrayDeque<Runnable> tasks){while(!tasks.isEmpty())tasks.remove().run();}
    static void install(NotificationIconArea area,String name,NotificationAppIconRuntime cache)throws Exception{
        Field field=NotificationIconArea.class.getDeclaredField(name);field.setAccessible(true);((NotificationAppIconRuntime)field.get(area)).releaseRuntime();field.set(area,cache);
    }
    static Runnable changed(NotificationIconArea area)throws Exception{
        Method method=NotificationIconArea.class.getDeclaredMethod("applicationIconsChanged");method.setAccessible(true);
        return ()->{try{method.invoke(area);}catch(Exception failure){throw new AssertionError(failure);}};
    }
    static final class Root extends ViewGroup {
        Root(Context context){super(context);}
        @Override public View findViewById(int id){for(int i=0;i<getChildCount();i++){View child=getChildAt(i);if(child.getId()==id)return child;}return null;}
    }
    static final class Template {
        final ExpandableNotificationRow row;final ViewGroup root;
        final CachingIconView icon;final ImageView avatar,largeImage;final NotificationHeaderViewWrapper wrapper;
        int clicks;
        Template(Context context,String pkg){
            row=new ExpandableNotificationRow(context,180,0,0);row.entry.mSbn.packageName=pkg;
            root=new Root(context);row.addView(root);
            icon=new CachingIconView(context);icon.setImageDrawable(new GradientDrawable());root.addView(icon);
            avatar=new ImageView(context);avatar.setImageDrawable(new GradientDrawable());root.addView(avatar);
            largeImage=new ImageView(context);largeImage.setImageDrawable(new GradientDrawable());root.addView(largeImage);
            wrapper=new NotificationHeaderViewWrapper(context,root,row);
            icon.setOnClickListener(view->clicks++);icon.setClickable(true);
        }
    }
    static Canvas draw(NotificationRowIcons rows,ImageView icon,int[] nativeDraws)throws Throwable{
        Canvas canvas=new Canvas();Object result=rows.draw(icon,canvas,()->{nativeDraws[0]++;return "native";});
        eq(canvas.bitmapDraws==0&&canvas.text==null?"native":null,result);return canvas;
    }
    static Bundle options(){Bundle options=new Bundle();options.putBoolean(NotificationIconArea.MASTER,true);
        options.putBoolean(NotificationIconArea.FOLLOW_LAUNCHER,true);options.putString(NotificationIconArea.MODE,"native");options.putBoolean(NotificationIconArea.SIZE_ENABLED,false);return options;}
    public static void main(String[] args)throws Throwable{
        Handler.postedForCheck.clear();LauncherNotificationIconsCheck.Owner context=new LauncherNotificationIconsCheck.Owner();
        NotificationIconArea area=new NotificationIconArea();area.resolve(NotificationRowIconsCheck.class.getClassLoader());
        ArrayDeque<Runnable> work=new ArrayDeque<>(),done=new ArrayDeque<>(),appWork=new ArrayDeque<>(),appDone=new ArrayDeque<>();int[] loads={0},appLoads={0},nativeDraws={0};
        NotificationAppIconRuntime launcher=new NotificationAppIconRuntime(work::add,done::add,(owner,pkg)->{
            loads[0]++;context.manager.preferred=LauncherNotificationIconsCheck.activity(pkg,pkg+".Main");return LauncherNotificationIcons.load(owner,pkg);},changed(area),true);
        Bitmap custom=LauncherNotificationIconsCheck.pixels(0xff8822cc);
        NotificationAppIconRuntime app=new NotificationAppIconRuntime(appWork::add,appDone::add,(owner,pkg)->{appLoads[0]++;return pkg.equals("com.missing")?null:custom;},changed(area));
        install(area,"launcherIcons",launcher);install(area,"applicationIcons",app);
        NotificationRowIcons rows=new NotificationRowIcons(area);NotificationRowIcons.Hooks hooks=rows.resolve(NotificationRowIconsCheck.class.getClassLoader());
        eq(NotificationHeaderViewWrapper.class,hooks.constructor.getDeclaringClass());eq(3,hooks.constructor.getParameterCount());
        eq("onContentUpdated",hooks.contentUpdated.getName());eq(ExpandableNotificationRow.class,hooks.contentUpdated.getParameterTypes()[0]);
        eq("onReinflated",hooks.reinflated.getName());eq("resolveHeaderViews",hooks.resolveHeaderViews.getName());
        eq("setEntryAdapter",hooks.setEntryAdapter.getName());eq(ImageView.class,hooks.draw.getDeclaringClass());eq("onDraw",hooks.draw.getName());
        Bundle options=options();area.configure(context,options);rows.configure(context,options);
        // Contracted, expanded and HeadsUp templates share a package cache; each only supplies its native header icon.
        Template collapsed=new Template(context,"com.source"),expanded=new Template(context,"com.source"),headsUp=new Template(context,"com.source");
        Drawable nativeIcon=collapsed.icon.getDrawable(),avatar=collapsed.avatar.getDrawable(),largeImage=collapsed.largeImage.getDrawable();
        rows.wrapperChanged(collapsed.wrapper);rows.wrapperChanged(expanded.wrapper);rows.wrapperChanged(headsUp.wrapper);
        eq(1,work.size());eq(0,context.manager.activityReads);eq(0,draw(rows,collapsed.icon,nativeDraws).bitmapDraws);
        NotificationIconOverridesCheck.Owner phone=new NotificationIconOverridesCheck.Owner();new NotificationIconOverridesCheck.Host(202).addView(phone);
        StatusBarIconView phoneIcon=NotificationIconOverridesCheck.icon(phone,"com.source");area.changed(phone);area.draw(phone,new Canvas(),()->null);Handler.drainForCheck();
        eq(1,work.size());drain(work);drain(done);area.draw(phone,new Canvas(),()->null);eq(1,loads[0]);eq(1,context.manager.activityReads);
        for(Template template:Arrays.asList(collapsed,expanded,headsUp)){
            Canvas canvas=draw(rows,template.icon,nativeDraws);eq(1,canvas.bitmapDraws);eq(launcher.icon("com.source"),canvas.drawnBitmapsForCheck.get(0));
        }
        NotificationIconOverridesCheck.NativeCanvas phoneCanvas=new NotificationIconOverridesCheck.NativeCanvas();NotificationIconOverridesCheck.nativeIcon(area,phoneIcon,phoneCanvas);
        eq(launcher.icon("com.source"),phoneCanvas.drawnBitmapsForCheck.get(0));
        eq(0,draw(rows,collapsed.avatar,nativeDraws).bitmapDraws);eq(0,draw(rows,collapsed.largeImage,nativeDraws).bitmapDraws);
        eq(nativeIcon,collapsed.icon.getDrawable());eq(avatar,collapsed.avatar.getDrawable());eq(largeImage,collapsed.largeImage.getDrawable());
        collapsed.icon.performClick();eq(1,collapsed.clicks);
        int sourceReads=collapsed.row.entry.mSbn.packageReads,adapterReads=collapsed.row.adapterReads,previousNative=nativeDraws[0];
        for(int i=0;i<1000;i++){Canvas canvas=draw(rows,collapsed.icon,nativeDraws);eq(1,canvas.bitmapDraws);}
        eq(previousNative,nativeDraws[0]);eq(1,loads[0]);eq(1,context.manager.activityReads);eq(0,context.ownerPmReads);
        eq(sourceReads,collapsed.row.entry.mSbn.packageReads);eq(adapterReads,collapsed.row.adapterReads);
        // Explicit overrides remain independent from the area master and preserve their missing-source native fallback.
        options.putBoolean(NotificationIconOverrides.MASTER,true);options.putString(NotificationIconOverrides.RULES,NotificationIconOverrides.put("[]","com.source","X",true));
        area.configure(context,options);rows.configure(context,options);collapsed.icon.setImageTintList(ColorStateList.valueOf(0xff224466));
        Canvas text=draw(rows,collapsed.icon,nativeDraws);eq("X",text.text);eq(0xff224466,text.textColor);eq(0,text.bitmapDraws);
        collapsed.icon.setImageAlpha(128);text=draw(rows,collapsed.icon,nativeDraws);eq(128,text.textAlpha);
        options.putString(NotificationIconOverrides.RULES,NotificationIconOverrides.putAppIcon("[]","com.source","com.custom",true));
        area.configure(context,options);rows.configure(context,options);eq(0,draw(rows,collapsed.icon,nativeDraws).bitmapDraws);
        drain(appWork);drain(appDone);Canvas bitmap=draw(rows,collapsed.icon,nativeDraws);eq(1,bitmap.bitmapDraws);eq(custom,bitmap.drawnBitmapsForCheck.get(0));eq(1,appLoads[0]);
        options.putString(NotificationIconOverrides.RULES,NotificationIconOverrides.putAppIcon("[]","com.source","com.missing",true));
        area.configure(context,options);rows.configure(context,options);drain(appWork);drain(appDone);eq(0,draw(rows,collapsed.icon,nativeDraws).bitmapDraws);
        options.putBoolean(NotificationIconOverrides.MASTER,false);area.configure(context,options);rows.configure(context,options);
        int invalidations=collapsed.icon.invalidations;context.resolver.changed();eq(true,collapsed.icon.invalidations>invalidations);
        eq(0,draw(rows,collapsed.icon,nativeDraws).bitmapDraws);eq(0,draw(rows,expanded.icon,nativeDraws).bitmapDraws);
        context.manager.drawable=new LauncherNotificationIconsCheck.Source(512,256,0xff10a060,context.manager.draws,context.manager.clones);
        drain(work);drain(done);bitmap=draw(rows,collapsed.icon,nativeDraws);eq(0xff10a060,bitmap.drawnBitmapsForCheck.get(0).pixels[0]);eq(2,loads[0]);
        // Recycled row metadata immediately revokes the old app; unreadable identity stays native.
        collapsed.row.entry.mSbn.packageName="com.rebound";rows.rowChanged(collapsed.row);eq(0,draw(rows,collapsed.icon,nativeDraws).bitmapDraws);
        drain(work);drain(done);eq(1,draw(rows,collapsed.icon,nativeDraws).bitmapDraws);eq(3,loads[0]);
        collapsed.row.setEntryAdapter(()->null);rows.rowChanged(collapsed.row);eq(0,draw(rows,collapsed.icon,nativeDraws).bitmapDraws);
        collapsed.row.setEntryAdapter(()->collapsed.row.entry.mSbn);collapsed.wrapper.onContentUpdated(collapsed.row);rows.wrapperChanged(collapsed.wrapper);drain(work);drain(done);
        // Reinflate/reparent/release restore the exact original image object without writing its drawable or tint.
        CachingIconView previousIcon=expanded.icon;ViewGroup replacementRoot=new Root(context);CachingIconView replacementIcon=new CachingIconView(context);
        replacementIcon.setImageDrawable(new GradientDrawable());replacementRoot.addView(replacementIcon);expanded.row.addView(replacementRoot);
        expanded.wrapper.mView=replacementRoot;expanded.wrapper.onReinflated();rows.wrapperChanged(expanded.wrapper);
        eq(0,draw(rows,previousIcon,nativeDraws).bitmapDraws);eq(1,draw(rows,replacementIcon,nativeDraws).bitmapDraws);
        new ViewGroup(context).addView(replacementIcon);eq(0,draw(rows,replacementIcon,nativeDraws).bitmapDraws);
        collapsed.icon.setVisibility(View.INVISIBLE);eq(0,draw(rows,collapsed.icon,nativeDraws).bitmapDraws);collapsed.icon.setVisibility(View.VISIBLE);
        collapsed.icon.setImageDrawable(null);eq(0,draw(rows,collapsed.icon,nativeDraws).bitmapDraws);collapsed.icon.setImageDrawable(nativeIcon);
        options.putBoolean(StatusBarSettings.SAFE_MODE,true);area.configure(context,options);rows.configure(context,options);eq(0,draw(rows,headsUp.icon,nativeDraws).bitmapDraws);
        options.putBoolean(StatusBarSettings.SAFE_MODE,false);area.configure(context,options);rows.configure(context,options);drain(work);drain(done);
        options.putString(NotificationIconArea.MODE,"heart");area.configure(context,options);rows.configure(context,options);eq(0,draw(rows,headsUp.icon,nativeDraws).bitmapDraws);
        options.putString(NotificationIconArea.MODE,"native");options.putBoolean(NotificationIconArea.MASTER,false);area.configure(context,options);rows.configure(context,options);
        eq(0,draw(rows,headsUp.icon,nativeDraws).bitmapDraws);
        options.putBoolean(NotificationIconOverrides.MASTER,true);options.putString(NotificationIconOverrides.RULES,NotificationIconOverrides.put("[]","com.source","X",true));
        area.configure(context,options);rows.configure(context,options);eq("X",draw(rows,headsUp.icon,nativeDraws).text);
        options.putBoolean(NotificationIconOverrides.MASTER,false);options.putBoolean(NotificationIconArea.MASTER,true);area.configure(context,options);rows.configure(context,options);drain(work);drain(done);
        int knownTemplates=((Map<?,?>)field(rows,"wrappers")).size(),knownReads=headsUp.row.adapterReads;
        rows.detached(new View(context));rows.attached(new View(context));
        eq(knownTemplates,((Map<?,?>)field(rows,"wrappers")).size());eq(knownReads,headsUp.row.adapterReads);
        rows.detached(headsUp.row);eq(0,draw(rows,headsUp.icon,nativeDraws).bitmapDraws);
        rows.detached(headsUp.icon);eq(knownReads,headsUp.row.adapterReads);
        // Reusing an existing HeadsUp/list template need not deliver any content/reinflate event.
        rows.attached(headsUp.row);eq(knownReads+1,headsUp.row.adapterReads);drain(work);drain(done);
        eq(1,draw(rows,headsUp.icon,nativeDraws).bitmapDraws);
        rows.attached(headsUp.icon);eq(knownReads+1,headsUp.row.adapterReads);
        NotificationRowIcons.GroupHooks groupHooks=rows.resolveGroup(NotificationRowIconsCheck.class.getClassLoader());
        eq("initIcon",groupHooks.initIcon.getName());eq("attachCollapsedGroupBadge",groupHooks.attachBadge.getName());
        Template summary=new Template(context,"com.source");rows.wrapperChanged(summary.wrapper);
        com.oplus.systemui.notification.row.oplusgroup.OplusNotificationGroupExtImpl group=new com.oplus.systemui.notification.row.oplusgroup.OplusNotificationGroupExtImpl();
        group.headerWrapper=summary.wrapper;group.oplusIcon=new com.oplus.systemui.notification.row.oplusgroup.OplusGroupNotificationRowIconView(context);
        group.oplusIcon.setImageDrawable(new GradientDrawable());summary.root.addView(group.oplusIcon);rows.groupChanged(group);drain(work);drain(done);
        eq(1,draw(rows,group.oplusIcon,nativeDraws).bitmapDraws);eq(0,draw(rows,summary.avatar,nativeDraws).bitmapDraws);
        int groupReads=summary.row.adapterReads;for(int i=0;i<100;i++)eq(1,draw(rows,group.oplusIcon,nativeDraws).bitmapDraws);
        eq(groupReads,summary.row.adapterReads);
        rows.detached(summary.row);eq(0,draw(rows,group.oplusIcon,nativeDraws).bitmapDraws);
        rows.attached(summary.row);drain(work);drain(done);eq(1,draw(rows,group.oplusIcon,nativeDraws).bitmapDraws);
        summary.row.entry.mSbn.packageName="com.grouprecycled";rows.rowChanged(summary.row);eq(0,draw(rows,group.oplusIcon,nativeDraws).bitmapDraws);
        drain(work);drain(done);eq(1,draw(rows,group.oplusIcon,nativeDraws).bitmapDraws);
        ImageView oldGroup=group.oplusIcon;group.oplusIcon=new com.oplus.systemui.notification.row.oplusgroup.OplusGroupNotificationRowIconView(context);
        group.oplusIcon.setImageDrawable(new GradientDrawable());summary.root.addView(group.oplusIcon);rows.groupChanged(group);
        eq(0,draw(rows,oldGroup,nativeDraws).bitmapDraws);eq(1,draw(rows,group.oplusIcon,nativeDraws).bitmapDraws);
        rows.releaseRuntime();eq(0,((Map<?,?>)field(rows,"images")).size());eq(0,((Map<?,?>)field(rows,"wrappers")).size());
        eq(0,draw(rows,collapsed.icon,nativeDraws).bitmapDraws);eq(nativeIcon,collapsed.icon.getDrawable());eq(0xff224466,collapsed.icon.getImageTintList().getDefaultColor());eq(128,collapsed.icon.getImageAlpha());
        // A row release cannot evict the still-used Phone icon source.
        eq(true,launcher.icon("com.source")!=null);area.changed(phone);area.draw(phone,new Canvas(),()->null);
        phoneCanvas=new NotificationIconOverridesCheck.NativeCanvas();NotificationIconOverridesCheck.nativeIcon(area,phoneIcon,phoneCanvas);eq(1,phoneCanvas.bitmapDraws);
        rows.wrapperChanged(headsUp.wrapper);rows.configure(context,options);eq(0,((Map<?,?>)field(rows,"images")).size());
        area.releaseRuntime();Handler.drainForCheck();drain(work);drain(done);eq(0,context.broadcasts.size());eq(0,context.resolver.registered.size());
        System.out.println(checks+" checks passed (header-only binding, collapsed/expanded/HeadsUp, shared launcher pixels, no draw PM, override priority and native restoration)");
    }
}
