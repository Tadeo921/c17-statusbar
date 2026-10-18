// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.content.SharedPreferences;
import android.os.Bundle;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/** Only the startup worker obtains framework preferences. Observing the revision
 * covers subsequent confirmed writes without waking the configuration app. */
final class SettingsFrameworkReader {
    private final Supplier<SharedPreferences> source;
    private final Runnable changed;
    private final BooleanSupplier removed;
    private volatile SharedPreferences preferences;
    private volatile boolean observed,stopped,providerConfirmed;
    private boolean bootSnapshotOffered;
    private Bundle lastPrimary;
    private long frameworkRevision, primaryFrameworkRevision;
    private final SharedPreferences.OnSharedPreferenceChangeListener listener;
    SettingsFrameworkReader(Supplier<SharedPreferences> source,Runnable changed,BooleanSupplier removed){
        this.source=source;this.changed=changed;this.removed=removed;
        listener=(owner,key)->{
            if(!stopped&&!removed.getAsBoolean()&&(key==null||SettingsFrameworkMirror.REVISION.equals(key)))changed.run();
        };
    }
    Bundle read() {
        if(stopped||removed.getAsBoolean())return null;
        try {
            SharedPreferences current=preferences;
            if(current==null) {
                current=source.get();
                if(current==null||stopped||removed.getAsBoolean())return null;
                preferences=current;
            }
            if(!observed) {
                current.registerOnSharedPreferenceChangeListener(listener);
                if(stopped||removed.getAsBoolean()) {
                    current.unregisterOnSharedPreferenceChangeListener(listener);preferences=null;return null;
                }
                observed=true;
            }
            java.util.Map<String,?> values=current.getAll();
            Bundle snapshot=SettingsFrameworkMirror.decode(values);
            if (snapshot!=null) frameworkRevision=(Long)values.get(SettingsFrameworkMirror.REVISION);
            return stopped||removed.getAsBoolean()?null:snapshot;
        } catch(RuntimeException | io.github.libxposed.api.error.XposedFrameworkError unavailable) {
            ModuleDiagnostics.error("settings","Framework boot configuration not ready; provider fallback retained",unavailable);
            return null;
        }
    }
    Bundle read(boolean preferProvider,SettingsStartupLoader.Source provider) {
        providerConfirmed=false;
        Bundle mirrored=read();
        if(mirrored!=null&&!preferProvider)return mirrored;
        Bundle fresh=null;
        try { fresh=provider.read(); }catch(Exception unavailable){/* OEM provider auto-start may be rejected. */}
        if (SettingsSnapshot.complete(fresh)) { providerConfirmed=true; return SettingsSnapshot.runtimeCopy(fresh); }
        return mirrored;
    }
    /** Deliver one immediate daemon snapshot, then confirm the primary source
     * during the loader's existing finite retry window. No permanent polling. */
    Bundle readStartup(boolean preferProvider,SettingsStartupLoader.Source provider) {
        if (!bootSnapshotOffered) {
            bootSnapshotOffered=true;
            if (!preferProvider) {
                Bundle mirrored=read();
                if (mirrored!=null) { providerConfirmed=false; return mirrored; }
            }
        }
        Bundle snapshot=read(true,provider);
        if (providerConfirmed) {
            lastPrimary=SettingsSnapshot.runtimeCopy(snapshot); primaryFrameworkRevision=frameworkRevision;
        } else if (lastPrimary!=null && frameworkRevision<=primaryFrameworkRevision) {
            // A transient provider failure is not evidence that the old daemon
            // mirror should roll back a newer successfully confirmed edit.
            return SettingsSnapshot.runtimeCopy(lastPrimary);
        }
        return snapshot;
    }
    boolean providerConfirmed(){return providerConfirmed&&!stopped;}
    boolean observed(){return observed&&!stopped;}
    void stop() {
        stopped=true;observed=false;
        SharedPreferences current=preferences;preferences=null;
        if(current!=null)try{current.unregisterOnSharedPreferenceChangeListener(listener);}
        catch(RuntimeException | io.github.libxposed.api.error.XposedFrameworkError unavailable){/* No late result is accepted. */}
    }
}
