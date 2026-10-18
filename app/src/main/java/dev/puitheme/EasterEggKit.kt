// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import kotlin.math.PI
import kotlin.math.sin

/**
 * One themed show. Every particle is generated once from a fixed seed in the constructor, so a frame only does
 * arithmetic and draw calls; nothing is allocated per particle while the show runs.
 * [draw] receives the show time in seconds and the shared closing fade (1 → 0 over the last 0.8 s).
 */
internal abstract class EggScene(val icon: ImageBitmap, val measurer: TextMeasurer) {
    abstract fun DrawScope.draw(t: Float, fade: Float)

    /** Measured text is cached by the measurer; colour and brush are applied at draw time so the cache keeps hitting. */
    fun layout(text: String, size: TextUnit, weight: FontWeight = FontWeight.Bold, family: FontFamily? = null,
        letterSpacing: TextUnit = TextUnit.Unspecified): TextLayoutResult =
        measurer.measure(text, TextStyle(color = Color.White, fontSize = size, fontWeight = weight, fontFamily = family,
            letterSpacing = letterSpacing, textAlign = TextAlign.Center))

    /** Draws [layout] centred horizontally on [cx] with its top at [top]. */
    fun DrawScope.centred(layout: TextLayoutResult, cx: Float, top: Float, color: Color, alpha: Float,
        blend: BlendMode = BlendMode.SrcOver) {
        if (alpha <= 0f) return
        drawText(layout, color, Offset(cx - layout.size.width / 2f, top), alpha.coerceIn(0f, 1f), blendMode = blend)
    }

    fun DrawScope.centred(layout: TextLayoutResult, cx: Float, top: Float, brush: Brush, alpha: Float,
        blend: BlendMode = BlendMode.SrcOver) {
        if (alpha <= 0f) return
        drawText(layout, brush, Offset(cx - layout.size.width / 2f, top), alpha.coerceIn(0f, 1f), blendMode = blend)
    }

    /** The app icon at [center], [sizePx] wide. [pixelated] keeps hard pixel edges for the arcade look. */
    fun DrawScope.drawIcon(image: ImageBitmap, center: Offset, sizePx: Float, alpha: Float, pixelated: Boolean = false,
        tint: Color? = null, blend: BlendMode = BlendMode.SrcOver, filter: ColorFilter? = null) {
        val side = sizePx.toInt()
        if (side < 1 || alpha <= 0f) return
        drawImage(image, IntOffset.Zero, IntSize(image.width, image.height),
            IntOffset((center.x - side / 2f).toInt(), (center.y - side / 2f).toInt()), IntSize(side, side),
            alpha = alpha.coerceIn(0f, 1f), colorFilter = tint?.let { ColorFilter.tint(it, BlendMode.SrcIn) } ?: filter,
            blendMode = blend, filterQuality = if (pixelated) FilterQuality.None else FilterQuality.Low)
    }
}

/**
 * The six shows mix proportional anchors with fixed-size title stacks. A 720 dp logical height keeps their
 * titles, thanks and lower controls in frame in a short landscape/multi-window surface. Expand the logical
 * bounds before scaling the existing canvas, so backgrounds still fill the actual window without a bitmap
 * or an off-screen layer. Normal-height portrait drawing follows the original path unchanged.
 */
internal inline fun DrawScope.withEggViewport(draw: DrawScope.() -> Unit) {
    if (size.minDimension < 1f) return
    val minimumHeight = 720f * density
    val height = size.height
    if (height >= minimumHeight) {
        draw()
        return
    }
    val width = size.width
    val factor = height / minimumHeight
    scale(factor, factor, Offset.Zero) {
        inset(0f, 0f, width - width / factor, height - minimumHeight, draw)
    }
}

internal const val EGG_TAU = (2.0 * PI).toFloat()

/** The original line of the first show. Every theme keeps it under its own title. */
internal const val EGG_THANKS = "你发现了隐藏彩蛋 · 谢谢你一直在用"

internal fun eggEase(x: Float): Float { val t = x.coerceIn(0f, 1f); return 1f - (1f - t) * (1f - t) * (1f - t) }

internal fun eggEaseInOut(x: Float): Float { val t = x.coerceIn(0f, 1f); return t * t * (3f - 2f * t) }

internal fun eggOvershoot(x: Float): Float {
    val t = x.coerceIn(0f, 1f); val c = 1.9f
    return 1f + (c + 1f) * (t - 1f) * (t - 1f) * (t - 1f) + c * (t - 1f) * (t - 1f)
}

/** 0 before [start], eases to 1 over [length] seconds. */
internal fun eggIn(t: Float, start: Float, length: Float): Float = eggEase((t - start) / length)

/** A 0..1 window: in over [fadeIn], hold, out over [fadeOut] ending at [end]. */
internal fun eggWindow(t: Float, start: Float, end: Float, fadeIn: Float = .3f, fadeOut: Float = .3f): Float =
    (eggEase((t - start) / fadeIn) * (1f - eggEase((t - (end - fadeOut)) / fadeOut))).coerceIn(0f, 1f)

/** Deterministic hash noise in 0..1, used for flicker and glitches that must look random but replay identically. */
internal fun eggNoise(seed: Int, step: Int): Float {
    var h = seed * 374761393 + step * 668265263
    h = (h xor (h ushr 13)) * 1274126177
    h = h xor (h ushr 16)
    return (h and 0x7fffffff) / 2147483647f
}

internal fun eggWave(t: Float, speed: Float, phase: Float = 0f): Float = .5f + .5f * sin(t * speed + phase)

internal fun Color.eggMix(other: Color, f: Float): Color {
    val k = f.coerceIn(0f, 1f)
    return Color(red + (other.red - red) * k, green + (other.green - green) * k, blue + (other.blue - blue) * k,
        alpha + (other.alpha - alpha) * k)
}
