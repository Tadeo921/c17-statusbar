package com.oplus.systemui.notification.blur;
/** Exact native TiltShift owned-view contract; no existing system overlay is borrowed. */
public final class OplusNotificationTiltShiftBlurContainer extends android.widget.FrameLayout {
    private android.graphics.RenderEffect blurRenderEffect;
    public android.graphics.RenderEffect backgroundEffect;
    public static int applyCalls,clearCalls,created;
    public OplusNotificationTiltShiftBlurContainer(android.content.Context context){super(context);created++;}
    @Override protected void onMeasure(int width,int height){setMeasuredDimension(android.view.View.MeasureSpec.getSize(width),android.view.View.MeasureSpec.getSize(height));}
    public void setMaterialBlur(){backgroundEffect=blurRenderEffect;applyCalls++;}
    public void resetMaterialBlur(){backgroundEffect=null;clearCalls++;}
}
