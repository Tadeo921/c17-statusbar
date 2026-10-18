package dev.puitheme;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;

/** A cached configuration must not bypass recovery of temporarily unavailable package resources. */
public final class StartupResourcesCheck {
    private static int checks;
    private static void equal(Object expected, Object actual) {
        checks++; if (!java.util.Objects.equals(expected, actual)) throw new AssertionError(expected + " != " + actual);
    }
    private static void set(String name, Object value) throws Exception {
        Field field = StatusBarModule.class.getDeclaredField(name); field.setAccessible(true); field.set(null, value);
    }
    private static Object get(String name) throws Exception {
        Field field = StatusBarModule.class.getDeclaredField(name); field.setAccessible(true); return field.get(null);
    }
    private static boolean apply(Bundle snapshot) throws Exception {
        Method method = StatusBarModule.class.getDeclaredMethod("applyStyleSnapshot", Bundle.class, boolean.class);
        method.setAccessible(true); return (Boolean) method.invoke(null, snapshot, false);
    }
    public static void main(String[] args) throws Exception {
        final int[] attempts = {0}; final boolean[] available = {false};
        Resources resources = new Resources(); ContentResolver resolver = new ContentResolver();
        Context resourceContext = new Context() { @Override public Resources getResources() { return resources; } };
        Context context = new Context() {
            @Override public ContentResolver getContentResolver() { return resolver; }
            @Override public Context createPackageContext(String name, int flags) {
                attempts[0]++;
                if (!available[0]) throw new IllegalStateException("Package resources are not ready");
                return resourceContext;
            }
        };
        set("context", context); set("moduleResources", null);
        Bundle snapshot = SettingsSnapshot.fromPreferences(Collections.emptyMap());
        SettingsSnapshot.rememberApplied(context, snapshot);
        equal(true, SettingsSnapshot.matchesApplied(context, snapshot));
        equal(true, apply(snapshot)); // Current visuals stay usable while the bounded loader retries readiness.
        equal(1, attempts[0]);
        equal(null, get("moduleResources"));
        available[0] = true;
        equal(true, apply(snapshot));
        equal(2, attempts[0]); equal(resources, get("moduleResources"));
        equal(true, apply(snapshot)); equal(2, attempts[0]);
        equal(true, SettingsSnapshot.matchesApplied(context, snapshot));
        System.out.println("StartupResourcesCheck: " + checks + " checks passed");
    }
}
