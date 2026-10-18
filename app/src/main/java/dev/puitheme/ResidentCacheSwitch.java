// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The in-app resource cache lives in SystemUI's own storage and needs no root. Its switch is an
 * ordinary confirmed preference write; SystemUI reports the real state on the next activation check.
 * (The former root backup of this cache is retired; the performance module removes it.)
 */
public final class ResidentCacheSwitch {
    public enum Action { CACHE_ON, CACHE_STOP, CACHE_REMOVE }
    public interface Callback { void completed(Result result); }
    public static final class Request {
        private volatile boolean detached;
        public void cancel() { detached = true; }
    }
    public static final class Result {
        public final boolean success;
        public final String message;
        Result(boolean success, String message) { this.success = success; this.message = message; }
    }
    private static final ExecutorService WORK = Executors.newSingleThreadExecutor();
    private static final AtomicBoolean BUSY = new AtomicBoolean();
    public static boolean busy() { return BUSY.get(); }
    private ResidentCacheSwitch() { }

    public static Request submit(Context host, Action action, Callback callback) {
        Request request = new Request();
        Context application = host.getApplicationContext();
        final Context context = application == null ? host : application;
        Handler main = new Handler(Looper.getMainLooper());
        if (!BUSY.compareAndSet(false, true)) {
            main.post(() -> { if (!request.detached) callback.completed(new Result(false, "已有缓存操作正在执行，请稍后再试。")); });
            return request;
        }
        WORK.execute(() -> {
            Result result;
            try {
                boolean installed = action != Action.CACHE_REMOVE, running = action == Action.CACHE_ON;
                SharedPreferences preferences = StatusBarSettings.preferences(context);
                SharedPreferences confirmed = new ActivationGuardPreferences(preferences, () -> true, () -> {}).withPersistence(context, () -> {});
                if (!confirmed.edit().putBoolean(ResidentCache.INSTALLED, installed).putBoolean(ResidentCache.RUNNING, running).commit()) {
                    result = new Result(false, "缓存设置保存失败，请重试。");
                } else {
                    context.getContentResolver().notifyChange(Uri.parse(StatusBarSettings.CONTENT_URI), null);
                    result = new Result(true, action == Action.CACHE_ON ? "缓存已启动，正在向系统界面确认实际状态。"
                            : action == Action.CACHE_STOP ? "缓存已关闭，文件保留。" : "缓存已卸载，系统界面会删除自己的缓存文件。");
                }
            } catch (RuntimeException failure) {
                result = new Result(false, "缓存设置保存失败，请重试。");
            }
            BUSY.set(false);
            final Result completed = result;
            main.post(() -> { if (!request.detached) callback.completed(completed); });
        });
        return request;
    }
}
