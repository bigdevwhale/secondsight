package app.secondsight

import android.content.Intent
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.getValue
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.secondsight.data.Demo
import app.secondsight.domain.Lang
import app.secondsight.domain.Practice
import app.secondsight.svc.Cues
import app.secondsight.svc.Watch
import app.secondsight.ui.Onboarding
import app.secondsight.ui.Root
import app.secondsight.ui.SightTheme
import app.secondsight.ui.Ui
import java.lang.ref.WeakReference

class MainActivity : AppCompatActivity() {
    private var then: (() -> Unit)? = null
    private val perms = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { then?.invoke(); then = null }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BuildConfig.DEMO && BuildConfig.DEMO_LANG.isNotEmpty() && AppCompatDelegate.getApplicationLocales().toLanguageTags() != BuildConfig.DEMO_LANG) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(BuildConfig.DEMO_LANG)); return
        }
        Lang.ru = resources.configuration.locales[0].language == "ru"
        current = WeakReference(this)
        val c = container
        if (BuildConfig.DEMO) Demo.seedIfEmpty(c.game)
        val t = android.graphics.Color.TRANSPARENT
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(t), navigationBarStyle = SystemBarStyle.dark(t))
        Cues.init(this)
        if (c.game.now.onboarded) { Watch.start(this); c.game.arrive() }
        go(intent)
        setContent {
            val s by c.game.state.collectAsStateWithLifecycle()
            SightTheme { if (s.onboarded) Root(s) else Onboarding() }
        }
    }

    override fun onResume() {
        super.onResume()
        app.secondsight.svc.Foreground.visible = true
        Watch.cancelAsk(this)
        container.game.arrive()
    }

    override fun onPause() {
        app.secondsight.svc.Foreground.visible = false
        super.onPause()
    }

    /**
     * From a notification or the tile: `--es practice far|dark|…` opens that practice.
     * Debug builds also take `--ei strain N` (seconds of mist) and `--ei glimpses N`.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        go(intent)
    }

    private fun go(intent: Intent?) {
        intent ?: return
        if (BuildConfig.DEBUG) {
            val g = container.game
            if (intent.hasExtra("strain")) g.write { it.copy(strain = intent.getIntExtra("strain", 0)) }
            if (intent.hasExtra("glimpses")) g.write { it.copy(glimpses = intent.getIntExtra("glimpses", 0)) }
            if (intent.hasExtra("lessons")) g.write { it.copy(lessons = intent.getIntExtra("lessons", 0), lessonDay = 0) }
            if (intent.hasExtra("fast")) Ui.fast = intent.getBooleanExtra("fast", false)
            if (intent.hasExtra("tab")) Ui.tab = intent.getIntExtra("tab", 0)
            if (intent.getBooleanExtra("ask", false)) Watch.ask(this, true)
        }
        val p = intent.getStringExtra(EXTRA_PRACTICE)?.let { Practice.of(it) } ?: return
        intent.removeExtra(EXTRA_PRACTICE)
        if (container.game.now.onboarded) Ui.start(p)
    }

    companion object {
        const val EXTRA_PRACTICE = "practice"
        private var current: WeakReference<MainActivity>? = null

        /** Asks for [list] (those not granted yet), then runs [done] whatever the answer. */
        fun ask(list: List<String>, done: () -> Unit) {
            val a = current?.get() ?: return done()
            val missing = list.filter { androidx.core.content.ContextCompat.checkSelfPermission(a, it) != android.content.pm.PackageManager.PERMISSION_GRANTED }
            if (missing.isEmpty()) return done()
            a.then = done
            a.perms.launch(missing.toTypedArray())
        }
    }
}

/** Switches the app language: "" follows the phone, "ru" or "en" pins it. */
fun setLanguage(tag: String) = AppCompatDelegate.setApplicationLocales(if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag))
fun currentLanguage(): String = AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore('-')
