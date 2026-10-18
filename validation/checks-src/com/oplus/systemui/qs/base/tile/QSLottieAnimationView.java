package com.oplus.systemui.qs.base.tile;
public class QSLottieAnimationView extends android.widget.ImageView {
    public int cancelledLoaderTasks,nativeCancels,nativeSetCompositions;public boolean autoPlay=true;
    public QSLottieAnimationView(){super(new android.content.Context());}
    public void cancelLoaderTask(){cancelledLoaderTasks++;}
    public void cancelAnimation(){nativeCancels++;}
    public void setComposition(com.airbnb.lottie.LottieComposition composition){nativeSetCompositions++;}
}
