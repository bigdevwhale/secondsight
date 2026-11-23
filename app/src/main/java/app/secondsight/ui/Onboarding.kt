package app.secondsight.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.secondsight.MainActivity
import app.secondsight.container
import app.secondsight.domain.tr
import app.secondsight.svc.Watch

/** Four short pages: the seer speaks for herself, one thought a page. */
@Composable
fun Onboarding() {
    val ctx = LocalContext.current
    val game = ctx.container.game
    val st by game.state.collectAsState()
    var page by remember { mutableIntStateOf(0) }
    val pages = 4
    val finish = {
        val list = if (android.os.Build.VERSION.SDK_INT >= 33) listOf(android.Manifest.permission.POST_NOTIFICATIONS) else emptyList()
        MainActivity.ask(list) {
            game.write { it.copy(onboarded = true, strain = 0) }
            game.arrive()
            Watch.start(ctx)
        }
    }
    if (page > 0) androidx.activity.compose.BackHandler { page-- }
    var drag by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    Box(Modifier.fillMaxSize().background(C.bg).pointerInput(Unit) {
        detectHorizontalDragGestures(
            onDragStart = { drag = 0f },
            onDragEnd = { if (drag < -80f && page < pages - 1) page++ else if (drag > 80f && page > 0) page-- },
        ) { _, d -> drag += d }
    }) {
        Pic("hall", Modifier.fillMaxSize(), scale = ContentScale.Crop, alpha = 0.6f)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to C.bg.copy(alpha = 0.7f), 0.35f to C.bg.copy(alpha = 0.15f), 0.62f to C.bg.copy(alpha = 0.75f), 1f to C.bg)))
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 32.dp, vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(30.dp))
            Text(tr("ВТОРОЕ ЗРЕНИЕ", "SECOND SIGHT"), style = T.caps.copy(color = C.ink.copy(alpha = 0.8f)))
            Spacer(Modifier.weight(1f))
            AnimatedContent(page, transitionSpec = { fadeIn(tween(700)) togetherWith fadeOut(tween(400)) }, label = "page") { p ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    when (p) {
                        0 -> Face("seer", 140.dp)
                        2 -> Orb(150.dp, 0.75f, glimpses = 0)
                        3 -> Orb(150.dp, 0f, picture = "v_t1_1", pictureAlpha = 0.5f, glimpses = 4)
                        else -> Face("seer", 96.dp)
                    }
                    Spacer(Modifier.height(30.dp))
                    Text(when (p) {
                        0 -> tr("Здравствуй. Я живу здесь, над облаками, и вижу в шаре то, чего не видят другие.",
                            "Hello. I live up here, above the clouds, and I see in the orb what others can't.")
                        1 -> tr("Могу научить и тебя. Только начнём с глаз: от экрана они устают быстрее, чем ты замечаешь.",
                            "I can teach you too. But let's begin with your eyes: a screen tires them faster than you notice.")
                        2 -> tr("Когда глазам пора отдохнуть, шар затуманится. Посмотри вдаль – и туман уйдёт.",
                            "When your eyes need a rest, the orb will mist over. Look far away, and the mist will go.")
                        else -> tr("После каждого отдыха в шаре остаётся проблеск. Из проблесков складываются сказки.",
                            "Every rest leaves a glimpse in the orb. The glimpses come together into tales.")
                    }, style = T.tale, textAlign = TextAlign.Center)
                    if (p == 1) {
                        Spacer(Modifier.height(26.dp))
                        Text(tr("как к тебе обращаться?", "how shall I call you?"), style = T.small)
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Btn(tr("ученица", "she"), main = st.female, small = true) { game.write { it.copy(female = true) } }
                            Btn(tr("ученик", "he"), main = !st.female, small = true) { game.write { it.copy(female = false) } }
                        }
                    }
                }
            }
            Spacer(Modifier.weight(1.1f))
            Glimmers(page + 1, pages, size = 18.dp) { page = it }
            Spacer(Modifier.height(14.dp))
            Box(Modifier.fillMaxWidth()) {
                if (page > 0) Link(tr("назад", "back"), Modifier.align(Alignment.CenterStart), color = C.faint) { page-- }
                Btn(if (page < pages - 1) tr("дальше", "next") else tr("начать", "begin"), Modifier.align(Alignment.Center)) {
                    if (page < pages - 1) page++ else finish()
                }
                if (page < pages - 1) Link(tr("пропустить", "skip"), Modifier.align(Alignment.CenterEnd), color = C.faint) { page = pages - 1 }
            }
        }
    }
}
