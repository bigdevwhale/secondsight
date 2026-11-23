package app.secondsight.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.secondsight.container
import app.secondsight.data.Shown
import app.secondsight.data.State
import app.secondsight.domain.Tales
import app.secondsight.domain.dayShort
import app.secondsight.domain.today
import app.secondsight.domain.tr
import java.time.LocalDate

/** The scrolls: the tales seen so far, and the record of the days. */
@Composable
fun Scrolls(s: State) {
    var page by remember { mutableIntStateOf(0) }
    val total = Tales.all.sumOf { it.visions.size }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 22.dp, vertical = 12.dp)) {
        Header(tr("Свитки", "Scrolls"), tr("Видений ${s.visions.size} из $total", "${s.visions.size} of $total visions seen"))
        Row(Modifier.offset(x = (-10).dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn(tr("Видения", "Visions"), main = page == 0, small = true) { page = 0 }
            Btn(tr("Записи", "Records"), main = page == 1, small = true) { page = 1 }
        }
        Spacer(Modifier.height(16.dp))
        if (page == 0) Visions(s) else Records(s)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Visions(s: State) {
    val game = LocalContext.current.container.game
    val next = game.nextVision(s)
    if (s.courses > 0) {
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).tap { Ui.pathEnd = s.courses }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Pic("v_path_end", Modifier.size(76.dp).clip(CircleShape), scale = ContentScale.Crop)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(tr("Конец пути", "The end of the path"), style = T.h3)
                Text(tr("Путь ученика пройден: ${s.courses}", "Apprentice's path walked: ${s.courses}×"), style = T.tiny.copy(color = C.jade))
            }
        }
        Spacer(Modifier.height(6.dp))
    }
    Tales.all.forEach { t ->
        val shown = game.shownOf(t, s)
        val current = next?.tale == t
        if (shown == 0 && !current) {
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Orb(40.dp, 1f, glow = 0.3f)
                Spacer(Modifier.width(12.dp))
                Text(tr("Сказка ещё в тумане", "A tale still in the mist"), style = T.said.copy(color = C.faint))
            }
        } else Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(t.title, Modifier.weight(1f), style = T.h3)
                Text(
                    if (shown == t.visions.size) tr("досмотрена", "complete") else tr("$shown из ${t.visions.size}", "$shown of ${t.visions.size}"),
                    style = T.tiny.copy(color = if (shown == t.visions.size) C.jade else C.amber),
                )
            }
            if (current) Text(tr("Для следующего видения: ${Tales.GLIMPSES} проблесков, потом Тьма", "The next vision: ${Tales.GLIMPSES} glimpses, then darkness"), style = T.tiny.copy(color = C.faint))
            Spacer(Modifier.height(10.dp))
            t.visions.indices.chunked(4).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { i ->
                        Box(Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                            if (i < shown) Pic(t.art(i), Modifier.fillMaxSize().clip(CircleShape).tap { Ui.vision = Shown(t, i) }, scale = ContentScale.Crop)
                            else if (current && i == shown) Orb(64.dp, 0.6f, picture = t.art(i), pictureAlpha = s.glimpses.coerceAtMost(Tales.GLIMPSES) / Tales.GLIMPSES.toFloat() * 0.5f, glimpses = s.glimpses, glow = 0.5f)
                            else Orb(52.dp, 1f, glow = 0f)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
        Spacer(Modifier.height(10.dp))
    }
}

/** Two weeks as a row of lanterns: the height is how often the eyes looked far; a green glow under it, daylight. */
@Composable
private fun Records(s: State) {
    val game = LocalContext.current.container.game
    val days = game.days(14, s)
    val maxFar = (days.maxOfOrNull { it.first } ?: 1).coerceAtLeast(8)
    Column(Modifier.fillMaxWidth().silk(18.dp).padding(16.dp)) {
        Text(tr("Две недели", "Two weeks"), style = T.h3)
        Text(tr("Высота фонаря – сколько раз смотрел${if (s.female) "а" else ""} вдаль, зелёная дымка – дневной свет", "Lantern height: times you looked far; the green haze: daylight"), style = T.tiny)
        Spacer(Modifier.height(12.dp))
        Canvas(Modifier.fillMaxWidth().height(150.dp)) {
            val w = size.width / days.size
            days.forEachIndexed { i, (far, _, sun) ->
                val x = w * i + w / 2
                val hgt = size.height * 0.85f * far / maxFar
                val base = size.height * 0.92f
                if (sun > 0) drawCircle(Brush.radialGradient(listOf(C.jade.copy(alpha = (sun / 60f).coerceIn(0.1f, 0.6f)), Color.Transparent), Offset(x, base), w * 0.9f), w * 0.9f, Offset(x, base))
                drawLine(C.line, Offset(x, base), Offset(x, base - hgt), 2f)
                if (far > 0) {
                    val c = Offset(x, base - hgt)
                    drawCircle(Brush.radialGradient(listOf(C.amber.copy(alpha = 0.8f), Color.Transparent), c, w * 0.45f), w * 0.45f, c)
                    drawCircle(C.amber, w * 0.12f, c)
                }
            }
        }
        Row {
            val t = today()
            (13 downTo 0).forEach { b ->
                Text(dayShort(LocalDate.ofEpochDay(t - b).dayOfWeek.value - 1), Modifier.weight(1f), style = T.tiny.copy(fontSize = T.tiny.fontSize * 0.85f), textAlign = TextAlign.Center)
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    val week = days.takeLast(7)
    Column(Modifier.fillMaxWidth().silk(18.dp).padding(16.dp)) {
        Text(tr("За неделю", "This week"), style = T.h3)
        Spacer(Modifier.height(8.dp))
        Line(tr("Взглядов вдаль", "Looks far away"), week.sumOf { it.first }.toString())
        Line(tr("Других практик", "Other practices"), week.sumOf { it.second }.toString())
        Line(tr("Минут при дневном свете", "Minutes of daylight"), week.sumOf { it.third }.toString())
        Line(tr("Видений", "Visions"), s.visions.size.toString())
        Line(tr("Ясность", "Clarity"), s.clarity.toString())
    }
}

@Composable
private fun Line(k: String, v: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(k, Modifier.weight(1f), style = T.body.copy(color = C.mute))
        Text(v, style = T.bodyB.copy(color = C.amber))
    }
}
