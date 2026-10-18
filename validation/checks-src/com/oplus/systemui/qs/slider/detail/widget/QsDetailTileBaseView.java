package com.oplus.systemui.qs.slider.detail.widget;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
/** A round button of the brightness / volume secondary page (auto brightness, dark mode ...). */
public class QsDetailTileBaseView extends View {
    public static final class TileState { public int state = 1; }
    public final TileState tileState = new TileState();
    public Drawable bgDrawable;
    public Object iconProxyForCheck;
    public QsDetailTileBaseView(){super(new Context());}
    public Object getState(){return tileState;}
    public Drawable getBgDrawable(){return bgDrawable;}
    public View getIvBg(){return null;}
    public void init(com.oplus.systemui.qs.slider.detail.tile.QsDetailTile tile){}
    public Object getIconViewProxy(){return iconProxyForCheck;}
}
