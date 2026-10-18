package com.android.systemui.plugins.qs;
import android.content.Context;
import android.graphics.drawable.Drawable;
import java.util.function.Supplier;
public interface QSTile {
    class State {public String spec="",lottiePrefix="fixture";public int state=1,threeStageMode;public boolean isTransient,disabledByPolicy;public Icon icon;public Supplier<Icon> iconSupplier;}
    class BooleanState extends State {public boolean value;}
    abstract class Icon {public abstract Drawable getDrawable(Context context);public Drawable getInvisibleDrawable(Context context){return getDrawable(context);}}
}
