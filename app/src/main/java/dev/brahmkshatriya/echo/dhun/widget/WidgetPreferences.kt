/*
 * Dhun Project Original (2026)
 * Dhun
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package dev.brahmkshatriya.echo.dhun.widget

import android.content.Context
import androidx.datastore.preferences.core.edit
import dev.brahmkshatriya.echo.dhun.constants.WidgetBackgroundMode
import dev.brahmkshatriya.echo.dhun.constants.WidgetBackgroundModeKey
import dev.brahmkshatriya.echo.dhun.constants.WidgetCornerRadiusKey
import dev.brahmkshatriya.echo.dhun.constants.WidgetScrimOpacityKey
import dev.brahmkshatriya.echo.dhun.constants.WidgetShowProgressBarKey
import dev.brahmkshatriya.echo.dhun.utils.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * DataStore-backed preferences for the home-screen player widgets.
 *
 * Consumed by the widget settings UI (WidgetSettings.kt) and by the widget
 * renderers to decide background mode, scrim strength and corner radius.
 */
object WidgetPreferences {

    const val DEFAULT_SCRIM_OPACITY = 0.32f
    const val DEFAULT_CORNER_RADIUS = 24f

    fun backgroundModeFlow(context: Context): Flow<WidgetBackgroundMode> =
        context.dataStore.data.map { prefs ->
            when (prefs[WidgetBackgroundModeKey]) {
                WidgetBackgroundMode.BLUR.name -> WidgetBackgroundMode.BLUR
                WidgetBackgroundMode.DOMINANT_COLOR.name -> WidgetBackgroundMode.DOMINANT_COLOR
                WidgetBackgroundMode.SOLID.name -> WidgetBackgroundMode.SOLID
                else -> WidgetBackgroundMode.BLUR
            }
        }

    fun scrimOpacityFlow(context: Context): Flow<Float> =
        context.dataStore.data.map { prefs ->
            prefs[WidgetScrimOpacityKey] ?: DEFAULT_SCRIM_OPACITY
        }

    fun cornerRadiusFlow(context: Context): Flow<Float> =
        context.dataStore.data.map { prefs ->
            prefs[WidgetCornerRadiusKey] ?: DEFAULT_CORNER_RADIUS
        }

    fun showProgressBarFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { prefs ->
            prefs[WidgetShowProgressBarKey] ?: true
        }

    suspend fun setBackgroundMode(context: Context, mode: WidgetBackgroundMode) {
        context.dataStore.edit { prefs ->
            prefs[WidgetBackgroundModeKey] = mode.name
        }
    }

    suspend fun setScrimOpacity(context: Context, value: Float) {
        context.dataStore.edit { prefs ->
            prefs[WidgetScrimOpacityKey] = value.coerceIn(0f, 1f)
        }
    }

    suspend fun setCornerRadius(context: Context, value: Float) {
        context.dataStore.edit { prefs ->
            prefs[WidgetCornerRadiusKey] = value.coerceAtLeast(0f)
        }
    }

    suspend fun setShowProgressBar(context: Context, value: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[WidgetShowProgressBarKey] = value
        }
    }
}
