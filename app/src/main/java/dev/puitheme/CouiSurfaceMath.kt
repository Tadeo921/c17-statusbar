// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

/** Check the displayed ink after alpha compositing, not the uncomposited theme token. */
internal fun c17ColorContrast(ink: Color, background: Color): Float {
    val first = ink.compositeOver(background).luminance()
    val second = background.luminance()
    return (maxOf(first, second) + .05f) / (minOf(first, second) + .05f)
}

/** Retain already-readable ink; otherwise minimally strengthen it for every possible surface. */
internal fun c17ReadableInk(ink: Color, backgrounds: List<Color>, minimum: Float = 4.5f): Color {
    fun contrast(candidate: Color) = backgrounds.minOf { c17ColorContrast(candidate, it) }
    if (contrast(ink) >= minimum) return ink
    val solid = ink.copy(alpha = 1f)
    val target = if (contrast(solid) >= minimum) solid else {
        if (contrast(Color.White) >= contrast(Color.Black)) Color.White else Color.Black
    }
    var lower = 0f
    var upper = 1f
    repeat(14) {
        val amount = (lower + upper) / 2f
        if (contrast(lerp(ink, target, amount)) >= minimum) upper = amount else lower = amount
    }
    return lerp(ink, target, upper)
}

/** Blur pixels are bounded by black/white. Include the selected panel's strongest overlay. */
internal fun c17GlassSurfaces(tint: Color, accent: Color, overlayAlpha: Float): List<Color> {
    val low = tint.compositeOver(Color.Black)
    val high = tint.compositeOver(Color.White)
    val overlay = accent.copy(alpha = overlayAlpha)
    return listOf(low, high, overlay.compositeOver(low), overlay.compositeOver(high))
}
