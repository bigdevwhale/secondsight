package app.secondsight.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.secondsight.container
import app.secondsight.data.State
import app.secondsight.domain.Practice
import app.secondsight.domain.Tales
import app.secondsight.domain.Village
import app.secondsight.domain.count
import app.secondsight.domain.dayOf
import app.secondsight.domain.today
import app.secondsight.domain.tr

/** How misted the orb is, 0..1. */
fun State.mist() = (strain.toFloat() / (interval * 60)).coerceIn(0f, 1f)

fun State.visionToday() = log.any { it.what.startsWith("vision:") && dayOf(it.at) == today() }

/**
 * The pavilion, all in the mist: the picture fills the screen and fades into the night; the rank, the seer's words and
 * the time float over it with no frames; the two things to do are two lights to reach for.
 */
@Composable
fun Hall(s: State) {
    val game = LocalContext.current.container.game
    val mist by animateFloatAsState(s.mist(), tween(1500), label = "mist")
    val next = game.nextVision(s)
    val rank = game.rank
    val darkReady = s.glimpses >= Tales.GLIMPSES && !s.visionToday() && next != null
    BoxWithConstraints(Modifier.fillMaxSize().background(C.bg)) {
        val w = maxWidth; val h = maxHeight
        Pic("hall", Modifier.fillMaxSize(), scale = ContentScale.Crop)
        // the night at the top and the mist below, where the words are
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
            0f to C.bg.copy(alpha = 0.93f), 0.26f to C.bg.copy(alpha = 0f), 0.6f to C.bg.copy(alpha = 0f), 0.76f to C.bg.copy(alpha = 0.82f), 0.9f to C.bg,
        )))

        val orb = (w * 0.56f).coerceAtMost(h * 0.3f)
        // a thin ring of light around the orb: the time left before it mists over, burning down
        val left = 1f - mist
        androidx.compose.foundation.Canvas(Modifier.align(Alignment.TopCenter).offset(y = h * 0.735f - orb * 0.93f - orb * 0.06f).size(orb * 1.12f)) {
            val sw = 2.dp.toPx()
            drawCircle(C.line.copy(alpha = 0.35f), size.minDimension / 2 - sw, style = androidx.compose.ui.graphics.drawscope.Stroke(sw / 2))
            drawArc(
                Brush.sweepGradient(listOf(C.amber.copy(alpha = 0.15f), C.amber.copy(alpha = 0.85f), C.amber.copy(alpha = 0.15f))),
                -90f, 360f * left, false,
                topLeft = androidx.compose.ui.geometry.Offset(sw, sw), size = androidx.compose.ui.geometry.Size(size.width - 2 * sw, size.height - 2 * sw),
                style = androidx.compose.ui.graphics.drawscope.Stroke(sw, cap = androidx.compose.ui.graphics.StrokeCap.Round),
            )
        }
        Orb(
            orb, mist,
            Modifier.align(Alignment.TopCenter).offset(y = h * 0.735f - orb * 0.93f).tap { Ui.start(if (darkReady) Practice.DARK else Practice.FAR) },
            picture = next?.let { it.tale.art(it.i) },
            pictureAlpha = (s.glimpses.coerceAtMost(Tales.GLIMPSES) / Tales.GLIMPSES.toFloat()) * 0.55f,
            glimpses = s.glimpses,
        )

        Column(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 30.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.fillMaxWidth()) {
                Column(Modifier.align(Alignment.Center).padding(top = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(rank.title.uppercase(), style = T.caps.copy(fontSize = 16.sp, color = C.ink.copy(alpha = 0.85f)))
                    rank.next?.let {
                        Spacer(Modifier.height(6.dp))
                        Thread((s.clarity - rank.from).toFloat() / (it.from - rank.from), Modifier.width(120.dp))
                        Spacer(Modifier.height(3.dp))
                        Text(tr("ясность ${s.clarity} · до звания «${it.title}» ${it.from - s.clarity}", "clarity ${s.clarity} · ${it.from - s.clarity} to ${it.title}"), style = T.tiny.copy(color = C.faint))
                    }
                }
                Icon(Icons.Outlined.Settings, tr("Настройки", "Settings"), Modifier.align(Alignment.CenterEnd).clip(CircleShape).tap { Ui.settings = true }.padding(10.dp).size(22.dp), tint = C.faint)
            }
            Spacer(Modifier.height(18.dp))
            Text(seerSays(s, game.ready(s)), style = T.said, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Rule(Modifier.width(70.dp))
        }

        // the pilgrim waits by the window
        val guest = Village.of(s.guest)
        if (guest != null) Column(
            Modifier.align(Alignment.TopEnd).offset(y = h * 0.33f).padding(end = 6.dp).width(84.dp).clip(RoundedCornerShape(16.dp)).tap { Ui.tab = 3 },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Face(guest.id, 54.dp)
            Text(guest.name, style = T.tiny.copy(color = C.ink.copy(alpha = 0.8f)))
            Text(if (game.ready(s)) tr("ждёт ответа", "awaits") else tr("с вопросом", "has a question"), style = T.tiny.copy(color = if (game.ready(s)) C.amber else C.mute), textAlign = TextAlign.Center)
        }

        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(status(s), style = T.title.copy(fontWeight = FontWeight.Light), textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            if (next == null) Text(tr("все сказки досмотрены", "every tale is seen"), style = T.tiny)
            else Row(verticalAlignment = Alignment.CenterVertically) {
                Glimmers(s.glimpses.coerceAtMost(Tales.GLIMPSES), Tales.GLIMPSES, size = 18.dp)
                Spacer(Modifier.width(8.dp))
                Text(tr("до видения", "to a vision"), style = T.tiny)
            }
            if (game.lessonDone(s)) Text(tr("сегодня вдаль: ${game.count("far")}", "looked far today: ${game.count("far")}"), style = T.tiny.copy(color = C.faint))
            // the day's lesson of the apprentice's path, until it's done
            if (!game.lessonDone(s)) {
                val day = game.lessonNo(s)
                Spacer(Modifier.height(6.dp))
                Btn(tr("занятие дня $day/${app.secondsight.domain.Course.DAYS} · ", "lesson $day/${app.secondsight.domain.Course.DAYS} · ") + app.secondsight.domain.dur(app.secondsight.domain.Course.sec(day)),
                    main = false, small = true, icon = "ic_scroll") { Ui.lesson(app.secondsight.domain.Course.lesson(day)) }
            }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(56.dp)) {
                Lantern(tr("вдаль", "far"), size = 52.dp, warm = !darkReady || mist >= 0.5f) { Ui.start(Practice.FAR) }
                Lantern(tr("тьма", "darkness"), size = 52.dp, warm = darkReady && mist < 0.5f) { Ui.start(Practice.DARK) }
            }
        }
    }
}

private fun status(s: State): String {
    val left = (s.interval * 60 - s.strain) / 60
    val main = when {
        s.strain >= s.interval * 60 -> tr("шар затуманился", "the orb has misted")
        left <= 1 -> tr("вот-вот затуманится", "about to mist over")
        else -> tr("вдаль через ", "look far in ") + count(left, "минуту", "минуты", "минут", "minute", "minutes")
    }
    return if (s.desk) tr("за компьютером · ", "at the desk · ") + main else main
}

private fun seerSays(s: State, ready: Boolean): String = when {
    s.strain >= s.interval * 60 -> tr("Шар затуманился. Подойди к окну и найди самую дальнюю точку. Двадцати секунд хватит.",
        "The orb has misted over. Go to the window and find the farthest point. Twenty seconds is enough.")
    ready -> tr("Паломник ждёт ответа, и шар готов его дать.", "A pilgrim is waiting, and the orb is ready to answer.")
    s.glimpses >= Tales.GLIMPSES && s.visionToday() -> tr("Сегодняшнее видение ты уже видел${if (s.female) "а" else ""}. Следующее – завтра.",
        "You've seen today's vision. The next one comes tomorrow.")
    s.glimpses >= Tales.GLIMPSES -> tr("Проблесков довольно. Закрой глаза ладонями – и шар покажет видение.",
        "There are enough glimpses. Cover your eyes with your palms, and the orb will show a vision.")
    s.visions.isEmpty() && s.log.isEmpty() -> tr("Когда глазам пора отдохнуть, шар затуманится. Я позову.",
        "When your eyes need a rest, the orb will mist over. I'll call you.")
    else -> tr("Шар чист. Береги глаза – и он покажет больше.", "The orb is clear. Look after your eyes, and it will show you more.")
}
