package com.oplus.systemui.livealert.classic.source;

/** DEX replay: TimerTaskSet.L wraps Function1 + key with classId 23. run only invokes it; the task map is retained. */
public final class d implements Runnable {
    public final int b;
    public final Object c, d;
    public d(int branch, Object callback, Object key) { b = branch; c = callback; d = key; }
    @Override public void run() {
        if (b == 23) ((kotlin.jvm.functions.Function1) c).invoke(d);
    }
}
