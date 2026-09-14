/*
 * Dhun Project Original (2026)
 * Dhun
 * Licensed Under GPL-3.0 | see git history for contributors
 */

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package dev.brahmkshatriya.echo.dhun.ui.player

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.dhun.constants.EnableHapticFeedbackKey
import dev.brahmkshatriya.echo.dhun.constants.MiniPlayerHeight
import dev.brahmkshatriya.echo.dhun.extensions.togglePlayPause
import dev.brahmkshatriya.echo.dhun.models.MediaMetadata
import dev.brahmkshatriya.echo.dhun.playback.PlayerConnection
import dev.brahmkshatriya.echo.dhun.together.TogetherSessionState
import dev.brahmkshatriya.echo.dhun.ui.component.BottomSheetState
import dev.brahmkshatriya.echo.dhun.utils.rememberHaptic
import dev.brahmkshatriya.echo.dhun.utils.rememberPreference
import kotlin.math.absoluteValue
import kotlin.math.roundToInt


@Composable
fun SwipeableMiniPlayerBox(
    modifier: Modifier = Modifier,
    swipeSensitivity: Float,
    swipeThumbnail: Boolean,
    playerConnection: PlayerConnection,
    layoutDirection: LayoutDirection,
    coroutineScope: CoroutineScope,
    pureBlack: Boolean = false,
    useLegacyBackground: Boolean = false,
    enableVerticalDismiss: Boolean = false,
    content: @Composable (Float) -> Unit
) {
    val offsetXAnimatable = remember { Animatable(0f) }
    var dragStartTime by remember { mutableStateOf(0L) }
    var totalDragDistance by remember { mutableFloatStateOf(0f) }

    val animationSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow
    )

    fun calculateAutoSwipeThreshold(swipeSensitivity: Float): Int {
        return (600 / (1f + kotlin.math.exp(-(-11.44748 * swipeSensitivity + 9.04945)))).roundToInt()
    }
    val autoSwipeThreshold = calculateAutoSwipeThreshold(swipeSensitivity)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(MiniPlayerHeight)
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
            .let { baseModifier ->
                if (useLegacyBackground) {
                    baseModifier.background(
                        if (pureBlack) Color.Black
                        else MaterialTheme.colorScheme.surfaceContainer
                    )
                } else {
                    baseModifier.padding(horizontal = 12.dp)
                }
            }
            .let { baseModifier ->
                if (swipeThumbnail) {
                    baseModifier.pointerInput(Unit) {
                        var totalX = 0f
                        var totalY = 0f
                        detectDragGestures(
                            onDragStart = {
                                dragStartTime = System.currentTimeMillis()
                                totalX = 0f
                                totalY = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val adjustedX =
                                    if (layoutDirection == LayoutDirection.Rtl) -dragAmount.x else dragAmount.x
                                totalX += adjustedX
                                totalY += dragAmount.y
                                if (kotlin.math.abs(totalX) >= kotlin.math.abs(totalY)) {
                                    val canSkipPrevious = playerConnection.player.previousMediaItemIndex != -1
                                    val canSkipNext = playerConnection.player.nextMediaItemIndex != -1
                                    val allowLeft = adjustedX < 0 && canSkipNext
                                    val allowRight = adjustedX > 0 && canSkipPrevious
                                    if (allowLeft || allowRight) {
                                        coroutineScope.launch {
                                            offsetXAnimatable.snapTo(offsetXAnimatable.value + adjustedX)
                                        }
                                    }
                                }
                            },
                            onDragCancel = {
                                coroutineScope.launch {
                                    offsetXAnimatable.animateTo(0f, animationSpec)
                                }
                            },
                            onDragEnd = {
                                val dragDuration = System.currentTimeMillis() - dragStartTime
                                val velocity = if (dragDuration > 0) totalX.absoluteValue / dragDuration else 0f
                                val horizontalThreshold = autoSwipeThreshold.toFloat()
                                val verticalThreshold = 90f
                                if (enableVerticalDismiss && totalY > verticalThreshold && totalY > totalX.absoluteValue * 1.15f) {
                                    // Down swipe: stop playback. The existing player sheet will
                                    // collapse automatically because its media state becomes empty.
                                    playerConnection.player.stop()
                                } else {
                                    val shouldChangeSong =
                                        (totalX.absoluteValue > 50f && velocity > ((swipeSensitivity * -8.25f) + 8.5f)) ||
                                            totalX.absoluteValue > horizontalThreshold
                                    if (shouldChangeSong) {
                                        val isRightSwipe = totalX > 0f
                                        if (isRightSwipe && playerConnection.player.previousMediaItemIndex != -1) {
                                            playerConnection.player.seekToPreviousMediaItem()
                                        } else if (!isRightSwipe && playerConnection.player.nextMediaItemIndex != -1) {
                                            playerConnection.player.seekToNext()
                                        }
                                    }
                                }
                                coroutineScope.launch {
                                    offsetXAnimatable.animateTo(0f, animationSpec)
                                }
                            }
                        )
                    }
                } else baseModifier
            }
    ) {
        content(offsetXAnimatable.value)

        // Visual indicator
        if (offsetXAnimatable.value.absoluteValue > 50f) {
            Box(
                modifier = Modifier
                    .align(if (offsetXAnimatable.value > 0) Alignment.CenterStart else Alignment.CenterEnd)
                    .padding(horizontal = 16.dp)
            ) {
                Icon(
                    painter = painterResource(
                        if (offsetXAnimatable.value > 0) R.drawable.skip_previous else R.drawable.skip_next
                    ),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(
                        alpha = (offsetXAnimatable.value.absoluteValue / autoSwipeThreshold).coerceIn(0f, 1f)
                    ),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun RowScope.MiniPlayerInfo(
    mediaMetadata: MediaMetadata
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.Center
    ) {
        AnimatedContent(
            targetState = mediaMetadata.title,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "title"
        ) { title ->
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.basicMarquee()
            )
        }

        AnimatedContent(
            targetState = mediaMetadata.artists,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "artist"
        ) { artists ->
            Text(
                text = artists.joinToString { it.name },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.basicMarquee()
            )
        }
    }
}

@Composable
private fun DhunGlassMiniPlayerContent(
    position: Long,
    duration: Long,
    playerConnection: PlayerConnection,
    onOpenPlayer: () -> Unit,
) {
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val playbackState by playerConnection.playbackState.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onOpenPlayer)
            .padding(horizontal = 10.dp, vertical = 7.dp),
    ) {
        MiniPlayerArtwork(
            mediaMetadata = mediaMetadata,
            position = position,
            duration = duration,
            isLoading = playbackState == Player.STATE_BUFFERING,
            modifier = Modifier.size(58.dp),
        )

        Spacer(Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = mediaMetadata?.title ?: "Dhun",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = mediaMetadata?.artists?.joinToString { it.name } ?: "Your music",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.70f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        IconButton(
            onClick = { playerConnection.player.togglePlayPause() },
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                painter = painterResource(
                    when {
                        playbackState == Player.STATE_ENDED -> R.drawable.replay
                        isPlaying -> R.drawable.pause
                        else -> R.drawable.play
                    }
                ),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(25.dp),
            )
        }

        IconButton(
            onClick = playerConnection::seekToNext,
            enabled = canSkipNext,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.skip_next),
                contentDescription = null,
                tint = Color.White.copy(alpha = if (canSkipNext) 1f else 0.35f),
                modifier = Modifier.size(27.dp),
            )
        }
    }
}

@Composable
private fun MiniPlayerArtwork(
    mediaMetadata: MediaMetadata?,
    position: Long,
    duration: Long,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(44.dp)
    ) {
        if (isLoading) {
            CircularWavyProgressIndicator(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
            )
        } else {
            CircularWavyProgressIndicator(
                progress = { if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f },
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
            )
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    shape = CircleShape
                )
        ) {
            val thumbnailUrl = mediaMetadata?.thumbnailUrl
            if (thumbnailUrl != null) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Image(
                    painter = painterResource(R.drawable.dhun),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun MiniPlayerTransportButton(
    iconResId: Int,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isPrimary: Boolean = false
) {
    val containerColor =
        if (isPrimary) MaterialTheme.colorScheme.surface else Color.Transparent
    val borderColor =
        if (enabled) MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
    val iconTint =
        if (enabled) MaterialTheme.colorScheme.onSurface
        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .then(modifier)
            .size(if (isPrimary) 40.dp else 36.dp)
            .clip(CircleShape)
            .background(containerColor)
            .border(width = 1.dp, color = borderColor, shape = CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Icon(
            painter = painterResource(iconResId),
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(if (isPrimary) 22.dp else 18.dp)
        )
    }
}

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@Composable
private fun MiniPlayerTransportControls(
    isPlaying: Boolean,
    playbackState: Int,
    isLoading: Boolean,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    playerConnection: PlayerConnection
) {
    val haptic = rememberHaptic()

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MiniPlayerTransportButton(
            iconResId = R.drawable.skip_previous,
            contentDescription = null,
            onClick = playerConnection::seekToPrevious,
            enabled = canSkipPrevious
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(40.dp)
        ) {
            MiniPlayerTransportButton(
                iconResId = when {
                    playbackState == Player.STATE_ENDED -> R.drawable.replay
                    isPlaying -> R.drawable.pause
                    else -> R.drawable.play
                },
                contentDescription = stringResource(
                    if (playbackState == Player.STATE_ENDED || !isPlaying) R.string.play else R.string.play
                ).let {
                    if (isPlaying && playbackState != Player.STATE_ENDED) "Pause" else it
                },
                onClick = {
                    haptic.click()
                    if (playbackState == Player.STATE_ENDED) {
                        playerConnection.player.seekTo(0, 0)
                        playerConnection.player.playWhenReady = true
                    } else {
                        playerConnection.player.togglePlayPause()
                    }
                },
                isPrimary = true
            )
        }

        MiniPlayerTransportButton(
            iconResId = R.drawable.skip_next,
            contentDescription = null,
            onClick = playerConnection::seekToNext,
            enabled = canSkipNext
        )
    }
}

@Composable
fun NewMiniPlayerContent(
    pureBlack: Boolean,
    position: Long,
    duration: Long,
    playerConnection: PlayerConnection,
    navController: NavController,
    state: BottomSheetState,
    useDhunGlass: Boolean = false,
) {
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val playbackState by playerConnection.playbackState.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val togetherSessionState by playerConnection.service.togetherSessionState.collectAsState()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)
    val isLiked = currentSong?.song?.liked == true

    val isLoading = playbackState == Player.STATE_BUFFERING

    if (useDhunGlass) {
        DhunGlassMiniPlayerContent(
            position = position,
            duration = duration,
            playerConnection = playerConnection,
            onOpenPlayer = { state.expandSoft() },
        )
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 8.dp),
        ) {
            MiniPlayerArtwork(
                mediaMetadata = mediaMetadata,
                position = position,
                duration = duration,
                isLoading = isLoading
            )

            Spacer(modifier = Modifier.width(12.dp))

            mediaMetadata?.let {
                MiniPlayerInfo(mediaMetadata = it)
            } ?: Spacer(Modifier.weight(1f))

            Spacer(modifier = Modifier.width(4.dp))

            MiniPlayerPlayPauseButton(
                isPlaying = isPlaying,
                isLoading = isLoading,
                playerConnection = playerConnection
            )

            Spacer(modifier = Modifier.width(4.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MiniPlayerPlayPauseButton(
    isPlaying: Boolean,
    isLoading: Boolean,
    playerConnection: PlayerConnection,
) {
    // Animación de escala del botón
    val buttonScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
            visibilityThreshold = 0.01f
        ),
        label = "buttonScale"
    )

    val animationKey by rememberUpdatedState(isPlaying)
    val (enableHaptic) = rememberPreference(EnableHapticFeedbackKey, true)
    val haptic = rememberHaptic(enabled = enableHaptic)

    FilledIconButton(
        onClick = {
            haptic.click()
            if (!isLoading) playerConnection.player.togglePlayPause()
        },
        modifier = Modifier
            .size(48.dp)
            .graphicsLayer {
                scaleX = buttonScale
                scaleY = buttonScale
            },
        shape = MaterialShapes.Cookie9Sided.toShape(),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        enabled = !isLoading,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp,
            )
        } else {
            AnimatedContent(
                targetState = isPlaying,
                transitionSpec = {
                    // Efecto Expressive: el icono entrante escala desde el centro
                    // mientras el saliente escala hacia afuera
                    (fadeIn(animationSpec = tween(150)) +
                            scaleIn(
                                initialScale = 0.4f, animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            )
                            ).togetherWith(
                            fadeOut(animationSpec = tween(100)) +
                                    scaleOut(targetScale = 1.6f, animationSpec = tween(100))
                        )
                },
                label = "playPauseIcon",
            ) { playing ->
                Icon(
                    painter = painterResource(
                        if (playing) R.drawable.pause else R.drawable.play
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}