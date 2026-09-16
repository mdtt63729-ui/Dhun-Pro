package dev.brahmkshatriya.echo.dhun.lastfm

object LastFM {
    const val DEFAULT_SCROBBLE_DELAY_PERCENT = 50
    const val DEFAULT_SCROBBLE_MIN_SONG_DURATION = 30
    const val DEFAULT_SCROBBLE_DELAY_SECONDS = 30
    var sessionKey: String? = null
    private var apiKey: String = ""
    private var secret: String = ""
    fun initialize(apiKey: String, secret: String) { this.apiKey = apiKey; this.secret = secret }
    suspend fun scrobble(artist: String, track: String, duration: Long?, timestamp: Long, album: String?) = Unit
    suspend fun updateNowPlaying(artist: String, track: String, album: String?, duration: Long?) = Unit
}
