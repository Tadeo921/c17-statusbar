// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

private val cosmosCyan = Color(0xFF7FE7FF)
private val cosmosViolet = Color(0xFFB28CFF)
private val cosmosPink = Color(0xFFFF7AC8)
private val cosmosGold = Color(0xFFFFE3A3)

/** When the warp jump ends and the galaxy appears. */
private const val WARP_END = 1.25f

/**
 * 星河漫游: a warp-speed jump that drops out beside a spiral galaxy, with nebulae, a ringed planet, orbiting moons,
 * shooting stars and a C17 constellation that draws itself above the title.
 */
internal class CosmosEgg(icon: ImageBitmap, measurer: TextMeasurer) : EggScene(icon, measurer) {
    private class Streak(val angle: Float, val offset: Float, val tint: Int)
    private class Dust(val radius: Float, val angle: Float, val size: Float, val warm: Float)
    private class Star(val x: Float, val y: Float, val size: Float, val phase: Float, val layer: Int)
    private class Meteor(val x: Float, val y: Float, val born: Float)

    private val streaks: List<Streak>
    private val galaxy: List<Dust>
    private val stars: List<Star>
    private val meteors: List<Meteor>
    /** C, 1 and 7 as constellation points (unit box, then placed above the title) and the lines between them. */
    private val constellation = listOf(
        Offset(.30f, .02f), Offset(.12f, .08f), Offset(.02f, .5f), Offset(.12f, .92f), Offset(.30f, .98f),
        Offset(.46f, .18f), Offset(.56f, 0f), Offset(.56f, 1f),
        Offset(.72f, 0f), Offset(1f, 0f), Offset(.82f, 1f))
    private val links = listOf(0 to 1, 1 to 2, 2 to 3, 3 to 4, 5 to 6, 6 to 7, 8 to 9, 9 to 10)

    init {
        val random = Random(4242)
        streaks = List(150) { Streak(random.nextFloat() * EGG_TAU, random.nextFloat(), random.nextInt(3)) }
        galaxy = List(420) { index ->
            val arm = index % 3
            val r = random.nextFloat().pow(.55f)
            val jitter = (random.nextFloat() - .5f) * (.5f + (1f - r) * .6f)
            Dust(r, arm * EGG_TAU / 3f + r * 5.2f + jitter, .8f + random.nextFloat() * 1.6f, 1f - r)
        }
        stars = List(140) { Star(random.nextFloat(), random.nextFloat(), .6f + random.nextFloat() * 1.6f,
            random.nextFloat() * EGG_TAU, random.nextInt(3)) }
        meteors = List(5) { index -> Meteor(.3f + random.nextFloat() * .8f, random.nextFloat() * .35f, 2.2f + index * .9f) }
    }

    override fun DrawScope.draw(t: Float, fade: Float) {
        val w = size.width
        val h = size.height
        val center = Offset(w / 2f, h * .36f)
        drawRect(Brush.verticalGradient(listOf(Color(0xFF02030C), Color(0xFF0A0B26), Color(0xFF130A2E), Color(0xFF02030C)), 0f, h),
            alpha = .98f * eggIn(t, 0f, .3f) * fade)
        if (t < WARP_END + .3f) warp(t, fade, w, h, center)
        if (t >= WARP_END - .05f) {
            val arrive = eggIn(t, WARP_END - .05f, .8f) * fade
            starField(t, arrive, w, h)
            nebula(t, arrive, w, h)
            planet(t, arrive, w, h)
            spiral(t, arrive, center)
            meteorShower(t, arrive, w, h)
        }
        core(t, fade, center)
        titles(t, fade, w, h, center)
        val flash = 1f - eggEase((t - WARP_END) / .45f)
        if (t >= WARP_END && flash > 0f) drawRect(Color(0xFFE8F6FF), alpha = flash * fade)
    }

    private fun DrawScope.warp(t: Float, fade: Float, w: Float, h: Float, center: Offset) {
        // Streaks rush outward on an exponential depth scale and stretch as the jump accelerates.
        val speed = eggEaseInOut(t / WARP_END)
        val vanish = 1f - eggEase((t - WARP_END) / .3f)
        val inner = 6f * density
        val outer = size.maxDimension
        val span = ln(outer / inner)
        val travel = t * .35f + t * t * .9f
        for (streak in streaks) {
            val depth = (streak.offset + travel) % 1f
            val r = inner * exp(depth * span)
            val length = r * (.04f + .9f * speed)
            val dir = Offset(cos(streak.angle), sin(streak.angle))
            val color = when (streak.tint) { 0 -> Color.White; 1 -> cosmosCyan; else -> cosmosViolet }
            drawLine(color, center + dir * r, center + dir * (r + length), (.8f + depth * 2.2f) * density, StrokeCap.Round,
                alpha = (depth * 1.4f).coerceAtMost(1f) * vanish * fade, blendMode = BlendMode.Plus)
        }
        // A blue tunnel glow pulls inward at the centre.
        drawCircle(Brush.radialGradient(listOf(Color(0x664FA8FF), Color.Transparent), center, w * .7f), w * .7f, center,
            alpha = speed * vanish * fade, blendMode = BlendMode.Plus)
    }

    private fun DrawScope.starField(t: Float, show: Float, w: Float, h: Float) {
        for (star in stars) {
            val drift = (star.layer + 1) * .006f * t
            val x = ((star.x + drift) % 1f) * w
            val twinkle = .45f + .55f * eggWave(t, 2.5f + star.layer, star.phase)
            drawCircle(Color.White, star.size * density * (1f + star.layer * .25f), Offset(x, star.y * h),
                alpha = twinkle * show)
        }
    }

    private fun DrawScope.nebula(t: Float, show: Float, w: Float, h: Float) {
        val clouds = listOf(Triple(Offset(w * .2f, h * .2f), cosmosViolet, .55f), Triple(Offset(w * .85f, h * .48f), cosmosPink, .5f),
            Triple(Offset(w * .35f, h * .72f), Color(0xFF3FD6C8), .45f), Triple(Offset(w * .7f, h * .12f), Color(0xFF5B7CFF), .4f))
        clouds.forEachIndexed { index, (spot, color, reach) ->
            val breathe = 1f + .08f * sin(t * .8f + index * 1.7f)
            val r = w * reach * breathe
            val c = spot + Offset(sin(t * .2f + index) * 10f * density, cos(t * .25f + index) * 8f * density)
            drawCircle(Brush.radialGradient(listOf(color.copy(alpha = .38f), color.copy(alpha = .12f), Color.Transparent), c, r),
                r, c, alpha = show, blendMode = BlendMode.Plus)
        }
    }

    private fun DrawScope.planet(t: Float, show: Float, w: Float, h: Float) {
        val rise = eggEase((t - WARP_END) / 1.6f)
        val c = Offset(w * .18f, h * .8f + (1f - rise) * 120f * density)
        val r = 64f * density
        val ringBox = Offset(c.x - r * 2f, c.y - r * .5f)
        val ringSize = Size(r * 4f, r)
        val ring = Brush.horizontalGradient(listOf(Color(0x00FFE3A3), Color(0xCCFFE3A3), Color(0x99B28CFF), Color(0x00B28CFF)),
            ringBox.x, ringBox.x + ringSize.width)
        rotate(-18f, c) {
            // Back half of the ring, the planet, then the front half over it.
            clipRect(ringBox.x - 2f, ringBox.y - 2f, ringBox.x + ringSize.width + 2f, c.y) {
                drawOval(ring, ringBox, ringSize, alpha = show, style = Stroke(5f * density))
            }
            drawCircle(Brush.radialGradient(listOf(Color(0xFFFFC9A8), Color(0xFFD0568F), Color(0xFF2A0F45)),
                c - Offset(r * .45f, r * .45f), r * 1.6f), r, c, alpha = show)
            drawCircle(Brush.radialGradient(listOf(Color.Transparent, Color(0xAA02030C)), c + Offset(r * .3f, r * .3f), r * 1.2f),
                r, c, alpha = show)
            clipRect(ringBox.x - 2f, c.y, ringBox.x + ringSize.width + 2f, ringBox.y + ringSize.height + 4f) {
                drawOval(ring, ringBox, ringSize, alpha = show, style = Stroke(5f * density))
            }
        }
    }

    private fun DrawScope.spiral(t: Float, show: Float, center: Offset) {
        val reach = size.width * .62f
        val bloom = eggEase((t - WARP_END) / 1.2f)
        rotate(-22f, center) {
            scale(1f, .46f, center) {
                drawCircle(Brush.radialGradient(listOf(cosmosGold.copy(alpha = .55f), cosmosViolet.copy(alpha = .18f), Color.Transparent),
                    center, reach * bloom + 1f), reach * bloom + 1f, center, alpha = show, blendMode = BlendMode.Plus)
                for (dust in galaxy) {
                    // Inner stars orbit faster, like a real disc.
                    val angle = dust.angle + t * .5f / sqrt(dust.radius + .08f)
                    val r = dust.radius * reach * bloom
                    val p = Offset(center.x + cos(angle) * r, center.y + sin(angle) * r)
                    val color = cosmosGold.eggMix(if (dust.angle % 2f > 1f) cosmosCyan else cosmosViolet, 1f - dust.warm)
                    drawCircle(color, dust.size * density, p, alpha = (.35f + .65f * dust.warm) * show, blendMode = BlendMode.Plus)
                }
            }
        }
    }

    private fun DrawScope.meteorShower(t: Float, show: Float, w: Float, h: Float) {
        for (meteor in meteors) {
            val age = t - meteor.born
            if (age < 0f || age > .7f) continue
            val p = age / .7f
            val head = Offset(meteor.x * w - p * w * .55f, meteor.y * h + p * w * .3f)
            val tail = head + Offset(80f * density, -44f * density)
            drawLine(Brush.linearGradient(listOf(Color.White, Color.Transparent), head, tail), head, tail, 2.2f * density,
                StrokeCap.Round, alpha = sin(p * Math.PI.toFloat()) * show, blendMode = BlendMode.Plus)
        }
    }

    /** The icon as the galaxy's bright core: glow, diffraction spikes and two orbiting moons. */
    private fun DrawScope.core(t: Float, fade: Float, center: Offset) {
        val appear = eggOvershoot((t - WARP_END) / .8f)
        if (appear <= 0f) return
        val iconSize = 96f * density * appear
        val pulse = 1f + .06f * sin(t * 3f)
        drawCircle(Brush.radialGradient(listOf(Color(0xCCFFFFFF), cosmosCyan.copy(alpha = .35f), Color.Transparent), center,
            iconSize * 1.6f * pulse), iconSize * 1.6f * pulse, center, alpha = fade, blendMode = BlendMode.Plus)
        for (k in 0 until 4) {
            val angle = k * 45f
            rotate(angle + t * 4f, center) {
                drawLine(Brush.horizontalGradient(listOf(Color.Transparent, Color.White, Color.Transparent), center.x - iconSize * 2.2f,
                    center.x + iconSize * 2.2f), Offset(center.x - iconSize * 2.2f, center.y), Offset(center.x + iconSize * 2.2f, center.y),
                    (if (k % 2 == 0) 2f else 1f) * density, alpha = .7f * fade, blendMode = BlendMode.Plus)
            }
        }
        drawIcon(icon, center, iconSize, fade)
        for (k in 0 until 2) {
            val a = t * (1.6f - k * .5f) + k * 2.4f
            val orbit = iconSize * (.95f + k * .35f)
            val moon = Offset(center.x + cos(a) * orbit, center.y + sin(a) * orbit * .38f)
            // Moons pass behind the icon on the far side of the orbit.
            if (sin(a) < 0f && kotlin.math.abs(cos(a)) * orbit < iconSize * .5f) continue
            drawCircle(if (k == 0) cosmosCyan else cosmosPink, (3.5f - k) * density, moon, alpha = fade, blendMode = BlendMode.Plus)
            drawCircle(Color.White, (1.6f - k * .4f) * density, moon, alpha = fade)
        }
    }

    private fun DrawScope.titles(t: Float, fade: Float, w: Float, h: Float, center: Offset) {
        // The C17 constellation: stars light up, then lines draw between them.
        val box = Size(150f * density, 64f * density)
        val origin = Offset(w / 2f - box.width / 2f, center.y + 128f * density)
        val point = { p: Offset -> Offset(origin.x + p.x * box.width, origin.y + p.y * box.height) }
        links.forEachIndexed { index, (a, b) ->
            val draw = eggEase((t - 2.4f - index * .12f) / .35f)
            if (draw <= 0f) return@forEachIndexed
            val start = point(constellation[a])
            val end = point(constellation[b])
            drawLine(cosmosCyan, start, start + (end - start) * draw, 1.4f * density, StrokeCap.Round, alpha = .8f * fade,
                blendMode = BlendMode.Plus)
        }
        constellation.forEachIndexed { index, p ->
            val lit = eggOvershoot((t - 2.1f - index * .05f) / .3f)
            if (lit <= 0f) return@forEachIndexed
            val at = point(p)
            drawCircle(Brush.radialGradient(listOf(Color.White, cosmosCyan.copy(alpha = .3f), Color.Transparent), at, 9f * density),
                9f * density * lit, at, alpha = fade, blendMode = BlendMode.Plus)
            drawCircle(Color.White, 2.2f * density * lit, at, alpha = fade)
        }
        val show = eggIn(t, 3.3f, .7f) * fade
        if (show <= 0f) return
        val top = origin.y + box.height + 24f * density
        val title = layout("STATUS C17", 34.sp, FontWeight.Light, letterSpacing = 10.sp)
        centred(title, w / 2f, top, Brush.horizontalGradient(listOf(cosmosCyan, Color.White, cosmosViolet, cosmosPink),
            w / 2f - title.size.width / 2f, w / 2f + title.size.width / 2f), show)
        val sub = layout("穿越星河 · 与你相遇", 16.sp, FontWeight.Normal, letterSpacing = 4.sp)
        centred(sub, w / 2f, top + title.size.height + 8f * density, Color(0xFFDDE6FF), eggIn(t, 3.7f, .6f) * fade)
        val thanks = layout(EGG_THANKS, 13.sp, FontWeight.Normal)
        centred(thanks, w / 2f, top + title.size.height + sub.size.height + 16f * density, cosmosGold, eggIn(t, 4f, .6f) * fade)
    }
}
