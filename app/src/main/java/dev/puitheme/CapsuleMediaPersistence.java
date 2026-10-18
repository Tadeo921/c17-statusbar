// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.os.Bundle;
import android.os.Build;
import android.os.Handler;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

/** Music has a second five-second capsule timeout in SystemUI, before data reaches the plugin.
 * Its Runnable is R8-merged with release: retain only the controller's actual timeout instance. */
public final class CapsuleMediaPersistence {
    static final String CONTROLLER = "com.oplus.systemui.media.seedling.livealert.MediaLiveAlertController";
    private volatile boolean enabled, released;
    private long epoch;
    private final Map<Object, Blocked> blocked = new WeakHashMap<>();

    public static final class Hooks {
        public final Method timeout;
        public final Method[] inlineCallers;
        private final Class<?> controller, callback;
        private final Field owner, classId, nativeTimer, handler, shown, destroyed, state, player, event;

        Hooks(ClassLoader loader) throws ReflectiveOperationException {
            controller = Class.forName(CONTROLLER, false, loader);
            callback = Class.forName(CONTROLLER + "$release$1", false, loader);
            if (callback.getSuperclass() != Object.class || !Modifier.isFinal(callback.getModifiers())
                    || !Runnable.class.isAssignableFrom(callback)) throw new NoSuchMethodException("Media timer structure changed");
            owner = field(callback, "this$0", controller, false);
            classId = field(callback, "$r8$classId", int.class, true);
            nativeTimer = field(controller, "statusBarTimeoutRunnable", Runnable.class, true);
            handler = field(controller, "myHandler", Handler.class, true);
            shown = field(controller, "showOnStatusBar", boolean.class, false);
            destroyed = field(controller, "isMediaCardDestroy", boolean.class, false);
            state = field(controller, "mediaCurrentState", int.class, false);
            player = field(controller, "lastStatusBarPlayerId", String.class, false);
            event = field(controller, "timeStamp", String.class, false);
            int owners = 0;
            for (Field candidate : callback.getDeclaredFields())
                if (!Modifier.isStatic(candidate.getModifiers()) && candidate.getType() == controller) owners++;
            if (owners != 1) throw new NoSuchMethodException("Media timer owner is ambiguous");
            timeout = method(callback, "run", void.class);
            inlineCallers = new Method[]{method(controller, "sendLiveAlertData", boolean.class, String.class)};
        }

        private static Field field(Class<?> type, String name, Class<?> value, boolean immutable)
                throws ReflectiveOperationException {
            Field field = type.getDeclaredField(name);
            if (field.getType() != value || Modifier.isStatic(field.getModifiers())
                    || Modifier.isFinal(field.getModifiers()) != immutable) throw new NoSuchFieldException(name);
            field.setAccessible(true);
            return field;
        }

        private static Method method(Class<?> type, String name, Class<?> result, Class<?>... args)
                throws ReflectiveOperationException {
            Method method = type.getDeclaredMethod(name, args);
            if (method.getReturnType() != result || Modifier.isStatic(method.getModifiers())) throw new NoSuchMethodException(name);
            method.setAccessible(true);
            return method;
        }

        private Object owner(Object timer) throws IllegalAccessException {
            if (timer == null || timer.getClass() != callback || classId.getInt(timer) != 1) return null;
            Object source = owner.get(timer);
            return source != null && source.getClass() == controller && nativeTimer.get(source) == timer ? source : null;
        }

        private boolean alive(Object source) throws IllegalAccessException {
            return shown.getBoolean(source) && !destroyed.getBoolean(source);
        }
    }

    public Hooks resolve(ClassLoader loader) throws ReflectiveOperationException {
        return released || loader == null || Build.VERSION.SDK_INT < 29 ? null : new Hooks(loader);
    }

    public void configure(Bundle values) {
        boolean next = !released && Build.VERSION.SDK_INT >= 29 && values != null && !SafetyMode.enabled(values)
                && values.getBoolean(CapsulePersistence.ENABLED, false);
        synchronized (blocked) {
            if (enabled == next) return;
            enabled = next;
            epoch++;
            if (!next) resumeBlocked(epoch);
        }
    }

    public boolean beforeTimeout(Hooks hooks, Object callback) {
        if (!enabled || released || hooks == null || Build.VERSION.SDK_INT < 29) return false;
        try {
            Object source = hooks.owner(callback);
            if (source == null || !hooks.alive(source)) return false;
            Handler handler = (Handler) hooks.handler.get(source);
            if (handler == null) return false;
            Blocked pending = new Blocked(hooks, source, callback, handler);
            synchronized (blocked) {
                if (!enabled || released) return false;
                blocked.put(callback, pending);
            }
            return true;
        } catch (ReflectiveOperationException | RuntimeException unavailable) {
            return false;
        }
    }

    private static final class Blocked {
        final Hooks hooks;
        final WeakReference<Object> source, callback;
        final WeakReference<Handler> handler;
        final int state;
        final String player, event;
        Blocked(Hooks hooks, Object source, Object callback, Handler handler) throws IllegalAccessException {
            this.hooks = hooks;
            this.source = new WeakReference<>(source);
            this.callback = new WeakReference<>(callback);
            this.handler = new WeakReference<>(handler);
            state = hooks.state.getInt(source);
            player = (String) hooks.player.get(source);
            event = (String) hooks.event.get(source);
        }
        boolean current(Object source, Object callback, Handler handler) throws IllegalAccessException {
            return hooks.owner(callback) == source && hooks.handler.get(source) == handler && hooks.alive(source)
                    && hooks.state.getInt(source) == state && Objects.equals(hooks.player.get(source), player)
                    && Objects.equals(hooks.event.get(source), event);
        }
    }

    /** Restore only expired callbacks. Do not restart media, force visibility, or resend canceled content. */
    private void resumeBlocked(long scheduledEpoch) {
        for (Blocked pending : new ArrayList<>(blocked.values())) {
            Handler handler = pending.handler.get();
            Object callback = pending.callback.get();
            if (handler == null || callback == null) continue;
            if (!handler.post(() -> resume(pending, scheduledEpoch))) blocked.remove(callback);
        }
    }

    private void resume(Blocked pending, long scheduledEpoch) {
        Object source = pending.source.get(), callback = pending.callback.get();
        Handler handler = pending.handler.get();
        synchronized (blocked) {
            // A queued disable followed by enable must not dispatch its old expiry later.
            if (enabled || epoch != scheduledEpoch || callback == null || blocked.get(callback) != pending) return;
            blocked.remove(callback);
        }
        if (source == null || handler == null) return;
        try {
            if (Build.VERSION.SDK_INT >= 29 && pending.current(source, callback, handler)
                    && !handler.hasCallbacks((Runnable) callback))
                ((Runnable) callback).run();
        } catch (ReflectiveOperationException | RuntimeException unavailable) {
            // A native owner disappeared or changed contract: retain its lifecycle decision.
        }
    }

    public void release() {
        synchronized (blocked) {
            if (released) return;
            released = true;
            enabled = false;
            epoch++;
            resumeBlocked(epoch);
        }
    }
}
