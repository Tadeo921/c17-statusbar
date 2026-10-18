// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import java.text.DateFormat;
import java.util.Date;

/**
 * Wording for the companion module's benefit record. Costs are what ART itself reported for each compile;
 * "avoided" is an estimate against a module that recompiled on every check; the runtime lines are one sample of SystemUI,
 * not a before/after comparison, so no speed-up percentage is ever produced here.
 */
final class PerformanceBenefit {
    final String since, compiles, compilesNote, saved, savedNote, aot, aotNote, jit, jitNote;

    private PerformanceBenefit(Builder b) {
        since = b.since; compiles = b.compiles; compilesNote = b.compilesNote; saved = b.saved; savedNote = b.savedNote;
        aot = b.aot; aotNote = b.aotNote; jit = b.jit; jitNote = b.jitNote;
    }

    static PerformanceBenefit of(RootPerformanceModule.Snapshot report) {
        Builder b = new Builder();
        boolean ledger = report != null && report.ledgerSince > 0L;
        b.since = ledger ? "自 " + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                .format(new Date(report.ledgerSince * 1000L)) + " 起" : "尚无记录";
        b.compiles = ledger ? report.ledgerCompiles + " 次" : "—";
        b.compilesNote = ledger ? "耗时 " + seconds(report.ledgerWallMs)
                + (report.ledgerSizeKb > 0L ? " · 新增产物 " + Math.round(report.ledgerSizeKb / 1024.0) + " MB" : "") : "ART 实测耗时";
        b.saved = ledger ? report.ledgerReuses + " 次" : "—";
        b.savedNote = ledger ? "约省编译 " + seconds(report.ledgerAvoidedMs) + "（估算）" : "相比每次都重编";
        boolean sampled = report != null && report.runtimeSampled > 0L && report.runtimeProcSeconds > 0L;
        b.aot = sampled ? report.runtimeAot ? "已使用" : "未使用" : "—";
        b.aotNote = sampled ? report.runtimeAot ? "系统界面运行的是预编译产物" : "系统界面尚在解释执行" : "是否使用预编译产物";
        boolean longEnough = sampled && report.runtimeProcSeconds >= 60L;
        b.jit = longEnough ? Math.round(report.runtimeJitMs / (report.runtimeProcSeconds / 60.0)) + " ms/分" : "—";
        b.jitNote = longEnough ? "JIT 线程占用 · 进程已运行 " + Math.round(report.runtimeProcSeconds / 60.0) + " 分钟"
                : sampled ? "进程刚启动，样本太短" : "JIT 线程占用";
        return new PerformanceBenefit(b);
    }

    static String seconds(long millis) {
        return (millis >= 10000L ? Long.toString(Math.round(millis / 1000.0)) : String.format(java.util.Locale.ROOT, "%.1f", millis / 1000.0)) + " 秒";
    }

    private static final class Builder {
        String since, compiles, compilesNote, saved, savedNote, aot, aotNote, jit, jitNote;
    }
}
