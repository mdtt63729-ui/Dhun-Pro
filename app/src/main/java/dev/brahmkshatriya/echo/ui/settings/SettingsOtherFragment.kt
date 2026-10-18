package dev.brahmkshatriya.echo.ui.settings

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.PreferenceFragmentCompat
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toResourceImageHolder
import dev.brahmkshatriya.echo.ui.extensions.ExtensionsViewModel
import dev.brahmkshatriya.echo.utils.ContextUtils.SETTINGS_NAME
import dev.brahmkshatriya.echo.utils.PermsUtils.registerActivityResultLauncher
import dev.brahmkshatriya.echo.utils.exportSettings
import dev.brahmkshatriya.echo.utils.importSettings
import dev.brahmkshatriya.echo.utils.ui.prefs.SwitchLongClickPreference
import dev.brahmkshatriya.echo.utils.ui.prefs.TransitionPreference
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import androidx.preference.PreferenceCategory
import androidx.preference.SwitchPreferenceCompat
import dev.brahmkshatriya.echo.utils.SettingsKeys
import dev.brahmkshatriya.echo.utils.net.HttpClient
import dev.brahmkshatriya.echo.utils.ui.prefs.MaterialListPreference
import dev.brahmkshatriya.echo.utils.ui.prefs.MaterialTextInputPreference
import android.widget.Toast
import androidx.media3.datasource.cache.SimpleCache
import coil3.imageLoader
import dev.brahmkshatriya.echo.utils.ui.prefs.MaterialSliderPreference
import org.koin.core.context.GlobalContext
import dev.brahmkshatriya.echo.ui.common.FragmentUtils.openFragment

class SettingsOtherFragment : BaseSettingsFragment() {
    override val title get() = getString(R.string.other_settings)
    override val icon get() = R.drawable.ic_more_horiz.toResourceImageHolder()
    override val creator = { OtherPreference() }

    class OtherPreference : PreferenceFragmentCompat() {

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            configure()
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            val context = preferenceManager.context
            preferenceManager.sharedPreferencesName = SETTINGS_NAME
            preferenceManager.sharedPreferencesMode = Context.MODE_PRIVATE
            val screen = preferenceManager.createPreferenceScreen(context)
            preferenceScreen = screen

            SwitchLongClickPreference(context).apply {
                title = getString(R.string.check_for_updates)
                summary = getString(R.string.check_for_updates_summary)
                key = "check_for_updates"
                layoutResource = R.layout.preference_switch
                isIconSpaceReserved = false
                setDefaultValue(true)
                screen.addPreference(this)
                setOnLongClickListener {
                    val viewModel by activityViewModel<ExtensionsViewModel>()
                    viewModel.update(requireActivity(), true)
                }
            }

            TransitionPreference(context).apply {
                key = "export"
                title = getString(R.string.export_settings)
                summary = getString(R.string.export_settings_summary)
                layoutResource = R.layout.preference
                isIconSpaceReserved = false
                screen.addPreference(this)
                setOnPreferenceClickListener {
                    val contract = ActivityResultContracts.CreateDocument("application/json")
                    requireActivity().registerActivityResultLauncher(contract) { uri ->
                        uri?.let { context.exportSettings(it) }
                    }.launch("echo-settings.json")
                    true
                }
            }

            TransitionPreference(context).apply {
                key = "import"
                title = getString(R.string.import_settings)
                summary = getString(R.string.import_settings_summary)
                layoutResource = R.layout.preference
                isIconSpaceReserved = false
                screen.addPreference(this)
                setOnPreferenceClickListener {
                    val contract = ActivityResultContracts.OpenDocument()
                    requireActivity().registerActivityResultLauncher(contract) {
                        it?.let {
                            context.importSettings(it)
                            requireActivity().recreate()
                        }
                    }.launch(arrayOf("application/json"))
                    true
                }
            }

            // ---- added from ArchiveTune: Internet ----

            PreferenceCategory(context).apply {
                title = getString(R.string.internet)
                key = "internet"
                isIconSpaceReserved = false
                layoutResource = R.layout.preference_category
                screen.addPreference(this)

                SwitchPreferenceCompat(context).apply {
                    key = SettingsKeys.DOH_ENABLED
                    title = getString(R.string.doh)
                    summary = getString(R.string.doh_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                MaterialListPreference(context).apply {
                    key = SettingsKeys.DOH_PROVIDER
                    title = getString(R.string.doh_provider)
                    summary = getString(R.string.doh_provider_summary)
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    entries = HttpClient.dohProviders
                    entryValues = HttpClient.dohProviderValues
                    setDefaultValue(HttpClient.dohProviderValues[0])
                    addPreference(this)
                }

                MaterialTextInputPreference(context).apply {
                    key = SettingsKeys.DOH_URL
                    title = getString(R.string.doh_url)
                    summary = getString(R.string.doh_url_summary)
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    setDefaultValue("")
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = SettingsKeys.PROXY_ENABLED
                    title = getString(R.string.proxy)
                    summary = getString(R.string.proxy_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                MaterialTextInputPreference(context).apply {
                    key = SettingsKeys.PROXY_HOST
                    title = getString(R.string.proxy_host)
                    summary = getString(R.string.proxy_host_summary)
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    setDefaultValue("")
                    addPreference(this)
                }

                MaterialTextInputPreference(context).apply {
                    key = SettingsKeys.PROXY_PORT
                    title = getString(R.string.proxy_port)
                    summary = getString(R.string.proxy_port_summary)
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    setDefaultValue("")
                    addPreference(this)
                }
            }

            // ---- added from ArchiveTune: Storage ----

            PreferenceCategory(context).apply {
                title = getString(R.string.storage)
                key = "storage"
                isIconSpaceReserved = false
                layoutResource = R.layout.preference_category
                screen.addPreference(this)

                MaterialSliderPreference(context, 50, 1000, allowOverride = true).apply {
                    key = SettingsKeys.IMAGE_CACHE_SIZE
                    title = getString(R.string.image_cache_size)
                    summary = getString(R.string.image_cache_size_summary)
                    isIconSpaceReserved = false
                    setDefaultValue(SettingsKeys.IMAGE_CACHE_SIZE_DEFAULT)
                    addPreference(this)
                }

                TransitionPreference(context).apply {
                    key = SettingsKeys.CLEAR_IMAGE_CACHE
                    title = getString(R.string.clear_image_cache)
                    summary = getString(R.string.clear_image_cache_summary)
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    addPreference(this)
                    setOnPreferenceClickListener {
                        runCatching { context.imageLoader.diskCache?.clear() }
                        Toast.makeText(
                            context, R.string.image_cache_cleared, Toast.LENGTH_SHORT
                        ).show()
                        true
                    }
                }

                TransitionPreference(context).apply {
                    key = SettingsKeys.CLEAR_SONG_CACHE
                    title = getString(R.string.clear_song_cache)
                    summary = getString(R.string.clear_song_cache_summary)
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    addPreference(this)
                    setOnPreferenceClickListener {
                        runCatching {
                            val cache = GlobalContext.get().get<SimpleCache>()
                            cache.keys.forEach { cache.removeResource(it) }
                        }
                        Toast.makeText(
                            context, R.string.song_cache_cleared, Toast.LENGTH_SHORT
                        ).show()
                        true
                    }
                }
            }

            // ---- added from ArchiveTune: Updates ----

            PreferenceCategory(context).apply {
                title = getString(R.string.updates)
                key = "updates"
                isIconSpaceReserved = false
                layoutResource = R.layout.preference_category
                screen.addPreference(this)

                MaterialListPreference(context).apply {
                    key = SettingsKeys.UPDATE_CHANNEL
                    title = getString(R.string.update_channel)
                    summary = getString(R.string.update_channel_summary)
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    entries = arrayOf(getString(R.string.stable), getString(R.string.beta_channel))
                    entryValues = arrayOf(
                        SettingsKeys.UPDATE_CHANNEL_STABLE, SettingsKeys.UPDATE_CHANNEL_BETA
                    )
                    setDefaultValue(SettingsKeys.UPDATE_CHANNEL_STABLE)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = SettingsKeys.UPDATE_NOTIFICATIONS
                    title = getString(R.string.update_notifications)
                    summary = getString(R.string.update_notifications_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }

                TransitionPreference(context).apply {
                    key = SettingsKeys.ABOUT
                    title = getString(R.string.about)
                    summary = getString(R.string.about_summary)
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    addPreference(this)
                    setOnPreferenceClickListener {
                        requireActivity().openFragment<AboutFragment>()
                        true
                    }
                }

                TransitionPreference(context).apply {
                    key = SettingsKeys.CHANGELOG
                    title = getString(R.string.changelog)
                    summary = getString(R.string.changelog_summary)
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    addPreference(this)
                    setOnPreferenceClickListener {
                        requireActivity().openFragment<ChangelogFragment>()
                        true
                    }
                }
            }
        }
    }
}