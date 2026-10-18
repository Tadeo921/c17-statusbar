// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie

package dev.puitheme;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** The fields each template category offers, read from one immutable weather / system snapshot. */
final class ComponentFields {
    private static final List<String> NO_ARGS = Collections.emptyList();
    private ComponentFields() { }

    /** How an {图标} field may be drawn: not at all (plain text), from the native SystemUI images, or as the settings app's sketch. */
    static final int PLAIN = 0, NATIVE = 1, PREVIEW = 2;

    /** Fields of a category bit, or null for an unknown category. A null snapshot reads as the current one. */
    static ComponentFormat.Fields of(int category, SystemWeatherText.Snapshot weather, BigClockSystemText.Snapshot system, int icons) {
        switch (category) {
            case BigClockTextContent.WEATHER: return new Weather(weather == null ? SystemWeatherText.snapshot() : weather, icons);
            case BigClockTextContent.BATTERY: return new Battery(system == null ? BigClockSystemText.snapshot() : system, icons);
            case BigClockTextContent.CELLULAR: return new Signal(system == null ? BigClockSystemText.snapshot() : system, icons);
            case BigClockTextContent.NETWORK: return new Network(system == null ? BigClockSystemText.snapshot() : system, icons);
            default: return null;
        }
    }

    // ---- shared helpers ----

    private static String choice(String field, List<String> args, String... allowed) {
        if (args.isEmpty()) return null;
        if (args.size() > 1) return "{" + field + "} 只能带一个参数";
        for (String option : allowed) if (option.equals(args.get(0))) return null;
        return "{" + field + "} 不支持参数“" + args.get(0) + "”，可用：" + String.join("、", allowed);
    }

    private static String none(String field, List<String> args) {
        return args.isEmpty() ? null : "{" + field + "} 不需要参数";
    }

    private static boolean english(List<String> args) { return !args.isEmpty() && "英文".equals(args.get(0)); }

    /** Zero padding written as 0, 00, 000 ... */
    private static String paddingError(String field, List<String> args) {
        if (args.isEmpty()) return null;
        String pad = args.get(0);
        boolean zeros = args.size() == 1 && !pad.isEmpty() && pad.length() <= 6;
        for (int i = 0; zeros && i < pad.length(); i++) zeros = pad.charAt(i) == '0';
        return zeros ? null : "{" + field + "} 的参数是补零位数，例如 {" + field + ":000}";
    }

    private static String padded(int value, List<String> args) {
        String digits = Integer.toString(value);
        if (args.isEmpty()) return digits;
        StringBuilder out = new StringBuilder();
        for (int i = digits.length(); i < args.get(0).length(); i++) out.append('0');
        return out.append(digits).toString();
    }

    /** {条:总格数:实心:空心}: the first argument is the cell count, the next two are the symbols you choose. */
    private static String barError(String field, List<String> args, int minimum, int maximum) {
        if (args.size() > 3) return "{" + field + "} 最多三个参数：格数、实心符号、空心符号";
        if (args.isEmpty()) return null;
        try {
            int cells = Integer.parseInt(args.get(0));
            if (cells < minimum || cells > maximum) return "{" + field + "} 的格数应为 " + minimum + " 到 " + maximum;
        } catch (NumberFormatException invalid) { return "{" + field + "} 的第一个参数是格数（数字），例如 {" + field + ":5:●:○}"; }
        return null;
    }

    private static String bar(List<String> args, String defaultFull, String defaultEmpty, int filled, int cells) {
        String full = args.size() > 1 && !args.get(1).isEmpty() ? args.get(1) : defaultFull;
        String empty = args.size() > 2 ? args.get(2) : defaultEmpty;
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < cells; i++) out.append(i < filled ? full : empty);
        return out.toString();
    }

    private static int cells(List<String> args, int defaultCells) {
        if (args.isEmpty()) return defaultCells;
        try { return Integer.parseInt(args.get(0)); } catch (NumberFormatException invalid) { return defaultCells; }
    }

    // ---- 天气 ----

    private static final class Weather implements ComponentFormat.Fields {
        private static final List<String> NAMES = Arrays.asList("描述", "温度", "图标", "夜", "昼", "有数据", "有温度");
        private final SystemWeatherText.Snapshot w; private final int icons;
        Weather(SystemWeatherText.Snapshot weather, int icons) { w = weather; this.icons = icons; }
        @Override public String category() { return "天气"; }
        @Override public List<String> names() { return NAMES; }
        @Override public int kind(String name) {
            switch (name) {
                case "描述": return ComponentFormat.TEXT;
                case "温度": return ComponentFormat.NUMBER;
                case "图标": return ComponentFormat.ICON;
                case "夜": case "昼": case "有数据": case "有温度": return ComponentFormat.BOOL;
                default: return 0;
            }
        }
        @Override public String argError(String name, List<String> args) {
            switch (name) {
                case "描述": return choice(name, args, "中文", "英文", "系统");
                case "温度": return choice(name, args, "数字", "单位");
                case "图标": return choice(name, args, "原色");
                default: return none(name, args);
            }
        }
        private boolean night() {
            return w.daylight == 0 || w.daylight < 0 && (w.weatherIconKind == WeatherTextIcon.Kind.CLEAR_NIGHT || w.weatherIconKind == WeatherTextIcon.Kind.CLOUDY_NIGHT);
        }
        /** Length of the leading signed integer of "23°", "-5°C" or "−5°". */
        private int digits() {
            String t = w.temperature; int i = 0;
            if (i < t.length() && (t.charAt(i) == '-' || t.charAt(i) == '−' || t.charAt(i) == '+')) i++;
            int begin = i;
            while (i < t.length() && t.charAt(i) >= '0' && t.charAt(i) <= '9') i++;
            return i > begin ? i : 0;
        }
        @Override public String text(String name, List<String> args) {
            if ("描述".equals(name)) {
                if (w.description.isEmpty()) return "";
                int language = english(args) ? WeatherLabels.ENGLISH : !args.isEmpty() && "中文".equals(args.get(0)) ? WeatherLabels.CHINESE : WeatherLabels.SYSTEM;
                return WeatherLabels.description(w, language);
            }
            String t = w.temperature; int split = digits();
            if (args.isEmpty()) return t;
            return "数字".equals(args.get(0)) ? t.substring(0, split) : t.substring(split);
        }
        @Override public double number(String name) {
            int split = digits();
            return split == 0 ? Double.NaN : Double.parseDouble(w.temperature.substring(0, split).replace('−', '-'));
        }
        @Override public boolean truth(String name) {
            switch (name) {
                case "夜": return night();
                case "昼": return w.daylight == 1 || w.daylight < 0 && w.weatherIconKind != WeatherTextIcon.Kind.UNKNOWN && !night();
                case "有数据": return !w.description.isEmpty();
                case "有温度": return !w.temperature.isEmpty();
                default: return icons == PREVIEW || icons == NATIVE && w.hasIcon();
            }
        }
        @Override public boolean hasData() { return !w.description.isEmpty(); }
        @Override public String unavailable() { return SystemWeatherText.UNAVAILABLE; }
    }

    // ---- 电池 ----

    private static final class Battery implements ComponentFormat.Fields {
        private static final List<String> NAMES = Arrays.asList("电量", "状态", "条", "图标", "充电", "满电", "未充电", "放电", "状态未知", "低电", "有数据");
        private final BigClockSystemText.Snapshot s; private final int icons;
        Battery(BigClockSystemText.Snapshot system, int icons) { s = system; this.icons = icons; }
        @Override public String category() { return "电池"; }
        @Override public List<String> names() { return NAMES; }
        @Override public int kind(String name) {
            switch (name) {
                case "电量": return ComponentFormat.NUMBER;
                case "状态": case "条": return ComponentFormat.TEXT;
                case "图标": return ComponentFormat.ICON;
                case "充电": case "满电": case "未充电": case "放电": case "状态未知": case "低电": case "有数据": return ComponentFormat.BOOL;
                default: return 0;
            }
        }
        @Override public String argError(String name, List<String> args) {
            switch (name) {
                case "电量": return paddingError(name, args);
                case "状态": return choice(name, args, "中文", "英文");
                case "条": return barError(name, args, 1, 20);
                default: return none(name, args);
            }
        }
        @Override public String text(String name, List<String> args) {
            if ("电量".equals(name)) return s.batteryPercent < 0 ? "" : padded(s.batteryPercent, args);
            if ("条".equals(name)) {
                if (s.batteryPercent < 0) return "";
                int cells = cells(args, 10);
                return bar(args, "█", "░", Math.round(s.batteryPercent * cells / 100f), cells);
            }
            boolean en = english(args);
            switch (s.charging) {
                case CHARGING: return en ? "Charging" : "充电中";
                case FULL: return en ? "Full" : "已充满";
                case NOT_CHARGING: return en ? "Not charging" : "未充电";
                case UNKNOWN: return s.batteryPercent < 0 ? "" : en ? "Status unknown" : "充电状态未知";
                default: return "";
            }
        }
        @Override public double number(String name) { return s.batteryPercent < 0 ? Double.NaN : s.batteryPercent; }
        @Override public boolean truth(String name) {
            switch (name) {
                case "充电": return s.charging == BigClockSystemText.Charging.CHARGING;
                case "满电": return s.charging == BigClockSystemText.Charging.FULL;
                case "未充电": return s.charging == BigClockSystemText.Charging.NOT_CHARGING;
                case "放电": return s.charging == BigClockSystemText.Charging.DISCHARGING;
                case "状态未知": return s.batteryPercent >= 0 && s.charging == BigClockSystemText.Charging.UNKNOWN;
                case "低电": return s.batteryPercent >= 0 && s.batteryPercent <= 20;
                case "有数据": return s.batteryPercent >= 0;
                default: return icons != PLAIN;
            }
        }
        @Override public boolean hasData() { return s.batteryPercent >= 0; }
        @Override public String unavailable() { return BigClockSystemText.BATTERY_UNAVAILABLE; }
    }

    // ---- 信号 ----

    private static final class Signal implements ComponentFormat.Fields {
        private static final List<String> NAMES = Arrays.asList("格数", "总格", "状态", "条", "图标", "飞行", "无卡", "无服务", "有信号", "有数据");
        private final BigClockSystemText.Snapshot s; private final int icons;
        Signal(BigClockSystemText.Snapshot system, int icons) { s = system; this.icons = icons; }
        @Override public String category() { return "信号"; }
        @Override public List<String> names() { return NAMES; }
        @Override public int kind(String name) {
            switch (name) {
                case "格数": case "总格": return ComponentFormat.NUMBER;
                case "状态": case "条": return ComponentFormat.TEXT;
                case "图标": return ComponentFormat.ICON;
                case "飞行": case "无卡": case "无服务": case "有信号": case "有数据": return ComponentFormat.BOOL;
                default: return 0;
            }
        }
        @Override public String argError(String name, List<String> args) {
            switch (name) {
                case "状态": return choice(name, args, "中文", "英文");
                case "条": return barError(name, args, 1, 8);
                default: return none(name, args);
            }
        }
        private boolean airplane() { return Boolean.TRUE.equals(s.airplaneMode); }
        private boolean absent() { return !airplane() && Boolean.TRUE.equals(s.simAbsent); }
        private boolean noService() { return !airplane() && !absent() && Boolean.FALSE.equals(s.inService); }
        @Override public String text(String name, List<String> args) {
            if ("格数".equals(name)) return s.signalLevel < 0 ? "" : Integer.toString(s.signalLevel);
            if ("总格".equals(name)) return "4";
            if ("条".equals(name)) {
                if (s.signalLevel < 0) return "";
                int cells = cells(args, 4);
                return bar(args, "●", "○", Math.round(s.signalLevel * cells / 4f), cells);
            }
            boolean en = english(args);
            if (airplane()) return en ? "Airplane mode" : "飞行模式";
            if (absent()) return en ? "No SIM" : "无SIM卡";
            if (noService()) return en ? "No service" : "无服务";
            return "";
        }
        @Override public double number(String name) {
            if ("总格".equals(name)) return 4;
            return s.signalLevel < 0 ? Double.NaN : s.signalLevel;
        }
        @Override public boolean truth(String name) {
            switch (name) {
                case "飞行": return airplane();
                case "无卡": return absent();
                case "无服务": return noService();
                case "有信号": return s.signalLevel >= 0;
                case "有数据": return hasData();
                default: return icons != PLAIN;
            }
        }
        @Override public boolean hasData() { return !BigClockSystemText.SIGNAL_UNAVAILABLE.equals(s.cellularSignal); }
        @Override public String unavailable() { return BigClockSystemText.SIGNAL_UNAVAILABLE; }
    }

    // ---- 网络 ----

    private static final class Network implements ComponentFormat.Fields {
        private static final List<String> NAMES = Arrays.asList("类型", "制式", "状态", "WiFi格数", "条", "图标",
                "联网", "WiFi", "蜂窝", "有线", "离线", "受限", "有数据");
        private final BigClockSystemText.Snapshot s; private final int icons;
        Network(BigClockSystemText.Snapshot system, int icons) { s = system; this.icons = icons; }
        @Override public String category() { return "网络"; }
        @Override public List<String> names() { return NAMES; }
        @Override public int kind(String name) {
            switch (name) {
                case "类型": case "制式": case "状态": case "条": return ComponentFormat.TEXT;
                case "WiFi格数": return ComponentFormat.NUMBER;
                case "图标": return ComponentFormat.ICON;
                case "联网": case "WiFi": case "蜂窝": case "有线": case "离线": case "受限": case "有数据": return ComponentFormat.BOOL;
                default: return 0;
            }
        }
        @Override public String argError(String name, List<String> args) {
            switch (name) {
                case "类型": case "状态": return choice(name, args, "中文", "英文");
                case "条": return barError(name, args, 1, 8);
                default: return none(name, args);
            }
        }
        private boolean connected() {
            switch (s.transport) {
                case WIFI: case CELLULAR: case ETHERNET: case OTHER: return true;
                default: return false;
            }
        }
        private int wifi() { return s.transport == BigClockSystemText.Transport.WIFI ? s.wifiLevel : -1; }
        @Override public String text(String name, List<String> args) {
            boolean en = english(args);
            switch (name) {
                case "WiFi格数": return wifi() < 0 ? "" : Integer.toString(wifi());
                case "制式": return s.transport == BigClockSystemText.Transport.CELLULAR ? s.dataGeneration : "";
                case "条": {
                    if (wifi() < 0) return "";
                    int cells = cells(args, 4);
                    return bar(args, "●", "○", Math.round(wifi() * cells / 4f), cells);
                }
                case "状态":
                    if (!connected()) return "";
                    if (Boolean.FALSE.equals(s.validated)) return en ? "Limited" : "受限";
                    return s.validated == null ? (en ? "Status unknown" : "联网状态未知") : "";
                default:
                    switch (s.transport) {
                        case WIFI: return "Wi-Fi";
                        case CELLULAR: return s.dataGeneration.isEmpty() ? (en ? "Cellular" : "蜂窝网络") : s.dataGeneration;
                        case ETHERNET: return en ? "Ethernet" : "以太网";
                        case OTHER: return en ? "Other network" : "其他网络";
                        case NONE: return en ? "Offline" : "未联网";
                        default: return "";
                    }
            }
        }
        @Override public double number(String name) { return wifi() < 0 ? Double.NaN : wifi(); }
        @Override public boolean truth(String name) {
            switch (name) {
                case "联网": return connected();
                case "WiFi": return s.transport == BigClockSystemText.Transport.WIFI;
                case "蜂窝": return s.transport == BigClockSystemText.Transport.CELLULAR;
                case "有线": return s.transport == BigClockSystemText.Transport.ETHERNET;
                case "离线": return s.transport == BigClockSystemText.Transport.NONE;
                case "受限": return connected() && Boolean.FALSE.equals(s.validated);
                case "有数据": return hasData();
                default: return icons != PLAIN;
            }
        }
        @Override public boolean hasData() { return s.transport != BigClockSystemText.Transport.UNKNOWN; }
        @Override public String unavailable() { return BigClockSystemText.NETWORK_UNAVAILABLE; }
    }
}
