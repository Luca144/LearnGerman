package app.wortlinge.ui.pets

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import app.wortlinge.core.pets.EyeStyle
import app.wortlinge.core.pets.Mouth
import app.wortlinge.core.pets.Pattern
import app.wortlinge.core.pets.PetGenes
import app.wortlinge.core.pets.SeededRandom
import app.wortlinge.core.pets.Species
import app.wortlinge.core.pets.Stage
import app.wortlinge.core.pets.Tuft
import kotlin.math.min
import kotlin.math.sqrt

/*
 * Pets are drawn as hand-made creatures in side view, all facing right:
 *   der  -> armoured four-legged drake (steel blue)
 *   die  -> two-legged winged wyvern (crimson)
 *   das  -> low shell-backed beast with moss fronds (moss green)
 *   none -> floating ember spirit (gold)
 * Every shape is defined in a 0..1 box (u to the right, v downwards, v = 1 is the ground)
 * and scaled by stage and genes.
 */

data class PetLook(
    val genes: PetGenes,
    val stage: Stage,
    val shiny: Boolean = false,
    val asleep: Boolean = false,
    val silhouette: Boolean = false,
)

private val SilhouetteColour = Color(0xFF3E3A48)

internal fun hsl(h: Float, s: Float, l: Float, a: Float = 1f): Color =
    Color.hsl(((h % 360f) + 360f) % 360f, s.coerceIn(0f, 1f), l.coerceIn(0f, 1f), a)

/** Colours for one pet, already adjusted for sleep and silhouettes. */
private class Tones(look: PetLook) {
    private val g = look.genes
    private val sat = if (look.asleep) g.saturation * 0.4f else g.saturation
    private val l = g.lightness
    private val sil = look.silhouette
    private fun c(colour: Color) = if (sil) SilhouetteColour else colour

    val body = c(hsl(g.hue, sat, l))
    val top = c(hsl(g.hue, sat * 0.9f, l + 0.1f))
    val bottom = c(hsl(g.hue, sat, l - 0.12f))
    val deep = c(hsl(g.hue, sat, l - 0.14f))
    val dark = c(hsl(g.hue, sat * 0.8f, l - 0.24f))
    val outline = c(hsl(g.hue, 0.35f, 0.11f))
    val accent = c(hsl(g.hue + g.accentHueShift, sat, l - 0.17f))
    val belly = c(hsl(g.hue + 25f, sat * 0.35f, l + 0.24f))
    val skin = c(hsl(g.hue + 12f, sat * 0.55f, l + 0.06f))
    val membrane = c(hsl(g.hue + 6f, sat * 0.85f, l - 0.06f))
    val bone = c(hsl(40f, 0.28f, if (look.asleep) 0.72f else 0.84f))
    val boneShade = c(hsl(34f, 0.25f, 0.6f))
    val leaf = c(hsl(g.hue + 22f, 0.5f, if (look.asleep) 0.42f else 0.28f))
    val ember = c(hsl(g.hue + 8f, 1f, if (look.asleep) 0.68f else 0.74f))
    val iris = hsl(46f, if (look.asleep) 0.4f else 0.95f, 0.56f)
    val glow = hsl(54f, 1f, 0.8f)
}

/** Maps the 0..1 creature box onto the canvas. */
private class Box(val left: Float, val top: Float, val w: Float, val h: Float) {
    fun p(u: Float, v: Float) = Offset(left + u * w, top + v * h)
    fun path(block: PathBuilder.() -> Unit): Path = PathBuilder(this).apply(block).path
}

private class PathBuilder(val b: Box) {
    val path = Path()
    fun m(u: Float, v: Float) { val o = b.p(u, v); path.moveTo(o.x, o.y) }
    fun l(u: Float, v: Float) { val o = b.p(u, v); path.lineTo(o.x, o.y) }
    fun q(cu: Float, cv: Float, u: Float, v: Float) {
        val c = b.p(cu, cv); val o = b.p(u, v); path.quadraticTo(c.x, c.y, o.x, o.y)
    }
    fun c(u1: Float, v1: Float, u2: Float, v2: Float, u: Float, v: Float) {
        val a = b.p(u1, v1); val c = b.p(u2, v2); val o = b.p(u, v)
        path.cubicTo(a.x, a.y, c.x, c.y, o.x, o.y)
    }
    fun z() = path.close()
}

/** Per-frame drawing context. */
private class Ctx(
    val look: PetLook,
    val t: Tones,
    val b: Box,
    val s: Float,
    /** 0 for babies, 0.5 for teens, 1 for adults. Drives horns, plates, wings. */
    val growth: Float,
    val headScale: Float,
    val closedEyes: Boolean,
) {
    val g get() = look.genes
    val line = Stroke(width = s * 0.013f, join = StrokeJoin.Round, cap = StrokeCap.Round)
    val thin = Stroke(width = s * 0.008f, join = StrokeJoin.Round, cap = StrokeCap.Round)
    val showDetail get() = !look.silhouette
}

/**
 * Draws a pet centred in the current [DrawScope]. [lift] raises it off the ground (for hops),
 * [blink] closes its eyes for one frame.
 */
fun DrawScope.drawPet(look: PetLook, lift: Float = 0f, blink: Boolean = false) {
    val g = look.genes
    val s = size.minDimension
    val (stageScale, growth, headScale) = when (look.stage) {
        Stage.BABY -> Triple(0.6f, 0f, 1.4f)
        Stage.TEEN -> Triple(0.8f, 0.5f, 1.15f)
        Stage.ADULT -> Triple(1f, 1f, 1f)
    }
    val base = min(size.width, size.height * 1.3f)
    val bw = min(base * 0.86f * stageScale * g.widthFactor, size.width * 0.96f)
    val bh = base * 0.68f * stageScale * g.heightFactor
    val groundY = size.height * 0.94f
    val floatGap = if (g.species == Species.WISP) bh * 0.12f else 0f
    val box = Box(size.width / 2f - bw / 2f, groundY - bh - floatGap - lift, bw, bh)
    val ctx = Ctx(look, Tones(look), box, s, growth, headScale, closedEyes = look.asleep || blink)

    // Ground shadow, smaller while hopping or floating.
    val shadowW = bw * 0.8f * (1f - (lift + floatGap) / s * 1.5f).coerceIn(0.35f, 1f)
    drawOval(Color.Black.copy(alpha = 0.16f), Offset(size.width / 2f - shadowW / 2, groundY - s * 0.022f), Size(shadowW, s * 0.044f))

    when (g.species) {
        Species.DER -> drawDrake(ctx)
        Species.DIE -> drawWyvern(ctx)
        Species.DAS -> drawShellback(ctx)
        Species.WISP -> drawEmberSpirit(ctx)
    }

    if (ctx.showDetail) {
        if (look.asleep) drawZzz(box, s)
        if (look.shiny) drawShinySparkles(box, s)
    }
}

// ---------------------------------------------------------------- shared helpers

/** Fills a body part with top-lit shading, then outlines it. */
private fun DrawScope.part(path: Path, c: Ctx, fill: Color? = null, outline: Boolean = true) {
    val b = c.b
    when {
        fill != null -> drawPath(path, fill)
        c.look.shiny && c.showDetail -> drawPath(
            path,
            Brush.linearGradient(
                listOf(Color.White, c.t.top, c.t.body, c.t.accent),
                start = b.p(0f, 0f), end = b.p(1f, 1f),
            ),
        )
        else -> drawPath(path, Brush.verticalGradient(listOf(c.t.top, c.t.body, c.t.bottom), startY = b.top, endY = b.top + b.h))
    }
    if (outline) drawPath(path, c.t.outline, style = c.line)
}

private fun cubicPoint(p0: Offset, p1: Offset, p2: Offset, p3: Offset, t: Float): Offset {
    val u = 1 - t
    return p0 * (u * u * u) + p1 * (3 * u * u * t) + p2 * (3 * u * t * t) + p3 * (t * t * t)
}

private fun cubicTangent(p0: Offset, p1: Offset, p2: Offset, p3: Offset, t: Float): Offset {
    val u = 1 - t
    return (p1 - p0) * (3 * u * u) + (p2 - p1) * (6 * u * t) + (p3 - p2) * (3 * t * t)
}

private fun Offset.normalised(): Offset {
    val len = sqrt(x * x + y * y)
    return if (len == 0f) this else Offset(x / len, y / len)
}

/** Spikes or plates standing up along a cubic curve (box coordinates). */
private fun DrawScope.ridge(
    c: Ctx, p0: Offset, p1: Offset, p2: Offset, p3: Offset,
    count: Int, from: Float, to: Float, height: Float, halfBase: Float, colour: Color,
) {
    if (count <= 0) return
    val pts = listOf(p0, p1, p2, p3).map { c.b.p(it.x, it.y) }
    for (i in 0 until count) {
        val t = if (count == 1) (from + to) / 2 else from + (to - from) * i / (count - 1)
        val at = cubicPoint(pts[0], pts[1], pts[2], pts[3], t)
        val tan = cubicTangent(pts[0], pts[1], pts[2], pts[3], t).normalised()
        var normal = Offset(tan.y, -tan.x)
        if (normal.y > 0) normal = -normal
        // Middle plates are tallest.
        val bump = 1f - 0.45f * kotlin.math.abs(2f * i / (count - 1).coerceAtLeast(1) - 1f)
        val tip = at + normal * (height * bump) - tan * (halfBase * 0.5f)
        val p = Path().apply {
            moveTo((at - tan * halfBase).x, (at - tan * halfBase).y)
            lineTo(tip.x, tip.y)
            lineTo((at + tan * halfBase).x, (at + tan * halfBase).y)
            close()
        }
        drawPath(p, colour)
        drawPath(p, c.t.outline, style = c.thin)
    }
}

private fun DrawScope.triangle(c: Ctx, a: Offset, b: Offset, d: Offset, colour: Color) {
    val p = Path().apply { moveTo(a.x, a.y); lineTo(b.x, b.y); lineTo(d.x, d.y); close() }
    drawPath(p, colour)
    drawPath(p, c.t.outline, style = c.thin)
}

/** Claws pointing forward from a foot whose front-bottom corner is at (u, v). */
private fun DrawScope.claws(c: Ctx, u: Float, v: Float, count: Int = 3) {
    val b = c.b
    for (i in 0 until count) {
        val x = u - i * 0.022f
        triangle(c, b.p(x - 0.012f, v - 0.03f), b.p(x + 0.022f, v), b.p(x - 0.016f, v), c.t.bone)
    }
}

/** Markings clipped to a body part. Babies have none; teens faint, adults strong. */
private fun DrawScope.markings(c: Ctx, clip: Path) {
    if (!c.showDetail || c.look.stage == Stage.BABY) return
    val alpha = if (c.look.stage == Stage.TEEN) 0.45f else 0.8f
    val b = c.b
    val r = SeededRandom(c.g.patternSeed)
    clipPath(clip) {
        when (c.g.pattern) {
            Pattern.NONE -> Unit
            Pattern.SPOTS -> repeat(14) {
                drawCircle(c.t.accent.copy(alpha = alpha), b.w * r.range(0.012f, 0.03f), b.p(r.range(0f, 1f), r.range(0.15f, 0.75f)))
            }
            Pattern.BANDS -> for (i in 0 until 7) {
                val u = 0.12f + i * 0.11f + r.range(-0.02f, 0.02f)
                drawLine(c.t.accent.copy(alpha = alpha), b.p(u, 0.1f), b.p(u - 0.07f, 0.62f), strokeWidth = b.w * 0.028f, cap = StrokeCap.Round)
            }
            Pattern.SCALES -> {
                val scale = b.w * 0.045f
                var row = 0
                var v = 0.22f
                while (v < 0.8f) {
                    var u = if (row % 2 == 0) 0.05f else 0.08f
                    while (u < 1f) {
                        drawArc(
                            c.t.dark.copy(alpha = alpha * 0.6f), 0f, 180f, useCenter = false,
                            topLeft = b.p(u, v) - Offset(scale / 2, scale / 2), size = Size(scale, scale), style = c.thin,
                        )
                        u += 0.06f
                    }
                    v += 0.05f; row++
                }
            }
        }
    }
}

/** A fierce eye with an angled brow. Closed eyes are a short line. */
private fun DrawScope.beastEye(c: Ctx, centre: Offset, r: Float, brow: Boolean = true) {
    if (!c.showDetail) return
    if (c.closedEyes) {
        val p = Path().apply {
            moveTo(centre.x - r * 1.1f, centre.y - r * 0.1f)
            quadraticTo(centre.x, centre.y + r * 0.6f, centre.x + r * 1.1f, centre.y + r * 0.1f)
        }
        drawPath(p, c.t.outline, style = Stroke(r * 0.4f, cap = StrokeCap.Round))
        return
    }
    when (c.g.eyeStyle) {
        EyeStyle.GLOWING -> {
            drawCircle(c.t.glow.copy(alpha = 0.35f), r * 1.9f, centre)
            drawCircle(c.t.glow, r * 1.05f, centre)
            drawCircle(Color.White, r * 0.5f, centre)
        }
        else -> {
            drawCircle(c.t.iris, r, centre)
            when (c.g.eyeStyle) {
                EyeStyle.SLIT -> drawOval(Color(0xFF120E0A), centre - Offset(r * 0.18f, r * 0.85f), Size(r * 0.36f, r * 1.7f))
                else -> drawCircle(Color(0xFF120E0A), r * 0.5f, centre)
            }
            drawCircle(Color.White.copy(alpha = 0.85f), r * 0.22f, centre + Offset(-r * 0.35f, -r * 0.35f))
            if (c.g.eyeStyle == EyeStyle.NARROW) {
                drawArc(c.t.dark, 180f, 180f, useCenter = true, topLeft = centre - Offset(r * 1.15f, r * 1.15f), size = Size(r * 2.3f, r * 2.3f))
            }
            drawCircle(c.t.outline, r, centre, style = Stroke(r * 0.2f))
        }
    }
    if (brow) {
        // Slants down towards the snout: gives the frown.
        drawLine(c.t.outline, centre + Offset(-r * 1.5f, -r * 1.55f), centre + Offset(r * 1.5f, -r * 0.75f), strokeWidth = r * 0.55f, cap = StrokeCap.Round)
    }
    if (c.g.scar && c.look.stage == Stage.ADULT) {
        drawLine(c.t.belly, centre + Offset(-r * 0.9f, -r * 2.4f), centre + Offset(r * 0.5f, r * 2.0f), strokeWidth = r * 0.3f, cap = StrokeCap.Round)
    }
}

/** Jaw line plus teeth or tusks, between the snout tip (tipU, tipV) and the corner (cornerU, cornerV). */
private fun DrawScope.jaw(c: Ctx, tipU: Float, tipV: Float, cornerU: Float, cornerV: Float) {
    if (!c.showDetail) return
    val b = c.b
    val tip = b.p(tipU, tipV)
    val corner = b.p(cornerU, cornerV)
    val mouth = c.g.mouth
    if (mouth == Mouth.SNARL && !c.look.asleep) {
        val open = b.h * 0.035f
        val p = Path().apply {
            moveTo(tip.x, tip.y); lineTo(corner.x, corner.y); lineTo(tip.x - (tip.x - corner.x) * 0.15f, tip.y + open); close()
        }
        drawPath(p, Color(0xFF3A1414))
    }
    drawLine(c.t.outline, tip, corner, strokeWidth = c.s * 0.01f, cap = StrokeCap.Round)
    val tooth = b.h * 0.035f
    when (mouth) {
        Mouth.CLOSED -> Unit
        Mouth.FANGS, Mouth.SNARL -> for (f in listOf(0.2f, 0.45f)) {
            val at = tip + (corner - tip) * f
            triangle(c, at - Offset(tooth * 0.35f, 0f), at + Offset(0f, tooth), at + Offset(tooth * 0.35f, 0f), c.t.bone)
        }
        Mouth.TUSKS -> {
            val at = tip + (corner - tip) * 0.35f
            val len = tooth * (1.6f + c.growth)
            val p = Path().apply {
                moveTo(at.x - tooth * 0.3f, at.y)
                quadraticTo(at.x + len * 0.3f, at.y + len * 0.2f, at.x + len * 0.55f, at.y - len * 0.75f)
                quadraticTo(at.x + len * 0.15f, at.y - len * 0.1f, at.x + tooth * 0.3f, at.y)
                close()
            }
            drawPath(p, c.t.bone)
            drawPath(p, c.t.outline, style = c.thin)
        }
    }
}

// ---------------------------------------------------------------- der: armoured drake

private fun DrawScope.drakeLeg(c: Ctx, u: Float, colour: Color) {
    val leg = c.b.path {
        // Rounded, muscular thigh at the top so it blends into the body instead of a flat seam.
        m(u - 0.075f, 0.68f); c(u - 0.085f, 0.52f, u + 0.075f, 0.5f, u + 0.07f, 0.66f)
        l(u + 0.045f, 0.93f)
        q(u + 0.075f, 0.94f, u + 0.08f, 1f); l(u - 0.06f, 1f); l(u - 0.05f, 0.92f); z()
    }
    part(leg, c, fill = colour)
    if (c.showDetail) claws(c, u + 0.085f, 1f)
}

private fun DrawScope.drawDrake(c: Ctx) {
    val b = c.b
    val t = c.t
    val g = c.g

    drakeLeg(c, 0.27f, t.deep)
    drakeLeg(c, 0.64f, t.deep)

    val tail = b.path {
        m(0.24f, 0.46f); c(0.1f, 0.43f, 0.03f, 0.56f, 0.0f, 0.74f)
        c(0.07f, 0.66f, 0.14f, 0.66f, 0.24f, 0.68f); z()
    }
    part(tail, c)
    if (c.growth >= 1f) {
        // tail club with spikes
        drawCircle(t.accent, b.w * 0.04f, b.p(0.01f, 0.72f))
        drawCircle(t.outline, b.w * 0.04f, b.p(0.01f, 0.72f), style = c.thin)
        triangle(c, b.p(-0.01f, 0.69f), b.p(-0.03f, 0.62f), b.p(0.02f, 0.68f), t.bone)
    }

    val torso = b.path {
        m(0.18f, 0.66f); c(0.14f, 0.38f, 0.42f, 0.27f, 0.62f, 0.35f)
        c(0.74f, 0.4f, 0.8f, 0.58f, 0.74f, 0.74f); c(0.6f, 0.84f, 0.3f, 0.84f, 0.18f, 0.66f); z()
    }
    part(torso, c, outline = false)
    if (c.showDetail) clipPath(torso) { drawRect(t.belly, b.p(0f, 0.7f), Size(b.w, b.h * 0.2f)) }
    markings(c, torso)
    drawPath(torso, t.outline, style = c.line)

    // Armour plates along the spine.
    val plates = when (c.look.stage) { Stage.BABY -> 3; Stage.TEEN -> 5; Stage.ADULT -> 7 }
    ridge(
        c, Offset(0.18f, 0.66f), Offset(0.14f, 0.38f), Offset(0.42f, 0.27f), Offset(0.62f, 0.35f),
        count = plates, from = 0.3f, to = 0.92f,
        height = b.h * (0.05f + 0.07f * c.growth) * g.featureSize, halfBase = b.w * 0.03f, colour = t.accent,
    )

    drakeLeg(c, 0.34f, t.body)
    drakeLeg(c, 0.71f, t.body)

    scale(c.headScale * g.reach, c.headScale, pivot = b.p(0.7f, 0.5f)) {
        val hornLen = (0.05f + 0.07f * c.growth) * g.featureSize
        fun horn(du: Float, dv: Float, colour: Color) {
            // Thick at the base, sweeping back and curling slightly up.
            val tipU = 0.76f + du - hornLen * 1.3f
            val tipV = 0.2f + dv - hornLen * 0.7f
            val h = b.path {
                m(0.78f + du, 0.3f + dv)
                q(0.74f + du, 0.24f + dv, tipU, tipV)
                q(0.8f + du, 0.17f + dv, 0.87f + du, 0.27f + dv); z()
            }
            drawPath(h, colour)
            drawPath(h, t.outline, style = c.thin)
        }
        horn(-0.035f, 0.01f, t.boneShade)

        if (g.tuft == Tuft.FRILL) {
            val frill = b.path { m(0.72f, 0.3f); q(0.62f, 0.36f, 0.68f, 0.52f); l(0.76f, 0.44f); z() }
            part(frill, c, fill = t.accent)
        }

        val head = b.path {
            m(0.66f, 0.42f); c(0.68f, 0.28f, 0.8f, 0.22f, 0.9f, 0.27f); l(1f, 0.36f)
            c(1f, 0.42f, 0.96f, 0.46f, 0.9f, 0.47f); c(0.84f, 0.52f, 0.76f, 0.56f, 0.7f, 0.56f); z()
        }
        part(head, c)
        horn(0f, 0f, t.bone)
        if (g.tuft == Tuft.SPINES || g.tuft == Tuft.CREST) {
            for (i in 0..2) {
                val u = 0.86f + i * 0.035f
                triangle(c, b.p(u - 0.015f, 0.29f + i * 0.02f), b.p(u, 0.23f + i * 0.02f), b.p(u + 0.015f, 0.3f + i * 0.02f), t.accent)
            }
        }
        beastEye(c, b.p(0.865f, 0.33f), b.h * 0.032f)
        jaw(c, 0.99f, 0.41f, 0.85f, 0.44f)
        if (c.showDetail) drawCircle(t.outline, b.h * 0.008f, b.p(0.975f, 0.35f)) // nostril
    }
}

// ---------------------------------------------------------------- die: winged wyvern

private fun DrawScope.wyvernWing(c: Ctx, du: Float, dv: Float, colour: Color, span: Float) {
    val b = c.b
    val sx = 0.5f + du
    val sy = 0.36f + dv
    fun w(u: Float, v: Float) = Offset(sx + (u - 0.5f) * span, sy + (v - 0.36f) * span)
    val e = w(0.38f, 0.1f)
    val tip = w(0.06f, 0.04f)
    val f1 = w(0.1f, 0.34f)
    val f2 = w(0.24f, 0.42f)
    val f3 = w(0.38f, 0.46f)
    val membrane = b.path {
        m(sx, sy); l(e.x, e.y); l(tip.x, tip.y)
        q((tip.x + f1.x) / 2 + 0.03f, (tip.y + f1.y) / 2, f1.x, f1.y)
        q((f1.x + f2.x) / 2 + 0.01f, (f1.y + f2.y) / 2 - 0.03f, f2.x, f2.y)
        q((f2.x + f3.x) / 2 + 0.01f, (f2.y + f3.y) / 2 - 0.03f, f3.x, f3.y)
        z()
    }
    drawPath(membrane, colour)
    drawPath(membrane, c.t.outline, style = c.thin)
    if (c.showDetail) {
        val bones = c.t.dark
        val sw = c.s * 0.012f
        drawLine(bones, b.p(sx, sy), b.p(e.x, e.y), sw, StrokeCap.Round)
        drawLine(bones, b.p(e.x, e.y), b.p(tip.x, tip.y), sw, StrokeCap.Round)
        drawLine(bones, b.p(e.x, e.y), b.p(f1.x, f1.y), sw * 0.7f, StrokeCap.Round)
        drawLine(bones, b.p(e.x, e.y), b.p(f2.x, f2.y), sw * 0.7f, StrokeCap.Round)
        triangle(c, b.p(e.x - 0.01f, e.y + 0.005f), b.p(e.x + 0.005f, e.y - 0.05f), b.p(e.x + 0.02f, e.y + 0.01f), c.t.bone)
    }
}

private fun DrawScope.wyvernLeg(c: Ctx, u: Float, colour: Color) {
    val leg = c.b.path {
        m(u - 0.07f, 0.56f); c(u + 0.06f, 0.54f, u + 0.09f, 0.68f, u + 0.03f, 0.76f)
        l(u + 0.0f, 0.93f); l(u + 0.09f, 1f); l(u - 0.05f, 1f); l(u - 0.045f, 0.8f)
        c(u - 0.11f, 0.72f, u - 0.11f, 0.6f, u - 0.07f, 0.56f); z()
    }
    part(leg, c, fill = colour)
    if (c.showDetail) claws(c, u + 0.1f, 1f)
}

private fun DrawScope.drawWyvern(c: Ctx) {
    val b = c.b
    val t = c.t
    val g = c.g
    val span = (0.55f + 0.45f * c.growth) * g.featureSize

    wyvernWing(c, -0.06f, -0.02f, t.deep, span * 0.9f)

    val tail = b.path {
        m(0.36f, 0.56f); c(0.2f, 0.66f, 0.1f, 0.8f, 0.02f, 0.86f)
        c(0.12f, 0.86f, 0.26f, 0.76f, 0.38f, 0.68f); z()
    }
    part(tail, c)
    if (c.showDetail) {
        val blade = b.path { m(0.05f, 0.85f); l(-0.02f, 0.8f); l(-0.04f, 0.9f); l(0.03f, 0.89f); z() }
        part(blade, c, fill = t.accent)
    }

    wyvernLeg(c, 0.4f, t.deep)

    val torso = b.path {
        m(0.32f, 0.58f); c(0.3f, 0.4f, 0.44f, 0.3f, 0.56f, 0.32f)
        c(0.66f, 0.35f, 0.68f, 0.56f, 0.58f, 0.68f); c(0.5f, 0.76f, 0.36f, 0.74f, 0.32f, 0.58f); z()
    }
    val neck = b.path {
        m(0.5f, 0.38f); c(0.55f, 0.24f, 0.6f, 0.16f, 0.68f, 0.12f)
        l(0.74f, 0.19f); c(0.67f, 0.24f, 0.64f, 0.34f, 0.65f, 0.5f); z()
    }
    part(neck, c, outline = false)
    part(torso, c, outline = false)
    if (c.showDetail) clipPath(torso) { drawOval(t.belly, b.p(0.46f, 0.38f), Size(b.w * 0.24f, b.h * 0.36f)) }
    markings(c, torso)
    drawPath(neck, t.outline, style = c.line)
    drawPath(torso, t.outline, style = c.line)
    if (g.tuft == Tuft.SPINES) {
        ridge(
            c, Offset(0.32f, 0.5f), Offset(0.36f, 0.34f), Offset(0.5f, 0.24f), Offset(0.62f, 0.14f),
            count = 5, from = 0.2f, to = 0.9f, height = b.h * 0.04f, halfBase = b.w * 0.018f, colour = t.accent,
        )
    }

    wyvernLeg(c, 0.5f, t.body)
    wyvernWing(c, 0f, 0f, t.membrane, span)

    scale(c.headScale * g.reach, c.headScale, pivot = b.p(0.7f, 0.16f)) {
        val hornLen = (0.05f + 0.08f * c.growth) * g.featureSize
        for ((dv, colour) in listOf(0.025f to t.boneShade, 0f to t.bone)) {
            // Wide base on the skull, swept back like a ram's horn.
            val h = b.path {
                m(0.65f, 0.11f + dv)
                q(0.6f, 0.06f + dv, 0.66f - hornLen * 1.2f, 0.02f + dv - hornLen * 0.5f)
                q(0.68f, 0.0f + dv, 0.74f, 0.05f + dv); z()
            }
            drawPath(h, colour)
            drawPath(h, t.outline, style = c.thin)
        }
        val head = b.path {
            m(0.63f, 0.14f); c(0.65f, 0.05f, 0.75f, 0.02f, 0.82f, 0.06f)
            l(0.97f, 0.12f); c(0.93f, 0.17f, 0.84f, 0.2f, 0.74f, 0.21f); z()
        }
        part(head, c)
        if (g.tuft == Tuft.CREST || g.tuft == Tuft.FRILL) {
            // a short fin of spikes along the top of the skull
            for (i in 0..2) {
                val u = 0.73f + i * 0.035f
                triangle(c, b.p(u - 0.015f, 0.06f - i * 0.004f), b.p(u - 0.02f, -0.01f - i * 0.006f), b.p(u + 0.018f, 0.05f - i * 0.006f), t.accent)
            }
        }
        beastEye(c, b.p(0.79f, 0.095f), b.h * 0.024f)
        jaw(c, 0.96f, 0.135f, 0.82f, 0.155f)
    }
}

// ---------------------------------------------------------------- das: shell-backed beast

private fun DrawScope.stumpLeg(c: Ctx, u: Float, colour: Color) {
    val leg = c.b.path { m(u - 0.06f, 0.66f); l(u + 0.06f, 0.66f); l(u + 0.068f, 1f); l(u - 0.068f, 1f); z() }
    part(leg, c, fill = colour)
    if (c.showDetail) for (i in 0..2) {
        val x = u - 0.04f + i * 0.04f
        drawArc(c.t.bone, 180f, 180f, true, c.b.p(x - 0.016f, 0.975f), Size(c.b.w * 0.032f, c.b.h * 0.05f))
    }
}

private fun DrawScope.drawShellback(c: Ctx) {
    val b = c.b
    val t = c.t
    val g = c.g

    stumpLeg(c, 0.22f, t.deep)
    stumpLeg(c, 0.62f, t.deep)

    val tail = b.path { m(0.12f, 0.6f); q(0.0f, 0.64f, 0.02f, 0.74f); q(0.08f, 0.7f, 0.14f, 0.7f); z() }
    part(tail, c, fill = t.skin)

    // soft body under the shell
    val belly = b.path { m(0.1f, 0.66f); c(0.12f, 0.84f, 0.8f, 0.84f, 0.84f, 0.66f); z() }
    part(belly, c, fill = t.skin)

    stumpLeg(c, 0.3f, t.skin)
    stumpLeg(c, 0.7f, t.skin)

    val shell = b.path {
        m(0.06f, 0.68f); c(0.06f, 0.32f, 0.34f, 0.18f, 0.5f, 0.2f)
        c(0.7f, 0.22f, 0.86f, 0.38f, 0.86f, 0.68f)
        val n = 7
        for (i in 0 until n) {
            val u0 = 0.86f - i * 0.8f / n
            val u1 = 0.86f - (i + 1) * 0.8f / n
            q((u0 + u1) / 2, 0.76f, u1, 0.68f)
        }
        z()
    }
    part(shell, c, outline = false)
    markings(c, shell)
    if (c.showDetail) {
        // scute seams radiating from the top of the shell
        val top = b.p(0.48f, 0.26f)
        for (u in listOf(0.18f, 0.34f, 0.5f, 0.66f, 0.8f)) {
            drawLine(t.dark.copy(alpha = 0.7f), top, b.p(u, 0.68f), strokeWidth = c.s * 0.008f, cap = StrokeCap.Round)
        }
        drawArc(t.dark.copy(alpha = 0.7f), 190f, 160f, false, b.p(0.18f, 0.32f), Size(b.w * 0.62f, b.h * 0.5f), style = c.thin)
    }
    drawPath(shell, t.outline, style = c.line)

    if (g.tuft == Tuft.SPINES || c.growth >= 1f) {
        ridge(
            c, Offset(0.1f, 0.6f), Offset(0.12f, 0.3f), Offset(0.36f, 0.18f), Offset(0.5f, 0.2f),
            count = 3 + (2 * c.growth).toInt(), from = 0.35f, to = 0.95f,
            height = b.h * (0.04f + 0.04f * c.growth), halfBase = b.w * 0.022f, colour = t.bone,
        )
    }

    // moss fronds sprouting from the shell
    val fronds = when (c.look.stage) { Stage.BABY -> 1; Stage.TEEN -> 2; Stage.ADULT -> 3 }
    val frondLen = (0.1f + 0.06f * c.growth) * g.featureSize
    listOf(0.52f to 0.0f, 0.62f to 0.4f, 0.42f to -0.45f).take(fronds).forEach { (u, lean) ->
        val base = b.p(u, 0.215f + kotlin.math.abs(u - 0.5f) * 0.25f)
        val tip = base + Offset(lean * frondLen * b.w * 0.6f + b.w * 0.02f, -frondLen * b.h * 1.4f)
        val mid = (base + tip) / 2f
        val side = Offset(-(tip.y - base.y), tip.x - base.x).normalised() * (frondLen * b.w * 0.22f)
        val leaf = Path().apply {
            moveTo(base.x, base.y)
            quadraticTo((mid + side).x, (mid + side).y, tip.x, tip.y)
            quadraticTo((mid - side).x, (mid - side).y, base.x, base.y)
            close()
        }
        drawPath(leaf, t.leaf)
        drawPath(leaf, t.outline, style = c.thin)
        if (c.showDetail) drawLine(t.dark.copy(alpha = 0.5f), base, tip, c.s * 0.005f)
    }

    scale(c.headScale * g.reach, c.headScale, pivot = b.p(0.8f, 0.6f)) {
        if (g.tuft == Tuft.FRILL) {
            val frill = b.path { m(0.8f, 0.46f); q(0.74f, 0.5f, 0.78f, 0.66f); l(0.84f, 0.6f); z() }
            part(frill, c, fill = t.accent)
        }
        val head = b.path {
            m(0.78f, 0.5f); c(0.84f, 0.4f, 0.95f, 0.42f, 0.99f, 0.53f)
            c(1f, 0.62f, 0.94f, 0.68f, 0.84f, 0.68f); c(0.8f, 0.66f, 0.77f, 0.6f, 0.78f, 0.5f); z()
        }
        part(head, c, fill = t.skin)
        if (c.showDetail) {
            // heavy bony brow plate
            val plate = b.path { m(0.82f, 0.46f); q(0.88f, 0.4f, 0.96f, 0.47f); l(0.93f, 0.49f); q(0.88f, 0.45f, 0.83f, 0.5f); z() }
            part(plate, c, fill = t.accent)
            if (g.tuft == Tuft.CREST) {
                triangle(c, b.p(0.86f, 0.44f), b.p(0.87f, 0.36f), b.p(0.9f, 0.43f), t.bone)
            }
        }
        beastEye(c, b.p(0.9f, 0.52f), b.h * 0.026f, brow = false)
        jaw(c, 0.99f, 0.58f, 0.89f, 0.615f)
    }
}


// ---------------------------------------------------------------- no article: ember spirit

private fun DrawScope.drawEmberSpirit(c: Ctx) {
    val b = c.b
    val t = c.t
    val g = c.g
    val k = (0.45f + 0.55f * c.growth) * g.featureSize

    val flame = b.path {
        m(0.52f, 0.96f)
        c(0.84f, 0.96f, 0.94f, 0.7f, 0.8f, 0.5f)
        q(0.84f, 0.46f - 0.18f * k, 0.74f, 0.4f - 0.32f * k)
        q(0.7f, 0.4f, 0.62f, 0.42f)
        q(0.58f, 0.36f - 0.16f * k, 0.46f, 0.3f - 0.3f * k)
        q(0.48f, 0.42f, 0.36f, 0.46f)
        q(0.26f, 0.44f, 0.16f, 0.36f - 0.16f * k)
        c(0.2f, 0.58f, 0.12f, 0.92f, 0.52f, 0.96f)
        z()
    }
    if (c.look.silhouette) {
        drawPath(flame, SilhouetteColour)
        return
    }
    drawPath(flame, Brush.verticalGradient(listOf(t.ember, t.body, t.deep), startY = b.top, endY = b.top + b.h))
    // bright core
    scale(0.58f, 0.55f, pivot = b.p(0.55f, 0.9f)) {
        drawPath(flame, Brush.verticalGradient(listOf(t.ember, t.glow.copy(alpha = 0.9f)), startY = b.top + b.h * 0.3f, endY = b.top + b.h))
    }
    drawPath(flame, t.outline, style = c.line)

    val coal = Color(0xFF241006)
    if (c.closedEyes) {
        for (u in listOf(0.52f, 0.7f)) {
            drawLine(coal, b.p(u - 0.05f, 0.62f), b.p(u + 0.05f, 0.64f), strokeWidth = c.s * 0.016f, cap = StrokeCap.Round)
        }
    } else {
        // Deep, angry eye sockets slanting down to the middle, with a burning pupil.
        for ((u, dir) in listOf(0.52f to 1f, 0.7f to -1f)) {
            val socket = b.path {
                m(u - 0.065f, 0.57f - dir * 0.025f); l(u + 0.065f, 0.57f + dir * 0.025f)
                l(u + 0.04f, 0.66f); q(u, 0.69f, u - 0.04f, 0.66f); z()
            }
            drawPath(socket, coal)
            val glow = if (g.eyeStyle == EyeStyle.GLOWING) Color.White else t.glow
            drawCircle(glow.copy(alpha = 0.4f), b.h * 0.032f, b.p(u, 0.625f))
            drawCircle(glow, b.h * 0.016f, b.p(u, 0.625f))
        }
    }
    // A maw with ragged fangs.
    if (!c.look.asleep) {
        val maw = b.path { m(0.5f, 0.74f); q(0.61f, 0.86f, 0.72f, 0.73f); q(0.61f, 0.78f, 0.5f, 0.74f); z() }
        drawPath(maw, coal)
        for (u in listOf(0.55f, 0.61f, 0.67f)) {
            triangle(c, b.p(u - 0.014f, 0.755f), b.p(u, 0.8f), b.p(u + 0.014f, 0.755f), t.glow)
        }
    } else {
        drawLine(coal, b.p(0.55f, 0.77f), b.p(0.67f, 0.77f), strokeWidth = c.s * 0.012f, cap = StrokeCap.Round)
    }
    // drifting embers
    val r = SeededRandom(g.patternSeed)
    repeat(3 + (3 * c.growth).toInt()) {
        val at = b.p(r.range(0.1f, 0.95f), r.range(-0.15f, 0.35f))
        fourPointStar(at, c.s * r.range(0.012f, 0.024f), t.ember)
    }
    if (g.tuft == Tuft.CREST || g.tuft == Tuft.SPINES) {
        // two small horns of hardened ash
        triangle(c, b.p(0.5f, 0.5f), b.p(0.47f, 0.38f), b.p(0.55f, 0.48f), Color(0xFF3B2A22))
        triangle(c, b.p(0.66f, 0.48f), b.p(0.71f, 0.36f), b.p(0.72f, 0.5f), Color(0xFF3B2A22))
    }
}

// ---------------------------------------------------------------- effects

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

private fun DrawScope.drawShinySparkles(b: Box, s: Float) {
    val spots = listOf(0.08f to 0.2f, 0.9f to 0.05f, 0.95f to 0.7f, 0.15f to 0.85f, 0.5f to 0.0f)
    spots.forEachIndexed { i, (u, v) ->
        val c = b.p(u, v)
        val r = s * (0.045f - i * 0.005f)
        fourPointStar(c, r, Color(0xFFFFD54A))
        fourPointStar(c, r * 0.45f, Color.White)
    }
}

private fun DrawScope.drawZzz(b: Box, s: Float) {
    val base = b.p(0.86f, -0.02f)
    for (i in 0..2) {
        val z = s * (0.035f + i * 0.015f)
        val o = base + Offset(i * z * 0.9f, -i * z * 1.3f)
        val p = Path().apply {
            moveTo(o.x, o.y); lineTo(o.x + z, o.y); lineTo(o.x, o.y + z); lineTo(o.x + z, o.y + z)
        }
        drawPath(p, Color(0xFF8C86A3), style = Stroke(width = s * 0.011f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
