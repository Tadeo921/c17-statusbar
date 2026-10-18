package dev.puitheme;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

/** Executes the compiled module's lazy-hook registry without installing any framework hooks. */
public final class LazyHookRecoveryCheck {
    private interface Action { void run() throws Throwable; }
    private static Class<?> moduleClass, installerClass;
    private static Method register, install;
    private static Field instance;
    private static Object module;
    private static Map<String, Object> hooks;
    private static int checks, cases;
    private static final List<String> failures = new ArrayList<>();

    private static void check(boolean value, String why) {
        checks++;
        if (!value) throw new AssertionError(why);
    }

    private static Object invoke(Method method, Object receiver, Object... arguments) throws Throwable {
        try { return method.invoke(receiver, arguments); }
        catch (InvocationTargetException failure) { throw failure.getCause(); }
    }

    private static Object actionProxy(Action action) {
        return Proxy.newProxyInstance(moduleClass.getClassLoader(), new Class<?>[]{installerClass},
                (owner, method, arguments) -> {
                    if (method.getName().equals("run")) { action.run(); return null; }
                    if (method.getName().equals("toString")) return "LazyHookRecoveryCheck installer";
                    if (method.getName().equals("hashCode")) return System.identityHashCode(owner);
                    if (method.getName().equals("equals")) return owner == arguments[0];
                    throw new AssertionError("Unexpected installer method: " + method.getName());
                });
    }
    private static void register(String name, BooleanSupplier wanted, Action action) throws Throwable {
        invoke(register, module, name, wanted, actionProxy(action));
    }
    private static void registerReversible(String name, BooleanSupplier wanted, Action add, Action remove) throws Throwable {
        Method reversible = moduleClass.getDeclaredMethod("lazyHook", String.class, BooleanSupplier.class, installerClass, installerClass);
        reversible.setAccessible(true);
        invoke(reversible, module, name, wanted, actionProxy(add), actionProxy(remove));
    }

    private static void install() throws Throwable { invoke(install, null); }

    private static void run(String name, Action action) {
        cases++;
        hooks.clear();
        try {
            instance.set(null, module);
            action.run();
            System.out.println("PASS " + name);
        } catch (Throwable failure) {
            String result = name + ": " + failure.getClass().getSimpleName() + ": " + failure.getMessage();
            failures.add(result);
            System.out.println("FAIL " + result);
        } finally {
            hooks.clear();
        }
    }

    private static void retryAfterFailure() throws Throwable {
        AtomicInteger attempts = new AtomicInteger();
        register("transient", () -> true, () -> {
            if (attempts.incrementAndGet() == 1) throw new IllegalStateException("fixture site unavailable");
        });
        check(attempts.get() == 1, "registration makes exactly one initial attempt");
        install();
        check(attempts.get() == 2, "next explicit install must retry the failed installer");
        install();
        check(attempts.get() == 2, "a successful retry must not install again");
    }

    private static void replaceFailedInstaller() throws Throwable {
        AtomicInteger oldAttempts = new AtomicInteger(), replacements = new AtomicInteger();
        register("replacement", () -> true, () -> {
            oldAttempts.incrementAndGet();
            throw new IllegalStateException("fixture old loader unavailable");
        });
        check(oldAttempts.get() == 1, "the original loader fails once before replacement");
        register("replacement", () -> true, replacements::incrementAndGet);
        install();
        check(replacements.get() == 1, "same-key failed entry must use the replacement installer");
        check(oldAttempts.get() == 1, "replacement must not retry the superseded installer");
        install();
        check(replacements.get() == 1, "replacement success remains installed");
    }

    private static void replacePendingInstallerAndPredicate() throws Throwable {
        AtomicInteger oldAttempts = new AtomicInteger(), replacements = new AtomicInteger();
        register("pending", () -> false, oldAttempts::incrementAndGet);
        check(oldAttempts.get() == 0, "a disabled registration is left pending");
        AtomicBoolean wanted = new AtomicBoolean(false);
        register("pending", wanted::get, replacements::incrementAndGet);
        install();
        check(oldAttempts.get() == 0 && replacements.get() == 0, "replacement preserves its disabled gate");
        wanted.set(true);
        install();
        check(replacements.get() == 1, "same-key pending entry must adopt the new installer and predicate");
        check(oldAttempts.get() == 0, "disabled old installer must never run");
    }

    private static void disabledUntilWanted() throws Throwable {
        AtomicInteger attempts = new AtomicInteger();
        AtomicBoolean wanted = new AtomicBoolean(false);
        register("gated", wanted::get, attempts::incrementAndGet);
        install(); install();
        check(attempts.get() == 0, "disabled hooks never invoke the installer");
        wanted.set(true);
        install();
        check(attempts.get() == 1, "enabling a pending hook installs it once");
        wanted.set(false); install();
        wanted.set(true); install();
        check(attempts.get() == 1, "later off/on cycles do not duplicate an installed hook");
    }
    private static void reversibleIndexLifecycle() throws Throwable {
        AtomicBoolean wanted = new AtomicBoolean(false); AtomicInteger added = new AtomicInteger(), removed = new AtomicInteger();
        registerReversible("index", wanted::get, added::incrementAndGet, () -> { removed.incrementAndGet(); install(); });
        install(); check(added.get() == 0 && removed.get() == 0, "an unused index hook never enters the framework");
        wanted.set(true); install(); install(); check(added.get() == 1, "native index remapping installs once");
        wanted.set(false); install(); install();
        check(removed.get() == 1, "leaving native remapping removes its global hook exactly once, including reentrant sync");
        wanted.set(true); install(); check(added.get() == 2, "returning to native remapping installs a new live hook");
    }
    private static void reversibleRemovalRetry() throws Throwable {
        AtomicBoolean wanted = new AtomicBoolean(true); AtomicInteger added = new AtomicInteger(), removed = new AtomicInteger();
        registerReversible("index-retry", wanted::get, added::incrementAndGet, () -> {
            if (removed.incrementAndGet() == 1) throw new IllegalStateException("native hook removal failed");
        });
        wanted.set(false); install(); install(); install();
        check(removed.get() == 2 && added.get() == 1, "failed removal retries on the next settings signal without duplicating the live hook");
        wanted.set(true); install(); check(added.get() == 2, "successful removal leaves the hook available for reactivation");
    }

    private static void successfulEntryIsStable() throws Throwable {
        AtomicInteger attempts = new AtomicInteger(), replacements = new AtomicInteger();
        register("complete", () -> true, attempts::incrementAndGet);
        install(); install();
        register("complete", () -> true, replacements::incrementAndGet);
        install();
        check(attempts.get() == 1, "successful registration installs only once");
        check(replacements.get() == 0, "same-key re-registration cannot replace an already installed hook");
    }

    private static void reentrantInstallIsSingle() throws Throwable {
        AtomicInteger attempts = new AtomicInteger();
        register("reentrant", () -> true, () -> {
            if (attempts.incrementAndGet() > 1) throw new AssertionError("installer reentered itself");
            install();
        });
        check(attempts.get() == 1, "reentrant install must not invoke the current installer twice");
        install();
        check(attempts.get() == 1, "the reentrant installer remains complete after returning");
    }

    private static void registerInsideInstaller() throws Throwable {
        AtomicBoolean outerWanted = new AtomicBoolean(false);
        AtomicInteger outer = new AtomicInteger(), child = new AtomicInteger(), disabled = new AtomicInteger();
        register("outer", outerWanted::get, () -> {
            outer.incrementAndGet();
            register("child", () -> true, child::incrementAndGet);
        });
        // Leave a later entry so a live-map iterator must advance after the structural mutation.
        register("disabled-tail", () -> false, disabled::incrementAndGet);
        outerWanted.set(true);
        install();
        check(outer.get() == 1, "outer installer completes while registering another hook");
        install();
        check(child.get() == 1, "the newly registered child is installed at most once and remains reachable");
        check(outer.get() == 1 && disabled.get() == 0, "nested registration preserves completion and disabled entries");
    }

    /** Reject all native/plugin contracts: the completed-group path must stay entirely in Java. */
    private static final class NoNativeHooksLoader extends ClassLoader {
        NoNativeHooksLoader() { super(moduleClass.getClassLoader()); }
        @Override protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (name.startsWith("com.android.systemui.") || name.startsWith("com.oplus.")
                    || name.startsWith("com.oplusos."))
                throw new ClassNotFoundException("Native contract intentionally absent: " + name);
            return super.loadClass(name, resolve);
        }
    }

    @SuppressWarnings("unchecked")
    private static void packageReadyRetriesCompletedGroups() throws Throwable {
        Field completedField = moduleClass.getDeclaredField("COMPLETED_HOOK_GROUPS"); completedField.setAccessible(true);
        Set<Object> completed = (Set<Object>) completedField.get(null);
        Set<Object> originalCompleted = new HashSet<>(completed);
        Field installedField = moduleClass.getDeclaredField("INSTALLED_HOOKS"); installedField.setAccessible(true);
        Map<Object, Set<Object>> installed = (Map<Object, Set<Object>>) installedField.get(null);
        Map<Object, Set<Object>> originalInstalled = new LinkedHashMap<>();
        for (Map.Entry<Object, Set<Object>> entry : installed.entrySet())
            originalInstalled.put(entry.getKey(), new HashSet<>(entry.getValue()));
        Class<?> group = Class.forName("dev.puitheme.StatusBarModule$HookGroup");
        Object[] everyGroup = group.getEnumConstants();
        Method systemUi = moduleClass.getDeclaredMethod("installSystemUiHooks", ClassLoader.class, String.class);
        systemUi.setAccessible(true);
        ClassLoader loader = new NoNativeHooksLoader();
        AtomicBoolean wanted = new AtomicBoolean(false);
        AtomicInteger attempts = new AtomicInteger();
        try {
            completed.clear();
            for (Object value : everyGroup) completed.add(value);
            register("fixture-package-ready", wanted::get, () -> {
                if (attempts.incrementAndGet() == 1) throw new IllegalStateException("fixture package not ready");
            });
            invoke(systemUi, module, loader, "package-ready");
            check(attempts.get() == 0, "completed-group package-ready must still honor disabled pending entries");
            wanted.set(true);
            invoke(systemUi, module, loader, "package-ready");
            check(attempts.get() == 1, "completed-group package-ready must attempt the now-enabled pending installer");
            invoke(systemUi, module, loader, "package-ready");
            check(attempts.get() == 2, "another package-ready must retry the earlier failure before the completed-group return");
            invoke(systemUi, module, loader, "package-ready");
            check(attempts.get() == 2, "completed-group package-ready must not reinstall a successful pending hook");
            check(completed.size() == everyGroup.length, "the test remains on the all-groups-completed fast path");
            check(installed.equals(originalInstalled), "the readiness check installs no real framework hooks");
        } finally {
            completed.clear(); completed.addAll(originalCompleted);
            installed.clear(); installed.putAll(originalInstalled);
        }
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] arguments) throws Throwable {
        moduleClass = Class.forName("dev.puitheme.StatusBarModule");
        installerClass = Class.forName("dev.puitheme.StatusBarModule$LazyInstall");
        Constructor<?> constructor = moduleClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        module = constructor.newInstance();
        instance = moduleClass.getDeclaredField("instance"); instance.setAccessible(true);
        Field registry = moduleClass.getDeclaredField("LAZY_HOOKS"); registry.setAccessible(true);
        hooks = (Map<String, Object>) registry.get(null);
        register = moduleClass.getDeclaredMethod("lazyHook", String.class, BooleanSupplier.class, installerClass);
        register.setAccessible(true);
        install = moduleClass.getDeclaredMethod("installLazyHooks"); install.setAccessible(true);
        Object originalInstance = instance.get(null);
        Map<String, Object> originalHooks = new LinkedHashMap<>(hooks);
        try {
            run("retry after failed install", LazyHookRecoveryCheck::retryAfterFailure);
            run("replace failed installer", LazyHookRecoveryCheck::replaceFailedInstaller);
            run("replace pending installer and predicate", LazyHookRecoveryCheck::replacePendingInstallerAndPredicate);
            run("disabled until wanted", LazyHookRecoveryCheck::disabledUntilWanted);
            run("reversible index lifecycle", LazyHookRecoveryCheck::reversibleIndexLifecycle);
            run("reversible removal retry", LazyHookRecoveryCheck::reversibleRemovalRetry);
            run("successful entry is stable", LazyHookRecoveryCheck::successfulEntryIsStable);
            run("reentrant install is single", LazyHookRecoveryCheck::reentrantInstallIsSingle);
            run("register inside installer", LazyHookRecoveryCheck::registerInsideInstaller);
            run("package-ready retries completed groups", LazyHookRecoveryCheck::packageReadyRetriesCompletedGroups);
        } finally {
            hooks.clear(); hooks.putAll(originalHooks); instance.set(null, originalInstance);
        }
        System.out.println("LazyHookRecoveryCheck: " + (cases - failures.size()) + "/" + cases
                + " cases passed, " + checks + " assertions reached");
        if (!failures.isEmpty()) throw new AssertionError("Lazy-hook recovery failures: " + failures);
    }
}
