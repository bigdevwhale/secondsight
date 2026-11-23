package app.secondsight.svc

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import app.secondsight.domain.Lang
import java.util.Locale
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Signals for closed or far-looking eyes: a small temple bell, a touch of vibration, and the seer's voice
 * (the phone's speech engine), so the screen never has to be checked.
 */
object Cues {
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var bell: ShortArray? = null

    fun init(c: Context) {
        if (tts != null) return
        tts = TextToSpeech(c.applicationContext) { st ->
            ttsReady = st == TextToSpeech.SUCCESS
            if (ttsReady) tts?.language = if (Lang.ru) Locale.forLanguageTag("ru") else Locale.UK
            tts?.setSpeechRate(0.9f); tts?.setPitch(0.95f)
        }
    }

    fun say(text: String) {
        if (!ttsReady) return
        tts?.language = if (Lang.ru) Locale.forLanguageTag("ru") else Locale.UK
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "cue")
    }

    fun hush() { tts?.stop() }

    /** A soft bowl-bell: a few inharmonic partials dying away over two seconds. */
    private fun bellPcm(): ShortArray {
        bell?.let { return it }
        val rate = 44100; val n = rate * 2
        val parts = listOf(528.0 to 1.0, 1351.0 to 0.45, 2420.0 to 0.18, 3920.0 to 0.06)
        val out = ShortArray(n)
        for (i in 0 until n) {
            val t = i.toDouble() / rate
            var v = 0.0
            for ((f, a) in parts) v += a * sin(2 * PI * f * t) * exp(-t * (1.6 + f / 1400))
            val attack = (t / 0.004).coerceAtMost(1.0)
            out[i] = (v * attack * 0.32 * Short.MAX_VALUE).toInt().toShort()
        }
        return out.also { bell = it }
    }

    fun ring(soft: Boolean = false) {
        val pcm = bellPcm()
        thread(name = "bell") {
            runCatching {
                val tr = AudioTrack.Builder()
                    .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                    .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(44100).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                    .setBufferSizeInBytes(pcm.size * 2).setTransferMode(AudioTrack.MODE_STATIC).build()
                tr.write(pcm, 0, pcm.size)
                tr.setVolume(if (soft) 0.4f else 0.8f)
                tr.play()
                Thread.sleep(2200)
                tr.release()
            }
        }
    }

    private var click: ShortArray? = null

    /** A short wooden click: the beat for a change that needs no bell. */
    fun tick() {
        val pcm = click ?: ShortArray(44100 / 20) { i ->
            val t = i / 44100.0
            ((sin(2 * PI * 1600 * t) * 0.6 + sin(2 * PI * 900 * t) * 0.4) * exp(-t * 90) * 0.5 * Short.MAX_VALUE).toInt().toShort()
        }.also { click = it }
        thread(name = "tick") {
            runCatching {
                val tr = AudioTrack.Builder()
                    .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                    .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(44100).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                    .setBufferSizeInBytes(pcm.size * 2).setTransferMode(AudioTrack.MODE_STATIC).build()
                tr.write(pcm, 0, pcm.size)
                tr.play()
                Thread.sleep(200)
                tr.release()
            }
        }
    }

    fun buzz(c: Context, ms: Long = 60, twice: Boolean = false) {
        val v = if (Build.VERSION.SDK_INT >= 31) (c.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        else @Suppress("DEPRECATION") (c.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator)
        runCatching {
            v.vibrate(if (twice) VibrationEffect.createWaveform(longArrayOf(0, ms, 120, ms), -1) else VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }
}
