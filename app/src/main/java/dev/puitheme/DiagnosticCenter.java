// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.os.Bundle;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Turns saved diagnostic events and the live hook report into what a person needs: a short list of conclusions,
 * a readable timeline, and one clean text report to send. Pure data in, pure data out; the screen only draws it.
 */
public final class DiagnosticCenter {
    static final int MAX_ENTRIES = 600;
    static final int EXPORT_TIMELINE = 200;

    public static final class Entry {
        final long time; final String level, source, sourceLabel, message, detail;
        Entry(long time, String level, String source, String sourceLabel, String message, String detail) {
            this.time = time; this.level = level; this.source = source; this.sourceLabel = sourceLabel; this.message = message; this.detail = detail;
        }
        String levelLabel() { return "error".equals(level) ? "错误" : "warn".equals(level) ? "警告" : "信息"; }
    }

    public static final class Result {
        final List<HookHealth.Finding> findings; final List<Entry> entries; final int errors, warnings;
        Result(List<HookHealth.Finding> findings, List<Entry> entries, int errors, int warnings) {
            this.findings = findings; this.entries = entries; this.errors = errors; this.warnings = warnings;
        }
    }

    /** What is in the saved log: how far back it goes, how much of it there is, and what an export would weigh. */
    public static final class Stats {
        public final int events, lines; public final long fileBytes, exportBytes, first, last;
        Stats(int events, int lines, long fileBytes, long exportBytes, long first, long last) {
            this.events = events; this.lines = lines; this.fileBytes = fileBytes; this.exportBytes = exportBytes; this.first = first; this.last = last;
        }
    }

    private DiagnosticCenter() { }

    static String sourceLabel(String source) {
        switch (source == null ? "" : source) {
            case "hook": return "挂钩";
            case "setting": return "设置";
            case "tile": return "磁贴";
            case "app": return "应用";
            case "update": return "更新";
            case "radio": return "网络";
            case "font": return "字体";
            case "export": return "导出";
            case "config": return "配置";
            case "scene": return "场景";
            case "clock": return "时钟";
            case "fluid": return "流体云";
            case "notification": return "通知";
            case "ui": return "界面";
            case "environment": return "环境";
            case "status_hints": return "提示图标";
            case "media": return "媒体";
            case "acrylic": return "亚克力";
            case "bigclock": return "大时钟";
            case "native-stack": return "原生堆叠";
            case "qs_style": return "磁贴样式";
            default: return source == null || source.isEmpty() ? "其他" : source;
        }
    }

    static long millis(String timestamp) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ROOT);
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            return format.parse(timestamp).getTime();
        } catch (Exception invalid) { return 0L; }
    }

    /** One display row per saved event, newest last. Nothing here reads text the sanitiser did not already clean. */
    static List<Entry> entries(List<JSONObject> events) {
        List<Entry> result = new ArrayList<>();
        if (events == null) return result;
        for (JSONObject event : events) {
            String source = event.optString("source", "app"), level = event.optString("level", "info");
            if (!"error".equals(level) && !"warn".equals(level)) level = "info";
            String message = event.optString("message", "");
            result.add(new Entry(millis(event.optString("timestamp", "")), level, source, sourceLabel(source), message, detail(event)));
        }
        int extra = result.size() - MAX_ENTRIES;
        return extra > 0 ? new ArrayList<>(result.subList(extra, result.size())) : result;
    }

    private static String detail(JSONObject event) {
        StringBuilder text = new StringBuilder();
        JSONObject details = event.optJSONObject("details");
        if (details != null) {
            String kind = details.optString("kind", "");
            if ("fact".equals(kind)) {
                text.append("状态 ").append(details.optString("topic", ""));
                JSONObject values = details.optJSONObject("values");
                if (values != null) {
                    int shown = 0;
                    for (Iterator<String> keys = values.keys(); keys.hasNext() && shown < 10; shown++) {
                        String key = keys.next();
                        text.append(shown == 0 ? "：" : "，").append(key).append('=').append(values.opt(key));
                    }
                }
            } else if ("hook_stage".equals(kind)) {
                text.append("阶段 ").append(details.optString("phase", ""));
            } else if ("setting_change".equals(kind)) {
                text.append("修改 ").append(settingName(details.optString("key", "")));
            } else if ("runtime_applied".equals(kind)) {
                text.append("系统界面进程 ").append(details.optInt("pid", 0)).append("，运行标识 ").append(details.optString("runtime", ""));
            }
        }
        JSONObject exception = event.optJSONObject("exception");
        JSONArray causes = exception == null ? null : exception.optJSONArray("causes");
        if (causes != null && causes.length() > 0) {
            if (text.length() > 0) text.append('\n');
            text.append("异常 ").append(causeLine(causes.optJSONObject(0)));
            if (causes.length() > 1) text.append("\n根因 ").append(causeLine(causes.optJSONObject(causes.length() - 1)));
        }
        return text.toString();
    }

    private static String causeLine(JSONObject cause) {
        if (cause == null) return "未知";
        StringBuilder text = new StringBuilder(cause.optString("class", "unknown"));
        JSONArray stack = cause.optJSONArray("stack");
        JSONObject top = stack == null || stack.length() == 0 ? null : stack.optJSONObject(0);
        if (top != null) text.append(" @ ").append(shortClass(top.optString("class", ""))).append('.').append(top.optString("method", ""))
                .append(':').append(top.optInt("line", -1));
        return text.toString();
    }

    private static String shortClass(String name) {
        int dot = name.lastIndexOf('.');
        return dot >= 0 && dot < name.length() - 1 ? name.substring(dot + 1) : name;
    }

    static String settingName(String key) {
        SettingsCatalog.Item item = SettingsCatalog.item(key);
        return item == null || item.title == null || item.title.isEmpty() ? key : item.title + "（" + key + "）";
    }

    /** Conclusions, most urgent first. */
    static Result analyze(List<JSONObject> events, Bundle hookHealth, boolean runtimeActive, String runtimeMessage,
                          Map<String, ?> values, JSONObject app, JSONObject packages, boolean loggingOn) {
        List<HookHealth.Finding> findings = new ArrayList<>();
        if (!runtimeActive) {
            findings.add(new HookHealth.Finding("runtime_inactive", HookHealth.Severity.ERROR, "模块没有在系统界面里生效",
                    (runtimeMessage == null || runtimeMessage.isEmpty() ? "" : runtimeMessage + "\n")
                            + "所有自定义效果都不会出现。在 LSPosed 里启用本模块并勾选“系统界面”，然后重启系统界面。"));
        }
        try {
            JSONArray advice = DiagnosticReport.diagnose(app == null ? new JSONObject() : app, packages == null ? new JSONObject() : packages, events, values);
            for (int i = 0; i < advice.length(); i++) {
                JSONObject item = advice.getJSONObject(i);
                String id = item.optString("id", ""), status = item.optString("status", "info");
                if ("none".equals(id)) continue;
                // Without any recorded apply event the runtime finding above (or the logging note below) already says it.
                if ("runtime_applied".equals(id) && (!runtimeActive || !loggingOn)) continue;
                // A verified live answer proves the running SystemUI is this build; saved events from before the last
                // reload would only argue the opposite.
                if (runtimeActive && ("runtime_applied".equals(id) || "runtime_token".equals(id) || "systemui_older_than_module".equals(id))) continue;
                HookHealth.Severity severity = "warn".equals(status) ? HookHealth.Severity.WARN : "ok".equals(status) ? HookHealth.Severity.OK : HookHealth.Severity.INFO;
                findings.add(new HookHealth.Finding(id, severity, titleOf(id), item.optString("text", "")));
            }
        } catch (JSONException ignored) { }
        if (runtimeActive) findings.add(new HookHealth.Finding("runtime_live", HookHealth.Severity.OK, "系统界面已加载当前版本",
                "刚刚向系统界面核验过：它运行的就是当前模块（" + (app == null ? "" : app.optString("expected_runtime", "")) + "）。"));
        if (runtimeActive || hookHealth != null) findings.addAll(HookHealth.findings(hookHealth));
        int errors = 0, warnings = 0;
        List<Entry> entries = entries(events);
        Map<String, int[]> repeated = new LinkedHashMap<>();
        Map<String, Entry> sample = new LinkedHashMap<>();
        for (Entry entry : entries) {
            if ("error".equals(entry.level)) errors++;
            else if ("warn".equals(entry.level)) warnings++;
            else continue;
            String key = entry.source + "|" + entry.message + "|" + firstLine(entry.detail);
            int[] count = repeated.get(key);
            if (count == null) { repeated.put(key, new int[]{1}); sample.put(key, entry); } else count[0]++;
        }
        if (errors > 0) {
            StringBuilder detail = new StringBuilder("日志里有 ").append(errors).append(" 条错误、").append(warnings).append(" 条警告。重复最多的：");
            List<Map.Entry<String, int[]>> ordered = new ArrayList<>(repeated.entrySet());
            Collections.sort(ordered, new Comparator<Map.Entry<String, int[]>>() {
                @Override public int compare(Map.Entry<String, int[]> a, Map.Entry<String, int[]> b) { return Integer.compare(b.getValue()[0], a.getValue()[0]); }
            });
            for (int i = 0; i < Math.min(3, ordered.size()); i++) {
                Entry first = sample.get(ordered.get(i).getKey());
                detail.append("\n· ").append(first.sourceLabel).append("：").append(first.message).append("（").append(ordered.get(i).getValue()[0]).append(" 次）");
                if (!first.detail.isEmpty()) detail.append("\n  ").append(firstLine(first.detail));
            }
            findings.add(new HookHealth.Finding("log_errors", HookHealth.Severity.WARN, errors + " 条错误记录", detail.toString()));
        } else if (loggingOn && !entries.isEmpty()) {
            findings.add(new HookHealth.Finding("log_clean", HookHealth.Severity.OK, "日志里没有错误", "已记录 " + entries.size() + " 条事件，其中警告 " + warnings + " 条。"));
        }
        if (!loggingOn) findings.add(new HookHealth.Finding("logging_off", HookHealth.Severity.INFO, "诊断日志没有开启",
                "挂钩状态不依赖日志。要看异常和时间线，先开启“诊断日志”，复现问题后回到这里。"));
        else if (entries.isEmpty()) findings.add(new HookHealth.Finding("log_empty", HookHealth.Severity.INFO, "还没有日志事件",
                "诊断日志已开启，但还没有记录。重启系统界面并操作一下有问题的功能后再来看。"));
        Collections.sort(findings, new Comparator<HookHealth.Finding>() {
            @Override public int compare(HookHealth.Finding a, HookHealth.Finding b) { return Integer.compare(rank(a.severity), rank(b.severity)); }
        });
        return new Result(findings, entries, errors, warnings);
    }

    private static int rank(HookHealth.Severity severity) {
        switch (severity) { case ERROR: return 0; case WARN: return 1; case INFO: return 2; default: return 3; }
    }

    private static String firstLine(String text) {
        int newline = text.indexOf('\n');
        return newline < 0 ? text : text.substring(0, newline);
    }

    private static String titleOf(String id) {
        switch (id) {
            case "runtime_applied": return "系统界面应用配置";
            case "runtime_token": return "系统界面运行的版本";
            case "systemui_older_than_module": return "系统界面进程比模块旧";
            case "systemui_version_changed": return "系统界面发生了更新";
            case "safe_mode": return "安全模式";
            case "dual_clock": return "双排时钟";
            case "clock_tick": return "时钟秒数刷新";
            default: return id;
        }
    }

    /** One readable plain-text report. */
    static String exportText(Result result, JSONObject device, JSONObject app, JSONObject packages, Bundle hookHealth,
                             Map<String, ?> values, long now) {
        StringBuilder out = new StringBuilder();
        SimpleDateFormat local = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT);
        out.append("C17 状态栏 · 诊断报告\n");
        out.append("生成时间  ").append(local.format(new Date(now))).append("  ").append(zone()).append('\n');
        out.append("模块版本  ").append(app == null ? "未知" : app.optString("version_name", "未知"))
                .append(" (").append(app == null ? 0 : app.optLong("version_code", 0)).append(")\n");
        out.append("运行标识  ").append(app == null ? "未知" : app.optString("expected_runtime", "未知")).append('\n');
        out.append("说明      报告已去除通知正文、密码、网络名与文件路径；请同时说明故障出现的方向、操作顺序、快慢，最好附录屏。\n");

        section(out, "一、结论");
        if (result.findings.isEmpty()) out.append("  没有发现明显问题。\n");
        for (HookHealth.Finding finding : result.findings) {
            out.append("  [").append(severityLabel(finding.severity)).append("] ").append(finding.title).append('\n');
            for (String line : finding.detail.split("\n")) if (!line.isEmpty()) out.append("      ").append(line).append('\n');
        }

        section(out, "二、设备与系统");
        if (device != null) {
            line(out, "机型", join(device.optString("manufacturer"), device.optString("model")));
            line(out, "Android", device.optString("android") + "（API " + device.optInt("sdk") + "）");
            line(out, "ColorOS", device.optString("coloros"));
            line(out, "版本号", device.optString("build_display"));
            line(out, "语言/时区", device.optString("locale") + " · " + device.optString("time_zone"));
            JSONObject display = device.optJSONObject("display");
            if (display != null) line(out, "屏幕", display.optInt("width_px") + "×" + display.optInt("height_px") + " px · 密度 "
                    + display.optDouble("density") + " · 字体缩放 " + display.optDouble("font_scale") + " · "
                    + ("landscape".equals(display.optString("orientation")) ? "横屏" : "竖屏") + (display.optBoolean("night") ? " · 深色" : ""));
        }
        if (packages != null) for (Iterator<String> keys = packages.keys(); keys.hasNext();) {
            String name = keys.next();
            JSONObject info = packages.optJSONObject(name);
            if (info != null) line(out, shortPackage(name), info.optString("version_name") + " (" + info.optLong("version_code") + ")，更新于 "
                    + local.format(new Date(info.optLong("last_update_ms"))));
        }

        section(out, "三、挂钩状态");
        List<String[]> table = HookHealth.table(hookHealth);
        if (table.isEmpty()) out.append("  系统界面没有回应，无法得到挂钩状态。\n");
        for (String[] row : table) out.append("  ").append(pad(row[1], 22)).append(pad(row[2], 8)).append(row[3]).append('\n');

        section(out, "四、已保存的配置");
        int shown = 0;
        TreeMap<String, Object> sorted = new TreeMap<>();
        if (values != null) for (Map.Entry<String, ?> entry : values.entrySet()) if (DiagnosticReport.known(entry.getKey())) sorted.put(entry.getKey(), entry.getValue());
        for (Map.Entry<String, Object> entry : sorted.entrySet()) {
            if (shown >= 400) { out.append("  … 其余省略\n"); break; }
            out.append("  ").append(settingName(entry.getKey())).append(" = ").append(safeValue(entry.getKey(), entry.getValue())).append('\n');
            shown++;
        }
        if (shown == 0) out.append("  全部为默认值。\n");

        section(out, "五、近期事件（最近 " + Math.min(EXPORT_TIMELINE, result.entries.size()) + " 条，本地时间）");
        int from = Math.max(0, result.entries.size() - EXPORT_TIMELINE);
        SimpleDateFormat clock = new SimpleDateFormat("MM-dd HH:mm:ss", Locale.ROOT);
        for (int i = from; i < result.entries.size(); i++) {
            Entry entry = result.entries.get(i);
            out.append("  ").append(clock.format(new Date(entry.time))).append("  ").append(pad(entry.levelLabel(), 6)).append(pad(entry.sourceLabel, 10))
                    .append(entry.message).append('\n');
            if (!entry.detail.isEmpty()) for (String line : entry.detail.split("\n")) out.append("      ").append(line).append('\n');
        }
        if (result.entries.isEmpty()) out.append("  没有记录。\n");
        return out.toString();
    }

    private static String safeValue(String key, Object raw) {
        try {
            JSONObject value = DiagnosticReport.settingValue(key, raw);
            if (value == null) return "—";
            String type = value.optString("type");
            if ("text".equals(type)) return "（文本，" + value.optInt("length", 0) + " 字，已隐藏）";
            if ("font".equals(type)) return "字体 " + value.optString("source");
            if ("priority".equals(type)) return String.valueOf(value.optJSONArray("slots"));
            return String.valueOf(value.opt("value"));
        } catch (JSONException invalid) { return "—"; }
    }

    private static void section(StringBuilder out, String title) { out.append('\n').append(title).append('\n'); }
    private static void line(StringBuilder out, String label, String value) { out.append("  ").append(pad(label, 12)).append(value).append('\n'); }
    private static String join(String a, String b) { return (a == null ? "" : a) + (b == null || b.isEmpty() ? "" : " " + b); }
    private static String shortPackage(String name) { return name.replace("com.android.", "").replace("com.oplus.", "oplus."); }
    private static String severityLabel(HookHealth.Severity severity) {
        switch (severity) { case ERROR: return "严重"; case WARN: return "警告"; case OK: return "正常"; default: return "提示"; }
    }
    private static String zone() { return TimeZone.getDefault().getID(); }
    /** Pads to a display width, counting each CJK character as two columns. */
    static String pad(String text, int columns) {
        int width = 0;
        for (int i = 0; i < text.length(); i++) width += text.charAt(i) > 0x2E7F ? 2 : 1;
        StringBuilder out = new StringBuilder(text);
        for (; width < columns; width++) out.append(' ');
        return out.toString();
    }
}
