// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import java.util.ArrayList;
import java.util.List;

/** Ready-made contents ("预设") for the left/right text components, offered the
 * same way as the time formats: one tap fills the field, the preview and
 * the check still apply. Every value is plain text built from tokens that
 * {@link BigClockTextContent} understands; the 类别:模板 ones show how the format is written. */
final class BigClockPresets {
    private BigClockPresets() { }

    private static final String[][] WEATHER = {
        {"（天气）", "Clima · predeterminado del sistema"},
        {"（天气:{图标} {温度}）", "Clima · icono y temperatura"},
        {"（天气:{温度}{图标}）", "Clima · temperatura e icono"},
        {"（天气:{描述} {温度}）", "Clima · texto y temperatura (sin icono)"},
        {"（天气:{描述:英文} {温度:数字}°C）", "Clima · texto en inglés y temperatura en °C"},
        {"（天气:{夜?🌙|☀️} {温度}）", "Clima · símbolo de día/noche y temperatura"},
        {"（天气:{有数据?{描述}{温度? ·{温度}}|暂无天气}）", "Clima · texto personalizado si no hay datos"},
        {"（天气:{描述:中文}）", "Solo texto del clima · chino"},
        {"（天气:{描述:英文}）", "Solo texto del clima · inglés"},
        {"（天气:{温度}）", "Solo temperatura"},
        {"（天气:{图标:原色}）", "Solo icono · color original"},
        {"（天气原色）", "Clima · icono del sistema con color original"},
    };

    private static final String[][] DATE_TIME = {
        {"（日期）", "Día y mes"},
        {"（日期） （周）", "Día, mes y día de la semana"},
        {"（年月日） （星期）", "Fecha completa y día de la semana"},
        {"（日期） （农历日期）", "Día y mes con fecha lunar"},
        {"（农历）", "Calendario lunar"},
        {"（干支） （农历日期）", "Año sexagenario y fecha lunar"},
        {"（时间）", "Hora y minutos · 24 horas"},
        {"（上午下午） （12小时）", "Hora · formato de 12 horas"},
        {"（时段） （时间）", "Franja horaria y hora"},
        {"（日期英文） （星期英文简）", "Fecha en inglés"},
        {"（星期英文）", "Día de la semana en inglés"},
        {"（天气） （日期）", "Clima y fecha"},
        {"（日期） （天气）", "Fecha y clima"},
        {"（格式:M月d日 {周} {时段}）", "Ejemplo de formato horario personalizado"},
    };

    private static final String[][] BATTERY = {
        {"（电池:{图标} {电量}%）", "Batería · icono y porcentaje"},
        {"（电池:{电量}%{充电?⚡}）", "Batería · porcentaje y rayo de carga"},
        {"（电池:{电量}%{状态? {状态}}）", "Batería · porcentaje y texto de carga"},
        {"（电池:{电量<=20?🪫|🔋} {电量}%）", "Batería · icono para nivel bajo"},
        {"（电池:{条:5:█:░} {电量}%）", "Batería · barra de nivel"},
        {"（电池:{图标}）", "Solo icono de batería"},
        {"（电池）", "Nivel de batería (con icono)"},
    };

    private static final String[][] SIGNAL = {
        {"（信号:{图标}）", "Señal · solo icono"},
        {"（信号:{条:4:●:○}）", "Señal · niveles con puntos"},
        {"（信号:{无服务?无服务|{格数}/{总格}格}）", "Señal · niveles y aviso sin servicio"},
        {"（信号:{图标} {状态}）", "Señal · icono y estado especial"},
        {"（蜂窝信号）", "Señal móvil (con icono)"},
    };

    private static final String[][] NETWORK = {
        {"（网络:{图标} {类型}）", "Red · icono y tipo"},
        {"（网络:{类型}{受限?'（受限）'}）", "Red · tipo e indicación de restricción"},
        {"（网络:{制式?{制式}|{类型}}）", "Red móvil · priorizar 5G / 4G"},
        {"（网络:{WiFi?Wi-Fi {条:4:●:○}|{类型}}）", "Red · niveles de señal Wi-Fi"},
        {"（网络:{图标}）", "Solo icono de red"},
        {"（网络状态）", "Estado de red (con icono)"},
    };

    private static final String[][] COMBINED = {
        {"（电池:{电量}%） · （网络:{类型}）", "Batería · red"},
        {"（信号:{图标}） （网络:{图标}） （电池:{图标}）", "Trío de iconos de estado"},
        {"信号（蜂窝信号） 网络（网络状态） 电量（电池）", "Señal, red y batería (descripción de texto)"},
    };

    static String[][] entries(boolean right) {
        List<String[]> all = new ArrayList<>();
        for (String[][] group : new String[][][]{WEATHER, DATE_TIME, BATTERY, SIGNAL, NETWORK, COMBINED})
            for (String[] item : group) all.add(item);
        return all.toArray(new String[0][]);
    }

    static String[] values(boolean right) { return column(entries(right), 0); }

    static String[] labels(boolean right) { return column(entries(right), 1); }

    private static String[] column(String[][] rows, int index) {
        String[] result = new String[rows.length];
        for (int i = 0; i < rows.length; i++) result[i] = rows[i][index];
        return result;
    }

    /** The example the settings app shows: it has no lock-screen weather or radio state of its own. */
    static SystemWeatherText.Snapshot sampleWeather() {
        SystemWeatherText.Snapshot weather = SystemWeatherText.snapshot();
        return weather.description.isEmpty() ? SystemWeatherText.sample("多云", "23°", WeatherTextIcon.Kind.PARTLY_CLOUDY) : weather;
    }

    static BigClockSystemText.Snapshot sampleSystem() {
        return BigClockSystemText.sample(85, BigClockSystemText.Charging.CHARGING, 3, BigClockSystemText.Transport.WIFI, Boolean.TRUE,
                3, "5G", Boolean.FALSE, Boolean.FALSE, Boolean.TRUE);
    }

    /** Live result for the editor: the text for the current time, with example weather and example radio/battery state
     * and sketched icons where the status row draws native ones. The reason is shown instead when the text cannot be saved. */
    static CharSequence preview(String input, long now) {
        if (input == null || input.isEmpty()) return "";
        String error = BigClockWidgetsSettings.textValidationError(input);
        if (error != null) return "Formato no válido: " + error;
        return BigClockTextContent.resolvePreview(input, now, java.util.TimeZone.getDefault(), sampleWeather(), sampleSystem());
    }
}
