// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.ReplacementSpan;

/** Gives every ASCII digit of a clock string the same advance, so a seconds display cannot shift, and keeps the
 * colons at the vertical middle of the digits.
 *
 * Proportional digits have different advances in most fonts; "1" is narrower than "0". The cell is
 * the widest digit of the paint that actually draws the text (size, face, variation axes and letter
 * spacing included) plus the user's spacing, and each digit is centered in it. Colons and other
 * characters are untouched. The cell is measured only when a layout or draw asks for it and cached
 * per paint state, so a stable second tick performs no extra measurement.
 */
final class ClockDigits {
    // CharSequence selects Paint.measureText(CharSequence, int, int), the overload every Paint implements.
    private static final CharSequence DIGITS = "0123456789";
    private ClockDigits() { }

    static boolean isDigit(char c) { return c >= '0' && c <= '9'; }

    static boolean isColon(char c) { return c == ':' || c == '\uff1a'; }

    /** Wrap a copy of text with one span per digit, or return the text itself when there is none. */
    static CharSequence apply(CharSequence text, float spacingPixels) {
        return align(text, spacingPixels, true, false);
    }

    /** Fixed digit cells and/or centred colons: one span per digit (when fixed) and per colon (when centred), or the
     * text itself when nothing needs a span. */
    static CharSequence align(CharSequence text, float spacingPixels, boolean fixedDigits, boolean centreColons) {
        if (text == null || text.length() == 0 || !fixedDigits && !centreColons) return text;
        SpannableString result = null;
        Cell cell = null;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            boolean digit = fixedDigits && isDigit(c), colon = centreColons && isColon(c);
            if (!digit && !colon) continue;
            if (result == null) { result = new SpannableString(text); cell = new Cell(spacingPixels); }
            result.setSpan(digit ? new DigitSpan(cell) : new ColonSpan(), i, i + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return result == null ? text : result;
    }

    /** True when spans from apply()/align() are present, i.e. plain Paint.measureText would ignore them. */
    static boolean hasSpans(CharSequence text) {
        if (!(text instanceof Spanned)) return false;
        Spanned spanned = (Spanned) text;
        return spanned.getSpans(0, text.length(), DigitSpan.class).length > 0
                || spanned.getSpans(0, text.length(), ColonSpan.class).length > 0;
    }

    /** How far a colon must move (pixels, negative = up) for its ink to sit at the middle of the digits' ink. */
    static float colonShift(Paint paint) {
        Rect digit = new Rect(), colon = new Rect();
        paint.getTextBounds("0", 0, 1, digit);
        paint.getTextBounds(":", 0, 1, colon);
        if (digit.height() <= 0 || colon.height() <= 0) return 0f;
        float shift = (digit.top + digit.bottom) / 2f - (colon.top + colon.bottom) / 2f;
        // A font whose colon is already centred stays exactly as it is; nothing is ever moved by more than a quarter em.
        float limit = paint.getTextSize() / 4f;
        return Math.abs(shift) < .5f ? 0f : Math.max(-limit, Math.min(limit, shift));
    }

    /** Natural width of text, honoring digit spans. Plain text keeps the exact Paint.measureText result. */
    static double width(CharSequence text, TextPaint paint) {
        if (text == null || text.length() == 0) return 0d;
        if (hasSpans(text)) return Math.max(0d, Layout.getDesiredWidth(text, 0, text.length(), paint));
        return Math.max(0d, paint.measureText(text, 0, text.length()));
    }

    /** Shared by all digits of one string: spacing is fixed, the measured cell follows the paint state. */
    static final class Cell {
        final float spacing;
        private float size = Float.NaN, letterSpacing = Float.NaN, width;
        private Typeface face;
        private String axes;
        private boolean measured;
        Cell(float spacing) { this.spacing = Float.isFinite(spacing) ? spacing : 0f; }

        float width(Paint paint) {
            float textSize = paint.getTextSize(), spacingEm = paint.getLetterSpacing();
            Typeface typeface = paint.getTypeface();
            String variation = paint.getFontVariationSettings();
            if (measured && textSize == size && spacingEm == letterSpacing && typeface == face
                    && (axes == null ? variation == null : axes.equals(variation))) return width;
            float widest = 0f;
            for (int i = 0; i < DIGITS.length(); i++) widest = Math.max(widest, paint.measureText(DIGITS, i, i + 1));
            width = Math.max(0f, widest + spacing);
            size = textSize; letterSpacing = spacingEm; face = typeface; axes = variation; measured = true;
            return width;
        }
    }

    /** A colon drawn at the middle of the digits. Fonts place it at the lower-case height, so beside tall digits it sits low. */
    private static final class ColonSpan extends ReplacementSpan {
        private float size = Float.NaN, letterSpacing = Float.NaN, shift;
        private Typeface face;
        private String axes;
        private boolean measured;

        private float shift(Paint paint) {
            float textSize = paint.getTextSize(), spacingEm = paint.getLetterSpacing();
            Typeface typeface = paint.getTypeface();
            String variation = paint.getFontVariationSettings();
            if (measured && textSize == size && spacingEm == letterSpacing && typeface == face
                    && (axes == null ? variation == null : axes.equals(variation))) return shift;
            shift = colonShift(paint);
            size = textSize; letterSpacing = spacingEm; face = typeface; axes = variation; measured = true;
            return shift;
        }

        @Override public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt metrics) {
            if (metrics != null) paint.getFontMetricsInt(metrics);
            return Math.round(paint.measureText(text, start, end));
        }

        @Override public void draw(Canvas canvas, CharSequence text, int start, int end, float x,
                                   int top, int y, int bottom, Paint paint) {
            canvas.drawText(text, start, end, x, y + shift(paint), paint);
        }
    }

    private static final class DigitSpan extends ReplacementSpan {
        private final Cell cell;
        DigitSpan(Cell cell) { this.cell = cell; }

        @Override public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt metrics) {
            // Keep the native line height; a replacement span would otherwise report none.
            if (metrics != null) paint.getFontMetricsInt(metrics);
            return Math.round(cell.width(paint));
        }

        @Override public void draw(Canvas canvas, CharSequence text, int start, int end, float x,
                                   int top, int y, int bottom, Paint paint) {
            float natural = paint.measureText(text, start, end);
            // Same integer cell getSize() reported, so the line and the glyphs agree.
            canvas.drawText(text, start, end, x + (Math.round(cell.width(paint)) - natural) / 2f, y, paint);
        }
    }
}
