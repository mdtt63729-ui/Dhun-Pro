package dev.brahmkshatriya.echo.ui.player.more.lyrics.applemusic

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

import kotlin.math.abs

/**
 * Apple Music-style per-line rendering: blur, alpha, and scale based on position.
 *
 * Adapted from SimpMusic's AppleMusicLyricsLines.kt (MIT, Copyright (c) maxrave-dev).
 *
 * Apple Music renders EVERY lyric line at the same size — what separates the line being sung
 * from the rest is focus, not scale: the active line is opaque and sharp, its neighbours dim
 * and progressively blurred with distance, as if the page had a shallow depth of field.
 *
 * Key adaptations for echo:
 * - `typo()` replaced with `MaterialTheme.typography`
 * - Desktop platform check removed (echo is Android-only; always uses BlurredEdgeTreatment.Unbounded)
 * - Colours and constants are shared via [AppleMusicShared.kt]
 */

// ══════════════════════════════════════════════════════════════════════════════════════════════
//  Focus constants
// ══════════════════════════════════════════════════════════════════════════════════════════════

// The DISTANCE rule is AMLL's (resolveBlurLevel in base/index.ts): the sung line is exempt
// outright, and lines already sung carry a +1 so the page behind the singer recedes faster
// than the page ahead — you are meant to read forward, not back.
//
// Expressed against the font size rather than in fixed dp, so changing the type size keeps the
// depth of field proportional.
private const val BLUR_PER_LINE_EM = 0.095f
private const val BLUR_MAX_EM = 0.45f

// Alpha: the active line is opaque, neighbours dim with distance. Blur and this work together
// rather than either doing the job alone: blur alone leaves far lines as bright smears, fade
// alone leaves them sharp and readable when they should not be.
private const val ACTIVE_LINE_ALPHA = 1f
private const val ALPHA_FALLOFF_PER_LINE = 0.25f
private const val MIN_LINE_ALPHA = 0.25f

// Before the FIRST line is due — an intro, a long instrumental opening — there is no sung line
// for anything to be near, so distance is meaningless. Lines are dimmed uniformly and NOT
// blurred: blur means "far from where we are in the song", and during an intro nowhere is where
// we are. It also has to stay readable, since this is exactly when someone glances down.
private const val PRE_ROLL_LINE_ALPHA = 0.6f

// ══════════════════════════════════════════════════════════════════════════════════════════════
//  Depth-of-field modifier
// ══════════════════════════════════════════════════════════════════════════════════════════════

/**
 * Depth-of-field treatment for one lyric line. [distanceFromCurrent] is signed line distance
 * from the line being sung; its magnitude drives both blur and dimming.
 *
 * Both values are animated rather than applied outright, so the focus glides down the page with
 * the song instead of snapping.
 *
 * @param distanceFromCurrent signed distance: 0 = active line, positive = upcoming, negative = sung
 * @param blurEnabled whether blur is supported (Android 12+ / RenderEffect)
 * @param hasActiveLine whether any line is currently active (false during pre-roll)
 */
@Composable
fun Modifier.appleMusicLyricFocus(
    distanceFromCurrent: Int,
    blurEnabled: Boolean,
    hasActiveLine: Boolean = true,
): Modifier {
    // Signed: negative means this line has already been sung. AMLL adds one to that side so the
    // page behind the singer recedes faster than the page ahead of it.
    val distance =
        when {
            distanceFromCurrent < 0 -> abs(distanceFromCurrent) + 1
            else -> distanceFromCurrent
        }

    val fontSizeDp = with(LocalDensity.current) { AppleMusicLyricFontSize.toDp() }

    val targetBlur: Dp =
        if (!blurEnabled || !hasActiveLine || distanceFromCurrent == 0) {
            0.dp
        } else {
            fontSizeDp * (distance * BLUR_PER_LINE_EM).coerceAtMost(BLUR_MAX_EM)
        }

    val targetAlpha =
        when {
            // Checked FIRST: the caller passes distance 0 for every line while no line is active,
            // and 0 otherwise means "this is the sung line". Read in the other order, the whole
            // sheet would take the sung line's full opacity during an intro.
            !hasActiveLine -> PRE_ROLL_LINE_ALPHA
            distanceFromCurrent == 0 -> ACTIVE_LINE_ALPHA
            else -> (1f - distance * ALPHA_FALLOFF_PER_LINE).coerceAtLeast(MIN_LINE_ALPHA)
        }

    val blurRadius by animateDpAsState(
        targetValue = targetBlur,
        animationSpec = tween(400),
        label = "appleMusicLyricBlur",
    )
    val lineAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(400),
        label = "appleMusicLyricAlpha",
    )

    // alpha BEFORE blur: blurring an already-faded line keeps the two effects independent,
    // whereas fading a blurred layer washes the blur out into a flat smear.
    return this
        .alpha(lineAlpha)
        .then(
            if (blurRadius > 0.dp) {
                // Unbounded, NOT the default. blur(radius) alone uses BlurredEdgeTreatment.Rectangle,
                // which clips the blur to the line's own bounds — so the softened glyphs get sliced
                // off square at the edges. Unbounded lets the blur bleed past the bounds, which is
                // what makes it look like depth of field.
                //
                // On Android, BlurredEdgeTreatment.Unbounded gives the full depth-of-field effect.
                // (SimpMusic also has a Desktop branch using Rectangle due to skiko clipping, but
                // echo is Android-only so that branch is removed here.)
                Modifier.blur(blurRadius, BlurredEdgeTreatment.Unbounded)
            } else {
                Modifier
            },
        )
}

// ══════════════════════════════════════════════════════════════════════════════════════════════
//  Line item composable
// ══════════════════════════════════════════════════════════════════════════════════════════════

/**
 * One line-synced lyric line, Apple Music style: same size for every line, white, hard left,
 * with the translation underneath. Focus is applied by the caller through [appleMusicLyricFocus]
 * so the blur wraps the whole line including its translation.
 *
 * @param originalWords the main lyric text
 * @param translatedWords optional translation text (rendered below the original)
 * @param isCurrent whether this is the line currently being sung
 * @param romanizedWords optional romanization/pronunciation aid (rendered between original and translation)
 * @param onClick optional click callback (the caller wraps with its own click target when null)
 * @param modifier additional modifier
 */
@Composable
fun AppleMusicLyricsLineItem(
    originalWords: String,
    translatedWords: String?,
    isCurrent: Boolean,
    romanizedWords: String? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val lineInteraction = remember { MutableInteractionSource() }
    val linePressed by lineInteraction.collectIsPressedAsState()

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(
                    color = if (linePressed) AppleMusicLyricPressedBackground else Color.Transparent,
                    shape = RoundedCornerShape(AppleMusicLyricCornerRadius),
                )
                .then(
                    if (onClick != null) {
                        Modifier.clickable(
                            interactionSource = lineInteraction,
                            indication = null,
                            onClick = onClick,
                        )
                    } else {
                        Modifier
                    },
                )
                .padding(horizontal = AppleMusicLyricPaddingX),
    ) {
        Spacer(modifier = Modifier.height(AppleMusicLyricGap))

        Text(
            text = originalWords,
            // fillMaxWidth + Start, both explicit: a wrapped line must break against the SAME left
            // edge as every other line, and a short line must not drift toward the middle.
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start,
            color = if (isCurrent) Color.White else AppleMusicInactiveLineColor,
            style =
                MaterialTheme.typography.headlineLarge.copy(
                    fontSize = AppleMusicLyricFontSize,
                    lineHeight = AppleMusicLyricLineHeight,
                ),
        )

        if (romanizedWords != null) {
            Spacer(modifier = Modifier.height(AppleMusicMainToSubGap))
            Text(
                text = romanizedWords,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start,
                style =
                    MaterialTheme.typography.bodyMedium.copy(
                        fontSize = AppleMusicSubLineFontSize,
                        lineHeight = AppleMusicSubLineHeight,
                    ),
                color = if (isCurrent) AppleMusicRomanizedColor else AppleMusicInactiveLineColor,
            )
        }

        if (translatedWords != null) {
            Spacer(modifier = Modifier.height(AppleMusicMainToSubGap))
            Text(
                text = translatedWords,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start,
                style =
                    MaterialTheme.typography.bodyMedium.copy(
                        fontSize = AppleMusicSubLineFontSize,
                        lineHeight = AppleMusicSubLineHeight,
                    ),
                color = if (isCurrent) AppleMusicTranslatedColor else AppleMusicInactiveLineColor,
            )
        }

        Spacer(modifier = Modifier.height(AppleMusicLyricGap))
    }
}
