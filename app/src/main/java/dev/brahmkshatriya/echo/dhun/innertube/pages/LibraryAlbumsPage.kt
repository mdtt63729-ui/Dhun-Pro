/*
 * Dhun Project Original (2026)
 * Dhun
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package dev.brahmkshatriya.echo.dhun.innertube.pages

import dev.brahmkshatriya.echo.dhun.innertube.models.Album
import dev.brahmkshatriya.echo.dhun.innertube.models.AlbumItem
import dev.brahmkshatriya.echo.dhun.innertube.models.Artist
import dev.brahmkshatriya.echo.dhun.innertube.models.ArtistItem
import dev.brahmkshatriya.echo.dhun.innertube.models.MusicResponsiveListItemRenderer
import dev.brahmkshatriya.echo.dhun.innertube.models.MusicTwoRowItemRenderer
import dev.brahmkshatriya.echo.dhun.innertube.models.PlaylistItem
import dev.brahmkshatriya.echo.dhun.innertube.models.SongItem
import dev.brahmkshatriya.echo.dhun.innertube.models.YTItem
import dev.brahmkshatriya.echo.dhun.innertube.models.oddElements
import dev.brahmkshatriya.echo.dhun.innertube.utils.parseTime

data class LibraryAlbumsPage(
    val albums: List<AlbumItem>,
    val continuation: String?,
) {
    companion object {
        fun fromMusicTwoRowItemRenderer(renderer: MusicTwoRowItemRenderer): AlbumItem? {
            val browseId = renderer.navigationEndpoint.browseEndpoint?.browseId ?: return null
            val playlistId = renderer.thumbnailOverlay?.musicItemThumbnailOverlayRenderer?.content
                ?.musicPlayButtonRenderer?.playNavigationEndpoint
                ?.watchPlaylistEndpoint?.playlistId
                ?: renderer.menu?.menuRenderer?.items?.firstOrNull()
                    ?.menuNavigationItemRenderer?.navigationEndpoint
                    ?.watchPlaylistEndpoint?.playlistId
                ?: browseId.removePrefix("MPREb_").let { "OLAK5uy_$it" }

            return AlbumItem(
                browseId = browseId,
                playlistId = playlistId,
                title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                artists = null,
                year = renderer.subtitle?.runs?.lastOrNull()?.text?.toIntOrNull(),
                thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                explicit = renderer.subtitleBadges?.find {
                    it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                } != null
            )
        }
    }
}
