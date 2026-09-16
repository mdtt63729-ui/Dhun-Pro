/*
 * Dhun Project Original (2026)
 * Dhun
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package dev.brahmkshatriya.echo.dhun.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.dhun.playback.MusicService
import dev.brahmkshatriya.echo.widget.WidgetCircle
import dev.brahmkshatriya.echo.widget.WidgetHorizontal
import dev.brahmkshatriya.echo.widget.WidgetVertical

/**
 * Pushes [PlayerWidgetState] snapshots into the app's home-screen widgets.
 *
 * The app already ships three [android.appwidget.AppWidgetProvider]
 * implementations (WidgetCircle / WidgetHorizontal / WidgetVertical, built on
 * the shared `BaseWidget` rendering pipeline that talks to a MediaController).
 * This updater performs a direct `AppWidgetManager.updateAppWidget` pass with
 * RemoteViews built from each provider's default layout so the widgets reflect
 * the current track even when no controller is connected yet, and wires the
 * widget buttons to [MusicService] via [PlayerWidgetActions] intents.
 */
object PlayerWidgetUpdater {

    /** Provider class -> layout used for the compact textual update pass. */
    private val providerLayouts: List<Pair<Class<*>, Int>> = listOf(
        WidgetCircle::class.java to R.layout.widget_circle,
        WidgetHorizontal::class.java to R.layout.widget_horizontal_narrow_short,
        WidgetVertical::class.java to R.layout.widget_vertical_small,
    )

    fun update(context: Context, state: PlayerWidgetState) {
        val appContext = context.applicationContext
        val appWidgetManager = AppWidgetManager.getInstance(appContext) ?: return

        for ((providerClass, layoutRes) in providerLayouts) {
            val provider = ComponentName(appContext, providerClass)
            val ids = appWidgetManager.getAppWidgetIds(provider)
            if (ids.isNullOrEmpty()) continue

            val views = buildViews(appContext, providerClass, layoutRes, state)
            appWidgetManager.updateAppWidget(provider, views)
        }
    }

    private fun buildViews(
        context: Context,
        providerClass: Class<*>,
        layoutRes: Int,
        state: PlayerWidgetState,
    ): RemoteViews {
        val views = RemoteViews(context.packageName, layoutRes)

        val hasTrack = !state.title.isNullOrBlank()
        views.setTextViewText(
            R.id.trackTitle,
            state.title ?: context.getString(R.string.so_empty),
        )
        views.setTextViewText(
            R.id.trackArtist,
            state.artist?.takeIf { it.isNotBlank() },
        )

        // Buttons target the playback service, which applies them in
        // MusicService.handlePlayerWidgetAction().
        views.setOnClickPendingIntent(
            R.id.playPauseButton,
            servicePendingIntent(context, PlayerWidgetActions.ACTION_PLAY_PAUSE),
        )
        views.setOnClickPendingIntent(
            R.id.nextButton,
            servicePendingIntent(context, PlayerWidgetActions.ACTION_NEXT),
        )
        views.setOnClickPendingIntent(
            R.id.previousButton,
            servicePendingIntent(context, PlayerWidgetActions.ACTION_PREVIOUS),
        )

        views.setImageViewResource(
            R.id.playPauseButton,
            if (state.isPlaying) R.drawable.ic_pause_48dp else R.drawable.ic_play_48dp,
        )
        views.setFloat(R.id.playPauseButton, "setAlpha", if (hasTrack) 1f else 0.5f)
        views.setFloat(R.id.nextButton, "setAlpha", if (hasTrack) 1f else 0.5f)
        views.setFloat(R.id.previousButton, "setAlpha", if (hasTrack) 1f else 0.5f)

        return views
    }

    private fun servicePendingIntent(context: Context, action: String): PendingIntent {
        val intent = Intent(context, MusicService::class.java).apply { this.action = action }
        return PendingIntent.getService(
            context,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
