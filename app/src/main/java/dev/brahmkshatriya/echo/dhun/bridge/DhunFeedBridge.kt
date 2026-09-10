package dev.brahmkshatriya.echo.dhun.bridge

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DhunFeedBridge {
    suspend fun fetchFeed(extension: MusicExtension): List<Shelf> {
        return withContext(Dispatchers.IO) {
            try { extension.get { it.loadHomeFeed().shelves }.getOrNull() ?: emptyList() }
            catch (e: Exception) { emptyList() }
        }
    }
    @Composable
    fun rememberExtensionFeed(extension: MusicExtension?): List<Shelf> {
        var feed by remember { mutableStateOf<List<Shelf>>(emptyList()) }
        LaunchedEffect(extension?.id) { if (extension != null) feed = fetchFeed(extension) }
        return feed
    }
}
