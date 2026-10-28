// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlin.math.pow
import kotlin.random.Random

private val cyberCyan = Color(0xFF00F0FF)
private val cyberMagenta = Color(0xFFFF2BD6)
private val cyberYellow = Color(0xFFFFE600)
private val cyberViolet = Color(0xFF7A2BFF)
private val cyberGlyphs = "ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄ0123456789ABCDEF<>/#"

/** 赛博霓虹: synthwave sun, neon grid, a skyline, digital rain and a glitching HUD around the icon. */
internal class CyberpunkEgg(icon: ImageBitmap, measurer: TextMeasurer) : EggScene(icon, measurer) {
    private class Tower(val x: Float, val width: Float, val height: Float)
    private class Window(val x: Float, val y: Float, val seed: Int)
    private class RainColumn(val x: Float, val speed: Float, val offset: Float, val glyphs: Int, val pink: Boolean)

    private val towers: List<Tower>
    private val windows: List<Window>
    private val rain: List<RainColumn>
    private val glyphColumns: List<String>

    init {
        val random = Random(2077)
        val built = ArrayList<Tower>()
        var x = -.02f
        while (x < 1.02f) {
            val width = .035f + random.nextFloat() * .07f
            built += Tower(x, width, .05f + random.nextFloat() * .17f)
            x += width + random.nextFloat() * .012f
        }
        towers = built
        windows = ArrayList<Window>().apply {
            for (tower in built) repeat((tower.height * 90f).toInt()) {
                add(Window(tower.x + random.nextFloat() * tower.width, random.nextFloat() * tower.height, random.nextInt()))
            }
        }
        glyphColumns = List(8) { index ->
            val glyph = Random(index * 31 + 7)
            (0 until 16).joinToString("\n") { cyberGlyphs[glyph.nextInt(cyberGlyphs.length)].toString() }
        }
        rain = List(26) { RainColumn(random.nextFloat(), .25f + random.nextFloat() * .55f, random.nextFloat(),
            random.nextInt(glyphColumns.size), random.nextFloat() < .3f) }
    }

    override fun DrawScope.draw(t: Float, fade: Float) {
        val w = size.width
        val h = size.height
        val dp = density
        val horizon = h * .64f
        val flicker = if (eggNoise(11, (t * 18f).toInt()) > .93f) .55f else 1f
        val glitch = if (t in .5f..1.1f || eggNoise(5, (t * 9f).toInt()) > .86f) 1f else 0f
        drawRect(Brush.verticalGradient(listOf(Color(0xFF07001A), Color(0xFF1B0640), Color(0xFF3A0A4E), Color(0xFF05010F)),
            0f, h), alpha = .97f * eggIn(t, 0f, .5f) * fade)
        sun(t, fade, w, horizon)
        skyline(t, fade * flicker, w, horizon)
        grid(t, fade, w, h, horizon)
        digitalRain(t, fade, w, h)
        hud(t, fade * flicker, w, h, dp, glitch)
        titles(t, fade, w, h, dp, glitch)
        // Scanlines and a vignette finish the CRT feel in two draw calls.
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color(0x44000000)), 0f, 3f * dp, TileMode.Repeated),
            alpha = fade)
        drawRect(Brush.radialGradient(listOf(Color.Transparent, Color(0xAA000000)), Offset(w / 2f, h / 2f),
            size.maxDimension * .75f), alpha = fade)
    }

    private fun DrawScope.sun(t: Float, fade: Float, w: Float, horizon: Float) {
        val rise = eggIn(t, .1f, 1.4f)
        if (rise <= 0f) return
        val radius = w * .34f
        val center = Offset(w / 2f, horizon + radius * .2f + (1f - rise) * radius * .6f)
        // Halo first, then the sun cut into the classic shrinking bands.
        drawCircle(Brush.radialGradient(listOf(cyberMagenta.copy(alpha = .55f), Color.Transparent), center, radius * 2.1f),
            radius * 2.1f, center, alpha = rise * fade, blendMode = BlendMode.Plus)
        val sunBrush = Brush.verticalGradient(listOf(cyberYellow, Color(0xFFFF8A3D), cyberMagenta),
            center.y - radius, center.y + radius * .3f)
        clipRect(0f, 0f, w, horizon) {
            var y = center.y - radius
            var band = 0
            while (y < horizon) {
                val solid = if (band < 4) radius * .32f else radius * (.2f - band * .012f).coerceAtLeast(.05f)
                val gap = if (band < 4) 0f else radius * (.02f + band * .009f)
                clipRect(0f, y, w, y + solid) { drawCircle(sunBrush, radius, center, alpha = rise * fade) }
                y += solid + gap
                band++
            }
        }
    }

    private fun DrawScope.skyline(t: Float, fade: Float, w: Float, horizon: Float) {
        val show = eggIn(t, .35f, 1f)
        if (show <= 0f) return
        for (tower in towers) {
            val top = horizon - tower.height * w * show
            drawRect(Color(0xFF0A0418), Offset(tower.x * w, top), Size(tower.width * w, horizon - top), alpha = fade)
            drawRect(cyberCyan, Offset(tower.x * w, top), Size(tower.width * w, density), alpha = .5f * show * fade,
                blendMode = BlendMode.Plus)
        }
        val pixel = 2f * density
        for (window in windows) {
            if (eggNoise(window.seed, (t * 2f).toInt()) < .35f) continue
            val y = horizon - window.y * w * show
            val color = if (window.seed and 3 == 0) cyberMagenta else if (window.seed and 3 == 1) cyberCyan else cyberYellow
            drawRect(color, Offset(window.x * w, y), Size(pixel, pixel), alpha = .8f * show * fade, blendMode = BlendMode.Plus)
        }
    }

    private fun DrawScope.grid(t: Float, fade: Float, w: Float, h: Float, horizon: Float) {
        val show = eggIn(t, .2f, .9f)
        if (show <= 0f) return
        drawRect(Brush.verticalGradient(listOf(Color(0xFF2A0A55), Color(0xFF05010F)), horizon, h), Offset(0f, horizon),
            Size(w, h - horizon), alpha = fade)
        val stroke = 1.6f * density
        val lines = 16
        for (i in 0 until lines) {
            val p = ((i + t * 1.6f) % lines) / lines
            val y = horizon + (h - horizon) * p.pow(2.4f)
            drawLine(cyberMagenta, Offset(0f, y), Offset(w, y), stroke * (.5f + p), alpha = (.25f + p * .75f) * show * fade,
                blendMode = BlendMode.Plus)
        }
        for (j in -12..12) {
            drawLine(cyberMagenta, Offset(w / 2f + j * w * .02f, horizon), Offset(w / 2f + j * w * .2f, h), stroke,
                alpha = .7f * show * fade, blendMode = BlendMode.Plus)
        }
        // The horizon glows where grid and sky meet.
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, cyberMagenta.copy(alpha = .8f), Color.Transparent),
            horizon - 14f * density, horizon + 14f * density), Offset(0f, horizon - 14f * density), Size(w, 28f * density),
            alpha = show * fade, blendMode = BlendMode.Plus)
    }

    private fun DrawScope.digitalRain(t: Float, fade: Float, w: Float, h: Float) {
        val show = eggIn(t, .15f, .8f) * fade
        if (show <= 0f) return
        for (column in rain) {
            val text = layout(glyphColumns[column.glyphs], 12.sp, FontWeight.Medium, FontFamily.Monospace)
            val travel = h + text.size.height
            val y = ((t * column.speed * h + column.offset * travel) % travel) - text.size.height
            val color = if (column.pink) cyberMagenta else cyberCyan
            centred(text, column.x * w, y, Brush.verticalGradient(listOf(Color.Transparent, color, Color.White),
                y, y + text.size.height), .42f * show, BlendMode.Plus)
        }
    }

    private fun DrawScope.hud(t: Float, fade: Float, w: Float, h: Float, dp: Float, glitch: Float) {
        val center = Offset(w / 2f, h * .34f)
        val appear = eggOvershoot((t - .45f) / .8f)
        val iconSize = 104f * dp * appear
        val show = eggIn(t, .45f, .5f) * fade
        if (show <= 0f) return
        drawCircle(Brush.radialGradient(listOf(cyberCyan.copy(alpha = .5f), cyberViolet.copy(alpha = .2f), Color.Transparent),
            center, iconSize * 1.9f + 1f), iconSize * 1.9f + 1f, center, alpha = show, blendMode = BlendMode.Plus)
        // Two counter-rotating dashed rings and a sweeping scanner arc.
        val ring = iconSize * .95f
        if (ring > 1f) {
            rotate(t * 40f, center) {
                for (i in 0 until 12) drawArc(cyberCyan, i * 30f, 18f, false, Offset(center.x - ring, center.y - ring),
                    Size(ring * 2f, ring * 2f), alpha = .85f * show, style = Stroke(2.2f * dp), blendMode = BlendMode.Plus)
            }
            val outer = ring * 1.22f
            rotate(-t * 70f, center) {
                for (i in 0 until 3) drawArc(cyberMagenta, i * 120f, 64f, false, Offset(center.x - outer, center.y - outer),
                    Size(outer * 2f, outer * 2f), alpha = .9f * show, style = Stroke(3.5f * dp), blendMode = BlendMode.Plus)
            }
            rotate(t * 200f, center) {
                drawArc(Brush.sweepGradient(listOf(Color.Transparent, cyberCyan.copy(alpha = .6f)), center), 0f, 90f, true,
                    Offset(center.x - outer, center.y - outer), Size(outer * 2f, outer * 2f), alpha = .5f * show,
                    blendMode = BlendMode.Plus)
            }
        }
        // Corner brackets lock onto the icon.
        val lock = 1f - eggEase((t - .6f) / .7f)
        val half = iconSize * (.78f + lock * .6f)
        val arm = 18f * dp
        val stroke = 2.5f * dp
        for (sx in listOf(-1f, 1f)) for (sy in listOf(-1f, 1f)) {
            val corner = Offset(center.x + sx * half, center.y + sy * half)
            drawLine(cyberYellow, corner, Offset(corner.x - sx * arm, corner.y), stroke, alpha = show)
            drawLine(cyberYellow, corner, Offset(corner.x, corner.y - sy * arm), stroke, alpha = show)
        }
        // Chromatic split while glitching, then the clean icon.
        val split = (4f + 10f * glitch * eggNoise(3, (t * 30f).toInt())) * dp
        drawIcon(icon, Offset(center.x - split, center.y), iconSize, .8f * show, tint = cyberCyan, blend = BlendMode.Plus)
        drawIcon(icon, Offset(center.x + split, center.y), iconSize, .8f * show, tint = cyberMagenta, blend = BlendMode.Plus)
        if (glitch > 0f) {
            val slices = 7
            val sliceHeight = iconSize / slices
            for (i in 0 until slices) {
                val top = center.y - iconSize / 2f + i * sliceHeight
                val shift = (eggNoise(i + 40, (t * 24f).toInt()) - .5f) * 22f * dp
                clipRect(0f, top, w, top + sliceHeight) { drawIcon(icon, Offset(center.x + shift, center.y), iconSize, show) }
            }
        } else drawIcon(icon, center, iconSize, show)
        // Side read-outs type themselves in.
        val lines = listOf("SYS://C17.STATUSBAR", "CORE TEMP  36.6°C", "LINK  ▮▮▮▮▮▮▮▮▮▮ 100%", "NEON  OVERDRIVE")
        val typing = ((t - 1.1f) * 22f).toInt()
        var budget = typing
        var top = h * .07f
        for (line in lines) {
            if (budget <= 0) break
            val shown = line.take(budget)
            budget -= line.length
            val text = layout(shown, 11.sp, FontWeight.Medium, FontFamily.Monospace)
            drawText(text, cyberCyan, Offset(24f * dp, top), .85f * fade)
            top += text.size.height + 2f * dp
        }
    }

    private fun DrawScope.titles(t: Float, fade: Float, w: Float, h: Float, dp: Float, glitch: Float) {
        val show = eggIn(t, 1.2f, .5f) * fade
        if (show <= 0f) return
        val cx = w / 2f
        val top = h * .34f + 150f * dp + (1f - show) * 20f * dp
        val title = layout("STATUS C17", 42.sp, FontWeight.Black, FontFamily.Monospace, 3.sp)
        val jitter = glitch * (eggNoise(9, (t * 20f).toInt()) - .5f) * 16f * dp
        val split = (2.5f + 4f * glitch) * dp
        centred(title, cx - split + jitter, top, cyberCyan, .9f * show, BlendMode.Plus)
        centred(title, cx + split + jitter, top, cyberMagenta, .9f * show, BlendMode.Plus)
        centred(title, cx + jitter, top, Color.White, show)
        // A neon underline that charges from the centre outwards.
        val charge = eggIn(t, 1.5f, .7f)
        val lineY = top + title.size.height + 6f * dp
        val half = title.size.width / 2f * charge
        drawLine(Brush.horizontalGradient(listOf(cyberCyan, cyberMagenta, cyberYellow), cx - half, cx + half),
            Offset(cx - half, lineY), Offset(cx + half, lineY), 3f * dp, alpha = show, blendMode = BlendMode.Plus)
        val sub = layout("El neón no se apaga · la barra de estado se funde con la noche", 15.sp, FontWeight.Medium)
        centred(sub, cx, lineY + 12f * dp, Color(0xFFE6F9FF), eggIn(t, 1.8f, .6f) * fade)
        val thanks = layout(EGG_THANKS, 13.sp, FontWeight.Medium)
        centred(thanks, cx, lineY + 16f * dp + sub.size.height, cyberCyan, .9f * eggIn(t, 2.2f, .6f) * fade)
        // ACCESS GRANTED stamps in with a hard pop and a short flash.
        val stamp = eggIn(t, 3.6f, .25f) * fade
        if (stamp > 0f) {
            val pop = 1f + .35f * (1f - eggEase((t - 3.6f) / .35f))
            val label = layout("ACCESS GRANTED", 17.sp, FontWeight.Bold, FontFamily.Monospace, 4.sp)
            val boxW = (label.size.width + 36f * dp) * pop
            val boxH = (label.size.height + 18f * dp) * pop
            val boxTop = lineY + 74f * dp
            drawRoundRect(cyberYellow, Offset(cx - boxW / 2f, boxTop), Size(boxW, boxH), CornerRadius(4f * dp),
                alpha = .16f * stamp, blendMode = BlendMode.Plus)
            drawRoundRect(cyberYellow, Offset(cx - boxW / 2f, boxTop), Size(boxW, boxH), CornerRadius(4f * dp),
                alpha = stamp, style = Stroke(2f * dp))
            centred(label, cx, boxTop + (boxH - label.size.height) / 2f, cyberYellow, stamp)
            val flash = 1f - eggEase((t - 3.6f) / .4f)
            if (flash > 0f && t >= 3.6f) drawRect(Color.White, alpha = .35f * flash * fade, blendMode = BlendMode.Plus)
        }
    }
}
