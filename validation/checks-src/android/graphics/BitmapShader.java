package android.graphics;
public class BitmapShader extends Shader {
    public static int creations;
    public static final int FILTER_MODE_DEFAULT=0,FILTER_MODE_NEAREST=1,FILTER_MODE_LINEAR=2;
    public int filterMode=FILTER_MODE_DEFAULT,filterModeWrites;
    public final Bitmap bitmap; public Matrix matrix;
    public final TileMode tileX,tileY;
    public BitmapShader(Bitmap bitmap,TileMode x,TileMode y){this.bitmap=bitmap;tileX=x;tileY=y;creations++;}
    public void setFilterMode(int value){filterMode=value;filterModeWrites++;}
    /** Android Shader copies the supplied local matrix; callers retain ownership. */
    public void setLocalMatrix(Matrix value){
        if(matrix==null)matrix=new Matrix();
        if(value==null)matrix.reset();else matrix.set(value);
        localMatrix=matrix;localMatrixSets++;
    }
}
