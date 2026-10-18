package com.oplus.systemui.plugins.shared.template.component.light;
/** Native f is only an AGSL decorative rectangle, never album art. */
public class f extends android.graphics.drawable.Drawable {
    public int calls;
    public void draw(android.graphics.Canvas canvas){calls++;}
    public void setAlpha(int alpha) { }
    public void setColorFilter(android.graphics.ColorFilter filter) { }
    public int getOpacity(){return android.graphics.PixelFormat.TRANSLUCENT;}
}
