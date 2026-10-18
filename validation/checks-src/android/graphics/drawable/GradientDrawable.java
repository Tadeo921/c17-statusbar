package android.graphics.drawable;
import android.graphics.*;
/** Native solid/stroke separation and existing-gradient fixture for background isolation checks. */
public class GradientDrawable extends Drawable {
    public enum Orientation { TOP_BOTTOM,TR_BL,RIGHT_LEFT,BR_TL,BOTTOM_TOP,BL_TR,LEFT_RIGHT,TL_BR }
    public Orientation orientation=Orientation.TOP_BOTTOM;
    public void setOrientation(Orientation value){orientation=value;}
    public Orientation getOrientation(){return orientation;}
    public static final int RECTANGLE=0,OVAL=1,LINE=2,RING=3;
    private int shape=RECTANGLE,fixtureWidth=-1,fixtureHeight=-1;
    public int getShape(){return shape;}
    public void setShape(int value){shape=value;}
    public void setSize(int width,int height){fixtureWidth=width;fixtureHeight=height;}
    public int getIntrinsicWidth(){return fixtureWidth<0?super.getIntrinsicWidth():fixtureWidth;}
    public int getIntrinsicHeight(){return fixtureHeight<0?super.getIntrinsicHeight():fixtureHeight;}
    public static final int LINEAR_GRADIENT=0,RADIAL_GRADIENT=1,SWEEP_GRADIENT=2;
    public int gradientTypeForCheck=LINEAR_GRADIENT;public float gradientRadiusForCheck,gradientCenterXForCheck,gradientCenterYForCheck;
    public void setGradientType(int value){gradientTypeForCheck=value;}
    public int getGradientType(){return gradientTypeForCheck;}
    public void setGradientCenter(float x,float y){gradientCenterXForCheck=x;gradientCenterYForCheck=y;}
    public void setGradientRadius(float value){gradientRadiusForCheck=value;}
    public static final class GradientState {public int[] mColors;public Object mGradientColors;public float[] mPositions;}
    public final GradientState mGradientState=new GradientState();
    public final Paint mFillPaint=new Paint(1),mStrokePaint=new Paint(1);
    public Shader seenShader,seenStroke;
    public int seenAlpha;
    private float cornerRadius;
    private float[] cornerRadii;
    private int drawableAlpha=255;
    private android.content.res.ColorStateList fillColor;
    public android.content.res.ColorStateList tintForCheck;
    private final ConstantState constantState=new ConstantState(){
        public Drawable newDrawable(){
            GradientDrawable copy=new GradientDrawable();
            copy.orientation=orientation;
            copy.shape=shape;copy.fixtureWidth=fixtureWidth;copy.fixtureHeight=fixtureHeight;
            copy.gradientTypeForCheck=gradientTypeForCheck;copy.gradientRadiusForCheck=gradientRadiusForCheck;
            copy.gradientCenterXForCheck=gradientCenterXForCheck;copy.gradientCenterYForCheck=gradientCenterYForCheck;
            copy.cornerRadius=cornerRadius;copy.cornerRadii=cornerRadii==null?null:cornerRadii.clone();
            copy.fillColor=fillColor;copy.drawableAlpha=drawableAlpha;
            copy.tintForCheck=tintForCheck;
            copy.mFillPaint.set(mFillPaint);copy.mStrokePaint.set(mStrokePaint);
            copy.mStrokePaint.strokeWidth=mStrokePaint.strokeWidth;
            copy.mGradientState.mColors=mGradientState.mColors==null?null:mGradientState.mColors.clone();
            copy.mGradientState.mGradientColors=mGradientState.mGradientColors;
            copy.mGradientState.mPositions=mGradientState.mPositions==null?null:mGradientState.mPositions.clone();
            return copy;
        }
    };
    public ConstantState getConstantState(){return constantState;}
    public float getCornerRadius(){return cornerRadius;}
    public float[] getCornerRadii(){return cornerRadii==null?null:cornerRadii.clone();}
    public void setCornerRadius(float radius){cornerRadius=radius;cornerRadii=null;}
    public void setCornerRadii(float[] radii){cornerRadius=0f;cornerRadii=radii==null?null:radii.clone();}
    public void setColor(int value){mFillPaint.setColor(value);fillColor=android.content.res.ColorStateList.valueOf(value);mGradientState.mColors=null;mGradientState.mPositions=null;}
    public void setColor(android.content.res.ColorStateList value){fillColor=value;mGradientState.mColors=null;if(value!=null)mFillPaint.setColor(value.getDefaultColor());}
    public android.content.res.ColorStateList getColor(){return fillColor;}
    /** What the last setStroke call set, for checks (the platform has no public getter). */
    public int strokeWidthForCheck(){return Math.round(mStrokePaint.strokeWidth);}
    public int strokeColorForCheck(){return mStrokePaint.getColor();}
    public int[] getColors(){return mGradientState.mColors==null?null:mGradientState.mColors.clone();}
    public void setColors(int[] value){setColors(value,null);}
    public void setColors(int[] value,float[] positions){mGradientState.mColors=value==null?null:value.clone();mGradientState.mPositions=positions==null?null:positions.clone();fillColor=null;}
    public void setStroke(int width,int color){mStrokePaint.setStrokeWidth(width);mStrokePaint.setColor(color);}
    private void ensureValidRect(){}
    public void draw(Canvas canvas){seenShader=mFillPaint.getShader();seenAlpha=mFillPaint.getAlpha();seenStroke=mStrokePaint.getShader();}
    public void setAlpha(int value){drawableAlpha=value;mFillPaint.setAlpha(value);}
    public int getAlpha(){return drawableAlpha;}
    public void setColorFilter(ColorFilter value){}
    @Override public void setTintList(android.content.res.ColorStateList value){tintForCheck=value;}
    @Override public void setTint(int color){tintForCheck=android.content.res.ColorStateList.valueOf(color);}
    public int getOpacity(){return 0;}
    @Override public boolean isStateful(){
        if(fillColor!=null&&fillColor.isStateful()||tintForCheck!=null&&tintForCheck.isStateful())return true;
        if(mGradientState.mGradientColors instanceof android.content.res.ColorStateList[])
            for(android.content.res.ColorStateList color:(android.content.res.ColorStateList[])mGradientState.mGradientColors)
                if(color!=null&&color.isStateful())return true;
        return false;
    }
}
