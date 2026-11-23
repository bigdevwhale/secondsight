package app.secondsight.svc

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews
import app.secondsight.R
import app.secondsight.container
import app.secondsight.domain.tr

/** The orb on the home screen: its mist and the minutes until looking far; a tap starts looking far. */
class OrbWidget : AppWidgetProvider() {
    override fun onUpdate(c: Context, m: AppWidgetManager, ids: IntArray) = update(c)

    companion object {
        private var last = ""

        fun update(c: Context) {
            val s = c.container.game.now
            val mist = (s.strain.toFloat() / (s.interval * 60)).coerceIn(0f, 1f)
            val left = (s.interval * 60 - s.strain) / 60
            val text = when {
                !s.onboarded -> tr("Второе зрение", "Second Sight")
                mist >= 1f -> tr("Посмотри вдаль", "Look far away")
                else -> tr("вдаль через $left мин", "look far in $left min")
            }
            val key = "$text/${(mist * 20).toInt()}"
            if (key == last) return
            last = key
            val v = RemoteViews(c.packageName, R.layout.widget).apply {
                setTextViewText(R.id.w_text, text)
                setInt(R.id.w_mist, "setImageAlpha", (mist * mist * 235).toInt())
                setOnClickPendingIntent(R.id.w_root, Watch.open(c, if (mist >= 0.5f) "far" else null, 50))
            }
            val m = AppWidgetManager.getInstance(c)
            runCatching { m.updateAppWidget(ComponentName(c, OrbWidget::class.java), v) }
        }
    }
}
