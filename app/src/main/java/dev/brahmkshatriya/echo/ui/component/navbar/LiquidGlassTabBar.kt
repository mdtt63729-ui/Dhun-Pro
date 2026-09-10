package dev.brahmkshatriya.echo.ui.component.navbar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlin.math.roundToInt
import kotlin.math.sign

/**
 * Liquid Glass Tab Bar — Apple Music-style floating capsule with a frosted glass
 * "blob" that slides between tabs with spring physics, squash/stretch based on
 * drag velocity, and press inflation.
 *
 * Three stacked layers (bottom -> top):
 *  1. Dark frosted glass capsule (background, responds to press with scale + glow)
 *  2. Frosted blob (slides to selected tab, squashes/stretches with velocity)
 *  3. Crisp icons + labels (on top, carries the drag gesture)
 *
 * Animation system: [DampedDragAnimation] with 5 parallel spring-driven Animatable values:
 * - Position (spring 1f, 1000f) — drives blob translationX
 * - Velocity (spring 0.5f, 300f) — drives squash/stretch
 * - Press progress (spring 1f, 1000f) — drives lens/innerShadow
 * - ScaleX (spring 0.6f, 250f) — horizontal inflate on press
 * - ScaleY (spring 0.7f, 250f) — vertical inflate on press
 *
 * On press: blob inflates from 56dp to ~76dp (pressedScale = 76f/56f ≈ 1.357),
 * bulging out of the 64dp capsule, then springs back on release.
 *
 * Velocity -> Squash/Stretch: when sliding fast, blob stretches horizontally
 * and compresses vertically (like a water droplet), springs back when slowing.
 */
@Composable
fun LiquidGlassTabBar(
    tabs: List<BottomNavScreen>,
    selectedTab: Int,
    modifier: Modifier = Modifier,
    availableWidth: Dp = Dp.Unspecified,
    onTabSelected: (Int) -> Unit,
) {
    val density = LocalDensity.current
    val tabsCount = tabs.size

    // Calculate tab width — divides available width evenly, capped at TabWidth
    val tabWidth =
        if (availableWidth.isSpecified && availableWidth > 0.dp) {
            ((availableWidth - BarInset * 2) / tabsCount).coerceAtMost(TabWidth)
        } else {
            TabWidth
        }
    val tabWidthPx = with(density) { tabWidth.toPx() }
    val currentTabWidthPx by rememberUpdatedState(tabWidthPx)
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val animationScope = rememberCoroutineScope()
    val barInteraction = rememberGlassInteraction()

    var currentIndex by remember { mutableIntStateOf(selectedTab.coerceAtLeast(0)) }
    val draggedFlag = remember { booleanArrayOf(false) }

    val dampedDrag =
        remember(animationScope, tabsCount) {
            DampedDragAnimation(
                animationScope = animationScope,
                initialValue = selectedTab.coerceAtLeast(0).toFloat(),
                valueRange = 0f..(tabsCount - 1).toFloat(),
                visibilityThreshold = 0.001f,
                initialScale = 1f,
                pressedScale = 76f / 56f,
                onDragStarted = { draggedFlag[0] = false },
                onDragStopped = {
                    if (draggedFlag[0]) {
                        val target = targetValue.roundToInt().coerceIn(0, tabsCount - 1)
                        currentIndex = target
                        animateToValue(target.toFloat())
                    }
                },
                onDrag = { _, dragAmount ->
                    if (dragAmount.x != 0f) draggedFlag[0] = true
                    updateValue(
                        (targetValue + dragAmount.x / currentTabWidthPx * if (isLtr) 1f else -1f)
                            .coerceIn(0f, (tabsCount - 1).toFloat()),
                    )
                },
            )
        }

    // Keep blob in sync when selection changes from outside (e.g. back stack)
    LaunchedEffect(selectedTab) {
        if (selectedTab >= 0 && currentIndex != selectedTab) currentIndex = selectedTab
    }

    // Drive blob animation from internal current index
    LaunchedEffect(dampedDrag) {
        snapshotFlow { currentIndex }
            .drop(1)
            .collectLatest { index ->
                dampedDrag.animateToValue(index.toFloat())
                if (draggedFlag[0]) onTabSelected(index)
            }
    }

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    Box(
        modifier =
            modifier
                .height(BarHeight)
                .width(tabWidth * tabsCount + BarInset * 2)
                .pointerInput(barInteraction) { barInteraction.detectPress(this) },
        contentAlignment = Alignment.CenterStart,
    ) {
        // ── Layer 1: Dark frosted glass capsule ──
        // Semi-transparent dark surface with press-driven scale + radial glow
        val capsuleScale = lerp(1f, 1.04f, barInteraction.pressProgress)
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer {
                    scaleX = capsuleScale
                    scaleY = capsuleScale
                }
                .clip(CapsuleShape)
                .background(
                    if (isDark)
                        MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.90f)
                    else
                        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f)
                )
                .drawWithContent {
                    drawContent()
                    // Press glow — radial gradient following touch position
                    val press = barInteraction.pressProgress
                    if (press > 0f) {
                        drawRect(
                            brush =
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.18f * press),
                                        Color.Transparent,
                                    ),
                                    center = barInteraction.touchPosition
                                        .takeIf { it != Offset.Zero }
                                        ?: Offset(size.width / 2f, size.height / 2f),
                                    radius = size.minDimension * 1.2f,
                                ),
                            blendMode = BlendMode.Plus,
                        )
                    }
                }
        )

        // ── Layer 2: Frosted blob selection indicator ──
        // Slides between tabs, squashes/stretches with velocity, inflates on press
        Box(
            Modifier
                .graphicsLayer {
                    // Position: value * tabWidthPx + BarInset
                    translationX =
                        (if (isLtr) dampedDrag.value else (tabsCount - 1) - dampedDrag.value) * tabWidthPx +
                            BarInset.toPx()

                    // Press inflation: scaleX/scaleY from spring animation
                    scaleX = dampedDrag.scaleX
                    scaleY = dampedDrag.scaleY

                    // Velocity -> squash/stretch: stretch horizontally when moving fast
                    val velocity = dampedDrag.velocity / 10f
                    scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                    scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                }
                .width(tabWidth)
                .height(BlobHeight)
                .clip(CapsuleShape)
                .background(
                    // Frosted glass look — semi-transparent with depth
                    MaterialTheme.colorScheme.secondaryContainer.copy(
                        alpha = lerp(0.70f, 0.90f, dampedDrag.pressProgress)
                    )
                )
                .drawWithContent {
                    drawContent()
                    // Inner shadow that grows with press progress
                    val progress = dampedDrag.pressProgress
                    if (progress > 0f) {
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.15f * progress),
                                ),
                                center = Offset(size.width / 2f, size.height / 2f),
                                radius = size.minDimension * 0.7f,
                            ),
                        )
                    }
                }
        )

        // ── Layer 3: Crisp icons + labels on top, carrying the drag gesture ──
        Row(
            Modifier
                .matchParentSize()
                .padding(horizontal = BarInset)
                .then(dampedDrag.modifier),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEachIndexed { position, screen ->
                LiquidGlassTab(
                    screen = screen,
                    selected = currentIndex == position,
                    width = tabWidth,
                ) {
                    if (position == currentIndex) {
                        onTabSelected(position)
                    } else {
                        currentIndex = position
                        onTabSelected(position)
                    }
                }
            }
        }
    }
}

@Composable
private fun LiquidGlassTab(
    screen: BottomNavScreen,
    selected: Boolean,
    width: Dp,
    onClick: () -> Unit,
) {
    val color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface
    Column(
        Modifier
            .width(width)
            .fillMaxHeight()
            .clip(CapsuleShape)
            .clickable(
                interactionSource = null,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            ),
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CompositionLocalProvider(LocalContentColor provides color) {
            Icon(
                imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                contentDescription = screen.title,
            )
            Text(
                text = screen.title,
                style = MaterialTheme.typography.bodySmall,
                color = color,
                maxLines = 1,
            )
        }
    }
}

// Helper to compute luminance for dark/light detection
private fun Color.luminance(): Float {
    return 0.299f * red + 0.587f * green + 0.114f * blue
}
