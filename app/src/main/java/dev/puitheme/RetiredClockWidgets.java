// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;
import android.appwidget.AppWidgetHost;
import android.content.Context;
import android.content.SharedPreferences;

/** Retire only this app's code168 host, once. No desktop or other host ID is touched. */
final class RetiredClockWidgets {
    private static boolean attempted;
    static synchronized void release(Context context) {
        if(attempted||context==null||!ModuleDiagnostics.PACKAGE_NAME.equals(context.getPackageName()))return;
        attempted=true;
        SharedPreferences local=context.getSharedPreferences("clock_widget_slots_v1",Context.MODE_PRIVATE);
        if(local.getBoolean("text_components_retired169",false))return;
        try {
            new AppWidgetHost(context,0x43313757).deleteHost();
            local.edit().clear().putBoolean("text_components_retired169",true).commit();
        } catch(RuntimeException unavailable) { /* A later app process can retry; text remains usable. */ }
    }
    private RetiredClockWidgets(){}
}
