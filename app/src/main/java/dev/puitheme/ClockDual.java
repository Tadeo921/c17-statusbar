// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

/** Geometry and the lower-row layout of the two-row status clock.
 *
 * The native TextView keeps drawing the upper row, so its measure, font, color and OEM hooks stay
 * native. The lower row is one StaticLayout owned by the clock entry, rebuilt only when its text or
 * style changes (at most once per tick) and merely drawn from the draw hook. Both rows share one
 * block that is centered vertically in the view and aligned horizontally by the widest row.
 */
final class ClockDual {
    private ClockDual() { }

    /** Where each row must be drawn, in view coordinates. Plain data so it can be checked without Android. */
    static final class Placement {
        /** Translation applied to the native upper row, relative to where the TextView drew it. */
        float upperDx, upperDy;
        /** Left edge and baseline of the lower row. */
        float lowerX, lowerBaseline;
        /** Width of the widest row, without padding or margins. */
        float contentWidth;
    }

    /** Pure placement. Heights are font ascent-to-descent boxes, ascents are positive distances above the baseline. */
    static Placement place(float paddingLeft, float paddingTop, float paddingBottom, float viewHeight,
                           float nativeBaseline, float nativeLeft,
                           float upperWidth, float upperAscent, float upperHeight,
                           float lowerWidth, float lowerAscent, float lowerHeight,
                           float lineGap, float marginStart, float alignFraction) {
        return place(new Placement(),paddingLeft,paddingTop,paddingBottom,viewHeight,nativeBaseline,nativeLeft,
                upperWidth,upperAscent,upperHeight,lowerWidth,lowerAscent,lowerHeight,lineGap,marginStart,alignFraction);
    }

    /** Each native clock entry owns one result; stable draws reuse it without sharing mutable geometry. */
    static Placement place(Placement result,float paddingLeft, float paddingTop, float paddingBottom, float viewHeight,
                           float nativeBaseline, float nativeLeft,
                           float upperWidth, float upperAscent, float upperHeight,
                           float lowerWidth, float lowerAscent, float lowerHeight,
                           float lineGap, float marginStart, float alignFraction) {
        float inner = Math.max(upperWidth, lowerWidth);
        float blockLeft = paddingLeft + marginStart;
        float centerY = (paddingTop + viewHeight - paddingBottom) / 2f;
        float block = upperHeight + lineGap + lowerHeight;
        float top = centerY - block / 2f;
        result.contentWidth = inner;
        result.upperDx = blockLeft + (inner - upperWidth) * alignFraction - nativeLeft;
        result.upperDy = top + upperAscent - nativeBaseline;
        result.lowerX = blockLeft + (inner - lowerWidth) * alignFraction;
        result.lowerBaseline = top + upperHeight + lineGap + lowerAscent;
        return result;
    }

    /** Width of the whole clock view for a given widest row: row + padding + both margins, never negative. */
    static double viewWidth(double contentWidth, double horizontalPadding, double marginStart, double marginEnd) {
        return Math.max(0d, contentWidth + horizontalPadding + marginStart + marginEnd);
    }

    /** One non-wrapping line. The width is rounded up so shaping can never move the last glyph to a new line. */
    static StaticLayout layout(CharSequence text, TextPaint paint) {
        int width = Math.max(1, (int) Math.ceil(ClockDigits.width(text, paint)) + 1);
        return StaticLayout.Builder.obtain(text, 0, text.length(), paint, width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL).setIncludePad(false).setMaxLines(1).build();
    }
}
