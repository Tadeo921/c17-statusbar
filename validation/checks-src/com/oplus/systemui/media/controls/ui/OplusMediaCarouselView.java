package com.oplus.systemui.media.controls.ui;

/** Audited native standard-media background owner; children draw outside onDraw. */
public class OplusMediaCarouselView extends android.widget.LinearLayout {
    public final com.oplus.systemui.notification.aod.OplusPanoramicAodRepository oplusPanoramicAodRepository =
            new com.oplus.systemui.notification.aod.OplusPanoramicAodCoreStartable();
    public int bgScrollerAmount;
    public float contextViewTransitionX;
    public android.graphics.drawable.Drawable panoramicAodDrawable;
    public OplusMediaCarouselView() { super(new android.content.Context()); }
    public void setPanoramicAodDrawable(android.graphics.drawable.Drawable value) { panoramicAodDrawable = value; }
    public void onDraw(android.graphics.Canvas canvas) { if (panoramicAodDrawable != null) panoramicAodDrawable.draw(canvas); }
}
