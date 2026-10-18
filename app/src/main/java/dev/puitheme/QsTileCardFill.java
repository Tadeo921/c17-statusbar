// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;
import java.lang.ref.WeakReference;
import java.util.Objects;

/** The SDK's complete connected-card material, fitted to the existing tile wrapper. */
final class QsTileCardFill extends Drawable implements Drawable.Callback {
    private final Drawable material;
    private final WeakReference<Drawable> wrapper;
    private float radius=Float.NaN;
    private Float weight;
    private int alpha=255;

    static QsTileCardFill create(View tile,View body,Drawable wrapper) throws ReflectiveOperationException {
        ClassLoader loader=tile.getClass().getClassLoader();
        Class<?> factory=Class.forName("com.oplus.deviceplugin.sdk.ui.view.drawable.b",false,loader);
        Class<?> states=Class.forName("com.oplus.deviceplugin.sdk.ui.view.drawable.c",false,loader);
        // Current SDK USABLE selects SepQSActive and derives night/material/stroke support itself.
        // Only native state==2 reaches this branch; inactive/unavailable retain their original body.
        Object usable=states.getField("b").get(null);
        Object created=QsTileAppearance.invoke(factory,"a",body.getContext(),body,usable);
        if(!(created instanceof Drawable)||!QsTileAppearance.type(created,"com.oplus.systemui.plugins.drawable.PluginDrawable"))
            throw new IllegalStateException("Native connected-card material unavailable");
        QsTileCardFill result=new QsTileCardFill((Drawable)created,wrapper);
        result.setBounds(wrapper.getBounds());result.syncShape();
        return result;
    }
    private QsTileCardFill(Drawable material,Drawable wrapper) {
        this.material=material;this.wrapper=new WeakReference<>(wrapper);material.setCallback(this);
    }
    /** Allows the existing appearance owner registration to reach the native AutoBlur child. */
    public Drawable getDrawable(){return material;}
    private void syncShape() throws ReflectiveOperationException {
        Drawable source=wrapper.get();
        // The verified wrapper APIs resolve its current provider themselves. Reading the
        // radius/weight directly avoids a third reflective call on every draw.
        Object r=source==null?null:QsTileAppearance.call(source,"getCornerRadius");
        Object w=source==null?null:QsTileAppearance.call(source,"getCornerWeight");
        if(!(r instanceof Number))throw new IllegalStateException("Native tile shape unavailable");
        float next=((Number)r).floatValue();Float nextWeight=w instanceof Number?((Number)w).floatValue():null;
        if(!Float.isFinite(next)||next<0f)throw new IllegalStateException("Native tile radius invalid");
        next=Math.min(next,Math.min(getBounds().width(),getBounds().height())/2f);
        if(next==radius&&Objects.equals(nextWeight,weight))return;
        QsTileAppearance.invoke(material,"setCornerRadius",next,nextWeight);
        // As on SDK device cards, the blur backend also owns five independent corner fields.
        Object auto=QsTileAppearance.field(material,"autoBlurDrawable");
        Object proxy=QsTileAppearance.call(auto,"getViewBlurProxy"),config=QsTileAppearance.call(proxy,"getBlurConfig");
        if(config!=null){QsTileAppearance.invoke(config,"setCornerRadius",next);QsTileAppearance.invoke(proxy,"applyBlurConfig");}
        radius=next;weight=nextWeight;
    }
    @Override public void draw(Canvas canvas) {
        // Shape providers can change in place when the user's corner setting changes.
        try{syncShape();}catch(ReflectiveOperationException|RuntimeException unavailable){/* Retain last native material. */}
        material.draw(canvas);
    }
    @Override protected void onBoundsChange(Rect bounds){material.setBounds(bounds);radius=Float.NaN;}
    @Override public void setAlpha(int value){alpha=value;material.setAlpha(value);}
    @Override public int getAlpha(){return alpha;}
    @Override public void setColorFilter(ColorFilter filter){material.setColorFilter(filter);}
    @Override public int getOpacity(){return material.getOpacity();}
    @Override public boolean isStateful(){return material.isStateful();}
    @Override protected boolean onStateChange(int[] state){return material.setState(state);}
    @Override protected boolean onLevelChange(int level){return material.setLevel(level);}
    @Override public void invalidateDrawable(Drawable drawable){invalidateSelf();}
    @Override public void scheduleDrawable(Drawable drawable,Runnable task,long when){scheduleSelf(task,when);}
    @Override public void unscheduleDrawable(Drawable drawable,Runnable task){unscheduleSelf(task);}
}
