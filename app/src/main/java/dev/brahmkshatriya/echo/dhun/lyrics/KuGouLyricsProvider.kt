/*
 * Dhun Project Original (2026)
 * Dhun
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package dev.brahmkshatriya.echo.dhun.lyrics

import android.content.Context
import dev.brahmkshatriya.echo.dhun.kugou.KuGou
import dev.brahmkshatriya.echo.dhun.constants.EnableKugouKey
import dev.brahmkshatriya.echo.dhun.utils.dataStore
import dev.brahmkshatriya.echo.dhun.utils.get

object KuGouLyricsProvider : LyricsProvider {
    override val name = "Kugou"
    override fun isEnabled(context: Context): Boolean =
        context.dataStore[EnableKugouKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        album: String?,
        duration: Int,
    ): Result<String> = KuGou.getLyrics(title, artist, duration)

    override suspend fun getAllLyrics(
        id: String,
        title: String,
        artist: String,
        album: String?,
        duration: Int,
        callback: (String) -> Unit,
    ) {
        KuGou.getAllPossibleLyricsOptions(title, artist, duration, callback)
    }
}
