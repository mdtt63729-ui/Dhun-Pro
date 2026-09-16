package dev.brahmkshatriya.echo.dhun.ui.screens.settings

import android.content.Context
import androidx.navigation.NavHostController
import dev.brahmkshatriya.echo.dhun.spotify.SpotifyAccountSession

enum class DarkMode { AUTO, ON, OFF }
enum class LyricsPosition { LEFT, CENTER, RIGHT }
enum class NavigationTab { HOME, SEARCH, LIBRARY }

enum class AccountSettings { ACCOUNT }

object DiscordPresenceManager {
    private var running = false
    fun isRunning() = running
    fun start(context: Context, token: String, songProvider: () -> Any?, positionProvider: () -> Long, isPausedProvider: () -> Boolean, intervalProvider: () -> Long) { running = token.isNotBlank() }
    fun stop() { running = false }
    fun restart(): Boolean { return running }
    fun updateNow(context: Context, token: String, song: Any?, positionMs: Long, isPaused: Boolean): Boolean = running
}

object ListenBrainzManager {
    suspend fun submitPlayingNow(context: Context, token: String, song: Any?, positionMs: Long) = Unit
    suspend fun submitFinished(context: Context, token: String, song: Any?, startMs: Long, endMs: Long) = Unit
}

object ThemePalettes {
    data class Palette(val id: String, val primary: androidx.compose.ui.graphics.Color, val secondary: androidx.compose.ui.graphics.Color, val tertiary: androidx.compose.ui.graphics.Color, val neutral: androidx.compose.ui.graphics.Color)
    private val palettes = listOf(
        Palette("default", androidx.compose.ui.graphics.Color(0xFFED5564), androidx.compose.ui.graphics.Color(0xFF9C4D5A), androidx.compose.ui.graphics.Color(0xFF7D5260), androidx.compose.ui.graphics.Color(0xFF777777)),
        Palette("ocean", androidx.compose.ui.graphics.Color(0xFF4267B2), androidx.compose.ui.graphics.Color(0xFF4F7CAC), androidx.compose.ui.graphics.Color(0xFF5C6BC0), androidx.compose.ui.graphics.Color(0xFF6F7888)),
        Palette("violet", androidx.compose.ui.graphics.Color(0xFF7C4DFF), androidx.compose.ui.graphics.Color(0xFF9C5DEB), androidx.compose.ui.graphics.Color(0xFFB05CE6), androidx.compose.ui.graphics.Color(0xFF777080),
        )
    )
    fun findById(id: String): Palette? = palettes.firstOrNull { it.id == id }
    fun generateRandomPalette(): String = "seedPalette:default"
}

@androidx.compose.runtime.Composable
fun AccountSettings(navController: NavHostController, onClose: () -> Unit, latestVersionName: String) { }
