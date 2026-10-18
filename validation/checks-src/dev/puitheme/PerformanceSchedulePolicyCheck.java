package dev.puitheme;

import java.util.HashMap;
import java.util.Map;

/** Replay authentic report facts: eligibility, stale tasks, blocked cancellation, bounded retries. */
public final class PerformanceSchedulePolicyCheck {
    private static int checks;
    private static final String BOOT = "abcdef12-1234-1234-1234-abcdef123456";
    private static void require(boolean value, String why) { checks++; if (!value) throw new AssertionError(why); }
    private static Map<String, String> fields() {
        Map<String, String> fields = new HashMap<>();
        fields.put("installed", "1"); fields.put("pending", "0"); fields.put("disabled", "0"); fields.put("remove", "0");
        fields.put("module_version_code", Long.toString(RootPerformanceModule.VERSION_CODE));
        fields.put("last_module_version_code", Long.toString(RootPerformanceModule.VERSION_CODE));
        fields.put("boot", BOOT); fields.put("last_boot", BOOT); fields.put("last_mode", "auto");
        fields.put("last_state", "current"); fields.put("last_error", "none"); fields.put("last_deferred", "0");
        fields.put("last_packages", "2"); fields.put("busy", "0"); fields.put("active_job", "0"); fields.put("record_present", "1"); return fields;
    }
    private static RootPerformanceModule.Snapshot report(Map<String, String> fields) { return new RootPerformanceModule.Snapshot(fields, true); }
    public static void main(String[] args) {
        Map<String, String> f = fields();
        require(PerformanceSchedulePolicy.available(report(f)), "active module unavailable");
        require(!PerformanceSchedulePolicy.needsCheck(report(f)), "current record queued unnecessary work");
        for (String flag : new String[] {"pending", "disabled", "remove"}) {
            f = fields(); f.put(flag, "1"); require(!PerformanceSchedulePolicy.available(report(f)), flag + " accepted");
        }
        f = fields(); f.put("installed", "0"); require(!PerformanceSchedulePolicy.available(report(f)), "missing module accepted");
        require(!PerformanceSchedulePolicy.available(new RootPerformanceModule.Snapshot(fields(), false)), "root-denied accepted");
        require(!PerformanceSchedulePolicy.available(null), "null accepted");
        f = fields(); f.put("module_version_code", "4"); require(!PerformanceSchedulePolicy.available(report(f)), "old module supports nonexistent auto protocol");
        f = fields(); f.put("last_boot", "12345678-1234-1234-1234-abcdef123456");
        require(PerformanceSchedulePolicy.needsCheck(report(f)), "historical record hid pending check");
        f.put("last_state", "running"); require(!PerformanceSchedulePolicy.running(report(f)), "historical task counted running");
        f = fields(); f.put("last_state", "running"); f.put("busy", "1"); require(PerformanceSchedulePolicy.running(report(f)), "current auto task lost");
        for (String mode : new String[] {"boot", "run", "auto"}) {
            f.put("last_mode", mode); require(PerformanceSchedulePolicy.running(report(f)), mode + " task lost");
        }
        f.put("last_mode", "uninstall"); require(PerformanceSchedulePolicy.running(report(f)), "live task allowed overlapping optimization"); f.put("busy", "0"); require(!PerformanceSchedulePolicy.running(report(f)), "stale same-boot record counted live");
        f = fields(); f.put("last_state", "waiting"); f.put("busy", "1"); require(PerformanceSchedulePolicy.running(report(f)), "preparing task overlapped");
        f.put("last_module_version_code", "4"); require(PerformanceSchedulePolicy.running(report(f)), "live lock ignored because status version old");
        f = fields(); f.put("last_state", "failed");
        for (String error : new String[] {"compile_cancel_failed", "restore_cancel_failed"}) {
            f.put("last_error", error);
            // The record only blocks while its task marker exists; the module clears an unknown one after 15 minutes.
            for (String marker : new String[] {"1", "unknown"}) {
                f.put("active_job", marker); require(PerformanceSchedulePolicy.unsafe(report(f)), error + " retried with marker " + marker);
            }
            f.put("active_job", "0"); require(!PerformanceSchedulePolicy.unsafe(report(f)), error + " still blocks after the marker was cleared");
            f.remove("active_job"); require(PerformanceSchedulePolicy.unsafe(report(f)), error + " retried without a task report");
            f.put("active_job", "0");
        }
        f.put("last_error", "interrupted"); require(PerformanceSchedulePolicy.unsafe(report(f)), "interrupted retried");
        Map<String, String> old = new HashMap<>(f); old.put("last_boot", "12345678-1234-1234-1234-abcdef123456");
        require(!PerformanceSchedulePolicy.unsafe(report(old)), "old interruption blocked fresh boot");
        f.put("last_error", "compile_failed"); require(!PerformanceSchedulePolicy.unsafe(report(f)), "ordinary failure mislabeled cancellation");
        f = fields(); f.put("last_state", "deferred"); f.put("last_deferred", "2");
        require(PerformanceSchedulePolicy.needsCheck(report(f)), "deferred component ignored");
        for (int attempts = 0; attempts < 3; attempts++) require(PerformanceSchedulePolicy.retryEnvironment(report(f), attempts), "retry lost at " + attempts);
        require(!PerformanceSchedulePolicy.retryEnvironment(report(f), 3), "unlimited retries");
        require(!PerformanceSchedulePolicy.retryEnvironment(report(f), -1), "negative retry count accepted");
        f.put("last_deferred", "0"); require(!PerformanceSchedulePolicy.retryEnvironment(report(f), 0), "no pending components retried");
        f.put("last_deferred", "2"); f.put("disabled", "1"); require(!PerformanceSchedulePolicy.retryEnvironment(report(f), 0), "disabled module retried");
        require(PerformanceSchedulePolicy.IDLE_DELAY_MS == 600_000L, "ten-minute input boundary changed");
        require(PerformanceSchedulePolicy.RESULT_DELAY_MS >= 300_000L, "result polling became frequent");
        System.out.println("Performance schedule policy checks: " + checks);
    }
}
