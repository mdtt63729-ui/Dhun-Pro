/*
 * Dhun Project Original (2026)
 * Dhun
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package dev.brahmkshatriya.echo.dhun.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Cached AI-translated lyrics.
 *
 * Each row stores the translated lyrics for one (videoId, language) pair.
 * This avoids redundant API calls — once a song is translated, the result
 * is stored locally and served from cache on subsequent plays.
 *
 * The [translatedLyrics] field holds the raw LRC-format lyrics with
 * timestamps preserved from the original lyrics (the AI only translates
 * text; timing is copied programmatically).
 */
@Entity(tableName = "ai_translated_lyrics")
data class AITranslatedLyricsEntity(
    /** Composite key: "$videoId:$language" */
    @PrimaryKey val id: String,
    val videoId: String,
    val language: String,
    val translatedLyrics: String,
    val originalSyncType: String = "LINE_SYNCED",
    val createdAt: Long = System.currentTimeMillis(),
    val error: Boolean = false,
) {
    companion object {
        fun compositeId(videoId: String, language: String): String = "$videoId:$language"
    }
}
