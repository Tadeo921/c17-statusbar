// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie

package dev.puitheme;

/** A local glyph colour mask. The original text draw keeps its shape, alpha,
 * animation and clip; no backdrop, extra drawing layer or GPU effect is owned. */
final class ClockColorMask {
    private ClockColorMask() { }

    /** Mask alpha, selected opacity and strength form one interpolation weight.
     * Text alpha belongs to the original glyph and is never multiplied twice. */
    static int apply(int textColor, int maskColor, float opacity, float amount) {
        double weight = ((maskColor >>> 24) / 255d) * unit(opacity) * unit(amount);
        if (weight <= 0d) return textColor;
        int red = channel(textColor >>> 16, maskColor >>> 16, weight);
        int green = channel(textColor >>> 8, maskColor >>> 8, weight);
        int blue = channel(textColor, maskColor, weight);
        return (textColor & 0xff000000) | (red << 16) | (green << 8) | blue;
    }

    private static double unit(float value) {
        return Float.isFinite(value) ? Math.max(0d, Math.min(1d, value)) : 0d;
    }

    private static int channel(int text, int mask, double weight) {
        int original = text & 255;
        return (int) Math.round(original + ((mask & 255) - original) * weight);
    }
}
