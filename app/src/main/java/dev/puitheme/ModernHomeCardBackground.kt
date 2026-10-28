// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme

import android.animation.ValueAnimator
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal data class HomeCardPalette(val base: Color, val accent: Color, val foreground: Color, val secondary: Color)

internal fun homeCardPalette(status: String, dark: Boolean): HomeCardPalette {
    val colors = when (status) {
        "Activado" -> Triple(0xffdff7ed, 0xff173c32, 0xff28ae83)
        "Instalado" -> Triple(0xffe5eeff, 0xff1c304b, 0xff5487e6)
        "No instalado" -> Triple(0xffeeeff3, 0xff2b2e35, 0xff8c96a8)
        "Desactivado" -> Triple(0xffffefdf, 0xff3b2f1c, 0xffce9846)
        "Pendiente de reinicio" -> Triple(0xffe1f3f7, 0xff17343c, 0xff5aa9bf)
        "Pendiente de desinstalación" -> Triple(0xfff9e6e5, 0xff432928, 0xffd17c78)
        "En espera" -> Triple(0xffffeed8, 0xff403019, 0xffd59b42)
        "Pendiente de actualización" -> Triple(0xffeee7fa, 0xff342747, 0xffa17ad5)
        "Procesando…" -> Triple(0xffe5e8ff, 0xff262e4c, 0xff7b87e2)
        "Optimizando…" -> Triple(0xffe4f3f0, 0xff1e3c38, 0xff6bb5a6)
        "Muro de agradecimientos" -> Triple(0xfffff0cd, 0xff3b301d, 0xffe9b952)
        else -> Triple(0xffe9edf2, 0xff262d37, 0xff8192aa)
    }
    val accent = Color(colors.third)
    val foreground = if (status == "Muro de agradecimientos") Color(if (dark) 0xfff2d697 else 0xff714e18)
        else lerp(Color(if (dark) 0xffedf5f3 else 0xff17272d), accent, .12f)
    val secondary = if (status == "Muro de agradecimientos") Color(if (dark) 0xffd6ba81 else 0xff89692e)
        else lerp(Color(if (dark) 0xffc4ced3 else 0xff526169), accent, .10f)
    return HomeCardPalette(Color(if (dark) colors.second else colors.first), accent, foreground, secondary)
}

/** Shared elapsed seconds are read only by drawing, never by page composition or Root polling. */
@Composable
internal fun rememberHomeCardTime(app: ModernMainActivity, visible: Boolean): State<Double> {
    val elapsed = remember { mutableDoubleStateOf(0.0) }
    var resumed by remember(app) { mutableStateOf(app.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    var animations by remember { mutableStateOf(ValueAnimator.areAnimatorsEnabled()) }
    DisposableEffect(app) {
        val listener = LifecycleEventObserver { _, _ ->
            resumed = app.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            animations = ValueAnimator.areAnimatorsEnabled()
        }
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                animations = ValueAnimator.areAnimatorsEnabled() &&
                    Settings.Global.getFloat(app.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
            }
        }
        app.lifecycle.addObserver(listener)
        app.contentResolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer)
        onDispose {
            app.lifecycle.removeObserver(listener)
            app.contentResolver.unregisterContentObserver(observer)
        }
    }
    LaunchedEffect(visible, resumed, animations) {
        if (visible && resumed && animations) {
            val origin = SystemClock.uptimeMillis() - (elapsed.doubleValue * 1000.0).toLong()
            while (isActive) {
                delay(42) // Visible decoration only, at most about 24 updates/second.
                elapsed.doubleValue = (SystemClock.uptimeMillis() - origin) / 1000.0
            }
        }
    }
    return elapsed
}

private data class HomeGradientSquare(val left: Float, val top: Float, val size: Size,
    val brush: Brush, val period: Double, val advance: Double, val dx: Float, val dy: Float)

private data class HomeBadgePixel(val point: Offset, val color: Color, val glintAdvance: Double?)

private data class HomePixelSpark(val point: Offset, val advance: Double)

private data class HomePixelOrbit(val radiusX: Float, val radiusY: Float, val advance: Double,
    val size: Size, val color: Color)

private fun homeCycle(seconds: Double, duration: Double, advance: Double = 0.0): Float =
    (((seconds + advance) % duration) / duration).toFloat()

private fun homeSway(progress: Float): Float = .5f - .5f * cos(progress * 2f * PI.toFloat())

private fun homeGlint(progress: Float): Float = when {
    progress < .3f || progress >= .7f -> 0f
    progress < .48f -> (progress - .3f) / .18f * .94f
    else -> (.7f - progress) / .22f * .94f
}

private fun homeSparkAlpha(progress: Float): Float = when {
    progress < .15f || progress >= .9f -> 0f
    progress < .46f -> (progress - .15f) / .31f * .85f
    progress < .7f -> .85f - (progress - .46f) / .24f * .65f
    else -> (.9f - progress) / .2f * .2f
}

/** Entrances and details share the same artwork; details keep a quiet right-side texture. */
@Composable
internal fun HomeCardBackground(status: String, elapsed: State<Double>? = null,
    modifier: Modifier = Modifier, expanded: Boolean = false, badgeWidth: Dp = 42.dp) {
    val dark = LocalC17Dark.current
    val palette = homeCardPalette(status, dark)
    Box(modifier.drawWithCache {
        if (status == "Muro de agradecimientos") {
            val body = Color(if (dark) 0xffd9a543 else 0xffe9b952)
            val shade = Color(if (dark) 0xffbd882e else 0xffd49b37)
            val edge = Color(if (dark) 0xffaa7d2c else 0xffbd862b)
            val light = Color(if (dark) 0xffffdfa0 else 0xfffff0b0)
            val base = Brush.linearGradient(listOf(palette.base, lerp(palette.base, body, .08f),
                lerp(palette.base, body, .14f)),
                Offset.Zero, Offset(size.width, size.height))
            // A continuous glow has no clipped diagonal band or moving hard edge.
            val atmosphere = Brush.radialGradient(0f to light.copy(alpha = if (dark) .08f else .16f),
                .5f to body.copy(alpha = .035f), 1f to Color.Transparent,
                center = Offset(size.width * .88f, size.height * .85f),
                radius = maxOf(size.width, size.height) * .95f)
            val medalWidth = badgeWidth.toPx()
            val ornamentScale = medalWidth / 42.dp.toPx()
            val rayRadius = medalWidth * .70f
            val rayBrush = Brush.radialGradient(0f to Color.Transparent, .12f to Color.Transparent,
                .39f to body.copy(alpha = .18f), .70f to light.copy(alpha = .12f),
                1f to Color.Transparent, center = Offset.Zero, radius = rayRadius)
            val innerRayBrush = Brush.radialGradient(0f to Color.Transparent,
                .31f to Color.Transparent, .58f to shade.copy(alpha = .14f),
                .85f to light.copy(alpha = .18f), 1f to Color.Transparent,
                center = Offset.Zero, radius = rayRadius)
            val glowBrush = Brush.radialGradient(0f to light.copy(alpha = .24f),
                .53f to body.copy(alpha = .10f), 1f to Color.Transparent,
                center = Offset.Zero, radius = rayRadius)
            val rayBox = Size(rayRadius * 2f, rayRadius * 2f)
            val rayTop = Offset(-rayRadius, -rayRadius)
            val cell = medalWidth / 13f
            val pixelSize = Size(cell, cell)
            val badgeHeight = cell * 17f
            val badgeLeft = size.width - 8.dp.toPx() - medalWidth
            val badgeTop = size.height - 10.dp.toPx() - badgeHeight
            val badgePivot = Offset(medalWidth * .5f, badgeHeight * .6f)
            val rayCenter = Offset(badgeLeft + medalWidth * .5f, badgeTop + badgeHeight * .55f)
            val star = arrayOf("...1...", "...1...", "1111111", ".11111.", "..111..", ".11.11.", ".1...1.")
            val pixels = ArrayList<HomeBadgePixel>(221)
            for (y in 0..16) for (x in 0..12) {
                var color: Color? = null
                if (y >= 10 && (x in 2..4 || x in 8..10) && !(y == 16 && (x == 3 || x == 9))) {
                    color = if (x == 2 || x == 4 || x == 8 || x == 10) edge else shade
                }
                val distance = (x - 6) * (x - 6) + (y - 6) * (y - 6)
                var glint: Double? = null
                if (distance <= 39 && y <= 12) {
                    color = if (distance > 27) edge else if (x + y < 7) light else body
                    if (x in 3..9 && y in 3..9 && star[y - 3][x - 3] == '1') color = light
                    if (distance <= 27) glint = 2.4 - (x + y) * .045
                }
                if (color != null) pixels.add(HomeBadgePixel(Offset(x * cell, y * cell), color, glint))
            }
            val sparkRight = floatArrayOf(9f, 17f, 29f, 53f, 57f, 11f, 20f, 47f, 34f, 58f)
            val sparkBottom = floatArrayOf(12f, 58f, 62f, 43f, 27f, 38f, 22f, 13f, 53f, 56f)
            val sparks = Array(10) { index ->
                HomePixelSpark(Offset(size.width - (8.dp.toPx() + (sparkRight[index] - 8f).dp.toPx() * ornamentScale),
                    size.height - (8.dp.toPx() + (sparkBottom[index] - 8f).dp.toPx() * ornamentScale)), index * .37)
            }
            val orbitPixels = Array(12) { index ->
                val side = (if (index % 3 == 0) 2.5f else 1.5f).dp.toPx() * ornamentScale
                HomePixelOrbit((if (index % 2 == 0) 27f else 23f).dp.toPx() * ornamentScale,
                    (if (index % 2 == 0) 29f else 26f).dp.toPx() * ornamentScale, index * 6.4 / 12,
                    Size(side, side), if (index % 3 == 0) light else shade)
            }
            val unit = 2.dp.toPx() * ornamentScale
            val sparkVertical = Size(unit, unit * 3f)
            val sparkHorizontal = Size(unit * 3f, unit)
            val sparkCross = Offset(-unit, unit)
            val floatDown = 1.dp.toPx()
            val floatTravel = 4.dp.toPx()
            onDrawBehind {
                val seconds = elapsed?.value ?: 0.0
                drawRect(base)
                drawRect(atmosphere, alpha = .72f + .18f * homeSway(homeCycle(seconds, 4.8)))
                translate(rayCenter.x, rayCenter.y) {
                    drawRect(glowBrush, rayTop, rayBox,
                        alpha = .64f + .26f * homeSway(homeCycle(seconds, 3.2)))
                    rotate(homeCycle(seconds, 12.8) * 360f, Offset.Zero) {
                        repeat(12) { index ->
                            drawArc(rayBrush, 8f + index * 30f, 12f, true, rayTop, rayBox)
                        }
                    }
                    rotate(-homeCycle(seconds, 9.6) * 360f, Offset.Zero) {
                        repeat(8) { index ->
                            drawArc(innerRayBrush, 13f + index * 45f, 8f, true, rayTop, rayBox)
                        }
                    }
                }
                val sway = homeSway(homeCycle(seconds, 2.8))
                translate(badgeLeft, badgeTop + floatDown - floatTravel * sway) {
                    rotate(-4f + 8f * sway, badgePivot) {
                        for (pixel in pixels) {
                            drawRect(pixel.color, pixel.point, pixelSize)
                            val advance = pixel.glintAdvance
                            if (advance != null) {
                                val alpha = homeGlint(homeCycle(seconds, 2.4, advance))
                                if (alpha > 0f) drawRect(light, pixel.point, pixelSize, alpha)
                            }
                        }
                    }
                }
                for (pixel in orbitPixels) {
                    val progress = homeCycle(seconds, 6.4, pixel.advance)
                    val angle = progress * 2f * PI.toFloat()
                    val alpha = .28f + .52f * homeSway(progress)
                    drawRect(pixel.color, Offset(rayCenter.x + cos(angle) * pixel.radiusX,
                        rayCenter.y + sin(angle) * pixel.radiusY), pixel.size, alpha)
                }
                for (spark in sparks) {
                    val progress = homeCycle(seconds, 2.72, spark.advance)
                    val alpha = homeSparkAlpha(progress)
                    if (alpha > 0f) {
                        val growth = .45f + .55f * homeSway(progress)
                        translate(spark.point.x, spark.point.y + unit * (1f - 3f * progress)) {
                            scale(growth, growth, Offset.Zero) {
                                drawRect(shade, size = sparkVertical, alpha = alpha)
                                drawRect(shade, sparkCross, sparkHorizontal, alpha)
                            }
                        }
                    }
                }
            }
        } else {
            val strength = if (expanded) .58f else 1f
            val plane = Brush.linearGradient(listOf(Color.Transparent, palette.accent.copy(alpha = .10f * strength)),
                Offset.Zero, Offset(size.width, size.height))
            val tiles = Array(28) { index ->
                val row = index / 7
                val column = index % 7
                val side = (if (expanded) 24 + index * 7 % 23 else 16 + index * 7 % 17).dp.toPx()
                val opacity = (.36f + index % 5 * .12f) * strength
                val x = -.14f + (column * .15f + row % 2 * .05f) * 1.28f
                HomeGradientSquare(size.width * (if (expanded) .35f + x * .65f else x),
                    size.height * (-.12f + (row * .26f + column % 3 * .03f) * 1.24f), Size(side, side),
                    Brush.linearGradient(listOf(palette.accent.copy(alpha = .64f * opacity),
                        palette.accent.copy(alpha = .08f * opacity)), Offset.Zero, Offset(side, side)),
                    10.0 * (.8 + index % 4 * .15), index * 1.9,
                    (if (index % 2 == 0) 28 else -26).dp.toPx(), (if (index % 3 == 0) 20 else -18).dp.toPx())
            }
            val corners = CornerRadius(3.dp.toPx())
            val textVeil = if (expanded) Brush.horizontalGradient(
                0f to palette.base, .28f to palette.base, .75f to palette.base.copy(alpha = 0f)) else null
            onDrawBehind {
                val seconds = elapsed?.value ?: 0.0
                drawRect(plane)
                for (tile in tiles) {
                    val travel = homeSway(homeCycle(seconds, tile.period, tile.advance))
                    translate(tile.left + tile.dx * travel, tile.top + tile.dy * travel) {
                        drawRoundRect(tile.brush, size = tile.size, cornerRadius = corners)
                    }
                }
                if (textVeil != null) drawRect(textVeil)
            }
        }
    })
}
