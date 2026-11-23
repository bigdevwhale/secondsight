package app.secondsight

import android.app.Application
import android.content.Context
import android.content.Intent
import android.service.quicksettings.TileService
import app.secondsight.data.Game
import app.secondsight.domain.Lang
import app.secondsight.svc.Watch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class Container(context: Context) {
    val app: Context = context.applicationContext
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val game = Game(app, scope)
}

class App : Application() {
    lateinit var container: Container
        private set

    override fun onCreate() {
        super.onCreate()
        Lang.ru = resources.configuration.locales[0].language == "ru"
        container = Container(this)
        container.game.load()
        Watch.channels(this)
        if (container.game.now.onboarded) Watch.start(this)
    }
}

val Context.container: Container get() = (applicationContext as App).container

/** The quick-settings tile: straight into the Darkness. */
class DarkTile : TileService() {
    override fun onClick() {
        super.onClick()
        val i = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra(MainActivity.EXTRA_PRACTICE, "dark")
        if (android.os.Build.VERSION.SDK_INT >= 34) {
            startActivityAndCollapse(android.app.PendingIntent.getActivity(this, 40, i, android.app.PendingIntent.FLAG_IMMUTABLE))
        } else {
            @Suppress("DEPRECATION", "StartActivityAndCollapseDeprecated")
            startActivityAndCollapse(i)
        }
    }
}
