package app.secondsight.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.secondsight.container
import app.secondsight.data.State
import app.secondsight.domain.Practice
import app.secondsight.domain.Ritual
import app.secondsight.domain.dur
import app.secondsight.domain.count
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import app.secondsight.domain.tr

/** Every practice, and the ready-made rituals at the top. */
@Composable
fun Practices(s: State) {
    val game = LocalContext.current.container.game
    val done = game.practicesToday(s)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 22.dp, vertical = 12.dp)) {
        Header(tr("Практики", "Practices"), if (done == 0) tr("Сегодня ещё ни одной. Начни с минутки.", "None yet today. Start with a minute.")
            else tr("Сегодня: ", "Today: ") + count(done, "практика", "практики", "практик", "practice", "practices"))
        ApprenticePath(s)
        Spacer(Modifier.height(26.dp))
        Label(tr("Обряды", "Rituals"))
        Spacer(Modifier.height(8.dp))
        Ritual.entries.forEach { r ->
            Card(Modifier.padding(bottom = 10.dp), onClick = { Ui.ritual(r.steps) }, pad = 12.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Pic(when (r) { Ritual.MINUTE -> Practice.FAR.icon; Ritual.MORNING -> "ic_sun"; Ritual.EVENING -> Practice.DARK.icon; else -> Practice.HANDS.icon }, Modifier.size(48.dp))
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(r.title, style = T.h3)
                        Text(r.sub + " · " + dur(r.sec), style = T.tiny)
                    }
                    Chevron()
                }
            }
        }
        Spacer(Modifier.height(22.dp))
        Label(tr("Для глаз", "For the eyes"))
        Spacer(Modifier.height(8.dp))
        Grid(Practice.eyes, s)
        Spacer(Modifier.height(16.dp))
        Label(tr("Для тела", "For the body"))
        Spacer(Modifier.height(8.dp))
        Grid(Practice.body + Practice.CANDLE, s)
        Spacer(Modifier.height(20.dp))
    }
}

/** Three to a row; the length and today's count sit over the name, so the names line up whatever their length. */
@Composable
private fun Grid(list: List<Practice>, s: State) {
    val game = LocalContext.current.container.game
    list.chunked(3).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { p ->
                val n = game.count(p.id, s = s)
                Card(Modifier.weight(1f), onClick = { Ui.start(p) }, pad = 8.dp) {
                    Box(Modifier.size(70.dp).align(Alignment.CenterHorizontally), contentAlignment = Alignment.Center) {
                        Pic(p.icon, Modifier.size(58.dp))
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(p.title, Modifier.fillMaxWidth(), style = T.smallB, textAlign = TextAlign.Center, minLines = 2, maxLines = 2)
                    Text(dur(p.sec) + if (n > 0) tr(" · ✓$n", " · ✓$n") else "", Modifier.fillMaxWidth(), style = T.tiny.copy(color = if (n > 0) C.amber else C.faint), textAlign = TextAlign.Center)
                }
            }
            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
        }
        Spacer(Modifier.height(10.dp))
    }
}
