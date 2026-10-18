// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie

package dev.puitheme;

/** Fit the looping names into the space left by the measured cards; no names are removed when fewer lanes fit. */
final class ThanksLaneBudget {
    static int lanes(int people, int available, int height, int gap, int padding) {
        int desired = people <= 6 ? 2 : people <= 16 ? 3 : people <= 30 ? 4 : 5;
        desired = Math.max(1, Math.min(desired, people));
        long row = Math.max(1, height), spacing = Math.max(0, gap);
        long room = (long) available - Math.max(0, padding) + spacing;
        int fits = (int) Math.max(1L, Math.min(5L, room / (row + spacing)));
        return Math.min(desired, fits);
    }

    private ThanksLaneBudget() {}
}
