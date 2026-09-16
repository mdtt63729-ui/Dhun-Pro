package dev.brahmkshatriya.echo.dhun.lrclib

object LrcLib {
    suspend fun getLyrics(title: String, artist: String, duration: Int): Result<String> = Result.failure(IllegalStateException("LRCLIB provider unavailable"))
    suspend fun getAllLyrics(title: String, artist: String, duration: Int, album: String?, callback: (String) -> Unit) = Unit
}
