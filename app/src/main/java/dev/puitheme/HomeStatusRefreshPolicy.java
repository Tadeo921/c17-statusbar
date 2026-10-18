// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

/** Main-thread read scheduling. Tickets fence late callbacks across page/lifecycle changes. */
public final class HomeStatusRefreshPolicy {
    public enum Kind { PERFORMANCE, RUNTIME }
    public static final long FAST_MS = 5000L, NORMAL_MS = 30000L, MAX_RETRY_MS = 60000L;
    private final long[] due = new long[2], pending = new long[2];
    private final long[] followUpDue = {-1L, -1L};
    private final int[] failures = new int[2];
    private final boolean[] blocked = new boolean[2];
    private final Object[] published = new Object[2];
    private final boolean[] hasPublished = new boolean[2];
    private boolean visible;
    private long serial;

    public boolean visible() { return visible; }
    /** Immutable semantic state, not a new report object's identity or a polling/loading flag. Retained across page reentry. */
    public boolean publish(Kind kind, Object state) {
        int i = kind.ordinal();
        if (hasPublished[i] && java.util.Objects.equals(published[i], state)) return false;
        hasPublished[i] = true; published[i] = state;
        return true;
    }
    public boolean setVisible(boolean next) {
        if (visible == next) return false;
        visible = next;
        for (int i = 0; i < due.length; i++) {
            due[i] = pending[i] = 0L; followUpDue[i] = -1L; failures[i] = 0; blocked[i] = false;
        }
        return true;
    }
    /** Zero means no new read; an external operation and an in-flight read are never duplicated. */
    public long begin(Kind kind, long now, boolean externalBusy) {
        int i = kind.ordinal();
        if (!visible || externalBusy || blocked[i] || pending[i] != 0L || now < due[i]) return 0L;
        pending[i] = ++serial;
        return pending[i];
    }
    public boolean complete(Kind kind, long ticket, long now, boolean success, boolean changing, boolean permissionDenied) {
        int i = kind.ordinal();
        if (!visible || ticket == 0L || pending[i] != ticket) return false;
        pending[i] = 0L;
        note(kind, now, success, changing, permissionDenied);
        // A read begun before a confirmed action cannot consume that action's follow-up.
        if (followUpDue[i] >= 0L) {
            blocked[i] = false; failures[i] = 0;
            due[i] = Math.min(due[i], followUpDue[i]); followUpDue[i] = -1L;
        }
        return true;
    }
    /** An already authorized action/explicit probe may finish independently of the home polling loop. */
    public void note(Kind kind, long now, boolean success, boolean changing, boolean permissionDenied) {
        int i = kind.ordinal();
        if (!visible || pending[i] != 0L) return;
        if (success) {
            failures[i] = 0; blocked[i] = false;
            due[i] = now + (changing ? FAST_MS : NORMAL_MS);
        } else {
            failures[i] = Math.min(3, failures[i] + 1);
            blocked[i] = permissionDenied;
            due[i] = now + Math.min(MAX_RETRY_MS, 15000L << (failures[i] - 1));
        }
    }
    public void retry(Kind kind, long now) {
        int i = kind.ordinal();
        blocked[i] = false; failures[i] = 0; due[i] = now;
        if (pending[i] != 0L) followUpDue[i] = now;
    }
    /** Re-read after a write without starting a second read if one is already in flight. */
    public void afterAction(Kind kind, long now) {
        int i = kind.ordinal();
        blocked[i] = false; failures[i] = 0; due[i] = now + 1000L;
        if (pending[i] != 0L) followUpDue[i] = due[i];
    }
    /** One bounded Handler callback, no work at all while the relevant page is hidden. */
    public long nextDelay(long now) {
        if (!visible) return -1L;
        long delay = NORMAL_MS;
        boolean work = false;
        for (int i = 0; i < due.length; i++) {
            if (blocked[i]) continue;
            work = true;
            delay = Math.min(delay, pending[i] != 0L ? FAST_MS : Math.max(1000L, due[i] - now));
        }
        return work ? delay : -1L;
    }
}
