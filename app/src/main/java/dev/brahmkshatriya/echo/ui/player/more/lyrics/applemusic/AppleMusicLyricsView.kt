package dev.brahmkshatriya.echo.ui.player.more.lyrics.applemusic

import android.os.Build
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

import dev.brahmkshatriya.echo.common.models.Lyrics

/**
 * Full Apple Music-style lyrics view with auto-scroll, blur, and depth-of-field focus.
 *
 * Adapted from SimpMusic's LyricsView.kt + AppleMusicLyricsView.kt
 * (MIT, Copyright (c) maxrave-dev), re-targeted to echo's architecture:
 *
 * - Takes echo's [Lyrics] model directly (Simple / Timed / WordByWord)
 * - `currentPosition` is a plain `Long` (echo's `PlayerViewModel.progress` is `Pair<Long, Long>`,
 *   pass `.first`)
 * - `onLineClick` receives the line's start time in milliseconds (seek target)
 * - `primaryColor` / `onBackgroundColor` come from echo's `UiViewModel.playerColors`
 *   (convert `PlayerColors.accent` / `.onBackground` Int → Compose `Color`)
 * - Auto-scroll uses [animateScrollAndCentralizeItem] with spring(dampingRatio = 0.9f, stiffness = 180f)
 * - `typo()` replaced with `MaterialTheme.typography`
 * - `Res.string.*` replaced with hardcoded strings
 * - No Hilt / Koin injection inside — all data arrives as parameters so the composable can be
 *   used from a ComposeView in echo's LyricsFragment
 *
 * @param lyrics echo's Lyrics object (lyrics.lyrics can be Simple, Timed, or WordByWord)
 * @param currentPosition current playback position in milliseconds
 * @param onLineClick callback receiving the clicked line's start time in ms (for seekTo)
 * @param primaryColor the player's accent colour (used for the active line if not white)
 * @param onBackgroundColor the player's on-background colour (used for inactive lines if not grey)
 * @param modifier additional modifier
 */
@Composable
fun AppleMusicLyricsView(
    lyrics: Lyrics?,
    currentPosition: Long,
    onLineClick: (Long) -> Unit,
    primaryColor: Color = Color.White,
    onBackgroundColor: Color = AppleMusicInactiveLineColor,
    modifier: Modifier = Modifier,
) {
    if (lyrics == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "No lyrics found",
                color = onBackgroundColor,
                textAlign = TextAlign.Center,
            )
        }
        return
    }

    // Extract the lyric list from echo's Lyrics model.
    val lyric = lyrics.lyrics
    val items: List<Lyrics.Item> =
        when (lyric) {
            is Lyrics.Simple -> listOf(Lyrics.Item(lyric.text, 0L, 0L))
            is Lyrics.Timed -> lyric.list
            is Lyrics.WordByWord -> lyric.list.flatten()
            null -> emptyList()
        }

    if (items.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "No lyrics available",
                color = onBackgroundColor,
                textAlign = TextAlign.Center,
            )
        }
        return
    }

    // Whether the lyrics are timed (and thus scrollable / clickable).
    val isSynced = lyric is Lyrics.Timed || lyric is Lyrics.WordByWord

    // Blur is supported on Android 12+ (RenderEffect). On older versions we fall back to
    // alpha-only focus, which still reads clearly.
    val blurSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val listState = rememberLazyListState()

    // AMLL drops blur to zero while the user is scrolling by hand — you are reading ahead at
    // that moment, and out-of-focus text is not readable. Dragged, not isScrollInProgress: the
    // latter is also true for the player's own animated scroll, which must stay blurred.
    val isDragging by listState.interactionSource.collectIsDraggedAsState()

    // Compute the current line index from the playback position.
    val currentLineIndex by remember(items) {
        derivedStateOf {
            if (!isSynced || currentPosition <= 0L) {
                -1
            } else {
                // Find the last line whose startTime <= currentPosition.
                var idx = -1
                for (i in items.indices) {
                    if (items[i].startTime <= currentPosition) {
                        idx = i
                    } else {
                        break
                    }
                }
                idx
            }
        }
    }

    // Auto-scroll: centralise the current line using a spring animation.
    LaunchedEffect(currentLineIndex, isSynced) {
        if (currentLineIndex > -1 && isSynced) {
            listState.animateScrollAndCentralizeItem(currentLineIndex)
        }
    }

    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxSize()
                .appleMusicVerticalFadeEdges(topFade = 28.dp, bottomFade = 18.dp),
    ) {
        // Apple keeps the sung line near the top even when it is the last line of the song —
        // which is only possible if there is empty space below it to scroll into. Without this
        // tail the closing lines pile up against the bottom edge.
        val tailPadding = if (blurSupported) maxHeight * 0.72f else maxHeight * 0.5f

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = tailPadding),
        ) {
            itemsIndexed(items) { index, item ->
                // Signed distance from the line being sung.
                val distanceFromCurrent =
                    if (currentLineIndex < 0) 0 else index - currentLineIndex

                val isCurrent = index == currentLineIndex && currentLineIndex >= 0

                AppleMusicLyricsLineItem(
                    originalWords = item.text,
                    translatedWords = null,
                    isCurrent = isCurrent,
                    onClick =
                        if (isSynced) {
                            { onLineClick(item.startTime) }
                        } else {
                            null
                        },
                    modifier =
                        Modifier.appleMusicLyricFocus(
                            distanceFromCurrent = distanceFromCurrent,
                            blurEnabled = blurSupported && !isDragging,
                            hasActiveLine = currentLineIndex >= 0,
                        ),
                )
            }
        }
    }
}
