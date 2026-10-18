package com.oplus.systemui.qs.base.tile;
import android.content.Context;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.view.View;
/** The tiles (WLAN, Bluetooth, mobile data, hotspot...) that open a secondary page. */
public class OplusQSHighlightTileView extends View {
    public int mState=1;
    public Drawable mBgDrawable;
    public View mBg;
    public Object iconView;
    public int nativeSpotLights;
    public OplusQSHighlightTileView(){super(new Context());}
    public void updateSpotLightPath(Path path,Object extra){nativeSpotLights++;}
}
