/*
 * Dhun Project Original (2026)
 * Dhun
 * Licensed Under GPL-3.0 | see git history for contributors
 */

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package dev.brahmkshatriya.echo.dhun.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import dev.brahmkshatriya.echo.dhun.LocalPlayerConnection
import dev.brahmkshatriya.echo.dhun.constants.SwipeSensitivityKey
import dev.brahmkshatriya.echo.dhun.constants.MiniPlayerHeight
import dev.brahmkshatriya.echo.dhun.ui.component.BottomSheetState
import dev.brahmkshatriya.echo.dhun.utils.rememberPreference
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.remember
import dev.brahmkshatriya.echo.dhun.constants.EnableLiquidGlassKey
import dev.brahmkshatriya.echo.extensions.ExtensionLoader
import org.koin.core.context.GlobalContext
import dev.brahmkshatriya.echo.dhun.ui.component.LocalBackdrop
import dev.brahmkshatriya.echo.dhun.ui.component.drawBackdropCustomShape


@Composable
fun MiniPlayer(
    position: Long,
    duration: Long,
    modifier: Modifier = Modifier,
    pureBlack: Boolean,
    navController: NavController,
    state: BottomSheetState
) {
    NewMiniPlayer(
        position = position,
        duration = duration,
        modifier = modifier,
        pureBlack = pureBlack,
        navController = navController,
        state = state
    )
}

@Composable
private fun NewMiniPlayer(
    position: Long,
    duration: Long,
    modifier: Modifier = Modifier,
    pureBlack: Boolean,
    navController: NavController,
    state: BottomSheetState
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val layoutDirection = LocalLayoutDirection.current
    val coroutineScope = rememberCoroutineScope()
    val swipeSensitivity by rememberPreference(SwipeSensitivityKey, 0.73f)
    val swipeThumbnail by rememberPreference(dev.brahmkshatriya.echo.dhun.constants.SwipeThumbnailKey, true)
    val enableLiquidGlass by rememberPreference(EnableLiquidGlassKey, defaultValue = false)
    val extensionLoader = remember { GlobalContext.get().get<ExtensionLoader>() }
    val currentExtension by extensionLoader.current.collectAsState()
    val useDhunGlass = enableLiquidGlass && currentExtension?.id == "dhun"

    val layer = rememberGraphicsLayer()
    val luminanceAnimation = remember { Animatable(0.3f) }
    val backdrop = LocalBackdrop.current

    val backgroundColor = MaterialTheme.colorScheme.surfaceContainer

    SwipeableMiniPlayerBox(
        modifier = modifier,
        swipeSensitivity = swipeSensitivity,
        swipeThumbnail = swipeThumbnail,
        playerConnection = playerConnection,
        layoutDirection = layoutDirection,
        coroutineScope = coroutineScope,
        pureBlack = pureBlack,
        useLegacyBackground = false,
        enableVerticalDismiss = useDhunGlass,
    ) { offsetX ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (useDhunGlass) 72.dp else MiniPlayerHeight)
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .let {
                    if (useDhunGlass && backdrop != null) {
                        it.drawBackdropCustomShape(
                            backdrop = backdrop,
                            layer = layer,
                            luminanceAnimation = luminanceAnimation.value,
                            shape = RoundedCornerShape(36.dp)
                        )
                    } else it
                }
                .clip(RoundedCornerShape(if (useDhunGlass) 36.dp else 32.dp))
                .background(color = if (useDhunGlass) Color.Transparent else backgroundColor)
                .then(
                    if (useDhunGlass) Modifier.border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(36.dp))
                    else Modifier
                )
        ) {
            NewMiniPlayerContent(
                pureBlack = pureBlack,
                position = position,
                duration = duration,
                playerConnection = playerConnection,
                navController = navController,
                state = state,
                useDhunGlass = useDhunGlass
            )
        }
    }
}