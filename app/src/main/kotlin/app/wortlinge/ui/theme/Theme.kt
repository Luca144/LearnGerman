package app.wortlinge.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.wortlinge.core.pets.Species

object Palette {
    val Cream = Color(0xFFFFF8EC)
    val Paper = Color(0xFFFFFFFF)
    val Ink = Color(0xFF2B2A33)
    val InkSoft = Color(0xFF6B6875)
    val Honey = Color(0xFFF2A93B)
    val HoneyDeep = Color(0xFFC77A12)
    val Berry = Color(0xFFD9466F)
    val Leaf = Color(0xFF3E8E41)
    val Sky = Color(0xFF3477C9)
    val Legendary = Color(0xFF8E5BD8)
    val Rare = Color(0xFF2E9BB5)
}

/** The colour used for an article's text and labels. Matches the pet families. */
fun Species.labelColour(): Color = when (this) {
    Species.DER -> Color(0xFF2F6FBF)
    Species.DIE -> Color(0xFFC93A5A)
    Species.DAS -> Color(0xFF3C8A3F)
    Species.WISP -> Color(0xFFB07A0E)
}

private val Rounded = FontFamily.SansSerif

private val AppTypography = Typography(
    headlineLarge = TextStyle(fontFamily = Rounded, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp),
    headlineSmall = TextStyle(fontFamily = Rounded, fontWeight = FontWeight.Bold, fontSize = 22.sp),
    titleLarge = TextStyle(fontFamily = Rounded, fontWeight = FontWeight.Bold, fontSize = 22.sp),
    titleMedium = TextStyle(fontFamily = Rounded, fontWeight = FontWeight.Bold, fontSize = 18.sp),
    bodyLarge = TextStyle(fontFamily = Rounded, fontSize = 17.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Rounded, fontSize = 15.sp, lineHeight = 21.sp),
    labelLarge = TextStyle(fontFamily = Rounded, fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
)

private val Colours = lightColorScheme(
    primary = Palette.HoneyDeep,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE3A8),
    onPrimaryContainer = Palette.Ink,
    secondary = Palette.Leaf,
    background = Palette.Cream,
    onBackground = Palette.Ink,
    surface = Palette.Paper,
    onSurface = Palette.Ink,
    onSurfaceVariant = Palette.InkSoft,
    surfaceVariant = Color(0xFFF6EBD9),
    error = Palette.Berry,
)

@Composable
fun WortlingeTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colours, typography = AppTypography, content = content)
}
