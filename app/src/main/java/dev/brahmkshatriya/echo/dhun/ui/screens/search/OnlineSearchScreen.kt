/*
 * Dhun Project Original (2026)
 * Dhun
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package dev.brahmkshatriya.echo.dhun.ui.screens.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.dhun.LocalDatabase
import dev.brahmkshatriya.echo.dhun.LocalPlayerConnection
import dev.brahmkshatriya.echo.dhun.innertube.YouTube
import dev.brahmkshatriya.echo.dhun.innertube.models.AlbumItem
import dev.brahmkshatriya.echo.dhun.innertube.models.ArtistItem
import dev.brahmkshatriya.echo.dhun.innertube.models.PlaylistItem
import dev.brahmkshatriya.echo.dhun.innertube.models.SearchSuggestions
import dev.brahmkshatriya.echo.dhun.innertube.models.SongItem
import dev.brahmkshatriya.echo.dhun.innertube.models.YTItem
import dev.brahmkshatriya.echo.dhun.models.toMediaMetadata
import dev.brahmkshatriya.echo.dhun.playback.queues.YouTubeQueue
import dev.brahmkshatriya.echo.dhun.ui.component.ListItem
import dev.brahmkshatriya.echo.dhun.ui.component.NavigationTitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Online (YouTube Music) suggestions shown inside the top search bar while
 * the ONLINE search source is selected. Tapping a suggestion or a recommended
 * item triggers [onSearch] / navigates to the matching detail screen.
 */
@Composable
fun OnlineSearchScreen(
    query: String,
    onQueryChange: (androidx.compose.ui.text.input.TextFieldValue) -> Unit,
    navController: NavController,
    onSearch: (String) -> Unit,
    onDismiss: () -> Unit,
    pureBlack: Boolean,
) {
    val database = LocalDatabase.current

    var suggestions by remember { mutableStateOf<SearchSuggestions?>(null) }

    val history by database.searchHistory(query = "")
        .collectAsState(initial = emptyList())

    LaunchedEffect(query) {
        suggestions = if (query.isBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                YouTube.searchSuggestions(query).getOrNull()
            }
        }
    }

    LazyColumn(Modifier.fillMaxSize()) {
        if (query.isBlank()) {
            if (history.isNotEmpty()) {
                item { NavigationTitle(title = stringResource(R.string.history)) }
            }
            items(history.size) { index ->
                val entry = history[index]
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSearch(entry.query) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.history),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = entry.query,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 16.dp),
                    )
                }
            }
        } else {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSearch(query) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.search),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.search) + ": " + query,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 16.dp),
                    )
                }
            }

            val queries = suggestions?.queries.orEmpty()
            items(queries.size) { index ->
                val suggestion = queries[index]
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSearch(suggestion) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.search),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = suggestion,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 16.dp),
                    )
                }
            }

            val recommended = suggestions?.recommendedItems.orEmpty()
            if (recommended.isNotEmpty()) {
                item { NavigationTitle(title = stringResource(R.string.top_results)) }
                items(recommended.size) { index ->
                    val item = recommended[index]
                    OnlineResultRow(
                        item = item,
                        navController = navController,
                        onDismiss = onDismiss,
                    )
                }
            }
        }
    }
}

/**
 * Full online search results shown at the `search/{query}` destination.
 */
@Composable
fun SearchResultsScreen(
    query: String,
    navController: NavController,
) {
    val playerConnection = LocalPlayerConnection.current

    var results by remember { mutableStateOf<List<YTItem>?>(null) }
    var searched by remember { mutableStateOf(false) }

    LaunchedEffect(query) {
        results = null
        searched = false
        results = withContext(Dispatchers.IO) {
            YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrNull()?.items
        }
        searched = true
    }

    Box(Modifier.fillMaxSize()) {
        if (!searched) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        } else if (results.isNullOrEmpty()) {
            Text(
                text = stringResource(R.string.no_results_found),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(results!!.size) { index ->
                    val item = results!![index]
                    OnlineResultRow(
                        item = item,
                        navController = navController,
                        onDismiss = {},
                        onPlay = { song ->
                            playerConnection?.playQueue(
                                YouTubeQueue.radio(song.toMediaMetadata()),
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun OnlineResultRow(
    item: YTItem,
    navController: NavController,
    onDismiss: () -> Unit,
    onPlay: (SongItem) -> Unit = {},
) {
    when (item) {
        is SongItem -> ListItem(
            title = item.title,
            subtitle = item.artists.joinToString { it.name },
            thumbnailContent = { OnlineThumbnail(item.thumbnail) },
            modifier = Modifier.clickable {
                onPlay(item)
                onDismiss()
            },
        )

        is AlbumItem -> ListItem(
            title = item.title,
            subtitle = item.artists?.joinToString { it.name },
            thumbnailContent = { OnlineThumbnail(item.thumbnail) },
            modifier = Modifier.clickable {
                navController.navigate("album/${item.browseId}")
                onDismiss()
            },
        )

        is ArtistItem -> ListItem(
            title = item.title,
            subtitle = stringResource(R.string.filter_artists),
            thumbnailContent = { OnlineThumbnail(item.thumbnail, circle = true) },
            modifier = Modifier.clickable {
                navController.navigate("artist/${item.id}")
                onDismiss()
            },
        )

        is PlaylistItem -> ListItem(
            title = item.title,
            subtitle = item.author?.name ?: item.songCountText,
            thumbnailContent = { OnlineThumbnail(item.thumbnail) },
            modifier = Modifier.clickable {
                navController.navigate("online_playlist/${item.id}")
                onDismiss()
            },
        )
    }
}

@Composable
private fun OnlineThumbnail(
    url: String?,
    circle: Boolean = false,
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
