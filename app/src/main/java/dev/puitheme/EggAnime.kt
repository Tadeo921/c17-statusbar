// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private val animeSkyTop = Color(0xFF3D7BFF)
private val animeSkyMid = Color(0xFF8EC5FF)
private val animeSkyLow = Color(0xFFFFD6E7)
private val animePink = Color(0xFFFF8FB8)
private val animeSakura = Color(0xFFFFC4D8)

private val phantomRed = Color(0xFFE60012)
private val phantomInk = Color(0xFF0B0B0B)

/** When the clear sky is slashed open and the red-black phantom style takes over. */
private const val SHATTER = 1.5f

/**
 * 樱花晴空: opens as a bright anime sky with clouds, speed lines and a sakura storm, then a jagged slash tears it
 * into a red-black calling-card style: sunburst, halftone, shards, stars and a ransom-note title.
 */
internal class AnimeEgg(icon: ImageBitmap, measurer: TextMeasurer) : EggScene(icon, measurer) {
    private class Petal(val x: Float, val y: Float, val fall: Float, val sway: Float, val phase: Float, val size: Float,
        val spin: Float, val depth: Float)
    private class Sparkle(val x: Float, val y: Float, val born: Float, val size: Float)
    private class Puff(val x: Float, val y: Float, val r: Float)

    private val petals: List<Petal>
    private val sparkles: List<Sparkle>
    private val cloudBank: List<Puff>
    private val speedLines: List<Float>
    /** One sakura petal, unit size, notched at the tip like the real flower. */
    private val petalPath = Path().apply {
        moveTo(0f, -1f)
        cubicTo(.75f, -.75f, .7f, .45f, 0f, 1f)
        cubicTo(-.7f, .45f, -.75f, -.75f, 0f, -1f)
        close()
    }

    init {
        val random = Random(39)
        petals = List(90) { Petal(random.nextFloat(), random.nextFloat(), .08f + random.nextFloat() * .14f,
            .02f + random.nextFloat() * .05f, random.nextFloat() * EGG_TAU, 5f + random.nextFloat() * 7f,
            (random.nextFloat() - .5f) * 6f, .4f + random.nextFloat() * .6f) }
        sparkles = List(26) { Sparkle(.08f + random.nextFloat() * .84f, .12f + random.nextFloat() * .55f,
            1.4f + random.nextFloat() * 5f, 8f + random.nextFloat() * 14f) }
        cloudBank = List(14) { index -> Puff(index / 13f, random.nextFloat() * .05f, .09f + random.nextFloat() * .08f) }
        speedLines = List(72) { random.nextFloat() }
    }

    override fun DrawScope.draw(t: Float, fade: Float) {
        val w = size.width
        val h = size.height
        val iris = eggEase(t / .7f)
        drawRect(Brush.verticalGradient(listOf(animeSkyTop, animeSkyMid, animeSkyLow, Color.White), 0f, h),
            alpha = fade)
        sunGlow(t, fade, w, h)
        clouds(t, fade, w, h)
        speed(t, fade, w, h)
        if (t >= SHATTER) phantom(t, fade, w, h)
        petalStorm(t, fade, w, h, back = true)
        hero(t, fade, w, h)
        petalStorm(t, fade, w, h, back = false)
        if (t >= SHATTER) {
            ransomTitle(t, fade, w, h)
            callingCard(t, fade, w, h)
        }
        sparkleField(t, fade, w, h)
        // Opening iris: the scene opens from a circle like an anime eyecatch.
        if (iris < 1f) {
            val big = size.maxDimension * 2f
            drawCircle(Color.Black, iris * size.maxDimension + big / 2f, Offset(w / 2f, h * .4f), style = Stroke(big))
        }
    }

    /** Four-point "kira" star, unit size. */
    private val kira = Path().apply {
        moveTo(0f, -1f)
        quadraticTo(.12f, -.12f, 1f, 0f)
        quadraticTo(.12f, .12f, 0f, 1f)
        quadraticTo(-.12f, .12f, -1f, 0f)
        quadraticTo(-.12f, -.12f, 0f, -1f)
        close()
    }

    /** The spiky impact burst behind the icon, unit radius. */
    private val burst = Path().apply {
        val spikes = 18
        for (i in 0 until spikes * 2) {
            val angle = i / (spikes * 2f) * EGG_TAU
            val r = if (i % 2 == 0) 1f else .72f
            if (i == 0) moveTo(cos(angle) * r, sin(angle) * r) else lineTo(cos(angle) * r, sin(angle) * r)
        }
        close()
    }

    private fun DrawScope.sunGlow(t: Float, fade: Float, w: Float, h: Float) {
        val sun = Offset(w * .84f, h * .09f)
        val show = eggIn(t, .2f, 1f) * fade
        drawCircle(Brush.radialGradient(listOf(Color.White, Color(0xCCFFF6C8), Color.Transparent), sun, w * .55f),
            w * .55f, sun, alpha = show)
        // God rays fanning from the sun, slowly turning.
        rotate(8f * sin(t * .4f), sun) {
            for (i in 0 until 9) {
                val angle = 95f + i * 9f
                rotate(angle, sun) {
                    drawRect(Brush.horizontalGradient(listOf(Color(0x66FFFFFF), Color.Transparent), sun.x, sun.x + h),
                        Offset(sun.x, sun.y - (6f + i % 3 * 5f) * density), Size(h, (12f + i % 3 * 10f) * density),
                        alpha = show * (.5f + .5f * eggWave(t, 1.3f, i.toFloat())), blendMode = BlendMode.Plus)
                }
            }
        }
        // Lens flare ghosts along the line through the screen centre.
        val axis = Offset(w / 2f - sun.x, h * .45f - sun.y)
        val ghosts = listOf(.35f to 18f, .6f to 34f, .85f to 12f, 1.2f to 46f, 1.45f to 22f)
        for ((k, r) in ghosts) {
            val c = Offset(sun.x + axis.x * k, sun.y + axis.y * k)
            val tint = if (k < 1f) Color(0x55FFE9A8) else Color(0x5590E0FF)
            drawCircle(tint, r * density, c, alpha = show * .8f, blendMode = BlendMode.Plus)
            drawCircle(Color.White, r * density, c, alpha = show * .25f, style = Stroke(1.5f * density), blendMode = BlendMode.Plus)
        }
    }

    private fun DrawScope.clouds(t: Float, fade: Float, w: Float, h: Float) {
        val rise = eggEase(t / 1.2f)
        val base = h * (.95f - .12f * rise)
        // Shadowed undersides first, then the bright tops: the classic cel-shaded cumulus.
        for (puff in cloudBank) {
            val x = puff.x * w + sin(t * .5f + puff.x * 9f) * 6f * density
            drawCircle(Color(0xFFB8CFF5), puff.r * w * 1.05f, Offset(x + 6f * density, base + puff.y * h + 10f * density), alpha = fade)
        }
        for (puff in cloudBank) {
            val x = puff.x * w + sin(t * .5f + puff.x * 9f) * 6f * density
            drawCircle(Color.White, puff.r * w, Offset(x, base + puff.y * h), alpha = fade)
        }
        drawRect(Color.White, Offset(0f, base + h * .05f), Size(w, h), alpha = fade)
        // Two small clouds drift across the upper sky.
        for (k in 0 until 2) {
            val x = ((.1f + k * .55f + t * .025f * (k + 1)) % 1.2f - .1f) * w
            val y = h * (.2f + k * .1f)
            for ((dx, r) in listOf(0f to 22f, 24f to 30f, 52f to 20f, 26f to 18f)) {
                drawCircle(Color(0xFFDDE9FF), r * density, Offset(x + dx * density + 3f * density, y + 5f * density), alpha = .9f * fade)
                drawCircle(Color.White, r * density, Offset(x + dx * density, y), alpha = .95f * fade)
            }
        }
    }

    private fun DrawScope.speed(t: Float, fade: Float, w: Float, h: Float) {
        val show = eggWindow(t, .45f, 2f, .1f, .6f) * fade
        if (show <= 0f) return
        val center = Offset(w / 2f, h * .38f)
        val outer = size.maxDimension
        speedLines.forEachIndexed { index, jitter ->
            val angle = index / speedLines.size.toFloat() * EGG_TAU + jitter * .05f
            val inner = (150f + jitter * 70f + 20f * sin(t * 20f + index)) * density
            val dir = Offset(cos(angle), sin(angle))
            drawLine(Color.White, center + dir * inner, center + dir * outer, (1f + jitter * 3.5f) * density,
                alpha = show * (.35f + .5f * eggNoise(index, (t * 24f).toInt())))
        }
    }

    private fun DrawScope.hero(t: Float, fade: Float, w: Float, h: Float) {
        val center = Offset(w / 2f, h * .38f + sin(t * 2.2f) * 6f * density)
        val pop = eggOvershoot((t - .5f) / .7f)
        if (pop <= 0f) return
        val iconSize = 112f * density * pop
        // Impact burst: pink and white in the sky, black and white once the phantom style takes over.
        val phantom = t >= SHATTER
        rotate(t * 25f, center) {
            translate(center.x, center.y) {
                scale(iconSize * 1.3f, iconSize * 1.3f, Offset.Zero) {
                    drawPath(burst, if (phantom) phantomInk else animePink, alpha = .95f * fade)
                }
                scale(iconSize * 1.08f, iconSize * 1.08f, Offset.Zero) { drawPath(burst, Color.White, alpha = fade) }
                if (phantom) scale(iconSize * .9f, iconSize * .9f, Offset.Zero) { drawPath(burst, phantomRed, alpha = fade) }
            }
        }
        drawCircle(Color(0xFF2B2F6B), iconSize * .62f, center + Offset(4f * density, 6f * density), alpha = .25f * fade)
        drawIcon(icon, center, iconSize, fade)
        // A shine stripe sweeps the icon once it has landed.
        val sweep = (t - 1.2f) / .6f
        if (sweep in 0f..1f) {
            val x = center.x - iconSize + sweep * iconSize * 2f
            val half = iconSize / 2f
            clipRect(center.x - half, center.y - half, center.x + half, center.y + half) {
                rotate(20f, center) {
                    drawRect(Brush.horizontalGradient(listOf(Color.Transparent, Color(0xCCFFFFFF), Color.Transparent),
                        x - 18f * density, x + 18f * density), Offset(x - 18f * density, center.y - iconSize),
                        Size(36f * density, iconSize * 2f), alpha = fade, blendMode = BlendMode.Plus)
                }
            }
        }
    }

    private fun DrawScope.petalStorm(t: Float, fade: Float, w: Float, h: Float, back: Boolean) {
        val show = eggIn(t, .3f, .8f) * fade
        if (show <= 0f) return
        for (petal in petals) {
            if ((petal.depth < .7f) != back) continue
            val x = ((petal.x + sin(t * 1.3f + petal.phase) * petal.sway + t * .05f) % 1.1f - .05f) * w
            val y = ((petal.y + t * petal.fall) % 1.1f - .05f) * h
            val s = petal.size * petal.depth * density
            val flip = cos(t * 3f + petal.phase)
            rotate(t * petal.spin * 57.3f + petal.phase * 57.3f, Offset(x, y)) {
                translate(x, y) {
                    scale(s * (.35f + .65f * kotlin.math.abs(flip)), s, Offset.Zero) {
                        val color = when {
                            t < SHATTER -> if (flip > 0f) animeSakura else animePink
                            flip > 0f -> Color.White
                            else -> phantomInk
                        }
                        drawPath(petalPath, color, alpha = show * (.55f + .45f * petal.depth))
                    }
                }
            }
        }
    }

    private fun DrawScope.sparkleField(t: Float, fade: Float, w: Float, h: Float) {
        for (s in sparkles) {
            val age = t - s.born
            if (age < 0f || age > .9f) continue
            val p = age / .9f
            val r = s.size * density * sin(p * Math.PI.toFloat())
            val c = Offset(s.x * w, s.y * h)
            if (t < SHATTER) {
                rotate(p * 90f, c) {
                    translate(c.x, c.y) { scale(r, r, Offset.Zero) { drawPath(kira, Color.White, alpha = fade) } }
                }
                drawCircle(Color(0xFFFFF3B0), r * .7f, c, alpha = .5f * fade, blendMode = BlendMode.Plus)
            } else {
                // Phantom stars: a black star with a white one inset, spinning as they pop.
                rotate(p * 160f, c) {
                    translate(c.x, c.y) {
                        scale(r * 1.4f, r * 1.4f, Offset.Zero) { drawPath(star, phantomInk, alpha = fade) }
                        scale(r * .9f, r * .9f, Offset.Zero) { drawPath(star, Color.White, alpha = fade) }
                    }
                }
            }
        }
    }

    /** Five-point star, unit radius. */
    private val star = Path().apply {
        for (i in 0 until 10) {
            val angle = -Math.PI.toFloat() / 2f + i * Math.PI.toFloat() / 5f
            val r = if (i % 2 == 0) 1f else .45f
            if (i == 0) moveTo(cos(angle) * r, sin(angle) * r) else lineTo(cos(angle) * r, sin(angle) * r)
        }
        close()
    }

    /** Black shards in unit coordinates (x by width, y by height). */
    private val shardShapes = listOf(
        floatArrayOf(-.1f, .70f, 1.1f, .47f, 1.1f, .58f, -.1f, .84f),
        floatArrayOf(.6f, -.05f, 1.1f, -.05f, 1.1f, .3f, .8f, .2f),
        floatArrayOf(-.1f, .04f, .3f, -.05f, .1f, .28f, -.1f, .34f))
    private var shardSize = Size.Zero
    private var shards: List<Path> = emptyList()

    private fun shardPaths(w: Float, h: Float): List<Path> {
        if (shardSize.width == w && shardSize.height == h) return shards
        shardSize = Size(w, h)
        shards = shardShapes.map { shape ->
            Path().apply {
                moveTo(shape[0] * w, shape[1] * h)
                for (i in 2 until shape.size step 2) lineTo(shape[i] * w, shape[i + 1] * h)
                close()
            }
        }
        return shards
    }

    private fun DrawScope.phantom(t: Float, fade: Float, w: Float, h: Float) {
        val wipe = eggEaseInOut((t - SHATTER) / .35f)
        if (wipe < 1f) {
            // A jagged diagonal cut sweeps across; the red world is drawn only on its trailing side.
            val edge = -w * .3f + w * 1.9f * wipe
            val tooth = 22f * density
            val cut = Path().apply {
                moveTo(-w, -10f)
                lineTo(edge, -10f)
                for (i in 1..8) {
                    val y = h * i / 8f
                    lineTo(edge - y * .35f + (if (i % 2 == 0) tooth else -tooth), y)
                }
                lineTo(-w, h + 10f)
                close()
            }
            clipPath(cut) { redWorld(t, fade, w, h) }
            drawPath(cut, Color.White, alpha = fade, style = Stroke(7f * density))
            drawPath(cut, phantomInk, alpha = fade, style = Stroke(2.5f * density))
        } else redWorld(t, fade, w, h)
        val flash = eggWindow(t, SHATTER + .3f, SHATTER + .7f, .05f, .3f)
        if (flash > 0f) drawRect(Color.White, alpha = .7f * flash * fade)
    }

    private fun DrawScope.redWorld(t: Float, fade: Float, w: Float, h: Float) {
        drawRect(phantomRed, alpha = fade)
        // Rotating sunburst behind the icon.
        val center = Offset(w / 2f, h * .38f)
        val reach = size.maxDimension * 1.3f
        rotate(t * 12f, center) {
            for (i in 0 until 16) drawArc(phantomInk, i * 22.5f, 11.25f, true, Offset(center.x - reach, center.y - reach),
                Size(reach * 2f, reach * 2f), alpha = .88f * fade)
        }
        // Halftone dots growing toward two corners.
        val step = 12f * density
        var y = h * .7f
        while (y < h + step) {
            var x = 0f
            while (x < w * .6f) {
                val f = (1f - kotlin.math.hypot(x, h - y) / (w * .6f)).coerceIn(0f, 1f)
                if (f > .08f) drawCircle(phantomInk, 5.5f * density * f, Offset(x, y), alpha = fade)
                x += step
            }
            y += step
        }
        y = 0f
        while (y < h * .25f) {
            var x = w * .45f
            while (x < w + step) {
                val f = (1f - kotlin.math.hypot(w - x, y) / (w * .55f)).coerceIn(0f, 1f)
                if (f > .08f) drawCircle(Color.White, 5f * density * f, Offset(x, y), alpha = .55f * fade)
                x += step
            }
            y += step
        }
        val drift = sin(t * 1.5f) * 4f * density
        translate(drift, 0f) {
            for (shard in shardPaths(w, h)) {
                drawPath(shard, phantomInk, alpha = fade)
                drawPath(shard, Color.White, alpha = fade, style = Stroke(3f * density))
            }
        }
    }

    private val ransomFamilies = listOf(FontFamily.Serif, FontFamily.SansSerif, FontFamily.Monospace)

    /** STATUS C17 as cut-out letters: every letter its own box, font, tilt and colour, popping in one by one. */
    private fun DrawScope.ransomTitle(t: Float, fade: Float, w: Float, h: Float) {
        val word = "STATUS C17"
        val pad = 6f * density
        val gap = 16f * density
        val letters = word.mapIndexed { index, ch ->
            if (ch == ' ') null else layout(ch.toString(), (36f + 14f * eggNoise(index, 1)).sp, FontWeight.Black,
                ransomFamilies[(eggNoise(index, 2) * 3f).toInt().coerceIn(0, 2)])
        }
        val total = letters.sumOf { (it?.size?.width?.plus(pad * 2f) ?: gap).toDouble() }.toFloat()
        val fit = ((w * .92f) / total).coerceAtMost(1f)
        val baseline = h * .6f
        var x: Float
        scale(fit, fit, Offset(w / 2f, baseline)) {
            x = (w - total) / 2f
            letters.forEachIndexed { index, letter ->
                if (letter == null) { x += gap; return@forEachIndexed }
                val boxW = letter.size.width + pad * 2f
                val boxH = letter.size.height + pad
                val appear = eggOvershoot((t - (SHATTER + .4f + index * .06f)) / .3f)
                val box = Offset(x + boxW / 2f, baseline + (eggNoise(index, 4) - .5f) * 12f * density)
                x += boxW
                if (appear <= 0f) return@forEachIndexed
                val style = (eggNoise(index, 5) * 3f).toInt()
                val (fill, ink) = when (style) {
                    0 -> Color.White to phantomInk
                    1 -> phantomInk to Color.White
                    else -> phantomRed to Color.White
                }
                rotate((eggNoise(index, 3) - .5f) * 20f, box) {
                    scale(appear, appear, box) {
                        val topLeft = Offset(box.x - boxW / 2f, box.y - boxH / 2f)
                        drawRect(phantomInk, topLeft + Offset(4f * density, 4f * density), Size(boxW, boxH), alpha = .6f * fade)
                        if (style == 2) drawRect(Color.White, topLeft - Offset(2.5f * density, 2.5f * density),
                            Size(boxW + 5f * density, boxH + 5f * density), alpha = fade)
                        drawRect(fill, topLeft, Size(boxW, boxH), alpha = fade)
                        centred(letter, box.x, box.y - letter.size.height / 2f, ink, fade)
                    }
                }
            }
        }
    }

    /** The tagline and the thanks line in tilted calling-card strips. */
    private fun DrawScope.callingCard(t: Float, fade: Float, w: Float, h: Float) {
        val cx = w / 2f
        val tagShow = eggEase((t - (SHATTER + 1f)) / .35f) * fade
        if (tagShow > 0f) {
            val tag = layout("Cielo de sakura · el ladrón ya ha aparecido", 17.sp, FontWeight.Black)
            val top = h * .6f + 58f * density
            val slide = (1f - tagShow) * w
            rotate(-5f, Offset(cx, top)) {
                val stripW = tag.size.width + 64f * density
                val stripH = tag.size.height + 16f * density
                drawRect(phantomInk, Offset(cx - stripW / 2f + slide, top - 8f * density), Size(stripW, stripH), alpha = tagShow)
                drawRect(phantomRed, Offset(cx - stripW / 2f + slide - 6f * density, top - 16f * density),
                    Size(58f * density, 18f * density), alpha = tagShow)
                val label = layout("Carta de aviso", 11.sp, FontWeight.Black)
                centred(label, cx - stripW / 2f + slide + 23f * density, top - 15f * density, Color.White, tagShow)
                centred(tag, cx + slide + 10f * density, top, Color.White, tagShow)
            }
        }
        val thanksShow = eggEase((t - (SHATTER + 1.35f)) / .35f) * fade
        if (thanksShow > 0f) {
            val thanks = layout(EGG_THANKS, 14.sp, FontWeight.Bold)
            val top = h * .6f + 112f * density
            val slide = (1f - thanksShow) * -w
            rotate(3f, Offset(cx, top)) {
                val stripW = thanks.size.width + 36f * density
                drawRect(phantomInk, Offset(cx - stripW / 2f + slide + 4f * density, top - 4f * density),
                    Size(stripW, thanks.size.height + 12f * density), alpha = .6f * thanksShow)
                drawRect(Color.White, Offset(cx - stripW / 2f + slide, top - 8f * density),
                    Size(stripW, thanks.size.height + 12f * density), alpha = thanksShow)
                centred(thanks, cx + slide, top - 2f * density, phantomInk, thanksShow)
            }
        }
    }
}
