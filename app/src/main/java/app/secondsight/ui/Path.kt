package app.secondsight.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import app.secondsight.container
import app.secondsight.data.State
import app.secondsight.domain.Herb
import app.secondsight.domain.Practice
import app.secondsight.domain.Village
import app.secondsight.domain.bi
import app.secondsight.domain.count
import app.secondsight.domain.tr
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/** The light around the phone in lux, or null without a sensor. */
suspend fun lux(c: Context): Float? {
    val sm = c.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    val sensor = sm.getDefaultSensor(Sensor.TYPE_LIGHT) ?: return null
    return withTimeoutOrNull(2500) {
        suspendCancellableCoroutine<Float> { k ->
            val l = object : SensorEventListener {
                override fun onSensorChanged(e: SensorEvent) { sm.unregisterListener(this); if (k.isActive) k.resume(e.values[0]) }
                override fun onAccuracyChanged(s: Sensor?, a: Int) {}
            }
            sm.registerListener(l, sensor, SensorManager.SENSOR_DELAY_NORMAL)
            k.invokeOnCancellation { sm.unregisterListener(l) }
        }
    }
}

/** The stairway up to the pavilion: the pilgrim with a question, the herbs gathered in daylight, the gifts left. */
@Composable
fun Path(s: State) {
    val ctx = LocalContext.current
    val game = ctx.container.game
    var answer by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    var answered by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 22.dp, vertical = 12.dp)) {
        Header(tr("Тропа", "The path"), tr("Паломники поднимаются к обители со своими вопросами", "Pilgrims climb up to the pavilion with their questions"))

        val guest = Village.of(s.guest)
        if (guest != null) {
            val ask = guest.asks[s.answered[guest.id] ?: 0]
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Face(guest.id, 110.dp)
                Spacer(Modifier.height(10.dp))
                Text(guest.name, style = T.title)
                Text(tr("вопрос ${(s.answered[guest.id] ?: 0) + 1} из ${guest.asks.size}", "question ${(s.answered[guest.id] ?: 0) + 1} of ${guest.asks.size}"), style = T.tiny)
                Spacer(Modifier.height(10.dp))
                Text("«${bi(ask.q)}»", style = T.tale, textAlign = TextAlign.Center)
                Spacer(Modifier.height(14.dp))
                Text(tr("Чтобы шар ответил", "For the orb to answer").uppercase(), style = T.caps.copy(fontSize = 13.sp, color = C.mute))
                Spacer(Modifier.height(8.dp))
                Card(pad = 12.dp) { game.progress(s).forEach { (need, n) ->
                    val p = Practice.of(need.what)
                    val full = n >= need.n
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).tap { p?.let { Ui.start(it) } ?: run { if (need.what == "sun") Ui.toast = tr("Выйди на дневной свет: ниже есть прогулка за травами.", "Go out into daylight: there's a herb walk below.") } }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Pic(p?.icon ?: if (need.what == "sun") "ic_sun" else "ic_basket", Modifier.size(38.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(need.label, Modifier.weight(1f), style = T.body)
                                Text("$n/${need.n}", style = T.tiny.copy(color = if (full) C.jade else C.amber))
                            }
                            Spacer(Modifier.height(4.dp))
                            Thread(n.toFloat() / need.n, Modifier.fillMaxWidth(), if (full) C.jade else C.amber)
                        }
                        Spacer(Modifier.width(12.dp))
                        if (full) Text("✓", style = T.h3.copy(color = C.jade)) else if (p != null) Btn(tr("начать", "start"), small = true) { Ui.start(p) }
                        else Spacer(Modifier.width(22.dp))
                    }
                } }
                Spacer(Modifier.height(16.dp))
                if (game.ready(s)) Lantern(tr("заглянуть в шар", "look into the orb")) { answered = guest.id; answer = game.answer() }
                else Text(tr("Когда всё будет сделано, шар ответит.", "When it's all done, the orb will answer."), style = T.said.copy(color = C.mute), textAlign = TextAlign.Center)
            }
        } else {
            Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Pic("tab_path", Modifier.size(90.dp))
                Spacer(Modifier.height(8.dp))
                Text(
                    if (Village.all.all { (s.answered[it.id] ?: 0) >= it.asks.size }) tr("Все паломники получили ответы. Тропа отдыхает.", "Every pilgrim has an answer. The path rests.")
                    else tr("Сегодня на тропе тихо. Завтра кто-нибудь поднимется.", "The path is quiet today. Someone will climb up tomorrow."),
                    style = T.said, textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(Modifier.height(28.dp))
        Herbs(s)

        val gifts = game.gifts
        Spacer(Modifier.height(20.dp))
        Label(tr("Дары паломников", "Gifts from pilgrims"))
        Spacer(Modifier.height(8.dp))
        Village.all.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { v ->
                    val has = v in gifts
                    Column(Modifier.weight(1f).padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Pic("gift_${v.id}", Modifier.size(58.dp), shadow = !has)
                        Text(if (has) v.gift else "?", style = T.tiny, textAlign = TextAlign.Center, minLines = 2)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(12.dp))
        Label(tr("Дары наставницы", "The seer's gifts"))
        Text(tr("За каждый пройденный путь ученика", "One for every apprentice's path walked"), style = T.tiny)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            app.secondsight.domain.Master.entries.forEachIndexed { i, m ->
                val has = s.courses > i
                Column(Modifier.weight(1f).padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Pic(m.art, Modifier.size(58.dp), shadow = !has)
                    Text(if (has) m.title else "?", style = T.tiny, textAlign = TextAlign.Center, minLines = 2)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Label(tr("Кто уже приходил", "Who has come"))
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Village.all.forEach { v -> Face(v.id, 36.dp, dim = (s.answered[v.id] ?: 0) == 0 && s.guest != v.id) }
        }
        Spacer(Modifier.height(24.dp))
    }
    Sheet(answer != null, { answer = null }) {
        val a = answer ?: return@Sheet
        val v = Village.of(answered)
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Orb(170.dp, 0f, glimpses = 6)
            Spacer(Modifier.height(12.dp))
            Text(bi(a.first), style = T.tale, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            if (v != null) Text(tr("${v.name} кланяется и уходит вниз по тропе.", "${v.name} bows and goes back down the path."), style = T.small, textAlign = TextAlign.Center)
            if (a.second && v != null) {
                Spacer(Modifier.height(12.dp))
                Pic("gift_${v.id}", Modifier.size(90.dp))
                Text(tr("Подарок на память: ", "They left a gift: ") + v.gift, style = T.smallB.copy(color = C.amber), textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(16.dp))
            Btn(tr("Хорошо", "Good"), Modifier.fillMaxWidth()) { answer = null }
        }
    }
}

@Composable
private fun Herbs(s: State) {
    val ctx = LocalContext.current
    val game = ctx.container.game
    val scope = rememberCoroutineScope()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(s.walkSince) { while (s.walkSince > 0) { now = System.currentTimeMillis(); delay(15_000) } }
    Label(tr("Травы к чаю", "Herbs for tea"))
    Spacer(Modifier.height(4.dp))
    Text(tr("Каждые ${Herb.MINUTES} минут на дневном свету – новый пучок. Дневной свет очень полезен глазам, особенно детским.",
        "Every ${Herb.MINUTES} minutes of daylight makes a bunch. Outdoor light is the best medicine for the eyes, especially for children."), style = T.tiny)
    Spacer(Modifier.height(8.dp))
    Card {
        Herb.entries.chunked(6).forEach { row ->
            Row {
                row.forEach { h ->
                    val n = s.herbs[h.id] ?: 0
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box { Pic(h.art, Modifier.size(44.dp), shadow = n == 0); if (n > 0) Text("$n", Modifier.align(Alignment.BottomEnd), style = T.tiny.copy(color = C.amber)) }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(tr("Сейчас растёт: ", "In season now: ") + Herb.season(java.time.LocalDate.now().monthValue).joinToString { it.title }, style = T.tiny)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(tr("до следующего пучка", "until the next bunch"), Modifier.weight(1f), style = T.tiny)
            Text(tr("${Herb.MINUTES - s.sunLeft} мин на свету", "${Herb.MINUTES - s.sunLeft} min of daylight"), style = T.tiny.copy(color = C.jade))
        }
        Spacer(Modifier.height(4.dp))
        Thread(s.sunLeft.toFloat() / Herb.MINUTES, Modifier.fillMaxWidth(), C.jade)
        Spacer(Modifier.height(14.dp))
        if (s.walkSince > 0) {
            val min = ((now - s.walkSince) / 60_000).toInt()
            Text(tr("Прогулка идёт: ", "Walking: ") + count(min, "минута", "минуты", "минут", "minute", "minutes"), style = T.bodyB.copy(color = C.jade))
            Spacer(Modifier.height(8.dp))
            Btn(tr(if (s.female) "Вернулась домой" else "Вернулся домой", "Back home"), Modifier.fillMaxWidth()) {
                val m = ((System.currentTimeMillis() - s.walkSince) / 60_000).toInt().coerceAtMost(180)
                game.write { it.copy(walkSince = 0) }
                val got = game.daylight(m)
                Ui.toast = if (got.isEmpty()) tr("Минуты на свету записаны", "Daylight minutes saved") else tr("Собрано: ", "Picked: ") + got.groupBy { it }.entries.joinToString { "${it.key.title} ×${it.value.size}" }
            }
        } else {
            Btn(tr("Иду гулять за травами", "Going out for herbs"), Modifier.fillMaxWidth(), main = false, icon = "ic_sun") {
                scope.launch {
                    val l = lux(ctx)
                    game.write { it.copy(walkSince = System.currentTimeMillis()) }
                    Ui.toast = if (l != null && l < 400f) tr("Здесь темновато. Травы собираются при дневном свете, на улице.", "It's dim here. Herbs are picked in daylight, outdoors.")
                    else tr("Хорошей прогулки. Нажми, когда вернёшься.", "Enjoy the walk. Tap when you're back.")
                }
            }
        }
    }
}
