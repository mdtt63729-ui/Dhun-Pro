/*
 * Dhun Project Original (2026)
 * Dhun
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package dev.brahmkshatriya.echo.dhun.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.layout.asPaddingValues
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.composable
import android.webkit.WebView
import android.webkit.WebViewClient
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.dhun.LocalPlayerAwareWindowInsets
import dev.brahmkshatriya.echo.dhun.ui.screens.musicrecognition.MusicRecognitionRoute
import dev.brahmkshatriya.echo.dhun.ui.screens.musicrecognition.MusicRecognitionScreen
import dev.brahmkshatriya.echo.dhun.ui.screens.settings.AccountSettings
import dev.brahmkshatriya.echo.dhun.spotify.SpotifyPlaylistViewModel
import dev.brahmkshatriya.echo.dhun.ui.screens.search.SearchResultsScreen

/**
 * Name of the login URL argument used both by the `Dhun://login` deep link and
 * the `login/{url}` navigation route.
 */
const val LOGIN_URL_ARGUMENT = "url"

/** Fallback URL opened by the in-app login web view. */
const val YOUTUBE_LOGIN_URL = "https://music.youtube.com/"

/**
 * Builds the route used to open the login screen. Blank/null URLs map to the
 * bare `login` route (which shows the default login URL).
 */
fun buildLoginRoute(url: String?): String =
    if (url.isNullOrBlank()) "login" else "login/${Uri.encode(url)}"

/**
 * A top level navigation destination of the app shell.
 *
 * `route` is the navigation route pattern, `titleId`/`iconIdActive`/
 * `iconIdInactive` are consumed by the navigation rail and the floating
 * navigation toolbar.
 */
class Screens(
    val route: String,
    val titleId: Int,
    val iconIdActive: Int,
    val iconIdInactive: Int,
) {
    companion object {
        val Home = Screens(
            route = "home",
            titleId = R.string.home,
            iconIdActive = R.drawable.home_filled,
            iconIdInactive = R.drawable.home_outlined,
        )

        val Search = Screens(
            route = "search",
            titleId = R.string.search,
            iconIdActive = R.drawable.ic_search_filled,
            iconIdInactive = R.drawable.ic_search_outline,
        )

        val MoodAndGenres = Screens(
            route = "mood_and_genres",
            titleId = R.string.mood_and_genres,
            iconIdActive = R.drawable.explore_filled,
            iconIdInactive = R.drawable.explore_outlined,
        )

        val Library = Screens(
            route = "library",
            titleId = R.string.filter_library,
            iconIdActive = R.drawable.art_library_music,
            iconIdInactive = R.drawable.ic_library_music,
        )

        val DownloadQueue = Screens(
            route = "download_queue",
            titleId = R.string.downloads,
            iconIdActive = R.drawable.download,
            iconIdInactive = R.drawable.ic_download,
        )

        /** Tabs shown in the navigation rail / floating toolbar. */
        val MainScreens = listOf(Home, Search, MoodAndGenres, Library)
    }
}

/**
 * Registers every destination of the app shell on the [NavGraphBuilder].
 * Called from `MainActivity` inside its `NavHost { ... }` block.
 */
@OptIn(ExperimentalMaterial3Api::class)
fun NavGraphBuilder.navigationBuilder(
    navController: NavHostController,
    topAppBarScrollBehavior: TopAppBarScrollBehavior,
    latestVersionName: String,
) {
    composable(Screens.Home.route) {
        HomeScreen(navController = navController)
    }

    // The search tab is surfaced through the search overlay; the bare route
    // exists so the start destination / back stack stay well-formed.
    composable(Screens.Search.route) {
        PlaceholderScreen(title = stringResource(R.string.search))
    }

    composable(Screens.MoodAndGenres.route) {
        PlaceholderScreen(title = stringResource(R.string.mood_and_genres))
    }

    composable(Screens.Library.route) {
        PlaceholderScreen(title = stringResource(R.string.filter_library))
    }

    composable(Screens.DownloadQueue.route) {
        PlaceholderScreen(title = stringResource(R.string.downloads))
    }

    composable(
        route = "search/{query}",
        arguments = listOf(navArgument("query") { type = NavType.StringType }),
    ) { entry ->
        SearchResultsScreen(
            query = entry.arguments?.getString("query").orEmpty(),
            navController = navController,
        )
    }

    composable(
        route = "account",
    ) {
        AccountSettings(
            navController = navController,
            onClose = { navController.popBackStack() },
            latestVersionName = latestVersionName,
        )
    }

    composable(
        route = "login",
    ) {
        LoginWebViewScreen(url = YOUTUBE_LOGIN_URL)
    }

    composable(
        route = "login/{$LOGIN_URL_ARGUMENT}",
        arguments = listOf(navArgument(LOGIN_URL_ARGUMENT) { type = NavType.StringType }),
    ) { entry ->
        LoginWebViewScreen(
            url = Uri.decode(entry.arguments?.getString(LOGIN_URL_ARGUMENT).orEmpty()),
        )
    }

    composable(MusicRecognitionRoute) {
        MusicRecognitionScreen()
    }

    composable(
        route = "spotify_playlist/{playlistId}",
        arguments = listOf(navArgument("playlistId") { type = NavType.StringType }),
    ) {
        val viewModel: SpotifyPlaylistViewModel = hiltViewModel()
        val state by viewModel.uiState.collectAsState()
        SpotifyPlaylistScreen(
            state = state,
            onRetry = viewModel::reload,
        )
    }

    composable(
        route = "history",
    ) {
        PlaceholderScreen(title = stringResource(R.string.history))
    }

    composable(
        route = "stats",
    ) {
        PlaceholderScreen(title = stringResource(R.string.stats))
    }

    composable(
        route = "new_release",
    ) {
        PlaceholderScreen(title = stringResource(R.string.new_release_albums))
    }

    composable(
        route = "settings",
    ) {
        PlaceholderScreen(title = stringResource(R.string.settings))
    }

    composable(
        route = "year_in_music",
    ) {
        PlaceholderScreen(title = "Year in Music")
    }

    composable(
        route = "always_on_display",
    ) {
        PlaceholderScreen(title = "Always On Display")
    }

    composable(
        route = "album/{browseId}",
        arguments = listOf(navArgument("browseId") { type = NavType.StringType }),
    ) { entry ->
        PlaceholderScreen(
            title = stringResource(R.string.filter_albums),
            subtitle = entry.arguments?.getString("browseId"),
        )
    }

    composable(
        route = "artist/{artistId}",
        arguments = listOf(navArgument("artistId") { type = NavType.StringType }),
    ) { entry ->
        PlaceholderScreen(
            title = stringResource(R.string.filter_artists),
            subtitle = entry.arguments?.getString("artistId"),
        )
    }

    composable(
        route = "browse/{browseId}",
        arguments = listOf(navArgument("browseId") { type = NavType.StringType }),
    ) { entry ->
        PlaceholderScreen(
            title = stringResource(R.string.explore),
            subtitle = entry.arguments?.getString("browseId"),
        )
    }

    composable(
        route = "online_playlist/{playlistId}",
        arguments = listOf(navArgument("playlistId") { type = NavType.StringType }),
    ) { entry ->
        PlaceholderScreen(
            title = stringResource(R.string.filter_playlists),
            subtitle = entry.arguments?.getString("playlistId"),
        )
    }

    composable(
        route = "local_playlist/{playlistId}",
        arguments = listOf(navArgument("playlistId") { type = NavType.StringType }),
    ) { entry ->
        PlaceholderScreen(
            title = stringResource(R.string.filter_playlists),
            subtitle = entry.arguments?.getString("playlistId"),
        )
    }
}

/** Simple centered stand-in screen used for destinations without a body yet. */
@Composable
private fun PlaceholderScreen(
    title: String,
    subtitle: String? = null,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(LocalPlayerAwareWindowInsets.current.asPaddingValues())
            .padding(32.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp),
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )
        }
    }
}

/** In-app web view used for the `Dhun://login` deep-link flow. */
@Composable
private fun LoginWebViewScreen(url: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(LocalPlayerAwareWindowInsets.current.asPaddingValues()),
    ) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    webViewClient = WebViewClient()
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    loadUrl(url)
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/** Renders the Spotify playlist state exposed by [SpotifyPlaylistViewModel]. */
@Composable
private fun SpotifyPlaylistScreen(
    state: dev.brahmkshatriya.echo.dhun.spotify.SpotifyPlaylistUiState,
    onRetry: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(LocalPlayerAwareWindowInsets.current.asPaddingValues()),
    ) {
        when {
            state.isLoading -> CircularProgressIndicator()

            state.errorMessage != null -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = state.errorMessage ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp),
                )
                TextButton(onClick = onRetry) {
                    Text(stringResource(android.R.string.ok))
                }
            }

            else -> Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                Text(
                    text = state.playlist?.name.orEmpty(),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                )
                Text(
                    text = state.playlist?.owner?.displayName
                        ?: state.playlist?.owner?.id
                        ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
                if (state.tracks.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_results_found),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
                state.tracks.forEach { track ->
                    Text(
                        text = track.name + if (track.artists.isNotEmpty()) {
                            " — " + track.artists.joinToString { it.name }
                        } else {
                            ""
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}
