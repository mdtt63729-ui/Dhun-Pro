package dev.brahmkshatriya.echo.dhun.kugou

object KuGou {
    var useTraditionalChinese: Boolean = false
    suspend fun getLyrics(title: String, artist: String, duration: Int): Result<String> = Result.failure(IllegalStateException("Kugou provider unavailable"))
    suspend fun getAllPossibleLyricsOptions(title: String, artist: String, duration: Int, callback: (String) -> Unit) = Unit
}
