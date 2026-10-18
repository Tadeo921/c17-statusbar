package com.oplus.systemui.qs.base.res.drawable;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import com.oplusos.systemui.common.outline.CornerOutlineProvider;
/** Background of the highlight tiles: its own class (a DrawableWrapper on the device), same path-provider interface. */
public class TileDrawableWrapper extends Drawable {
    public static final class Delegate {
        private CornerOutlineProvider pathProvider,blockPathProvider;
        Delegate(CornerOutlineProvider provider){pathProvider=provider;}
    }
    private final Delegate delegate;
    public int setters,pathUpdates;
    public TileDrawableWrapper(CornerOutlineProvider provider){delegate=new Delegate(provider);setBounds(0,0,240,120);}
    public CornerOutlineProvider getPathProvider(){return delegate.blockPathProvider!=null?delegate.blockPathProvider:delegate.pathProvider;}
    public float getCornerRadius(){return getPathProvider().getCornerRadius(this);}
    public Float getCornerWeight(){return getPathProvider().getCornerWeight(this);}
    public void setPathProvider(CornerOutlineProvider next){setters++;delegate.pathProvider=next;}
    public void invalidatePath(){pathUpdates++;}
    public void setBlockPathProvider(CornerOutlineProvider block){delegate.blockPathProvider=block;invalidatePath();}
    public void draw(Canvas canvas){}
    public void setAlpha(int value){}
    public void setColorFilter(ColorFilter filter){}
    public int getOpacity(){return -3;}
}
