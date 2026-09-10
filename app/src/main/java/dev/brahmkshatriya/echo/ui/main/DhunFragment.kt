package dev.brahmkshatriya.echo.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.dhun.bridge.DhunFeedBridge
import dev.brahmkshatriya.echo.ui.component.navbar.BottomNavScreen
import dev.brahmkshatriya.echo.ui.component.navbar.LiquidGlassTabBar
import dev.brahmkshatriya.echo.ui.extensions.ExtensionsViewModel
import org.koin.androidx.viewmodel.ext.android.activityViewModel

/**
 * Dhun Fragment — hosts the Compose-based Dhun home screen.
 *
 * When Dhun extension is selected, shows:
 * 1. Ultra-smooth bouncy scroll animations (snap fling behavior)
 * 2. Haptic feedback on every card/button touch
 * 3. Card scale-down on press with spring bounce-back
 * 4. Header parallax (alpha + scale driven by scroll)
 * 5. Bouncy card placement animation (animateItem with spring)
 * 6. Liquid glass nav bar with blob shift animation (DampedDragAnimation)
 *    - Blob slides between tabs with spring physics
 *    - Squash/stretch based on drag velocity
 *    - Press inflation (56dp → 76dp)
 */
class DhunFragment : Fragment() {

    private val extensionsViewModel by activityViewModel<ExtensionsViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    DhunHomeContent(extensionsViewModel)
                }
            }
        }
    }
}

// ── Spring specs ──────────────────────────────────────────────────────────────

private val ultraSmoothSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioLowBouncy,
    stiffness = Spring.StiffnessLow,
)

private val pressSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessMedium,
)

private val placementSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessMediumLow,
)

// ── Haptic helper ──────────────────────────────────────────────────────────────

@Composable
private fun rememberHaptic(): () -> Unit {
    val haptic = LocalHapticFeedback.current
    return remember(haptic) {
        { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
    }
}

// ── Main Content ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DhunHomeContent(viewModel: ExtensionsViewModel) {
    val currentExtension by viewModel.extensionLoader.current.collectAsState()
    val feed = DhunFeedBridge.rememberExtensionFeed(currentExtension)

    val lazyListState = rememberLazyListState()
    val flingBehavior = rememberSnapFlingBehavior(lazyListState)
    val onHaptic = rememberHaptic()

    var selectedNavTab by remember { mutableStateOf(0) }

    // Scroll-driven header parallax
    val scrollOffset by remember {
        derivedStateOf {
            if (lazyListState.firstVisibleItemIndex == 0)
                lazyListState.firstVisibleItemScrollOffset.toFloat()
            else 1000f
        }
    }

    val headerAlpha by animateFloatAsState(
        targetValue = (1f - (scrollOffset / 800f)).coerceIn(0f, 1f),
        animationSpec = ultraSmoothSpring,
        label = "headerAlpha",
    )
    val headerScale by animateFloatAsState(
        targetValue = (1f - (scrollOffset / 4000f)).coerceIn(0.85f, 1f),
        animationSpec = ultraSmoothSpring,
        label = "headerScale",
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Feed ──
            LazyColumn(
                state = lazyListState,
                flingBehavior = flingBehavior,
                modifier = Modifier.fillMaxSize().weight(1f),
                contentPadding = PaddingValues(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Header
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 8.dp)
                            .graphicsLayer {
                                alpha = headerAlpha
                                scaleX = headerScale
                                scaleY = headerScale
                            },
                    ) {
                        Text(
                            text = "Dhun",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = currentExtension?.name ?: "Select an extension",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }

                if (feed.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(top = 80.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                BouncyLoadingDots()
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Loading feed...",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                } else {
                    items(
                        items = feed,
                        key = { shelf ->
                            when (shelf) {
                                is Shelf.Lists -> "lists_${shelf.title}"
                                is Shelf.Items -> "items_${shelf.title}"
                                else -> "other_${shelf.hashCode()}"
                            }
                        },
                    ) { shelf ->
                        BouncyShelfCard(shelf, onHaptic)
                    }
                }
            }

            // ── Liquid Glass Navigation Bar ──
            // Uses DampedDragAnimation with 5 parallel spring-driven values:
            // - Position: spring(1f, 1000f) — blob slides between tabs
            // - Velocity: spring(0.5f, 300f) — squash/stretch
            // - Press progress: spring(1f, 1000f) — inflate on press
            // - ScaleX: spring(0.6f, 250f) — horizontal inflate
            // - ScaleY: spring(0.7f, 250f) — vertical inflate
            LiquidGlassTabBar(
                tabs = listOf(BottomNavScreen.Home, BottomNavScreen.Library, BottomNavScreen.Search),
                selectedTab = selectedNavTab,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                onTabSelected = { index ->
                    selectedNavTab = index
                    onHaptic()
                },
            )
        }
    }
}

// ── Bouncy Shelf Card with haptic ─────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BouncyShelfCard(
    shelf: Shelf,
    onHaptic: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = pressSpring,
        label = "shelfScale",
    )

    val title = when (shelf) {
        is Shelf.Lists -> shelf.title
        is Shelf.Items -> shelf.title
        else -> "Section"
    }
    val itemCount = when (shelf) {
        is Shelf.Lists -> shelf.items.size
        is Shelf.Items -> shelf.items.size
        else -> 0
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .scale(cardScale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
            ) { onHaptic() }
            .animateItem(
                fadeIn(animationSpec = tween(400)),
                placementSpec = placementSpring,
            ),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = title ?: "Section",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                ) {
                    Text(
                        text = "$itemCount",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            when (shelf) {
                is Shelf.Lists -> BouncyHorizontalList(itemCount, onHaptic)
                is Shelf.Items -> BouncyHorizontalList(itemCount, onHaptic)
                else -> {
                    Text(
                        text = "$itemCount items available",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// ── Bouncy Horizontal List ───────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BouncyHorizontalList(itemCount: Int, onHaptic: () -> Unit) {
    val rowState = rememberLazyListState()
    val rowFling = rememberSnapFlingBehavior(rowState)

    LazyRow(
        state = rowState,
        flingBehavior = rowFling,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(itemCount.coerceAtMost(10)) { idx ->
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val cardScale by animateFloatAsState(
                targetValue = if (isPressed) 0.90f else 1f,
                animationSpec = pressSpring,
                label = "hCard_$idx",
            )

            Surface(
                modifier = Modifier
                    .size(width = 140.dp, height = 160.dp)
                    .scale(cardScale)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                    ) { onHaptic() },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 1.dp,
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                                        )
                                    )
                                ),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Item ${idx + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

// ── Bouncy Loading Dots ──────────────────────────────────────────────────────

@Composable
private fun BouncyLoadingDots() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(3) { index ->
            val dotScale by animateFloatAsState(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow,
                ),
                label = "dot_$index",
            )
            Surface(
                modifier = Modifier.size(8.dp).scale(dotScale),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
            ) {}
        }
    }
}
