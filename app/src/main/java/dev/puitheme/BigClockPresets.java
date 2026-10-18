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
        {"（天气）", "天气 · 系统默认"},
        {"（天气:{图标} {温度}）", "天气 · 图标与温度"},
        {"（天气:{温度}{图标}）", "天气 · 温度与图标"},
        {"（天气:{描述} {温度}）", "天气 · 文字与温度（无图标）"},
        {"（天气:{描述:英文} {温度:数字}°C）", "天气 · English 与摄氏数字"},
        {"（天气:{夜?🌙|☀️} {温度}）", "天气 · 昼夜符号与温度"},
        {"（天气:{有数据?{描述}{温度? ·{温度}}|暂无天气}）", "天气 · 无数据时的自定义文字"},
        {"（天气:{描述:中文}）", "仅天气文字 · 中文"},
        {"（天气:{描述:英文}）", "仅天气文字 · English"},
        {"（天气:{温度}）", "仅温度"},
        {"（天气:{图标:原色}）", "仅图标 · 原色"},
        {"（天气原色）", "天气 · 系统原色图标"},
    };

    private static final String[][] DATE_TIME = {
        {"（日期）", "月日"},
        {"（日期） （周）", "月日与星期"},
        {"（年月日） （星期）", "完整日期与星期"},
        {"（日期） （农历日期）", "月日与农历"},
        {"（农历）", "农历"},
        {"（干支） （农历日期）", "干支年与农历"},
        {"（时间）", "时分 · 24 小时"},
        {"（上午下午） （12小时）", "时分 · 12 小时"},
        {"（时段） （时间）", "时段与时间"},
        {"（日期英文） （星期英文简）", "English 日期"},
        {"（星期英文）", "English 星期"},
        {"（天气） （日期）", "天气与日期"},
        {"（日期） （天气）", "日期与天气"},
        {"（格式:M月d日 {周} {时段}）", "自定义时间格式示例"},
    };

    private static final String[][] BATTERY = {
        {"（电池:{图标} {电量}%）", "电池 · 图标与电量"},
        {"（电池:{电量}%{充电?⚡}）", "电池 · 电量与充电闪电"},
        {"（电池:{电量}%{状态? {状态}}）", "电池 · 电量与充电状态文字"},
        {"（电池:{电量<=20?🪫|🔋} {电量}%）", "电池 · 低电量换符号"},
        {"（电池:{条:5:█:░} {电量}%）", "电池 · 电量条"},
        {"（电池:{图标}）", "仅电池图标"},
        {"（电池）", "电池电量（含图标）"},
    };

    private static final String[][] SIGNAL = {
        {"（信号:{图标}）", "信号 · 仅图标"},
        {"（信号:{条:4:●:○}）", "信号 · 圆点格数"},
        {"（信号:{无服务?无服务|{格数}/{总格}格}）", "信号 · 格数，无服务时提示"},
        {"（信号:{图标} {状态}）", "信号 · 图标与特殊状态"},
        {"（蜂窝信号）", "蜂窝信号（含图标）"},
    };

    private static final String[][] NETWORK = {
        {"（网络:{图标} {类型}）", "网络 · 图标与类型"},
        {"（网络:{类型}{受限?'（受限）'}）", "网络 · 类型，受限时标注"},
        {"（网络:{制式?{制式}|{类型}}）", "网络 · 蜂窝优先显示 5G / 4G"},
        {"（网络:{WiFi?Wi-Fi {条:4:●:○}|{类型}}）", "网络 · Wi-Fi 信号点"},
        {"（网络:{图标}）", "仅网络图标"},
        {"（网络状态）", "网络状态（含图标）"},
    };

    private static final String[][] COMBINED = {
        {"（电池:{电量}%） · （网络:{类型}）", "电量 · 网络"},
        {"（信号:{图标}） （网络:{图标}） （电池:{图标}）", "状态图标三件套"},
        {"信号（蜂窝信号） 网络（网络状态） 电量（电池）", "信号、网络与电量（文字说明）"},
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
        if (error != null) return "格式有误：" + error;
        return BigClockTextContent.resolvePreview(input, now, java.util.TimeZone.getDefault(), sampleWeather(), sampleSystem());
    }
}
