package app.wortlinge.ui.pictures

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import app.wortlinge.ui.pets.fourPointStar

/**
 * Small picture of what a word means, drawn in code. Each drawing works in a 0..1 unit square
 * so it scales to any size. Words without a drawing get a neutral question mark bubble.
 */
@Composable
fun WordPicture(wordId: String, modifier: Modifier = Modifier) {
    Canvas(modifier) { drawWordPicture(wordId) }
}

private val Outline = Color(0xFF3A2E2A)
private val Crust = Color(0xFFB8732E)
private val CrustDark = Color(0xFF8A5220)
private val Dough = Color(0xFFE9B872)
private val Cream = Color(0xFFFFF6E6)
private val Cherry = Color(0xFFC4183C)
private val Choc = Color(0xFF5A3420)
private val Skin = Color(0xFFF6C667)
private val Water = Color(0xFF8FD3F4)

/** Unit-square helper: maps 0..1 coordinates onto the canvas. */
private class U(val scope: DrawScope) {
    val w = scope.size.width
    val h = scope.size.height
    val m = minOf(w, h)
    private val ox = (w - m) / 2
    private val oy = (h - m) / 2
    fun p(x: Float, y: Float) = Offset(ox + x * m, oy + y * m)
    fun s(x: Float, y: Float) = Size(x * m, y * m)
    fun d(v: Float) = v * m
    fun path(block: Path.(U) -> Unit) = Path().apply { this.block(this@U) }
}

private fun Path.moveTo(o: Offset) = moveTo(o.x, o.y)
private fun Path.lineTo(o: Offset) = lineTo(o.x, o.y)
private fun Path.quadTo(c: Offset, e: Offset) = quadraticTo(c.x, c.y, e.x, e.y)
private fun Path.cubic(c1: Offset, c2: Offset, e: Offset) = cubicTo(c1.x, c1.y, c2.x, c2.y, e.x, e.y)

fun DrawScope.drawWordPicture(wordId: String) {
    val u = U(this)
    val line = Stroke(width = u.d(0.03f), cap = StrokeCap.Round, join = StrokeJoin.Round)
    when (wordId) {
        "brot" -> loaf(u, line)
        "broetchen" -> roll(u, line)
        "brezel" -> pretzel(u)
        "kuchen" -> cakeSlice(u, line)
        "kaffee" -> coffee(u, line)
        "milch" -> milk(u, line)
        "wasser" -> waterGlass(u, line)
        "schwarzwaelder_kirschtorte" -> blackForest(u, line)
        "hallo" -> wavingHand(u, line)
        "danke" -> heart(u, line)
        "bitte" -> gift(u, line)
        "tschuess" -> door(u, line)
        else -> drawCircle(Color(0xFFEDE4D3), u.d(0.4f), u.p(0.5f, 0.5f))
    }
}

private fun DrawScope.loaf(u: U, line: Stroke) {
    val p = u.path {
        moveTo(u.p(0.12f, 0.72f))
        cubic(u.p(0.08f, 0.35f), u.p(0.3f, 0.25f), u.p(0.5f, 0.25f))
        cubic(u.p(0.7f, 0.25f), u.p(0.92f, 0.35f), u.p(0.88f, 0.72f))
        close()
    }
    drawPath(p, Crust)
    drawPath(p, Outline, style = line)
    for (i in 0..2) {
        val x = 0.3f + i * 0.18f
        drawLine(Dough, u.p(x, 0.42f), u.p(x + 0.1f, 0.33f), strokeWidth = u.d(0.04f), cap = StrokeCap.Round)
    }
    drawLine(Outline, u.p(0.12f, 0.72f), u.p(0.88f, 0.72f), strokeWidth = u.d(0.03f), cap = StrokeCap.Round)
}

private fun DrawScope.roll(u: U, line: Stroke) {
    drawOval(Dough, u.p(0.18f, 0.32f), u.s(0.64f, 0.44f))
    drawOval(Crust, u.p(0.18f, 0.28f), u.s(0.64f, 0.42f))
    drawOval(Outline, u.p(0.18f, 0.28f), u.s(0.64f, 0.42f), style = line)
    val cut = u.path {
        moveTo(u.p(0.3f, 0.47f)); quadTo(u.p(0.5f, 0.36f), u.p(0.7f, 0.47f))
    }
    drawPath(cut, Dough, style = Stroke(width = u.d(0.06f), cap = StrokeCap.Round))
}

private fun DrawScope.pretzel(u: U) {
    val dough = Stroke(width = u.d(0.1f), cap = StrokeCap.Round, join = StrokeJoin.Round)
    val p = u.path {
        moveTo(u.p(0.3f, 0.78f))
        cubic(u.p(0.02f, 0.62f), u.p(0.08f, 0.2f), u.p(0.38f, 0.22f))
        cubic(u.p(0.56f, 0.24f), u.p(0.58f, 0.46f), u.p(0.5f, 0.55f))
        cubic(u.p(0.42f, 0.46f), u.p(0.44f, 0.24f), u.p(0.62f, 0.22f))
        cubic(u.p(0.92f, 0.2f), u.p(0.98f, 0.62f), u.p(0.7f, 0.78f))
        moveTo(u.p(0.3f, 0.78f)); lineTo(u.p(0.6f, 0.5f))
        moveTo(u.p(0.7f, 0.78f)); lineTo(u.p(0.4f, 0.5f))
    }
    drawPath(p, CrustDark, style = Stroke(width = u.d(0.13f), cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(p, Crust, style = dough)
    listOf(0.2f to 0.4f, 0.8f to 0.42f, 0.35f to 0.25f, 0.66f to 0.26f, 0.5f to 0.68f).forEach { (x, y) ->
        drawRect(Color.White, u.p(x, y), u.s(0.025f, 0.025f))
    }
}

private fun DrawScope.cakeSlice(u: U, line: Stroke) {
    val sponge = u.path {
        moveTo(u.p(0.15f, 0.45f)); lineTo(u.p(0.85f, 0.35f)); lineTo(u.p(0.85f, 0.75f)); lineTo(u.p(0.15f, 0.8f)); close()
    }
    drawPath(sponge, Color(0xFFF3D27A))
    drawLine(Color(0xFFE9849E), u.p(0.15f, 0.62f), u.p(0.85f, 0.55f), strokeWidth = u.d(0.06f))
    val icing = u.path {
        moveTo(u.p(0.12f, 0.45f)); lineTo(u.p(0.88f, 0.34f)); lineTo(u.p(0.88f, 0.42f))
        quadTo(u.p(0.8f, 0.5f), u.p(0.74f, 0.43f)); quadTo(u.p(0.62f, 0.52f), u.p(0.55f, 0.45f))
        quadTo(u.p(0.4f, 0.56f), u.p(0.32f, 0.5f)); quadTo(u.p(0.2f, 0.56f), u.p(0.12f, 0.5f)); close()
    }
    drawPath(icing, Cream)
    drawPath(sponge, Outline, style = line)
}

private fun DrawScope.coffee(u: U, line: Stroke) {
    drawOval(Color(0xFFE8E2D8), u.p(0.12f, 0.72f), u.s(0.76f, 0.14f))
    drawOval(Outline, u.p(0.12f, 0.72f), u.s(0.76f, 0.14f), style = line)
    drawArc(Color.White, -70f, 140f, useCenter = false, topLeft = u.p(0.6f, 0.42f), size = u.s(0.2f, 0.2f), style = Stroke(u.d(0.05f)))
    drawArc(Outline, -70f, 140f, useCenter = false, topLeft = u.p(0.6f, 0.42f), size = u.s(0.2f, 0.2f), style = line)
    val cup = u.path {
        moveTo(u.p(0.22f, 0.38f)); lineTo(u.p(0.72f, 0.38f))
        cubic(u.p(0.72f, 0.65f), u.p(0.62f, 0.78f), u.p(0.47f, 0.78f))
        cubic(u.p(0.32f, 0.78f), u.p(0.22f, 0.65f), u.p(0.22f, 0.38f)); close()
    }
    drawPath(cup, Color.White)
    drawPath(cup, Outline, style = line)
    drawOval(Color(0xFF6B3E1E), u.p(0.24f, 0.35f), u.s(0.46f, 0.08f))
    for (i in 0..2) {
        val x = 0.34f + i * 0.13f
        val steam = u.path {
            moveTo(u.p(x, 0.3f)); cubic(u.p(x - 0.05f, 0.24f), u.p(x + 0.05f, 0.18f), u.p(x, 0.1f))
        }
        drawPath(steam, Color(0xFFB9B0A6), style = Stroke(u.d(0.025f), cap = StrokeCap.Round))
    }
}

private fun DrawScope.milk(u: U, line: Stroke) {
    val bottle = u.path {
        moveTo(u.p(0.4f, 0.15f)); lineTo(u.p(0.6f, 0.15f)); lineTo(u.p(0.6f, 0.28f))
        cubic(u.p(0.72f, 0.35f), u.p(0.72f, 0.4f), u.p(0.72f, 0.48f))
        lineTo(u.p(0.72f, 0.82f)); quadTo(u.p(0.72f, 0.88f), u.p(0.66f, 0.88f))
        lineTo(u.p(0.34f, 0.88f)); quadTo(u.p(0.28f, 0.88f), u.p(0.28f, 0.82f))
        lineTo(u.p(0.28f, 0.48f)); cubic(u.p(0.28f, 0.4f), u.p(0.28f, 0.35f), u.p(0.4f, 0.28f)); close()
    }
    drawPath(bottle, Color.White)
    drawRect(Color(0xFF4F8FD8), u.p(0.28f, 0.55f), u.s(0.44f, 0.16f))
    drawRoundRect(Color(0xFF4F8FD8), u.p(0.38f, 0.1f), u.s(0.24f, 0.08f), CornerRadius(u.d(0.02f)))
    drawPath(bottle, Outline, style = line)
}

private fun DrawScope.waterGlass(u: U, line: Stroke) {
    val glass = u.path {
        moveTo(u.p(0.25f, 0.15f)); lineTo(u.p(0.75f, 0.15f)); lineTo(u.p(0.68f, 0.86f)); lineTo(u.p(0.32f, 0.86f)); close()
    }
    val water = u.path {
        moveTo(u.p(0.28f, 0.38f)); quadTo(u.p(0.5f, 0.34f), u.p(0.72f, 0.38f)); lineTo(u.p(0.68f, 0.86f)); lineTo(u.p(0.32f, 0.86f)); close()
    }
    drawPath(glass, Color(0xFFF2FAFF))
    drawPath(water, Water)
    drawLine(Color.White, u.p(0.36f, 0.45f), u.p(0.38f, 0.78f), strokeWidth = u.d(0.035f), cap = StrokeCap.Round)
    drawPath(glass, Outline, style = line)
}

private fun DrawScope.blackForest(u: U, line: Stroke) {
    drawOval(Color(0xFFE8E2D8), u.p(0.06f, 0.78f), u.s(0.88f, 0.12f))
    val layers = listOf(Choc, Cream, Choc, Cream, Choc)
    val top = 0.4f
    val step = 0.08f
    layers.forEachIndexed { i, c ->
        drawRect(c, u.p(0.14f, top + i * step), u.s(0.72f, step))
    }
    drawOval(Cream, u.p(0.14f, top - 0.06f), u.s(0.72f, 0.12f))
    drawRect(Outline, u.p(0.14f, top), u.s(0.72f, step * 5), style = line)
    for (i in 0..3) {
        val x = 0.22f + i * 0.18f
        drawCircle(Color.White, u.d(0.06f), u.p(x, top - 0.04f))
        drawCircle(Cherry, u.d(0.04f), u.p(x + 0.01f, top - 0.1f))
        drawLine(Color(0xFF3F7F2C), u.p(x + 0.01f, top - 0.13f), u.p(x + 0.04f, top - 0.2f), strokeWidth = u.d(0.012f))
    }
    for (i in 0..5) drawRect(Choc, u.p(0.2f + i * 0.1f, top + 0.0f), u.s(0.02f, 0.025f))
}

private fun DrawScope.wavingHand(u: U, line: Stroke) {
    rotate(-15f, pivot = u.p(0.5f, 0.6f)) {
        val palm = u.p(0.3f, 0.45f)
        val fingers = listOf(0.3f to 0.18f, 0.42f to 0.12f, 0.54f to 0.14f, 0.66f to 0.22f)
        fingers.forEach { (x, y) ->
            drawRoundRect(Skin, u.p(x, y), u.s(0.1f, 0.38f), CornerRadius(u.d(0.05f)))
            drawRoundRect(Outline, u.p(x, y), u.s(0.1f, 0.38f), CornerRadius(u.d(0.05f)), style = line)
        }
        drawRoundRect(Skin, palm, u.s(0.46f, 0.38f), CornerRadius(u.d(0.14f)))
        drawRoundRect(Outline, palm, u.s(0.46f, 0.38f), CornerRadius(u.d(0.14f)), style = line)
        rotate(-40f, pivot = u.p(0.3f, 0.65f)) {
            drawRoundRect(Skin, u.p(0.12f, 0.55f), u.s(0.24f, 0.1f), CornerRadius(u.d(0.05f)))
            drawRoundRect(Outline, u.p(0.12f, 0.55f), u.s(0.24f, 0.1f), CornerRadius(u.d(0.05f)), style = line)
        }
    }
    listOf(0.12f, 0.2f).forEach { r ->
        drawArc(Color(0xFFB9B0A6), 200f, 60f, false, u.p(0.5f - r, 0.3f - r), u.s(r * 2, r * 2), style = Stroke(u.d(0.025f), cap = StrokeCap.Round))
    }
}

private fun DrawScope.heart(u: U, line: Stroke) {
    val p = u.path {
        moveTo(u.p(0.5f, 0.85f))
        cubic(u.p(0.1f, 0.6f), u.p(0.08f, 0.25f), u.p(0.3f, 0.22f))
        cubic(u.p(0.42f, 0.2f), u.p(0.48f, 0.28f), u.p(0.5f, 0.34f))
        cubic(u.p(0.52f, 0.28f), u.p(0.58f, 0.2f), u.p(0.7f, 0.22f))
        cubic(u.p(0.92f, 0.25f), u.p(0.9f, 0.6f), u.p(0.5f, 0.85f)); close()
    }
    drawPath(p, Color(0xFFE5486A))
    drawOval(Color.White.copy(alpha = 0.5f), u.p(0.24f, 0.3f), u.s(0.12f, 0.08f))
    drawPath(p, Outline, style = line)
    fourPointStar(u.p(0.86f, 0.18f), u.d(0.06f), Color(0xFFFFC94A))
    fourPointStar(u.p(0.14f, 0.82f), u.d(0.045f), Color(0xFFFFC94A))
}

private fun DrawScope.gift(u: U, line: Stroke) {
    val box = Color(0xFF5BA4E6)
    val ribbon = Color(0xFFFFC94A)
    drawRect(box, u.p(0.2f, 0.45f), u.s(0.6f, 0.4f))
    drawRect(Color(0xFF4A90D0), u.p(0.16f, 0.36f), u.s(0.68f, 0.12f))
    drawRect(ribbon, u.p(0.45f, 0.36f), u.s(0.1f, 0.49f))
    drawRect(Outline, u.p(0.2f, 0.45f), u.s(0.6f, 0.4f), style = line)
    drawRect(Outline, u.p(0.16f, 0.36f), u.s(0.68f, 0.12f), style = line)
    for (side in listOf(-1f, 1f)) {
        val bow = u.path {
            moveTo(u.p(0.5f, 0.36f))
            cubic(u.p(0.5f + side * 0.05f, 0.18f), u.p(0.5f + side * 0.25f, 0.18f), u.p(0.5f + side * 0.08f, 0.36f))
            close()
        }
        drawPath(bow, ribbon)
        drawPath(bow, Outline, style = line)
    }
}

private fun DrawScope.door(u: U, line: Stroke) {
    drawRoundRect(Color(0xFFFFE7B3), u.p(0.18f, 0.14f), u.s(0.4f, 0.74f), CornerRadius(u.d(0.04f)))
    val open = u.path {
        moveTo(u.p(0.18f, 0.14f)); lineTo(u.p(0.36f, 0.2f)); lineTo(u.p(0.36f, 0.82f)); lineTo(u.p(0.18f, 0.88f)); close()
    }
    drawPath(open, Color(0xFFB8732E))
    drawPath(open, Outline, style = line)
    drawCircle(Outline, u.d(0.02f), u.p(0.32f, 0.52f))
    drawRoundRect(Outline, u.p(0.18f, 0.14f), u.s(0.4f, 0.74f), CornerRadius(u.d(0.04f)), style = line)
    val arrow = u.path {
        moveTo(u.p(0.5f, 0.52f)); lineTo(u.p(0.86f, 0.52f))
        moveTo(u.p(0.76f, 0.42f)); lineTo(u.p(0.86f, 0.52f)); lineTo(u.p(0.76f, 0.62f))
    }
    drawPath(arrow, Color(0xFF3E8E41), style = Stroke(u.d(0.05f), cap = StrokeCap.Round, join = StrokeJoin.Round))
}
