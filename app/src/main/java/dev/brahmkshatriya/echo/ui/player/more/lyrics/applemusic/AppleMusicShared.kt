package dev.brahmkshatriya.echo.ui.player.more.lyrics.applemusic

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme

/**
 * Shared types, constants, and modifier utilities for the Apple Music-style lyrics view.
 *
 * Adapted from SimpMusic's AppleMusicShared.kt and AppleMusicLyricsLines.kt
 * (MIT, Copyright (c) maxrave-dev), re-targeted to echo's architecture:
 * - `typo()` replaced with `MaterialTheme.typography`
 * - Koin DI retained (echo already uses Koin)
 * - SimpMusic platform abstractions removed (echo is Android-only)
 * - `Res.string.*` replaced with hardcoded strings
 */

// ══════════════════════════════════════════════════════════════════════════════════════════════
//  Font sizes / dimensions  (from AppleMusicLyricsLines.kt + AMLL stylesheet)
// ══════════════════════════════════════════════════════════════════════════════════════════════

/**
 * Apple sets lyrics far larger than body copy — a line fills most of the width and wraps after
 * a few words, which is what makes the page readable at arm's length while the phone sits on a
 * desk.
 */
val AppleMusicLyricFontSize = 28.sp

/** Leading INSIDE a wrapped line — tighter than the padding between two lines. */
val AppleMusicLyricLineHeight = 34.sp

/** Vertical gap between two different lyric items. */
val AppleMusicLyricGap = 20.dp

/** Translation / sub-line font size (from AMLL's .lyricSubLine { font-size: max(0.5em, 10px) }). */
val AppleMusicSubLineFontSize = 14.sp
val AppleMusicSubLineHeight = 21.sp

/** Gap between the main lyric and its translation/romanization. */
val AppleMusicMainToSubGap = 12.dp

/** Horizontal gutter applied to every lyric line. */
val AppleMusicLyricPaddingX = 20.dp

/** Corner radius for the press-state background panel. */
val AppleMusicLyricCornerRadius = 8.dp

/** Tinted panel behind the whole wrapper when pressed (not a ripple). */
val AppleMusicLyricPressedBackground = Color.White.copy(alpha = 0.067f)

/** Explicit row spacing for FlowRow-based rich-sync lines (tighter than AppleMusicLyricGap). */
val AppleMusicWrappedLineSpacing = 2.dp

// ══════════════════════════════════════════════════════════════════════════════════════════════
//  Colours
// ══════════════════════════════════════════════════════════════════════════════════════════════

/** White, slightly under full opacity so the translation reads as secondary. */
val AppleMusicTranslatedColor = Color.White.copy(alpha = 0.78f)

/** A shade under the translation's: recedes first once you know the song. */
val AppleMusicRomanizedColor = Color.White.copy(alpha = 0.62f)

/** The line NOT being sung is grey — white at reduced opacity is still white on a dark page. */
val AppleMusicInactiveLineColor = Color(0xFF9B9B9B)

/** The unsung remainder of the line currently being sung (e.g. "ce" in "dan|ce"). */
val AppleMusicPendingWordColor = Color(0xFF8E8E8E)

/** Secondary text colour for compact headers / captions. */
val AppleMusicTextSecondary = Color.White.copy(alpha = 0.72f)

/** Inactive pill background. */
val AppleMusicPillInactive = Color.White.copy(alpha = 0.24f)

// ══════════════════════════════════════════════════════════════════════════════════════════════
//  Typography
// ══════════════════════════════════════════════════════════════════════════════════════════════

@Immutable
data class AppleMusicTypography(
    val mainTitle: TextStyle,
    val mainArtist: TextStyle,
    val compactTitle: TextStyle,
    val compactArtist: TextStyle,
    val times: TextStyle,
    val badge: TextStyle,
    val footer: TextStyle,
    val idleLyric: TextStyle,
    val idleTranslated: TextStyle,
)

/**
 * Maps every Apple Music text slot onto [MaterialTheme.typography] roles.
 * Replaces SimpMusic's `typo()` with echo's `MaterialTheme.typography`.
 */
@Composable
fun rememberAppleMusicTypography(): AppleMusicTypography {
    val t = MaterialTheme.typography
    return AppleMusicTypography(
        mainTitle = t.titleMedium,
        mainArtist = t.bodyMedium,
        compactTitle = t.titleMedium.copy(fontSize = t.labelSmall.fontSize),
        compactArtist = t.bodySmall,
        times = t.bodyMedium,
        badge = t.bodySmall,
        footer = t.bodySmall,
        idleLyric = t.bodyMedium.copy(color = Color.White),
        idleTranslated = t.bodyMedium.copy(color = Color.Yellow),
    )
}

// ══════════════════════════════════════════════════════════════════════════════════════════════
//  Modifier utilities
// ══════════════════════════════════════════════════════════════════════════════════════════════

/**
 * True alpha fade at the top/bottom edges of a scrolling region (DstIn mask): content dissolves
 * into whatever is behind it without painting a color and without touching the wrapped component.
 */
fun Modifier.appleMusicVerticalFadeEdges(
    topFade: Dp,
    bottomFade: Dp,
): Modifier =
    graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            val topPx = topFade.toPx().coerceAtMost(size.height / 2f)
            val bottomPx = bottomFade.toPx().coerceAtMost(size.height / 2f)
            val topStop = if (size.height > 0f) topPx / size.height else 0f
            val bottomStop = if (size.height > 0f) 1f - bottomPx / size.height else 1f
            drawRect(
                brush =
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        topStop to Color.Black,
                        bottomStop to Color.Black,
                        1f to Color.Transparent,
                    ),
                blendMode = BlendMode.DstIn,
            )
        }

/**
 * Press-to-swell like the liquid-glass buttons: the control springs up while a finger is on it
 * and settles back on release.
 */
@Composable
fun Modifier.appleMusicPressInflate(pressedScale: Float = 1.35f): Modifier {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 380f),
        label = "appleMusicPressInflate",
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }.pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                pressed = true
                waitForUpOrCancellation()
                pressed = false
            }
        }
}

// ══════════════════════════════════════════════════════════════════════════════════════════════
//  LazyListState scroll helpers  (from SimpMusic's UIExt.kt)
// ══════════════════════════════════════════════════════════════════════════════════════════════

/**
 * Smoothly scrolls the list so that the item at [index] is centred in the viewport.
 *
 * Uses a spring animation (dampingRatio = 0.9f, stiffness = 180f) so the page glides with the
 * song instead of snapping — a spring is driven by the distance itself, so a one-line step and
 * a six-line jump after a seek both settle naturally.
 *
 * If the target item is not currently visible, it jumps close first so layoutInfo updates on
 * the next frame.
 */
suspend fun LazyListState.animateScrollAndCentralizeItem(index: Int) {
    if (index < 0) return

    // If target item is not currently visible, jump close to it first so layoutInfo updates next frame.
    val initiallyVisible = this.layoutInfo.visibleItemsInfo.any { it.index == index }
    if (!initiallyVisible) {
        this.scrollToItem(index)
    }

    // Wait for one frame so visibleItemsInfo reflects the latest layout pass.
    withFrameNanos { }

    val itemInfo =
        this.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index } ?: return
    val viewportStart = this.layoutInfo.viewportStartOffset
    val viewportEnd = this.layoutInfo.viewportEndOffset
    val viewportCenter = (viewportStart + viewportEnd) / 2
    val itemCenter = itemInfo.offset + itemInfo.size / 2

    this.animateScrollBy(
        value = (itemCenter - viewportCenter).toFloat(),
        animationSpec = spring(dampingRatio = 0.9f, stiffness = 180f),
    )
}

/**
 * Smoothly scrolls the list so that the item at [index] is anchored near the top of the
 * viewport, with [extraOffsetPx] added to the scroll (negative leaves that much of the previous
 * content visible above [index]).
 *
 * Also uses a spring animation — gentler than the centralising version on purpose: this runs on
 * every lyric line, and a fixed-duration tween reads as a jolt once per line.
 */
suspend fun LazyListState.animateScrollAndAnchorItemTop(
    index: Int,
    extraOffsetPx: Float = 0f,
) {
    if (index < 0) return

    val initiallyVisible = this.layoutInfo.visibleItemsInfo.any { it.index == index }
    if (!initiallyVisible) {
        this.scrollToItem(index)
    }

    withFrameNanos { }

    val itemInfo =
        this.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index } ?: return

    this.animateScrollBy(
        value = (itemInfo.offset - this.layoutInfo.viewportStartOffset).toFloat() + extraOffsetPx,
        animationSpec = spring(dampingRatio = 0.9f, stiffness = 180f),
    )
}

// ══════════════════════════════════════════════════════════════════════════════════════════════
//  Gradient helper
// ══════════════════════════════════════════════════════════════════════════════════════════════

/**
 * The page gradient's colour at [fraction] of the screen height: the seed barely darkened at
 * the top, ~32%-darkened by mid-page (48%), ~78%-darkened at the bottom.
 */
fun appleMusicGradientColorAt(
    seedColor: Color,
    fraction: Float,
): Color {
    val top = lerp(seedColor, Color.Black, 0.05f)
    val mid = lerp(seedColor, Color.Black, 0.32f)
    val bottom = lerp(seedColor, Color.Black, 0.78f)
    return if (fraction <= 0.48f) {
        lerp(top, mid, (fraction / 0.48f).coerceIn(0f, 1f))
    } else {
        lerp(mid, bottom, ((fraction - 0.48f) / 0.52f).coerceIn(0f, 1f))
    }
}
