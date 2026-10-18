package com.oplus.posteffect.drawable;
/** Actual SystemUI DEX inheritance, with Blend's onDrawContent inherited. */
public class ContinuousBlurDrawable extends BlendDrawable {
    public static final java.util.List<ContinuousBlurDrawable> registeredForCheck=new java.util.ArrayList<>();
    public static int nextId,removeCalls,drawCalls;public static boolean failAdd,failRemove;
    public final int drawableId=++nextId;
    public android.view.SurfaceControl surfaceControl;
    public com.oplus.posteffect.BlurParam params=new com.oplus.posteffect.BlurParam();
    public com.oplus.posteffect.BlurParam submittedForCheck;
    public final java.util.List<com.oplus.posteffect.BlurParam> submissionsForCheck=new java.util.ArrayList<>();
    public Object foreground;
    public final android.graphics.Rect curPosition=new android.graphics.Rect(0,0,240,100);
    public final int[] windowOffsetOnScreen=new int[2];
    public int rotation,screenWidth=240,screenHeight=100,viewAxisRemapRotation;
    public boolean viewAxisRemapEnabled,isRegistered;
    public String state="LOST";
    public final android.graphics.Matrix bitmapMatrix=new android.graphics.Matrix();
    public ImageInfo pending,drawing;
    public int paramReadsForCheck,imageReadsForCheck,matrixReadsForCheck,enabledReadsForCheck;
    public android.view.SurfaceControl getSurfaceControl(){return surfaceControl;}
    public com.oplus.posteffect.BlurParam getDrawingParam(){paramReadsForCheck++;return params;}
    public android.graphics.Rect getCurPosition(){return curPosition;}
    public String getCurPositionState(){return state;}public void setCurPositionState(String value){state=value;}
    public void setEnableBlurShader(boolean value){enableShader=value;}public boolean getEnableShader(){enabledReadsForCheck++;return enableShader;}
    public int getDrawableId(){return drawableId;}
    public Object getForegroundBlurParams(){return foreground;}public void setForegroundBlurParams(Object value){foreground=value;}
    public void setBlurParamsSync(com.oplus.posteffect.BlurParam value){params=value.detachedCopy$app_sdk_release();
        if(isRegistered){if(!params.blend.empty())throw new AssertionError("private update contains mix");submissionsForCheck.add(params.detachedCopy$app_sdk_release());}
        if(!"LOST".equals(state)&&!isRegistered)addBlurDrawable();}
    public void setBlurParamsInternal(com.oplus.posteffect.BlurParam value,com.oplus.posteffect.ForegroundBlurParam front,boolean synchronous){params=value;foreground=front;}
    public void syncBlurParams(boolean positionChanged,boolean synchronous){params.toFloatArray(enableShader);}
    public void addBlurDrawable(){if(failAdd)throw new IllegalStateException("add failed");if(isRegistered)return;
        if(!params.blend.empty()||foreground!=null||!enableShader)throw new AssertionError("private first request contains mix");
        submittedForCheck=params.detachedCopy$app_sdk_release();submissionsForCheck.add(submittedForCheck);registeredForCheck.add(this);isRegistered=true;}
    public void removeBlurDrawable(boolean keep){removeCalls++;if(failRemove)throw new IllegalStateException("remove failed");isRegistered=false;registeredForCheck.remove(this);pending=drawing=null;}
    public boolean preDrawContent(){return super.preDrawContent()&&isRegistered;}
    public ImageInfo getDrawingImageInfo(){imageReadsForCheck++;return drawing;}public android.graphics.Matrix getBitmapMatrix(){matrixReadsForCheck++;return bitmapMatrix;}
    public void setMatrixDirty(boolean value){}
    public void completeForCheck(android.graphics.Bitmap bitmap){pending=new ImageInfo(bitmap);invalidateSelf();}
    @Override public void draw(android.graphics.Canvas canvas){drawCalls++;}
    public static final class ImageInfo extends BaseDrawable.ImageInfo {
        public ImageInfo(android.graphics.Bitmap bitmap){super(bitmap);}
    }
}
