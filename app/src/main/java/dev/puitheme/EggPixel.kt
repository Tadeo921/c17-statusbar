// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.random.Random

private val pixelPalette = listOf(Color(0xFFFF004D), Color(0xFFFFA300), Color(0xFFFFEC27), Color(0xFF00E436),
    Color(0xFF29ADFF), Color(0xFF83769C), Color(0xFFFF77A8), Color(0xFFFFFFFF))
private val pixelSky = Color(0xFF1D2B53)
private val pixelBrick = Color(0xFFAB5236)
private val pixelGold = Color(0xFFFFEC27)

/** 像素街机: an 8-bit stage. The icon jumps, bumps a ? block, coins fly, blocky fireworks and a LEVEL UP banner. */
internal class PixelEgg(icon: ImageBitmap, measurer: TextMeasurer) : EggScene(icon, measurer) {
    private class Star(val x: Float, val y: Float, val seed: Int)
    private class Cloud(val x: Float, val y: Float, val scale: Int, val speed: Float)
    private class Burst(val x: Float, val y: Float, val born: Float, val color: Int)
    private class Coin(val vx: Float, val vy: Float, val born: Float)

    /** The icon reduced to 24×24 and drawn without filtering, so it becomes chunky pixel art. */
    private val sprite: ImageBitmap = runCatching {
        Bitmap.createScaledBitmap(icon.asAndroidBitmap(), 24, 24, false).asImageBitmap()
    }.getOrDefault(icon)
    private val stars: List<Star>
    private val clouds: List<Cloud>
    private val bursts: List<Burst>
    private val coins: List<Coin>

    init {
        val random = Random(1985)
        stars = List(70) { Star(random.nextFloat(), random.nextFloat() * .62f, random.nextInt()) }
        clouds = List(5) { Cloud(random.nextFloat(), .08f + random.nextFloat() * .3f, 2 + random.nextInt(2), .03f + random.nextFloat() * .05f) }
        bursts = List(6) { index -> Burst(.15f + random.nextFloat() * .7f, .1f + random.nextFloat() * .28f,
            2.3f + index * .55f, random.nextInt(pixelPalette.size)) }
        coins = List(9) { index -> Coin((index - 4) * 26f + (random.nextFloat() - .5f) * 10f, -420f - random.nextFloat() * 160f,
            1.05f + index * .06f) }
    }

    override fun DrawScope.draw(t: Float, fade: Float) {
        val w = size.width
        val h = size.height
        val px = 4f * density
        val ground = floor(h * .8f / px) * px
        drawRect(pixelSky, alpha = .98f * fade)
        drawRect(Color(0xFF7E2553), Offset(0f, ground - 30f * px), Size(w, 30f * px), alpha = .35f * fade)
        sky(t, fade, w, ground, px)
        stage(t, fade, w, h, ground, px)
        hero(t, fade, w, ground, px)
        fireworks(t, fade, w, h, px)
        banner(t, fade, w, h, px)
        hudBar(t, fade, w, px)
        wipe(t, w, h)
    }

    private fun snap(v: Float, px: Float) = floor(v / px) * px

    /** A filled rectangle snapped to the pixel grid. */
    private fun DrawScope.block(color: Color, x: Float, y: Float, cols: Float, rows: Float, px: Float, alpha: Float) {
        drawRect(color, Offset(snap(x, px), snap(y, px)), Size(cols * px, rows * px), alpha = alpha.coerceIn(0f, 1f))
    }

    private fun DrawScope.sky(t: Float, fade: Float, w: Float, ground: Float, px: Float) {
        for (star in stars) {
            val on = eggNoise(star.seed, (t * 3f).toInt()) > .3f
            val big = star.seed and 7 == 0
            block(if (big) pixelGold else Color.White, star.x * w, star.y * ground, if (big) 2f else 1f, if (big) 2f else 1f,
                px, (if (on) .9f else .25f) * fade)
        }
        // A crescent moon built from two squares.
        val moon = Offset(w * .8f, ground * .12f)
        block(Color(0xFFFFF1E8), moon.x, moon.y, 8f, 8f, px, fade)
        block(pixelSky, moon.x + 3f * px, moon.y - px, 6f, 7f, px, fade)
        for (cloud in clouds) {
            val s = cloud.scale.toFloat()
            val x = ((cloud.x + t * cloud.speed) % 1.3f - .15f) * w
            val y = cloud.y * ground
            block(Color.White, x, y + 2f * s * px, 10f * s, 3f * s, px, .92f * fade)
            block(Color.White, x + 2f * s * px, y + s * px, 6f * s, 2f * s, px, .92f * fade)
            block(Color.White, x + 4f * s * px, y, 3f * s, 2f * s, px, .92f * fade)
            block(Color(0xFFC2C3C7), x, y + 5f * s * px - px, 10f * s, 1f, px, .92f * fade)
        }
    }

    private fun DrawScope.stage(t: Float, fade: Float, w: Float, h: Float, ground: Float, px: Float) {
        // Stepped hills, then the brick floor, then a pipe on the right.
        for ((cx, steps) in listOf(w * .18f to 9, w * .7f to 6)) for (i in 0 until steps) {
            val width = (steps - i) * 4f
            block(if (i == steps - 1) Color(0xFF00E436) else Color(0xFF008751), cx - width * px / 2f, ground - (i + 1) * 3f * px,
                width, 3f, px, fade)
        }
        val brickW = 8f * px
        val brickH = 4f * px
        var row = 0
        var y = ground
        while (y < h) {
            var x = if (row % 2 == 0) 0f else -brickW / 2f
            while (x < w) {
                drawRect(pixelBrick, Offset(x, y), Size(brickW - px, brickH - px), alpha = fade)
                drawRect(Color(0xFFFFCCAA), Offset(x, y), Size(brickW - px, px), alpha = .5f * fade)
                x += brickW
            }
            y += brickH
            row++
        }
        val pipeX = snap(w * .82f, px)
        block(Color(0xFF008751), pipeX, ground - 18f * px, 14f, 18f, px, fade)
        block(Color(0xFF00E436), pipeX - 2f * px, ground - 22f * px, 18f, 5f, px, fade)
        block(Color(0xFF8FF5A2), pipeX + px, ground - 21f * px, 2f, 4f, px, fade)
        block(Color(0xFF8FF5A2), pipeX + 2f * px, ground - 17f * px, 2f, 16f, px, fade)
        // The ? block: bumps when hit, then turns into a used block.
        val hit = t >= 1.05f
        val bump = if (t in 1.05f..1.3f) sin((t - 1.05f) / .25f * Math.PI.toFloat()) * 3f * px else 0f
        val bx = snap(w / 2f - 8f * px, px)
        val by = snap(ground - 24f * px - 15f * 4f * density - 16f * px, px) - bump
        block(Color(0xFF000000), bx + px, by + px, 16f, 16f, px, .4f * fade)
        block(if (hit) Color(0xFF8A4B2A) else Color(0xFFFFA300), bx, by, 16f, 16f, px, fade)
        block(if (hit) Color(0xFFB0683A) else pixelGold, bx, by, 16f, 1f, px, fade)
        if (!hit) {
            val mark = layout("?", 40.sp, FontWeight.Black, FontFamily.Monospace)
            val blink = if (((t * 4f).toInt() and 1) == 0) 1f else .75f
            centred(mark, bx + 8f * px + px, by + 8f * px - mark.size.height / 2f + px, Color(0xFF7E2553), fade)
            centred(mark, bx + 8f * px, by + 8f * px - mark.size.height / 2f, Color.White, blink * fade)
        }
    }

    private fun DrawScope.hero(t: Float, fade: Float, w: Float, ground: Float, px: Float) {
        val side = 24f * px
        val jump = 15f * px
        // Drop in from above, one big jump into the block, then small victory hops.
        val lift = when {
            t < .55f -> (1f - eggEase(t / .55f)) * ground
            t in .7f..1.4f -> { val p = (t - .7f) / .7f; 4f * p * (1f - p) * jump * 1f }
            t > 2.2f -> { val p = ((t - 2.2f) % .8f) / .8f; 4f * p * (1f - p) * 4f * px }
            else -> 0f
        }
        val squash = if (t in .55f..0.7f) 1f - .12f * sin((t - .55f) / .15f * Math.PI.toFloat()) else 1f
        val cx = w / 2f
        val cy = ground - side / 2f - lift
        drawRect(Color.Black, Offset(snap(cx - side * .4f, px), ground - px), Size(snap(side * .8f, px), px * 2f),
            alpha = .35f * fade)
        drawIcon(sprite, Offset(snap(cx, px), snap(cy + side * (1f - squash) / 2f, px)), side, fade, pixelated = true)
        // Coins spin out of the block once it is hit, with a +100 for each.
        val blockTop = ground - side - jump - 16f * px
        for (coin in coins) {
            val age = t - coin.born
            if (age < 0f || age > 1.6f) continue
            val x = cx + coin.vx * age * density * 1.6f
            val y = blockTop + (coin.vy * age + 900f * age * age) * density
            val spin = abs(cos(age * 14f))
            val cw = (6f * spin).coerceAtLeast(1f)
            val alpha = (1f - age / 1.6f) * fade
            block(Color(0xFFFFA300), x - cw * px / 2f, y, cw, 8f, px, alpha)
            block(pixelGold, x - cw * px / 2f + px, y + px, (cw - 2f).coerceAtLeast(1f), 6f, px, alpha)
        }
        val score = eggWindow(t, 1.1f, 2.1f, .1f, .4f)
        if (score > 0f) {
            val text = layout("+100", 18.sp, FontWeight.Black, FontFamily.Monospace)
            val y = blockTop - 30f * density - eggEase((t - 1.1f) / 1f) * 40f * density
            centred(text, cx + 3f * density, y + 3f * density, Color.Black, .6f * score)
            centred(text, cx, y, Color.White, score)
        }
    }

    private fun DrawScope.fireworks(t: Float, fade: Float, w: Float, h: Float, px: Float) {
        for (burst in bursts) {
            val age = t - burst.born
            if (age < -.5f || age > 1.4f) continue
            val color = pixelPalette[burst.color]
            val cx = burst.x * w
            val cy = burst.y * h
            if (age < 0f) {
                // The rocket climbs as a short dotted trail.
                val p = 1f + age / .5f
                for (i in 0 until 4) block(color, cx, cy + (1f - p) * h * .4f + i * 2f * px, 1f, 1f, px, (1f - i * .22f) * fade)
                continue
            }
            val radius = eggEase(age / .9f) * 26f * px
            val alpha = (1f - age / 1.4f) * fade
            for (i in 0 until 16) {
                val angle = i / 16f * EGG_TAU
                val dx = cos(angle) * radius
                val dy = sin(angle) * radius + age * age * 18f * px
                block(color, cx + dx, cy + dy, 2f, 2f, px, alpha)
                block(Color.White, cx + dx * .6f, cy + dy * .6f, 1f, 1f, px, alpha * .8f)
            }
            if (age < .15f) block(Color.White, cx - 3f * px, cy - 3f * px, 6f, 6f, px, fade)
        }
    }

    private fun DrawScope.banner(t: Float, fade: Float, w: Float, h: Float, px: Float) {
        val show = eggIn(t, 1.6f, .2f) * fade
        if (show <= 0f) return
        val cx = w / 2f
        val top = h * .1f + (if (t < 1.8f) (1f - eggEase((t - 1.6f) / .2f)) * -60f * density else 0f)
        // Rainbow cycling title with a hard pixel drop shadow.
        val title = layout("LEVEL UP!", 46.sp, FontWeight.Black, FontFamily.Monospace)
        val shade = pixelPalette[((t * 10f).toInt()) % 7]
        centred(title, cx + 2f * px, top + 2f * px, Color(0xFF7E2553), show)
        centred(title, cx, top, shade, show)
        val name = layout("STATUS C17", 22.sp, FontWeight.Bold, FontFamily.Monospace)
        centred(name, cx + px, top + title.size.height + px, Color.Black, .7f * show)
        centred(name, cx, top + title.size.height, Color.White, show)
        val sub = layout("恭喜通关 · 获得 1UP", 15.sp, FontWeight.Bold)
        val subTop = top + title.size.height + name.size.height + 8f * density
        centred(sub, cx, subTop, pixelGold, eggIn(t, 2f, .3f) * fade)
        val thanks = layout(EGG_THANKS, 13.sp, FontWeight.Bold)
        centred(thanks, cx + density, subTop + sub.size.height + 5f * density, Color.Black, .6f * eggIn(t, 2.3f, .3f) * fade)
        centred(thanks, cx, subTop + sub.size.height + 4f * density, Color.White, eggIn(t, 2.3f, .3f) * fade)
        if (t > 3.2f && ((t * 2.5f).toInt() and 1) == 0) {
            val start = layout("▶ PRESS START", 16.sp, FontWeight.Bold, FontFamily.Monospace)
            centred(start, cx, h * .86f, Color.White, fade)
        }
    }

    private fun DrawScope.hudBar(t: Float, fade: Float, w: Float, px: Float) {
        val show = eggIn(t, .3f, .2f) * fade
        if (show <= 0f) return
        val count = (eggEase((t - 1.05f) / 2.4f) * 17017f).toInt().coerceAtLeast(0)
        val score = layout("SCORE " + count.toString().padStart(6, '0'), 13.sp, FontWeight.Bold, FontFamily.Monospace)
        val top = 44f * density
        drawText(score, Color.White, Offset(16f * density, top), show)
        val hi = layout("HI 999999", 13.sp, FontWeight.Bold, FontFamily.Monospace)
        drawText(hi, pixelGold, Offset(w - hi.size.width - 16f * density, top), show)
        // Three pixel hearts.
        for (i in 0 until 3) {
            val x = 16f * density + i * 9f * px
            val y = top + score.size.height + 2f * px
            block(pixelPalette[0], x, y, 2f, 2f, px, show)
            block(pixelPalette[0], x + 3f * px, y, 2f, 2f, px, show)
            block(pixelPalette[0], x - px, y + px, 7f, 2f, px, show)
            block(pixelPalette[0], x, y + 3f * px, 5f, 1f, px, show)
            block(pixelPalette[0], x + px, y + 4f * px, 3f, 1f, px, show)
            block(pixelPalette[0], x + 2f * px, y + 5f * px, 1f, 1f, px, show)
        }
    }

    /** The stage dissolves in from black tiles in a fixed pseudo-random order. */
    private fun DrawScope.wipe(t: Float, w: Float, h: Float) {
        if (t >= .6f) return
        val tile = 24f * density
        val cols = (w / tile).toInt() + 1
        val rows = (h / tile).toInt() + 1
        val progress = t / .6f
        for (r in 0 until rows) for (c in 0 until cols) {
            if (eggNoise(r * 131 + c, 3) < progress) continue
            drawRect(Color.Black, Offset(c * tile, r * tile), Size(tile, tile))
        }
    }
}
