package android.graphics;
/** Desktop fixture: what a radial gradient was built with. */
public class RadialGradient extends Shader {
    public final float cx, cy, radius;
    public final int color0, color1;
    public final int[] colors;
    public final float[] positions;
    public final TileMode tileMode;
    public RadialGradient(float cx, float cy, float radius, int color0, int color1, TileMode mode) {
        this(cx, cy, radius, new int[]{color0, color1}, null, mode);
    }
    public RadialGradient(float cx, float cy, float radius, int[] colors, float[] positions, TileMode mode) {
        this.cx = cx; this.cy = cy; this.radius = radius; this.colors = colors.clone(); this.positions = positions == null ? null : positions.clone();
        this.color0 = colors[0]; this.color1 = colors[colors.length - 1]; this.tileMode = mode;
    }
}
