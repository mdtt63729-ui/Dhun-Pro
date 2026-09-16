/*
 * Dhun Project Original (2026)
 * Dhun
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package dev.brahmkshatriya.echo.dhun.ui.screens.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.dhun.LocalDatabase
import dev.brahmkshatriya.echo.dhun.LocalPlayerConnection
import dev.brahmkshatriya.echo.dhun.models.toMediaMetadata
import dev.brahmkshatriya.echo.dhun.playback.queues.YouTubeQueue
import dev.brahmkshatriya.echo.dhun.ui.component.ListItem
import dev.brahmkshatriya.echo.dhun.ui.component.NavigationTitle

/**
 * On-device search results shown inside the top search bar while the
 * LOCAL search source is selected (see `MainActivity`'s `Crossfade`).
 */
@Composable
fun LocalSearchScreen(
    query: String,
    navController: NavController,
    onDismiss: () -> Unit,
    pureBlack: Boolean,
) {
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current

    val songs by database.searchSongs(query, previewSize = 20)
        .collectAsState(initial = emptyList())
    val artists by database.searchArtists(query, previewSize = 20)
        .collectAsState(initial = emptyList())
    val albums by database.searchAlbums(query, previewSize = 20)
        .collectAsState(initial = emptyList())
    val playlists by database.searchPlaylists(query, previewSize = 20)
        .collectAsState(initial = emptyList())

    val hasQuery = query.isNotBlank()
    val isBlankResult = hasQuery &&
            songs.isEmpty() && artists.isEmpty() &&
            albums.isEmpty() && playlists.isEmpty()

    if (!hasQuery) {
        // Nothing to search yet — the overlay is empty until the user types.
        Box(Modifier.fillMaxSize())
        return
    }

    LazyColumn(Modifier.fillMaxSize()) {
        if (isBlankResult) {
            item {
                NavigationTitle(title = stringResource(R.string.no_results_found))
            }
        }

        if (songs.isNotEmpty()) {
            item { NavigationTitle(title = stringResource(R.string.filter_songs)) }
            items(songs.size) { index ->
                val song = songs[index]
                ListItem(
                    title = song.title,
                    subtitle = song.artists.joinToString { it.name },
                    thumbnailContent = {
                        SearchThumbnail(
                            url = song.thumbnailUrl,
                            circle = false,
                        )
                    },
                    modifier = Modifier.clickable {
                        playerConnection?.playQueue(
                            YouTubeQueue.radio(song.toMediaMetadata()),
                        )
                        onDismiss()
                    },
                )
            }
        }

        if (artists.isNotEmpty()) {
            item { NavigationTitle(title = stringResource(R.string.filter_artists)) }
            items(artists.size) { index ->
                val artist = artists[index]
                ListItem(
                    title = artist.title,
                    subtitle = artist.songCount.toString(),
                    thumbnailContent = {
                        SearchThumbnail(
                            url = artist.thumbnailUrl,
                            circle = true,
                        )
                    },
                    modifier = Modifier.clickable {
                        navController.navigate("artist/${artist.id}")
                        onDismiss()
                    },
                )
            }
        }

        if (albums.isNotEmpty()) {
            item { NavigationTitle(title = stringResource(R.string.filter_albums)) }
            items(albums.size) { index ->
                val album = albums[index]
                ListItem(
                    title = album.title,
                    subtitle = album.album.year?.toString(),
                    thumbnailContent = {
                        SearchThumbnail(
                            url = album.thumbnailUrl,
                            circle = false,
                        )
                    },
                    modifier = Modifier.clickable {
                        navController.navigate("album/${album.id}")
                        onDismiss()
                    },
                )
            }
        }

        if (playlists.isNotEmpty()) {
            item { NavigationTitle(title = stringResource(R.string.filter_playlists)) }
            items(playlists.size) { index ->
                val playlist = playlists[index]
                ListItem(
                    title = playlist.title,
                    subtitle = playlist.songCount.toString(),
                    thumbnailContent = {
                        SearchThumbnail(
                            url = playlist.thumbnails.firstOrNull(),
                            circle = false,
                        )
                    },
                    modifier = Modifier.clickable {
                        navController.navigate("local_playlist/${playlist.id}")
                        onDismiss()
                    },
                )
            }
        }
    }
}

@Composable
private fun SearchThumbnail(
    url: String?,
    circle: Boolean,
) {
    if (url != null) {
        AsyncImage(
            model = url,
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .clip(if (circle) CircleShape else RoundedCornerShape(8.dp)),
        )
    } else {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.music_note),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
