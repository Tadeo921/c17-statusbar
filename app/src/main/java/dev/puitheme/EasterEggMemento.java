// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

/**
 * The personal line on the hidden show's card: how many days this install has been with the user, and a limited
 * serial number. Both come from the local first-install time only; nothing is stored, sent or read from elsewhere.
 */
final class EasterEggMemento {
    private static final long DAY_MILLIS = 86_400_000L;
    static final int MAX_DAYS = 99_999;
    static final int SERIAL_RANGE = 9_999;

    private EasterEggMemento() {
    }

    /** Day 1 is the install day. Unknown or future install times (clock changes) still show day 1. */
    static int days(long installMillis, long nowMillis) {
        if (installMillis <= 0L || nowMillis <= installMillis) return 1;
        long days = (nowMillis - installMillis) / DAY_MILLIS + 1L;
        return (int) Math.min(MAX_DAYS, days);
    }

    /** A stable number from 1 to 9999 for this install, so the same phone always shows the same serial. */
    static int serial(long installMillis) {
        long mixed = installMillis * 0x9E3779B97F4A7C15L;
        mixed ^= mixed >>> 31;
        return (int) Math.floorMod(mixed, (long) SERIAL_RANGE) + 1;
    }

    static String daysLine(int days) {
        return "Llevas " + days + " días con C17";
    }

    static String serialLine(int serial) {
        String digits = Integer.toString(serial);
        StringBuilder padded = new StringBuilder("No.");
        for (int i = digits.length(); i < 4; i++) padded.append('0');
        return padded.append(digits).toString();
    }
}
