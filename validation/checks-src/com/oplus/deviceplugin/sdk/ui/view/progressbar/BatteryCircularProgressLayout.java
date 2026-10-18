package com.oplus.deviceplugin.sdk.ui.view.progressbar;
public class BatteryCircularProgressLayout extends android.view.ViewGroup {
    public android.widget.ImageView f,d;
    public Object k;
    public final android.view.View c;
    public int width=124,height=80;
    @Override public int getWidth(){return width;}
    @Override public int getHeight(){return height;}
    public BatteryCircularProgressLayout() {
        super(new android.content.Context());
        f=new android.widget.ImageView(getContext()); d=new android.widget.ImageView(getContext());
        c=new android.view.View(getContext());addView(f);addView(d);addView(c);
        f.setLayoutParams(new LayoutParams(56,56)); d.setLayoutParams(new LayoutParams(-1,0));
    }
    public void setDeviceIcon(int resource){}
    public void setDeviceIconWithAnimation(int resource){}
    public void a(android.graphics.drawable.Drawable graphic){f.setImageDrawable(graphic);}
    public void a(int first,int second){}
    public void onConfigurationChanged(android.content.res.Configuration configuration){}
}
