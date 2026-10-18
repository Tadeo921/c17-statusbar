package com.oplus.systemui.plugins.seedling.capsule.strategy;

import com.oplus.systemui.plugins.seedling.capsule.data.model.ContentState;
import java.util.function.BiPredicate;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

/** DEX replay of the shared Function1: branch 0 is state-machine exit, branch 1 is capsule-to-bubble timeout. */
public final class b implements Function1 {
    public final int b;
    public final d c;
    public static BiPredicate<Object, Object> beforeForCheck;
    public b(d owner, int branch) { b = branch; c = owner; }
    @Override public Object invoke(Object value) {
        if (beforeForCheck != null && beforeForCheck.test(this, value)) return Unit.a;
        if (b == 0) { c.exits++; return Unit.a; }
        if (!(value instanceof String)) throw new ClassCastException();
        if (!c.c.isEmpty() && c.g == ContentState.NORMAL && c.h) { c.automatic++; c.g = ContentState.MIN; }
        return Unit.a;
    }
}
