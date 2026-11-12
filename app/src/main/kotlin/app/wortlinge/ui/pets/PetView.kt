package app.wortlinge.ui.pets

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.wortlinge.core.pets.Seeds
import kotlinx.coroutines.delay

/**
 * An animated pet: it breathes gently, blinks now and then, and hops while its word is playing.
 * Sleeping pets only breathe.
 */
@Composable
fun PetView(look: PetLook, modifier: Modifier = Modifier, talking: Boolean = false) {
    val idle = rememberInfiniteTransition(label = "idle")
    val breathe by idle.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathe",
    )
    val float by idle.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Reverse),
        label = "float",
    )

    val hop = remember { Animatable(0f) }
    LaunchedEffect(talking) {
        if (talking && !look.asleep) {
            while (true) {
                hop.animateTo(1f, tween(160, easing = FastOutSlowInEasing))
                hop.animateTo(0f, tween(200, easing = FastOutSlowInEasing))
            }
        } else hop.animateTo(0f, tween(150))
    }

    var blink by remember { mutableStateOf(false) }
    LaunchedEffect(look.genes, look.asleep, look.silhouette) {
        if (look.asleep || look.silhouette) return@LaunchedEffect
        // Each pet blinks on its own rhythm so a grid of pets doesn't blink in unison.
        val offset = (Seeds.of(look.genes.hashCode().toString()) ushr 50) % 2000
        delay(offset)
        while (true) {
            delay(2600 + offset)
            blink = true
            delay(130)
            blink = false
        }
    }

    Canvas(modifier) {
        val s = size.minDimension
        val wispFloat = if (look.genes.species == app.wortlinge.core.pets.Species.WISP) float * s * 0.04f else 0f
        val lift = hop.value * s * 0.08f + wispFloat + breathe * s * 0.006f
        drawPet(look, lift = lift, blink = blink)
    }
}
