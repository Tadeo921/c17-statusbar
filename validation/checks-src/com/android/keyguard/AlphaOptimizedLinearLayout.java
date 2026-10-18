package com.android.keyguard;

import android.content.res.Resources;
import android.view.View;

/** Exact OEM anonymous notification wrapper class; native geometry only. */
public class AlphaOptimizedLinearLayout extends android.view.ViewGroup {
    private final Resources resources;
    private int left, top, right, bottom;
    public AlphaOptimizedLinearLayout(Resources resources, int width, int height) {
        super(null); this.resources = resources; setId(View.NO_ID); layout(0, 0, width, height);
    }
    @Override public Resources getResources() { return resources; }
    @Override public int getLeft() { return left; }
    @Override public int getRight() { return right; }
    @Override public int getTop() { return top; }
    @Override public int getBottom() { return bottom; }
    @Override public int getWidth() { return right - left; }
    @Override public int getHeight() { return bottom - top; }
    @Override public void layout(int l, int t, int r, int b) { left = l; top = t; right = r; bottom = b; }
    @Override protected void onMeasure(int width, int height) {
        setMeasuredDimension(View.resolveSize(getWidth(), width), View.resolveSize(getHeight(), height));
    }
}
