package android.graphics;
public final class Color {
    public static final int WHITE=0xffffffff,TRANSPARENT=0;
    private final float r,g,b,a;private final ColorSpace space;
    private Color(float r,float g,float b,float a,ColorSpace space){this.r=r;this.g=g;this.b=b;this.a=a;this.space=space;}
    public static Color valueOf(int value){return new Color(red(value)/255f,green(value)/255f,blue(value)/255f,alpha(value)/255f,ColorSpace.get(ColorSpace.Named.SRGB));}
    public static Color valueOf(float r,float g,float b,float a,ColorSpace space){return new Color(r,g,b,a,space);}
    public float red(){return r;}public float green(){return g;}public float blue(){return b;}public float alpha(){return a;}
    public ColorSpace getColorSpace(){return space;}
    public Color convert(ColorSpace target){return new Color(r,g,b,a,target);}
    public int toArgb(){return Math.round(a*255)<<24|Math.round(r*255)<<16|Math.round(g*255)<<8|Math.round(b*255);}
    public static int alpha(int value) { return value >>> 24; }
    public static int red(int value) { return (value >>> 16) & 255; }
    public static int green(int value) { return (value >>> 8) & 255; }
    public static int blue(int value) { return value & 255; }
}
