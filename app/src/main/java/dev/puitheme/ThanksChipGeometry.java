// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie

package dev.puitheme;

/**
 * One capsule for every name on the thanks wall. The avatar sits in the start cap with the same inset on the start, top and
 * bottom, so it is concentric with the cap's curve; the name is centred on the same line and the end cap is as roomy as the
 * start cap. Everything is in dp and derives from the chip height alone, which the lane budget already measures.
 */
final class ThanksChipGeometry {
    /** Space between the capsule edge and the avatar, equal on the start, top and bottom. */
    static final float AVATAR_INSET = 4f;
    /** Space between the avatar and the name. */
    static final float TEXT_GAP = 8f;
    /** Space after the name: the visible round of the end cap is balanced against the avatar inset. */
    static final float END_PADDING = 14f;
    /** Keeps single-letter names from producing chips that look unlike the rest of the lane. */
    static final float MIN_WIDTH = 76f;
    /** A very long name ends with an ellipsis instead of widening one chip past the lane. */
    static final float MAX_TEXT_WIDTH = 168f;
    /** The smallest chip: never below the touch-friendly height the wall always used. */
    static final float MIN_HEIGHT = 40f;
    /** Vertical room above and below one line of name text. */
    static final float TEXT_PADDING = 16f;

    /** The shared chip height for the actual (font-scaled) height of one text line. */
    static float height(float textLineDp) {
        float line = Float.isNaN(textLineDp) || Float.isInfinite(textLineDp) ? 0f : Math.max(0f, textLineDp);
        return Math.max(MIN_HEIGHT, line + TEXT_PADDING);
    }

    /** The avatar fills the capsule except for the inset on every side. */
    static float avatarSize(float height) {
        return Math.max(0f, height - 2f * AVATAR_INSET);
    }

    /** Every chip is a full capsule: the corner radius is half the height. */
    static float cornerRadius(float height) {
        return height / 2f;
    }

    /** Distance from the start edge to the avatar's centre; it equals the cap radius, which makes the two concentric. */
    static float avatarCenterX(float height) {
        return AVATAR_INSET + avatarSize(height) / 2f;
    }

    /** The avatar and the name share this vertical centre line. */
    static float centerY(float height) {
        return height / 2f;
    }

    private ThanksChipGeometry() {}
}
