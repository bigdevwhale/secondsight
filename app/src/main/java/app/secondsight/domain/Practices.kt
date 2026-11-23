package app.secondsight.domain

/** Who the apprentice is: verbs and ranks agree with it («посмотрела» / «посмотрел»). */
object Me { @Volatile var f = true }

/** «{ученик|ученица}»-style choice by the apprentice's gender. */
fun g(m: String, f: String) = if (Me.f) f else m

/** Russian with {м|ж} marks, English as is: "Ты {посмотрел|посмотрела}|You looked". */
fun gb(s: String): String = bi(s).replace(Regex("\\{([^|}]*)\\|([^}]*)\\}")) { if (Me.f) it.groupValues[2] else it.groupValues[1] }

enum class Group { EYES, BODY }

/**
 * A practice of the Sight. [sec] is its usual length; [glimpses] what it brings to the orb.
 * The ids are stored in the log, so they never change.
 */
enum class Practice(
    val id: String, val icon: String, val group: Group, val sec: Int, val glimpses: Int,
    private val ru: String, private val en: String, private val whyRu: String, private val whyEn: String,
) {
    FAR("far", "ic_far", Group.EYES, 20, 1, "Взгляд вдаль", "Look far away",
        "Двадцать секунд смотреть на самую дальнюю точку за окном. Глазам больше не нужно держать фокус вблизи, и они отдыхают.",
        "Twenty seconds on the farthest point outside the window. The eyes stop holding a near focus and rest."),
    TRACK("track", "ic_track", Group.EYES, 60, 1, "Восемь сторон", "Eight directions",
        "Уводить взгляд до упора вверх, вниз, в стороны и по диагоналям, потом кругами. У экрана глаза двигаются в крошечном окошке, а здесь – во всю ширь.",
        "Take the gaze as far as it goes up, down, sideways and corner-wise, then round in circles. At a screen the eyes move in a tiny window; here the muscles work through their whole range."),
    NEARFAR("nearfar", "ic_nearfar", Group.EYES, 60, 1, "Близко и далеко", "Near and far",
        "Смотреть то на палец у лица, то на дальний предмет. Так тренируется фокусировка.",
        "Look at a finger near your face, then at something far away, in turns. This trains focusing."),
    JUMP("jump", "ic_jump", Group.EYES, 45, 1, "Два фонаря", "Two lanterns",
        "По щелчку перебрасывать взгляд от края до края: влево-вправо, вверх-вниз, из угла в угол. Взгляд учится прыгать быстро и точно.",
        "On each click, throw the gaze from edge to edge: left and right, up and down, corner to corner. The eyes learn to jump fast and true."),
    BLINK("blink", "ic_blink", Group.EYES, 30, 1, "Роса", "Dew",
        "Мягко моргать в такт падающей росе. У экрана мы моргаем втрое реже, и глаза сохнут.",
        "Blink softly with the falling dew. At a screen we blink three times less, and the eyes dry out."),
    THIRD("third", "ic_third", Group.EYES, 45, 1, "Третий глаз", "The third eye",
        "Держать телефон на вытянутой руке и медленно подносить к переносице, не теряя огонька. Так тренируется сведение глаз.",
        "Hold the phone at arm's length and slowly bring it to the bridge of your nose, keeping the light single. The eyes learn to converge."),
    SIDE("side", "ic_side", Group.EYES, 60, 1, "Боковое зрение", "Side vision",
        "Смотреть в центр и замечать вспышки по краям. Провидица видит не только то, на что смотрит.",
        "Keep your eyes on the centre and notice the flashes at the edges. A seer sees more than what she looks at."),
    DARK("dark", "ic_dark", Group.EYES, 120, 0, "Тьма", "Darkness",
        "Накрыть закрытые глаза тёплыми ладонями и посидеть в темноте. После Тьмы шар показывает видение.",
        "Cover your closed eyes with warm palms and sit in the dark. After the Darkness the orb shows a vision."),
    HANDS("hands", "ic_hands", Group.BODY, 90, 1, "Мудры", "Mudras",
        "Разминка кистей и пальцев: шесть жестов-мудр. Для тех, кто много печатает.",
        "Six mudras to loosen wrists and fingers. For those who type a lot."),
    BACK("back", "ic_back", Group.BODY, 90, 1, "Бамбук", "Bamboo",
        "Разминка для шеи и плеч. Сидеть ровно, как растёт бамбук.",
        "Neck and shoulders. Sit straight, the way bamboo grows."),
    BREATH("breath", "ic_breath", Group.BODY, 60, 1, "Чаша", "The bowl",
        "Медленно дышать, пока вода в чаше не успокоится.",
        "Breathe slowly until the water in the bowl is still."),
    CANDLE("candle", "ic_candle", Group.BODY, 30, 0, "Погасить фонарь", "Put out the lantern",
        "Вечером проститься с экраном до утра. Глазам и сну это нужно больше всего.",
        "In the evening say goodbye to the screen until morning. Your eyes and your sleep need it most.");

    val title get() = tr(ru, en)
    val why get() = tr(whyRu, whyEn)

    companion object {
        fun of(id: String) = entries.firstOrNull { it.id == id }
        val eyes get() = entries.filter { it.group == Group.EYES }
        val body get() = entries.filter { it.group == Group.BODY && it != CANDLE }
    }
}

/** Ready-made sets: one tap runs them one after another. */
enum class Ritual(val id: String, val steps: List<Practice>, private val ru: String, private val en: String, private val subRu: String, private val subEn: String) {
    MINUTE("minute", listOf(Practice.BLINK, Practice.FAR), "Минутка", "One minute", "Поморгать и посмотреть вдаль", "Blink and look far"),
    MORNING("morning", listOf(Practice.BLINK, Practice.TRACK, Practice.NEARFAR, Practice.THIRD), "Утро", "Morning", "Разбудить глаза", "Wake your eyes up"),
    EVENING("evening", listOf(Practice.FAR, Practice.JUMP, Practice.SIDE, Practice.DARK), "После работы", "After work", "Снять усталость за день", "Shed the day's strain"),
    BODY("body", listOf(Practice.HANDS, Practice.BACK, Practice.BREATH), "Руки и спина", "Hands and back", "Для тех, кто весь день за столом", "For a day at the desk");

    val title get() = tr(ru, en)
    val sub get() = tr(subRu, subEn)
    val sec get() = steps.sumOf { it.sec }
}

/** The six gestures of «Мудры» and the moves of «Бамбук»: (what to do, seconds). */
object Moves {
    val hands = listOf(
        "Сожми кулаки, потом широко раскрой ладони, как будто отпускаешь светлячков. Десять раз.|Make fists, then open your palms wide as if letting fireflies go. Ten times." to 15,
        "Рисуй кистями круги – пять в одну сторону, пять в другую.|Draw circles with your wrists – five one way, five the other." to 15,
        "Коснись большим пальцем каждого пальца по очереди, туда и обратно.|Touch each fingertip with your thumb in turn, there and back." to 15,
        "Вытяни руку ладонью вперёд и мягко потяни пальцы на себя другой рукой. Потом другую руку.|Stretch an arm out, palm forward, and gently pull the fingers back with the other hand. Then switch." to 20,
        "Сложи ладони у груди и медленно опусти их, не разнимая. Почувствуй, как тянутся запястья.|Press your palms together at your chest and slowly lower them, keeping them together. Feel the wrists stretch." to 15,
        "Встряхни кисти, как будто стряхиваешь воду.|Shake your hands out as if shaking off water." to 10,
    )
    val back = listOf(
        "Сядь ровно, макушкой тянись вверх, плечи вниз.|Sit tall: crown of the head up, shoulders down." to 10,
        "Медленно наклони голову к правому плечу, потом к левому. По три раза.|Slowly tilt your head to the right shoulder, then the left. Three times each." to 20,
        "Отведи подбородок назад, будто делаешь двойной подбородок. Пять раз.|Draw your chin straight back, making a double chin. Five times." to 15,
        "Подними плечи к ушам, задержи и резко опусти. Пять раз.|Lift your shoulders to your ears, hold, and drop them. Five times." to 15,
        "Сведи лопатки, раскрой грудь и задержись так на пять вдохов.|Squeeze your shoulder blades together, open the chest, hold for five breaths." to 15,
        "Поверни голову вправо, посмотри через плечо вдаль, потом влево.|Turn your head right and look far over your shoulder, then left." to 15,
    )
}
