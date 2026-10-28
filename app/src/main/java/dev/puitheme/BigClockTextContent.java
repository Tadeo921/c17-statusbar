// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie

package dev.puitheme;

import android.text.SpannableString;
import android.text.Spanned;
import android.text.SpannedString;
import java.util.ArrayList;
import java.util.TimeZone;

/** Literal text with explicit full-width/ASCII parenthesis placeholders.
 * User English, quotes, braces and ordinary date letters stay literal. */
final class BigClockTextContent {
    static final int TIME=1,WEATHER=2,CELLULAR=4,NETWORK=8,BATTERY=16;
    private BigClockTextContent() { }

    static String resolve(String template, long now, TimeZone zone) {
        return resolve(template, now, zone, SystemWeatherText.snapshot());
    }

    static String resolve(String template, long now, TimeZone zone, SystemWeatherText.Snapshot weather) {
        return render(template, now, zone, weather, BigClockSystemText.snapshot(), false, null).toString();
    }

    static CharSequence resolveStyled(String template, long now, TimeZone zone) {
        return resolveStyled(template,now,zone,SystemWeatherText.snapshot(),BigClockSystemText.snapshot(),null);
    }

    /** The row passes one immutable event snapshot to both slots. Existing
     * weather spans belong to this same slot and can retain their cached path. */
    static CharSequence resolveStyled(String template,long now,TimeZone zone,SystemWeatherText.Snapshot weather,
            BigClockSystemText.Snapshot system,CharSequence previous) {
        return render(template,now,zone,weather,system,true,false,previous);
    }

    /** The settings app's preview: the same text, with sketched icons where the SystemUI row draws native ones. */
    static CharSequence resolvePreview(String template,long now,TimeZone zone,SystemWeatherText.Snapshot weather,
            BigClockSystemText.Snapshot system) {
        return render(template,now,zone,weather,system,true,true,null);
    }

    private static CharSequence render(String template, long now, TimeZone zone,
            SystemWeatherText.Snapshot weather,BigClockSystemText.Snapshot system, boolean styled,CharSequence previous) {
        return render(template,now,zone,weather,system,styled,false,previous);
    }

    private static CharSequence render(String template, long now, TimeZone zone,
            SystemWeatherText.Snapshot weather,BigClockSystemText.Snapshot system, boolean styled,boolean preview,CharSequence previous) {
        if (template == null || template.isEmpty()) return "";
        TimeZone timeZone = zone == null ? TimeZone.getDefault() : zone;
        SystemWeatherText.Snapshot actualWeather = weather == null ? SystemWeatherText.snapshot() : weather;
        BigClockSystemText.Snapshot actualSystem = system == null ? BigClockSystemText.snapshot() : system;
        StringBuilder output = new StringBuilder(template.length());
        ArrayList<Icon> icons = new ArrayList<>(2);
        String time = null, date = null, week = null;
        for (int index = 0; index < template.length();) {
            char start = template.charAt(index);
            if (start == '（' || start == '(') {
                int end = ComponentFormat.tokenEnd(template, index);
                if (end > index) {
                    String token = template.substring(index + 1, end), value = null;
                    switch (token) {
                        case "天气": case "天气中文": case "天气英文": case "天气原色": case "天气中文原色": case "天气英文原色": {
                            int language = token.contains("中文") ? WeatherLabels.CHINESE : token.contains("英文") ? WeatherLabels.ENGLISH : WeatherLabels.SYSTEM;
                            if (styled && (actualWeather.hasIcon()||preview) && !actualWeather.description.isEmpty()) {
                                output.append(weatherInformation(actualWeather, language)).append(' ');
                                icons.add(new Icon(output.length(), actualWeather.weatherIconKind, token.endsWith("原色"))); output.append('\ufffc');value="";
                            } else value=styled?weatherInformation(actualWeather, language):plainWeather(actualWeather, language);
                            break;
                        }
                        case "天气文字": value = actualWeather.text; break;
                        case "天气文字中文": value = plainWeather(actualWeather, WeatherLabels.CHINESE); break;
                        case "天气文字英文": value = plainWeather(actualWeather, WeatherLabels.ENGLISH); break;
                        case "天气描述": value = weatherWords(actualWeather, WeatherLabels.SYSTEM); break;
                        case "天气描述中文": value = weatherWords(actualWeather, WeatherLabels.CHINESE); break;
                        case "天气描述英文": value = weatherWords(actualWeather, WeatherLabels.ENGLISH); break;
                        case "天气图标": case "天气图标原色":
                            if (styled && (actualWeather.hasIcon()||preview)) {
                                icons.add(new Icon(output.length(), actualWeather.weatherIconKind, token.endsWith("原色"))); output.append('\ufffc'); value = "";
                            } else value = styled?"":actualWeather.description.isEmpty() ? SystemWeatherText.UNAVAILABLE : actualWeather.description;
                            break;
                        case "温度": value = actualWeather.temperatureText(); break;
                        case "Señal móvil":
                            if(styled){icons.add(new Icon(output.length(),CELLULAR));output.append('\ufffc').append(' ');}
                            value = actualSystem.cellularSignal; break;
                        case "网络状态":
                            if(styled){icons.add(new Icon(output.length(),NETWORK));output.append('\ufffc').append(' ');}
                            value = actualSystem.networkStatus; break;
                        case "Batería":
                            if(styled){icons.add(new Icon(output.length(),BATTERY));output.append('\ufffc').append(' ');}
                            value = actualSystem.battery; break;
                        case "Hora":
                            if (time == null) time = TimeFormat.format("HH:mm", now, timeZone);
                            value = time; break;
                        case "Fecha":
                            if (date == null) date = TimeFormat.format("M d", now, timeZone);
                            value = date; break;
                        case "Día de la semana":
                            if (week == null) week = TimeFormat.format("{星期}", now, timeZone);
                            value = week; break;
                        default: {
                            int category = ComponentFormat.category(token);
                            value = category != 0 ? template(category, token, actualWeather, actualSystem, styled, preview, output, icons)
                                    : extraTimeToken(token, now, timeZone);
                            break;
                        }
                    }
                    if (value != null) { output.append(value); index = end + 1; continue; }
                }
            }
            output.append(start); index++;
        }
        if (icons.isEmpty()) return output.toString();
        SpannableString value = new SpannableString(output);
        WeatherTextIcon.Span[] retained=previous instanceof Spanned
                ?((Spanned)previous).getSpans(0,previous.length(),WeatherTextIcon.Span.class):null;
        BigClockSystemIcon.Span[] retainedSystem=previous instanceof Spanned
                ?((Spanned)previous).getSpans(0,previous.length(),BigClockSystemIcon.Span.class):null;
        int weatherIndex=0,systemIndex=0;
        for (int index=0;index<icons.size();index++) {
            Icon icon=icons.get(index);
            if(preview) {
                value.setSpan(icon.systemToken!=0?new BigClockPreviewIcon(icon.systemToken,actualSystem)
                        :new BigClockPreviewIcon(icon.kind,icon.nativeColours),icon.start,icon.start+1,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if(icon.systemToken!=0) {
                BigClockSystemIcon.Span prior=retainedSystem!=null&&systemIndex<retainedSystem.length?retainedSystem[systemIndex]:null;
                value.setSpan(BigClockSystemIcon.obtain(icon.systemToken,actualSystem,prior),icon.start,icon.start+1,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                systemIndex++;
            } else {
                WeatherTextIcon.Span span=retained!=null&&weatherIndex<retained.length&&retained[weatherIndex].kind==icon.kind
                        &&retained[weatherIndex].image==actualWeather.weatherIcon&&retained[weatherIndex].nativeColours==icon.nativeColours
                        ?retained[weatherIndex]:new WeatherTextIcon.Span(icon.kind,actualWeather.weatherIcon,icon.nativeColours);
                value.setSpan(span, icon.start, icon.start + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);weatherIndex++;
            }
        }
        return new SpannedString(value);
    }

    /** "（类别:模板）": the template fills {@code output} itself and its icons join {@code icons}.
     * A template that is not valid stays visible as ordinary text, like any other unknown token. */
    private static String template(int category, String token, SystemWeatherText.Snapshot weather,
            BigClockSystemText.Snapshot system, boolean styled, boolean preview, StringBuilder output, ArrayList<Icon> icons) {
        ComponentFormat.Template parsed = ComponentFormat.compile(category, ComponentFormat.body(token));
        if (parsed == null) return null;
        int mode = !styled ? ComponentFields.PLAIN : preview ? ComponentFields.PREVIEW : ComponentFields.NATIVE;
        ComponentFormat.render(parsed, ComponentFields.of(category, weather, system, mode), new ComponentFormat.Sink() {
            @Override public void text(String value) { output.append(value); }
            @Override public void icon(java.util.List<String> args) {
                if (!styled) return;
                if (category == WEATHER) {
                    if (!weather.hasIcon() && !preview) return;
                    icons.add(new Icon(output.length(), weather.weatherIconKind, args.contains("原色")));
                } else icons.add(new Icon(output.length(), category));
                output.append('\ufffc');
            }
        });
        return "";
    }

    private static final class Icon {
        final int start,systemToken; final WeatherTextIcon.Kind kind; final boolean nativeColours;
        Icon(int start, WeatherTextIcon.Kind kind, boolean nativeColours) { this.start = start; this.kind = kind;systemToken=0;this.nativeColours=nativeColours; }
        Icon(int start,int token) {this.start=start;kind=null;systemToken=token;nativeColours=false;}
    }

    /** "晴 ·23°": description, dotted temperature, in the requested language. */
    private static String weatherInformation(SystemWeatherText.Snapshot weather, int language) {
        String words = weatherWords(weather, language);
        return weather.description.isEmpty()?WeatherLabels.unavailable(language):weather.temperature.isEmpty()
                ?words:words+" ·"+weather.temperature;
    }

    /** Accessible plain form: "晴 23°" (no icon). */
    private static String plainWeather(SystemWeatherText.Snapshot weather, int language) {
        if (language == WeatherLabels.SYSTEM) return weather.text;
        String words = weatherWords(weather, language);
        return weather.description.isEmpty() ? WeatherLabels.unavailable(language)
                : weather.temperature.isEmpty() ? words : words + " " + weather.temperature;
    }

    private static String weatherWords(SystemWeatherText.Snapshot weather, int language) {
        return weather.description.isEmpty() ? WeatherLabels.unavailable(language) : WeatherLabels.description(weather, language);
    }

    static boolean usesWeather(String template) {
        return (dependencies(template)&WEATHER)!=0;
    }

    private static final String FORMAT_PREFIX = "格式:", FORMAT_PREFIX_WIDE = "格式：";

    /** Extra date/time tokens. Minute resolution only: a pattern with seconds is
     * rejected so a component can never need a per-second timer. */
    private static String extraTimeToken(String token, long now, TimeZone zone) {
        switch (token) {
            case "年": return TimeFormat.format("yyyy", now, zone);
            case "年月日": return TimeFormat.format("yyyy年M月d日", now, zone);
            case "周": return TimeFormat.format("{周}", now, zone);
            case "12小时": return TimeFormat.format("hh:mm", now, zone);
            case "上午下午": return TimeFormat.format("{上午下午}", now, zone);
            case "Franja horaria": return TimeFormat.format("{时段}", now, zone);
            case "农历": return TimeFormat.format("{农历}", now, zone);
            case "Fecha lunar": return TimeFormat.format("{农历日期}", now, zone);
            case "干支": return TimeFormat.format("{干支}", now, zone);
            case "日期英文": return english("MMM d", now, zone);
            case "星期英文": return english("EEEE", now, zone);
            case "星期英文简": return english("EEE", now, zone);
            case "月份英文": return english("MMMM", now, zone);
            default:
                String pattern = formatPattern(token);
                if (pattern == null) return null;
                try {
                    if (TimeFormat.validationError(pattern) != null || TimeFormat.hasSeconds(pattern)) return null;
                    return TimeFormat.format(pattern, now, zone);
                } catch (IllegalArgumentException invalid) { return null; }
        }
    }

    private static String formatPattern(String token) {
        if (token.startsWith(FORMAT_PREFIX)) return token.substring(FORMAT_PREFIX.length());
        if (token.startsWith(FORMAT_PREFIX_WIDE)) return token.substring(FORMAT_PREFIX_WIDE.length());
        return null;
    }

    private static String english(String pattern, long now, TimeZone zone) {
        java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat(pattern, java.util.Locale.ENGLISH);
        formatter.setTimeZone(zone);
        return formatter.format(new java.util.Date(now));
    }

    private static int tokenDependency(String token) {
        switch (token) {
            case "Hora": case "Fecha": case "Día de la semana": case "年": case "年月日": case "周": case "12小时": case "上午下午": case "Franja horaria":
            case "农历": case "Fecha lunar": case "干支": case "日期英文": case "星期英文": case "星期英文简": case "月份英文": return TIME;
            case "天气": case "天气中文": case "天气英文": case "天气原色": case "天气中文原色": case "天气英文原色": case "天气文字":
            case "天气文字中文": case "天气文字英文": case "天气描述": case "天气描述中文": case "天气描述英文": case "天气图标":
            case "天气图标原色": case "温度": return WEATHER;
            case "Señal móvil": return CELLULAR;
            case "网络状态": return NETWORK;
            case "Batería": return BATTERY;
            default: return formatPattern(token) != null ? TIME : ComponentFormat.category(token);
        }
    }

    static int dependencies(String template) {
        if(template==null||template.isEmpty())return 0;
        int result=0;
        for (int index = 0; index < template.length(); index++) {
            char start = template.charAt(index);
            if (start != '（' && start != '(') continue;
            int end = ComponentFormat.tokenEnd(template, index);
            if (end < 0) break;
            result |= tokenDependency(template.substring(index + 1, end));
            index = end;
        }
        return result;
    }

    /** The first problem of a component text, or null: a "（类别:模板）" that cannot close or whose fields are wrong.
     * Everything outside such a token is free text and never an error. */
    static String templateError(String template) {
        if (template == null || template.isEmpty()) return null;
        for (int index = 0; index < template.length(); index++) {
            char start = template.charAt(index);
            if (start != '（' && start != '(') continue;
            int end = ComponentFormat.tokenEnd(template, index);
            int category = ComponentFormat.templateCategoryAt(template, index);
            if (category != 0) {
                String name = ComponentFormat.templateNameAt(template, index);
                if (end < 0) return "（" + name + ":…）缺少右括号；括号需要出现在模板里时请放进单引号";
                String problem = ComponentFormat.validationError(category, ComponentFormat.body(template.substring(index + 1, end)));
                if (problem != null) return "（" + name + ":…）" + problem;
            }
            if (end < 0) break;
            index = end;
        }
        return null;
    }
}
