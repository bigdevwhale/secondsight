package app.secondsight.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.unit.sp
import app.secondsight.R

/**
 * A mountain night: indigo silk, mist, and the amber of paper lanterns. The app is for tired eyes,
 * so it is always dark, never pure white, and keeps contrast soft.
 */
object C {
    val bg = Color(0xFF0E1220)
    val night = Color(0xFF151B2E)
    /** Silk panels: a little lighter than the night, a little see-through. */
    val silk = Color(0xFF1C2338)
    /** Moonlight: the cool light of the second, quieter choice. */
    val moon = Color(0xFFB9C6E6)
    val silk2 = Color(0xFF252D46)
    val ink = Color(0xFFEDE4D3)
    val mute = Color(0xFFA9A9B4)
    val faint = Color(0xFF6E7286)
    val line = Color(0xFF39415C)
    /** Lantern light. */
    val amber = Color(0xFFEDB86C)
    val amberDeep = Color(0xFFC9833F)
    val amberInk = Color(0xFF2A1A08)
    val jade = Color(0xFF86C2AA)
    val rose = Color(0xFFE0A2A0)
    val mist = Color(0xFFD9DEEA)
    val sky = Color(0xFF8FA5D6)
}

/** Cormorant Garamond, light and airy, for nearly every word; Alegreya Sans only for the small print. */
@OptIn(ExperimentalTextApi::class)
private fun cg(w: Int) = Font(R.font.cormorant_var, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))
@OptIn(ExperimentalTextApi::class)
val Serif = FontFamily(cg(300), cg(400), cg(500), cg(600), Font(R.font.cormorant_light_italic, FontWeight.Light, FontStyle.Italic))
val Sans = FontFamily(Font(R.font.alegreya_regular, FontWeight.Normal), Font(R.font.alegreya_medium, FontWeight.Medium), Font(R.font.alegreya_bold, FontWeight.Bold))

object T {
    val huge = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Light, fontSize = 54.sp, lineHeight = 56.sp, color = C.ink)
    val big = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Light, fontSize = 36.sp, lineHeight = 40.sp, color = C.ink)
    val title = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Normal, fontSize = 28.sp, lineHeight = 32.sp, color = C.ink)
    val h3 = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Medium, fontSize = 23.sp, lineHeight = 26.sp, color = C.ink)
    /** The seer's words and the tales: light italic. */
    val tale = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Light, fontStyle = FontStyle.Italic, fontSize = 23.sp, lineHeight = 30.sp, color = C.ink)
    val said = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Light, fontStyle = FontStyle.Italic, fontSize = 21.sp, lineHeight = 27.sp, color = C.ink)
    val body = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Medium, fontSize = 19.sp, lineHeight = 25.sp, color = C.ink)
    val bodyB = TextStyle(fontFamily = Serif, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, lineHeight = 25.sp, color = C.ink)
    val small = TextStyle(fontFamily = Sans, fontSize = 14.sp, lineHeight = 19.sp, color = C.mute)
    val smallB = TextStyle(fontFamily = Serif, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 21.sp, color = C.ink)
    val tiny = TextStyle(fontFamily = Sans, fontSize = 12.5.sp, lineHeight = 16.sp, color = C.mute)
    /** Words you tap. */
    val btn = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Medium, fontSize = 22.sp, letterSpacing = 0.5.sp)
    /** Small capitals with air between the letters: ranks, section names. */
    val caps = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Normal, fontSize = 15.sp, letterSpacing = 4.sp, color = C.ink)
}

@Composable
fun SightTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(primary = C.amber, background = C.bg, surface = C.silk, onSurface = C.ink, onBackground = C.ink), content = content)
}
