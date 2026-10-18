// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

public final class EasterEggPlaybackCheck {
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
        EasterEggPlayback show = new EasterEggPlayback(0L);
        equal(show.frame(SECOND), 0L, "Background has no playback time");
        expect(!show.canExit() && !show.claimExit(), "Unfinished show cannot complete");
        show.setResumed(true);
        equal(show.frame(2 * SECOND), 0L, "First visible frame establishes a baseline");
        equal(show.frame(4 * SECOND), 2 * SECOND, "Visible frames advance normally");
        show.setResumed(false);
        equal(show.frame(20 * SECOND), 2 * SECOND, "Home or lock pauses the show");
        show.setResumed(true);
        equal(show.frame(40 * SECOND), 2 * SECOND, "Resume excludes the entire background interval");
        expect(!show.canExit(), "Returning from another app must not skip the remaining show");
        equal(show.frame(41 * SECOND), 3 * SECOND, "Playback continues after resume");
        show.setResumed(false);
        show.setResumed(true);
        equal(show.frame(100 * SECOND), 3 * SECOND, "Pause and resume coalesced before recomposition still reset time");
        equal(show.frame(101 * SECOND), 4 * SECOND, "The resumed session advances again");

        // Rotation restores only progress. A new frame clock must never inherit the old timestamp.
        EasterEggPlayback restored = new EasterEggPlayback(4 * SECOND);
        restored.setResumed(true);
        equal(restored.frame(500 * SECOND), 4 * SECOND, "Configuration recreation keeps progress");
        equal(restored.frame(501 * SECOND), 5 * SECOND, "Recreated show continues");
        expect(!restored.canExit(), "Recreated unfinished show stays modal");
        equal(restored.frame(502 * SECOND), EasterEggPlayback.DURATION_NANOS, "Completion is bounded to the show duration");
        expect(restored.canExit() && restored.claimExit(), "Completed show closes once");
        expect(!restored.claimExit(), "Repeated frame/dismiss signals cannot close twice");

        EasterEggPlayback failed = new EasterEggPlayback(SECOND);
        failed.setResumed(true);
        failed.frame(SECOND);
        failed.fail();
        expect(failed.canExit() && failed.claimExit(), "A rendering error safely releases the modal show");
        equal(failed.frame(100 * SECOND), SECOND, "An error cannot restart the animation");
        expect(!failed.claimExit(), "An error cannot repeatedly dispatch completion");

        EasterEggPlayback replay = new EasterEggPlayback(0L);
        replay.setResumed(true);
        equal(replay.frame(1_000 * SECOND), 0L, "A newly triggered show starts at zero");
        expect(!replay.canExit(), "A new show does not inherit an earlier completion or error");
        expect(new EasterEggPlayback(Long.MAX_VALUE).claimExit(), "Restored completed progress is safe to close");
        EasterEggPlayback invalid = new EasterEggPlayback(-1L);
        invalid.setResumed(true);
        equal(invalid.frame(-SECOND), 0L, "Invalid restored progress is clamped");
        equal(invalid.frame(0L), SECOND, "A monotonic frame clock may start below zero");
        equal(invalid.frame(0L), SECOND, "Duplicate frames do not advance");
        equal(invalid.frame(-SECOND), SECOND, "A backwards timestamp does not add progress");
        System.out.println("EasterEggPlaybackCheck: " + checks + " checks passed");
    }
}
