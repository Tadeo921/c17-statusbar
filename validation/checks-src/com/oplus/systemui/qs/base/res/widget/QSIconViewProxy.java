package com.oplus.systemui.qs.base.res.widget;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import com.android.systemui.plugins.qs.QSTile;
import com.oplus.systemui.qs.base.tile.QSLottieAnimationView;
/** The post-bind native endpoint and an old end-listener can independently overwrite the host. */
public class QSIconViewProxy {
    private final QSLottieAnimationView iconView=new QSLottieAnimationView();private final View iconHostView=iconView;
    private Drawable iconDrawable;private ColorStateList tintList=ColorStateList.valueOf(0xffffffff);
    public boolean animationEnabled=true,isDetailTile;public int scheduledIconTransactionId=-1,iconTransactionId,updates;public boolean loaderPending;
    public static boolean nativeAssetAllowed=true;
    public ImageView getIconView(){return iconView;}
    public static String getLottieAnimAsset(Context context,QSTile.State state){return nativeAssetAllowed&&state.lottiePrefix!=null&&!state.lottiePrefix.isEmpty()?state.lottiePrefix:null;}
    public void setIcon(QSTile.State state,boolean animate){QSTile.Icon icon=state.iconSupplier!=null?state.iconSupplier.get():state.icon;updateIcon(icon,state.isTransient,animate);}
    public void updateIcon(QSTile.Icon icon,boolean isTransient,boolean animate){updates++;scheduledIconTransactionId=-1;iconView.cancelAnimation();if(icon!=null)iconDrawable=animate?icon.getDrawable(iconView.getContext()):icon.getInvisibleDrawable(iconView.getContext());iconView.setImageDrawable(iconDrawable);updateIconTint();}
    public void updateIconTint(){iconView.setAlpha(1f);iconView.setImageTintList(tintList);}
    public void tintForCheck(int color){tintList=ColorStateList.valueOf(color);updateIconTint();}
    public void onRecycle(){iconView.cancelAnimation();iconView.setImageDrawable(null);}
}
