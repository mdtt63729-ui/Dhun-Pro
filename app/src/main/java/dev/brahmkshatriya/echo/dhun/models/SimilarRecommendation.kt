/*
 * Dhun Project Original (2026)
 * Dhun
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package dev.brahmkshatriya.echo.dhun.models

import dev.brahmkshatriya.echo.dhun.innertube.models.YTItem
import dev.brahmkshatriya.echo.dhun.db.entities.LocalItem

data class SimilarRecommendation(
    val title: LocalItem,
    val items: List<YTItem>,
)
