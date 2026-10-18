// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.view.SurfaceControl;
import android.view.SurfaceControlViewHost;
import android.view.SurfaceView;
import android.view.View;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/** Connects the OEM's in-process remote ViewRoot to its visible SurfaceView.
 * No snapshots, binder polling, view reparenting or surface transactions are used.
 * SurfacePackage copies have different Java identities; compare the native layer.
 */
public final class FluidCloudSurfaceRoots {
    private static final Map<Object,WeakReference<View>> hosts=Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<SurfaceControlViewHost.SurfacePackage,WeakReference<View>> packages=Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<SurfaceView,WeakReference<SurfaceControlViewHost.SurfacePackage>> children=Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<SurfaceView,WeakReference<View>> roots=Collections.synchronizedMap(new WeakHashMap<>());
    private static volatile Method surfaceGetter,sameSurface;
    private static volatile boolean unavailable;
    private FluidCloudSurfaceRoots() { }
    public static void hostView(Object host,View root) {
        if(host==null)return;
        if(root==null)hosts.remove(host);else hosts.put(host,new WeakReference<>(root));
    }
    public static void hostPackage(Object host,Object value) {
        if(!(value instanceof SurfaceControlViewHost.SurfacePackage))return;
        WeakReference<View> reference=hosts.get(host);View root=reference==null?null:reference.get();
        if(root==null)return;
        SurfaceControlViewHost.SurfacePackage valuePackage=(SurfaceControlViewHost.SurfacePackage)value;
        packages.put(valuePackage,new WeakReference<>(root));
        synchronized(children){for(Map.Entry<SurfaceView,WeakReference<SurfaceControlViewHost.SurfacePackage>> child:children.entrySet()) {
            if(same(valuePackage,child.getValue().get()))roots.put(child.getKey(),new WeakReference<>(root));
        }}
    }
    public static void childPackage(SurfaceView child,Object value) {
        if(child==null)return;
        roots.remove(child);
        if(!(value instanceof SurfaceControlViewHost.SurfacePackage)){children.remove(child);return;}
        SurfaceControlViewHost.SurfacePackage valuePackage=(SurfaceControlViewHost.SurfacePackage)value;
        children.put(child,new WeakReference<>(valuePackage));
        synchronized(packages){for(Map.Entry<SurfaceControlViewHost.SurfacePackage,WeakReference<View>> entry:packages.entrySet()) {
            View root=entry.getValue().get();
            if(root!=null&&same(valuePackage,entry.getKey())){roots.put(child,new WeakReference<>(root));return;}
        }}
    }
    public static View contentOf(View card) {
        if(!(card instanceof SurfaceView))return card;
        WeakReference<View> reference=roots.get((SurfaceView)card);
        return reference==null?null:reference.get();
    }
    /** Resolve the current displayed owner at native bind/material events.
     * An entry may retain a floating content reference after leaving the shade;
     * only a real row ancestor or its visible attached SurfaceView owns it here.
     * No cached CustomRowEntry references or surface mutations are consulted.
     */
    static boolean notificationHosted(View host) {
        if(host==null)return false;
        if(notificationAncestor(host))return true;
        synchronized(roots) {
            for(Map.Entry<SurfaceView,WeakReference<View>> entry:roots.entrySet()) {
                SurfaceView child=entry.getKey();
                View remote=entry.getValue().get();
                if(child!=null&&remote!=null&&child.isAttachedToWindow()&&child.isShown()
                        &&notificationAncestor(child)&&contains(remote,host))return true;
            }
        }
        return false;
    }
    private static boolean notificationAncestor(View host) {
        for(int depth=0;host!=null&&depth<64;depth++) {
            if(FluidCloudStyle.CUSTOM_ROW.equals(host.getClass().getName()))return true;
            android.view.ViewParent parent=host.getParent();
            host=parent instanceof View?(View)parent:null;
        }
        return false;
    }
    /** Remote content can be nested in an OEM wrapper, not the entry's direct
     * SurfaceView. Resolve only registered native surfaces at bind events. */
    static boolean contains(View container, View child) {
        if(container==null||child==null)return false;
        for(View current=child;current!=null;) {
            if(current==container)return true;
            android.view.ViewParent parent=current.getParent();
            current=parent instanceof View?(View)parent:null;
        }
        java.util.ArrayList<View> pending=new java.util.ArrayList<>();pending.add(container);
        for(int i=0;i<pending.size()&&i<256;i++) {
            View node=pending.get(i);
            if(node instanceof SurfaceView) {
                View remote=contentOf(node);
                if(remote!=null)for(View current=child;current!=null;) {
                    if(current==remote)return true;
                    android.view.ViewParent parent=current.getParent();
                    current=parent instanceof View?(View)parent:null;
                }
            } else if(node instanceof android.view.ViewGroup) {
                android.view.ViewGroup group=(android.view.ViewGroup)node;
                for(int n=0;n<group.getChildCount()&&pending.size()<256;n++)pending.add(group.getChildAt(n));
            }
        }
        return false;
    }
    public static View childDetached(SurfaceView child) {
        WeakReference<View> reference=roots.remove(child);children.remove(child);
        return reference==null?null:reference.get();
    }
    public static void releaseHost(Object host) {
        WeakReference<View> reference=hosts.remove(host);View root=reference==null?null:reference.get();
        if(root==null)return;
        synchronized(packages){packages.entrySet().removeIf(entry->entry.getValue().get()==root);}
        synchronized(roots){roots.entrySet().removeIf(entry->entry.getValue().get()==root);}
    }
    // This contract runs only in the injected SystemUI hook (system UID), never
    // in the settings app. Both hidden methods are checked against the device's
    // Android 17 framework; reflection failure retains the OEM surface.
    @android.annotation.SuppressLint("BlockedPrivateApi")
    private static boolean same(SurfaceControlViewHost.SurfacePackage first,SurfaceControlViewHost.SurfacePackage second) {
        if(first==null||second==null)return false;
        try {
            if(unavailable)return false;
            if(surfaceGetter==null)synchronized(FluidCloudSurfaceRoots.class){if(surfaceGetter==null){
                Method getter=SurfaceControlViewHost.SurfacePackage.class.getDeclaredMethod("getSurfaceControl");
                Method compare=SurfaceControl.class.getDeclaredMethod("isSameSurface",SurfaceControl.class);
                if(getter.getReturnType()!=SurfaceControl.class||compare.getReturnType()!=Boolean.TYPE)return false;
                getter.setAccessible(true);compare.setAccessible(true);sameSurface=compare;surfaceGetter=getter;
            }}
            SurfaceControl a=(SurfaceControl)surfaceGetter.invoke(first),b=(SurfaceControl)surfaceGetter.invoke(second);
            return a!=null&&b!=null&&a.isValid()&&b.isValid()&&Boolean.TRUE.equals(sameSurface.invoke(a,b));
        } catch(NoSuchMethodException error){unavailable=true;return false;}
        catch(ReflectiveOperationException|RuntimeException|LinkageError error){return false;}
    }
}
