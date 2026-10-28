// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie

package dev.puitheme;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Templates for the weather, battery, signal and network tokens of the left/right text components,
 * written like the time format: literal text around {field} tags, e.g. （电池:{电量}%{充电?⚡}）.
 *
 * Grammar of the text after "（类别:":
 *   template := ( literal | 'quoted' | {field} )*
 *   field    := name [ :arg { :arg } ] [ cmp number ] [ ? template [ | template ] ]
 * Literal text is shown as typed. '...' keeps braces, bars, parentheses and colons literal ('' is one apostrophe).
 * Pure Java with no Android types: the same code runs in SystemUI, in the settings preview and in desktop checks. */
final class ComponentFormat {
    static final int TEXT = 1, NUMBER = 2, BOOL = 3, ICON = 4;
    private static final int LT = 1, LE = 2, GT = 3, GE = 4, EQ = 5, NE = 6;
    private ComponentFormat() { }

    /** What a category supplies to its template. {@code kind} is 0 for an unknown field. */
    interface Fields {
        String category();
        List<String> names();
        int kind(String name);
        /** null when the arguments are accepted. */
        String argError(String name, List<String> args);
        String text(String name, List<String> args);
        /** NaN when the value is not known. */
        double number(String name);
        boolean truth(String name);
        boolean hasData();
        String unavailable();
    }

    interface Sink {
        void text(String value);
        void icon(List<String> args);
    }

    private static final class Field {
        final String name; final List<String> args; final int cmp; final double number;
        final boolean conditional; final List<Node> yes, no;
        Field(String name, List<String> args, int cmp, double number, boolean conditional, List<Node> yes, List<Node> no) {
            this.name = name; this.args = args; this.cmp = cmp; this.number = number;
            this.conditional = conditional; this.yes = yes; this.no = no;
        }
    }

    private static final class Node {
        final String text; final Field field;
        Node(String text) { this.text = text; field = null; }
        Node(Field field) { this.field = field; text = null; }
    }

    static final class Template {
        private final List<Node> nodes; private final boolean usesAvailability;
        private Template(List<Node> nodes, boolean usesAvailability) { this.nodes = nodes; this.usesAvailability = usesAvailability; }
    }

    // ---- locating a template token inside the component text ----

    /** The BigClockTextContent dependency bit of a category name, or 0 when the name is not a template category. */
    static int categoryOf(String name) {
        if (name == null) return 0;
        switch (name.trim()) {
            case "天气": return BigClockTextContent.WEATHER;
            case "Batería": return BigClockTextContent.BATTERY;
            case "信号": case "Señal móvil": return BigClockTextContent.CELLULAR;
            case "网络": case "网络状态": return BigClockTextContent.NETWORK;
            default: return 0;
        }
    }

    private static boolean colon(char c) { return c == ':' || c == '：'; }

    private static int firstColon(String text, int from) {
        for (int i = from; i < text.length(); i++) if (colon(text.charAt(i))) return i;
        return -1;
    }

    /** The category of a token written "类别:模板", or 0 for every other token (including "（电池）").
     * A template needs at least one {field}: "（电池：满格）" stays the plain note it always was. */
    static int category(String token) {
        int at = token == null ? -1 : firstColon(token, 0);
        return at <= 0 || token.indexOf('{', at) < 0 ? 0 : categoryOf(token.substring(0, at));
    }

    /** The category of the template token opened at {@code open}, even when it never closes; 0 for any other token. */
    static int templateCategoryAt(String text, int open) {
        int at = firstColon(text, open + 1);
        return at < 0 || text.indexOf('{', at) < 0 ? 0 : categoryOf(text.substring(open + 1, at));
    }

    /** The category word as typed, for messages. */
    static String templateNameAt(String text, int open) {
        return text.substring(open + 1, firstColon(text, open + 1)).trim();
    }

    /** The template of a token already known to be "类别:模板". */
    static String body(String token) {
        return token.substring(firstColon(token, 0) + 1);
    }

    /** Index of the closing parenthesis of the token opened at {@code open}, or -1 when it never closes.
     * A "类别:模板" token respects quotes and braces, so its template may contain parentheses; every other
     * token ends at the first closing parenthesis exactly as before. */
    static int tokenEnd(String text, int open) {
        char start = text.charAt(open), close = start == '（' ? '）' : ')';
        int at = firstColon(text, open + 1);
        if (at < 0 || categoryOf(text.substring(open + 1, at)) == 0) return text.indexOf(close, open + 1);
        int depth = 0; boolean quoted = false;
        for (int i = at + 1; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\'') {
                if (quoted && i + 1 < text.length() && text.charAt(i + 1) == '\'') { i++; continue; }
                quoted = !quoted; continue;
            }
            if (quoted) continue;
            if (c == '{') depth++;
            else if (c == '}') { if (depth > 0) depth--; }
            else if (c == close && depth == 0) return i;
        }
        return -1;
    }

    // ---- parsing ----

    static Template parse(String body) {
        Parser parser = new Parser(body == null ? "" : body);
        List<Node> nodes = parser.sequence(false);
        return new Template(nodes, usesAvailability(nodes));
    }

    private static boolean usesAvailability(List<Node> nodes) {
        if (nodes == null) return false;
        for (Node node : nodes) {
            if (node.field == null) continue;
            if ("有数据".equals(node.field.name) || usesAvailability(node.field.yes) || usesAvailability(node.field.no)) return true;
        }
        return false;
    }

    private static final class Parser {
        private final String s; private int i;
        Parser(String s) { this.s = s; }

        /** A run of literal text and fields. Inside a condition it stops at the closing brace or the bar. */
        List<Node> sequence(boolean nested) {
            List<Node> out = new ArrayList<>(); StringBuilder text = new StringBuilder();
            while (i < s.length()) {
                char c = s.charAt(i);
                if (c == '\'') quote(text);
                else if (c == '{') { flush(out, text); out.add(new Node(field())); }
                else if (c == '}') {
                    if (!nested) throw new IllegalArgumentException("多余的 }，要显示大括号请放进单引号");
                    break;
                } else if (c == '|' && nested) break;
                else { text.append(c); i++; }
            }
            flush(out, text);
            return out;
        }

        private void flush(List<Node> out, StringBuilder text) {
            if (text.length() > 0) { out.add(new Node(text.toString())); text.setLength(0); }
        }

        /** At an apostrophe: '' is one apostrophe, otherwise everything up to the closing apostrophe is literal. */
        private void quote(StringBuilder into) {
            if (i + 1 < s.length() && s.charAt(i + 1) == '\'') { into.append('\''); i += 2; return; }
            i++;
            while (true) {
                if (i >= s.length()) throw new IllegalArgumentException("单引号需要成对");
                char c = s.charAt(i);
                if (c == '\'') {
                    if (i + 1 < s.length() && s.charAt(i + 1) == '\'') { into.append('\''); i += 2; continue; }
                    i++; return;
                }
                into.append(c); i++;
            }
        }

        private Field field() {
            i++;
            int begin = i;
            while (i < s.length()) {
                char c = s.charAt(i);
                if (colon(c) || c == '?' || c == '}' || c == '<' || c == '>' || c == '=' || c == '!') break;
                i++;
            }
            if (i >= s.length()) throw new IllegalArgumentException("缺少 }");
            String name = s.substring(begin, i).trim();
            if (name.isEmpty()) throw new IllegalArgumentException("{} 里需要写字段名");
            List<String> args = Collections.emptyList(); int cmp = 0; double number = 0;
            char c = s.charAt(i);
            if (colon(c)) {
                i++; args = arguments();
                if (i >= s.length()) throw new IllegalArgumentException("缺少 }");
                c = s.charAt(i);
            } else if (c == '<' || c == '>' || c == '=' || c == '!') {
                cmp = comparator();
                number = number(name);
                if (i >= s.length()) throw new IllegalArgumentException("缺少 }");
                c = s.charAt(i);
            }
            boolean conditional = false; List<Node> yes = null, no = null;
            if (c == '?') {
                conditional = true; i++;
                yes = sequence(true);
                if (i < s.length() && s.charAt(i) == '|') {
                    i++; no = sequence(true);
                    if (i < s.length() && s.charAt(i) == '|') throw new IllegalArgumentException("一个条件里只能有一个 |，要显示竖线请放进单引号");
                }
            }
            if (i >= s.length() || s.charAt(i) != '}') throw new IllegalArgumentException("缺少 }");
            i++;
            return new Field(name, args, cmp, number, conditional, yes, no);
        }

        private int comparator() {
            char c = s.charAt(i), next = i + 1 < s.length() ? s.charAt(i + 1) : 0;
            if (c == '<' && next == '=') { i += 2; return LE; }
            if (c == '>' && next == '=') { i += 2; return GE; }
            if (c == '!' && next == '=') { i += 2; return NE; }
            if (c == '<') { i++; return LT; }
            if (c == '>') { i++; return GT; }
            if (c == '=') { i++; if (i < s.length() && s.charAt(i) == '=') i++; return EQ; }
            throw new IllegalArgumentException("! 后面需要 =，要显示感叹号请放进单引号");
        }

        private double number(String field) {
            int begin = i;
            while (i < s.length() && ("0123456789.-+−".indexOf(s.charAt(i)) >= 0)) i++;
            String digits = s.substring(begin, i).replace('−', '-');
            if (digits.isEmpty()) throw new IllegalArgumentException("{" + field + "} 的比较后面需要数字");
            try { return Double.parseDouble(digits); }
            catch (NumberFormatException invalid) { throw new IllegalArgumentException("{" + field + "} 的比较数字不正确：" + digits); }
        }

        /** Arguments up to ? or }, separated by : (or the full-width colon). Quoted text keeps those characters. */
        private List<String> arguments() {
            List<String> parts = new ArrayList<>(); StringBuilder part = new StringBuilder(); boolean quoted = false;
            while (i < s.length()) {
                char c = s.charAt(i);
                if (c == '\'') { quoted = true; quote(part); continue; }
                if (c == '?' || c == '}') break;
                if (colon(c)) { parts.add(quoted ? part.toString() : part.toString().trim()); part.setLength(0); quoted = false; i++; continue; }
                if (c == '{') throw new IllegalArgumentException("参数里不能再写 {，需要时请放进单引号");
                part.append(c); i++;
            }
            parts.add(quoted ? part.toString() : part.toString().trim());
            return parts;
        }
    }

    // ---- checking and rendering ----

    /** The reason the template cannot be used, or null. Names and arguments are checked against the category. */
    static String validate(Fields fields, String body) {
        try { check(fields, parse(body).nodes); return null; }
        catch (IllegalArgumentException invalid) { return invalid.getMessage(); }
    }

    /** The reason a "类别:模板" token cannot be used, or null. */
    static String validationError(int category, String body) {
        Fields fields = ComponentFields.of(category, null, null, ComponentFields.PLAIN);
        return fields == null ? "未知类别" : validate(fields, body);
    }

    private static final java.util.Map<String, Object> COMPILED = new java.util.LinkedHashMap<String, Object>(32, .75f, true) {
        @Override protected boolean removeEldestEntry(java.util.Map.Entry<String, Object> eldest) { return size() > 48; }
    };
    private static final Object INVALID = new Object();

    /** A checked template, parsed once and reused by every refresh; null when it is not valid for the category. */
    static Template compile(int category, String body) {
        String key = category + ":" + body;
        Object known;
        synchronized (COMPILED) { known = COMPILED.get(key); }
        if (known == null) {
            known = validationError(category, body) == null ? parse(body) : INVALID;
            synchronized (COMPILED) { COMPILED.put(key, known); }
        }
        return known == INVALID ? null : (Template) known;
    }

    private static void check(Fields fields, List<Node> nodes) {
        if (nodes == null) return;
        for (Node node : nodes) {
            if (node.field == null) continue;
            Field field = node.field;
            int kind = fields.kind(field.name);
            if (kind == 0) throw new IllegalArgumentException("未知字段 {" + field.name + "}。" + fields.category() + "可用：" + join(fields.names()));
            String problem = fields.argError(field.name, field.args);
            if (problem != null) throw new IllegalArgumentException(problem);
            if (field.cmp != 0 && kind != NUMBER) throw new IllegalArgumentException("{" + field.name + "} 不是数字，不能用 < > = 比较");
            check(fields, field.yes); check(fields, field.no);
        }
    }

    private static String join(List<String> names) {
        StringBuilder out = new StringBuilder();
        for (String name : names) { if (out.length() > 0) out.append('、'); out.append(name); }
        return out.toString();
    }

    static void render(Template template, Fields fields, Sink sink) {
        if (!template.usesAvailability && !fields.hasData()) { sink.text(fields.unavailable()); return; }
        emit(template.nodes, fields, sink);
    }

    private static void emit(List<Node> nodes, Fields fields, Sink sink) {
        if (nodes == null) return;
        for (Node node : nodes) {
            if (node.field == null) { sink.text(node.text); continue; }
            Field field = node.field;
            int kind = fields.kind(field.name);
            if (kind == 0) continue;
            if (field.conditional) emit(condition(field, kind, fields) ? field.yes : field.no, fields, sink);
            else if (kind == ICON) sink.icon(field.args);
            else if (kind != BOOL) sink.text(fields.text(field.name, field.args));
        }
    }

    private static boolean condition(Field field, int kind, Fields fields) {
        if (field.cmp != 0) {
            double value = fields.number(field.name);
            if (Double.isNaN(value)) return false;
            switch (field.cmp) {
                case LT: return value < field.number;
                case LE: return value <= field.number;
                case GT: return value > field.number;
                case GE: return value >= field.number;
                case EQ: return value == field.number;
                default: return value != field.number;
            }
        }
        switch (kind) {
            case NUMBER: return !Double.isNaN(fields.number(field.name));
            case TEXT: return !fields.text(field.name, field.args).isEmpty();
            default: return fields.truth(field.name);
        }
    }
}
