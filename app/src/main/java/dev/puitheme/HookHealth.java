// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.os.Bundle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Which SystemUI hook groups really installed, as SystemUI itself reported with a verified runtime probe.
 * The report carries only fixed identifiers and counts; the labels and the effect on settings live here, in the app.
 */
final class HookHealth {
    static final String KEY = "hook_health";
    /** "id=count" for every hook group, where count is the number of hooks that were created. */
    static final String GROUPS = "groups";
    /** Ids of the groups whose installer finished and was recorded as complete. */
    static final String COMPLETE = "complete";
    /** "name=on|off|failed" for each hook that is installed only while its feature is on. */
    static final String LAZY = "lazy";
    private static final int MAX_ITEMS = 64;

    enum Severity { OK, INFO, WARN, ERROR }

    static final class Finding {
        final String id, title, detail; final Severity severity;
        Finding(String id, Severity severity, String title, String detail) {
            this.id = id; this.severity = severity; this.title = title; this.detail = detail;
        }
    }

    private static final class Group {
        final String label, impact; final boolean onDemand;
        Group(String label, String impact, boolean onDemand) { this.label = label; this.impact = impact; this.onDemand = onDemand; }
    }

    private static final Map<String, Group> GROUP_INFO = new LinkedHashMap<>();
    private static final Map<String, Group> LAZY_INFO = new LinkedHashMap<>();
    private static void group(String id, String label, String impact) { GROUP_INFO.put(id, new Group(label, impact, false)); }
    private static void lazy(String id, String label, String impact) { LAZY_INFO.put(id, new Group(label, impact, true)); }
    static {
        group("context", "Entorno básico", "Todas las funciones dependen de este entorno; sin él, el módulo prácticamente no funciona.");
        group("panel_mode", "Detección del modo de panel", "Determina si el panel desplegable está combinado o separado; afecta a los ajustes del centro de notificaciones y del centro de control.");
        group("shade_wallpaper", "Fondo del panel desplegable", "Fondo del panel desplegable");
        group("model", "Estado de red y batería", "Las funciones de red y batería de la barra de estado no pueden obtener su estado.");
        group("drawable", "Dibujo de iconos de la barra de estado", "Formas, colores y opacidad de los iconos de la barra de estado.");
        group("badge", "Ocultación de indicadores", "Ocultación de los indicadores de red.");
        group("wifi", "Icono de Wi-Fi", "Tamaño, posición y estilo del icono Wi-Fi.");
        group("speed", "Velocidad de red", "Visualización y posición de la velocidad de red.");
        group("compose", "Compatibilidad de Compose con la barra de estado", "Texto, iconos y orden en la nueva barra de estado basada en Compose.");
        group("network_order", "Orden de los iconos de red", "Orden y posición de los iconos de Wi-Fi, red móvil y datos.");
        group("status_flow", "Orden y márgenes de la barra de estado", "Orden experimental de la barra de estado y espaciado lateral.");
        group("status_hints", "Iconos de aviso del sistema", "Cantidad y espaciado de los iconos de aviso, como Bluetooth y ubicación.");
        group("text", "Reloj y texto", "Reloj de la barra de estado, texto del operador y estilos del reloj y los textos de la pantalla de bloqueo.");
        group("battery", "Batería", "Icono de batería, porcentaje y estilo de carga.");
        group("data_spacing", "Separación entre datos y batería", "Separación entre los iconos de red y batería.");
        group("tiles", "Desplazamiento entre páginas de tarjetas del centro de control", "Efecto de transición entre páginas de tarjetas (instalable bajo demanda).");
        group("qs_appearance", "Color de las tarjetas", "Color de fondo, degradado y opacidad de las tarjetas del centro de control.");
        group("qs_full_fill", "Expandir las tarjetas 2×1 para llenar el área", "Apariencia de las tarjetas 2×1 a pantalla completa.");
        group("qs_corners", "Esquinas redondeadas de las tarjetas", "Radio de las esquinas de tarjetas y botones.");
        group("qs_icon_size", "Tamaño de los iconos de las tarjetas", "Tamaño de los iconos de las tarjetas del centro de control.");
        group("icon_management", "Gestión de iconos", "Iconos, colores, opacidad y paquetes de iconos animados integrados del centro de control.");
        group("qs_panel_corners", "Esquinas redondeadas del panel", "Radio de las esquinas del centro de control y del panel de notificaciones.");
        group("qs_media", "Tarjeta musical", "Fondo de la portada de la tarjeta musical del centro de control.");
        group("big_clock", "Reloj grande", "Reloj grande, fecha, componentes de texto laterales y fondo del centro de notificaciones.");
        group("notification_clear", "Botón de borrar", "Apariencia del botón para borrar notificaciones.");
        group("notification_icons", "Iconos de notificación", "Visibilidad de los iconos de notificación de la barra de estado");
        group("notification_row_icons", "Iconos de las filas de notificación", "Iconos de aplicaciones dentro de las tarjetas de notificación.");
        group("c17_highlights", "Eliminación de reflejos", "Eliminación de reflejos en notificaciones y tarjetas.");
        group("lockscreen_blur", "Desenfoque y fondo de la pantalla de bloqueo", "Desenfoque y fondo de Fluid Cloud en la pantalla de bloqueo y AOD.");
        group("fluid_accent", "Color de énfasis de Fluid Cloud", "Color de Fluid Cloud según el contenido multimedia y apariencia en la pantalla de bloqueo y AOD.");
        group("fluid_media", "Tarjeta musical de Fluid Cloud", "Dibujo de la tarjeta musical de Fluid Cloud (instalable bajo demanda).");
        group("fluid_notification", "Estilo de Fluid Cloud", "Estilo de Fluid Cloud y sincronización con las notificaciones.");
        group("mini_stack", "Apilado pequeño por aplicación", "Apilado compacto de notificaciones de una misma aplicación.");

        lazy("statusbar_flow_layout", "Distribución del orden de la barra de estado", "Orden experimental de la barra de estado y espaciado lateral.");
        lazy("status_hint_child_index", "Distribución de los iconos de aviso", "Cantidad, espaciado y orden de los iconos de aviso del sistema.");
        lazy("network_order_child_index", "Orden de los iconos de red", "Orden de los iconos de Wi-Fi, red móvil y datos");
        lazy("network_shared_native_placement", "Posición compartida de los iconos de red", "Posición compartida de los iconos de red");
        lazy("shade_wallpaper", "Dibujo del fondo del panel desplegable", "Fondo del panel desplegable");
        lazy("qs_media_child", "Vista secundaria de la tarjeta musical", "Fondo de la portada de la tarjeta musical del centro de control.");
        lazy("notification_mini_stack", "Apilado pequeño por aplicación", "Apilado compacto de notificaciones de una misma aplicación.");
        lazy("lock_hide", "Ocultación de elementos de la pantalla de bloqueo", "Oculta la hora, la fecha y otros elementos de aviso de la pantalla de bloqueo.");
        lazy("device_gradient", "Degradado de las tarjetas", "Degradado nativo de Fluid Cloud.");
        lazy("capsule_bounce", "Rebote al aparecer la cápsula", "Animación de rebote al aparecer la cápsula de Fluid Cloud.");
    }

    private HookHealth() { }

    static String label(String id) {
        Group group = GROUP_INFO.get(id);
        if (group == null) group = LAZY_INFO.get(id);
        return group == null ? id : group.label;
    }

    static boolean knownGroup(String id) { return GROUP_INFO.containsKey(id); }
    static List<String> groupIds() { return Collections.unmodifiableList(new ArrayList<>(GROUP_INFO.keySet())); }

    /** Typed, bounded copy of a report; anything unexpected is dropped. */
    static Bundle copy(Bundle source) {
        if (source == null) return null;
        Bundle copy = new Bundle();
        copy.putStringArrayList(GROUPS, entries(source.getStringArrayList(GROUPS), "[a-z][a-z0-9_]{0,31}=[0-9]{1,5}"));
        copy.putStringArrayList(COMPLETE, entries(source.getStringArrayList(COMPLETE), "[a-z][a-z0-9_]{0,31}"));
        copy.putStringArrayList(LAZY, entries(source.getStringArrayList(LAZY), "[a-z][a-z0-9_]{0,47}=(on|off|failed)"));
        return copy;
    }

    private static ArrayList<String> entries(List<String> raw, String pattern) {
        ArrayList<String> result = new ArrayList<>();
        if (raw != null) for (String value : raw) {
            if (result.size() >= MAX_ITEMS) break;
            if (value != null && value.matches(pattern)) result.add(value);
        }
        return result;
    }

    /** Each group's hook count, 0 when it never installed anything. */
    static Map<String, Integer> counts(Bundle report) {
        Map<String, Integer> result = new LinkedHashMap<>();
        if (report == null) return result;
        List<String> raw = report.getStringArrayList(GROUPS);
        if (raw != null) for (String entry : raw) {
            int split = entry.indexOf('=');
            if (split <= 0) continue;
            try { result.put(entry.substring(0, split), Integer.parseInt(entry.substring(split + 1))); }
            catch (NumberFormatException ignored) { }
        }
        return result;
    }

    static Map<String, String> lazyStates(Bundle report) {
        Map<String, String> result = new LinkedHashMap<>();
        if (report == null) return result;
        List<String> raw = report.getStringArrayList(LAZY);
        if (raw != null) for (String entry : raw) {
            int split = entry.indexOf('=');
            if (split > 0) result.put(entry.substring(0, split), entry.substring(split + 1));
        }
        return result;
    }

    /** Plain-language findings. {@code report} null: SystemUI did not answer, so nothing can be said about hooks. */
    static List<Finding> findings(Bundle report) {
        List<Finding> result = new ArrayList<>();
        if (report == null) {
            result.add(new Finding("hooks_unknown", Severity.INFO, "Todavía no se recibió el estado de los hooks de la interfaz del sistema.",
                    "El módulo no está activo en la interfaz del sistema o esta todavía no respondió. Comprueba en Inicio que la versión actual esté aplicada y vuelve a actualizar."));
            return result;
        }
        Map<String, Integer> counts = counts(report);
        List<String> complete = report.getStringArrayList(COMPLETE);
        List<String> broken = new ArrayList<>(), ready = new ArrayList<>();
        int hooks = 0;
        for (String id : GROUP_INFO.keySet()) {
            Group info = GROUP_INFO.get(id);
            int count = counts.containsKey(id) ? counts.get(id) : 0;
            hooks += count;
            boolean done = count > 0 || (complete != null && complete.contains(id));
            if (done) ready.add(id);
            else if (!info.onDemand && !"tiles".equals(id) && !"fluid_media".equals(id)) broken.add(id);
        }
        if (broken.isEmpty()) {
            result.add(new Finding("hooks_ok", Severity.OK, "Todos los hooks están listos",
                    ready.size() + " grupos de funciones instalados; total de hooks: " + hooks + " hooks."));
        } else {
            boolean base = broken.contains("context");
            StringBuilder detail = new StringBuilder("No se instalaron los hooks de estas funciones; sus ajustes no tendrán efecto:");
            for (String id : broken) detail.append("\n· ").append(label(id)).append("：").append(GROUP_INFO.get(id).impact);
            detail.append("\nLas causas habituales son cambios en los nombres de clases internas tras una actualización de SystemUI o la ausencia de esa estructura en este dispositivo. Activa el registro de diagnóstico, reinicia SystemUI y envía el informe exportado al autor.");
            result.add(new Finding("hooks_missing", base ? Severity.ERROR : Severity.WARN,
                    broken.size() + " grupos de funciones sin hooks listos", detail.toString()));
        }
        Map<String, String> lazy = lazyStates(report);
        List<String> failed = new ArrayList<>();
        for (Map.Entry<String, String> entry : lazy.entrySet()) if ("failed".equals(entry.getValue())) failed.add(entry.getKey());
        if (!failed.isEmpty()) {
            StringBuilder detail = new StringBuilder("No se pudieron instalar estos hooks bajo demanda cuando se necesitaron:");
            for (String name : failed) detail.append("\n· ").append(label(name)).append("：").append(effect(name));
            result.add(new Finding("lazy_failed", Severity.WARN, failed.size() + " hooks bajo demanda no se pudieron instalar", detail.toString()));
        }
        return result;
    }

    private static String effect(String lazyName) {
        Group group = LAZY_INFO.get(lazyName);
        return group == null ? "La función correspondiente podría no funcionar." : group.impact;
    }

    /** Every group in a stable order with its state, for the detail list. */
    static List<String[]> table(Bundle report) {
        List<String[]> rows = new ArrayList<>();
        if (report == null) return rows;
        Map<String, Integer> counts = counts(report);
        List<String> complete = report.getStringArrayList(COMPLETE);
        for (String id : GROUP_INFO.keySet()) {
            int count = counts.containsKey(id) ? counts.get(id) : 0;
            boolean done = count > 0 || (complete != null && complete.contains(id));
            rows.add(new String[]{id, GROUP_INFO.get(id).label, done ? "Listo" : GROUP_INFO.get(id).onDemand || "tiles".equals(id) || "fluid_media".equals(id) ? "Bajo demanda" : "No listo",
                    count > 0 ? count + " hooks" : ""});
        }
        for (Map.Entry<String, String> entry : lazyStates(report).entrySet())
            rows.add(new String[]{entry.getKey(), label(entry.getKey()), "on".equals(entry.getValue()) ? "Instalado" : "failed".equals(entry.getValue()) ? "Fallido" : "No activado", "Bajo demanda"});
        return rows;
    }

    static String toId(Enum<?> constant) { return constant.name().toLowerCase(Locale.ROOT); }
}
