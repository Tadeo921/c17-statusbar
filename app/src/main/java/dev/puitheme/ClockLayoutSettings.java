// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Keys and defaults for clock digit alignment and the independent two-row status clock.
 *
 * Digit alignment is available to every text clock group. The two-row clock is its own mode of
 * the status-bar time: it has its own master switch, content, size, weight, spacing, position,
 * colours and margins, and it does not depend on the default clock's switches.
 */
public final class ClockLayoutSettings {
    public static final String STATUS = "clock";
    /** Groups that can share fixed-width digit cells. */
    public static final String[] DIGIT_GROUPS = {STATUS, "shade_clock", PanelMode.CLOCK};

    // Schema 13 (code90).
    public static final String DUAL_ENABLED = "clock_dual_enabled";
    public static final String DUAL_UPPER_PATTERN = "clock_dual_upper_pattern";
    public static final String DUAL_LOWER_PATTERN = "clock_dual_lower_pattern";
    public static final String DUAL_UPPER_SCALE = "clock_dual_upper_scale";
    public static final String DUAL_LOWER_SCALE = "clock_dual_lower_scale";
    public static final String DUAL_UPPER_WEIGHT = "clock_dual_upper_weight";
    public static final String DUAL_LOWER_WEIGHT = "clock_dual_lower_weight";
    public static final String DUAL_LINE_GAP = "clock_dual_line_gap";
    public static final String DUAL_ALIGN = "clock_dual_align";
    public static final String MARGIN_ENABLED = "clock_margin_enabled";
    public static final String MARGIN_START = "clock_margin_start";
    public static final String MARGIN_END = "clock_margin_end";
    // Schema 14: the two-row clock owns its position, letter spacing and colours.
    public static final String DUAL_OFFSET_X = "clock_dual_offset_x";
    public static final String DUAL_OFFSET_Y = "clock_dual_offset_y";
    public static final String DUAL_SPACING = "clock_dual_spacing";
    public static final String DUAL_COLOR_ENABLED = "clock_dual_color_enabled";
    public static final String DUAL_COLOR_LIGHT = "clock_dual_color_light";
    public static final String DUAL_COLOR_DARK = "clock_dual_color_dark";
    /** IconAppearance group name for the two-row colours (keys are group + _color_light/_dark). */
    public static final String DUAL_COLOR_GROUP = "clock_dual";

    public static final String DEFAULT_UPPER_PATTERN = "HH:mm";
    public static final String DEFAULT_LOWER_PATTERN = "M月d日 {周}";
    /** Both rows use the same native-pixel scale. Saved independent percentages remain independent. */
    public static final float DEFAULT_ROW_SCALE = 72f;
    public static final String ALIGN_CENTER = "center", ALIGN_LEFT = "left", ALIGN_RIGHT = "right";

    public static final Map<String, Boolean> BOOLEANS;
    public static final Map<String, Float> NUMBERS;
    public static final Map<String, String> STRINGS;
    public static final Map<String, Integer> COLORS;

    static {
        Map<String, Boolean> switches = new LinkedHashMap<>();
        Map<String, Float> numbers = new LinkedHashMap<>();
        Map<String, String> strings = new LinkedHashMap<>();
        Map<String, Integer> colors = new LinkedHashMap<>();
        for (String group : DIGIT_GROUPS) {
            switches.put(digitKey(group), true);
            switches.put(colonKey(group), true);
            numbers.put(digitSpacingKey(group), 0f);
        }
        switches.put(DUAL_ENABLED, false);
        switches.put(MARGIN_ENABLED, false);
        switches.put(DUAL_COLOR_ENABLED, false);
        numbers.put(DUAL_UPPER_SCALE, DEFAULT_ROW_SCALE);
        numbers.put(DUAL_LOWER_SCALE, DEFAULT_ROW_SCALE);
        numbers.put(DUAL_UPPER_WEIGHT, 600f);
        numbers.put(DUAL_LOWER_WEIGHT, 500f);
        numbers.put(DUAL_LINE_GAP, 0f);
        numbers.put(MARGIN_START, 0f);
        numbers.put(MARGIN_END, 0f);
        numbers.put(DUAL_OFFSET_X, 0f);
        numbers.put(DUAL_OFFSET_Y, 0f);
        numbers.put(DUAL_SPACING, 0f);
        strings.put(DUAL_UPPER_PATTERN, DEFAULT_UPPER_PATTERN);
        strings.put(DUAL_LOWER_PATTERN, DEFAULT_LOWER_PATTERN);
        strings.put(DUAL_ALIGN, ALIGN_CENTER);
        colors.put(DUAL_COLOR_LIGHT, 0xff000000);
        colors.put(DUAL_COLOR_DARK, 0xffffffff);
        BOOLEANS = Collections.unmodifiableMap(switches);
        NUMBERS = Collections.unmodifiableMap(numbers);
        STRINGS = Collections.unmodifiableMap(strings);
        COLORS = Collections.unmodifiableMap(colors);
    }

    private ClockLayoutSettings() { }

    /** Shared by the upper TextView and lower StaticLayout; density is already present in nativePixels. */
    static float rowPixels(float nativePixels, float percent) {
        return NumericPolicy.textPixels(nativePixels * (double) percent / 100d);
    }

    /** Fixed-width digit cells for one clock group. */
    /** The default clock and the two-row clock exclude each other: turning one on names the master to turn off. */
    public static String turnsOff(String key) {
        String status = FeatureOptions.masterKey(STATUS);
        if (DUAL_ENABLED.equals(key)) return status;
        return status.equals(key) ? DUAL_ENABLED : null;
    }

    public static String digitKey(String group) { return group + "_digit_fixed_enabled"; }
    /** Colons drawn at the vertical middle of the digits instead of the font's own, lower, height. */
    public static String colonKey(String group) { return group + "_colon_centered_enabled"; }
    /** Extra width added to every digit cell, in dp. Negative values tighten the cells. */
    public static String digitSpacingKey(String group) { return group + "_digit_spacing"; }

    public static boolean validAlign(Object value) {
        return ALIGN_CENTER.equals(value) || ALIGN_LEFT.equals(value) || ALIGN_RIGHT.equals(value);
    }

    /** 0 = left edge, 0.5 = centered, 1 = right edge of the widest row. */
    public static float alignFraction(String align) {
        return ALIGN_LEFT.equals(align) ? 0f : ALIGN_RIGHT.equals(align) ? 1f : .5f;
    }
}
