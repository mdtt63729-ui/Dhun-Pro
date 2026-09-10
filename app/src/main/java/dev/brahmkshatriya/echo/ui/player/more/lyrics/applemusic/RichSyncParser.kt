package dev.brahmkshatriya.echo.ui.player.more.lyrics.applemusic

import dev.brahmkshatriya.echo.common.models.Lyrics

/**
 * Represents a single word with its timing information for rich sync lyrics.
 *
 * Adapted from SimpMusic's RichSyncParser.kt (MIT, Copyright (c) maxrave-dev).
 */
data class WordTiming(
    val text: String,
    val startTimeMs: Long,
)

/**
 * Represents a parsed rich sync line with all word timings.
 */
data class ParsedRichSyncLine(
    val words: List<WordTiming>,
    val lineStartTimeMs: Long,
    val lineEndTimeMs: Long,
)

/**
 * Decodes common HTML entities in a string. Inline replacement for SimpMusic's
 * `decodeHtmlEntities` so this file stays self-contained.
 */
private fun decodeHtmlEntities(text: String): String {
    return text
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&apos;", "'")
        .replace("&nbsp;", " ")
}

/**
 * Parses rich sync lyrics format into word-level timing data.
 *
 * Expected format: `<MM:SS.mm> word <MM:SS.mm> word ...`
 * Example: `<00:16.62> Và <00:16.64> em <00:16.68> nói`
 *
 * @param words The rich sync string containing timestamps and words
 * @param lineStartTimeMs The start time of the line as a string
 * @param lineEndTimeMs The end time of the line as a string
 * @return [ParsedRichSyncLine] if parsing succeeds, null if parsing fails or input is invalid
 */
fun parseRichSyncWords(
    words: String,
    lineStartTimeMs: String,
    lineEndTimeMs: String,
): ParsedRichSyncLine? {
    if (words.isBlank()) return null

    // Regex to match timestamp only: <MM:SS.mm> or <MM:SS.mmm>
    val timestampRegex = Regex("""<(\d{2}):(\d{2})\.(\d{2,3})>""")

    val wordTimings = mutableListOf<WordTiming>()
    val timestamps = timestampRegex.findAll(words).toList()

    timestamps.forEachIndexed { index, match ->
        val (minutes, seconds, fraction) = match.destructured

        // Convert time to milliseconds
        // If 2 digits (centiseconds): multiply by 10 to get ms
        // If 3 digits (milliseconds): use as-is
        val fractionMs = fraction.toLongOrNull() ?: 0L
        val timeMs =
            (minutes.toLongOrNull() ?: 0L) * 60000L +
                (seconds.toLongOrNull() ?: 0L) * 1000L +
                if (fraction.length == 2) fractionMs * 10L else fractionMs

        // Extract text after this timestamp until the next timestamp (or end of string)
        val startPos = match.range.last + 1
        val endPos =
            if (index < timestamps.size - 1) {
                timestamps[index + 1].range.first
            } else {
                words.length
            }

        val textBetween = words.substring(startPos, endPos).trim()

        if (textBetween.isNotBlank()) {
            wordTimings.add(WordTiming(text = decodeHtmlEntities(textBetween), startTimeMs = timeMs))
        }
    }

    if (wordTimings.isEmpty()) return null

    val lineStart = lineStartTimeMs.toLongOrNull() ?: 0L
    val lineEnd = lineEndTimeMs.toLongOrNull() ?: Long.MAX_VALUE

    return ParsedRichSyncLine(
        words = wordTimings,
        lineStartTimeMs = lineStart,
        lineEndTimeMs = lineEnd,
    )
}

/**
 * Converts a [ParsedRichSyncLine] into a list of echo [Lyrics.Item]s, one per word.
 * Each item's startTime is the word's timestamp and endTime is the next word's timestamp
 * (or the line end time for the last word).
 */
fun ParsedRichSyncLine.toLyricsItems(): List<Lyrics.Item> {
    return words.mapIndexed { index, word ->
        val endTime = if (index < words.size - 1) words[index + 1].startTimeMs else lineEndTimeMs
        Lyrics.Item(
            text = word.text,
            startTime = word.startTimeMs,
            endTime = endTime,
        )
    }
}

/**
 * Strips rich sync timestamp markers from a string, leaving only the plain text.
 * Example: `<00:16.62> Và <00:16.64> em` → `Và em`
 */
fun String.stripRichSyncTimestamps(): String {
    return Regex("""<\d{2}:\d{2}\.\d{2,3}>""").replace(this, "").replace(Regex("\s+"), " ").trim()
}
