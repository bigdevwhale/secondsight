package app.secondsight.data

import app.secondsight.domain.Practice
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

/**
 * `-Pdemo`: three weeks of an apprentice's practice, played through the real rules, for screenshots.
 */
object Demo {
    fun seedIfEmpty(g: Game) {
        if (g.now.onboarded) return
        val rnd = Random(11)
        val zone = ZoneId.systemDefault()
        g.write { it.copy(onboarded = true, female = true, lanternAt = -1) }
        val start = LocalDate.now().minusDays(20)
        for (d in 0..20) {
            val day = start.plusDays(d.toLong())
            fun at(h: Int, m: Int) = day.atTime(h, m).atZone(zone).toInstant().toEpochMilli()
            val fars = 4 + rnd.nextInt(6)
            repeat(fars) { i -> g.done(Practice.FAR, at(9 + i, rnd.nextInt(60))) }
            if (rnd.nextInt(3) > 0) g.done(Practice.BLINK, at(11, 5))
            if (d % 2 == 0) g.done(Practice.TRACK, at(13, 10))
            if (d % 3 == 0) { g.done(Practice.NEARFAR, at(15, 20)); g.done(Practice.HANDS, at(15, 30)) }
            if (d % 4 == 1) g.done(Practice.SIDE, at(16, 40))
            if (d % 2 == 1) g.done(Practice.JUMP, at(17, 15))
            g.daylight(10 + rnd.nextInt(40), at(13, 30))
            g.done(Practice.DARK, at(20, 30))
            if (d % 3 != 2) g.done(Practice.DARK, at(21, 30))
            // the pilgrim of the day gets the answer in the evening when ready
            g.arrive(at(8, 0))
            if (g.ready()) g.answer(at(21, 50))
        }
        g.write { it.copy(strain = 11 * 60, glimpses = 2, lastAnswer = 0, lessons = 7, lessonDay = app.secondsight.domain.today() - 1, courses = 1) }
        g.arrive()
    }
}
