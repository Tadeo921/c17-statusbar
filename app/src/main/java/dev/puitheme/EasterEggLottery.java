// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

/** Which show the five taps start: five regular styles in turn-free random order, and a 5% hidden one. */
final class EasterEggLottery {
    static final int NONE = -1;
    static final int CYBERPUNK = 0;
    static final int PIXEL = 1;
    static final int ANIME = 2;
    static final int INK = 3;
    static final int COSMOS = 4;
    /** The hidden show. Never chosen by the regular draw, only by the hidden roll. */
    static final int LEGEND = 5;
    static final int REGULAR_COUNT = 5;
    static final double HIDDEN_CHANCE = 0.05;

    private static final long REGULAR_NANOS = 7_200_000_000L;
    private static final long LEGEND_NANOS = 13_000_000_000L;

    private EasterEggLottery() {
    }

    /**
     * Picks a show from two uniform rolls in [0, 1). The hidden roll alone decides the hidden show, so its chance is
     * exactly {@link #HIDDEN_CHANCE} whatever happened before. A regular show never repeats the previous regular one.
     */
    static int pick(double hiddenRoll, double styleRoll, int lastRegular) {
        if (hiddenRoll >= 0.0 && hiddenRoll < HIDDEN_CHANCE) return LEGEND;
        double roll = Double.isNaN(styleRoll) ? 0.0 : Math.max(0.0, Math.min(0.999999999, styleRoll));
        boolean exclude = isRegular(lastRegular);
        int choices = exclude ? REGULAR_COUNT - 1 : REGULAR_COUNT;
        int index = (int) (roll * choices);
        if (exclude && index >= lastRegular) index++;
        return index;
    }

    static boolean isRegular(int theme) {
        return theme >= CYBERPUNK && theme < REGULAR_COUNT;
    }

    static boolean isValid(int theme) {
        return isRegular(theme) || theme == LEGEND;
    }

    static long durationNanos(int theme) {
        return theme == LEGEND ? LEGEND_NANOS : REGULAR_NANOS;
    }

    static String title(int theme) {
        switch (theme) {
            case CYBERPUNK: return "Neón cibernético";
            case PIXEL: return "Arcade de píxeles";
            case ANIME: return "Cielo despejado y sakura";
            case INK: return "Paisaje de tinta china";
            case COSMOS: return "Paseo por la galaxia";
            case LEGEND: return "Leyenda dorada";
            default: return "";
        }
    }
}
