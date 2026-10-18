package dev.puitheme;

import android.graphics.Canvas;
import android.graphics.Paint;
import java.util.Locale;
import java.util.TimeZone;

/** The (类别:模板) format of the left/right components: syntax, fields, conditions, errors, icons and old-token compatibility. */
public final class ComponentFormatCheck {
    private static int checks;
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    private static final long NOW = instant();
    private static long instant() {
        java.util.Calendar time = new java.util.GregorianCalendar(UTC, Locale.SIMPLIFIED_CHINESE);
        time.clear(); time.set(2026, java.util.Calendar.OCTOBER, 16, 15, 5, 0);
        return time.getTimeInMillis();
    }

    private static void check(boolean value, String why) { checks++; if (!value) throw new AssertionError(why); }
    private static void same(String expected, String actual, String why) { checks++; if (!expected.equals(actual)) throw new AssertionError(why + ": expected <" + expected + "> but got <" + actual + ">"); }

    private static SystemWeatherText.Snapshot weather(String words, String temperature, WeatherTextIcon.Kind kind) { return SystemWeatherText.sample(words, temperature, kind); }
    private static BigClockSystemText.Snapshot system(int battery, BigClockSystemText.Charging charging, int signal, BigClockSystemText.Transport route,
            Boolean validated, int wifi, String generation) {
        return BigClockSystemText.sample(battery, charging, signal, route, validated, wifi, generation, Boolean.FALSE, Boolean.FALSE, Boolean.TRUE);
    }
    private static final SystemWeatherText.Snapshot CLOUDY = weather("多云", "23°", WeatherTextIcon.Kind.PARTLY_CLOUDY);
    private static final BigClockSystemText.Snapshot WIFI = system(85, BigClockSystemText.Charging.CHARGING, 3, BigClockSystemText.Transport.WIFI, Boolean.TRUE, 3, "5G");

    private static String render(String template, SystemWeatherText.Snapshot weather, BigClockSystemText.Snapshot system) {
        return BigClockTextContent.resolveStyled(template, NOW, UTC, weather, system, null).toString().replace("￼", "<icon>");
    }
    private static String render(String template, BigClockSystemText.Snapshot system) { return render(template, CLOUDY, system); }
    private static String render(String template) { return render(template, CLOUDY, WIFI); }

    private static void weatherFields() {
        same("多云 23°", render("（天气:{描述} {温度}）"), "description and temperature");
        same("Partly cloudy 23°C", render("（天气:{描述:英文} {温度:数字}°C）"), "english words, number only, the user's own unit");
        same("°", render("（天气:{温度:单位}）"), "unit alone");
        same("多云", render("（天气:{描述:中文}）"), "chinese words");
        same("D", render("（天气:{夜?N|D}）"), "a day icon is not night");
        same("N", render("（天气:{夜?N|D}）", weather("晴", "12°", WeatherTextIcon.Kind.CLEAR_NIGHT), WIFI), "a night icon is night");
        same("暖", render("（天气:{温度<20?冷|{温度>=30?热|暖}}）"), "a condition nested in the else part");
        same("冷", render("（天气:{温度<20?冷|{温度>=30?热|暖}}）", weather("小雨", "-5°", WeatherTextIcon.Kind.LIGHT_RAIN), WIFI), "negative temperature compares as a number");
        same("冷", render("（天气:{温度<0?冷|暖}）", weather("小雪", "−3°C", WeatherTextIcon.Kind.LIGHT_SNOW), WIFI), "typographic minus compares as a number");
        same("多云", render("（天气:{描述}{温度? ·{温度}}）", weather("多云", "", WeatherTextIcon.Kind.PARTLY_CLOUDY), WIFI), "an empty temperature drops its separator");
        same("多云 ·23°", render("（天气:{描述}{温度? ·{温度}}）"), "a present temperature keeps its separator");
        SystemWeatherText.Snapshot none = weather("", "", WeatherTextIcon.Kind.UNKNOWN);
        same("天气暂无数据", render("（天气:{描述} {温度}）", none, WIFI), "no weather shows the usual notice");
        same("暂无", render("（天气:{有数据?{描述}|暂无}）", none, WIFI), "the template can write its own notice");
        same("", render("（天气:{图标}）"), "no native weather image, no icon");
        same("0", render("（天气:{图标?1|0}）"), "the icon condition is false without a native image");
    }

    private static void batteryFields() {
        same("85%⚡", render("（电池:{电量}%{充电?⚡}）"), "percent and charging symbol");
        same("085", render("（电池:{电量:000}）"), "zero padding");
        same("充电中", render("（电池:{状态}）"), "state words");
        same("Charging", render("（电池:{状态:英文}）"), "english state words");
        same("████░", render("（电池:{条:5:█:░}）"), "bar of five cells");
        same("█████████░", render("（电池:{条}）"), "bar with the default ten cells");
        same("●●●●●○", render("（电池:{条:6:●:○}）"), "bar with the user's own symbols");
        same("够", render("（电池:{电量<=20?低|够}）"), "comparison false");
        BigClockSystemText.Snapshot low = system(15, BigClockSystemText.Charging.DISCHARGING, 3, BigClockSystemText.Transport.WIFI, Boolean.TRUE, 3, "");
        same("低", render("（电池:{电量<=20?低|够}）", low), "comparison true");
        same("低", render("（电池:{低电?低|够}）", low), "low battery condition");
        same("15%", render("（电池:{电量}%{状态? {状态}}）", low), "a discharging battery has no state words");
        same("85% 充电中", render("（电池:{电量}%{状态? {状态}}）"), "a charging battery adds its state words");
        BigClockSystemText.Snapshot missing = system(-1, BigClockSystemText.Charging.UNKNOWN, -1, BigClockSystemText.Transport.UNKNOWN, null, -1, "");
        same("电池暂无数据", render("（电池:{电量}%）", missing), "no battery data shows the usual notice");
        same("无", render("（电池:{有数据?{电量}%|无}）", missing), "the template can write its own notice");
        same("<icon> 85%", render("（电池:{图标} {电量}%）"), "icon then percent");
    }

    private static void signalFields() {
        same("3/4", render("（信号:{格数}/{总格}）"), "bars of total");
        same("●●●○", render("（信号:{条:4:●:○}）"), "signal dots");
        same("●●●●●●○○", render("（信号:{条:8:●:○}）"), "signal dots scaled to eight cells");
        BigClockSystemText.Snapshot noService = BigClockSystemText.sample(85, BigClockSystemText.Charging.DISCHARGING, -1, BigClockSystemText.Transport.WIFI,
                Boolean.TRUE, 3, "", Boolean.FALSE, Boolean.FALSE, Boolean.FALSE);
        same("无服务", render("（信号:{无服务?无服务|{格数}格}）", noService), "no service condition");
        same("3格", render("（信号:{无服务?无服务|{格数}格}）"), "service shows the bars");
        BigClockSystemText.Snapshot airplane = BigClockSystemText.sample(85, BigClockSystemText.Charging.DISCHARGING, -1, BigClockSystemText.Transport.NONE,
                null, -1, "", Boolean.TRUE, Boolean.FALSE, null);
        same("飞行模式", render("（信号:{状态}）", airplane), "airplane mode words");
        same("Airplane mode", render("（信号:{状态:英文}）", airplane), "airplane mode words in english");
    }

    private static void networkFields() {
        same("Wi-Fi", render("（网络:{类型}{受限?'（受限）'}）"), "type of a good connection");
        BigClockSystemText.Snapshot limited = system(85, BigClockSystemText.Charging.CHARGING, 3, BigClockSystemText.Transport.WIFI, Boolean.FALSE, 3, "");
        same("Wi-Fi（受限）", render("（网络:{类型}{受限?'（受限）'}）", limited), "quoted full-width parentheses inside the template");
        same("Wi-Fi(受限)", render("(网络:{类型}{受限?'(受限)'})", limited), "quoted ASCII parentheses inside an ASCII token");
        BigClockSystemText.Snapshot cellular = system(85, BigClockSystemText.Charging.CHARGING, 3, BigClockSystemText.Transport.CELLULAR, Boolean.TRUE, -1, "5G");
        same("5G", render("（网络:{制式?{制式}|{类型}}）", cellular), "cellular generation first");
        same("Wi-Fi", render("（网络:{制式?{制式}|{类型}}）"), "type when there is no generation");
        same("Wi-Fi ●●●○", render("（网络:{WiFi?Wi-Fi {条:4:●:○}|{类型}}）"), "wifi bars");
        BigClockSystemText.Snapshot offline = system(85, BigClockSystemText.Charging.CHARGING, 3, BigClockSystemText.Transport.NONE, null, -1, "");
        same("Offline", render("（网络:{类型:英文}）", offline), "offline in english");
        same("未联网", render("（网络:{类型}）", offline), "offline in chinese");
        same("网络暂无数据", render("（网络:{类型}）", system(85, BigClockSystemText.Charging.CHARGING, 3, BigClockSystemText.Transport.UNKNOWN, null, -1, "")), "unknown route shows the usual notice");
        same("联网状态未知", render("（网络:{状态}）", system(85, BigClockSystemText.Charging.CHARGING, 3, BigClockSystemText.Transport.WIFI, null, 3, "")), "unvalidated route");
    }

    private static void syntax() {
        same("{电量} 85", render("（电池:'{电量}' {电量}）"), "quoted braces stay literal");
        same("It's 85", render("（电池:It''s {电量}）"), "two apostrophes are one");
        same("a)b 85", render("（电池:'a)b' {电量}）"), "a quoted closing parenthesis does not end the token");
        same("a:b 85", render("（电池:'a:b' {电量}）"), "a quoted colon is literal");
        same("85", render("（电池：{电量}）"), "the full-width colon works");
        same("电量{电量}", render("电量{电量}"), "text outside a token is never interpreted");
        same("（电池：满格）", render("（电池：满格）"), "a note without a field is left alone");
        same("（电池:{电量}", render("（电池:{电量}"), "a token that never closes stays visible");
        same("10月16日 85%", render("（日期） （电池:{电量}%）"), "templates mix with the old tokens");
        same("85% · Wi-Fi", render("（电池:{电量}%） · （网络:{类型}）"), "two templates in one text");
        same("85 85", render("（电池:{ 电量 }） （电池:{电量}）"), "spaces around a field name are ignored");
        same("85>20", render("（电池:{电量>20?{电量}>20|低}）"), "a condition that shows a field and a symbol");
    }

    private static void errors() {
        String unknown = BigClockTextContent.templateError("（电池:{电了}）");
        check(unknown != null && unknown.contains("未知字段 {电了}") && unknown.contains("电池可用：") && unknown.contains("电量"), "unknown field lists what is available");
        check(BigClockTextContent.templateError("（电池:{电量}") .contains("缺少右括号"), "a token that never closes is reported");
        check(BigClockTextContent.templateError("（电池:{电量}}）").contains("多余的 }"), "a stray closing brace");
        check(BigClockTextContent.templateError("（电池:{充电?a|b|c}）").contains("只能有一个 |"), "two bars");
        check(BigClockTextContent.templateError("（电池:{状态:法文}）").contains("不支持参数“法文”"), "unknown argument");
        check(BigClockTextContent.templateError("（电池:{充电>1?a}）").contains("不是数字"), "comparison of a non-number");
        check(BigClockTextContent.templateError("（电池:{条:x}）").contains("格数"), "bar with a non-numeric count");
        check(BigClockTextContent.templateError("（电池:{条:30}）").contains("1 到 20"), "bar count out of range");
        check(BigClockTextContent.templateError("（电池:{电量:12}）").contains("补零"), "padding that is not zeros");
        check(BigClockTextContent.templateError("（电池:'abc {电量}）").contains("缺少右括号"), "an unfinished quote swallows the closing parenthesis and is reported");
        check(BigClockTextContent.templateError("（电池:{电量<=?a}）").contains("需要数字"), "comparison without a number");
        check(BigClockTextContent.templateError("（电池:{!电量}）") != null, "a lone exclamation mark");
        check(BigClockTextContent.templateError("（天气:{图标:原色}）") == null, "a valid weather template");
        check(BigClockTextContent.templateError("（天气:{图标:黑色}）").contains("原色"), "weather icon colour argument");
        check(BigClockTextContent.templateError("（天气:{描述:日文}）").contains("中文、英文、系统"), "weather language argument");
        check(BigClockTextContent.templateError("（信号:{格数<2?少|多}）") == null, "number comparison on signal bars");
        check(BigClockTextContent.templateError("今天（天气）（电池）和 (网络状态) 一切正常") == null, "old tokens and free text are never errors");
        check(BigClockWidgetsSettings.textValidationError("（电池:{电了}）") != null, "the settings validation reports template errors");
        StringBuilder longest = new StringBuilder();
        for (int i = 0; i < BigClockWidgetsSettings.MAX_TEXT; i++) longest.append('字');
        check(BigClockWidgetsSettings.textValidationError(longest.toString()) == null, "the longest allowed text is accepted");
        check(BigClockWidgetsSettings.textValidationError(longest + "字") != null, "one more character is refused");
        check(BigClockWidgetsSettings.textValidationError("a\nb") != null, "a line break is refused");
    }

    private static void dependenciesAndCompatibility() {
        check(BigClockTextContent.dependencies("（电池:{电量}）") == BigClockTextContent.BATTERY, "battery template depends on the battery only");
        check(BigClockTextContent.dependencies("（天气:{描述}） （信号:{格数}） （网络:{类型}）")
                == (BigClockTextContent.WEATHER | BigClockTextContent.CELLULAR | BigClockTextContent.NETWORK), "each template subscribes to its own category");
        check(BigClockTextContent.dependencies("（蜂窝信号:{格数}） （网络状态:{类型}）") == (BigClockTextContent.CELLULAR | BigClockTextContent.NETWORK), "long category names are accepted");
        check(BigClockTextContent.dependencies("（电池：满格）") == 0, "a note is not a subscription");
        check(BigClockTextContent.dependencies("（电池:'）' {电量}）（时间）") == (BigClockTextContent.BATTERY | BigClockTextContent.TIME), "a quoted parenthesis does not hide later tokens");
        check(BigClockTextContent.usesWeather("（天气:{描述}）"), "weather templates opt into the native snapshot");
        check(!BigClockTextContent.usesWeather("（电池:{电量}）"), "battery templates do not");
        same("85% 充电中", render("（电池）", WIFI).replace("<icon> ", ""), "the old battery token still works");
        same("3/4格", render("（蜂窝信号）", WIFI).replace("<icon> ", ""), "the old signal token still works");
        same("Wi-Fi", render("（网络状态）", WIFI).replace("<icon> ", ""), "the old network token still works");
    }

    private static void plainText() {
        BigClockSystemText.clear();
        BigClockSystemText.batteryRaw(85, 100, 2);
        same(" 85%", BigClockTextContent.resolve("（电池:{图标} {电量}%）", NOW, UTC), "plain text leaves icons out");
        BigClockSystemText.clear();
    }

    private static void previewIcons() {
        CharSequence preview = BigClockPresets.preview("（天气:{图标} {温度}） （电池:{图标} {电量}%） （信号:{图标}） （网络:{图标}）", NOW);
        check(preview instanceof android.text.Spanned, "the preview of icons is styled text");
        android.text.Spanned styled = (android.text.Spanned) preview;
        check(styled.getSpans(0, styled.length(), BigClockPreviewIcon.class).length == 4, "one sketched icon for each icon field");
        check(!preview.toString().contains("暂无数据") && !preview.toString().contains("（"), "the preview resolves every field");
        for (WeatherTextIcon.Kind kind : WeatherTextIcon.Kind.values()) for (boolean colours : new boolean[]{false, true}) draw(new BigClockPreviewIcon(kind, colours));
        for (int token : new int[]{BigClockTextContent.BATTERY, BigClockTextContent.CELLULAR, BigClockTextContent.NETWORK}) {
            for (BigClockSystemText.Transport route : BigClockSystemText.Transport.values()) for (int level : new int[]{-1, 0, 2, 4}) {
                draw(new BigClockPreviewIcon(token, BigClockSystemText.sample(level < 0 ? -1 : level * 25, BigClockSystemText.Charging.CHARGING, level, route, Boolean.TRUE, level, "5G", Boolean.FALSE, Boolean.FALSE, Boolean.TRUE)));
            }
        }
        check(BigClockPresets.preview("（电池:{电了}）", NOW).toString().startsWith("格式有误："), "a bad template is explained in the preview");
        check(BigClockPresets.preview("", NOW).length() == 0, "an empty field has no preview");
    }

    private static void draw(BigClockPreviewIcon icon) {
        Paint paint = new Paint(); paint.setTextSize(48f); paint.setColor(0xff336699);
        Paint.FontMetricsInt metrics = new Paint.FontMetricsInt();
        int width = icon.getSize(paint, "x", 0, 1, metrics);
        check(width > 0 && metrics.descent > metrics.ascent, "a sketched icon has a size");
        icon.draw(new Canvas(), "x", 0, 1, 10f, 0, 60, 80, paint);
    }

    private static void presets() {
        for (boolean right : new boolean[]{false, true}) {
            java.util.HashSet<String> seen = new java.util.HashSet<>();
            for (String[] preset : BigClockPresets.entries(right)) {
                check(preset.length == 2 && !preset[0].isEmpty() && !preset[1].isEmpty() && seen.add(preset[0]), "every preset has a value and a name, once");
                check(BigClockWidgetsSettings.textValidationError(preset[0]) == null, "preset saves: " + preset[0]);
                String shown = BigClockPresets.preview(preset[0], NOW).toString();
                check(!shown.isEmpty() && !shown.startsWith("格式有误") && !shown.contains("暂无数据") && !shown.contains("{") && !shown.contains("}"), "preset previews: " + preset[0] + " -> " + shown);
            }
        }
        boolean templates = false, system = false;
        for (String[] preset : BigClockPresets.entries(false)) { templates |= preset[0].contains(":{"); system |= preset[0].contains("电池:"); }
        check(templates && system, "the left side offers the battery, signal and network templates too");
    }

    public static void main(String[] args) {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.SIMPLIFIED_CHINESE);
            SystemWeatherText.clear(); BigClockSystemText.clear();
            weatherFields(); batteryFields(); signalFields(); networkFields(); syntax(); errors(); dependenciesAndCompatibility(); plainText(); previewIcons(); presets();
        } finally { Locale.setDefault(original); SystemWeatherText.clear(); BigClockSystemText.clear(); }
        System.out.println("ComponentFormatCheck: " + checks + " checks passed");
    }
}
