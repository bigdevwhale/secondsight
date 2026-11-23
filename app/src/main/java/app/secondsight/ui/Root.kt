package app.secondsight.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import app.secondsight.container
import app.secondsight.data.Shown
import app.secondsight.data.State
import app.secondsight.domain.Practice
import app.secondsight.domain.tr

/** What the screens show besides the tabs; one place, so notifications and the tile can open things. */
object Ui {
    var tab by mutableIntStateOf(0)
    /** Practices to play one after another (a ritual or a single one). */
    var play by mutableStateOf<List<Practice>>(emptyList())
    var vision by mutableStateOf<Shown?>(null)
    var settings by mutableStateOf(false)
    var toast by mutableStateOf<String?>(null)
    /** Lengths for the steps of [play] when they differ from the usual (a lesson of the path). */
    var secs: List<Int>? = null
    /** [play] is today's lesson of the apprentice's path. */
    var lesson = false
    /** The end of the path to show, by the number of the course just walked. */
    var pathEnd by mutableStateOf<Int?>(null)

    fun start(vararg p: Practice) { secs = null; lesson = false; play = p.toList() }
    fun ritual(list: List<Practice>) { secs = null; lesson = false; play = list }
    fun lesson(steps: List<app.secondsight.domain.Step>) { secs = steps.map { if (fast) 3 else it.sec }; lesson = true; play = steps.map { it.p } }
    /** Debug builds: lesson steps of three seconds, to walk a course through quickly. */
    var fast = false
}

@Composable
fun Root(s: State) {
    val game = LocalContext.current.container.game
    LaunchedEffect(Unit) { game.revealed.collect { /* the player shows it itself */ } }
    Box(Modifier.fillMaxSize().background(C.bg)) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                Crossfade(Ui.tab, label = "tab") { t ->
                    when (t) {
                        0 -> Hall(s)
                        1 -> Practices(s)
                        2 -> Scrolls(s)
                        else -> Path(s)
                    }
                }
            }
            Tabs(s)
        }
        Settings(s)
        Ui.vision?.let { v -> VisionView(v) { Ui.vision = null } }
        AnimatedVisibility(Ui.play.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
            if (Ui.play.isNotEmpty()) Player(Ui.play) { Ui.play = emptyList() }
        }
        Ui.pathEnd?.let { n -> PathEnd(n) { Ui.pathEnd = null } }
        Toast()
    }
    if (Ui.tab != 0 && Ui.play.isEmpty() && Ui.vision == null && Ui.pathEnd == null && !Ui.settings) BackHandler { Ui.tab = 0 }
}

/** The sections are just words in the mist; a line of light under the one you're in. */
@Composable
private fun Tabs(s: State) {
    val game = LocalContext.current.container.game
    val tabs = listOf(tr("обитель", "pavilion"), tr("практики", "practices"), tr("свитки", "scrolls"), tr("тропа", "path"))
    val icons = listOf("tab_hall", "tab_practice", "tab_scroll", "tab_path")
    Row(
        Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, C.bg.copy(alpha = 0.9f), C.bg)))
            .navigationBarsPadding().padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        tabs.forEachIndexed { i, label ->
            val on = Ui.tab == i
            Column(Modifier.clip(RoundedCornerShape(12.dp)).tap { Ui.tab = i }.padding(horizontal = 8.dp, vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Pic(icons[i], Modifier.size(30.dp), alpha = if (on) 1f else 0.45f)
                        Text(label, style = T.btn.copy(fontSize = 16.sp, color = if (on) C.ink else C.faint))
                    }
                    // a spark where something waits: a pilgrim ready to hear the answer
                    if (i == 3 && s.guest.isNotEmpty() && game.ready(s)) Light(12.dp, modifier = Modifier.align(Alignment.TopEnd).offset(x = 10.dp, y = (-2).dp))
                }
                Spacer(Modifier.height(3.dp))
                Rule(Modifier.width(44.dp), alpha = if (on) 0.9f else 0f)
            }
        }
    }
}

@Composable
private fun Toast() {
    val msg = Ui.toast
    LaunchedEffect(msg) { if (msg != null) { kotlinx.coroutines.delay(3200); Ui.toast = null } }
    AnimatedVisibility(msg != null, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().padding(bottom = 110.dp, start = 24.dp, end = 24.dp), contentAlignment = Alignment.BottomCenter) {
            Text(msg.orEmpty(), Modifier.silk().padding(horizontal = 18.dp, vertical = 14.dp), style = T.said, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}
