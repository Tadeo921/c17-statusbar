package android.graphics;
public class BlendModeColorFilter extends ColorFilter {
    private final int color;
    public BlendModeColorFilter(int color,BlendMode mode){this.color=color;}
    public int getColor(){return color;}
}
