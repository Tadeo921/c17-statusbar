// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemClock;

/** One persisted system job per need/event; no app service, alarm loop or wake lock. */
public final class PerformanceScheduler {
    static final String METHOD = "performance_schedule";
    static final int JOB_ID = 171041;
    private static final String STORE = "performance_schedule";
    private PerformanceScheduler() { }

    private static SharedPreferences preferences(Context context) {
        return context.createDeviceProtectedStorageContext().getSharedPreferences(STORE, Context.MODE_PRIVATE);
    }
    static synchronized void observe(Context context, RootPerformanceModule.Snapshot report) {
        if (context == null || report == null || !report.root) return;
        SharedPreferences saved = preferences(context);
        boolean before = saved.getBoolean("enabled", false);
        boolean enabled = PerformanceSchedulePolicy.available(report);
        saved.edit().putBoolean("authorized", true).putBoolean("enabled", enabled).apply();
        if (!enabled) { cancel(context, "inactive"); return; }
        if (PerformanceSchedulePolicy.unsafe(report)) { block(context); return; }
        if (PerformanceSchedulePolicy.running(report)) return;
        if (saved.getBoolean("blocked", false)) {
            if (report.currentBoot && ("ok".equals(report.lastState) || "current".equals(report.lastState)))
                saved.edit().putBoolean("blocked", false).putInt("result_checks", 0).apply();
            else return;
        }
        if (!before || (PerformanceSchedulePolicy.needsCheck(report) && saved.getInt("attempts", 0) < PerformanceSchedulePolicy.MAX_ENV_RETRIES))
            enqueue(context, PerformanceSchedulePolicy.IDLE_DELAY_MS, false);
    }
    /** The provider authenticates the caller synchronously. This only changes scheduling. */
    static synchronized Bundle hint(Context context, String event) {
        return hint(context, event, false);
    }
    static synchronized Bundle hint(Context context, String event, boolean rootBoot) {
        Bundle reply = new Bundle();
        if (context == null || !("boot".equals(event) || "screen_off".equals(event) || "screen_on".equals(event) || "package_changed".equals(event))) {
            reply.putBoolean("accepted", false); return reply;
        }
        SharedPreferences saved = preferences(context);
        // The validated module service can register after an APK-driven upgrade, before the UI is reopened.
        // A previous successful rooted report is required, so boot never initiates a new authorization prompt.
        if (rootBoot && "boot".equals(event) && saved.getBoolean("authorized", false))
            saved.edit().putBoolean("enabled", true).apply();
        if ("boot".equals(event)) saved.edit().putBoolean("blocked", false).putInt("attempts", 0).putInt("result_checks", 0).apply();
        if (!saved.getBoolean("enabled", false) || saved.getBoolean("blocked", false)) {
            reply.putBoolean("accepted", false); return reply;
        }
        if (!"running".equals(saved.getString("state", "inactive"))) {
            saved.edit().putInt("attempts", 0).apply();
            enqueue(context, "screen_off".equals(event) ? 0 : PerformanceSchedulePolicy.IDLE_DELAY_MS, "screen_off".equals(event));
        }
        reply.putBoolean("accepted", true); return reply;
    }
    static synchronized void packageChanged(Context context) {
        if (!preferences(context).getBoolean("enabled", false)) return;
        if (preferences(context).getBoolean("blocked", false)) return;
        preferences(context).edit().putInt("attempts", 0).apply();
        enqueue(context, PerformanceSchedulePolicy.IDLE_DELAY_MS, false);
    }
    static synchronized boolean mayRun(Context context) {
        SharedPreferences saved = preferences(context);
        return saved.getBoolean("enabled", false) && !saved.getBoolean("blocked", false);
    }
    static synchronized boolean mayLaunch(Context context, RootPerformanceModule.Snapshot report) {
        int attempts = preferences(context).getInt("attempts", 0);
        return !("deferred".equals(report.lastState) && attempts >= PerformanceSchedulePolicy.MAX_ENV_RETRIES)
                && !("failed".equals(report.lastState) && report.currentBoot && attempts > 0);
    }
    static synchronized void started(Context context) { writeState(context, "running", 0); }
    static synchronized void busy(Context context) { confirmLater(context); }
    static synchronized void block(Context context) {
        preferences(context).edit().putBoolean("blocked", true).apply(); cancel(context, "blocked");
    }
    static synchronized void stop(Context context) {
        if ("running".equals(preferences(context).getString("state", ""))) writeState(context, "deferred", 0);
    }
    static synchronized void finish(Context context, RootPerformanceModule.Result result, boolean launched) {
        RootPerformanceModule.Snapshot report = result.snapshot;
        SharedPreferences saved = preferences(context);
        if (!result.success && RootPerformanceModule.busy()) { confirmLater(context); return; }
        if (!result.success || report == null || !report.root) {
            // Never repeatedly prompt for revoked root authorization from background events.
            saved.edit().putBoolean("enabled", false).apply();
            JobScheduler jobs = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            if (jobs != null) jobs.cancel(JOB_ID);
            writeState(context, "unavailable", 0, false); return;
        }
        if (!PerformanceSchedulePolicy.available(report)) {
            saved.edit().putBoolean("enabled", false).apply(); cancel(context, "inactive"); return;
        }
        if (PerformanceSchedulePolicy.unsafe(report)) { block(context); return; }
        int attempts = saved.getInt("attempts", 0);
        if (launched) { attempts++; saved.edit().putInt("attempts", attempts).putInt("result_checks", 0).apply(); }
        if (PerformanceSchedulePolicy.running(report)) { confirmLater(context); return; }
        if (PerformanceSchedulePolicy.retryEnvironment(report, attempts)) {
            enqueue(context, PerformanceSchedulePolicy.IDLE_DELAY_MS, true); return;
        }
        writeState(context, "deferred".equals(report.lastState) ? "deferred" : "inactive", 0);
    }
    private static void confirmLater(Context context) {
        SharedPreferences saved = preferences(context);
        int checks = saved.getInt("result_checks", 0) + 1;
        saved.edit().putInt("result_checks", checks).apply();
        if (checks >= PerformanceSchedulePolicy.MAX_RESULT_CHECKS) { block(context); return; }
        enqueue(context, PerformanceSchedulePolicy.RESULT_DELAY_MS, true);
    }
    private static void cancel(Context context, String state) {
        JobScheduler jobs = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (jobs != null) jobs.cancel(JOB_ID);
        writeState(context, state, 0);
    }
    private static void enqueue(Context context, long delay, boolean replace) {
        JobScheduler jobs = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (jobs == null) { writeState(context, "unavailable", 0); return; }
        if (!replace && jobs.getPendingJob(JOB_ID) != null) return;
        try {
            JobInfo job = new JobInfo.Builder(JOB_ID, new ComponentName(context, PerformanceJobService.class))
                    .setMinimumLatency(Math.max(0, delay)).setPersisted(true).build();
            if (jobs.schedule(job) == JobScheduler.RESULT_SUCCESS) writeState(context, "scheduled", System.currentTimeMillis() / 1000L + delay / 1000L);
            else writeState(context, "unavailable", 0);
        } catch (RuntimeException denied) { writeState(context, "unavailable", 0); }
    }
    private static void writeState(Context context, String state, long nextAfter) {
        writeState(context, state, nextAfter, true);
    }
    private static void writeState(Context context, String state, long nextAfter, boolean reportRoot) {
        SharedPreferences saved = preferences(context);
        String previous = saved.getString("state", "");
        long previousTime = saved.getLong("next_after", -1);
        if (state.equals(previous) && previousTime == nextAfter) return;
        saved.edit().putString("state", state).putLong("next_after", nextAfter)
                .putLong("updated_elapsed", SystemClock.elapsedRealtime()).apply();
        if (reportRoot) RootPerformanceModule.recordSchedule(state, nextAfter);
    }
}
