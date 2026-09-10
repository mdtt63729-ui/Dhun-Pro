/*
 * Dhun Project Original (2026)
 * Dhun
 * Licensed Under GPL-3.0 | see git history for contributors
 *
 * SimpMusic-style floating capsule navigation bar with liquid glass effect.
 *
 * When liquid glass is enabled (EnableLiquidGlassKey), the capsule and all
 * circular buttons render as real glass surfaces using the Kyant backdrop
 * library: real-time blur, vibrancy, lens refraction, and press-responsive
 * glow + scale. On Android < 12 (API 31), falls back to translucent surface.
 *
 * The glass surfaces sample from the backdrop source that MainActivity wraps
 * around the entire content area via layerBackdrop(backdrop). The glass
 * elements MUST be siblings of (not children of) the backdrop source.
 */

package dev.brahmkshatriya.echo.dhun.ui.component

import android.os.Build
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.dhun.ui.screens.Screens

// Mirrors SimpMusic's geometry constants exactly.
private val FlatTabWidth = 96.dp
private val FlatBarHeight = 64.dp
private val FlatIndicatorHeight = 56.dp
private val CapsuleInset = 6.dp

@Composable
fun FloatingNavigationToolbar(
    items: List<Screens>,
    pureBlack: Boolean,
    modifier: Modifier = Modifier,
    onFabClick: (() -> Unit)? = null,
    fabIconRes: Int? = null,
    fabContentDescription: String = "",
    onShuffleClick: (() -> Unit)? = null,
    shuffleIconRes: Int? = null,
    shuffleContentDescription: String = "",
    onMusicRecognitionClick: (() -> Unit)? = null,
    musicRecognitionContentDescription: String = "",
    isSelected: (Screens) -> Boolean = { false },
    onItemClick: (Screens, Boolean) -> Unit = { _, _ -> },
) {
    val hasOverflowAction = onShuffleClick != null && shuffleIconRes != null
    val hasFabAction = onFabClick != null && fabIconRes != null

    // Search rides in its own circular button, so the capsule holds everything else.
    val barTabs = items.filter { it.route != Screens.Search.route }
    val searchItem = items.find { it.route == Screens.Search.route }

    // Read the backdrop from CompositionLocal (provided by MainActivity).
    val backdrop = LocalBackdrop.current
    val canUseGlass = backdrop != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    // The capsule colour: solid surface, or near-black in pureBlack mode.
    val capsuleColor = if (pureBlack) {
        Color.Black.copy(alpha = 0.92f)
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    val indicatorColor = if (pureBlack) {
        Color.White.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }

    // Which position in barTabs is selected? Used for the sliding indicator.
    val selectedPosition = barTabs.indexOfFirst { isSelected(it) }

    // Glass interaction state for the capsule (shared press glow).
    val capsuleInteraction = if (canUseGlass) rememberGlassInteraction() else null
    val capsuleLayer = if (canUseGlass) rememberGraphicsLayer() else null

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxWidth(),
    ) {
        BoxWithConstraints(Modifier.weight(1f, fill = false)) {
            val tabWidth = ((maxWidth - CapsuleInset * 2) / barTabs.size.coerceAtLeast(1))
                .coerceAtMost(FlatTabWidth)

            // Sliding indicator — animateDpAsState uses a default spring, smooth at all refresh rates.
            val indicatorOffset by animateDpAsState(
                targetValue = tabWidth * selectedPosition.coerceAtLeast(0),
                label = "flatBarIndicator",
            )

            // Capsule modifier: liquid glass when available, solid surface otherwise.
            val capsuleModifier = if (canUseGlass && capsuleLayer != null) {
                Modifier
                    .height(FlatBarHeight)
                    .drawInteractiveGlass(
                        isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f,
                        backdrop = backdrop!!,
                        layer = capsuleLayer,
                        luminanceAnimation = 0.5f,
                        shape = RoundedCornerShape(FlatBarHeight / 2),
                        interaction = capsuleInteraction,
                        pressedScale = 1.02f,
                        minScrim = 0.08f,
                        maxScrim = 0.35f,
                    )
            } else {
                Modifier
                    .height(FlatBarHeight)
                    .clip(RoundedCornerShape(FlatBarHeight / 2))
                    .background(capsuleColor)
            }

            Box(
                modifier = capsuleModifier
                    .padding(horizontal = CapsuleInset),
                contentAlignment = Alignment.CenterStart,
            ) {
                // Sliding indicator — hidden while Search is selected.
                if (selectedPosition >= 0) {
                    Box(
                        modifier = Modifier
                            .offset(x = indicatorOffset)
                            .size(width = tabWidth, height = FlatIndicatorHeight)
                            .clip(RoundedCornerShape(FlatIndicatorHeight / 2))
                            .background(indicatorColor),
                    )
                }

                // Tab items — all same width, icon + label centered.
                Row {
                    barTabs.forEach { screen ->
                        val selected = isSelected(screen)
                        val contentColor = if (selected) {
                            if (pureBlack) Color.White
                            else MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            if (pureBlack) Color.White.copy(alpha = 0.82f)
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        }

                        // Press-to-scale feedback for buttery feel.
                        val interactionSource = remember { MutableInteractionSource() }
                        val isPressed by interactionSource.collectIsPressedAsState()
                        val pressScale by animateFloatAsState(
                            targetValue = if (isPressed) 0.92f else 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium,
                            ),
                            label = "pressScale_${screen.route}",
                        )

                        Column(
                            modifier = Modifier
                                .width(tabWidth)
                                .fillMaxHeight()
                                .scale(pressScale)
                                .clip(RoundedCornerShape(FlatIndicatorHeight / 2))
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                ) { onItemClick(screen, selected) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            CompositionLocalProvider(LocalContentColor provides contentColor) {
                                Icon(
                                    painter = painterResource(
                                        if (selected) screen.iconIdActive else screen.iconIdInactive
                                    ),
                                    contentDescription = stringResource(screen.titleId),
                                )
                            }
                            Text(
                                text = stringResource(screen.titleId),
                                style = MaterialTheme.typography.labelSmall,
                                color = contentColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }

        // ── Search circular FAB ────────────────────────────────────────────
        if (searchItem != null) {
            Spacer(Modifier.size(12.dp))
            val searchSelected = isSelected(searchItem)
            val searchModifier = if (canUseGlass && backdrop != null) {
                Modifier
                    .size(FlatIndicatorHeight)
                    .liquidGlass(
                        backdrop = backdrop,
                        shape = CircleShape,
                        interactive = true,
                    )
            } else {
                Modifier
                    .size(FlatIndicatorHeight)
                    .clip(CircleShape)
                    .background(if (searchSelected) indicatorColor else capsuleColor)
            }
            Box(
                modifier = searchModifier.clickable { onItemClick(searchItem, searchSelected) },
                contentAlignment = Alignment.Center,
            ) {
                CompositionLocalProvider(
                    LocalContentColor provides if (searchSelected) {
                        if (pureBlack) Color.White
                        else MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        if (pureBlack) Color.White.copy(alpha = 0.82f)
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    },
                ) {
                    Icon(
                        painter = painterResource(searchItem.iconIdActive),
                        contentDescription = stringResource(searchItem.titleId),
                    )
                }
            }
        }

        // ── Overflow FAB (shuffle + music recognition) ────────────────────
        if (hasOverflowAction) {
            Spacer(Modifier.size(12.dp))
            OverflowActionFAB(
                pureBlack = pureBlack,
                backdrop = backdrop,
                canUseGlass = canUseGlass,
                onShuffleClick = onShuffleClick,
                shuffleIconRes = shuffleIconRes,
                shuffleContentDescription = shuffleContentDescription,
                onMusicRecognitionClick = onMusicRecognitionClick,
                musicRecognitionContentDescription = musicRecognitionContentDescription,
            )
        } else if (hasFabAction) {
            Spacer(Modifier.size(12.dp))
            SimpleFAB(
                pureBlack = pureBlack,
                backdrop = backdrop,
                canUseGlass = canUseGlass,
                onClick = onFabClick!!,
                iconRes = fabIconRes!!,
                contentDescription = fabContentDescription,
            )
        }
    }
}

/**
 * Circular FAB for overflow actions (shuffle + music recognition).
 * Uses liquid glass when available.
 */
@Composable
private fun OverflowActionFAB(
    pureBlack: Boolean,
    backdrop: PlatformBackdrop?,
    canUseGlass: Boolean,
    onShuffleClick: (() -> Unit)?,
    shuffleIconRes: Int?,
    shuffleContentDescription: String,
    onMusicRecognitionClick: (() -> Unit)?,
    musicRecognitionContentDescription: String,
) {
    var fabMenuExpanded by rememberSaveable { mutableStateOf(false) }

    val fabModifier = if (canUseGlass && backdrop != null) {
        Modifier
            .size(FlatIndicatorHeight)
            .liquidGlass(
                backdrop = backdrop,
                shape = CircleShape,
                interactive = true,
            )
    } else {
        Modifier
            .size(FlatIndicatorHeight)
            .clip(CircleShape)
            .background(
                if (pureBlack) Color.White.copy(alpha = 0.12f)
                else MaterialTheme.colorScheme.tertiaryContainer,
            )
    }

    Box {
        Box(
            modifier = fabModifier.clickable { fabMenuExpanded = !fabMenuExpanded },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.more_horiz),
                contentDescription = shuffleContentDescription.ifEmpty {
                    stringResource(R.string.more)
                },
                tint = if (pureBlack) Color.White
                else MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }

        DropdownMenu(
            expanded = fabMenuExpanded,
            onDismissRequest = { fabMenuExpanded = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = if (pureBlack) Color.Black
            else MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.music_recognition)) },
                onClick = {
                    fabMenuExpanded = false
                    onMusicRecognitionClick?.invoke()
                },
                leadingIcon = {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = if (pureBlack) Color.White.copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = if (pureBlack) Color.White
                        else MaterialTheme.colorScheme.onSecondaryContainer,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(R.drawable.mic),
                                contentDescription = musicRecognitionContentDescription.ifEmpty {
                                    stringResource(R.string.music_recognition)
                                },
                            )
                        }
                    }
                },
                enabled = onMusicRecognitionClick != null,
                colors = MenuDefaults.itemColors(
                    textColor = if (pureBlack) Color.White
                    else MaterialTheme.colorScheme.onSurface,
                    leadingIconColor = if (pureBlack) Color.White.copy(alpha = 0.82f)
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )

            if (onShuffleClick != null && shuffleIconRes != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.shuffle)) },
                    onClick = {
                        fabMenuExpanded = false
                        onShuffleClick()
                    },
                    leadingIcon = {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            color = if (pureBlack) Color.White.copy(alpha = 0.12f)
                            else MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = if (pureBlack) Color.White
                            else MaterialTheme.colorScheme.onSecondaryContainer,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(shuffleIconRes),
                                    contentDescription = shuffleContentDescription.ifEmpty {
                                        stringResource(R.string.shuffle)
                                    },
                                )
                            }
                        }
                    },
                    colors = MenuDefaults.itemColors(
                        textColor = if (pureBlack) Color.White
                        else MaterialTheme.colorScheme.onSurface,
                        leadingIconColor = if (pureBlack) Color.White.copy(alpha = 0.82f)
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
        }
    }
}

/**
 * Simple circular FAB for a single action.
 * Uses liquid glass when available.
 */
@Composable
private fun SimpleFAB(
    pureBlack: Boolean,
    backdrop: PlatformBackdrop?,
    canUseGlass: Boolean,
    onClick: () -> Unit,
    iconRes: Int,
    contentDescription: String,
) {
    val fabModifier = if (canUseGlass && backdrop != null) {
        Modifier
            .size(FlatIndicatorHeight)
            .liquidGlass(
                backdrop = backdrop,
                shape = CircleShape,
                interactive = true,
            )
    } else {
        Modifier
            .size(FlatIndicatorHeight)
            .clip(CircleShape)
            .background(
                if (pureBlack) Color.White.copy(alpha = 0.12f)
                else MaterialTheme.colorScheme.tertiaryContainer,
            )
    }

    Box(
        modifier = fabModifier.clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription.ifEmpty {
                stringResource(R.string.create_playlist)
            },
            tint = if (pureBlack) Color.White
            else MaterialTheme.colorScheme.onTertiaryContainer,
        )
    }
}

private fun Color.luminance(): Float {
    return 0.2126f * red + 0.7152f * green + 0.0722f * blue
}
