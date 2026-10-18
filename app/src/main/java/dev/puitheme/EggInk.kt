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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
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

private val inkPaper = Color(0xFFF3ECDC)
private val inkPaperDeep = Color(0xFFE4D6B8)
private val inkBlack = Color(0xFF1A1A1C)
private val inkCinnabar = Color(0xFFC8322B)
private val inkGold = Color(0xFFC9A247)

/**
 * 水墨山河: a gold-glinting ink drop falls on black, floods the screen and recedes into a painted landscape; two koi
 * circle the icon in an ensō, plum branches grow and blossom, and the red seal bursts into gold leaf.
 */
internal class InkEgg(icon: ImageBitmap, measurer: TextMeasurer) : EggScene(icon, measurer) {
    private class Blob(val dx: Float, val dy: Float, val r: Float, val delay: Float)
    private class Fleck(val x: Float, val y: Float, val drift: Float, val phase: Float, val size: Float)
    private class Bird(val y: Float, val delay: Float, val scale: Float)
    /** One plum branch segment in unit coordinates, grown at [born]; [flower] ends carry a blossom. */
    private class Twig(val x0: Float, val y0: Float, val x1: Float, val y1: Float, val width: Float, val born: Float,
        val flower: Boolean)
    private class Flake(val vx: Float, val vy: Float, val spin: Float, val size: Float, val gold: Boolean)

    private val blooms: List<Blob>
    private val flecks: List<Fleck>
    private val birds: List<Bird>
    private val twigs = ArrayList<Twig>()
    private val flakes: List<Flake>
    /** Three ridge lines (back to front), unit width; heights are fractions of the screen height. */
    private val ridges: List<FloatArray>

    init {
        val random = Random(1127)
        blooms = List(26) { Blob((random.nextFloat() - .5f) * 1.1f, (random.nextFloat() - .5f) * .9f,
            .25f + random.nextFloat() * .5f, random.nextFloat() * .5f) }
        flecks = List(46) { Fleck(random.nextFloat(), random.nextFloat(), .01f + random.nextFloat() * .03f,
            random.nextFloat() * EGG_TAU, 1f + random.nextFloat() * 2.5f) }
        birds = List(5) { index -> Bird(.16f + random.nextFloat() * .12f, 1.2f + index * .35f, .7f + random.nextFloat() * .5f) }
        // One plum branch rising from the lower left, answering the red sun across the diagonal.
        grow(random, .0f, .99f, -.9f, .34f, 8f, 1.35f, 0)
        flakes = List(40) {
            val angle = random.nextFloat() * EGG_TAU
            val speed = 120f + random.nextFloat() * 260f
            Flake(cos(angle) * speed, sin(angle) * speed - 140f, (random.nextFloat() - .5f) * 14f, 3f + random.nextFloat() * 5f,
                random.nextFloat() < .8f)
        }
        ridges = List(3) { layer ->
            val points = 9 + layer * 2
            FloatArray(points) { .07f + random.nextFloat() * (.1f + layer * .03f) }
        }
    }

    /** Recursive branch: a slightly bending limb that forks twice per level and ends in blossoms. */
    private fun grow(random: Random, x: Float, y: Float, angle: Float, length: Float, width: Float, born: Float, depth: Int) {
        var px = x
        var py = y
        var heading = angle
        val parts = 3
        for (i in 0 until parts) {
            heading += (random.nextFloat() - .5f) * .5f
            val nx = px + cos(heading) * length / parts
            val ny = py + sin(heading) * length / parts * .6f
            val last = i == parts - 1
            twigs += Twig(px, py, nx, ny, width * (1f - i * .18f), born + depth * .3f + i * .1f, last && depth >= 2)
            if (depth < 3 && i > 0) {
                val side = if (random.nextBoolean()) 1f else -1f
                grow(random, nx, ny, heading + side * (.5f + random.nextFloat() * .5f), length * .55f, width * .55f,
                    born + .1f, depth + 1)
            }
            px = nx
            py = ny
        }
        if (depth >= 1) twigs += Twig(px, py, px, py, 0f, born + depth * .3f + .3f, true)
    }

    override fun DrawScope.draw(t: Float, fade: Float) {
        val w = size.width
        val h = size.height
        drawRect(Brush.radialGradient(listOf(inkPaper, inkPaperDeep), Offset(w / 2f, h * .4f), size.maxDimension * .8f),
            alpha = .99f * fade)
        sunAndBirds(t, fade, w, h)
        mountains(t, fade, w, h)
        water(t, fade, w, h)
        plum(t, fade, w, h)
        inkDrop(t, fade, w, h)
        enso(t, fade, w, h)
        koi(t, fade, w, h)
        calligraphy(t, fade, w, h)
        goldFlecks(t, fade, w, h)
        flood(t, fade, w, h)
    }

    private var ridgeSize = Size.Zero
    private var ridgePaths: List<Path> = emptyList()

    /** Smooth mountain silhouettes, rebuilt only when the window size changes. */
    private fun ridgePaths(w: Float, h: Float): List<Path> {
        if (ridgeSize.width == w && ridgeSize.height == h) return ridgePaths
        ridgeSize = Size(w, h)
        ridgePaths = ridges.mapIndexed { layer, peaks ->
            val base = h * (.76f + layer * .065f)
            val step = w * 1.3f / (peaks.size - 1)
            Path().apply {
                moveTo(-w * .15f, h)
                var px = -w * .15f
                var py = base - peaks[0] * h * .9f
                lineTo(px, py)
                for (i in 1 until peaks.size) {
                    val x = -w * .15f + i * step
                    val y = base - peaks[i] * h * .9f * (if (i % 2 == 0) 1f else .45f)
                    quadraticTo(px, py, (px + x) / 2f, (py + y) / 2f)
                    px = x
                    py = y
                }
                lineTo(px, py)
                lineTo(w * 1.2f, h)
                close()
            }
        }
        return ridgePaths
    }

    private fun DrawScope.sunAndBirds(t: Float, fade: Float, w: Float, h: Float) {
        val sun = Offset(w * .77f, h * .12f)
        val show = eggIn(t, .3f, 1.2f) * fade
        drawCircle(Brush.radialGradient(listOf(inkCinnabar.copy(alpha = .35f), Color.Transparent), sun, 110f * density),
            110f * density, sun, alpha = show)
        drawCircle(inkCinnabar, 40f * density, sun, alpha = .85f * show)
        val wing = 9f * density
        for (bird in birds) {
            val age = t - bird.delay
            if (age < 0f) continue
            val x = w * 1.05f - age * w * .16f * bird.scale
            val y = h * bird.y + sin(age * 1.5f) * 10f * density
            val flap = sin(age * 9f + bird.y * 20f) * 5f * density
            val s = bird.scale
            drawLine(inkBlack, Offset(x, y), Offset(x - wing * s, y - flap * s - 3f * density), 1.6f * density * s,
                StrokeCap.Round, alpha = .8f * fade)
            drawLine(inkBlack, Offset(x, y), Offset(x + wing * s, y - flap * s - 3f * density), 1.6f * density * s,
                StrokeCap.Round, alpha = .8f * fade)
        }
    }

    private fun DrawScope.mountains(t: Float, fade: Float, w: Float, h: Float) {
        val paths = ridgePaths(w, h)
        val strength = listOf(.28f, .5f, .85f)
        paths.forEachIndexed { layer, path ->
            val rise = eggIn(t, .15f + layer * .2f, 1.4f)
            if (rise <= 0f) return@forEachIndexed
            // Dark along the ridge, washing out quickly below it: clean paper shows between the layers.
            val base = h * (.76f + layer * .065f)
            val top = base - (.17f + layer * .03f) * .9f * h
            translate(sin(t * .3f + layer) * 6f * density, (1f - rise) * h * .25f) {
                drawPath(path, Brush.verticalGradient(listOf(inkBlack.copy(alpha = strength[layer]),
                    inkBlack.copy(alpha = strength[layer] * .35f), inkPaper.copy(alpha = .0f)), top, base + h * .03f),
                    alpha = rise * fade)
                drawPath(path, inkGold, alpha = (.25f + layer * .15f) * rise * fade, style = Stroke(1.2f * density))
            }
            // A drifting mist band between layers.
            val mistY = base + h * .01f
            val drift = ((t * .04f * (layer + 1)) % 1f) * w
            for (k in -1..1) {
                val mist = Offset(drift + k * w, mistY)
                // A circle squashed into a soft ellipse, so the mist fades on every side.
                scale(1f, 60f * density / w, mist) {
                    drawCircle(Brush.radialGradient(listOf(Color(0xAAF7F1E4), Color.Transparent), mist, w * .5f), w * .5f, mist,
                        alpha = .7f * rise * fade)
                }
            }
        }
    }

    private fun DrawScope.inkDrop(t: Float, fade: Float, w: Float, h: Float) {
        val center = Offset(w / 2f, h * .3f)
        val radius = 120f * density
        if (t < .6f) return
        // The ink lightens after the bloom so the icon stays the star.
        val settle = 1f - .45f * eggIn(t, 1.6f, 1.6f)
        for (blob in blooms) {
            val grow = eggEase((t - .6f - blob.delay) / 1.3f)
            if (grow <= 0f) continue
            val c = Offset(center.x + blob.dx * radius * .6f * grow, center.y + blob.dy * radius * .6f * grow)
            drawCircle(Brush.radialGradient(listOf(inkBlack.copy(alpha = .26f), inkBlack.copy(alpha = .12f), Color.Transparent),
                c, blob.r * radius * grow + 1f), blob.r * radius * grow + 1f, c, alpha = settle * fade)
        }
        // A paper-coloured hollow opens in the ink, and the icon rises out of it.
        val reveal = eggIn(t, 1f, .6f) * fade
        val iconSize = 100f * density * (.85f + .15f * reveal)
        drawCircle(Brush.radialGradient(listOf(inkPaper, inkPaper.copy(alpha = 0f)), center, iconSize * .95f), iconSize * .95f,
            center, alpha = reveal)
        drawCircle(inkGold, iconSize * .74f, center, alpha = .8f * reveal, style = Stroke(1.5f * density))
        drawIcon(icon, center, iconSize, reveal)
    }

    private fun DrawScope.enso(t: Float, fade: Float, w: Float, h: Float) {
        val progress = eggEaseInOut((t - 1.3f) / 1f)
        if (progress <= 0f) return
        val center = Offset(w / 2f, h * .3f)
        val radius = 92f * density
        val segments = 90
        val total = 330f
        val drawn = (segments * progress).toInt()
        val box = Offset(center.x - radius, center.y - radius)
        val boxSize = Size(radius * 2f, radius * 2f)
        for (i in 0 until drawn) {
            val f = i / segments.toFloat()
            // Heavy where the brush lands, thinning into a dry tail.
            val pressure = sin(Math.PI.toFloat() * (.15f + .85f * (1f - f))).coerceAtLeast(.15f)
            val width = (2.5f + 8f * pressure + 2f * eggNoise(i, 7)) * density
            drawArc(inkBlack, -70f + f * total, total / segments + .6f, false, box, boxSize,
                alpha = (.6f + .35f * pressure) * fade, style = Stroke(width, cap = StrokeCap.Round))
            if (f > .55f && eggNoise(i, 9) > .35f) {
                val dry = 5f * density
                drawArc(inkBlack, -70f + f * total, total / segments, false, Offset(box.x - dry, box.y - dry),
                    Size(boxSize.width + dry * 2f, boxSize.height + dry * 2f), alpha = .35f * fade, style = Stroke(1.2f * density))
            }
        }
    }

    private fun DrawScope.calligraphy(t: Float, fade: Float, w: Float, h: Float) {
        val cx = w / 2f
        val top = h * .3f + 128f * density
        val show = eggIn(t, 1.9f, .8f) * fade
        if (show > 0f) {
            val title = layout("Status C17", 40.sp, FontWeight.Bold, FontFamily.Serif, 2.sp)
            // A faint offset copy reads as ink soaking into the paper.
            centred(title, cx + 1.5f * density, top + 1.5f * density, inkBlack, .18f * show)
            centred(title, cx, top, inkBlack, show)
            val sweep = (t - 3.05f) / .9f
            if (sweep in 0f..1f) {
                val left = cx - title.size.width / 2f
                val bandX = left + (title.size.width + 80f * density) * sweep - 40f * density
                centred(title, cx, top, Brush.horizontalGradient(listOf(Color.Transparent, inkGold, Color(0xFFFFF1C2), inkGold,
                    Color.Transparent), bandX - 40f * density, bandX + 40f * density), show)
            }
            val line = layout("方寸之间 · 自有山河", 17.sp, FontWeight.Medium, FontFamily.Serif, 4.sp)
            val lineTop = top + title.size.height + 6f * density
            centred(line, cx, lineTop, Color(0xFF3B3B3F), eggIn(t, 2.3f, .7f) * fade)
            val thanks = layout(EGG_THANKS, 13.sp, FontWeight.Normal, FontFamily.Serif)
            centred(thanks, cx, lineTop + line.size.height + 8f * density, Color(0xFF6A645A), eggIn(t, 2.6f, .7f) * fade)
            seal(t, fade, cx + title.size.width / 2f + 22f * density, top + title.size.height * .2f)
        }
        // A vertical line of verse down the left margin.
        val verse = eggIn(t, 2.8f, 1f) * fade
        if (verse > 0f) {
            val column = layout("一\n栏\n纳\n万\n象", 16.sp, FontWeight.Medium, FontFamily.Serif)
            centred(column, 34f * density, h * .1f, inkBlack, .75f * verse)
            drawLine(inkCinnabar, Offset(34f * density, h * .1f - 10f * density), Offset(34f * density, h * .1f - 2f * density),
                3f * density, StrokeCap.Round, alpha = verse)
        }
    }

    /** The red seal stamps down with a short squash, a gold shockwave and a burst of gold leaf. */
    private fun DrawScope.seal(t: Float, fade: Float, x: Float, y: Float) {
        val stamp = eggIn(t, 3f, .18f) * fade
        if (stamp <= 0f) return
        val press = 1f + .5f * (1f - eggEase((t - 3f) / .25f))
        val side = 42f * density
        val center = Offset(x, y + side / 2f)
        val age = t - 3f
        if (age < .7f) drawCircle(inkGold, eggEase(age / .7f) * 140f * density + 1f, center, alpha = (1f - age / .7f) * fade,
            style = Stroke(3f * density))
        if (age < 1.8f) for (flake in flakes) {
            val p = Offset(center.x + flake.vx * age * density, center.y + (flake.vy * age + 240f * age * age) * density)
            val flip = cos(age * flake.spin)
            val s = flake.size * density
            rotate(age * flake.spin * 40f, p) {
                drawRect(if (flake.gold) inkGold else Color(0xFFFFE9A8), Offset(p.x - s * kotlin.math.abs(flip) / 2f, p.y - s / 2f),
                    Size((s * kotlin.math.abs(flip)).coerceAtLeast(1f), s), alpha = (1f - age / 1.8f) * fade)
            }
        }
        rotate(-6f, center) {
            scale(press, press, center) {
                drawRoundRect(inkCinnabar, Offset(center.x - side / 2f, center.y - side / 2f), Size(side, side),
                    CornerRadius(5f * density), alpha = .92f * stamp)
                val inset = 3.5f * density
                drawRoundRect(Color(0xFFF7E9D8), Offset(center.x - side / 2f + inset, center.y - side / 2f + inset),
                    Size(side - inset * 2f, side - inset * 2f), CornerRadius(3f * density), alpha = .9f * stamp,
                    style = Stroke(1.2f * density))
                val mark = layout("C17", 15.sp, FontWeight.Black, FontFamily.Serif)
                centred(mark, center.x, center.y - mark.size.height / 2f, Color(0xFFF7E9D8), stamp)
            }
        }
    }

    private fun DrawScope.goldFlecks(t: Float, fade: Float, w: Float, h: Float) {
        val show = eggIn(t, .9f, 1f) * fade
        if (show <= 0f) return
        for (fleck in flecks) {
            val x = (fleck.x + sin(t * .7f + fleck.phase) * .02f) * w
            val y = ((fleck.y - t * fleck.drift) % 1f + 1f) % 1f * h
            val twinkle = .4f + .6f * eggWave(t, 3f, fleck.phase)
            drawCircle(inkGold, fleck.size * density, Offset(x, y), alpha = twinkle * show)
            drawCircle(Color(0xFFFFF1C2), fleck.size * .45f * density, Offset(x, y), alpha = twinkle * show)
        }
    }

    /** The opening: black ink with a falling gold-rimmed drop, then the ink recedes from the impact in a ragged ring. */
    private fun DrawScope.flood(t: Float, fade: Float, w: Float, h: Float) {
        val impact = Offset(w / 2f, h * .3f)
        if (t < .55f) {
            drawRect(inkBlack, alpha = fade)
            val y = eggEaseInOut((t - .1f) / .45f) * impact.y
            if (t > .1f) {
                drawCircle(Brush.radialGradient(listOf(inkGold.copy(alpha = .5f), Color.Transparent), Offset(impact.x, y), 40f * density),
                    40f * density, Offset(impact.x, y), alpha = fade)
                drawCircle(Color(0xFF050505), 8f * density, Offset(impact.x, y), alpha = fade)
                drawCircle(inkGold, 8f * density, Offset(impact.x, y), alpha = fade, style = Stroke(1.5f * density))
                drawCircle(Color.White, 2f * density, Offset(impact.x - 3f * density, y - 3f * density), alpha = .9f * fade)
            }
            return
        }
        val open = eggEaseInOut((t - .55f) / 1.6f)
        if (open >= 1f) return
        val reach = size.maxDimension * 1.25f
        val radius = open * reach
        val big = size.maxDimension * 3f
        drawCircle(inkBlack, radius + big / 2f, impact, alpha = fade, style = Stroke(big))
        // Ink tongues along the rim make the edge organic instead of a clean circle.
        blooms.forEachIndexed { index, blob ->
            val angle = index / blooms.size.toFloat() * EGG_TAU + blob.dx
            val r = (24f + blob.r * 60f) * density * (1f - open * .4f)
            val at = Offset(impact.x + cos(angle) * (radius + r * .2f), impact.y + sin(angle) * (radius + r * .2f))
            drawCircle(inkBlack, r, at, alpha = fade)
        }
        // Gold ripples race outward just inside the edge.
        for (k in 0 until 4) {
            val rr = radius * (.55f + k * .13f)
            if (rr > 1f) drawCircle(inkGold, rr, impact, alpha = (1f - open) * (.7f - k * .12f) * fade,
                style = Stroke((2.5f - k * .4f) * density))
        }
        val flash = 1f - eggEase((t - .55f) / .3f)
        if (flash > 0f) drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF4D0), Color.Transparent), impact, 160f * density),
            160f * density, impact, alpha = flash * fade, blendMode = BlendMode.Plus)
    }

    private fun DrawScope.plum(t: Float, fade: Float, w: Float, h: Float) {
        twigs.forEachIndexed { index, twig ->
            val grow = ((t - twig.born) / .25f).coerceIn(0f, 1f)
            if (grow <= 0f) return@forEachIndexed
            val start = Offset(twig.x0 * w, twig.y0 * h)
            val end = Offset(twig.x1 * w, twig.y1 * h)
            if (twig.width > 0f) drawLine(inkBlack, start, start + (end - start) * grow, twig.width * density, StrokeCap.Round,
                alpha = .88f * fade)
            if (!twig.flower) return@forEachIndexed
            val pop = eggOvershoot((t - twig.born - .15f) / .35f)
            if (pop <= 0f) return@forEachIndexed
            val turn = eggNoise(index, 21) * 72f
            val petal = 5.5f * density * pop * (.75f + .5f * eggNoise(index, 22))
            for (k in 0 until 5) {
                val angle = Math.toRadians((turn + k * 72f).toDouble()).toFloat()
                drawCircle(inkCinnabar, petal, end + Offset(cos(angle), sin(angle)) * petal * .85f, alpha = .85f * fade)
            }
            drawCircle(Color(0xFFFFE7A0), petal * .45f, end, alpha = fade)
        }
    }

    /** A koi in unit length, head toward +x. */
    private val koiBody = Path().apply {
        moveTo(.5f, 0f)
        cubicTo(.45f, -.21f, -.12f, -.19f, -.38f, 0f)
        cubicTo(-.12f, .19f, .45f, .21f, .5f, 0f)
        close()
    }
    private val koiTail = Path().apply {
        moveTo(-.34f, 0f)
        lineTo(-.64f, -.18f)
        quadraticTo(-.54f, 0f, -.64f, .18f)
        close()
    }

    private fun DrawScope.koi(t: Float, fade: Float, w: Float, h: Float) {
        val show = eggIn(t, 1.7f, .6f) * fade
        if (show <= 0f) return
        val center = Offset(w / 2f, h * .3f)
        val rx = 128f * density
        val ry = 106f * density
        val length = 48f * density
        for (k in 0 until 2) {
            val dir = if (k == 0) 1f else -1f
            val angle = (t - 1.7f) * .95f * dir + k * Math.PI.toFloat() + .6f
            // An ink wake trails behind each fish.
            for (j in 1..16) {
                val a = angle - dir * j * .065f
                val p = Offset(center.x + cos(a) * rx, center.y + sin(a) * ry)
                drawCircle(inkBlack, (6.5f - j * .33f) * density, p, alpha = .2f * (1f - j / 16f) * show)
            }
            val at = Offset(center.x + cos(angle) * rx, center.y + sin(angle) * ry)
            val heading = Math.toDegrees(kotlin.math.atan2(cos(angle) * ry * dir, -sin(angle) * rx * dir).toDouble()).toFloat()
            val wiggle = sin(t * 11f + k * 2f) * 22f
            rotate(heading, at) {
                translate(at.x, at.y) {
                    scale(length, length, Offset.Zero) {
                        val tailPivot = Offset(-.34f, 0f)
                        rotate(wiggle, tailPivot) {
                            drawPath(koiTail, if (k == 0) inkCinnabar else inkGold, alpha = .9f * show)
                        }
                        drawOval(inkBlack, Offset(.08f, -.2f), Size(.16f, .08f), alpha = .5f * show)
                        drawOval(inkBlack, Offset(.08f, .12f), Size(.16f, .08f), alpha = .5f * show)
                        if (k == 0) drawPath(koiBody, Color(0xFFFFF8F0), alpha = show)
                        else drawPath(koiBody, Brush.horizontalGradient(listOf(Color(0xFFE59A2C), inkGold, Color(0xFFFFE08A)),
                            -.4f, .5f), alpha = show)
                        clipPath(koiBody) {
                            val patch = if (k == 0) inkCinnabar else inkBlack
                            drawCircle(patch, .12f, Offset(.26f, -.04f), alpha = .9f * show)
                            drawCircle(patch, .1f, Offset(-.06f, .05f), alpha = .85f * show)
                            drawCircle(patch, .07f, Offset(-.24f, -.03f), alpha = .8f * show)
                        }
                        drawPath(koiBody, inkBlack, alpha = .7f * show, style = Stroke(.018f))
                        drawCircle(inkBlack, .022f, Offset(.4f, -.06f), alpha = show)
                        drawCircle(inkBlack, .022f, Offset(.4f, .06f), alpha = show)
                    }
                }
            }
        }
    }

    /** Hull of the small boat, unit width, keel down. */
    private val hull = Path().apply {
        moveTo(-.5f, -.06f)
        quadraticTo(0f, .02f, .5f, -.14f)
        quadraticTo(.36f, .14f, 0f, .16f)
        quadraticTo(-.38f, .14f, -.5f, -.06f)
        close()
    }

    /** Still water in the lower right: a few dry-brush ripple strokes and a lone fisherman drifting by. */
    private fun DrawScope.water(t: Float, fade: Float, w: Float, h: Float) {
        val show = eggIn(t, 1.2f, 1.2f) * fade
        if (show <= 0f) return
        val strokes = listOf(Triple(.92f, .42f, .96f), Triple(.935f, .55f, 1.02f), Triple(.95f, .36f, .7f),
            Triple(.962f, .62f, .98f), Triple(.975f, .48f, .86f))
        strokes.forEachIndexed { index, (y, from, to) ->
            val sway = sin(t * .8f + index) * 6f * density
            drawLine(inkBlack, Offset(from * w + sway, y * h), Offset(to * w + sway, y * h), (1.2f - index * .1f) * density,
                StrokeCap.Round, alpha = .22f * show)
        }
        val boat = Offset(w * (.76f - t * .01f), h * .925f + sin(t * 1.6f) * 1.5f * density)
        val width = 58f * density
        rotate(sin(t * 1.6f) * 1.5f, boat) {
            translate(boat.x, boat.y) {
                scale(width, width, Offset.Zero) { drawPath(hull, inkBlack, alpha = .85f * show) }
            }
            // Fisherman: a hunched body under a wide straw hat, his line slanting into the water.
            drawCircle(inkBlack, 5f * density, boat + Offset(4f * density, -9f * density), alpha = .85f * show)
            drawLine(inkBlack, boat + Offset(-6f * density, -16f * density), boat + Offset(14f * density, -16f * density),
                2.6f * density, StrokeCap.Round, alpha = .85f * show)
            drawLine(inkBlack, boat + Offset(4f * density, -21f * density), boat + Offset(-6f * density, -16f * density),
                2f * density, StrokeCap.Round, alpha = .85f * show)
            drawLine(inkBlack, boat + Offset(4f * density, -21f * density), boat + Offset(14f * density, -16f * density),
                2f * density, StrokeCap.Round, alpha = .85f * show)
            drawLine(inkBlack, boat + Offset(8f * density, -10f * density), boat + Offset(-30f * density, -34f * density),
                1.1f * density, StrokeCap.Round, alpha = .7f * show)
            drawLine(inkBlack, boat + Offset(-30f * density, -34f * density), boat + Offset(-34f * density, 6f * density),
                .6f * density, alpha = .45f * show)
        }
        // Rings spread from where the line meets the water.
        val hook = boat + Offset(-34f * density, 6f * density)
        for (k in 0 until 2) {
            val phase = ((t * .5f + k * .5f) % 1f)
            drawOval(inkBlack, Offset(hook.x - phase * 26f * density, hook.y - phase * 4f * density),
                Size(phase * 52f * density + 1f, phase * 8f * density + 1f), alpha = (1f - phase) * .35f * show,
                style = Stroke(.8f * density))
        }
    }
}
