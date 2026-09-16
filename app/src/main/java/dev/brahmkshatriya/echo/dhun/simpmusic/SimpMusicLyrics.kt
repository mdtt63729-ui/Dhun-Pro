package dev.brahmkshatriya.echo.dhun.simpmusic

object SimpMusicLyrics {
    suspend fun getLyrics(videoId: String, duration: Int): Result<String> = Result.failure(IllegalStateException("SimpMusic provider unavailable"))
    suspend fun getAllLyrics(videoId: String, duration: Int, callback: (String) -> Unit) = Unit
}
