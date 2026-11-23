package app.secondsight.ui

import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings as Sys
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.secondsight.container
import app.secondsight.currentLanguage
import app.secondsight.data.State
import app.secondsight.setLanguage
import app.secondsight.svc.Watch
import app.secondsight.domain.tr

private fun hm(m: Int) = "%d:%02d".format(m / 60, m % 60)

@Composable
fun Settings(s: State) {
    val ctx = LocalContext.current
    val game = ctx.container.game
    Sheet(Ui.settings, { Ui.settings = false }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(tr("Настройки", "Settings"), Modifier.weight(1f), style = T.big)
            Link(tr("закрыть", "close")) { Ui.settings = false }
        }
        Spacer(Modifier.height(14.dp))
        Label(tr("Напоминания", "Reminders"))
        Spacer(Modifier.height(8.dp))
        Card(pad = 14.dp) {
        Toggle(tr("Звать смотреть вдаль", "Call me to look far"), s.remind) { v -> game.write { it.copy(remind = v) } }
        if (s.remind) {
            Pick(tr("Шар туманится за", "The orb mists over in"), listOf(15, 20, 30, 45), s.interval, { tr("$it мин", "$it min") }) { v -> game.write { it.copy(interval = v) } }
            Toggle(tr("Я за компьютером", "I'm at the computer"), s.desk,
                tr("Шар туманится по часам, а не по экрану телефона. Само выключится через 10 часов.", "The orb mists by the clock, not the phone's screen. Turns off by itself after 10 hours.")) { v ->
                game.write { it.copy(deskSince = if (v) System.currentTimeMillis() else 0, strain = 0) }
                Watch.start(ctx)
            }
        }
        }
        Section(tr("Ночь", "Night"))
        Card(pad = 14.dp) {
        Text(tr("Тишина ${if (s.quietFrom == 24 * 60 - 1) "0:00" else hm(s.quietFrom)} – ${hm(s.quietTo)}", "Quiet ${if (s.quietFrom == 24 * 60 - 1) "0:00" else hm(s.quietFrom)} – ${hm(s.quietTo)}"), style = T.body)
        Text(tr("В эти часы шар не зовёт.", "The orb doesn't call in these hours."), style = T.tiny)
        Pick(tr("с", "from"), listOf(21 * 60, 22 * 60, 23 * 60, 24 * 60 - 1), s.quietFrom, { if (it == 24 * 60 - 1) "0:00" else hm(it) }, small = true) { v -> game.write { it.copy(quietFrom = v) } }
        Pick(tr("до", "until"), listOf(6 * 60, 7 * 60, 8 * 60, 9 * 60), s.quietTo, { hm(it) }, small = true) { v -> game.write { it.copy(quietTo = v) } }
        Pick(tr("Вечерний фонарь – пора отложить телефон", "Evening lantern: time to put the phone down"), listOf(-1, 21 * 60 + 30, 22 * 60 + 30, 23 * 60 + 30), s.lanternAt, { if (it < 0) tr("нет", "off") else hm(it) }) { v -> game.write { it.copy(lanternAt = v) } }
        }
        Section(tr("Звук", "Sound"))
        Card(pad = 14.dp) {
        Toggle(tr("Звон колокольчика", "Bell sound"), s.sound) { v -> game.write { it.copy(sound = v) } }
        Toggle(tr("Голос провидицы", "The seer's voice"), s.voice, tr("Подсказывает вслух, что делать. Говорит голосом телефона", "Tells you when to begin and end, using the phone's speech engine")) { v -> game.write { it.copy(voice = v) } }
        }
        Section(tr("Ты", "You"))
        Card(pad = 14.dp) {
        Pick(tr("Обращаться ко мне", "Call me"), listOf(true, false), s.female, { if (it) tr("ученица", "she") else tr("ученик", "he") }) { v -> game.write { it.copy(female = v) } }
        Pick(tr("Язык", "Language"), listOf("", "ru", "en"), currentLanguage(), { when (it) { "ru" -> "Русский"; "en" -> "English"; else -> tr("как в телефоне", "as the phone") } }) { setLanguage(it) }
        }
        Spacer(Modifier.height(12.dp))
        Rule(Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        Text(tr("Чтобы напоминания приходили", "So that reminders arrive"), style = T.h3)
        Text(tr("На Xiaomi, Huawei и некоторых других телефонах приложение надо разрешить запускать в фоне и не экономить на нём батарею.",
            "On Xiaomi, Huawei and some other phones, allow the app to run in the background and don't let the battery saver stop it."), style = T.small)
        Spacer(Modifier.height(8.dp))
        val pm = ctx.getSystemService(PowerManager::class.java)
        if (!pm.isIgnoringBatteryOptimizations(ctx.packageName)) Btn(tr("Не экономить батарею", "Don't optimise battery"), Modifier.fillMaxWidth(), main = false, small = true) {
            runCatching { ctx.startActivity(Intent(Sys.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${ctx.packageName}"))) }
        }
        Spacer(Modifier.height(6.dp))
        Btn(tr("Настройки приложения", "App settings"), Modifier.fillMaxWidth(), main = false, small = true) {
            runCatching { ctx.startActivity(Intent(Sys.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}"))) }
        }
        Spacer(Modifier.height(14.dp))
        Text(tr("Приложение не заменяет врача. Если зрение падает, глаза болят или краснеют, сходи к офтальмологу.",
            "This app is no substitute for a doctor. If your sight gets worse or your eyes hurt or redden, see an eye doctor."), style = T.tiny)
        Spacer(Modifier.height(6.dp))
        Spacer(Modifier.height(10.dp))
        Rule(Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        Text("StillCraft", style = T.h3)
        fun go(u: String) = runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u))) }
        Link(tr("Сообщить об ошибке", "Report a bug")) { go(Community.BUG) }
        Link(tr("Предложить идею", "Suggest an idea")) { go(Community.IDEA) }
        Link(tr("Следить за обновлениями", "Follow updates")) { go(Community.FOLLOW) }
        Link(tr("Чат StillCraft", "StillCraft chat")) { go(Community.CHAT) }
        Link(tr("Поддержать «Второе зрение»", "Support Second Sight")) { go(Community.SUPPORT) }
    }
}

@Composable
private fun Toggle(title: String, on: Boolean, sub: String? = null, set: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().tap { set(!on) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = T.body)
            if (sub != null) Text(sub, style = T.tiny)
        }
        // a switch of misty glass: the light slides to the right and glows when on
        androidx.compose.foundation.layout.Box(
            Modifier.size(56.dp, 32.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(50))
                .background(if (on) C.amber.copy(alpha = 0.18f) else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.04f))
                .border(1.dp, if (on) C.amber.copy(alpha = 0.7f) else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.2f), androidx.compose.foundation.shape.RoundedCornerShape(50)),
            contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart,
        ) { Light(30.dp, warm = on, strength = if (on) 1f else 0.45f) }
    }
}

@Composable
private fun <V> Pick(title: String, values: List<V>, current: V, label: (V) -> String, small: Boolean = false, set: (V) -> Unit) {
    if (small) Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.width(36.dp), style = T.small)
        values.forEach { v -> Btn(label(v), Modifier.weight(1f), main = v == current, small = true) { set(v) } }
    } else Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(title, style = T.body)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            values.forEach { v -> Btn(label(v), Modifier.weight(1f), main = v == current, small = true) { set(v) } }
        }
    }
}

@Composable
private fun Section(title: String) {
    Spacer(Modifier.height(22.dp))
    Label(title)
    Spacer(Modifier.height(8.dp))
}

/** The StillCraft bot: deep links file a bug or an idea for this app, or follow its releases. */
object Community {
    private const val BOT = "https://t.me/ArcaneWhisperBot?start="
    const val SLUG = "secondsight"
    const val BUG = "${BOT}bug_$SLUG"
    const val IDEA = "${BOT}wish_$SLUG"
    const val FOLLOW = "${BOT}f_$SLUG"
    const val CHAT = "https://t.me/+Q4gFEDk8O842OGVi"
    const val SUPPORT = "https://bigdevwhale.github.io/stillcraft/support/"
}
