package app.wortlinge.ui.pets

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import app.wortlinge.core.pets.EyeStyle
import app.wortlinge.core.pets.Mouth
import app.wortlinge.core.pets.Pattern
import app.wortlinge.core.pets.PetGenes
import app.wortlinge.core.pets.SeededRandom
import app.wortlinge.core.pets.Species
import app.wortlinge.core.pets.Stage
import app.wortlinge.core.pets.Tuft
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

data class PetLook(
    val genes: PetGenes,
    val stage: Stage,
    val shiny: Boolean = false,
    val asleep: Boolean = false,
    val silhouette: Boolean = false,
)

private val Ink = Color(0xFF26232E)
private val SilhouetteColour = Color(0xFF4A4658)

internal fun hsl(h: Float, s: Float, l: Float, a: Float = 1f): Color =
    Color.hsl(((h % 360f) + 360f) % 360f, s.coerceIn(0f, 1f), l.coerceIn(0f, 1f), a)

/** Colours for one pet, already adjusted for sleep and silhouettes. */
private class Tones(look: PetLook) {
    private val g = look.genes
    private val sat = if (look.asleep) g.saturation * 0.35f else g.saturation
    private val sil = look.silhouette
    val body = if (sil) SilhouetteColour else hsl(g.hue, sat, g.lightness)
    val dark = if (sil) SilhouetteColour else hsl(g.hue, sat, g.lightness - 0.27f)
    val accent = if (sil) SilhouetteColour else hsl(g.hue + g.accentHueShift, sat, g.lightness - 0.13f)
    val light = if (sil) SilhouetteColour else hsl(g.hue, sat * 0.75f, min(0.93f, g.lightness + 0.27f))
    val horn = if (sil) SilhouetteColour else hsl(g.hue, 0.25f, 0.9f)
    val leaf = if (sil) SilhouetteColour else hsl(g.hue + 18f, 0.6f, if (look.asleep) 0.5f else 0.34f)
    val sparkle = if (sil) SilhouetteColour else hsl(g.hue + 10f, 0.9f, 0.82f)
}

/** Body box, computed once per frame so every part lines up. */
private class Frame(val cx: Float, val top: Float, val bottom: Float, val bw: Float, val bh: Float, val s: Float) {
    val left get() = cx - bw / 2
    val right get() = cx + bw / 2
    val cy get() = (top + bottom) / 2
}

/**
 * Draws a pet centred in the current [DrawScope]. [lift] raises it off the ground (for hops),
 * [blink] closes its eyes for one frame.
 */
fun DrawScope.drawPet(look: PetLook, lift: Float = 0f, blink: Boolean = false) {
    val g = look.genes
    val s = size.minDimension
    val stageScale = when (look.stage) { Stage.BABY -> 0.62f; Stage.TEEN -> 0.8f; Stage.ADULT -> 0.95f }
    var bw = s * 0.62f * g.widthFactor * stageScale
    var bh = s * 0.56f * g.heightFactor * stageScale
    when (g.species) {
        Species.DAS -> { bw *= 1.18f; bh *= 0.8f }
        Species.DIE -> { bw *= 0.86f; bh *= 1.1f }
        Species.WISP -> { bw *= 0.92f; bh *= 0.86f }
        Species.DER -> Unit
    }
    val groundY = size.height * 0.92f
    val floatGap = if (g.species == Species.WISP) bh * 0.34f else 0f
    val bottom = groundY - floatGap - lift
    val f = Frame(size.width / 2f, bottom - bh, bottom, bw, bh, s)
    val t = Tones(look)
    val outline = Stroke(width = s * 0.016f, join = StrokeJoin.Round, cap = StrokeCap.Round)

    // Shadow on the ground, smaller while hopping or floating.
    val shadowW = bw * 0.85f * (1f - (lift + floatGap) / s * 1.2f).coerceIn(0.4f, 1f)
    drawOval(Color.Black.copy(alpha = 0.12f), Offset(f.cx - shadowW / 2, groundY - s * 0.025f), Size(shadowW, s * 0.05f))

    // Parts that sit behind the body.
    when (g.species) {
        Species.DER -> { drawHorns(f, g, t, outline); drawFeet(f, t) }
        Species.DIE -> drawEars(f, g, t, outline)
        else -> Unit
    }

    val body = bodyPath(f, g)
    val fill: Brush = if (look.shiny && !look.silhouette) {
        Brush.linearGradient(
            listOf(Color.White, t.light, t.body, t.accent),
            start = Offset(f.left, f.top), end = Offset(f.right, f.bottom),
        )
    } else SolidColor(t.body)
    drawPath(body, fill)

    if (!look.silhouette) {
        if (look.stage != Stage.BABY) {
            val alpha = if (look.stage == Stage.TEEN) 0.55f else 0.95f
            clipPath(body) { drawPattern(f, g, t, alpha) }
        }
        clipPath(body) {
            drawOval(
                Color.White.copy(alpha = if (look.shiny) 0.45f else 0.22f),
                Offset(f.left + bw * 0.12f, f.top + bh * 0.1f), Size(bw * 0.35f, bh * 0.22f),
            )
        }
    }
    drawPath(body, t.dark, style = outline)

    // Parts in front of the body.
    when (g.species) {
        Species.DAS -> drawLeaf(f, g, t)
        Species.WISP -> drawWispSparkles(f, t)
        else -> Unit
    }
    drawTuft(f, g, t, outline)

    if (!look.silhouette) {
        drawFace(f, look, t, closed = look.asleep || blink)
        if (look.asleep) drawZzz(f)
        if (look.shiny) drawShinySparkles(f)
    }
}

private fun bodyPath(f: Frame, g: PetGenes): Path = Path().apply {
    val cx = f.cx; val top = f.top; val bottom = f.bottom; val bw = f.bw; val bh = f.bh
    when (g.species) {
        Species.DER -> addRoundRect(RoundRect(f.left, top, f.right, bottom, CornerRadius(bw * 0.3f)))
        Species.DIE -> {
            moveTo(cx, top)
            cubicTo(cx + bw * 0.14f, top, cx + bw * 0.5f, top + bh * 0.42f, cx + bw * 0.5f, top + bh * 0.68f)
            cubicTo(cx + bw * 0.5f, bottom - bh * 0.02f, cx + bw * 0.25f, bottom, cx, bottom)
            cubicTo(cx - bw * 0.25f, bottom, cx - bw * 0.5f, bottom - bh * 0.02f, cx - bw * 0.5f, top + bh * 0.68f)
            cubicTo(cx - bw * 0.5f, top + bh * 0.42f, cx - bw * 0.14f, top, cx, top)
            close()
        }
        Species.DAS -> {
            val base = bottom - bh * 0.08f
            moveTo(f.left, base)
            cubicTo(f.left, top + bh * 0.12f, cx - bw * 0.3f, top, cx, top)
            cubicTo(cx + bw * 0.3f, top, f.right, top + bh * 0.12f, f.right, base)
            val bumps = 4
            val step = bw / bumps
            for (i in 0 until bumps) {
                val x0 = f.right - i * step
                quadraticTo(x0 - step / 2, bottom + bh * 0.06f, x0 - step, base)
            }
            close()
        }
        Species.WISP -> {
            val fs = g.featureSize
            moveTo(cx, top)
            cubicTo(cx + bw * 0.56f, top, cx + bw * 0.56f, bottom - bh * 0.25f, cx + bw * 0.2f, bottom - bh * 0.08f)
            cubicTo(cx + bw * 0.3f, bottom, cx + bw * 0.45f * fs, bottom + bh * 0.04f, cx + bw * 0.56f * fs, bottom - bh * 0.06f)
            cubicTo(cx + bw * 0.36f * fs, bottom + bh * 0.24f, cx - bw * 0.3f, bottom + bh * 0.1f, cx - bw * 0.45f, bottom - bh * 0.3f)
            cubicTo(cx - bw * 0.6f, top + bh * 0.25f, cx - bw * 0.3f, top, cx, top)
            close()
        }
    }
}

private fun DrawScope.drawHorns(f: Frame, g: PetGenes, t: Tones, outline: Stroke) {
    val len = f.bh * 0.3f * g.featureSize
    for (side in listOf(-1f, 1f)) {
        val bx = f.cx + side * f.bw * 0.28f
        val horn = Path().apply {
            moveTo(bx - f.bw * 0.08f, f.top + f.bh * 0.12f)
            quadraticTo(bx + side * f.bw * 0.02f, f.top - len * 0.55f, bx + side * f.bw * 0.13f, f.top - len)
            quadraticTo(bx + side * f.bw * 0.06f, f.top - len * 0.25f, bx + f.bw * 0.08f, f.top + f.bh * 0.12f)
            close()
        }
        drawPath(horn, t.horn)
        drawPath(horn, t.dark, style = outline)
    }
}

private fun DrawScope.drawFeet(f: Frame, t: Tones) {
    for (side in listOf(-1f, 1f)) {
        val w = f.bw * 0.22f
        val h = f.bh * 0.14f
        drawRoundRect(t.dark, Offset(f.cx + side * f.bw * 0.22f - w / 2, f.bottom - h * 0.6f), Size(w, h), CornerRadius(h / 2))
    }
}

private fun DrawScope.drawEars(f: Frame, g: PetGenes, t: Tones, outline: Stroke) {
    val len = f.bh * 0.55f * g.featureSize
    val w = f.bw * 0.22f
    for (side in listOf(-1f, 1f)) {
        val pivot = Offset(f.cx + side * f.bw * 0.13f, f.top + f.bh * 0.25f)
        rotate(degrees = side * 20f, pivot = pivot) {
            val tl = Offset(pivot.x - w / 2, pivot.y - len)
            drawOval(t.body, tl, Size(w, len))
            drawOval(t.light, Offset(pivot.x - w * 0.25f, pivot.y - len * 0.88f), Size(w * 0.5f, len * 0.7f))
            drawOval(t.dark, tl, Size(w, len), style = outline)
        }
    }
}

private fun DrawScope.drawLeaf(f: Frame, g: PetGenes, t: Tones) {
    val base = Offset(f.cx, f.top + f.bh * 0.03f)
    val tip = Offset(f.cx + f.bw * 0.03f, f.top - f.bh * 0.16f)
    drawLine(t.leaf, base, tip, strokeWidth = f.s * 0.018f, cap = StrokeCap.Round)
    val l = f.bw * 0.28f * g.featureSize
    for ((dir, scale) in listOf(1f to 1f, -1f to 0.7f)) {
        val ll = l * scale
        val leaf = Path().apply {
            moveTo(tip.x, tip.y)
            quadraticTo(tip.x + dir * ll * 0.5f, tip.y - ll * 0.6f, tip.x + dir * ll, tip.y - ll * 0.15f)
            quadraticTo(tip.x + dir * ll * 0.5f, tip.y + ll * 0.25f, tip.x, tip.y)
            close()
        }
        drawPath(leaf, t.leaf)
    }
}

internal fun DrawScope.fourPointStar(c: Offset, r: Float, colour: Color) {
    val p = Path().apply {
        moveTo(c.x, c.y - r)
        quadraticTo(c.x, c.y, c.x + r, c.y)
        quadraticTo(c.x, c.y, c.x, c.y + r)
        quadraticTo(c.x, c.y, c.x - r, c.y)
        quadraticTo(c.x, c.y, c.x, c.y - r)
        close()
    }
    drawPath(p, colour)
}

private fun DrawScope.drawWispSparkles(f: Frame, t: Tones) {
    for (i in 0 until 3) {
        val a = (i * 2.1 + 0.6) * PI
        val c = Offset(f.cx + cos(a).toFloat() * f.bw * 0.72f, f.cy + sin(a).toFloat() * f.bh * 0.6f)
        fourPointStar(c, f.s * (0.03f + 0.012f * i), t.sparkle)
    }
}

private fun DrawScope.drawShinySparkles(f: Frame) {
    val spots = listOf(-0.62f to -0.35f, 0.6f to -0.5f, 0.68f to 0.25f, -0.55f to 0.4f)
    spots.forEachIndexed { i, (dx, dy) ->
        val c = Offset(f.cx + dx * f.bw, f.cy + dy * f.bh)
        val r = f.s * (0.04f - i * 0.005f)
        fourPointStar(c, r, Color(0xFFFFE27A))
        fourPointStar(c, r * 0.45f, Color.White)
    }
}

private fun DrawScope.drawPattern(f: Frame, g: PetGenes, t: Tones, alpha: Float) {
    val r = SeededRandom(g.patternSeed)
    when (g.pattern) {
        Pattern.NONE -> Unit
        Pattern.SPOTS -> repeat(7) {
            val c = Offset(r.range(f.left, f.right), r.range(f.top, f.bottom))
            drawCircle(t.accent.copy(alpha = alpha), radius = f.bw * r.range(0.04f, 0.09f), center = c)
        }
        Pattern.STRIPES -> for (i in 0..2) {
            val y = f.top + f.bh * (0.2f + i * 0.17f)
            for (side in listOf(-1f, 1f)) {
                drawLine(
                    t.accent.copy(alpha = alpha),
                    Offset(f.cx + side * f.bw * 0.6f, y),
                    Offset(f.cx + side * f.bw * 0.3f, y + f.bh * 0.05f),
                    strokeWidth = f.bh * 0.06f, cap = StrokeCap.Round,
                )
            }
        }
        Pattern.BELLY -> drawOval(
            t.light.copy(alpha = alpha),
            Offset(f.cx - f.bw * 0.28f, f.bottom - f.bh * 0.48f),
            Size(f.bw * 0.56f, f.bh * 0.44f),
        )
    }
}

private fun DrawScope.drawTuft(f: Frame, g: PetGenes, t: Tones, outline: Stroke) {
    val x = if (g.species == Species.DAS) f.cx - f.bw * 0.22f else f.cx
    val y = when (g.species) {
        Species.DAS -> f.top + f.bh * 0.1f
        Species.DIE -> f.top + f.bh * 0.02f
        else -> f.top + f.s * 0.004f
    }
    val h = f.bh * 0.18f
    when (g.tuft) {
        Tuft.NONE -> Unit
        Tuft.CURL -> {
            val p = Path().apply {
                moveTo(x, y + h * 0.1f)
                cubicTo(x - h * 0.4f, y - h * 0.6f, x + h * 0.55f, y - h * 0.85f, x + h * 0.3f, y - h * 0.35f)
            }
            drawPath(p, t.dark, style = Stroke(width = f.s * 0.02f, cap = StrokeCap.Round))
        }
        Tuft.SPIKES -> for (i in -1..1) {
            val sx = x + i * h * 0.45f
            val sh = if (i == 0) h else h * 0.7f
            val p = Path().apply {
                moveTo(sx - h * 0.2f, y + h * 0.15f); lineTo(sx, y - sh); lineTo(sx + h * 0.2f, y + h * 0.15f); close()
            }
            drawPath(p, t.body)
            drawPath(p, t.dark, style = outline)
        }
        Tuft.BOW -> {
            val bowColour = Color(0xFFFFF4E0)
            for (side in listOf(-1f, 1f)) {
                val p = Path().apply {
                    moveTo(x, y); lineTo(x + side * h * 0.7f, y - h * 0.4f); lineTo(x + side * h * 0.7f, y + h * 0.4f); close()
                }
                drawPath(p, bowColour)
                drawPath(p, t.dark, style = outline)
            }
            drawCircle(bowColour, h * 0.16f, Offset(x, y))
            drawCircle(t.dark, h * 0.16f, Offset(x, y), style = outline)
        }
    }
}

private fun DrawScope.drawFace(f: Frame, look: PetLook, t: Tones, closed: Boolean) {
    val g = look.genes
    val eyeScale = when (look.stage) { Stage.BABY -> 1.35f; Stage.TEEN -> 1.12f; Stage.ADULT -> 1f }
    val eyeY = when (g.species) {
        Species.DIE -> f.top + f.bh * 0.55f
        Species.DAS -> f.top + f.bh * 0.42f
        else -> f.top + f.bh * 0.4f
    }
    val r = min(f.bw * 0.075f * eyeScale, f.s * 0.06f)
    val dx = f.bw * 0.18f * g.eyeSpacing + r * 0.3f
    val eyes = listOf(Offset(f.cx - dx, eyeY), Offset(f.cx + dx, eyeY))

    if (g.blush) eyes.forEachIndexed { i, e ->
        val side = if (i == 0) -1f else 1f
        drawOval(
            Color(0xFFFF7A9A).copy(alpha = 0.4f),
            Offset(e.x + side * r * 0.5f - r * 0.6f, e.y + r * 1.15f), Size(r * 1.2f, r * 0.6f),
        )
    }

    if (closed) {
        eyes.forEach { e ->
            val p = Path().apply { moveTo(e.x - r, e.y); quadraticTo(e.x, e.y + r * 0.9f, e.x + r, e.y) }
            drawPath(p, Ink, style = Stroke(width = r * 0.32f, cap = StrokeCap.Round))
        }
    } else eyes.forEach { e ->
        when (g.eyeStyle) {
            EyeStyle.ROUND -> {
                drawCircle(Ink, r, e)
                drawCircle(Color.White, r * 0.36f, e + Offset(-r * 0.3f, -r * 0.35f))
            }
            EyeStyle.SPARKLY -> {
                drawCircle(Ink, r * 1.05f, e)
                drawCircle(Color.White, r * 0.42f, e + Offset(-r * 0.3f, -r * 0.35f))
                drawCircle(Color.White, r * 0.18f, e + Offset(r * 0.38f, r * 0.32f))
            }
            EyeStyle.SLEEPY_LIDS -> {
                drawCircle(Ink, r, e)
                drawCircle(Color.White, r * 0.3f, e + Offset(-r * 0.25f, r * 0.2f))
                drawArc(t.body, 180f, 180f, useCenter = true, topLeft = e - Offset(r * 1.1f, r * 1.1f), size = Size(r * 2.2f, r * 2.2f))
                drawLine(Ink, e - Offset(r * 1.05f, 0f), e + Offset(r * 1.05f, 0f), strokeWidth = r * 0.25f, cap = StrokeCap.Round)
            }
            EyeStyle.WIDE -> {
                val ew = r * 1.25f
                val eh = r * 1.4f
                drawOval(Color.White, e - Offset(ew, eh), Size(ew * 2, eh * 2))
                drawOval(Ink, e - Offset(ew, eh), Size(ew * 2, eh * 2), style = Stroke(width = r * 0.18f))
                drawCircle(Ink, r * 0.7f, e + Offset(0f, r * 0.2f))
                drawCircle(Color.White, r * 0.25f, e + Offset(-r * 0.2f, -r * 0.05f))
            }
        }
    }

    val my = eyeY + r * 2.0f
    val mw = f.bw * 0.13f
    val mouthStroke = Stroke(width = r * 0.3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    if (look.asleep) {
        drawOval(Ink, Offset(f.cx - mw * 0.12f, my), Size(mw * 0.24f, mw * 0.28f))
        return
    }
    when (g.mouth) {
        Mouth.SMILE -> drawPath(Path().apply {
            moveTo(f.cx - mw / 2, my); quadraticTo(f.cx, my + mw * 0.55f, f.cx + mw / 2, my)
        }, Ink, style = mouthStroke)
        Mouth.OPEN -> {
            drawArc(Ink, 0f, 180f, useCenter = true, topLeft = Offset(f.cx - mw * 0.38f, my - mw * 0.3f), size = Size(mw * 0.76f, mw * 0.76f))
            drawCircle(Color(0xFFFF8FA3), mw * 0.15f, Offset(f.cx, my + mw * 0.24f))
        }
        Mouth.CAT -> drawPath(Path().apply {
            moveTo(f.cx - mw / 2, my)
            quadraticTo(f.cx - mw / 4, my + mw * 0.4f, f.cx, my)
            quadraticTo(f.cx + mw / 4, my + mw * 0.4f, f.cx + mw / 2, my)
        }, Ink, style = mouthStroke)
        Mouth.TOOTH -> {
            drawPath(Path().apply {
                moveTo(f.cx - mw / 2, my); quadraticTo(f.cx, my + mw * 0.5f, f.cx + mw / 2, my)
            }, Ink, style = mouthStroke)
            val tw = mw * 0.2f
            val tooth = Offset(f.cx + mw * 0.12f, my + mw * 0.2f)
            drawRect(Color.White, tooth, Size(tw, tw * 1.1f))
            drawRect(Ink, tooth, Size(tw, tw * 1.1f), style = Stroke(width = r * 0.12f))
        }
    }
}

private fun DrawScope.drawZzz(f: Frame) {
    val base = Offset(f.right - f.bw * 0.05f, f.top - f.bh * 0.05f)
    for (i in 0..2) {
        val z = f.s * (0.035f + i * 0.015f)
        val o = base + Offset(i * z * 0.9f, -i * z * 1.4f)
        val p = Path().apply {
            moveTo(o.x, o.y); lineTo(o.x + z, o.y); lineTo(o.x, o.y + z); lineTo(o.x + z, o.y + z)
        }
        drawPath(p, Color(0xFF8C86A3), style = Stroke(width = f.s * 0.011f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
