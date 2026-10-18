package com.oplus.systemui.qs.media;
public class OplusLottieButton extends android.widget.FrameLayout {
    private final android.widget.ImageView image=new android.widget.ImageView(getContext());
    public final android.view.View effective=new android.view.View(getContext());
    public OplusLottieButton(){super(new android.content.Context());addView(image);addView(effective);}
    public final android.widget.ImageView getImageView(){return image;}
    public final android.view.View getLottieAnimationView(){return effective;}
}
