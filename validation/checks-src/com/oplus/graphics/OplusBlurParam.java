package com.oplus.graphics;
/** Audited public native parameter contract; no OEM implementation redistributed. */
public class OplusBlurParam {
    private float[] values = new float[] {1f, 2f, .8f, 4f, .2f, .3f, .4f, .7f,
            .5f, .6f, .7f, .8f, .8f, .7f, .6f, .9f, 3f, 1f};
    public OplusBlurParam() { }
    public float[] toFloatArray() { return values.clone(); }
    public void fromFloatArray(float[] source) { if (source.length != 18) throw new IllegalArgumentException(); values = source.clone(); }
}
