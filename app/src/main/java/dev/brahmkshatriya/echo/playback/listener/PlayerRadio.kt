package dev.brahmkshatriya.echo.playback.listener

import android.content.SharedPreferences
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.Timeline
import dev.brahmkshatriya.echo.common.Extension
import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.clients.RadioClient
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Feed.Companion.pagedDataOfFirst
import dev.brahmkshatriya.echo.di.App
import dev.brahmkshatriya.echo.download.Downloader
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.get
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.getExtension
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.getIf
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.getOrThrow
import dev.brahmkshatriya.echo.extensions.MediaState
import dev.brahmkshatriya.echo.playback.MediaItemUtils
import dev.brahmkshatriya.echo.playback.MediaItemUtils.context
import dev.brahmkshatriya.echo.playback.MediaItemUtils.extensionId
import dev.brahmkshatriya.echo.playback.MediaItemUtils.track
import dev.brahmkshatriya.echo.playback.PlayerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayerRadio(
    private val app: App,
    private val scope: CoroutineScope,
    private val player: Player,
    private val throwFlow: MutableSharedFlow<Throwable>,
    private val stateFlow: MutableStateFlow<PlayerState.Radio>,
    private val extensionList: StateFlow<List<MusicExtension>>,
    private val downloadFlow: StateFlow<List<Downloader.Info>>
) : Player.Listener {

    companion object {
        const val AUTO_START_RADIO = "auto_start_radio"
        const val ENDLESS_QUEUE = "endless_queue"
        const val RADIO_THRESHOLD = "radio_threshold"
        // Default: start loading radio when 3 or fewer tracks remain after current
        const val DEFAULT_THRESHOLD = 3
        suspend fun start(
            throwableFlow: MutableSharedFlow<Throwable>,
            extension: Extension<*>,
            item: EchoMediaItem,
            itemContext: EchoMediaItem?
        ): PlayerState.Radio.Loaded? {
            if (!item.isRadioSupported) return null
            return extension.getIf<RadioClient, PlayerState.Radio.Loaded?> {
                val radio = radio(item, itemContext)
                val tracks = loadTracks(radio).pagedDataOfFirst()
                PlayerState.Radio.Loaded(extension.id, radio, null) {
                    extension.get { tracks.loadPage(it) }.getOrThrow(throwableFlow)
                }
            }.getOrThrow(throwableFlow)
        }

        suspend fun play(
            player: Player,
            downloadFlow: StateFlow<List<Downloader.Info>>,
            app: App,
            stateFlow: MutableStateFlow<PlayerState.Radio>,
            loaded: PlayerState.Radio.Loaded
        ) {
            stateFlow.value = PlayerState.Radio.Loading
            val tracks = loaded.tracks(loaded.cont) ?: return

            stateFlow.value = if (tracks.continuation == null) PlayerState.Radio.Empty
            else loaded.copy(cont = tracks.continuation)

            val item = tracks.data.map {
                MediaItemUtils.build(
                    app,
                    downloadFlow.value,
                    MediaState.Unloaded(loaded.clientId, it),
                    loaded.context
                )
            }

            withContext(Dispatchers.Main) {
                player.addMediaItems(item)
                player.prepare()
            }
        }
    }

    private suspend fun loadPlaylist() {
        val mediaItem = withContext(Dispatchers.Main) { player.currentMediaItem } ?: return
        val extensionId = mediaItem.extensionId
        val item = mediaItem.track
        val itemContext = mediaItem.context
        stateFlow.value = PlayerState.Radio.Loading
        val extension = extensionList.getExtension(extensionId) ?: return
        val loaded = start(throwFlow, extension, item, itemContext)
        stateFlow.value = loaded ?: PlayerState.Radio.Empty
        if (loaded != null) play(player, downloadFlow, app, stateFlow, loaded)
    }

    private var autoStartRadio = app.settings.getBoolean(AUTO_START_RADIO, true)
    private var endlessQueue = app.settings.getBoolean(ENDLESS_QUEUE, true)
    private var threshold = app.settings.getInt(RADIO_THRESHOLD, DEFAULT_THRESHOLD)

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { pref, key ->
        when (key) {
            AUTO_START_RADIO -> autoStartRadio = pref.getBoolean(AUTO_START_RADIO, true)
            ENDLESS_QUEUE -> endlessQueue = pref.getBoolean(ENDLESS_QUEUE, true)
            RADIO_THRESHOLD -> threshold = pref.getInt(RADIO_THRESHOLD, DEFAULT_THRESHOLD)
        }
    }

    init {
        app.settings.registerOnSharedPreferenceChangeListener(listener)
    }

    // ── Threshold-based radio trigger (from SimpMusic queue algorithm) ──
    // Instead of only triggering at the last track, check if remaining tracks
    // after current are <= threshold. This gives time to fetch radio tracks
    // before the queue runs out, avoiding a "loading gap".
    private fun tracksRemaining(): Int {
        val total = player.mediaItemCount
        val current = player.currentMediaItemIndex
        return (total - 1) - current
    }

    private suspend fun startRadio() {
        if (!autoStartRadio || !endlessQueue) return
        val shouldNotStart = withContext(Dispatchers.Main) {
            player.run {
                currentMediaItem == null || repeatMode != REPEAT_MODE_OFF
            }
        }
        if (shouldNotStart) return

        // Threshold check: start radio when remaining tracks <= threshold
        val remaining = withContext(Dispatchers.Main) { tracksRemaining() }
        if (remaining > threshold) return

        when (val state = stateFlow.value) {
            is PlayerState.Radio.Loading -> {}
            is PlayerState.Radio.Empty -> loadPlaylist()
            is PlayerState.Radio.Loaded -> {
                // Only play if we still need more tracks
                if (remaining <= threshold) {
                    play(player, downloadFlow, app, stateFlow, state)
                }
            }
        }
    }

    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
        scope.launch { startRadio() }
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        if (player.mediaItemCount == 0) stateFlow.value = PlayerState.Radio.Empty
        scope.launch { startRadio() }
    }
}

