// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie

package dev.puitheme;

/**
 * iOS 27 battery style values. Device code162: the author asked for PUI's logic, so the style is
 * drawn by {@link PuiBatteryStyle} with the c17_ios27_battery_* outline in the same native slots,
 * rects, 29x17dp box and native percentage text; it also applies without a system percentage.
 */
public final class Ios27BatteryStyle {
    public static final String STYLE = "ios27";
    /** code161 test values, still accepted from saved or imported settings. */
    static final String NUMBER = "ios27_number", PLAIN = "ios27_plain";
    private Ios27BatteryStyle() { }

    public static boolean known(String value) { return STYLE.equals(value) || NUMBER.equals(value) || PLAIN.equals(value); }
}
