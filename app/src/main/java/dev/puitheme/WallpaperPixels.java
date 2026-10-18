// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

/**
 * The pixel work of the pull-down wallpaper on plain ARGB arrays: a Gaussian-like blur, the clock zone's blend between two blurs, and
 * brightness. No Android classes, so the maths is checked on the desktop. All of it runs on the wallpaper's own worker thread, on small
 * copies of the picture (a few hundred pixels wide), never while the shade is drawn.
 */
final class WallpaperPixels {
    private WallpaperPixels() { }

    /**
     * Three box passes in each direction: close to a Gaussian whose standard deviation is {@code sigma} pixels, which is what a window or
     * render-effect blur radius means. Edges repeat their outermost pixel. Every channel, alpha included, is treated alike.
     */
    static void blur(int[] pixels, int width, int height, float sigma) {
        if (pixels == null || width < 2 || height < 2 || pixels.length < width * height || !(sigma >= .5f)) return;
        int radius = Math.max(1, Math.round(sigma));
        int[] scratch = new int[width * height];
        for (int pass = 0; pass < 3; pass++) {
            boxHorizontal(pixels, scratch, width, height, radius);
            boxVertical(scratch, pixels, width, height, radius);
        }
    }

    private static void boxHorizontal(int[] source, int[] target, int width, int height, int radius) {
        int window = 2 * radius + 1;
        for (int y = 0; y < height; y++) {
            int row = y * width;
            int a = 0, r = 0, g = 0, b = 0;
            for (int i = -radius; i <= radius; i++) {
                int p = source[row + clamp(i, width)];
                a += p >>> 24; r += (p >> 16) & 255; g += (p >> 8) & 255; b += p & 255;
            }
            for (int x = 0; x < width; x++) {
                target[row + x] = pack(a, r, g, b, window);
                int out = source[row + clamp(x - radius, width)], in = source[row + clamp(x + radius + 1, width)];
                a += (in >>> 24) - (out >>> 24); r += ((in >> 16) & 255) - ((out >> 16) & 255);
                g += ((in >> 8) & 255) - ((out >> 8) & 255); b += (in & 255) - (out & 255);
            }
        }
    }

    private static void boxVertical(int[] source, int[] target, int width, int height, int radius) {
        int window = 2 * radius + 1;
        for (int x = 0; x < width; x++) {
            int a = 0, r = 0, g = 0, b = 0;
            for (int i = -radius; i <= radius; i++) {
                int p = source[clamp(i, height) * width + x];
                a += p >>> 24; r += (p >> 16) & 255; g += (p >> 8) & 255; b += p & 255;
            }
            for (int y = 0; y < height; y++) {
                target[y * width + x] = pack(a, r, g, b, window);
                int out = source[clamp(y - radius, height) * width + x], in = source[clamp(y + radius + 1, height) * width + x];
                a += (in >>> 24) - (out >>> 24); r += ((in >> 16) & 255) - ((out >> 16) & 255);
                g += ((in >> 8) & 255) - ((out >> 8) & 255); b += (in & 255) - (out & 255);
            }
        }
    }

    private static int clamp(int index, int size) { return index < 0 ? 0 : index >= size ? size - 1 : index; }
    private static int pack(int a, int r, int g, int b, int window) {
        return ((a + window / 2) / window) << 24 | ((r + window / 2) / window) << 16 | ((g + window / 2) / window) << 8 | (b + window / 2) / window;
    }

    /**
     * How much of the zone's blur shows at a row: all of it down to {@code solidBottom}, then fading to none over {@code feather} rows. A
     * smooth S-curve, so the join between the two blurs has no visible band.
     */
    static float zoneWeight(float row, float solidBottom, float feather) {
        if (!(row >= 0f) || !(solidBottom > 0f) && !(feather > 0f)) return 0f;
        if (row <= solidBottom) return 1f;
        if (!(feather > 0f) || row >= solidBottom + feather) return 0f;
        float t = (row - solidBottom) / feather;
        return 1f - t * t * (3f - 2f * t);
    }

    /**
     * The picture as it is shown behind the notification centre: the main blur everywhere, the zone's blur at the top, blended down by
     * {@link #zoneWeight}. {@code top} is the first row of the picture that the screen shows (the picture is cropped to the screen's shape),
     * {@code rowsPerScreenPixel} the picture rows that one screen pixel covers.
     */
    static int[] zoneComposite(int[] main, int[] zone, int width, int height, float top, float rowsPerScreenPixel, float zoneBottomScreen, float featherScreen) {
        int[] result = new int[width * height];
        float solid = zoneBottomScreen * rowsPerScreenPixel, feather = featherScreen * rowsPerScreenPixel;
        for (int y = 0; y < height; y++) {
            float w = zoneWeight(y - top, solid, feather);
            int row = y * width;
            if (w <= 0f) { System.arraycopy(main, row, result, row, width); continue; }
            if (w >= 1f) { System.arraycopy(zone, row, result, row, width); continue; }
            int weight = Math.round(w * 256f);
            for (int x = 0; x < width; x++) result[row + x] = mix(main[row + x], zone[row + x], weight);
        }
        return result;
    }

    /** {@code weight} of 0..256 of {@code second} over the rest of {@code first}. */
    static int mix(int first, int second, int weight) {
        int rest = 256 - weight;
        int a = ((first >>> 24) * rest + (second >>> 24) * weight + 128) >> 8;
        int r = (((first >> 16) & 255) * rest + ((second >> 16) & 255) * weight + 128) >> 8;
        int g = (((first >> 8) & 255) * rest + ((second >> 8) & 255) * weight + 128) >> 8;
        int b = ((first & 255) * rest + (second & 255) * weight + 128) >> 8;
        return a << 24 | r << 16 | g << 8 | b;
    }

    /** 100 keeps the picture, below it darkens, above it lightens (the same curve as the settings preview). Alpha is untouched. */
    static void brightness(int[] pixels, float percent) {
        if (pixels == null || percent == 100f || !Float.isFinite(percent)) return;
        float multiplier = percent <= 100f ? percent / 100f : 2f - percent / 100f;
        float offset = percent > 100f ? (percent - 100f) * 2.55f : 0f;
        for (int i = 0; i < pixels.length; i++) {
            int p = pixels[i];
            int r = Math.max(0, Math.min(255, Math.round(((p >> 16) & 255) * multiplier + offset)));
            int g = Math.max(0, Math.min(255, Math.round(((p >> 8) & 255) * multiplier + offset)));
            int b = Math.max(0, Math.min(255, Math.round((p & 255) * multiplier + offset)));
            pixels[i] = (p & 0xff000000) | r << 16 | g << 8 | b;
        }
    }

    /** A signed lift: {@code percent} above 0 blends every colour toward white by that share, below 0 toward black. Alpha is untouched. */
    static void lift(int[] pixels, float percent) {
        if (pixels == null || percent == 0f || !Float.isFinite(percent)) return;
        float share = Math.min(1f, Math.abs(percent) / 100f), target = percent > 0f ? 255f : 0f;
        for (int i = 0; i < pixels.length; i++) {
            int p = pixels[i];
            int r = Math.round(((p >> 16) & 255) * (1f - share) + target * share);
            int g = Math.round(((p >> 8) & 255) * (1f - share) + target * share);
            int b = Math.round((p & 255) * (1f - share) + target * share);
            pixels[i] = (p & 0xff000000) | r << 16 | g << 8 | b;
        }
    }

    /** 100 keeps the colours, 0 is grey, 200 doubles how far every colour is from its grey. Alpha is untouched. */
    static void saturation(int[] pixels, float percent) {
        if (pixels == null || percent == 100f || !Float.isFinite(percent)) return;
        float factor = Math.max(0f, Math.min(200f, percent)) / 100f;
        for (int i = 0; i < pixels.length; i++) {
            int p = pixels[i];
            float r = (p >> 16) & 255, g = (p >> 8) & 255, b = p & 255;
            float grey = .299f * r + .587f * g + .114f * b;
            int nr = Math.max(0, Math.min(255, Math.round(grey + (r - grey) * factor)));
            int ng = Math.max(0, Math.min(255, Math.round(grey + (g - grey) * factor)));
            int nb = Math.max(0, Math.min(255, Math.round(grey + (b - grey) * factor)));
            pixels[i] = (p & 0xff000000) | nr << 16 | ng << 8 | nb;
        }
    }

    /** {@code argb} laid over every pixel, its alpha being the strength. Alpha of the picture is untouched. */
    static void tint(int[] pixels, int argb) {
        int strength = argb >>> 24;
        if (pixels == null || strength == 0) return;
        for (int i = 0; i < pixels.length; i++) {
            int p = pixels[i];
            int mixed = mix(p | 0xff000000, argb | 0xff000000, strength + (strength >> 7));
            pixels[i] = (p & 0xff000000) | (mixed & 0x00ffffff);
        }
    }

    /** The system's own curve for how the pull-down's blur and dim come in with the panel's expansion (0..1): nothing for the first sixth. */
    static float pullCurve(float fraction) {
        if (!Float.isFinite(fraction)) return 0f;
        float f = Math.max(0f, Math.min(1f, fraction)) * 1.2f - 0.2f;
        if (f <= 0f) return 0f;
        return (float) (1d - .5d * (1d - Math.cos(Math.PI * Math.pow(1d - f, 2d))));
    }

    /**
     * Alpha of the first layer so that, drawn first and then a second layer of alpha {@code second} over it, the pair shows
     * {@code (1 - blend)} of the first picture and {@code blend} of the second at a total coverage of {@code coverage}. See ShadeWallpaper.
     */
    static float firstLayerAlpha(float coverage, float second) {
        float rest = 1f - second;
        if (!(rest > 1e-4f)) return 0f;
        return Math.max(0f, Math.min(1f, 1f - (1f - coverage) / rest));
    }
}
