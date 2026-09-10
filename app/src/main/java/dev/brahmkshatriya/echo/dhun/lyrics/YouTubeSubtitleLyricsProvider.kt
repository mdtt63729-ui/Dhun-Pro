/*
 * Dhun Project Original (2026)
 * Dhun
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package dev.brahmkshatriya.echo.dhun.lyrics

import android.content.Context
import dev.brahmkshatriya.echo.dhun.innertube.YouTube

object YouTubeSubtitleLyricsProvider : LyricsProvider {
    override val name = "YouTube Subtitle"

    override fun isEnabled(context: Context) = true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        album: String?,
        duration: Int,
    ): Result<String> = YouTube.transcript(id)

    override suspend fun getAllLyrics(
        id: String,
        title: String,
        artist: String,
        album: String?,
        duration: Int,
        callback: (String) -> Unit,
    ) {
        YouTube.transcript(id).onSuccess(callback)
    }
}
