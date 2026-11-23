package app.secondsight.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.secondsight.container
import app.secondsight.data.State
import app.secondsight.domain.Course
import app.secondsight.domain.Stage
import app.secondsight.domain.dur
import app.secondsight.domain.tr

/** The apprentice's path at the top of the practices: where you are, the seer's word on it, today's lesson. */
@Composable
fun ApprenticePath(s: State) {
    val game = LocalContext.current.container.game
    val done = game.lessonDone(s)
    val day = game.lessonNo(s)
    val stage = Stage.of(day)
    val steps = Course.lesson(day)
    Card(pad = 18.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(tr("Путь ученика", "The apprentice's path"), style = T.h3)
                Text(tr("Ступень «${stage.title}» · день $day из ${Course.DAYS}", "${stage.title} · day $day of ${Course.DAYS}"), style = T.tiny.copy(color = C.amber))
            }
            if (s.courses > 0) Text("✓${s.courses}", style = T.small.copy(color = C.jade))
        }
        Spacer(Modifier.height(10.dp))
        // 21 days as a thread of lights
        val lit = if (done) day else day - 1
        Thread(lit / Course.DAYS.toFloat(), Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        Text(stage.say, style = T.small)
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            steps.forEach { Pic(it.p.icon, Modifier.size(28.dp), alpha = if (done) 0.45f else 1f); Spacer(Modifier.width(4.dp)) }
            Spacer(Modifier.weight(1f))
            Text(dur(Course.sec(day)), style = T.small)
        }
        Spacer(Modifier.height(14.dp))
        if (done) Text(tr("Сегодня занятие пройдено. Следующее – завтра.", "Today's lesson is done. The next one is tomorrow."), style = T.small.copy(color = C.jade))
        else Btn(tr("Начать занятие дня", "Start today's lesson"), Modifier.fillMaxWidth()) { Ui.lesson(steps) }
    }
}

/** The end of the path, full screen: the painting of the seer and the apprentice over the clouds, and her gift. */
@Composable
fun PathEnd(n: Int, close: () -> Unit) {
    androidx.activity.compose.BackHandler(onBack = close)
    val gift = app.secondsight.domain.Master.of(n)
    var shown by androidx.compose.runtime.remember(n) { androidx.compose.runtime.mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(n) { kotlinx.coroutines.delay(300); shown = true }
    val clear by androidx.compose.animation.core.animateFloatAsState(if (shown) 0f else 1f, androidx.compose.animation.core.tween(3500), label = "clear")
    val text by androidx.compose.animation.core.animateFloatAsState(if (shown) 1f else 0f, androidx.compose.animation.core.tween(1500, 2000), label = "text")
    Column(
        Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0xFF080B14)).tap(close)
            .statusBarsPadding().navigationBarsPadding().padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(12.dp))
        Text(tr("Путь ученика пройден", "The apprentice's path is walked").uppercase(), style = T.caps.copy(fontSize = 14.sp, color = C.amber))
        if (n > 1) Text(tr("в ${n}-й раз", "for the ${n}th time"), style = T.tiny)
        Spacer(Modifier.weight(1f))
        Orb(300.dp, clear, picture = "v_path_end", pictureAlpha = 1f, glow = 1.2f)
        Spacer(Modifier.height(22.dp))
        Column(Modifier.alpha(text), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(tr("Двадцать один день позади. Теперь твои глаза умеют отдыхать, смотреть вдаль и видеть широко.",
                "Twenty-one days behind you. Your eyes now know how to rest, to look far and to see wide."), style = T.tale, textAlign = TextAlign.Center)
            Spacer(Modifier.height(18.dp))
            Pic(gift.art, Modifier.size(110.dp))
            Text(gift.title, style = T.h3.copy(color = C.amber), textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text(gift.say, style = T.said.copy(color = C.mute), textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(tr("ясность +${app.secondsight.data.Game.COURSE_CLARITY}", "clarity +${app.secondsight.data.Game.COURSE_CLARITY}"), style = T.tiny.copy(color = C.amber))
        }
        Spacer(Modifier.weight(1f))
        Text(tr("Коснись, чтобы закрыть", "Tap to close"), Modifier.alpha(text * 0.7f), style = T.tiny)
    }
}
