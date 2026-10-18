// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Protected platform broadcasts only; package changes just request a bounded check. */
public final class PerformanceScheduleReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) PerformanceScheduler.hint(context, "boot");
        else if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) PerformanceScheduler.packageChanged(context);
    }
}
