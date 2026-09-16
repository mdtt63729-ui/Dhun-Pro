/*
 * Dhun Project Original (2026)
 * Dhun
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package dev.brahmkshatriya.echo.dhun.ui.screens.settings

import android.content.Context
import dev.brahmkshatriya.echo.dhun.db.entities.Song
import dev.brahmkshatriya.echo.dhun.utils.DiscordRPC
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Singleton owning the lifecycle of the Discord Rich Presence updater.
 *
 * The manager wraps a [DiscordRPC] (kizzy-compatible) client and periodically
 * refreshes the presence using values pulled from the providers supplied to
 * [start]. All provider lambdas touch a media3 player and are therefore always
 * invoked on the main thread, regardless of the dispatcher the caller used.
 */
object DiscordPresenceManager {

    private const val TAG = "DiscordPresenceManager"

    /** Minimum delay between two periodic presence refreshes. */
    private const val MIN_INTERVAL_MILLIS = 5_000L

    private class PresenceParams(
        val context: Context,
        val token: String,
        val songProvider: () -> Song?,
        val positionProvider: () -> Long,
        val isPausedProvider: () -> Boolean,
        val intervalProvider: () -> Long,
    )

    @Volatile
    private var running = false

    private var appContext: Context? = null
    private var params: PresenceParams? = null
    private var scope: CoroutineScope? = null
    private var loopJob: Job? = null
    private var rpc: DiscordRPC? = null
    private var rpcToken: String? = null

    private val lock = Any()

    /** Captures the application context so [restart] can be used from anywhere. */
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun isRunning(): Boolean = running

    /**
     * Starts the periodic presence updater.
     *
     * @param context any context; stored as the application context.
     * @param token the Discord token used for the RPC connection.
     * @param songProvider returns the song to display (null clears presence).
     * @param positionProvider current playback position in milliseconds.
     * @param isPausedProvider true when playback is paused.
     * @param intervalProvider delay between two refreshes in milliseconds.
     * @return true when the updater was started.
     */
    fun start(
        context: Context,
        token: String,
        songProvider: () -> Song?,
        positionProvider: () -> Long,
        isPausedProvider: () -> Boolean,
        intervalProvider: () -> Long,
    ): Boolean {
        stop()

        val presenceContext = context.applicationContext
        val presenceParams = PresenceParams(
            context = presenceContext,
            token = token,
            songProvider = songProvider,
            positionProvider = positionProvider,
            isPausedProvider = isPausedProvider,
            intervalProvider = intervalProvider,
        )

        synchronized(lock) {
            params = presenceParams
            val presenceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            scope = presenceScope
            loopJob = presenceScope.launch {
                while (isActive) {
                    try {
                        refreshOnce(presenceParams)
                    } catch (t: Throwable) {
                        Timber.tag(TAG).w(t, "periodic presence refresh failed")
                    }
                    val interval = runCatching { presenceParams.intervalProvider() }
                        .getOrDefault(MIN_INTERVAL_MILLIS)
                        .coerceAtLeast(MIN_INTERVAL_MILLIS)
                    kotlinx.coroutines.delay(interval)
                }
            }
            running = true
        }
        return true
    }

    /** Stops the updater and closes the underlying RPC connection. */
    fun stop() {
        synchronized(lock) {
            loopJob?.cancel()
            loopJob = null
            scope?.let { it.cancel() }
            scope = null
            closeRpcLocked()
            running = false
        }
    }

    /**
     * Restarts the updater with the parameters of the last [start] call.
     *
     * @return true when the manager was restarted, false if it was never
     * started (or no application context is known).
     */
    fun restart(): Boolean {
        val lastParams = synchronized(lock) { params } ?: return false
        val context = lastParams.context
        return start(
            context = context,
            token = lastParams.token,
            songProvider = lastParams.songProvider,
            positionProvider = lastParams.positionProvider,
            isPausedProvider = lastParams.isPausedProvider,
            intervalProvider = lastParams.intervalProvider,
        )
    }

    /**
     * Performs a single immediate presence update, bypassing the periodic loop.
     *
     * @return true when the presence was updated successfully.
     */
    suspend fun updateNow(
        context: Context,
        token: String,
        song: Song?,
        positionMs: Long,
        isPaused: Boolean,
    ): Boolean {
        if (song == null) return false
        if (token.isBlank()) return false
        val client = rpcFor(context.applicationContext, token) ?: return false
        return try {
            client.updateSong(song, positionMs, isPaused).isSuccess
        } catch (t: Throwable) {
            Timber.tag(TAG).w(t, "immediate presence update failed")
            false
        }
    }

    private suspend fun refreshOnce(presenceParams: PresenceParams) {
        // Providers read player state, which must happen on the main thread.
        val (song, position, isPaused) = withContext(Dispatchers.Main) {
            runCatching {
                Triple(
                    presenceParams.songProvider(),
                    presenceParams.positionProvider(),
                    presenceParams.isPausedProvider(),
                )
            }.getOrNull() ?: Triple(null, 0L, true)
        }

        if (song == null) {
            Timber.tag(TAG).v("no song available; skipping presence refresh")
            return
        }

        val client = rpcFor(presenceParams.context, presenceParams.token) ?: return
        client.updateSong(song, position, isPaused)
    }

    private suspend fun rpcFor(context: Context, token: String): DiscordRPC? {
        synchronized(lock) {
            if (rpc != null && rpcToken == token) return rpc
            closeRpcLocked()
            val client = runCatching { DiscordRPC(context, token) }
                .onFailure { Timber.tag(TAG).w(it, "failed to create DiscordRPC client") }
                .getOrNull() ?: return null
            rpc = client
            rpcToken = token
            return client
        }
    }

    private fun closeRpcLocked() {
        val client = rpc ?: return
        runCatching { client.closeRPC() }
            .onFailure { Timber.tag(TAG).w(it, "failed to close DiscordRPC client") }
        rpc = null
        rpcToken = null
    }
}
