package dev.brahmkshatriya.echo.ui.player.more.lyrics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import dev.brahmkshatriya.echo.common.models.Lyrics
import dev.brahmkshatriya.echo.ui.common.UiViewModel
import dev.brahmkshatriya.echo.ui.player.PlayerColors
import dev.brahmkshatriya.echo.ui.player.PlayerColors.Companion.defaultPlayerColors
import dev.brahmkshatriya.echo.ui.player.PlayerViewModel
import dev.brahmkshatriya.echo.ui.player.more.lyrics.applemusic.AppleMusicLyricsView
import org.koin.androidx.viewmodel.ext.android.activityViewModel

/**
 * Apple Music-style lyrics container.
 *
 * Replaces echo's original RecyclerView-based lyrics with a Compose LazyColumn
 * that renders lyrics with:
 * - Blur/alpha focus treatment (active line sharp, far lines blurred)
 * - Spring-based auto-scroll (dampingRatio=0.9f, stiffness=180f)
 * - Tap-to-seek
 * - Support for Timed, Simple, and WordByWord lyrics
 * - Pre-roll dimming (before first line starts)
 */
class AppleMusicLyricsContainer : Fragment() {

    private val viewModel by activityViewModel<LyricsViewModel>()
    private val playerVM by activityViewModel<PlayerViewModel>()
    private val uiViewModel by activityViewModel<UiViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    val lyricsState by viewModel.lyricsState.collectAsState()
                    val progress by playerVM.progress.collectAsState()
                    val playerColors by uiViewModel.playerColors.collectAsState()

                    val colors = playerColors ?: requireContext().defaultPlayerColors()
                    val currentPosition = progress.first

                    val lyricsItem = (lyricsState as? LyricsViewModel.State.Loaded)
                        ?.result?.getOrNull()
                    val lyrics = lyricsItem?.lyrics

                    AppleMusicLyricsView(
                        lyrics = lyrics,
                        currentPosition = currentPosition,
                        onLineClick = { startTime ->
                            playerVM.seekTo(startTime)
                        },
                        primaryColor = Color(colors.primary),
                        onBackgroundColor = Color(colors.onBackground),
                    )
                }
            }
        }
    }
}
