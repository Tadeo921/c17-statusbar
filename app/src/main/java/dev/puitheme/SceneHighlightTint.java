// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.graphics.Color;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/** Audited native lockscreen optics: own parameter copies, retaining every native control and colour alpha. */
final class SceneHighlightTint {
    private final Params stroke,shadow;
    private SceneHighlightTint(Params stroke,Params shadow){this.stroke=stroke;this.shadow=shadow;}
    static SceneHighlightTint resolve(ClassLoader loader){
        try{return new SceneHighlightTint(new Params(loader,"GradientStrokeLineParams","StrokeLineColor"),
                new Params(loader,"InnerShadowParams","ShadowColor"));}
        catch(ReflectiveOperationException|RuntimeException|LinkageError changed){return null;}
    }
    /** Called only when the existing recipe cache rebuilds, never by a draw or shader callback. */
    Object[] tint(Object nativeStroke,Object nativeShadow,int rgb){
        try{return new Object[]{stroke.copy(nativeStroke,rgb),shadow.copy(nativeShadow,rgb)};}
        catch(ReflectiveOperationException|RuntimeException|LinkageError changed){return null;}
    }
    private static final class Params {
        final Class<?> type;final Constructor<?> create;final Method copy,getColor,setColor;
        Params(ClassLoader loader,String name,String property)throws ReflectiveOperationException{
            type=loader.loadClass("com.oplus.posteffect.params."+name);create=type.getDeclaredConstructor();
            copy=type.getDeclaredMethod("copyFrom",type);getColor=type.getDeclaredMethod("get"+property);
            setColor=type.getDeclaredMethod("set"+property,Color.class);
            if(copy.getReturnType()!=Void.TYPE||getColor.getReturnType()!=Color.class||setColor.getReturnType()!=Void.TYPE)
                throw new NoSuchMethodException("Native optical colour contract changed");
            create.setAccessible(true);copy.setAccessible(true);getColor.setAccessible(true);setColor.setAccessible(true);
        }
        Object copy(Object source,int rgb)throws ReflectiveOperationException{
            if(source==null)return null;
            // Unknown subclasses can carry additional optical state; leave their full native recipe intact.
            if(source.getClass()!=type)throw new IllegalArgumentException("Unknown native optical parameters");
            Color original=(Color)getColor.invoke(source);
            if(original==null)throw new IllegalArgumentException("Missing native optical colour");
            // Media RGB is sRGB. Convert it before combining it with a wide-gamut native alpha/space.
            Color hue=Color.valueOf(0xff000000|rgb&0x00ffffff).convert(original.getColorSpace());
            Color colour=Color.valueOf(hue.red(),hue.green(),hue.blue(),original.alpha(),original.getColorSpace());
            Object owned=create.newInstance();copy.invoke(owned,source);setColor.invoke(owned,colour);return owned;
        }
    }
}
