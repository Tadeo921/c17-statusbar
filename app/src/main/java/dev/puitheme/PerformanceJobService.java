// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.app.job.JobParameters;
import android.app.job.JobService;

/** Only JobScheduler (BIND_JOB_SERVICE) may enter; no input can select a root command. */
public final class PerformanceJobService extends JobService {
    private RootPerformanceModule.Request request;
    private JobParameters active;
    @Override public boolean onStartJob(JobParameters parameters) {
        if (!PerformanceScheduler.mayRun(this)) return false;
        active = parameters; PerformanceScheduler.started(this);
        if (RootPerformanceModule.busy()) {
            active = null; jobFinished(parameters, false); PerformanceScheduler.busy(this); return true;
        }
        request = RootPerformanceModule.submit(this, RootPerformanceModule.Action.CHECK, result -> {
            if (active != parameters) return;
            if (!result.success || !PerformanceSchedulePolicy.available(result.snapshot)
                    || PerformanceSchedulePolicy.unsafe(result.snapshot) || PerformanceSchedulePolicy.running(result.snapshot)
                    || !PerformanceScheduler.mayLaunch(this, result.snapshot)) {
                finish(parameters, result, false); return;
            }
            request = RootPerformanceModule.submit(this, RootPerformanceModule.Action.AUTOMATIC, automatic -> {
                if (active == parameters) finish(parameters, automatic, true);
            });
        });
        return true;
    }
    private void finish(JobParameters parameters, RootPerformanceModule.Result result, boolean launched) {
        request = null; active = null;
        jobFinished(parameters, false);
        PerformanceScheduler.finish(this, result, launched);
    }
    @Override public boolean onStopJob(JobParameters parameters) {
        active = null;
        if (request != null) request.cancel(); request = null;
        // Detaching never cancels an already accepted ART operation. A later event reads its status.
        PerformanceScheduler.stop(this); return false;
    }
}
