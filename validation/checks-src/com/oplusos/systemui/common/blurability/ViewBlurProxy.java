package com.oplusos.systemui.common.blurability;
/** Native apply reads raw BlurConfig fields, independently of the drawable path provider. */
public class ViewBlurProxy {
    /** Exact sealed-class name inspected in the current ROM. */
    public static class BlurType {public static final class BlurTypeWindowBg extends BlurType {}}
    public static int motionParamBuildsForCheck;
    public static com.oplus.graphics.OplusBlurParam buildMotionBlurParam(MixColor color,Float weight) {
        motionParamBuildsForCheck++;
        com.oplus.graphics.OplusBlurParam param=new com.oplus.graphics.OplusBlurParam();
        float[] values=new float[18];values[0]=2;values[1]=1;values[2]=1;values[17]=1;
        if(color!=null){values[3]=color.getMode();argbForCheck(values,4,color.getBottomLayerColor());argbForCheck(values,8,color.getTopLayerColor());}
        if(weight!=null&&weight>0)values[16]=weight;
        param.fromFloatArray(values);return param;
    }
    private static void argbForCheck(float[] values,int offset,int color) {
        values[offset]=((color>>16)&255)/255f;values[offset+1]=((color>>8)&255)/255f;
        values[offset+2]=(color&255)/255f;values[offset+3]=(color>>>24)/255f;
    }
    /** Confirmed native private final owner field; mutable here only to exercise recycling. */
    public android.view.View view;
    public BlurConfig blurConfig=new BlurConfig();
    public float visibleScalar;
    public Float visibleWeight;
    public final float[] visibleCorners=new float[4];
    public int applied,failApplyCalls;
    public dev.puitheme.FluidCloudStyle styleForCheck;
    public com.oplusos.systemui.common.blurability.platformblur.PlatformBlurDrawable drawableForCheck=new com.oplusos.systemui.common.blurability.platformblur.PlatformBlurDrawable();
    public Object appliedMixForCheck,appliedStrokeForCheck,appliedShadowForCheck;
    public Object appliedMotionMixForCheck;
    public int backendForCheck,motionAppliesForCheck,wallpaperAppliesForCheck;
    public com.android.internal.graphics.drawable.BackgroundBlurDrawable motionDrawableForCheck=new com.android.internal.graphics.drawable.BackgroundBlurDrawable();
    public com.oplusos.systemui.common.blurability.wallpaper.BlendWallpaperBlurDrawable wallpaperDrawableForCheck=new com.oplusos.systemui.common.blurability.wallpaper.BlendWallpaperBlurDrawable();
    public boolean wallpaperSkipsApplyForCheck;
    public int platformAppliesForCheck;
    public boolean nativeReturnsFallbackForCheck,failDrawableForCheck;
    public ViewBlurProxy(){applyBlurConfig();applied=0;}
    public BlurConfig getBlurConfig(){return blurConfig;}
    public android.view.View getView(){return view;}
    public android.graphics.drawable.Drawable getBlurDrawable(android.graphics.drawable.Drawable fallback){
        if(failDrawableForCheck)throw new IllegalStateException("native platform drawable lookup failed");
        return styleForCheck==null||nativeReturnsFallbackForCheck?fallback:backendForCheck==1?motionDrawableForCheck:backendForCheck==2?wallpaperDrawableForCheck:drawableForCheck;
    }
    public void applyConfigToPlatformBlur(){
        if(styleForCheck!=null&&styleForCheck.deferScenePlatform(this))return;
        AutoCloseable scope=beginForCheck("applyConfigToPlatformBlur");
        try{platformAppliesForCheck++;appliedMixForCheck=blurConfig.platformMixConfig;appliedStrokeForCheck=blurConfig.gradientStrokeLineParam;appliedShadowForCheck=blurConfig.innerShadowParams;
            if(failApplyCalls>0){failApplyCalls--;throw new IllegalStateException("native platform submit failed");}
            if(styleForCheck!=null)styleForCheck.scenePlatformCompleted(scope);
        }finally{if(scope!=null)try{scope.close();}catch(Exception failed){throw new IllegalStateException(failed);}}
    }
    private AutoCloseable beginForCheck(String method){
        try{return styleForCheck==null?null:styleForCheck.beginSceneBackend(this,ViewBlurProxy.class.getDeclaredMethod(method));}
        catch(ReflectiveOperationException failed){throw new IllegalStateException(failed);}
    }
    public void applyConfigToMotionBlur(){
        if(styleForCheck!=null&&styleForCheck.deferScenePlatform(this))return;
        AutoCloseable scope=beginForCheck("applyConfigToMotionBlur");
        try{motionAppliesForCheck++;appliedMotionMixForCheck=blurConfig.motionBlurMixConfig;
            if(failApplyCalls>0){failApplyCalls--;throw new IllegalStateException("native motion submit failed");}
            if(styleForCheck!=null)styleForCheck.scenePlatformCompleted(scope);
        }finally{if(scope!=null)try{scope.close();}catch(Exception failed){throw new IllegalStateException(failed);}}
    }
    public void applyConfigToBlendWallpaper(){
        if(styleForCheck!=null&&styleForCheck.deferScenePlatform(this))return;
        AutoCloseable scope=beginForCheck("applyConfigToBlendWallpaper");
        try{wallpaperAppliesForCheck++;appliedMixForCheck=blurConfig.platformMixConfig;appliedStrokeForCheck=blurConfig.gradientStrokeLineParam;appliedShadowForCheck=blurConfig.innerShadowParams;
            if(failApplyCalls>0){failApplyCalls--;throw new IllegalStateException("native wallpaper submit failed");}
            if(wallpaperDrawableForCheck!=null&&!wallpaperSkipsApplyForCheck){wallpaperDrawableForCheck.currentBlurMixConfig=blurConfig.platformMixConfig;wallpaperDrawableForCheck.hostView=view;}
            if(styleForCheck!=null)styleForCheck.scenePlatformCompleted(scope);
        }finally{if(scope!=null)try{scope.close();}catch(Exception failed){throw new IllegalStateException(failed);}}
    }
    public void applyBlurConfig(){
        applied++;visibleScalar=blurConfig.getCornerRadius();visibleWeight=blurConfig.getRadiusWeight();
        visibleCorners[0]=blurConfig.getLeftTopCornerRadius();visibleCorners[1]=blurConfig.getRightTopCornerRadius();
        visibleCorners[2]=blurConfig.getRightBottomCornerRadius();visibleCorners[3]=blurConfig.getLeftBottomCornerRadius();
        if(failApplyCalls>0){failApplyCalls--;throw new IllegalStateException("native blur apply after mutation");}
        if(styleForCheck!=null){if(backendForCheck==1)applyConfigToMotionBlur();else if(backendForCheck==2)applyConfigToBlendWallpaper();else applyConfigToPlatformBlur();}
    }
}
