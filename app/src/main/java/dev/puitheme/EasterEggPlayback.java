// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

/** Foreground frame time for one show; saved state contains progress, never a wall-clock baseline. */
final class EasterEggPlayback {
    /** The original single show; themed shows pass their own length. */
    static final long DURATION_NANOS = 5_600_000_000L;
    private final long durationNanos;
    private long elapsedNanos;
    private long lastFrameNanos;
    private boolean hasFrame;
    private boolean resumed;
    private boolean failed;
    private boolean exitClaimed;

    EasterEggPlayback(long restoredNanos) {
        this(restoredNanos, DURATION_NANOS);
    }

    EasterEggPlayback(long restoredNanos, long durationNanos) {
        this.durationNanos = Math.max(1L, durationNanos);
        elapsedNanos = Math.max(0L, Math.min(this.durationNanos, restoredNanos));
    }

    long durationNanos() {
        return durationNanos;
    }

    void setResumed(boolean value) {
        // Reset in the lifecycle callback itself: pause/resume may coalesce before Compose runs again.
        if (resumed != value) hasFrame = false;
        resumed = value;
    }

    long frame(long nowNanos) {
        if (!resumed || canExit()) return elapsedNanos;
        if (hasFrame) {
            if (nowNanos <= lastFrameNanos) return elapsedNanos;
            elapsedNanos += Math.min(durationNanos - elapsedNanos, nowNanos - lastFrameNanos);
        }
        lastFrameNanos = nowNanos;
        hasFrame = true;
        return elapsedNanos;
    }

    void fail() {
        failed = true;
        hasFrame = false;
    }

    boolean canExit() {
        return failed || elapsedNanos >= durationNanos;
    }

    boolean claimExit() {
        if (!canExit() || exitClaimed) return false;
        exitClaimed = true;
        return true;
    }
}
