/*
 * Dhun Project Original (2026)
 * Dhun
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package dev.brahmkshatriya.echo.dhun.innertube.pages

import dev.brahmkshatriya.echo.dhun.innertube.models.YTItem

data class ArtistItemsContinuationPage(
    val items: List<YTItem>,
    val continuation: String?,
)
