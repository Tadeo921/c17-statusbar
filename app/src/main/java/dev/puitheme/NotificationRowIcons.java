// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.view.View;
import android.view.ViewParent;
import android.widget.ImageView;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/** Only the native notification-header application icon. Message avatars, big pictures,
 * actions, row backgrounds and native ImageView properties remain owned by SystemUI. */
public final class NotificationRowIcons {
    private static final int MAX_BINDINGS=512;
    public interface Draw {Object run()throws Throwable;}
    public static final class Hooks {
        public final Constructor<?> constructor;
        public final Method contentUpdated,reinflated,resolveHeaderViews,setEntryAdapter,draw;
        private Hooks(Constructor<?> constructor,Method contentUpdated,Method reinflated,
                Method resolveHeaderViews,Method setEntryAdapter,Method draw){
            this.constructor=constructor;this.contentUpdated=contentUpdated;this.reinflated=reinflated;
            this.resolveHeaderViews=resolveHeaderViews;this.setEntryAdapter=setEntryAdapter;this.draw=draw;
        }
    }
    private final NotificationIconArea artwork;
    private final Map<Object,Binding> wrappers=new WeakHashMap<>();
    private final Map<ImageView,Binding> images=new WeakHashMap<>();
    private final Map<View,Set<Binding>> identities=new WeakHashMap<>();
    private final Runnable invalidation=this::invalidateIcons;
    private WeakReference<Context> context=new WeakReference<>(null);
    private Class<?> wrapperType,rowType,cachingIconType;
    private Field wrapperRow,wrapperRoot,wrapperIcon;
    private Method entryAdapter,adapterSbn;
    private Class<?> groupType;private Field groupWrapper,groupIcon;
    private boolean released;

    public NotificationRowIcons(NotificationIconArea artwork){
        if(artwork==null)throw new IllegalArgumentException("Notification icon cache is required");
        this.artwork=artwork;
    }
    /** These exact classes/fields/events were confirmed in the device's SystemUI classes2.dex. */
    public Hooks resolve(ClassLoader loader)throws ReflectiveOperationException{
        Class<?> header=loader.loadClass("com.android.systemui.statusbar.notification.row.wrapper.NotificationHeaderViewWrapper");
        Class<?> row=loader.loadClass("com.android.systemui.statusbar.notification.row.ExpandableNotificationRow");
        Class<?> caching=loader.loadClass("com.android.internal.widget.CachingIconView");
        Field icon=header.getDeclaredField("mIcon"),owner=field(header,"mRow"),root=field(header,"mView");
        if(icon.getType()!=caching||!ImageView.class.isAssignableFrom(caching)||owner.getType()!=row||root.getType()!=View.class)
            throw new NoSuchFieldException("Native notification header app-icon contract changed");
        Method adapter=row.getMethod("getEntryAdapter"),sbn=adapter.getReturnType().getMethod("getSbn");
        if(sbn.getReturnType()!=StatusBarNotification.class)throw new NoSuchMethodException("Native notification entry adapter changed");
        Method draw=ImageView.class.getDeclaredMethod("onDraw",Canvas.class);draw.setAccessible(true);
        Hooks hooks=new Hooks(header.getDeclaredConstructor(Context.class,View.class,row),header.getDeclaredMethod("onContentUpdated",row),
                header.getDeclaredMethod("onReinflated"),header.getDeclaredMethod("resolveHeaderViews"),
                row.getDeclaredMethod("setEntryAdapter",adapter.getReturnType()),draw);
        icon.setAccessible(true);owner.setAccessible(true);root.setAccessible(true);
        wrapperType=header;rowType=row;cachingIconType=caching;wrapperIcon=icon;wrapperRow=owner;wrapperRoot=root;entryAdapter=adapter;adapterSbn=sbn;
        return hooks;
    }
    public static final class GroupHooks {
        public final Method initIcon,recreate,attachBadge;
        GroupHooks(Method initIcon,Method recreate,Method attachBadge){this.initIcon=initIcon;this.recreate=recreate;this.attachBadge=attachBadge;}
    }
    /** ColorOS renders the collapsed stack's app icon in a separate OEM view;
     * hooking only NotificationHeaderViewWrapper.mIcon misses that visible copy. */
    public GroupHooks resolveGroup(ClassLoader loader)throws ReflectiveOperationException {
        Class<?> group=loader.loadClass("com.oplus.systemui.notification.row.oplusgroup.OplusNotificationGroupExtImpl");
        Class<?> container=loader.loadClass("com.android.systemui.statusbar.notification.stack.NotificationChildrenContainer");
        Class<?> header=loader.loadClass("android.view.NotificationHeaderView");
        Field icon=group.getDeclaredField("oplusIcon"),wrapper=group.getDeclaredField("headerWrapper");
        if(!ImageView.class.isAssignableFrom(icon.getType())||wrapper.getType()!=wrapperType)
            throw new NoSuchFieldException("Native stack app-icon contract changed");
        GroupHooks result=new GroupHooks(group.getDeclaredMethod("initIcon",container),
                group.getDeclaredMethod("recreateOplusNotificationHeader",container,View.OnClickListener.class,Boolean.TYPE),
                group.getDeclaredMethod("attachCollapsedGroupBadge",header,cachingIconType,container));
        icon.setAccessible(true);wrapper.setAccessible(true);groupType=group;groupIcon=icon;groupWrapper=wrapper;
        return result;
    }
    public void groupChanged(Object group){
        if(released||groupType==null||!groupType.isInstance(group))return;
        Binding previous=wrappers.remove(group);if(previous!=null)forget(previous);
        try {
            Object wrapper=groupWrapper.get(group),candidate=groupIcon.get(group);
            if(!wrapperType.isInstance(wrapper)||!(candidate instanceof ImageView))return;
            bind(group,wrapperRow.get(wrapper),wrapperRoot.get(wrapper),candidate);
        }catch(ReflectiveOperationException|RuntimeException unknown){ }
        finally{refreshSources();invalidateIcons();}
    }
    private void bind(Object identity,Object owner,Object root,Object candidate)throws ReflectiveOperationException {
        if(!rowType.isInstance(owner)||!(root instanceof View)||!(candidate instanceof ImageView))return;
        Object adapter=entryAdapter.invoke(owner),source=adapter==null?null:adapterSbn.invoke(adapter);
        if(!(source instanceof StatusBarNotification))return;
        String pkg=((StatusBarNotification)source).getPackageName();
        try{NotificationIconOverrides.validatePackage(pkg,"El nombre de paquete de la aplicación de notificación no es válido");}catch(IllegalArgumentException unknown){return;}
        if(wrappers.size()>=MAX_BINDINGS)return;
        Binding binding=new Binding(identity,(View)owner,(View)root,(ImageView)candidate,pkg);
        wrappers.put(identity,binding);images.put((ImageView)candidate,binding);
        index((View)owner,binding);index((View)root,binding);index((View)candidate,binding);
        if(context.get()==null&&((View)owner).getContext()!=null)context=new WeakReference<>(((View)owner).getContext());
    }
    private static Field field(Class<?> type,String name)throws NoSuchFieldException{
        for(Class<?> current=type;current!=null;current=current.getSuperclass())try{return current.getDeclaredField(name);}
        catch(NoSuchFieldException absent){ }
        throw new NoSuchFieldException(name);
    }
    /** Call after NotificationIconArea.configure, so both surfaces use one rule/cache transaction. */
    public void configure(Context owner,Bundle options){
        if(released)return;
        if(owner!=null)context=new WeakReference<>(owner);
        refreshSources();invalidateIcons();
    }
    /** After native header binding/reinflation: read package identity once, never in onDraw. */
    public void wrapperChanged(Object wrapper){
        if(groupType!=null&&groupType.isInstance(wrapper)){groupChanged(wrapper);return;}
        if(released||wrapperType==null||!wrapperType.isInstance(wrapper))return;
        Binding previous=wrappers.remove(wrapper);if(previous!=null)forget(previous);
        try{
            Object owner=wrapperRow.get(wrapper),root=wrapperRoot.get(wrapper),candidate=wrapperIcon.get(wrapper);
            if(!rowType.isInstance(owner)||!(root instanceof View)||!cachingIconType.isInstance(candidate))return;
            bind(wrapper,owner,root,candidate);
        }catch(ReflectiveOperationException|RuntimeException staleNativeRow){/* Unknown metadata keeps the real application icon. */}
        finally{refreshSources();invalidateIcons();}
    }
    /** A recycled row can change metadata before RemoteViews finishes rebinding its header. */
    public void rowChanged(Object row){
        if(released||rowType==null||!rowType.isInstance(row))return;
        Set<Binding> found=identities.get(row);if(found==null)return;
        for(Binding binding:new ArrayList<>(found))if(binding.row.get()==row){Object wrapper=binding.wrapper.get();if(wrapper!=null)wrapperChanged(wrapper);}
    }
    public void detached(View view){
        if(released||view==null)return;
        Set<Binding> found=identities.get(view);if(found==null)return;
        boolean changed=false;
        for(Binding binding:new ArrayList<>(found)){
            if(!binding.detached){binding.detached=true;ImageView image=binding.image.get();if(image!=null)image.invalidate();changed=true;}
        }
        if(changed)refreshSources();
    }
    /** SystemUI reuses headers when moving a row between the list and HeadsUp. Their
     * weak identity survives detach, but no custom paint/cache source stays active. */
    public void attached(View view){
        if(released||view==null)return;
        Set<Binding> found=identities.get(view);if(found==null)return;
        for(Binding binding:new ArrayList<>(found))if(binding.detached){
            Object wrapper=binding.wrapper.get();
            if(wrapper!=null&&wrappers.get(wrapper)==binding)wrapperChanged(wrapper);
            else forget(binding);
        }
    }
    private void forget(Binding binding){
        ImageView image=binding.image.get();if(image!=null){if(images.get(image)==binding)images.remove(image);image.invalidate();}
        unindex(binding.row.get(),binding);unindex(binding.root.get(),binding);unindex(image,binding);
    }
    private void index(View view,Binding binding){
        Set<Binding> members=identities.get(view);
        if(members==null){members=Collections.newSetFromMap(new IdentityHashMap<>());identities.put(view,members);}
        members.add(binding);
    }
    private void unindex(View view,Binding binding){
        if(view==null)return;Set<Binding> members=identities.get(view);
        if(members!=null){members.remove(binding);if(members.isEmpty())identities.remove(view);}
    }
    private void refreshSources(){
        Set<String> sources=new LinkedHashSet<>();
        if(artwork.cardFollowsLauncher())for(Binding binding:new ArrayList<>(wrappers.values())){
            if(!binding.detached&&binding.image.get()!=null&&binding.root.get()!=null&&binding.row.get()!=null){sources.add(binding.packageName);
                if(sources.size()>=NotificationAppIconRuntime.MAX_ICONS)break;}
        }
        artwork.cardIconSources(context.get(),sources,invalidation);
    }
    private void invalidateIcons(){
        if(released)return;
        for(ImageView image:new ArrayList<>(images.keySet()))if(image!=null)image.invalidate();
    }
    /** Replace only ImageView's image paint, leaving its parent/foreground/alpha/interaction intact. */
    public Object draw(ImageView image,Canvas canvas,Draw nativeDraw)throws Throwable{
        Binding binding=released?null:images.get(image);
        if(binding==null||binding.detached||binding.wrapper.get()==null||wrappers.get(binding.wrapper.get())!=binding||image.getDrawable()==null
                ||image.getVisibility()!=View.VISIBLE||!descendant(image,binding.root.get()))return nativeDraw.run();
        NotificationIconOverrides.Rule rule=artwork.cardOverride(binding.packageName);
        Bitmap bitmap=artwork.cardArtwork(binding.packageName,rule);
        if(rule==null&&bitmap==null||rule!=null&&rule.isAppIcon()&&bitmap==null)return nativeDraw.run();
        float width=image.getWidth()-image.getPaddingLeft()-image.getPaddingRight();
        float height=image.getHeight()-image.getPaddingTop()-image.getPaddingBottom();
        float side=Math.min(width,height);
        if(side<=0||!Float.isFinite(side))return nativeDraw.run();
        Paint paint=binding.paint;float cx=image.getPaddingLeft()+width/2f,cy=image.getPaddingTop()+height/2f;
        paint.setAlpha(image.getImageAlpha());
        if(bitmap!=null){
            float ratio=(float)bitmap.getWidth()/bitmap.getHeight(),w=ratio>=1?side:side*ratio,h=ratio>=1?side/ratio:side;
            binding.target.set(cx-w/2f,cy-h/2f,cx+w/2f,cy+h/2f);canvas.drawBitmap(bitmap,null,binding.target,paint);return null;
        }
        if(binding.rule!=rule||binding.textSide!=side){
            binding.rule=rule;binding.textSide=side;paint.setTextSize(side);
            paint.getFontMetrics(binding.metrics);
            float measured=Math.max(paint.measureText((CharSequence)rule.text,0,rule.text.length()),binding.metrics.descent-binding.metrics.ascent);
            if(measured>side)paint.setTextSize(side*side/measured);
            paint.getFontMetrics(binding.metrics);binding.textCenter=(binding.metrics.ascent+binding.metrics.descent)/2f;
        }
        android.content.res.ColorStateList tint=image.getImageTintList();
        int color=tint==null?Color.WHITE:tint.getColorForState(image.getDrawableState(),tint.getDefaultColor());
        paint.setColor(color);paint.setAlpha(Math.round(Color.alpha(color)*image.getImageAlpha()/255f));
        canvas.drawText(rule.text,cx,cy-binding.textCenter,paint);return null;
    }
    private static boolean descendant(View image,View root){
        if(root==null)return false;
        for(View current=image;current!=null;){
            if(current==root)return true;ViewParent parent=current.getParent();current=parent instanceof View?(View)parent:null;
        }
        return false;
    }
    public void releaseRuntime(){
        if(released)return;
        invalidateIcons();released=true;wrappers.clear();images.clear();identities.clear();context.clear();
        artwork.cardIconSources(null,Collections.emptySet(),null);
    }
    private static final class Binding {
        final WeakReference<Object> wrapper;
        final WeakReference<View> row,root;
        final WeakReference<ImageView> image;final String packageName;
        final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
        final Paint.FontMetrics metrics=new Paint.FontMetrics();final RectF target=new RectF();
        NotificationIconOverrides.Rule rule;float textSide=-1,textCenter;boolean detached;
        Binding(Object wrapper,View row,View root,ImageView image,String packageName){this.wrapper=new WeakReference<>(wrapper);this.row=new WeakReference<>(row);this.root=new WeakReference<>(root);
            this.image=new WeakReference<>(image);this.packageName=packageName;paint.setTextAlign(Paint.Align.CENTER);}
    }
}
