package android.graphics;
/** Desktop fixture: the colours and the centre a running light was built with. */
public class SweepGradient extends Shader {
    public final float cx, cy;
    public final int[] colors;
    public SweepGradient(float cx, float cy, int[] colors, float[] positions) { this.cx = cx; this.cy = cy; this.colors = colors.clone(); }
}
