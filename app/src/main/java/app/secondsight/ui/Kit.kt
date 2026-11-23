package app.secondsight.ui

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

fun Modifier.tap(onClick: () -> Unit) = this.clickable(MutableInteractionSource(), null, onClick = onClick)

private val ids = HashMap<String, Int>()

@SuppressLint("DiscouragedApi")
@Composable
fun drawableId(name: String): Int {
    val ctx = LocalContext.current
    return ids.getOrPut(name) { ctx.resources.getIdentifier(name, "drawable", ctx.packageName) }
}

/** A drawable by name (the cut Codex art); [shadow] draws a silhouette of what is not found yet. */
@Composable
fun Pic(name: String, modifier: Modifier = Modifier, scale: ContentScale = ContentScale.Fit, alpha: Float = 1f, shadow: Boolean = false) {
    val id = drawableId(name)
    if (id != 0) Image(painterResource(id), null, modifier.alpha(alpha), contentScale = scale,
        colorFilter = if (shadow) ColorFilter.tint(C.line, BlendMode.SrcIn) else null)
    else Box(modifier)
}

/**
 * No panels in the mist: only a soft darkening behind words that sit over a picture, with no edge to it.
 * (The name stays from the first design; every caller now gets mist instead of a box.)
 */
fun Modifier.silk(r: Dp = 20.dp, fill: Color = C.bg, glow: Color = C.amber) = this.drawBehind {
    val c = Offset(size.width / 2, size.height / 2)
    val rr = maxOf(size.width, size.height) * 0.62f
    drawRect(Brush.radialGradient(listOf(fill.copy(alpha = 0.5f), fill.copy(alpha = 0.25f), Color.Transparent), c, rr))
}

/** A thin line of light that fades at both ends. */
@Composable
fun Rule(modifier: Modifier = Modifier, color: Color = C.amber, alpha: Float = 0.6f) =
    Box(modifier.height(1.dp).background(Brush.horizontalGradient(listOf(Color.Transparent, color.copy(alpha = alpha), Color.Transparent))))

/** A small living light: amber for the warm choice, moonlight for the quiet one. */
@Composable
fun Light(size: Dp, warm: Boolean = true, modifier: Modifier = Modifier, strength: Float = 1f) {
    val t = clock(8_000)
    val pulse = 0.88f + 0.12f * kotlin.math.sin(t * 0.8f)
    val col = if (warm) C.amber else C.moon
    androidx.compose.foundation.Canvas(modifier.size(size)) {
        val r = this.size.minDimension / 2
        drawCircle(Brush.radialGradient(listOf(col.copy(alpha = 0.55f * strength * pulse), col.copy(alpha = 0.18f * strength), Color.Transparent), center, r), r)
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF3DA).copy(alpha = strength), col.copy(alpha = 0.7f * strength), Color.Transparent), center, r * 0.42f * pulse), r * 0.42f)
    }
}

/** The two main choices of a screen: a light to reach for, with its word in a capsule under it. */
@Composable
fun Lantern(text: String, modifier: Modifier = Modifier, warm: Boolean = true, size: Dp = 64.dp, onClick: () -> Unit) {
    Column(modifier.clip(RoundedCornerShape(28.dp)).clickable(onClick = onClick).padding(horizontal = 6.dp, vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Light(size, warm)
        Text(
            text,
            Modifier.clip(RoundedCornerShape(50)).background(if (warm) C.amber.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.05f))
                .border(1.dp, if (warm) C.amber.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.18f), RoundedCornerShape(50))
                .padding(horizontal = 22.dp, vertical = 7.dp),
            style = T.btn.copy(fontSize = 20.sp, color = if (warm) Color(0xFFF6E6C6) else C.ink.copy(alpha = 0.85f)), textAlign = TextAlign.Center,
        )
    }
}

/**
 * A button: a capsule with a thin rim. [main] glows warm (amber rim, a breath of amber inside); the rest have a pale rim.
 * Clear to see as a thing to press, without a flat coloured slab.
 */
@Composable
fun Btn(text: String, modifier: Modifier = Modifier, main: Boolean = true, enabled: Boolean = true, icon: String? = null, small: Boolean = false, onClick: () -> Unit) {
    val color = when { !enabled -> C.faint; main -> Color(0xFFF6E6C6); else -> C.ink.copy(alpha = 0.85f) }
    val shape = RoundedCornerShape(50)
    Row(
        modifier.clip(shape)
            .background(if (main && enabled) Brush.verticalGradient(listOf(C.amber.copy(alpha = 0.20f), C.amber.copy(alpha = 0.08f))) else Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.05f), Color.White.copy(alpha = 0.02f))))
            .border(1.dp, if (main && enabled) C.amber.copy(alpha = 0.65f) else Color.White.copy(alpha = if (enabled) 0.16f else 0.07f), shape)
            .clickable(enabled = enabled, onClick = onClick)
            .heightIn(min = if (small) 40.dp else 52.dp).padding(horizontal = if (small) 12.dp else 20.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) { Pic(icon, Modifier.size(if (small) 22.dp else 28.dp)); Spacer(Modifier.width(8.dp)) }
        Text(text, style = T.btn.copy(fontSize = if (small) 17.sp else 20.sp, color = color), textAlign = TextAlign.Center, maxLines = 1)
    }
}

/** A pane of misty glass: what sits on it belongs together; with [onClick] the whole pane opens something (and shows a ›). */
@Composable
fun Card(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, pad: Dp = 16.dp, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier.fillMaxWidth().clip(shape)
            .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.065f), Color.White.copy(alpha = 0.025f))))
            .border(1.dp, Color.White.copy(alpha = 0.09f), shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Column(Modifier.fillMaxWidth().padding(pad), content = content)
    }
}

/** The small arrow at the end of a row that opens something. */
@Composable
fun Chevron(modifier: Modifier = Modifier) = Text("›", modifier, style = T.title.copy(color = C.amber.copy(alpha = 0.8f)))

/** A quiet text link. */
@Composable
fun Link(text: String, modifier: Modifier = Modifier, color: Color = C.mute, onClick: () -> Unit) =
    Text(text, modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 8.dp), style = T.btn.copy(fontSize = 18.sp, color = color))

/** A thread of lantern light: how far along. */
@Composable
fun Thread(f: Float, modifier: Modifier = Modifier, color: Color = C.amber) {
    Box(modifier.height(2.dp).background(C.line.copy(alpha = 0.45f))) {
        Box(Modifier.fillMaxWidth(f.coerceIn(0f, 1f)).height(2.dp).background(Brush.horizontalGradient(listOf(color.copy(alpha = 0.2f), color))))
    }
}

/** A bottom sheet of silk over a dimmed screen. */
@Composable
fun Sheet(open: Boolean, onClose: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    if (open) BackHandler(onBack = onClose)
    AnimatedVisibility(open, enter = fadeIn(), exit = fadeOut()) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)).tap(onClose))
    }
    AnimatedVisibility(open, enter = slideInVertically { it }, exit = slideOutVertically { it }) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 760.dp)
                    .background(Brush.verticalGradient(0f to C.bg.copy(alpha = 0f), 0.06f to C.bg.copy(alpha = 0.96f), 1f to C.bg))
                    .tap { }
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = 22.dp, vertical = 18.dp),
            ) {
                Spacer(Modifier.height(26.dp))
                content()
            }
        }
    }
}

@Composable
fun Label(text: String, modifier: Modifier = Modifier, style: TextStyle = T.tiny) =
    Text(text.uppercase(), modifier, style = T.caps.copy(fontSize = 14.sp, color = C.amber.copy(alpha = 0.85f)))

/** A round portrait in a thin amber ring. */
@Composable
fun Face(id: String, size: Dp, modifier: Modifier = Modifier, dim: Boolean = false) {
    Box(modifier.size(size).drawBehind {
        if (!dim) drawCircle(Brush.radialGradient(listOf(C.amber.copy(alpha = 0.28f), Color.Transparent), center, this.size.minDimension * 0.75f), this.size.minDimension * 0.75f)
    }.clip(RoundedCornerShape(50)).border(1.dp, C.amber.copy(alpha = if (dim) 0.15f else 0.5f), RoundedCornerShape(50))) {
        Pic("face_$id", Modifier.fillMaxSize(), alpha = if (dim) 0.35f else 1f)
    }
}

/** [of] small lights in a row, [n] of them lit: glimpses gathered, steps of a ritual, pages. */
@Composable
fun Glimmers(n: Int, of: Int, modifier: Modifier = Modifier, size: Dp = 16.dp, warm: Boolean = true, onTap: ((Int) -> Unit)? = null) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(size * 0.15f), verticalAlignment = Alignment.CenterVertically) {
        repeat(of) { i ->
            val m = if (onTap != null) Modifier.clip(RoundedCornerShape(50)).clickable { onTap(i) } else Modifier
            Light(size, warm = warm && i < n, modifier = m, strength = if (i < n) 1f else 0.28f)
        }
    }
}

/** The head of a tab: a light title and one quiet line under it. */
@Composable
fun Header(title: String, sub: String? = null, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(top = 8.dp, bottom = 14.dp)) {
        Text(title, style = T.big)
        if (sub != null) Text(sub, style = T.said.copy(fontSize = 18.sp, lineHeight = 23.sp, color = C.mute))
        Spacer(Modifier.height(10.dp))
        Rule(Modifier.width(90.dp), alpha = 0.45f)
    }
}
