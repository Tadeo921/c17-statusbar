// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import java.util.Random;

public final class EasterEggLotteryCheck {
    private static int checks;
    private static final long SECOND = 1_000_000_000L;

    private static void equal(long actual, long expected, String message) {
        checks++;
        if (actual != expected) throw new AssertionError(message + ": " + actual + " != " + expected);
    }

    private static void expect(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        // The hidden roll alone decides the hidden show, exactly below 5%.
        equal(EasterEggLottery.pick(0.0, 0.5, EasterEggLottery.NONE), EasterEggLottery.LEGEND, "Roll 0 is hidden");
        equal(EasterEggLottery.pick(0.0499999, 0.5, EasterEggLottery.CYBERPUNK), EasterEggLottery.LEGEND, "Just under 5% is hidden");
        expect(EasterEggLottery.pick(0.05, 0.5, EasterEggLottery.NONE) != EasterEggLottery.LEGEND, "Exactly 5% is regular");
        expect(EasterEggLottery.pick(Double.NaN, 0.5, EasterEggLottery.NONE) != EasterEggLottery.LEGEND, "NaN never unlocks the hidden show");
        expect(EasterEggLottery.pick(-0.1, 0.5, EasterEggLottery.NONE) != EasterEggLottery.LEGEND, "Negative roll is not hidden");

        // Without a previous show every regular style is reachable, in order of the style roll.
        for (int style = 0; style < EasterEggLottery.REGULAR_COUNT; style++) {
            double roll = (style + 0.5) / EasterEggLottery.REGULAR_COUNT;
            equal(EasterEggLottery.pick(0.5, roll, EasterEggLottery.NONE), style, "First show reaches style " + style);
        }
        // Out-of-range style rolls are clamped to a valid regular show.
        equal(EasterEggLottery.pick(0.5, -3.0, EasterEggLottery.NONE), EasterEggLottery.CYBERPUNK, "Low roll clamps");
        equal(EasterEggLottery.pick(0.5, 1.0, EasterEggLottery.NONE), EasterEggLottery.COSMOS, "Roll 1.0 clamps to the last style");
        equal(EasterEggLottery.pick(0.5, Double.NaN, EasterEggLottery.NONE), EasterEggLottery.CYBERPUNK, "NaN style roll is safe");

        // A regular show never repeats the previous regular one, and every other style stays reachable.
        for (int last = 0; last < EasterEggLottery.REGULAR_COUNT; last++) {
            boolean[] seen = new boolean[EasterEggLottery.REGULAR_COUNT];
            for (int step = 0; step <= 1000; step++) {
                int pick = EasterEggLottery.pick(0.5, step / 1000.0, last);
                expect(EasterEggLottery.isRegular(pick), "Regular roll stays regular");
                expect(pick != last, "Style " + last + " is not repeated");
                seen[pick] = true;
            }
            for (int style = 0; style < EasterEggLottery.REGULAR_COUNT; style++) {
                expect(seen[style] == (style != last), "After " + last + " style " + style + " reachability");
            }
        }
        // The hidden show is not a "previous regular" and excludes nothing.
        equal(EasterEggLottery.pick(0.5, 0.0, EasterEggLottery.LEGEND), EasterEggLottery.CYBERPUNK, "Hidden show excludes nothing");

        // Simulated taps: about 5% hidden, regular styles even, no back-to-back repeats.
        Random random = new Random(17);
        int hidden = 0;
        int[] counts = new int[EasterEggLottery.REGULAR_COUNT];
        int lastRegular = EasterEggLottery.NONE;
        int previous = EasterEggLottery.NONE;
        int trials = 200_000;
        for (int i = 0; i < trials; i++) {
            int pick = EasterEggLottery.pick(random.nextDouble(), random.nextDouble(), lastRegular);
            expect(EasterEggLottery.isValid(pick), "Every pick is a valid show");
            if (pick == EasterEggLottery.LEGEND) hidden++;
            else {
                expect(pick != lastRegular, "No regular repeat in simulation");
                counts[pick]++;
                lastRegular = pick;
            }
            previous = pick;
        }
        double rate = hidden / (double) trials;
        expect(rate > 0.047 && rate < 0.053, "Hidden rate is about 5%: " + rate);
        for (int style = 0; style < EasterEggLottery.REGULAR_COUNT; style++) {
            double share = counts[style] / (double) (trials - hidden);
            expect(share > 0.19 && share < 0.21, "Style " + style + " share is even: " + share);
        }
        expect(previous != EasterEggLottery.NONE, "Simulation ran");

        // Themes, titles and durations.
        expect(!EasterEggLottery.isValid(EasterEggLottery.NONE), "NONE is not a show");
        expect(!EasterEggLottery.isValid(6), "Unknown theme is not a show");
        expect(!EasterEggLottery.isRegular(EasterEggLottery.LEGEND), "Hidden show is not regular");
        for (int theme = 0; theme <= EasterEggLottery.LEGEND; theme++) {
            expect(!EasterEggLottery.title(theme).isEmpty(), "Theme " + theme + " has a title");
            expect(EasterEggLottery.durationNanos(theme) >= 7 * SECOND, "Theme " + theme + " plays at least 7 s");
        }
        expect(EasterEggLottery.durationNanos(EasterEggLottery.LEGEND) > EasterEggLottery.durationNanos(EasterEggLottery.CYBERPUNK),
                "The hidden show is the longest");

        // Playback honours each theme's own length and still closes exactly once.
        EasterEggPlayback legend = new EasterEggPlayback(0L, EasterEggLottery.durationNanos(EasterEggLottery.LEGEND));
        legend.setResumed(true);
        legend.frame(0L);
        equal(legend.frame(8 * SECOND), 8 * SECOND, "Legend still plays after 8 s");
        expect(!legend.canExit(), "Legend is not finished at 8 s");
        equal(legend.frame(20 * SECOND), EasterEggLottery.durationNanos(EasterEggLottery.LEGEND), "Legend is bounded to its own length");
        expect(legend.claimExit() && !legend.claimExit(), "Legend closes once");
        EasterEggPlayback restored = new EasterEggPlayback(Long.MAX_VALUE, EasterEggLottery.durationNanos(EasterEggLottery.PIXEL));
        equal(restored.durationNanos(), EasterEggLottery.durationNanos(EasterEggLottery.PIXEL), "Duration is kept");
        expect(restored.claimExit(), "Restored past-the-end progress closes");
        EasterEggPlayback broken = new EasterEggPlayback(0L, 0L);
        expect(broken.durationNanos() >= 1L, "A zero duration is bounded");
        // The personal line on the hidden card: install days and a stable serial, local data only.
        long day = 86_400_000L;
        long install = 1_760_000_000_000L;
        equal(EasterEggMemento.days(install, install), 1, "Install moment is day 1");
        equal(EasterEggMemento.days(install, install + day - 1L), 1, "Still day 1 before 24 h");
        equal(EasterEggMemento.days(install, install + day), 2, "Next day is day 2");
        equal(EasterEggMemento.days(install, install + 127L * day + 5L), 128, "Day count is inclusive");
        equal(EasterEggMemento.days(install, install - day), 1, "A clock set back still shows day 1");
        equal(EasterEggMemento.days(0L, install), 1, "Unknown install time shows day 1");
        equal(EasterEggMemento.days(1L, Long.MAX_VALUE), EasterEggMemento.MAX_DAYS, "Absurd spans are bounded");
        expect(EasterEggMemento.daysLine(128).equals("你与 C17 的第 128 天"), "Days line text");
        for (long sample = -50_000L; sample < 50_000L; sample += 7L) {
            int serial = EasterEggMemento.serial(install + sample * 997L);
            expect(serial >= 1 && serial <= EasterEggMemento.SERIAL_RANGE, "Serial stays in 1..9999");
        }
        equal(EasterEggMemento.serial(install), EasterEggMemento.serial(install), "Serial is stable for one install");
        expect(EasterEggMemento.serial(install) != EasterEggMemento.serial(install + 1_000L), "Different installs usually differ");
        expect(EasterEggMemento.serialLine(7).equals("No.0007"), "Serial is padded");
        expect(EasterEggMemento.serialLine(9999).equals("No.9999"), "Largest serial");
        System.out.println("EasterEggLotteryCheck: " + checks + " checks passed");
    }
}
