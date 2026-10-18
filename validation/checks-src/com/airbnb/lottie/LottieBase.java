package com.airbnb.lottie;

import android.content.Context;
import android.graphics.Rect;

/** Desktop fixture for the plugin's Lottie base class: it takes an animation as JSON text (the one-argument method forwards to
 * the two-argument one, JSON then a cache key) and reports the composition it loaded and that composition's length. */
public class LottieBase extends android.widget.ImageView {
    public String json, key;
    public int jsonSets;
    public Composition composition;
    public long duration;

    /** What a loaded animation reports: its bounds. */
    public static final class Composition {
        private final Rect bounds;
        public Composition(int width, int height) { bounds = new Rect(0, 0, width, height); }
        public Rect getBounds() { return bounds; }
    }

    public LottieBase(Context context) { super(context); }
    public void setAnimationFromJson(String json) { setAnimationFromJson(json, null); }
    public void setAnimationFromJson(String json, String key) { this.json = json; this.key = key; jsonSets++; }
    public Composition getComposition() { return composition; }
    public long getDuration() { return duration; }
}
