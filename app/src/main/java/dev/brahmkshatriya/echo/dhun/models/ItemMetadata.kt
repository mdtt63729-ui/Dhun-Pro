/*
 * Dhun Project Original (2026)
 * Dhun
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package dev.brahmkshatriya.echo.dhun.models

import androidx.compose.runtime.Immutable

@Immutable
data class ItemMetadata(
    val isLiked: Boolean = false,
    val isInLibrary: Boolean = false,
    val downloadState: Int? = null,
)
