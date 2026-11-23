package app.secondsight.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import app.secondsight.BuildConfig
import app.secondsight.domain.Rand
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Screenshots want a still screen. */
object Still { @Volatile var on = BuildConfig.SHOTS }

/** Time in seconds, looping slowly; still for screenshots. */
@Composable
fun clock(period: Int = 600_000): Float {
    if (Still.on) return 37f
    val t = rememberInfiniteTransition(label = "clock")
    val v by t.animateFloat(0f, period / 1000f, infiniteRepeatable(tween(period, easing = LinearEasing), RepeatMode.Restart), label = "t")
    return v
}

/**
 * The crystal orb. [mist] 0..1 is how misted it is (screen time without rest); [picture] is a vision showing
 * through faintly; [glimpses] little sparks drift inside; [glow] is the warm halo behind it.
 */
@Composable
fun Orb(dim: Dp, mist: Float, modifier: Modifier = Modifier, picture: String? = null, pictureAlpha: Float = 0.35f, glimpses: Int = 0, glow: Float = 1f) {
    val t = clock()
    Box(modifier.size(dim)) {
        Canvas(Modifier.fillMaxSize()) { halo(glow) }
        Box(Modifier.fillMaxSize().clip(CircleShape)) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(Brush.radialGradient(listOf(Color(0xFF2B3757), Color(0xFF141A2E)), center = Offset(size.width * 0.45f, size.height * 0.4f), radius = size.minDimension * 0.6f))
            }
            if (picture != null) Pic(picture, Modifier.fillMaxSize(), scale = ContentScale.Crop, alpha = pictureAlpha * (1f - mist * 0.85f))
            Canvas(Modifier.fillMaxSize()) {
                mistOf(mist, t)
                sparks(glimpses, t)
                glass()
            }
        }
    }
}

private fun DrawScope.halo(glow: Float) {
    val r = size.minDimension / 2
    drawCircle(Brush.radialGradient(listOf(C.amber.copy(alpha = 0.22f * glow), C.amber.copy(alpha = 0.06f * glow), Color.Transparent), center, r * 1.0f), r * 1.0f)
}

private fun DrawScope.mistOf(mist: Float, t: Float) {
    if (mist <= 0.01f) return
    val r = size.minDimension / 2
    // a whitish veil that thickens, and drifting clouds of mist inside it
    drawCircle(C.mist.copy(alpha = 0.32f * mist * mist), r, center)
    val rnd = Rand(7)
    repeat(14) { i ->
        val sp = rnd.range(0.02f, 0.06f) * (if (i % 2 == 0) 1 else -1)
        val a = rnd.next() * 2 * PI.toFloat() + t * sp
        val d = rnd.range(0.1f, 0.6f) * r
        val c = Offset(center.x + cos(a) * d, center.y + sin(a) * d * 0.7f + sin(t * 0.05f + i) * r * 0.05f)
        val br = rnd.range(0.25f, 0.55f) * r
        val tint = if (i % 3 == 0) C.sky else C.mist
        drawCircle(Brush.radialGradient(listOf(tint.copy(alpha = 0.42f * mist), tint.copy(alpha = 0.12f * mist), Color.Transparent), c, br), br, c)
    }
}

private fun DrawScope.sparks(n: Int, t: Float) {
    if (n <= 0) return
    val r = size.minDimension / 2
    val rnd = Rand(31)
    repeat(n.coerceAtMost(12)) { i ->
        val a = rnd.next() * 6.28f + t * rnd.range(0.08f, 0.2f)
        val d = rnd.range(0.15f, 0.55f) * r
        val c = Offset(center.x + cos(a) * d, center.y + sin(a * 1.3f) * d * 0.8f)
        val tw = 0.6f + 0.4f * sin(t * 2f + i)
        drawCircle(Brush.radialGradient(listOf(C.amber.copy(alpha = 0.9f * tw), Color.Transparent), c, r * 0.07f), r * 0.07f, c)
        drawCircle(Color(0xFFFFF3D6).copy(alpha = tw), r * 0.012f, c)
    }
}

/** Highlights of the glass: a soft window of light top left, a thin rim, a warm reflection below. */
private fun DrawScope.glass() {
    val r = size.minDimension / 2
    drawCircle(Brush.radialGradient(listOf(Color.Transparent, Color.Transparent, Color(0xFF0B0F1C).copy(alpha = 0.55f)), center, r), r, center)
    val hl = Offset(center.x - r * 0.38f, center.y - r * 0.42f)
    drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.32f), Color.Transparent), hl, r * 0.32f), r * 0.32f, hl)
    val lo = Offset(center.x + r * 0.25f, center.y + r * 0.62f)
    drawCircle(Brush.radialGradient(listOf(C.amber.copy(alpha = 0.22f), Color.Transparent), lo, r * 0.35f), r * 0.35f, lo)
    drawCircle(Color.White.copy(alpha = 0.18f), r - 1.5f, center, style = Stroke(2f))
}

/** A small seeded random, here so the orb's mist always drifts the same way. */
@Composable
fun rememberSeed(key: Any) = remember(key) { key.hashCode() }
