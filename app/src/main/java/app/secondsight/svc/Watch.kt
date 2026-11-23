package app.secondsight.svc

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import app.secondsight.BuildConfig
import app.secondsight.MainActivity
import app.secondsight.R
import app.secondsight.container
import app.secondsight.domain.Lang
import app.secondsight.domain.count
import app.secondsight.domain.dayOf
import app.secondsight.domain.tr
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalTime
import kotlin.coroutines.resume

/**
 * Watches the screen the whole day: while it's on, the orb mists over; while it's off, the eyes rest and the mist
 * thins three times as fast. When the mist is thick, a gentle note asks to look far away. Also samples the light
 * now and then, to count the minutes spent in daylight, and lights the evening lantern note.
 *
 * A foreground service, because HyperOS and friends freeze anything less; its own note shows the orb's state.
 */
class Watch : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var loop: Job? = null
    private var lastTick = 0L
    private var offAt = 0L
    private var lastSun = 0L
    private var lastAsk = 0L
    private var lastSave = 0L

    private val screen = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            when (i.action) {
                Intent.ACTION_SCREEN_OFF -> { tick(); offAt = System.currentTimeMillis(); container.game.saveNow() }
                Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> {
                    if (offAt > 0) rest(System.currentTimeMillis() - offAt)
                    offAt = 0; lastTick = System.currentTimeMillis()
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        channels(this)
        ServiceCompat.startForeground(this, ID_STATUS, status(), if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0)
        ContextCompat.registerReceiver(this, screen, IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF); addAction(Intent.ACTION_SCREEN_ON); addAction(Intent.ACTION_USER_PRESENT)
        }, ContextCompat.RECEIVER_NOT_EXPORTED)
        lastTick = System.currentTimeMillis()
        loop = scope.launch {
            while (isActive) {
                tick()
                delay(30_000)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACT_SNOOZE -> snooze(intent.getIntExtra("min", 10))
            else -> refresh()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(screen) }
        container.game.saveNow()
        scope.cancel()
        super.onDestroy()
    }

    private val interactive get() = (getSystemService(POWER_SERVICE) as PowerManager).isInteractive

    /** The eyes rested for [ms] with the screen off: the mist thins three times as fast as it gathered. */
    private fun rest(ms: Long) {
        val g = container.game
        if (g.now.desk) return
        val sec = (ms / 1000 * 3).toInt()
        g.write(save = false) { it.copy(strain = (it.strain - sec).coerceAtLeast(0)) }
        refresh()
    }

    private fun tick() {
        val g = container.game
        val now = System.currentTimeMillis()
        val dt = ((now - lastTick) / 1000).toInt().coerceIn(0, 120)
        lastTick = now
        val s = g.now
        if (!s.onboarded) return
        // at the desk the clock counts; desk mode ends by itself after ten hours
        if (s.desk && now - s.deskSince > 10 * 3600_000L) g.write { it.copy(deskSince = 0) }
        val counting = g.now.desk || interactive
        if (counting && dt > 0) g.write(save = false) { it.copy(strain = (it.strain + dt).coerceAtMost(4 * 3600)) }
        if (now - lastSave > 120_000) { lastSave = now; g.saveNow() }
        maybeAsk(now)
        maybeLantern(now)
        if (interactive && now - lastSun > 120_000) { lastSun = now; scope.launch { sampleSun() } }
        refresh()
    }

    private fun quiet(minute: Int, from: Int, to: Int) = if (from <= to) minute in from until to else minute >= from || minute < to

    private fun inCall(): Boolean {
        val m = (getSystemService(AUDIO_SERVICE) as AudioManager).mode
        return m == AudioManager.MODE_IN_CALL || m == AudioManager.MODE_IN_COMMUNICATION || m == AudioManager.MODE_RINGTONE
    }

    /** The mist is thick: ask once, then again every ten minutes while it stays. */
    private fun maybeAsk(now: Long) {
        val s = container.game.now
        if (!s.remind || s.strain < s.interval * 60 || now < s.snoozeUntil) return
        val t = LocalTime.now(); if (quiet(t.hour * 60 + t.minute, s.quietFrom, s.quietTo)) return
        if (inCall() || Foreground.practising || Foreground.visible) return
        if (!s.desk && !interactive) return
        if (now - lastAsk < 10 * 60_000) return
        lastAsk = now
        ask(this, s.sound)
    }

    private fun maybeLantern(now: Long) {
        val g = container.game
        val s = g.now
        if (s.lanternAt < 0 || !interactive) return
        val t = LocalTime.now(); val m = t.hour * 60 + t.minute
        if (m < s.lanternAt || m > s.lanternAt + 90) return
        val key = "lantern:${dayOf(now)}"
        if (key in s.told || s.log.any { it.what == "candle" && dayOf(it.at) == dayOf(now) }) return
        g.tell(key)
        note(this, ID_LANTERN, CH_ASK, tr("Пора погасить фонарь", "Time to put out the lantern"),
            tr("Провидица ложится спать. Попрощайся с экраном до утра.", "The seer is going to bed. Say goodbye to the screen until morning."),
            "candle", s.sound)
    }

    /** One look at the light sensor: daylight is a thousand lux and more. */
    private suspend fun sampleSun() {
        val g = container.game
        val h = LocalTime.now().hour
        if (h !in 7..20) return
        val sm = getSystemService(SENSOR_SERVICE) as SensorManager
        val sensor = sm.getDefaultSensor(Sensor.TYPE_LIGHT) ?: return
        val lux = withTimeoutOrNull(3000) {
            suspendCancellableCoroutine<Float> { k ->
                val l = object : SensorEventListener {
                    override fun onSensorChanged(e: SensorEvent) { sm.unregisterListener(this); if (k.isActive) k.resume(e.values[0]) }
                    override fun onAccuracyChanged(s: Sensor?, a: Int) {}
                }
                sm.registerListener(l, sensor, SensorManager.SENSOR_DELAY_NORMAL)
                k.invokeOnCancellation { sm.unregisterListener(l) }
            }
        } ?: return
        val now = System.currentTimeMillis()
        if (lux >= DAYLIGHT_LUX && g.now.walkSince == 0L) {
            val herbs = g.daylight(2, now)
            if (herbs.isNotEmpty()) note(this, ID_HERB, CH_QUIET, tr("Ты на дневном свету", "You're in daylight"),
                tr("Собран пучок: ${herbs.first().title}", "Picked a bunch: ${herbs.first().title}"), "village", false)
        }
    }

    private fun snooze(min: Int) {
        container.game.write { it.copy(snoozeUntil = System.currentTimeMillis() + min * 60_000L) }
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).cancel(ID_ASK)
        lastAsk = 0
        refresh()
    }

    private fun refresh() {
        OrbWidget.update(this)
        if (BuildConfig.DEMO) return
        runCatching { (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(ID_STATUS, status()) }
    }

    private fun status(): Notification {
        val s = container.game.now
        val left = (s.interval * 60 - s.strain).coerceAtLeast(0)
        val text = when {
            !s.remind -> tr("Напоминания выключены", "Reminders are off")
            System.currentTimeMillis() < s.snoozeUntil -> tr("Отложено", "Snoozed")
            left == 0 -> tr("Шар затуманился. Посмотри вдаль", "The orb is misted. Look far away")
            left < 60 -> tr("Шар почти затуманился", "The orb is nearly misted")
            else -> tr("До взгляда вдаль ${left / 60} мин", "${left / 60} min until looking far")
        } + if (s.desk) tr(" · за компьютером", " · at the desk") else ""
        return NotificationCompat.Builder(this, CH_STATUS)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle(text)
            .setContentText(if (s.glimpses > 0) count(s.glimpses, "проблеск", "проблеска", "проблесков", "glimpse", "glimpses") + tr(" в шаре", " in the orb") else tr("Шар ждёт проблесков", "The orb is waiting for glimpses"))
            .setOngoing(true).setShowWhen(false).setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentIntent(open(this, null, 0))
            .build()
    }

    companion object {
        const val CH_STATUS = "status"
        const val CH_ASK = "ask"
        const val CH_QUIET = "quiet"
        const val ID_STATUS = 1
        const val ID_ASK = 2
        const val ID_LANTERN = 3
        const val ID_HERB = 4
        const val ACT_SNOOZE = "snooze"
        const val DAYLIGHT_LUX = 1000f

        fun channels(c: Context) {
            val nm = c.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel(CH_STATUS, tr("Шар", "The orb"), NotificationManager.IMPORTANCE_MIN).apply {
                description = tr("Сколько осталось до взгляда вдаль", "Time left until looking far"); setShowBadge(false)
            })
            nm.createNotificationChannel(NotificationChannel(CH_ASK, tr("Посмотреть вдаль", "Look far away"), NotificationManager.IMPORTANCE_HIGH).apply {
                description = tr("Когда шар затуманится", "When the orb mists over")
            })
            nm.createNotificationChannel(NotificationChannel(CH_QUIET, tr("Травы и паломники", "Herbs and pilgrims"), NotificationManager.IMPORTANCE_LOW))
        }

        fun start(c: Context) {
            if (BuildConfig.DEMO) return
            runCatching { ContextCompat.startForegroundService(c, Intent(c, Watch::class.java)) }
        }

        fun open(c: Context, practice: String?, code: Int): PendingIntent =
            PendingIntent.getActivity(c, code, Intent(c, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                if (practice != null) putExtra(MainActivity.EXTRA_PRACTICE, practice)
            }, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        private fun snoozeIntent(c: Context, min: Int, code: Int): PendingIntent =
            PendingIntent.getService(c, code, Intent(c, Watch::class.java).setAction(ACT_SNOOZE).putExtra("min", min), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        fun ask(c: Context, sound: Boolean) {
            val n = NotificationCompat.Builder(c, CH_ASK)
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle(tr("Шар затуманился", "The orb has misted over"))
                .setContentText(tr("Посмотри вдаль двадцать секунд", "Look far away for twenty seconds"))
                .setContentIntent(open(c, "far", 10))
                .addAction(0, tr("Вдаль", "Look far"), open(c, "far", 11))
                .addAction(0, tr("10 минут", "10 min"), snoozeIntent(c, 10, 12))
                .addAction(0, tr("Через час", "In an hour"), snoozeIntent(c, 60, 13))
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setTimeoutAfter(9 * 60_000)
                .apply { if (!sound) setSilent(true) }
                .build()
            runCatching { c.getSystemService(NotificationManager::class.java).notify(ID_ASK, n) }
        }

        fun note(c: Context, id: Int, ch: String, title: String, text: String, open: String?, sound: Boolean) {
            val n = NotificationCompat.Builder(c, ch)
                .setSmallIcon(R.drawable.ic_stat).setContentTitle(title).setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setContentIntent(open(c, open, 20 + id)).setAutoCancel(true)
                .apply { if (!sound) setSilent(true) }
                .build()
            runCatching { c.getSystemService(NotificationManager::class.java).notify(id, n) }
        }

        fun cancelAsk(c: Context) = c.getSystemService(NotificationManager::class.java).cancel(ID_ASK)
    }
}

/** Set while a practice is on screen: no reminders then. */
object Foreground {
    @Volatile var practising = false
    /** The app is on screen: the orb itself shows the mist, no note needed. */
    @Volatile var visible = false
}

/** Starts the watcher after a reboot or an update. */
class Boot : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        Lang.ru = c.resources.configuration.locales[0].language == "ru"
        if (c.container.game.now.onboarded) Watch.start(c)
    }
}
