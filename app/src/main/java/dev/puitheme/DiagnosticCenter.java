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
        String levelLabel() { return "error".equals(level) ? "Error" : "warn".equals(level) ? "Advertencia" : "Información"; }
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
            case "hook": return "Hooks";
            case "setting": return "Ajustes";
            case "tile": return "Tarjetas";
            case "app": return "Aplicación";
            case "update": return "Actualización";
            case "radio": return "Red";
            case "font": return "Fuentes";
            case "export": return "Exportación";
            case "config": return "Configuración";
            case "scene": return "Escena";
            case "clock": return "Reloj";
            case "fluid": return "Fluid Cloud";
            case "notification": return "Notificaciones";
            case "ui": return "Interfaz";
            case "environment": return "Entorno";
            case "status_hints": return "Iconos de aviso";
            case "media": return "Contenido multimedia";
            case "acrylic": return "Acrílico";
            case "bigclock": return "Reloj grande";
            case "native-stack": return "Apilado nativo";
            case "qs_style": return "Estilo de las tarjetas";
            default: return source == null || source.isEmpty() ? "Otros" : source;
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
                text.append("Estado ").append(details.optString("topic", ""));
                JSONObject values = details.optJSONObject("values");
                if (values != null) {
                    int shown = 0;
                    for (Iterator<String> keys = values.keys(); keys.hasNext() && shown < 10; shown++) {
                        String key = keys.next();
                        text.append(shown == 0 ? " — " : ", ").append(key).append('=').append(values.opt(key));
                    }
                }
            } else if ("hook_stage".equals(kind)) {
                text.append("Etapa ").append(details.optString("phase", ""));
            } else if ("setting_change".equals(kind)) {
                text.append("Cambio: ").append(settingName(details.optString("key", "")));
            } else if ("runtime_applied".equals(kind)) {
                text.append("Proceso de la interfaz del sistema ").append(details.optInt("pid", 0)).append(", identificador de ejecución ").append(details.optString("runtime", ""));
            }
        }
        JSONObject exception = event.optJSONObject("exception");
        JSONArray causes = exception == null ? null : exception.optJSONArray("causes");
        if (causes != null && causes.length() > 0) {
            if (text.length() > 0) text.append('\n');
            text.append("Excepción: ").append(causeLine(causes.optJSONObject(0)));
            if (causes.length() > 1) text.append("\nCausa raíz: ").append(causeLine(causes.optJSONObject(causes.length() - 1)));
        }
        return text.toString();
    }

    private static String causeLine(JSONObject cause) {
        if (cause == null) return "Desconocido";
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
        return item == null || item.title == null || item.title.isEmpty() ? key : item.title + " (" + key + ")";
    }

    /** Conclusions, most urgent first. */
    static Result analyze(List<JSONObject> events, Bundle hookHealth, boolean runtimeActive, String runtimeMessage,
                          Map<String, ?> values, JSONObject app, JSONObject packages, boolean loggingOn) {
        List<HookHealth.Finding> findings = new ArrayList<>();
        if (!runtimeActive) {
            findings.add(new HookHealth.Finding("runtime_inactive", HookHealth.Severity.ERROR, "El módulo no está activo en la interfaz del sistema",
                    (runtimeMessage == null || runtimeMessage.isEmpty() ? "" : runtimeMessage + "\n")
                            + "No aparecerá ningún efecto personalizado. Activa este módulo en LSPosed, marca «Interfaz del sistema» y reinicia la interfaz del sistema."));
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
        if (runtimeActive) findings.add(new HookHealth.Finding("runtime_live", HookHealth.Severity.OK, "La interfaz del sistema cargó la versión actual",
                "Se acaba de verificar la interfaz del sistema: está ejecutando este módulo (" + (app == null ? "" : app.optString("expected_runtime", "")) + ")."));
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
            StringBuilder detail = new StringBuilder("Hay ").append(errors).append(" errores y ").append(warnings).append(" advertencias en el registro. Los eventos más repetidos:");
            List<Map.Entry<String, int[]>> ordered = new ArrayList<>(repeated.entrySet());
            Collections.sort(ordered, new Comparator<Map.Entry<String, int[]>>() {
                @Override public int compare(Map.Entry<String, int[]> a, Map.Entry<String, int[]> b) { return Integer.compare(b.getValue()[0], a.getValue()[0]); }
            });
            for (int i = 0; i < Math.min(3, ordered.size()); i++) {
                Entry first = sample.get(ordered.get(i).getKey());
                detail.append("\n· ").append(first.sourceLabel).append(" — ").append(first.message).append(" (").append(ordered.get(i).getValue()[0]).append(" veces)");
                if (!first.detail.isEmpty()) detail.append("\n  ").append(firstLine(first.detail));
            }
            findings.add(new HookHealth.Finding("log_errors", HookHealth.Severity.WARN, errors + " registros de error", detail.toString()));
        } else if (loggingOn && !entries.isEmpty()) {
            findings.add(new HookHealth.Finding("log_clean", HookHealth.Severity.OK, "No hay errores en el registro", "Se registraron " + entries.size() + " eventos, incluidas " + warnings + " advertencias."));
        }
        if (!loggingOn) findings.add(new HookHealth.Finding("logging_off", HookHealth.Severity.INFO, "El registro de diagnóstico está desactivado",
                "El estado de los hooks no depende del registro. Para ver errores y la cronología, activa «Registro de diagnóstico», reproduce el problema y vuelve aquí."));
        else if (entries.isEmpty()) findings.add(new HookHealth.Finding("log_empty", HookHealth.Severity.INFO, "Todavía no hay eventos de registro",
                "El registro de diagnóstico está activado, pero todavía no contiene eventos. Reinicia la interfaz del sistema, prueba la función problemática y vuelve a consultar."));
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
            case "runtime_applied": return "Aplicación de la configuración en la interfaz del sistema";
            case "runtime_token": return "Versión en ejecución de la interfaz del sistema";
            case "systemui_older_than_module": return "El proceso de la interfaz del sistema es anterior al módulo";
            case "systemui_version_changed": return "La interfaz del sistema se actualizó";
            case "safe_mode": return "Modo seguro";
            case "dual_clock": return "Reloj de dos filas";
            case "clock_tick": return "Actualización de los segundos del reloj";
            default: return id;
        }
    }

    /** One readable plain-text report. */
    static String exportText(Result result, JSONObject device, JSONObject app, JSONObject packages, Bundle hookHealth,
                             Map<String, ?> values, long now) {
        StringBuilder out = new StringBuilder();
        SimpleDateFormat local = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT);
        out.append("Barra de estado C17 · Informe de diagnóstico\n");
        out.append("Fecha de generación  ").append(local.format(new Date(now))).append("  ").append(zone()).append('\n');
        out.append("Versión del módulo  ").append(app == null ? "Desconocido" : app.optString("version_name", "Desconocido"))
                .append(" (").append(app == null ? 0 : app.optLong("version_code", 0)).append(")\n");
        out.append("Identificador de ejecución  ").append(app == null ? "Desconocido" : app.optString("expected_runtime", "Desconocido")).append('\n');
        out.append("Nota      El informe omite el contenido de las notificaciones, contraseñas, nombres de red y rutas de archivo. Describe también la orientación, los pasos, la frecuencia y la velocidad del fallo; si es posible, adjunta una grabación de pantalla.\n");

        section(out, "1. Conclusiones");
        if (result.findings.isEmpty()) out.append("  No se detectaron problemas evidentes.\n");
        for (HookHealth.Finding finding : result.findings) {
            out.append("  [").append(severityLabel(finding.severity)).append("] ").append(finding.title).append('\n');
            for (String line : finding.detail.split("\n")) if (!line.isEmpty()) out.append("      ").append(line).append('\n');
        }

        section(out, "2. Dispositivo y sistema");
        if (device != null) {
            line(out, "Modelo", join(device.optString("manufacturer"), device.optString("model")));
            line(out, "Android", device.optString("android") + " (API " + device.optInt("sdk") + ")");
            line(out, "ColorOS", device.optString("coloros"));
            line(out, "Versión", device.optString("build_display"));
            line(out, "Idioma/zona horaria", device.optString("locale") + " · " + device.optString("time_zone"));
            JSONObject display = device.optJSONObject("display");
            if (display != null) line(out, "Pantalla", display.optInt("width_px") + "×" + display.optInt("height_px") + " px · densidad "
                    + display.optDouble("density") + " · escala de fuente " + display.optDouble("font_scale") + " · "
                    + ("landscape".equals(display.optString("orientation")) ? "Horizontal" : "Vertical") + (display.optBoolean("night") ? " · modo oscuro" : ""));
        }
        if (packages != null) for (Iterator<String> keys = packages.keys(); keys.hasNext();) {
            String name = keys.next();
            JSONObject info = packages.optJSONObject(name);
            if (info != null) line(out, shortPackage(name), info.optString("version_name") + " (" + info.optLong("version_code") + "), actualizado el "
                    + local.format(new Date(info.optLong("last_update_ms"))));
        }

        section(out, "3. Estado de los hooks");
        List<String[]> table = HookHealth.table(hookHealth);
        if (table.isEmpty()) out.append("  La interfaz del sistema no respondió; no se pudo obtener el estado de los hooks.\n");
        for (String[] row : table) out.append("  ").append(pad(row[1], 22)).append(pad(row[2], 8)).append(row[3]).append('\n');

        section(out, "4. Configuración guardada");
        int shown = 0;
        TreeMap<String, Object> sorted = new TreeMap<>();
        if (values != null) for (Map.Entry<String, ?> entry : values.entrySet()) if (DiagnosticReport.known(entry.getKey())) sorted.put(entry.getKey(), entry.getValue());
        for (Map.Entry<String, Object> entry : sorted.entrySet()) {
            if (shown >= 400) { out.append("  … el resto se omitió\n"); break; }
            out.append("  ").append(settingName(entry.getKey())).append(" = ").append(safeValue(entry.getKey(), entry.getValue())).append('\n');
            shown++;
        }
        if (shown == 0) out.append("  Todos los valores son predeterminados.\n");

        section(out, "5. Eventos recientes (últimos " + Math.min(EXPORT_TIMELINE, result.entries.size()) + " eventos, hora local)");
        int from = Math.max(0, result.entries.size() - EXPORT_TIMELINE);
        SimpleDateFormat clock = new SimpleDateFormat("MM-dd HH:mm:ss", Locale.ROOT);
        for (int i = from; i < result.entries.size(); i++) {
            Entry entry = result.entries.get(i);
            out.append("  ").append(clock.format(new Date(entry.time))).append("  ").append(pad(entry.levelLabel(), 6)).append(pad(entry.sourceLabel, 10))
                    .append(entry.message).append('\n');
            if (!entry.detail.isEmpty()) for (String line : entry.detail.split("\n")) out.append("      ").append(line).append('\n');
        }
        if (result.entries.isEmpty()) out.append("  No hay registros.\n");
        return out.toString();
    }

    private static String safeValue(String key, Object raw) {
        try {
            JSONObject value = DiagnosticReport.settingValue(key, raw);
            if (value == null) return "—";
            String type = value.optString("type");
            if ("text".equals(type)) return "(texto, " + value.optInt("length", 0) + " caracteres; oculto)";
            if ("font".equals(type)) return "Fuente " + value.optString("source");
            if ("priority".equals(type)) return String.valueOf(value.optJSONArray("slots"));
            return String.valueOf(value.opt("value"));
        } catch (JSONException invalid) { return "—"; }
    }

    private static void section(StringBuilder out, String title) { out.append('\n').append(title).append('\n'); }
    private static void line(StringBuilder out, String label, String value) { out.append("  ").append(pad(label, 12)).append(value).append('\n'); }
    private static String join(String a, String b) { return (a == null ? "" : a) + (b == null || b.isEmpty() ? "" : " " + b); }
    private static String shortPackage(String name) { return name.replace("com.android.", "").replace("com.oplus.", "oplus."); }
    private static String severityLabel(HookHealth.Severity severity) {
        switch (severity) { case ERROR: return "Grave"; case WARN: return "Advertencia"; case OK: return "Correcto"; default: return "Aviso"; }
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
