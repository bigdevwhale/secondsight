package app.secondsight

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import app.secondsight.data.Demo
import app.secondsight.data.Shown
import app.secondsight.domain.Lang
import app.secondsight.domain.Practice
import app.secondsight.domain.Ritual
import app.secondsight.domain.Tales
import app.secondsight.ui.Onboarding
import app.secondsight.ui.Root
import app.secondsight.ui.SightTheme
import app.secondsight.ui.Still
import app.secondsight.ui.Ui
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** The main screens over the demo weeks, drawn to PNGs (run with `-Prender --tests '*RenderScreens*'`), in both languages. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
abstract class ScreensBase {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    abstract val lang: String
    private val out by lazy { File(System.getProperty("render.out") ?: "build/art", "screens-$lang").apply { mkdirs() } }
    private val game get() = rule.activity.container.game

    @Before fun still() {
        Still.on = true
        Lang.ru = lang == "ru"
        Ui.tab = 0; Ui.play = emptyList(); Ui.vision = null; Ui.settings = false; Ui.toast = null; Ui.pathEnd = null
    }

    private fun shot(name: String, content: @Composable () -> Unit) {
        rule.mainClock.autoAdvance = false
        rule.setContent { SightTheme { content() } }
        rule.mainClock.advanceTimeBy(2_000)
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(4_000)
        rule.runOnUiThread {
            val view = rule.activity.window.decorView
            val image = android.graphics.Bitmap.createBitmap(view.width, view.height, android.graphics.Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(image))
            File(out, "$name.png").outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    private fun root(name: String, setup: () -> Unit = {}) {
        Demo.seedIfEmpty(game)
        setup()
        shot(name) { Root(game.now) }
    }

    @Test fun hall() = root("1-hall")
    @Test fun hallMist() = root("1b-hall-mist") { game.write { it.copy(strain = 25 * 60, glimpses = 3) } }
    @Test fun practices() = root("2-practices") { Ui.tab = 1 }
    @Test fun pathEnd() = root("11-path-end") { Ui.pathEnd = 1 }
    @Test fun scrolls() = root("3-scrolls") { Ui.tab = 2 }
    @Test fun path() = root("4-path") { Ui.tab = 3 }
    @Test fun settings() = root("5-settings") { Ui.settings = true }
    @Test fun track() = root("6-track-intro") { Ui.play = listOf(Practice.TRACK) }
    @Test fun dark() = root("7-dark-intro") { Ui.play = listOf(Practice.DARK) }
    @Test fun far() = root("8-far") { Ui.play = listOf(Practice.FAR) }
    @Test fun ritual() = root("9-ritual") { Ui.play = Ritual.MORNING.steps }
    @Test fun vision() = root("10-vision") { Ui.vision = Shown(Tales.all[0], 0) }
    @Test fun onboarding() = shot("0-onboarding") { Onboarding() }

    /** The launcher icon at 512 px. */
    @Test fun icon() {
        val ctx = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val d = androidx.core.content.ContextCompat.getDrawable(ctx, R.mipmap.ic_launcher)!!
        val image = android.graphics.Bitmap.createBitmap(512, 512, android.graphics.Bitmap.Config.ARGB_8888)
        val c = android.graphics.Canvas(image)
        c.clipPath(android.graphics.Path().apply { addRoundRect(0f, 0f, 512f, 512f, 112f, 112f, android.graphics.Path.Direction.CW) })
        d.setBounds(0, 0, 512, 512); d.draw(c)
        File(out.parentFile, "icon.png").outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }
}

@Config(sdk = [34], qualifiers = "ru-w393dp-h873dp-xxhdpi")
class RenderScreensRu : ScreensBase() { override val lang = "ru" }

@Config(sdk = [34], qualifiers = "en-w393dp-h873dp-xxhdpi")
class RenderScreensEn : ScreensBase() { override val lang = "en" }
