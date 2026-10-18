package com.heytap.log.collect.auto;

import com.oplus.systemui.plugins.seedling.util.j;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import kotlin.jvm.functions.Function1;

/** Audited native register/start/delete behaviour, with an explicit queue instead of real Android elapsed time. */
public final class d {
    public Object c = new ConcurrentHashMap<String, j>();
    public Runnable queued;
    public int starts;
    public long delay;
    @SuppressWarnings("unchecked") public void L(String key, long duration, int flags, Function1 callback) {
        ((Map<String, j>) c).put(key, new j(duration, new com.oplus.systemui.livealert.classic.source.d(23, callback, key)));
    }
    @SuppressWarnings("unchecked") public void s(long duration, String key) {
        j task = ((Map<String, j>) c).get(key);
        if (task == null) return;
        if (duration > 0) task.b = duration;
        delay = task.b; queued = task.d; starts++;
    }
    public void i(String key) { ((Map<?, ?>) c).remove(key); queued = null; }
    public void fire() { Runnable task = queued; queued = null; if (task != null) task.run(); }
}
