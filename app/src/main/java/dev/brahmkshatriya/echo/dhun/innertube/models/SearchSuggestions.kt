/*
 * Dhun Project Original (2026)
 * Dhun
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package dev.brahmkshatriya.echo.dhun.innertube.models

data class SearchSuggestions(
    val queries: List<String>,
    val recommendedItems: List<YTItem>,
)
