// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;
import android.os.Bundle;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

/** Actual runtime settings, including stored incompatible combinations. */
public final class ClockBackdropModeCheck {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static boolean flag(Object owner,String name)throws Exception {
        Field field=owner.getClass().getDeclaredField(name);field.setAccessible(true);return field.getBoolean(owner);
    }
    public static void main(String[] args)throws Exception {
        Constructor<?> constructor=Class.forName("dev.puitheme.NotificationBigClock$Settings").getDeclaredConstructor(Bundle.class,String.class);
        constructor.setAccessible(true);
        for(String prefix:new String[]{NotificationBigClockSettings.PORTRAIT_PREFIX,NotificationBigClockSettings.LANDSCAPE_PREFIX}) {
            Bundle saved=new Bundle();saved.putBoolean(prefix+"glass",true);saved.putBoolean(prefix+"lock_style_enabled",true);
            saved.putBoolean(prefix+"lock_style_native",true);saved.putBoolean(prefix+"digit_reveal_enabled",true);
            saved.putBoolean(prefix+"follow_style_enabled",true);
            Object enabled=constructor.newInstance(saved,prefix);
            check(!flag(enabled,"glass"),"backdrop glyph mode must bypass the ordinary translucent gradient");
            check(!flag(enabled,"lockStyle")&&!flag(enabled,"lockNative"),"backdrop glyph mode must bypass native and simulated shadow materials");
            check(flag(enabled,"followStyle"),"date following remains opt-in within the same mode");
            saved.putBoolean(prefix+"digit_reveal_enabled",false);
            Object restored=constructor.newInstance(saved,prefix);
            check(flag(restored,"glass")&&flag(restored,"lockStyle")&&flag(restored,"lockNative"),"disabling the mode restores all saved material choices");
            check(!flag(restored,"followStyle"),"saved following is paused while the mode is disabled");
            check(saved.getBoolean(prefix+"glass",false)&&saved.getBoolean(prefix+"lock_style_enabled",false),"effective mode never rewrites saved styles");
        }
        System.out.println("ClockBackdropModeCheck "+checks+" checks");
    }
}
