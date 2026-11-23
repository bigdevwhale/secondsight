package app.secondsight.data

import android.content.Context
import app.secondsight.domain.Herb
import app.secondsight.domain.Course
import app.secondsight.svc.OrbWidget
import app.secondsight.domain.Me
import app.secondsight.domain.Need
import app.secondsight.domain.Practice
import app.secondsight.domain.Rank
import app.secondsight.domain.Tale
import app.secondsight.domain.Tales
import app.secondsight.domain.Village
import app.secondsight.domain.Villager
import app.secondsight.domain.dayOf
import app.secondsight.domain.today
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.time.LocalDate

/** A vision just shown: the tale and its index. */
data class Shown(val tale: Tale, val i: Int)

/** What finishing a practice brought, for the closing card. */
data class Outcome(val p: Practice, val glimpses: Int, val vision: Shown? = null, val missing: Int = 0, val rank: Rank? = null, val tomorrow: Boolean = false)

/**
 * The whole game: one state, saved as JSON after every change, and the rules that change it.
 * Every write goes through [write], which is synchronized, so the watcher service and the screens never clash.
 */
class Game(private val app: Context, private val scope: CoroutineScope) {
    companion object { const val LESSON_CLARITY = 5; const val COURSE_CLARITY = 60 }

    private val file = File(app.filesDir, "secondsight.json")
    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state
    val now get() = _state.value
    /** A vision to open full-screen, once. */
    val revealed = MutableSharedFlow<Shown>(extraBufferCapacity = 4)

    fun load() {
        _state.value = runCatching { State.of(JSONObject(file.readText())) }.getOrElse { State() }
        Me.f = now.female
        arrive()
    }

    @Synchronized
    fun write(save: Boolean = true, f: (State) -> State): State {
        val s = f(_state.value)
        _state.value = s
        Me.f = s.female
        if (save) persist(s)
        return s
    }

    private var saving: kotlinx.coroutines.Job? = null

    /** Saves a quarter of a second after the last change, so a burst of changes is one write. */
    private fun persist(s: State) {
        saving?.cancel()
        saving = scope.launch(Dispatchers.IO) {
            kotlinx.coroutines.delay(250)
            val text = _state.value.toJson().toString()
            synchronized(file) {
                val tmp = File(file.path + ".tmp")
                tmp.writeText(text)
                tmp.renameTo(file).takeIf { it } ?: file.writeText(text)
            }
        }
    }

    fun saveNow() = persist(now)

    // ——— the orb ———

    /** The next vision to show, if any tale is left. */
    fun nextVision(s: State = now): Shown? {
        val n = s.visions.size
        var left = n
        for (t in Tales.all) {
            if (left < t.visions.size) return Shown(t, left)
            left -= t.visions.size
        }
        return null
    }

    val rank get() = Rank.of(now.clarity)

    /** Shown visions of a tale (how many). */
    fun shownOf(t: Tale, s: State = now) = s.visions.count { it.startsWith("${t.id}:") }

    /**
     * A practice is done. Looking far and the Darkness rest the eyes; the Darkness also shows the next vision
     * when enough glimpses are gathered.
     */
    fun done(p: Practice, at: Long = System.currentTimeMillis()): Outcome {
        val before = rank
        var out = Outcome(p, p.glimpses)
        write { s ->
            var st = s.copy(
                log = (s.log + Entry(at, p.id)).takeLast(4000),
                glimpses = s.glimpses + p.glimpses,
                clarity = s.clarity + p.glimpses,
            )
            if (p == Practice.FAR || p == Practice.DARK || p == Practice.NEARFAR) st = st.copy(strain = 0, snoozeUntil = 0)
            if (p == Practice.DARK) {
                val next = nextVision(st)
                val shownToday = st.log.any { it.what.startsWith("vision:") && dayOf(it.at) == dayOf(at) }
                if (next != null && shownToday) out = out.copy(tomorrow = true)
                else if (next != null && st.glimpses >= Tales.GLIMPSES) {
                    st = st.copy(
                        glimpses = st.glimpses - Tales.GLIMPSES, clarity = st.clarity + 5,
                        visions = st.visions + "${next.tale.id}:${next.i}",
                        log = st.log + Entry(at, "vision:${next.tale.id}:${next.i}"),
                    )
                    out = out.copy(vision = next)
                } else if (next != null) out = out.copy(missing = Tales.GLIMPSES - st.glimpses)
            }
            st
        }
        val after = rank
        if (after != before) out = out.copy(rank = after)
        out.vision?.let { revealed.tryEmit(it) }
        arrive(at)
        runCatching { OrbWidget.update(app) }
        return out
    }

    // ——— the apprentice's path ———

    /** Today's lesson number (1..[Course.DAYS]); the same number all day once it's done. */
    fun lessonNo(s: State = now) = if (lessonDone(s)) s.lessons.takeIf { it > 0 } ?: Course.DAYS else s.lessons + 1
    fun lessonDone(s: State = now) = s.lessonDay == today()

    /**
     * A lesson walked to its end. One counts a day; the last one closes the course with +[COURSE_CLARITY] clarity.
     * Returns the day just done and whether the course is complete, or null if today's lesson was already counted.
     */
    fun finishLesson(at: Long = System.currentTimeMillis()): Pair<Int, Boolean>? {
        if (lessonDone()) return null
        var res: Pair<Int, Boolean>? = null
        write { s ->
            val n = s.lessons + 1
            val end = n >= Course.DAYS
            res = n to end
            s.copy(
                lessons = if (end) 0 else n, lessonDay = dayOf(at), courses = s.courses + if (end) 1 else 0,
                clarity = s.clarity + LESSON_CLARITY + if (end) COURSE_CLARITY else 0,
                log = (s.log + Entry(at, "lesson:$n")).takeLast(4000),
            )
        }
        return res
    }

    // ——— the pilgrims ———

    /** The question a villager asks next (their answered count), or null when all are answered. */
    private fun askOf(v: Villager, s: State) = v.asks.getOrNull(s.answered[v.id] ?: 0)

    val guest: Villager? get() = Village.of(now.guest)

    /** A new pilgrim climbs up once the last answer was given on an earlier day (or at the very start). */
    fun arrive(at: Long = System.currentTimeMillis()) {
        val s = now
        if (!s.onboarded || s.guest.isNotEmpty()) return
        if (s.lastAnswer > 0 && dayOf(s.lastAnswer) >= dayOf(at)) return
        // the round: everyone's first question, then everyone's second, and so on
        val next = (0..2).firstNotNullOfOrNull { round -> Village.all.firstOrNull { (s.answered[it.id] ?: 0) == round } } ?: return
        write { it.copy(guest = next.id, guestSince = at) }
    }

    /** How much of each need is done since the pilgrim came. */
    fun progress(s: State = now): List<Pair<Need, Int>> {
        val v = Village.of(s.guest) ?: return emptyList()
        val ask = askOf(v, s) ?: return emptyList()
        val since = s.log.filter { it.at >= s.guestSince }
        return ask.needs.map { need ->
            need to when (need.what) {
                "herb" -> since.filter { it.what.startsWith("herb:") }.sumOf { it.n }
                "sun" -> since.filter { it.what == "sun" }.sumOf { it.n }
                else -> since.count { it.what == need.what }
            }.coerceAtMost(need.n)
        }
    }

    fun ready(s: State = now) = progress(s).let { p -> p.isNotEmpty() && p.all { (need, n) -> n >= need.n } }

    /** The orb answers the pilgrim; returns the answer text (bilingual) and whether they left a gift. */
    fun answer(at: Long = System.currentTimeMillis()): Pair<String, Boolean>? {
        val s = now
        val v = Village.of(s.guest) ?: return null
        val idx = s.answered[v.id] ?: 0
        val ask = v.asks.getOrNull(idx) ?: return null
        if (!ready(s)) return null
        write {
            it.copy(
                answered = it.answered + (v.id to idx + 1), guest = "", guestSince = 0, lastAnswer = at,
                clarity = it.clarity + 10, log = it.log + Entry(at, "answer:${v.id}:$idx"),
            )
        }
        return ask.answer to (idx + 1 == v.asks.size)
    }

    val gifts: List<Villager> get() = Village.all.filter { (now.answered[it.id] ?: 0) >= it.asks.size }

    // ——— daylight ———

    /** Minutes in daylight: every [Herb.MINUTES] become a bunch of this season's herb. Returns the herbs picked. */
    fun daylight(min: Int, at: Long = System.currentTimeMillis()): List<Herb> {
        if (min <= 0) return emptyList()
        val picked = ArrayList<Herb>()
        write { s ->
            var left = s.sunLeft + min
            var herbs = s.herbs
            var log = s.log + Entry(at, "sun", min)
            val season = Herb.season(LocalDate.now().monthValue)
            while (left >= Herb.MINUTES) {
                left -= Herb.MINUTES
                val h = season[(herbs.values.sum()) % season.size]
                herbs = herbs + (h.id to (herbs[h.id] ?: 0) + 1)
                log = log + Entry(at, "herb:${h.id}")
                picked += h
            }
            s.copy(sunLeft = left, herbs = herbs, log = log.takeLast(4000))
        }
        return picked
    }

    // ——— the day's numbers ———

    fun count(what: String, day: Long = today(), s: State = now) = s.log.count { it.what == what && dayOf(it.at) == day }
    fun sunToday(s: State = now) = s.log.filter { it.what == "sun" && dayOf(it.at) == today() }.sumOf { it.n }
    fun practicesToday(s: State = now) = s.log.count { Practice.of(it.what) != null && dayOf(it.at) == today() }

    /** Per day of the last [n] days: looks far, other practices, daylight minutes. */
    fun days(n: Int, s: State = now): List<Triple<Int, Int, Int>> {
        val t = today()
        val by = s.log.groupBy { dayOf(it.at) }
        return (n - 1 downTo 0).map { back ->
            val l = by[t - back].orEmpty()
            Triple(l.count { it.what == "far" }, l.count { Practice.of(it.what) != null && it.what != "far" }, l.filter { it.what == "sun" }.sumOf { it.n })
        }
    }

    fun tell(key: String) = write { it.copy(told = it.told + key) }
}
