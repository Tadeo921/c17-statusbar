package com.oplus.systemui.plugins.seedling.capsule.strategy;

import com.oplus.systemui.plugins.seedling.capsule.data.model.ContentState;

/** DEX replay: h() requires CAPSULE_STATE + NORMAL + bubble switch; native timeout uses the retained task. */
public final class d {
    public final com.heytap.log.collect.auto.d a = new com.heytap.log.collect.auto.d();
    public String c = "delivery";
    public long e = 5000;
    public b f = new b(this, 1);
    public ContentState g = ContentState.NORMAL;
    public boolean h = true;
    public boolean capsuleState = true;
    public int automatic, manual, exits, removed;
    public d() { a.L("capsuleToBubble", e, 1, f); }
    public boolean h() { return capsuleState && g == ContentState.NORMAL && h; }
    public void arm() { a.s(e, "capsuleToBubble"); }
    public void manualCollapse() { manual++; g = ContentState.MIN; }
    public void remove() { removed++; c = ""; g = ContentState.HIDE; a.i("capsuleToBubble"); }
}
