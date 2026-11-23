package app.secondsight.domain

/** One step of a lesson: a practice and how long it runs this time. */
data class Step(val p: Practice, val sec: Int)

/** A part of the apprentice's path: what the eyes learn in these days, and what the seer says about it. */
enum class Stage(val from: Int, private val ru: String, private val en: String, private val sayRu: String, private val sayEn: String) {
    LET_GO(1, "Отпустить", "Letting go",
        "Сначала глаза надо научить отдыхать. Моргать, смотреть вдаль, уходить в темноту. Без этого всё остальное не пойдёт впрок.",
        "First the eyes must learn to rest: to blink, to look far, to sink into the dark. Without this, nothing else takes."),
    FOCUS(6, "Фокус", "Focus",
        "Теперь учимся переводить фокус: ближе – дальше, к переносице и обратно. Глаза вспоминают, что умеют смотреть не только в экран.",
        "Now we learn to shift focus: near and far, toward the nose and back. The eyes remember they can look at more than a screen."),
    MOTION(11, "Движение", "Motion",
        "Глазам нужно двигаться, как рукам и спине. Будем уводить взгляд до упора во все стороны и перебрасывать его от края до края.",
        "The eyes need to move, as hands and back do. We'll take the gaze as far as it goes every way and throw it from edge to edge."),
    WIDE(16, "Широкий взгляд", "The wide gaze",
        "Последняя ступень – замечать то, что сбоку. Кто видит широко, тот меньше напрягается, когда смотрит прямо.",
        "The last step is to see from the corner of the eye. Whoever sees wide strains less looking straight ahead.");

    val title get() = tr(ru, en)
    val say get() = tr(sayRu, sayEn)

    companion object {
        fun of(day: Int) = entries.last { day >= it.from }
    }
}

/** The apprentice's path: 21 lessons, one a day, from rest to the wide gaze; each a little longer than the last. */
object Course {
    const val DAYS = 21

    /** The lesson of [day] (1..[DAYS]). */
    fun lesson(day: Int): List<Step> {
        val st = Stage.of(day)
        val k = day - st.from // 0.. within the stage
        return when (st) {
            Stage.LET_GO -> listOf(Step(Practice.BLINK, 30), Step(Practice.FAR, 20 + 5 * k), Step(Practice.DARK, 60 + 15 * k))
            Stage.FOCUS -> listOf(Step(Practice.BLINK, 30), Step(Practice.NEARFAR, 60 + 10 * k), Step(Practice.THIRD, 40 + 5 * k), Step(Practice.FAR, 20), Step(Practice.DARK, 90))
            Stage.MOTION -> listOf(Step(Practice.BLINK, 30), Step(Practice.TRACK, if (k >= 3) 120 else 60), Step(Practice.JUMP, if (k >= 2) 90 else 45), Step(Practice.NEARFAR, 60), Step(Practice.DARK, 90))
            Stage.WIDE -> listOf(Step(Practice.BLINK, 30), Step(Practice.SIDE, 60 + 10 * k), Step(Practice.TRACK, 60), Step(Practice.THIRD, 60), Step(Practice.FAR, 20), Step(Practice.DARK, 120 + 10 * k))
        }
    }

    fun sec(day: Int) = lesson(day).sumOf { it.sec }
}

/** What the seer gives for a path walked to the end: one gift a course, in this order. */
enum class Master(val id: String, private val ru: String, private val en: String, private val sayRu: String, private val sayEn: String) {
    BRUSH("brush", "Кисть и тушечница", "Brush and inkstone",
        "Возьми мою кисть. Когда глаза отдохнули, и линия ложится ровнее.",
        "Take my brush. When the eyes are rested, the line lies truer."),
    MOON("moon", "Нефритовый полумесяц", "Jade crescent",
        "Носи его с собой. Когда глаза устанут, коснись его и вспомни про тьму.",
        "Keep it with you. When your eyes grow tired, touch it and remember the dark."),
    MIRROR("mirror", "Бронзовое зеркало", "Bronze mirror",
        "Это зеркало показывает не лицо, а то, как ты смотришь на мир.",
        "This mirror shows not your face but how you look at the world."),
    ORB("orb", "Свой шар", "An orb of your own",
        "Вот и твой собственный шар. Мне больше нечему тебя учить – дальше смотри сам{а}.",
        "Here is an orb of your own. I have nothing more to teach you; from now on, see for yourself.");

    val title get() = tr(ru, en)
    val say get() = tr(sayRu.replace("{а}", g("", "а")), sayEn)
    val art get() = "master_$id"

    companion object {
        /** The gift of the [n]th course walked (1-based); after the fourth the orb is given again. */
        fun of(n: Int) = entries[(n - 1).coerceIn(0, entries.size - 1)]
    }
}
