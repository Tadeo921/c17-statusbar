package dev.puitheme;

import java.util.HashMap;
import java.util.Map;

public final class PerformanceBenefitCheck {
    private static int checks;
    private static final String BOOT = "abcdef12-1234-1234-1234-abcdef123456";
    private static void equal(Object expected, Object actual) {
        checks++;
        if (!expected.equals(actual)) throw new AssertionError("expected " + expected + " but was " + actual);
    }
    private static Map<String, String> fields() {
        Map<String, String> f = new HashMap<>();
        f.put("installed", "1"); f.put("pending", "0"); f.put("disabled", "0"); f.put("remove", "0");
        f.put("boot", BOOT); f.put("last_boot", BOOT); f.put("record_present", "1");
        return f;
    }
    private static RootPerformanceModule.Snapshot report(Map<String, String> f) { return new RootPerformanceModule.Snapshot(f, true); }
    public static void main(String[] args) {
        Map<String, String> f = fields();
        f.put("total_since", "1792257000"); f.put("total_compiles", "3"); f.put("total_wall_ms", "12500"); f.put("total_size_kb", "6144");
        f.put("total_reuses", "8"); f.put("total_avoided_ms", "30000");
        f.put("runtime_sampled", "1792257900"); f.put("runtime_jit_ms", "6000"); f.put("runtime_proc_s", "600"); f.put("runtime_aot", "1");
        PerformanceBenefit full = PerformanceBenefit.of(report(f));
        equal("3 次", full.compiles); equal("8 次", full.saved); equal("已使用", full.aot); equal("600 ms/分", full.jit);
        equal(true, full.compilesNote.contains("13 秒") && full.compilesNote.contains("6 MB"));
        equal(true, full.savedNote.contains("30 秒") && full.savedNote.contains("估算"));
        equal(true, full.jitNote.contains("10 分钟")); equal(true, full.since.startsWith("自 "));

        PerformanceBenefit none = PerformanceBenefit.of(report(fields()));
        equal("—", none.compiles); equal("—", none.saved); equal("—", none.aot); equal("—", none.jit); equal("尚无记录", none.since);
        equal("—", PerformanceBenefit.of(null).compiles);

        f.put("runtime_proc_s", "30");
        PerformanceBenefit young = PerformanceBenefit.of(report(f));
        equal("—", young.jit); equal("进程刚启动，样本太短", young.jitNote); equal("已使用", young.aot);
        f.put("runtime_proc_s", "600"); f.put("runtime_aot", "0");
        equal("未使用", PerformanceBenefit.of(report(f)).aot);
        equal("1.5 秒", PerformanceBenefit.seconds(1500)); equal("10 秒", PerformanceBenefit.seconds(10000)); equal("0.0 秒", PerformanceBenefit.seconds(0));
        // No speed-up percentage may ever be produced from these counters.
        for (String text : new String[] {full.compiles, full.compilesNote, full.saved, full.savedNote, full.aot, full.aotNote, full.jit, full.jitNote})
            equal(false, text.contains("%") || text.contains("加速"));
        System.out.println("PerformanceBenefitCheck: " + checks + " checks passed");
    }
}
