/*
 * Dhun Project Original (2026)
 * Dhun
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package dev.brahmkshatriya.echo.dhun.lyrics

import android.content.Context
import dev.brahmkshatriya.echo.dhun.betterlyrics.BetterLyrics
import dev.brahmkshatriya.echo.dhun.constants.EnableBetterLyricsKey
import dev.brahmkshatriya.echo.dhun.utils.dataStore
import dev.brahmkshatriya.echo.dhun.utils.get

import dev.brahmkshatriya.echo.dhun.canvas.providers.GlobalLog
import android.util.Log

object BetterLyricsProvider : LyricsProvider {
    init {
        BetterLyrics.logger = { message ->
            GlobalLog.append(Log.INFO, "BetterLyrics", message)
        }
    }

    override val name = "BetterLyrics"

    override fun isEnabled(context: Context): Boolean = context.dataStore[EnableBetterLyricsKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        album: String?,
        duration: Int,
    ): Result<String> = BetterLyrics.getLyrics(title = title, artist = artist, album = null, durationSeconds = duration)

    override suspend fun getAllLyrics(
        id: String,
        title: String,
        artist: String,
        album: String?,
        duration: Int,
        callback: (String) -> Unit,
    ) {
        BetterLyrics.getAllLyrics(
            title = title,
            artist = artist,
            album = album,
            durationSeconds = duration,
            callback = callback,
        )
    }
}
