// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

/** Small, deterministic policy for bounded, event-driven automatic checks. */
final class PerformanceSchedulePolicy {
    static final long IDLE_DELAY_MS = 600_000L;
    static final long RESULT_DELAY_MS = 300_000L;
    static final int MAX_ENV_RETRIES = 3;
    static final int MAX_RESULT_CHECKS = 6;
    private PerformanceSchedulePolicy() { }

    static boolean available(RootPerformanceModule.Snapshot report) {
        return report != null && report.root && report.installed && !report.pending
                && !report.disabled && !report.remove && !report.outdated
                && report.moduleVersionCode >= RootPerformanceModule.VERSION_CODE;
    }
    static boolean unsafe(RootPerformanceModule.Snapshot report) {
        // A cancel failure only blocks while its task marker still exists; the module drops an unknown one after 15 minutes.
        return report != null && (report.taskBlocked || (report.currentBoot && (("compile_cancel_failed".equals(report.error)
                || "restore_cancel_failed".equals(report.error)) && !"0".equals(report.activeJobState)
                || "interrupted".equals(report.error))));
    }
    static boolean running(RootPerformanceModule.Snapshot report) {
        // A same-boot running record can outlive a killed shell. Only a live validated lock is work in flight.
        return report != null && report.taskActive;
    }
    static boolean needsCheck(RootPerformanceModule.Snapshot report) {
        return available(report) && (!report.currentBoot
                || report.recordVersionCode != report.moduleVersionCode
                || "unknown".equals(report.lastState) || "deferred".equals(report.lastState));
    }
    static boolean retryEnvironment(RootPerformanceModule.Snapshot report, int attempts) {
        return available(report) && report.currentBoot && "deferred".equals(report.lastState)
                && report.deferred > 0 && attempts >= 0 && attempts < MAX_ENV_RETRIES && !unsafe(report);
    }
}
