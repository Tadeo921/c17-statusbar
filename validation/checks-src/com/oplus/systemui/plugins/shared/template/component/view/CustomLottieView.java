package com.oplus.systemui.plugins.shared.template.component.view;

import android.content.Context;
import android.graphics.drawable.Drawable;

/** The audited plugin view that plays the canned spectrum: an ImageView with setAnimation(int) and its own setImageDrawable. */
public class CustomLottieView extends com.airbnb.lottie.LottieBase {
    public int animation;
    public int drawableSets;
    public CustomLottieView(Context context) { super(context); }
    public void setAnimation(int resource) { animation = resource; }
    @Override public void setImageDrawable(Drawable drawable) { drawableSets++; super.setImageDrawable(drawable); }
}
