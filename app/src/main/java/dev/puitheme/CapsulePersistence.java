// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Map;
import java.util.WeakHashMap;

/** Keeps the camera capsule open by skipping its own capsule-to-bubble timeout. Native swipe, click,
 * removal and window lifecycle paths do not pass this callback and are never intercepted here. */
public final class CapsulePersistence {
    public static final String ENABLED = "fluid_cloud_capsule_persistent_enabled";
    static final String KEY = "capsuleToBubble";
    static final String STRATEGY = "com.oplus.systemui.plugins.seedling.capsule.strategy.d";
    static final String TIMEOUT = "com.oplus.systemui.plugins.seedling.capsule.strategy.b";
    static final String TIMER = "com.heytap.log.collect.auto.d";
    static final String TASK = "com.oplus.systemui.plugins.seedling.util.j";
    static final String RUNNABLE = "com.oplus.systemui.livealert.classic.source.d";
    static final String STATE = "com.oplus.systemui.plugins.seedling.capsule.data.model.ContentState";

    /** Exact audited callback and the interface-dispatch caller that ART may inline it into. */
    public static final class Hooks {
        public final Method timeout;
        public final Method[] inlineCallers;
        public final Object completed;
        private final Class<?> callbackType, strategyType, taskType, runnableType;
        private final Field branch, strategy, currentCallback, service, state, bubbleEnabled, timer, duration;
        private final Field tasks, taskRunnable, runnableBranch, runnableCallback, runnableKey;
        private final Method capsuleActive, schedule;

        Hooks(ClassLoader loader) throws ReflectiveOperationException {
            callbackType = Class.forName(TIMEOUT, false, loader);
            strategyType = Class.forName(STRATEGY, false, loader);
            taskType = Class.forName(TASK, false, loader);
            runnableType = Class.forName(RUNNABLE, false, loader);
            Class<?> timerType = Class.forName(TIMER, false, loader);
            Class<?> contentState = Class.forName(STATE, false, loader);
            Class<?> function = Class.forName("kotlin.jvm.functions.Function1", false, loader);
            Class<?> unit = Class.forName("kotlin.Unit", false, loader);
            if (!function.isInterface() || !function.isAssignableFrom(callbackType) || !contentState.isEnum()
                    || callbackType.getSuperclass() != Object.class || !Modifier.isFinal(callbackType.getModifiers())
                    || !Runnable.class.isAssignableFrom(runnableType)) throw new NoSuchMethodException("Capsule timeout structure");
            timeout = method(callbackType, "invoke", Object.class, Object.class);
            inlineCallers = new Method[]{method(runnableType, "run", void.class)};
            branch = field(callbackType, "b", int.class);
            strategy = field(callbackType, "c", strategyType);
            currentCallback = field(strategyType, "f", callbackType);
            service = field(strategyType, "c", String.class);
            state = field(strategyType, "g", contentState);
            bubbleEnabled = field(strategyType, "h", boolean.class);
            timer = field(strategyType, "a", timerType);
            duration = field(strategyType, "e", long.class);
            capsuleActive = method(strategyType, "h", boolean.class);
            schedule = method(timerType, "s", void.class, long.class, String.class);
            tasks = field(timerType, "c", Object.class);
            taskRunnable = field(taskType, "d", runnableType);
            runnableBranch = field(runnableType, "b", int.class);
            runnableCallback = field(runnableType, "c", Object.class);
            runnableKey = field(runnableType, "d", Object.class);
            Field singleton = unit.getDeclaredField("a");
            if (singleton.getType() != unit || !Modifier.isStatic(singleton.getModifiers())
                    || !Modifier.isFinal(singleton.getModifiers())) throw new NoSuchFieldException("Unit singleton");
            singleton.setAccessible(true);
            completed = singleton.get(null);
            if (completed == null) throw new NoSuchFieldException("Unit singleton is missing");
        }

        private static Field field(Class<?> type, String name, Class<?> value) throws ReflectiveOperationException {
            Field field = type.getDeclaredField(name);
            if (field.getType() != value || Modifier.isStatic(field.getModifiers())) throw new NoSuchFieldException(name);
            field.setAccessible(true);
            return field;
        }

        private static Method method(Class<?> type, String name, Class<?> value, Class<?>... args)
                throws ReflectiveOperationException {
            Method method = type.getDeclaredMethod(name, args);
            if (method.getReturnType() != value || Modifier.isStatic(method.getModifiers())) throw new NoSuchMethodException(name);
            method.setAccessible(true);
            return method;
        }

        /** Timer registration must still belong to this exact callback. A deleted or replaced task stays native. */
        private boolean current(Object owner, Object callback) throws ReflectiveOperationException {
            if (owner == null || owner.getClass() != strategyType || callback == null || callback.getClass() != callbackType
                    || branch.getInt(callback) != 1 || strategy.get(callback) != owner || currentCallback.get(owner) != callback
                    || !bubbleEnabled.getBoolean(owner)) return false;
            Object contentState = state.get(owner);
            if (!(contentState instanceof Enum) || !"NORMAL".equals(((Enum<?>) contentState).name())) return false;
            String id = (String) service.get(owner);
            if (id == null || id.isEmpty() || !Boolean.TRUE.equals(capsuleActive.invoke(owner))) return false;
            Object timerSet = timer.get(owner);
            if (timerSet == null || timerSet.getClass() != schedule.getDeclaringClass()) return false;
            Object registered = tasks.get(timerSet);
            if (!(registered instanceof Map)) return false;
            Object task = ((Map<?, ?>) registered).get(KEY);
            if (task == null || task.getClass() != taskType) return false;
            Object runnable = taskRunnable.get(task);
            return runnable != null && runnable.getClass() == runnableType && runnableBranch.getInt(runnable) == 23
                    && runnableCallback.get(runnable) == callback && KEY.equals(runnableKey.get(runnable));
        }
    }

    private static final class Held {
        final Hooks hooks;
        final String service;
        Held(Hooks hooks, String service) { this.hooks = hooks; this.service = service; }
    }

    private final Map<Object, Held> held = new WeakHashMap<>();
    private volatile boolean enabled, released;

    public Hooks resolve(ClassLoader loader) {
        if (loader == null || released) return null;
        try { return new Hooks(loader); }
        catch (ReflectiveOperationException | LinkageError | RuntimeException unavailable) {
            ModuleDiagnostics.info("fluid", "Camera capsule persistence contract unavailable; native lifecycle retained");
            return null;
        }
    }

    /** Return true only for the registered native camera timeout; the hook returns hooks.completed. */
    public boolean beforeTimeout(Hooks hooks, Object callback, Object key) {
        if (!enabled || released || hooks == null || !KEY.equals(key) || callback == null
                || callback.getClass() != hooks.callbackType) return false;
        try {
            Object owner = hooks.strategy.get(callback);
            if (!hooks.current(owner, callback)) return false;
            synchronized (held) {
                if (!enabled || released) return false;
                held.put(owner, new Held(hooks, (String) hooks.service.get(owner)));
            }
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) { return false; }
    }

    public void configure(Bundle settings) {
        boolean next = !released && settings != null && !SafetyMode.enabled(settings) && settings.getBoolean(ENABLED, false);
        boolean wasEnabled;
        synchronized (held) { wasEnabled = enabled; enabled = next; }
        if (wasEnabled && !next) resumeNativeTimers();
    }

    /** Reuse the still-registered timer with its native duration, never replay an old hide or resurrect a removed task. */
    private void resumeNativeTimers() {
        Runnable resume = () -> {
            if (enabled) return;
            ArrayList<Map.Entry<Object, Held>> pending;
            synchronized (held) {
                pending = new ArrayList<>();
                for (Map.Entry<Object, Held> entry : held.entrySet())
                    pending.add(new java.util.AbstractMap.SimpleImmutableEntry<>(entry.getKey(), entry.getValue()));
                held.clear();
            }
            for (Map.Entry<Object, Held> entry : pending) {
                Object owner = entry.getKey(); Held saved = entry.getValue(); Hooks hooks = saved.hooks;
                try {
                    if (!saved.service.equals(hooks.service.get(owner))) continue;
                    Object callback = hooks.currentCallback.get(owner);
                    if (!hooks.current(owner, callback)) continue;
                    hooks.schedule.invoke(hooks.timer.get(owner), hooks.duration.getLong(owner), KEY);
                } catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) {
                    // A native task/window can disappear between validation and scheduling. Preserve its cleanup.
                }
            }
        };
        if (Looper.myLooper() == Looper.getMainLooper()) resume.run();
        else new Handler(Looper.getMainLooper()).post(resume);
    }

    public void release() {
        synchronized (held) { enabled = false; released = true; }
        // A background disable may already have queued restoration. Keep its weak owners until main-thread recovery.
        resumeNativeTimers();
    }
}
