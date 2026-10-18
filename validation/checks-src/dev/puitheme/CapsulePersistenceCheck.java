package dev.puitheme;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import com.oplus.systemui.plugins.seedling.capsule.data.model.ContentState;
import com.oplus.systemui.plugins.seedling.capsule.strategy.b;
import com.oplus.systemui.plugins.seedling.capsule.strategy.d;
import java.lang.reflect.Method;
import java.util.Map;

/** Replays real timer register -> wrapper.run -> Function1.invoke, native swipe/remove, and configuration changes. */
public final class CapsulePersistenceCheck {
    private static int checks;
    private static void check(boolean okay, String why) { checks++; if (!okay) throw new AssertionError(why); }
    private static Bundle enabled(boolean value) { Bundle result = new Bundle(); result.putBoolean(CapsulePersistence.ENABLED, value); return result; }
    private static final class Rig {
        final CapsulePersistence persistence = new CapsulePersistence();
        final CapsulePersistence.Hooks hooks = persistence.resolve(CapsulePersistenceCheck.class.getClassLoader());
        final d owner = new d();
        Rig(boolean on) {
            check(hooks != null, "native timeout structure resolves");
            b.beforeForCheck = (callback, key) -> persistence.beforeTimeout(hooks, callback, key);
            persistence.configure(enabled(on));
        }
        void timeout() { owner.arm(); owner.a.fire(); }
    }

    public static void main(String[] args) throws Exception {
        CapsulePersistence missing = new CapsulePersistence();
        check(missing.resolve(null) == null, "missing loader keeps native lifecycle");
        check(missing.resolve(new ClassLoader(null) { }) == null, "unmatched ROM keeps native lifecycle");
        Rig nativeRig = new Rig(false); nativeRig.timeout();
        check(nativeRig.owner.automatic == 1 && nativeRig.owner.g == ContentState.MIN, "default off allows native timed collapse");

        Rig rig = new Rig(true);
        check(rig.hooks.timeout.getDeclaringClass() == b.class, "only exact capsule timeout callback is hooked");
        check(rig.hooks.inlineCallers.length == 1 && "run".equals(rig.hooks.inlineCallers[0].getName()), "audited interface-dispatch caller is deoptimized");
        check(rig.hooks.completed == kotlin.Unit.a, "skip returns native Kotlin Unit");
        for (Method method : rig.hooks.inlineCallers)
            check(method.getDeclaringClass().getName().equals(CapsulePersistence.RUNNABLE), "no View/Handler/framework timeout hook");
        rig.timeout();
        check(rig.owner.automatic == 0 && rig.owner.g == ContentState.NORMAL, "enabled keeps capsule after scheduled timeout");
        check(((Map<?, ?>) rig.owner.a.c).containsKey(CapsulePersistence.KEY), "native timeout callback leaves timer registration intact");
        int starts = rig.owner.a.starts;
        rig.persistence.configure(enabled(false));
        check(rig.owner.a.starts == starts + 1 && rig.owner.a.queued != null && rig.owner.a.delay == 5000,
                "turning off rearms native timer with original delay");
        check(rig.owner.automatic == 0, "turning off does not hide immediately");
        rig.owner.a.fire();
        check(rig.owner.automatic == 1 && rig.owner.g == ContentState.MIN, "rearmed timer restores native timed collapse");
        rig.persistence.configure(enabled(false));
        check(rig.owner.a.starts == starts + 1, "repeated disabled settings do not rearm repeatedly");

        rig = new Rig(true); rig.timeout(); starts = rig.owner.a.starts;
        Bundle safe = enabled(true); safe.putBoolean(StatusBarSettings.SAFE_MODE, true); rig.persistence.configure(safe);
        check(rig.owner.a.starts == starts + 1, "safe mode restores held native timer");
        rig.owner.a.fire();
        check(rig.owner.automatic == 1, "safe mode does not suppress native timer even with persistence enabled");

        rig = new Rig(true); rig.timeout(); rig.owner.manualCollapse();
        check(rig.owner.manual == 1 && rig.owner.g == ContentState.MIN, "human collapse remains effective");
        starts = rig.owner.a.starts; rig.persistence.configure(enabled(false));
        check(rig.owner.a.starts == starts, "turning off after manual collapse never replays hidden content");
        rig = new Rig(true); rig.timeout(); rig.owner.remove(); starts = rig.owner.a.starts;
        check(rig.owner.removed == 1 && rig.owner.g == ContentState.HIDE, "native event cancellation removes capsule");
        rig.persistence.configure(enabled(false));
        check(rig.owner.a.starts == starts && ((Map<?, ?>) rig.owner.a.c).isEmpty(), "removed event is never restored");

        rig = new Rig(true); rig.timeout(); rig.owner.c = "new-delivery"; starts = rig.owner.a.starts;
        rig.persistence.configure(enabled(false));
        check(rig.owner.a.starts == starts, "another service is never rearmed from old held event");
        rig = new Rig(true); rig.timeout(); rig.owner.capsuleState = false; starts = rig.owner.a.starts;
        rig.persistence.configure(enabled(false));
        check(rig.owner.a.starts == starts, "lock/window/state-machine transition is left native");
        rig = new Rig(true); rig.timeout(); rig.owner.a.i(CapsulePersistence.KEY); starts = rig.owner.a.starts;
        rig.persistence.configure(enabled(false));
        check(rig.owner.a.starts == starts, "deleted native timer is not recreated");
        rig = new Rig(true); rig.timeout(); rig.owner.f = new b(rig.owner, 1); starts = rig.owner.a.starts;
        rig.persistence.configure(enabled(false));
        check(rig.owner.a.starts == starts, "replaced timer callback rejects old registration");

        rig = new Rig(true);
        check(!rig.persistence.beforeTimeout(rig.hooks, rig.owner.f, "seizeCapsule"), "other timer keys are untouched");
        check(!rig.persistence.beforeTimeout(rig.hooks, new b(rig.owner, 0), new Object()), "state-machine callback branch remains native");
        new b(rig.owner, 0).invoke(new Object());
        check(rig.owner.exits == 1, "state-machine exit actually runs");
        check(!rig.persistence.beforeTimeout(rig.hooks, new b(rig.owner, 1), CapsulePersistence.KEY), "unregistered synthetic callback is untouched");
        check(!rig.persistence.beforeTimeout(rig.hooks, new Object(), CapsulePersistence.KEY), "unrelated class is untouched");
        rig.owner.g = ContentState.MIN;
        check(!rig.persistence.beforeTimeout(rig.hooks, rig.owner.f, CapsulePersistence.KEY), "already minimized content stays native");
        rig.owner.g = ContentState.NORMAL; rig.owner.h = false;
        check(!rig.persistence.beforeTimeout(rig.hooks, rig.owner.f, CapsulePersistence.KEY), "native bubble switch off stays native");
        rig.owner.h = true; rig.owner.c = "";
        check(!rig.persistence.beforeTimeout(rig.hooks, rig.owner.f, CapsulePersistence.KEY), "empty service stays native");

        rig = new Rig(true); rig.timeout(); starts = rig.owner.a.starts;
        Looper.setCurrentForCheck(null); rig.persistence.configure(enabled(false));
        check(rig.owner.a.starts == starts, "background config does not operate native timer before main dispatch");
        Looper.resetCurrentForCheck(); Handler.drainForCheck();
        check(rig.owner.a.starts == starts + 1, "background disable restores native timer on main thread");
        rig = new Rig(true); rig.timeout(); starts = rig.owner.a.starts;
        Looper.setCurrentForCheck(null); rig.persistence.configure(enabled(false)); rig.persistence.configure(enabled(true));
        Looper.resetCurrentForCheck(); Handler.drainForCheck();
        check(rig.owner.a.starts == starts, "quick re-enable cancels pending restore");
        rig.persistence.configure(enabled(false));
        check(rig.owner.a.starts == starts + 1, "held owner survives cancelled restore until actual disable");
        rig = new Rig(true); rig.timeout(); starts = rig.owner.a.starts;
        Looper.setCurrentForCheck(null); rig.persistence.configure(enabled(false)); rig.persistence.release();
        check(rig.owner.a.starts == starts, "release after background disable waits for main thread");
        Looper.resetCurrentForCheck(); Handler.drainForCheck();
        check(rig.owner.a.starts == starts + 1, "release never drops pending timer restoration or schedules it twice");
        rig = new Rig(true); rig.timeout(); starts = rig.owner.a.starts; rig.persistence.release();
        check(rig.owner.a.starts == starts + 1, "release restores live native timer");
        check(rig.persistence.resolve(CapsulePersistenceCheck.class.getClassLoader()) == null, "released helper cannot bind again");
        rig.persistence.configure(enabled(true));
        check(!rig.persistence.beforeTimeout(rig.hooks, rig.owner.f, CapsulePersistence.KEY), "released helper never suppresses");
        b.beforeForCheck = null;
        System.out.println("CapsulePersistenceCheck: " + checks + " checks passed");
    }
}
