package app.secondsight.ui

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.secondsight.container
import app.secondsight.data.Outcome
import app.secondsight.domain.Moves
import app.secondsight.domain.Practice
import app.secondsight.domain.Tales
import app.secondsight.domain.bi
import app.secondsight.domain.count
import app.secondsight.domain.g
import app.secondsight.domain.tr
import app.secondsight.svc.Cues
import app.secondsight.svc.Foreground
import app.secondsight.svc.Watch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private enum class Phase { INTRO, RUN, DONE }

/** Seconds before the next step of a ritual or lesson starts by itself. */
private const val AUTO = 6

/** Plays [list] one after another: a short word on what and why, the practice itself, and what it brought. */
@Composable
fun Player(list: List<Practice>, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val game = ctx.container.game
    val s = game.now
    var idx by remember(list) { mutableIntStateOf(0) }
    val p = list[idx]
    // looking far is asked for many times a day: it starts at once
    var phase by remember(list, idx) { mutableStateOf(if (p == Practice.FAR) Phase.RUN else Phase.INTRO) }
    var out by remember(list, idx) { mutableStateOf<Outcome?>(null) }
    var darkMin by remember { mutableIntStateOf(2) }
    val secs = remember(list) { Ui.secs }
    val lesson = remember(list) { Ui.lesson }
    val sec = secs?.getOrNull(idx)
    var lessonRes by remember(list) { mutableStateOf<Pair<Int, Boolean>?>(null) }
    val view = LocalView.current
    DisposableEffect(Unit) {
        Foreground.practising = true; view.keepScreenOn = true
        Watch.cancelAsk(ctx); Cues.init(ctx)
        onDispose { Foreground.practising = false; view.keepScreenOn = false; Cues.hush() }
    }
    BackHandler { onClose() }
    val finish: () -> Unit = {
        out = game.done(p)
        if (lesson && idx == list.size - 1) {
            lessonRes = game.finishLesson()
            lessonRes?.let { (n, end) -> if (out?.vision != null) Ui.toast = lessonLine(n, end) }
            if (lessonRes?.second == true) Ui.pathEnd = game.now.courses
        }
        if (s.sound) Cues.ring()
        Cues.buzz(ctx, 90, twice = true)
        val v = out?.vision
        if (v != null) { Ui.vision = v; if (idx < list.size - 1) idx++ else onClose() } else phase = Phase.DONE
    }
    Box(Modifier.fillMaxSize().background(Color(0xFF080B14)).pointerInput(Unit) { detectTapGestures { } }) {
        when (phase) {
            Phase.INTRO -> Intro(p, idx, list.size, darkMin, { darkMin = it }, sec, onClose) { phase = Phase.RUN }
            Phase.RUN -> Run(p, sec ?: if (p == Practice.DARK) darkMin * 60 else p.sec, s.voice, s.sound, onClose, finish)
            Phase.DONE -> Done(out!!, if (idx < list.size - 1) list[idx + 1] else null, lesson, lessonRes, onClose) { idx++ }
        }
    }
}

@Composable
private fun Intro(p: Practice, i: Int, n: Int, darkMin: Int, setDark: (Int) -> Unit, sec: Int?, close: () -> Unit, start: () -> Unit) {
    // after the first step of a sequence the seer reads the how aloud and the step begins by itself
    val auto = n > 1 && i > 0
    var left by remember(p, i) { mutableIntStateOf(if (auto) 12 else 0) }
    val voice = LocalContext.current.container.game.now.voice
    if (auto) LaunchedEffect(p, i) {
        if (voice) Cues.say(how(p))
        while (left > 0) { kotlinx.coroutines.delay(1000); left-- }
        start()
    }
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth()) {
            if (n > 1) Glimmers(i + 1, n, Modifier.padding(top = 10.dp), size = 18.dp)
            Spacer(Modifier.weight(1f))
            Link(tr("закрыть", "close"), color = C.mute, onClick = close)
        }
        Spacer(Modifier.weight(1f))
        Pic(p.icon, Modifier.size(150.dp))
        Spacer(Modifier.height(18.dp))
        Text(p.title, style = T.big, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        val len = app.secondsight.domain.dur(sec ?: if (p == Practice.DARK) darkMin * 60 else p.sec)
        Text(
            len + if (p.glimpses > 0) tr(" · проблеск в шар", " · a glimpse for the orb") else "",
            style = T.small.copy(color = C.amber.copy(alpha = 0.9f)),
        )
        Spacer(Modifier.height(12.dp))
        Text(p.why, style = T.small, textAlign = TextAlign.Center)
        Spacer(Modifier.height(14.dp))
        Rule(Modifier.width(60.dp))
        Spacer(Modifier.height(14.dp))
        Text(how(p), style = T.said, textAlign = TextAlign.Center)
        if (p == Practice.DARK && sec == null) {
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1, 2, 3, 5).forEach { m ->
                    Btn(tr("$m мин", "$m min"), main = m == darkMin, small = true) { setDark(m) }
                }
            }
        }
        Spacer(Modifier.weight(1.2f))
        if (auto) Text(tr("Начнётся само через $left с", "Starts by itself in $left s"), style = T.tiny)
        Btn(if (p == Practice.CANDLE) tr("Погасить", "Put it out") else tr("Начать", "Begin"), Modifier.fillMaxWidth(), onClick = start)
    }
}

private fun how(p: Practice) = when (p) {
    Practice.FAR -> tr("Подойди к окну. Звон колокольчика скажет, когда можно вернуться.", "Go to a window. A bell will tell you when to come back.")
    Practice.TRACK -> tr("Голова неподвижна, двигаются только глаза. Голос скажет, куда смотреть: до упора вверх, вниз, в стороны и по диагоналям, потом круги. На экран смотреть не нужно.", "Keep your head still and move only your eyes. The voice says where: as far up, down, sideways and corner-wise as they go, then circles. No need to watch the screen.")
    Practice.NEARFAR -> tr("Подними палец сантиметрах в двадцати от лица. Горит фонарь – смотри на палец, загорелась звезда – смотри вдаль. Телефон коротко завибрирует, когда пора сменить взгляд.", "Raise a finger about 20 cm from your face. Lantern lit: look at the finger; star lit: look far away. The phone buzzes when it's time to switch.")
    Practice.JUMP -> tr("Представь фонарь у каждого края. По щелчку переводи взгляд на другой фонарь – до упора, голова неподвижна.", "Imagine a lantern at each edge. On each click, jump your gaze to the other one, all the way, head still.")
    Practice.BLINK -> tr("Моргай мягко, когда падает капля. Потом зажмурься, когда попросят.", "Blink softly each time a drop falls. Then squeeze your eyes shut when asked.")
    Practice.THIRD -> tr("Вытяни руку с телефоном. Когда огонёк растёт, медленно подноси телефон к переносице, пока он не начнёт двоиться. Когда огонёк уменьшается, отводи руку.", "Hold the phone at arm's length. While the light grows, slowly bring it toward your nose until it starts to double. While it shrinks, move it away.")
    Practice.SIDE -> tr("Смотри только на точку в центре. Заметишь вспышку краем глаза – коснись экрана.", "Look only at the dot in the middle. When you notice a flash out of the corner of your eye, tap the screen.")
    Practice.DARK -> tr("Сядь удобно, разотри ладони до тепла и накрой ими закрытые глаза. Пальцы на лбу, на глаза не давить. Колокольчик позовёт обратно.", "Sit comfortably, rub your palms warm and cup them over your closed eyes, fingers on the forehead, no pressure. A bell will call you back.")
    Practice.HANDS -> tr("Шесть движений по 10–20 секунд. Телефон завибрирует, когда пора переходить к следующему.", "Six moves of 10–20 seconds each. The phone buzzes when it's time for the next.")
    Practice.BACK -> tr("Шесть движений по 10–20 секунд. Двигайся медленно, без боли.", "Six moves of 10–20 seconds. Move slowly, never into pain.")
    Practice.BREATH -> tr("Вдох, пока круг растёт, выдох, пока он тает. Выдох длиннее вдоха.", "Breathe in while the circle grows, out while it fades. The out-breath is the longer one.")
    Practice.CANDLE -> tr("Подержи палец на фонаре, пока он не погаснет. И отложи телефон до утра.", "Hold your finger on the lantern until it goes out. Then put the phone away until morning.")
}

/** Seconds since this composition started, ticking every frame. */
@Composable
private fun elapsed(): Float {
    var t by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val t0 = withFrameNanos { it }
        while (true) withFrameNanos { t = (it - t0) / 1e9f }
    }
    return t
}

@Composable
private fun Run(p: Practice, sec: Int, voice: Boolean, sound: Boolean, close: () -> Unit, done: () -> Unit) {
    val total = sec.toFloat()
    val t = elapsed()
    // the practices with a fixed course of parts go round again when a lesson makes them longer
    val loop = t % p.sec
    val ctx = LocalContext.current
    var ended by remember { mutableStateOf(false) }
    val over = t >= total
    LaunchedEffect(over) { if (over && p != Practice.CANDLE && !ended) { ended = true; done() } }
    LaunchedEffect(Unit) {
        if (!voice) return@LaunchedEffect
        when (p) {
            Practice.FAR -> Cues.say(tr("Посмотри вдаль.", "Look far away."))
            Practice.DARK -> Cues.say(tr("Закрой глаза и накрой их ладонями.", "Close your eyes and cover them with your palms."))
            else -> {}
        }
    }
    Box(Modifier.fillMaxSize()) {
        when (p) {
            Practice.FAR -> Far(t, total)
            Practice.DARK -> Dark(t, total)
            Practice.TRACK -> Gaze(loop, eight(), tr("Восемь сторон", "Eight directions"))
            Practice.NEARFAR -> NearFar(t)
            Practice.JUMP -> Gaze(loop, lanterns(), tr("Два фонаря", "Two lanterns"))
            Practice.BLINK -> Blink(loop)
            Practice.THIRD -> Third(t)
            Practice.SIDE -> Side(t, total)
            Practice.HANDS -> Moves(t, Moves.hands, "ic_hands")
            Practice.BACK -> Moves(t, Moves.back, "ic_back")
            Practice.BREATH -> Breath(t)
            Practice.CANDLE -> Candle { if (!ended) { ended = true; done() } }
        }
        // the time left: a thin thread along the bottom, and a way out
        if (p != Practice.CANDLE) {
            Column(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(20.dp).alpha(if (p == Practice.DARK || p == Practice.FAR) 0.35f else 0.8f), horizontalAlignment = Alignment.CenterHorizontally) {
                Thread(t / total, Modifier.fillMaxWidth(0.6f), C.amber.copy(alpha = 0.7f))
                Spacer(Modifier.height(6.dp))
                Link(tr("Прервать", "Stop"), color = C.faint) { close() }
            }
        }
    }
    // every change of what to do gets a signal the eyes don't need: a buzz, a soft bell for the big changes, and the seer's word
    val ph = phaseOf(p, loop)
    if (ph != null) {
        val key = (t / p.sec).toInt() * 1000 + ph.first
        LaunchedEffect(key) {
            val gaze = p == Practice.TRACK || p == Practice.JUMP
            if (key == 0) { if (voice && gaze) ph.second?.let { Cues.say(it) }; return@LaunchedEffect }
            val small = p == Practice.NEARFAR || p == Practice.THIRD || p == Practice.BREATH ||
                gaze && !(segsOf(p).getOrNull(ph.first)?.strong ?: false)
            Cues.buzz(ctx, if (small) 40 else 80, twice = !small)
            if (sound) { if (small) Cues.tick() else Cues.ring(soft = true) }
            if (voice) ph.second?.let { Cues.say(it) }
        }
    }
}

/** One part of an exercise of the gaze: where to look (dx, dy to the very edge), or round and round ([circle] = +1 or -1). */
private data class Seg(val dur: Float, val say: String?, val dx: Float = 0f, val dy: Float = 0f, val circle: Int = 0, val strong: Boolean = true)

private const val D = 0.7071f

/** Eight directions: to the very edge and back to the centre, eight ways; then slow circles; then rest. 60 s. */
private fun eight(): List<Seg> {
    val dirs = listOf(
        Triple(tr("Вверх, до упора.", "Up, as far as it goes."), 0f, -1f), Triple(tr("Вниз, до упора.", "Down, all the way."), 0f, 1f),
        Triple(tr("Влево, до упора.", "Left, all the way."), -1f, 0f), Triple(tr("Вправо, до упора.", "Right, all the way."), 1f, 0f),
        Triple(tr("Вверх и влево.", "Up and left."), -D, -D), Triple(tr("Вниз и вправо.", "Down and right."), D, D),
        Triple(tr("Вверх и вправо.", "Up and right."), D, -D), Triple(tr("Вниз и влево.", "Down and left."), -D, D),
    )
    return dirs.flatMap { (say, x, y) -> listOf(Seg(2.5f, say, x, y), Seg(1.5f, tr("Центр.", "Centre."), strong = false)) } + listOf(
        Seg(12f, tr("Медленный круг по краю, по часовой.", "A slow circle round the edge, clockwise."), circle = 1),
        Seg(12f, tr("Теперь против часовой.", "Now the other way."), circle = -1),
        Seg(4f, tr("Закрой глаза и отдохни.", "Close your eyes and rest.")),
    )
}

/** Two lanterns: one at each edge; the gaze jumps from one to the other, all the way, on each click. 45 s. */
private fun lanterns(): List<Seg> {
    fun mode(say: String, a: Pair<Float, Float>, b: Pair<Float, Float>) =
        listOf(Seg(3f, say)) + (0 until 8).map { i -> val q = if (i % 2 == 0) a else b; Seg(1.5f, null, q.first, q.second, strong = false) }
    return mode(tr("Влево и вправо до упора, по щелчку.", "Left and right, all the way, on each click."), -1f to 0f, 1f to 0f) +
        mode(tr("Вверх и вниз, по щелчку.", "Up and down, on each click."), 0f to -1f, 0f to 1f) +
        mode(tr("Из угла в угол, по щелчку.", "Corner to corner, on each click."), -D to -D, D to D)
}

private fun segsOf(p: Practice) = if (p == Practice.TRACK) eight() else lanterns()

/** The part on at [t], and the seconds since it began. */
private fun segAt(list: List<Seg>, t: Float): Triple<Int, Seg, Float> {
    var acc = 0f
    for ((i, x) in list.withIndex()) { if (t < acc + x.dur) return Triple(i, x, t - acc); acc += x.dur }
    return Triple(list.lastIndex, list.last(), list.last().dur)
}

/** Which part of a practice is on at [t] and what to say when it starts: the cue when the eyes are closed or far away. */
private fun phaseOf(p: Practice, t: Float): Pair<Int, String?>? = when (p) {
    Practice.BLINK -> when {
        t < 15f -> 0 to tr("Моргай, когда падает капля.", "Blink as each drop falls.")
        t < 19f -> 1 to tr("Зажмурься покрепче.", "Squeeze your eyes shut.")
        t < 23f -> 2 to tr("Открой глаза.", "Open your eyes.")
        else -> 3 to tr("Поморгай часто-часто.", "Blink fast and light.")
    }
    Practice.TRACK, Practice.JUMP -> segAt(segsOf(p), t).let { (i, seg, _) -> i to seg.say }
    Practice.NEARFAR -> ((t / 5f).toInt() % 2).let { it to if (it == 0) tr("На палец.", "The finger.") else tr("Вдаль.", "Far.") }
    Practice.THIRD -> ((t / 5f).toInt() % 2).let { it to if (it == 0) tr("Ближе.", "Closer.") else tr("Отводи.", "Away.") }
    Practice.BREATH -> (if (t % 10f < 4f) 0 else 1).let { it to if (it == 0) tr("Вдох.", "Breathe in.") else tr("Выдох.", "Breathe out.") }
    Practice.HANDS, Practice.BACK -> {
        val moves = if (p == Practice.HANDS) Moves.hands else Moves.back
        var acc = 0f; var r: Pair<Int, String> = moves.lastIndex to bi(moves.last().first)
        for ((i, m) in moves.withIndex()) { if (t < acc + m.second) { r = i to bi(m.first); break }; acc += m.second }
        r
    }
    else -> null
}

@Composable
private fun Hint(text: String, modifier: Modifier = Modifier, big: Boolean = false) =
    Text(text, modifier.fillMaxWidth().padding(horizontal = 28.dp), style = (if (big) T.title else T.body).copy(color = C.ink), textAlign = TextAlign.Center)

@Composable
private fun Far(t: Float, total: Float) {
    // the words fade after a moment: there is nothing to look at here
    val a by animateFloatAsState(if (t < 3f) 1f else 0.18f, tween(1200), label = "fade")
    Column(Modifier.fillMaxSize().alpha(a), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Pic("ic_far", Modifier.size(180.dp))
        Spacer(Modifier.height(20.dp))
        Hint(tr("Найди за окном самую дальнюю точку", "Find the farthest point outside the window"), big = true)
        Spacer(Modifier.height(10.dp))
        Hint(tr("и смотри на неё. Не на экран.", "and look at it. Not at the screen."))
        Spacer(Modifier.height(24.dp))
        Text("${(total - t).toInt().coerceAtLeast(0)}", style = T.huge.copy(color = C.amber.copy(alpha = 0.8f)))
    }
}

@Composable
private fun Dark(t: Float, total: Float) {
    val ctx = LocalContext.current
    // the screen itself goes as dark as it can
    DisposableEffect(Unit) {
        val w = (ctx as? Activity)?.window
        val before = w?.attributes?.screenBrightness
        w?.attributes = w?.attributes?.apply { screenBrightness = 0.01f }
        onDispose { w?.attributes = w?.attributes?.apply { screenBrightness = before ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE } }
    }
    val a by animateFloatAsState(if (t < 4f) 0.7f else 0f, tween(2000), label = "dark")
    Column(Modifier.fillMaxSize().background(Color.Black).alpha(a), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Hint(tr("Тьма", "Darkness"), big = true)
        Spacer(Modifier.height(8.dp))
        Hint(tr("Глаза закрыты, ладони тёплые. Дыши медленно.", "Eyes closed, palms warm. Breathe slowly."))
    }
}

/** The eyes go to the very edge: a dark orb shows where, the voice says it, a buzz and a click mark each change. */
@Composable
private fun Gaze(t: Float, segs: List<Seg>, title: String) {
    val (i, seg, u) = segAt(segs, t)
    val said = segs.subList(0, i + 1).lastOrNull { it.say != null && it.strong }?.say ?: title
    Column(Modifier.fillMaxSize().statusBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(40.dp))
        Hint(said.trimEnd('.'), big = true)
        Spacer(Modifier.height(4.dp))
        Hint(tr("Голова неподвижна. Можно смотреть мимо экрана.", "Keep your head still. You may look past the screen."))
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Orb(300.dp, 0f, glow = 0.5f)
            Canvas(Modifier.size(300.dp)) {
                val r = size.minDimension * 0.42f
                val (x, y) = if (seg.circle != 0) {
                    val a = seg.circle * u / seg.dur * 2f * PI.toFloat() - PI.toFloat() / 2
                    cos(a) to sin(a)
                } else seg.dx to seg.dy
                val c = Offset(center.x + x * r, center.y + y * r)
                if (x != 0f || y != 0f) drawLine(C.amber.copy(alpha = 0.25f), center, c, 2f)
                spark(c, size.minDimension * 0.035f)
            }
        }
    }
}

private fun DrawScope.spark(c: Offset, r: Float, a: Float = 1f) {
    drawCircle(Brush.radialGradient(listOf(C.amber.copy(alpha = 0.9f * a), C.amber.copy(alpha = 0.2f * a), Color.Transparent), c, r * 2.2f), r * 2.2f, c)
    drawCircle(Color(0xFFFFF4DA).copy(alpha = a), r * 0.35f, c)
}

@Composable
private fun NearFar(t: Float) {
    val near = (t / 5f).toInt() % 2 == 0
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(260.dp), contentAlignment = Alignment.Center) {
            val lantern by animateFloatAsState(if (near) 1f else 0.15f, tween(500), label = "l")
            val star by animateFloatAsState(if (near) 0.15f else 1f, tween(500), label = "s")
            Canvas(Modifier.size(260.dp)) {
                spark(Offset(size.width * 0.5f, size.height * 0.62f), size.minDimension * 0.16f, lantern)
                spark(Offset(size.width * 0.72f, size.height * 0.18f), size.minDimension * 0.03f, star)
            }
        }
        Spacer(Modifier.height(16.dp))
        Hint(if (near) tr("Фонарь: смотри на палец", "Lantern: look at your finger") else tr("Звезда: смотри вдаль", "Star: look far away"), big = true)
    }
}

@Composable
private fun Blink(t: Float) {
    val (text, drop) = when {
        t < 15f -> tr("Моргни, когда упадёт капля", "Blink as each drop falls") to ((t % 2.5f) / 2.5f)
        t < 19f -> tr("Зажмурься покрепче", "Squeeze your eyes shut") to -1f
        t < 23f -> tr("Открой глаза", "Open your eyes") to -1f
        else -> tr("Поморгай часто-часто", "Blink fast and light") to ((t % 0.6f) / 0.6f)
    }
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Canvas(Modifier.size(220.dp)) {
            if (drop >= 0f) {
                val y = size.height * (0.1f + 0.7f * drop * drop)
                drawCircle(Brush.radialGradient(listOf(C.sky.copy(alpha = 0.9f), C.sky.copy(alpha = 0.1f)), Offset(center.x, y), 18f), 18f, Offset(center.x, y))
                val ripple = (drop - 0.85f).coerceAtLeast(0f) / 0.15f
                if (ripple > 0f) drawCircle(C.sky.copy(alpha = 1 - ripple), 20f + 80f * ripple, Offset(center.x, size.height * 0.82f), style = Stroke(3f))
            } else {
                drawCircle(C.amber.copy(alpha = 0.25f), size.minDimension * 0.3f, center)
            }
        }
        Spacer(Modifier.height(20.dp))
        Hint(text, big = true)
    }
}

@Composable
private fun Third(t: Float) {
    val k = (t % 10f) / 10f
    val grow = k < 0.5f
    val f = if (grow) k * 2 else (1 - k) * 2
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Canvas(Modifier.size(300.dp)) { spark(center, size.minDimension * (0.03f + 0.12f * f)) }
        Spacer(Modifier.height(20.dp))
        Hint(if (grow) tr("Медленно подноси к переносице", "Slowly toward your nose") else tr("Медленно отводи на вытянутую руку", "Slowly back to arm's length"), big = true)
    }
}

/** Flashes at the edges while the eyes stay on the centre; a tap soon after a flash counts. */
@Composable
private fun Side(t: Float, total: Float) {
    val flashes = remember { mutableStateListOf<Pair<Float, Offset>>() }
    var seen by remember { mutableIntStateOf(0) }
    var nextAt by remember { mutableFloatStateOf(2f) }
    if (t >= nextAt && t < total - 2f) {
        val a = Random.nextFloat() * 2 * PI.toFloat()
        flashes += t to Offset(0.5f + cos(a) * 0.44f, 0.5f + sin(a) * 0.44f)
        nextAt = t + 1.6f + Random.nextFloat() * 1.8f
    }
    val last = flashes.lastOrNull()
    Box(Modifier.fillMaxSize().pointerInput(Unit) {
        detectTapGestures { val l = flashes.lastOrNull(); if (l != null && seen < flashes.size && abs(currentT - l.first) < 1.3f) seen = flashes.size }
    }) {
        currentT = t
        Canvas(Modifier.fillMaxSize()) {
            spark(center, size.minDimension * 0.018f)
            if (last != null && t - last.first < 0.45f) {
                val o = Offset(last.second.x * size.width, (0.12f + last.second.y * 0.76f) * size.height)
                spark(o, size.minDimension * 0.05f, 1f - (t - last.first) / 0.45f)
            }
        }
        Column(Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 30.dp)) {
            Hint(tr("Смотри только в центр", "Keep your eyes on the centre"))
            Spacer(Modifier.height(4.dp))
            Text(tr("Замечено: $seen из ${flashes.size}", "Noticed: $seen of ${flashes.size}"), Modifier.fillMaxWidth(), style = T.tiny, textAlign = TextAlign.Center)
        }
    }
}

private var currentT = 0f

@Composable
private fun Moves(t: Float, moves: List<Pair<String, Int>>, icon: String) {
    val ctx = LocalContext.current
    var acc = 0f; var i = moves.lastIndex
    for ((k, m) in moves.withIndex()) { if (t < acc + m.second) { i = k; break }; acc += m.second }
    val f = ((t - acc) / moves[i].second).coerceIn(0f, 1f)
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(30.dp))
        Text(tr("${i + 1} из ${moves.size}", "${i + 1} of ${moves.size}"), style = T.tiny)
        Spacer(Modifier.weight(0.5f))
        Pic(icon, Modifier.size(130.dp))
        Spacer(Modifier.height(24.dp))
        Text(bi(moves[i].first), style = T.tale, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        Thread(f, Modifier.fillMaxWidth(0.5f), C.jade)
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun Breath(t: Float) {
    val k = t % 10f
    val inhale = k < 4f
    val f = if (inhale) k / 4f else 1f - (k - 4f) / 6f
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(contentAlignment = Alignment.Center) {
            Pic("ic_breath", Modifier.size(280.dp), alpha = 0.55f)
            Canvas(Modifier.size(280.dp)) {
                val r = size.minDimension * (0.12f + 0.3f * f)
                drawCircle(Brush.radialGradient(listOf(C.jade.copy(alpha = 0.35f), Color.Transparent), center, r * 1.3f), r * 1.3f)
                drawCircle(C.jade.copy(alpha = 0.8f), r, style = Stroke(3f))
            }
        }
        Spacer(Modifier.height(24.dp))
        Hint(if (inhale) tr("Вдох", "Breathe in") else tr("Выдох", "Breathe out"), big = true)
    }
}

/** Hold the lantern until it goes out. */
@Composable
private fun Candle(done: () -> Unit) {
    var held by remember { mutableStateOf(false) }
    var out by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(held) {
        var last = withFrameNanos { it }
        while (held && out < 1f) withFrameNanos { out = (out + (it - last) / 1e9f / 2.2f).coerceAtMost(1f); last = it }
        if (out >= 1f) { kotlinx.coroutines.delay(1400); done() }
        else while (!held && out > 0f) withFrameNanos { out = (out - (it - last) / 1e9f).coerceAtLeast(0f); last = it }
    }
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(280.dp).pointerInput(Unit) { detectTapGestures(onPress = { held = true; tryAwaitRelease(); held = false }) }, contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val c = Offset(center.x, size.height * 0.55f)
                drawCircle(Brush.radialGradient(listOf(C.amber.copy(alpha = 0.55f * (1 - out)), Color.Transparent), c, size.minDimension * 0.5f), size.minDimension * 0.5f, c)
            }
            Pic("ic_candle", Modifier.size(200.dp))
        }
        Spacer(Modifier.height(20.dp))
        Hint(if (out >= 1f) tr("Доброй ночи", "Good night") else tr("Держи палец на фонаре", "Keep your finger on the lantern"), big = true)
        if (out >= 1f) { Spacer(Modifier.height(8.dp)); Hint(tr("Экран до утра можно не открывать.", "The screen can wait until morning.")) }
    }
}

private fun lessonLine(n: Int, end: Boolean) =
    if (end) tr("Путь ученика пройден до конца! Ясность +${app.secondsight.data.Game.COURSE_CLARITY}.", "The apprentice's path is walked to the end! Clarity +${app.secondsight.data.Game.COURSE_CLARITY}.")
    else tr("Занятие $n из ${app.secondsight.domain.Course.DAYS} пройдено. Следующее – завтра.", "Lesson $n of ${app.secondsight.domain.Course.DAYS} done. The next one is tomorrow.")

@Composable
private fun Done(o: Outcome, next: Practice?, lesson: Boolean, lessonRes: Pair<Int, Boolean>?, close: () -> Unit, goNext: () -> Unit) {
    val game = LocalContext.current.container.game
    val s = game.now
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(0.5f))
        Orb(200.dp, s.mist(), picture = game.nextVision(s)?.let { it.tale.art(it.i) }, pictureAlpha = (s.glimpses.coerceAtMost(Tales.GLIMPSES) / Tales.GLIMPSES.toFloat()) * 0.55f, glimpses = s.glimpses)
        Spacer(Modifier.height(20.dp))
        Text(when (o.p) {
            Practice.FAR -> tr("Туман рассеялся", "The mist has cleared")
            Practice.CANDLE -> tr("Фонарь погас", "The lantern is out")
            Practice.DARK -> tr("Тьма отступила", "The Darkness lifts")
            else -> g(tr("Готово", "Done"), tr("Готово", "Done"))
        }, style = T.big, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        val line = when {
            o.glimpses > 0 -> tr("В шаре новый проблеск. ", "A new glimpse in the orb. ") +
                if (s.glimpses >= Tales.GLIMPSES) tr("Их уже довольно для видения: нужна только Тьма.", "Enough for a vision now: all it needs is the Darkness.")
                else tr("До видения ещё ", "Until the vision: ") + count(Tales.GLIMPSES - s.glimpses, "проблеск", "проблеска", "проблесков", "glimpse", "more glimpses")
            o.tomorrow -> tr("Глазам Тьма пошла на пользу. А видение шар показывает одно в день: следующее будет завтра.", "The Darkness did your eyes good. The orb shows one vision a day: the next comes tomorrow.")
            o.missing > 0 -> tr("Глазам Тьма пошла на пользу, но для видения шару не хватает проблесков: ещё ", "The Darkness did your eyes good, but the orb needs more glimpses for a vision: ") +
                count(o.missing, "проблеск", "проблеска", "проблесков", "more", "more") + "."
            o.p == Practice.CANDLE -> tr("Сон – лучший отдых для глаз. До завтра.", "Sleep is the best rest for the eyes. Until tomorrow.")
            else -> tr("Глаза отдохнули.", "Your eyes have rested.")
        }
        Text(line, style = T.body.copy(color = C.mute), textAlign = TextAlign.Center)
        lessonRes?.let { (n, end) ->
            Spacer(Modifier.height(18.dp))
            Text(tr("путь ученика", "the apprentice's path").uppercase(), style = T.caps.copy(fontSize = 13.sp, color = C.amber))
            Spacer(Modifier.height(6.dp))
            Glimmers(if (end) app.secondsight.domain.Course.DAYS else n, app.secondsight.domain.Course.DAYS, size = 13.dp)
            Spacer(Modifier.height(6.dp))
            Text(lessonLine(n, end), style = T.said, textAlign = TextAlign.Center)
        }
        o.rank?.let {
            Spacer(Modifier.height(16.dp))
            Text(tr("новое звание", "new rank"), style = T.tiny)
            Text(it.title.uppercase(), style = T.caps.copy(fontSize = 20.sp, color = C.amber))
        }
        Spacer(Modifier.weight(1f))
        if (next != null) {
            // in a ritual or a lesson the next one comes by itself: the eyes may still be closed
            var left by remember { mutableIntStateOf(AUTO) }
            LaunchedEffect(Unit) {
                if (s.voice) Cues.say(tr("Дальше – ", "Next: ") + next.title + ".")
                while (left > 0) { kotlinx.coroutines.delay(1000); left-- }
                goNext()
            }
            Text(tr("Следующее начнётся через $left с", "The next one starts in $left s"), style = T.tiny)
            Spacer(Modifier.height(4.dp))
            Btn(tr("Дальше: ", "Next: ") + next.title, Modifier.fillMaxWidth(), icon = next.icon, onClick = goNext)
            Spacer(Modifier.height(8.dp))
            Btn(if (lesson) tr("Прервать занятие", "Stop the lesson") else tr("Хватит на сегодня", "That's enough"), Modifier.fillMaxWidth(), main = false, onClick = close)
        } else Btn(tr("Вернуться", "Back"), Modifier.fillMaxWidth(), onClick = close)
    }
}

/** A vision, full screen: it rises out of the mist, with its line of the tale beneath. */
@Composable
fun VisionView(v: app.secondsight.data.Shown, close: () -> Unit) {
    BackHandler(onBack = close)
    var shown by remember(v) { mutableStateOf(false) }
    LaunchedEffect(v) { kotlinx.coroutines.delay(300); shown = true }
    val clear by animateFloatAsState(if (shown) 0f else 1f, tween(3500), label = "clear")
    val text by animateFloatAsState(if (shown) 1f else 0f, tween(1500, 2200), label = "text")
    Column(Modifier.fillMaxSize().background(Color(0xFF080B14)).tap(close).statusBarsPadding().navigationBarsPadding().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(16.dp))
        Text(v.tale.title, style = T.h3.copy(color = C.amber))
        Text(tr("видение ${v.i + 1} из ${v.tale.visions.size}", "vision ${v.i + 1} of ${v.tale.visions.size}"), style = T.tiny)
        Spacer(Modifier.weight(1f))
        Orb(340.dp, clear, picture = v.tale.art(v.i), pictureAlpha = 1f, glow = 1.2f)
        Spacer(Modifier.height(26.dp))
        Text(v.tale.line(v.i), Modifier.alpha(text), style = T.tale, textAlign = TextAlign.Center)
        Spacer(Modifier.weight(1f))
        Text(tr("Коснись, чтобы закрыть", "Tap to close"), Modifier.alpha(text * 0.7f).clip(RoundedCornerShape(8.dp)), style = T.tiny)
    }
}
