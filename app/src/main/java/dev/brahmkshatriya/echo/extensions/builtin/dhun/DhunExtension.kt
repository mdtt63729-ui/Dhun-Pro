package dev.brahmkshatriya.echo.extensions.builtin.dhun

import dev.brahmkshatriya.echo.BuildConfig
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.HomeFeedClient
import dev.brahmkshatriya.echo.common.clients.SearchFeedClient
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.ExtensionType
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toResourceImageHolder
import dev.brahmkshatriya.echo.common.models.ImportType
import dev.brahmkshatriya.echo.common.models.Metadata
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Tab
import dev.brahmkshatriya.echo.common.settings.Setting
import dev.brahmkshatriya.echo.common.settings.Settings

/**
 * Dhun — a builtin music extension that serves as the default selected extension.
 * Shows a placeholder home feed. When the user selects Dhun from the extension
 * switcher, a new custom UI will be shown (to be implemented later).
 *
 * This extension is always present and cannot be uninstalled (ImportType.BuiltIn).
 * It appears in the extension switcher alongside other music extensions.
 */
class DhunExtension : ExtensionClient, HomeFeedClient, SearchFeedClient {

    companion object {
        val metadata = Metadata(
            className = "DhunExtension",
            path = "",
            importType = ImportType.BuiltIn,
            type = ExtensionType.MUSIC,
            id = "dhun",
            name = "Dhun",
            description = "Default music extension with personalized recommendations.",
            version = "v${BuildConfig.VERSION_CODE}",
            author = "Echo",
            icon = R.drawable.ic_dhun.toResourceImageHolder(),
        )
    }

    override suspend fun onExtensionSelected() {
        // Called when user selects Dhun as their music extension
    }

    override suspend fun onInitialize() {
        // Called once when the extension is loaded
    }

    override suspend fun loadHomeFeed(): Feed<Shelf> {
        // Placeholder empty feed — the actual UI will be a custom fragment
        return Feed(listOf())
    }

    override suspend fun getSettingItems(): List<Setting> {
        return listOf()
    }

    override suspend fun onSettingsChanged(settings: Settings, key: String?) {
        // No settings to handle
    }

    override val tabs: List<Tab>
        get() = listOf(Tab("home", "Home"))

    override suspend fun search(query: String, tab: Tab?, page: Int): PagedData<EchoMediaItem> {
        return PagedData.Single { listOf() }
    }

    override fun searchableTabs(): List<Tab> {
        return listOf(Tab("all", "All"))
    }
}
