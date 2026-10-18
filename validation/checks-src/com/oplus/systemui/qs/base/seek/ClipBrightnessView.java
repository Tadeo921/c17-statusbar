package com.oplus.systemui.qs.base.seek;
public class ClipBrightnessView extends android.view.View {
    private android.graphics.drawable.Drawable brightnessIcon;
    public boolean auto;
    public ClipBrightnessView(){super(new android.content.Context());}
    public android.graphics.drawable.Drawable getBrightnessIcon(){return brightnessIcon;}
    public void setBrightnessIcon(android.graphics.drawable.Drawable value){brightnessIcon=value;}
    public boolean getAutoMode(){return auto;}
    public void init(){}
    public void updateIconColor(){}
    public void updateIconConfig(){}
    public void disableTheme(boolean value){}
    public void setAutoMode(boolean value){auto=value;}
    public void setIconColorFilter(int color,android.graphics.PorterDuff.Mode mode){}
    private android.graphics.drawable.Drawable getBrightnessIconMode(){return brightnessIcon;}
}
