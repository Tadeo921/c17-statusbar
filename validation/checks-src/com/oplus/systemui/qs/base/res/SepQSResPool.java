package com.oplus.systemui.qs.base.res;

import android.graphics.drawable.Drawable;
import android.view.View;
import com.oplus.systemui.qs.base.res.drawable.MixColorTileDrawable;
import com.oplusos.systemui.common.util.QSBlurConfigProvider;

/** Exact native pool/flow/build names used by the production reflective factory. */
public final class SepQSResPool {
    public static final SepQSResPool INSTANCE = new SepQSResPool();
    public static final class Flow {
        public Object value;
        public Object getValue() { return value; }
    }
    public static final class Builder {
        public int builds, cachedReturns, color = 0xffffffff;
        public boolean fail, reuse, animated;
        public Integer fixedState;
        public View body;
        public Drawable previous;
        public Drawable build(View body, boolean animated, Integer fixedState) {
            builds++; this.body = body; this.animated = animated; this.fixedState = fixedState;
            if (fail) throw new IllegalStateException("factory failure");
            if (reuse) return previous;
            // Real MixColorTileDrawable$Builder returns the body's tagged drawable before creating one.
            Drawable tagged = QSBlurConfigProvider.getBlurDrawableTag(body);
            if (tagged instanceof MixColorTileDrawable) { cachedReturns++; return tagged; }
            MixColorTileDrawable fill = new MixColorTileDrawable(); fill.maskColor = color;
            fill.setBounds(0, 0, body.getWidth(), body.getHeight()); previous = fill;
            QSBlurConfigProvider.setBlurDrawableTag(body, fill); return fill;
        }
    }
    public final Flow normal = new Flow(), theme = new Flow();
    public Flow getTileViewDrawable() { return normal; }
    public Flow getTileThemeDrawable() { return theme; }
}
