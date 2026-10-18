package dev.puitheme;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;

/** Calls the module's actual startup read seam with a stale daemon and fresh DE provider. */
public final class StartupSnapshotPriorityCheck {
    private static int checks;
    private static void equal(Object expected, Object actual) {
        checks++; if (!java.util.Objects.equals(expected, actual)) throw new AssertionError(expected + " != " + actual);
    }
    private static void field(String name, Object value) throws Exception {
        Field field = StatusBarModule.class.getDeclaredField(name); field.setAccessible(true); field.set(null, value);
    }
    private static Object call(String name) throws Exception {
        Method method = StatusBarModule.class.getDeclaredMethod(name); method.setAccessible(true); return method.invoke(null);
    }
    private static Bundle saved(float offset) {
        return SettingsSnapshot.fromPreferences(Collections.singletonMap(StatusBarSettings.DATA_OFFSET_X, offset));
    }
    public static void main(String[] args) throws Exception {
        Bundle old = saved(1f), latest = saved(9f);
        ActivationGuardPreferencesCheck.MemoryPreferences daemon = new ActivationGuardPreferencesCheck.MemoryPreferences();
        equal(true, SettingsFrameworkMirror.write(daemon, old, 10L));
        final int[] reads = {0}; final boolean[] available = {true};
        ContentResolver resolver = new ContentResolver() {
            @Override public Bundle call(Uri uri, String method, String arg, Bundle extras) {
                reads[0]++; return available[0] ? latest : null;
            }
        };
        Context context = new Context() { @Override public ContentResolver getContentResolver() { return resolver; } };
        SettingsFrameworkReader reader = new SettingsFrameworkReader(() -> daemon, () -> {}, () -> false);
        field("context", context); field("frameworkSettings", reader); field("settingsObserverRegistered", true);
        field("moduleResources", new android.content.res.Resources());
        Bundle first = (Bundle) call("readStartupSettings");
        equal(1f, first.get(StatusBarSettings.DATA_OFFSET_X));
        equal(0, reads[0]); // Preserve immediate framework boot without waking the app first.
        Bundle second = (Bundle) call("readStartupSettings");
        equal(9f, second.get(StatusBarSettings.DATA_OFFSET_X));
        equal(1, reads[0]);
        equal(true, call("startupSettingsReady"));
        available[0] = false;
        Bundle fallback = (Bundle) call("readStartupSettings");
        equal(9f, fallback.get(StatusBarSettings.DATA_OFFSET_X));
        equal(false, call("startupSettingsReady")); // Valid fallback is useful but cannot finish freshness recovery.
        equal(true, SettingsFrameworkMirror.write(daemon, saved(20f), 11L));
        equal(20f, ((Bundle) call("readStartupSettings")).get(StatusBarSettings.DATA_OFFSET_X));
        equal(false, call("startupSettingsReady")); // A confirmed newer daemon edit remains usable while DE IPC is unavailable.
        available[0] = true;
        equal(9f, ((Bundle) call("readStartupSettings")).get(StatusBarSettings.DATA_OFFSET_X));
        equal(true, call("startupSettingsReady"));
        reader.stop(); equal(false, call("startupSettingsReady"));
        System.out.println("StartupSnapshotPriorityCheck: " + checks + " checks passed");
    }
}
