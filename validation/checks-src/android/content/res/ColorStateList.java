package android.content.res;
public class ColorStateList {
    private final int color;
    private final boolean stateful;
    private ColorStateList(int color){this.color=color;this.stateful=false;}
    public ColorStateList(int[][] states,int[] colors){this.color=colors[colors.length-1];this.stateful=states.length>1;}
    public static ColorStateList valueOf(int color){return new ColorStateList(color);}
    public int getDefaultColor(){return color;}
    public boolean isStateful(){return stateful;}
    public int getColorForState(int[] state,int fallback){return color;}
}
