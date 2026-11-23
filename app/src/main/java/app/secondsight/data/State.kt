package app.secondsight.data

import org.json.JSONArray
import org.json.JSONObject

/** One thing done: a practice id, "sun" (n = minutes of daylight), "herb:<id>", "vision:<tale>:<i>", "answer:<villager>:<i>". */
data class Entry(val at: Long, val what: String, val n: Int = 1)

data class State(
    val onboarded: Boolean = false,
    /** The apprentice is a girl: «посмотрела», «Послушница». */
    val female: Boolean = true,
    // — reminders —
    val remind: Boolean = true,
    /** Minutes of screen between two looks far away. */
    val interval: Int = 20,
    /** Minutes of the day: no reminders between [quietFrom] and [quietTo]. */
    val quietFrom: Int = 23 * 60,
    val quietTo: Int = 8 * 60,
    /** The evening lantern, minutes of the day; -1 = off. */
    val lanternAt: Int = 22 * 60 + 30,
    /** At the computer: the strain grows with the clock, not the phone's screen. */
    val deskSince: Long = 0,
    val snoozeUntil: Long = 0,
    val sound: Boolean = true,
    val voice: Boolean = true,
    // — the orb —
    /** Seconds of looking near since the eyes last rested. */
    val strain: Int = 0,
    /** Glimpses gathered towards the next vision. */
    val glimpses: Int = 0,
    /** Clarity: every glimpse ever, plus 5 for each vision. Gives the rank. */
    val clarity: Int = 0,
    /** Visions shown, in order: "t1:0", "t1:1", … */
    val visions: List<String> = emptyList(),
    // — the stairway —
    /** The pilgrim waiting at the pavilion and since when their question counts. */
    val guest: String = "",
    val guestSince: Long = 0,
    /** Answers given: villager → how many of their questions. */
    val answered: Map<String, Int> = emptyMap(),
    val lastAnswer: Long = 0,
    // — daylight and herbs —
    val herbs: Map<String, Int> = emptyMap(),
    /** Daylight minutes not yet made into a bunch. */
    val sunLeft: Int = 0,
    /** A walk for herbs, started by hand. */
    val walkSince: Long = 0,
    // — the apprentice's path —
    /** Lessons done in the current course, and the day (epoch) of the last one. */
    val lessons: Int = 0,
    val lessonDay: Long = 0,
    /** Courses walked to the end. */
    val courses: Int = 0,
    val log: List<Entry> = emptyList(),
    val told: Set<String> = emptySet(),
) {
    val desk get() = deskSince > 0

    fun toJson(): JSONObject = JSONObject().apply {
        put("v", 1)
        put("onboarded", onboarded); put("female", female)
        put("remind", remind); put("interval", interval); put("quietFrom", quietFrom); put("quietTo", quietTo)
        put("lanternAt", lanternAt); put("deskSince", deskSince); put("snoozeUntil", snoozeUntil); put("sound", sound); put("voice", voice)
        put("strain", strain); put("glimpses", glimpses); put("clarity", clarity)
        put("visions", JSONArray(visions))
        put("guest", guest); put("guestSince", guestSince); put("lastAnswer", lastAnswer)
        put("answered", JSONObject(answered as Map<*, *>))
        put("herbs", JSONObject(herbs as Map<*, *>)); put("sunLeft", sunLeft); put("walkSince", walkSince)
        put("lessons", lessons); put("lessonDay", lessonDay); put("courses", courses)
        put("log", JSONArray().apply { log.forEach { put(JSONArray().put(it.at).put(it.what).put(it.n)) } })
        put("told", JSONArray(told.toList()))
    }

    companion object {
        private fun JSONObject.ints(k: String): Map<String, Int> = optJSONObject(k)?.let { o -> o.keys().asSequence().associateWith { o.getInt(it) } } ?: emptyMap()
        private fun JSONObject.strs(k: String): List<String> = optJSONArray(k)?.let { a -> List(a.length()) { a.getString(it) } } ?: emptyList()

        fun of(o: JSONObject): State {
            val d = State()
            return State(
                onboarded = o.optBoolean("onboarded"), female = o.optBoolean("female", true),
                remind = o.optBoolean("remind", d.remind), interval = o.optInt("interval", d.interval),
                quietFrom = o.optInt("quietFrom", d.quietFrom), quietTo = o.optInt("quietTo", d.quietTo),
                lanternAt = o.optInt("lanternAt", d.lanternAt), deskSince = o.optLong("deskSince"), snoozeUntil = o.optLong("snoozeUntil"),
                sound = o.optBoolean("sound", true), voice = o.optBoolean("voice", true),
                strain = o.optInt("strain"), glimpses = o.optInt("glimpses"), clarity = o.optInt("clarity"),
                visions = o.strs("visions"),
                guest = o.optString("guest"), guestSince = o.optLong("guestSince"), lastAnswer = o.optLong("lastAnswer"),
                answered = o.ints("answered"),
                herbs = o.ints("herbs"), sunLeft = o.optInt("sunLeft"), walkSince = o.optLong("walkSince"),
                lessons = o.optInt("lessons"), lessonDay = o.optLong("lessonDay"), courses = o.optInt("courses"),
                log = o.optJSONArray("log")?.let { a -> List(a.length()) { i -> a.getJSONArray(i).let { e -> Entry(e.getLong(0), e.getString(1), e.optInt(2, 1)) } } } ?: emptyList(),
                told = o.strs("told").toSet(),
            )
        }
    }
}
