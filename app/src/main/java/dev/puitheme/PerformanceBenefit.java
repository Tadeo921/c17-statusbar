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
        b.since = ledger ? "Desde " + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                .format(new Date(report.ledgerSince * 1000L)) + " en adelante" : "Sin registros";
        b.compiles = ledger ? report.ledgerCompiles + " veces" : "—";
        b.compilesNote = ledger ? "Tiempo empleado: " + seconds(report.ledgerWallMs)
                + (report.ledgerSizeKb > 0L ? " · artefactos nuevos: " + Math.round(report.ledgerSizeKb / 1024.0) + " MB" : "") : "Tiempo medido por ART";
        b.saved = ledger ? report.ledgerReuses + " veces" : "—";
        b.savedNote = ledger ? "Ahorro estimado de compilación: " + seconds(report.ledgerAvoidedMs) + " (estimado)" : "Comparado con recompilar en cada comprobación";
        boolean sampled = report != null && report.runtimeSampled > 0L && report.runtimeProcSeconds > 0L;
        b.aot = sampled ? report.runtimeAot ? "Sí" : "No" : "—";
        b.aotNote = sampled ? report.runtimeAot ? "SystemUI está usando código precompilado" : "SystemUI todavía ejecuta código interpretado" : "Uso de código precompilado";
        boolean longEnough = sampled && report.runtimeProcSeconds >= 60L;
        b.jit = longEnough ? Math.round(report.runtimeJitMs / (report.runtimeProcSeconds / 60.0)) + " ms/min" : "—";
        b.jitNote = longEnough ? "Uso del hilo JIT · proceso activo durante " + Math.round(report.runtimeProcSeconds / 60.0) + " min"
                : sampled ? "El proceso acaba de iniciarse; la muestra es demasiado breve" : "Uso del hilo JIT";
        return new PerformanceBenefit(b);
    }

    static String seconds(long millis) {
        return (millis >= 10000L ? Long.toString(Math.round(millis / 1000.0)) : String.format(java.util.Locale.ROOT, "%.1f", millis / 1000.0)) + " s";
    }

    private static final class Builder {
        String since, compiles, compilesNote, saved, savedNote, aot, aotNote, jit, jitNote;
    }
}
