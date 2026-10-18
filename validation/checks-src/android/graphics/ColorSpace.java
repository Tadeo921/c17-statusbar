package android.graphics;
/** Framework colour-space identity used by native optical parameter copies. */
public final class ColorSpace {
    public enum Named { SRGB, DISPLAY_P3 }
    private static final ColorSpace SRGB=new ColorSpace(),P3=new ColorSpace();
    public static ColorSpace get(Named name){return name==Named.SRGB?SRGB:P3;}
}
