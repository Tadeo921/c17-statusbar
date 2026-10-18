package com.oplus.systemui.qs;
public class OplusQSFooterImpl extends android.widget.FrameLayout {
    public android.widget.ImageView mSettingsButtonIcon,mMoreIcon,mMultiUserAvatar;
    public OplusQSFooterImpl(){super(new android.content.Context());
        mSettingsButtonIcon=new android.widget.ImageView(getContext());mMoreIcon=new android.widget.ImageView(getContext());mMultiUserAvatar=new android.widget.ImageView(getContext());
        addView(mSettingsButtonIcon);addView(mMoreIcon);addView(mMultiUserAvatar);
    }
    protected void onFinishInflate(){}
    public void updateResources$11(){}
    public void updateTextColor(int color){}
    public void refreshFooterButtonIconColor(){}
    public boolean updateQsMoreButton(android.content.res.Resources resources,boolean enabled){return enabled;}
}
