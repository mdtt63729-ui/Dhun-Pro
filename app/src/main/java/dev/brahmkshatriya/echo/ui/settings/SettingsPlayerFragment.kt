package dev.brahmkshatriya.echo.ui.settings


import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toResourceImageHolder
import dev.brahmkshatriya.echo.playback.PlayerService.Companion.CACHE_SIZE
import dev.brahmkshatriya.echo.playback.PlayerService.Companion.CLOSE_PLAYER
import dev.brahmkshatriya.echo.playback.PlayerService.Companion.MORE_BRAIN_CAPACITY
import dev.brahmkshatriya.echo.playback.PlayerService.Companion.SKIP_SILENCE
import dev.brahmkshatriya.echo.playback.PlayerService.Companion.STREAM_QUALITY
import dev.brahmkshatriya.echo.playback.PlayerService.Companion.UNMETERED_STREAM_QUALITY
import dev.brahmkshatriya.echo.playback.PlayerService.Companion.streamQualities
import dev.brahmkshatriya.echo.playback.listener.PlayerRadio.Companion.AUTO_START_RADIO
import dev.brahmkshatriya.echo.ui.common.FragmentUtils.openFragment
import dev.brahmkshatriya.echo.ui.player.PlayerViewModel.Companion.KEEP_QUEUE
import dev.brahmkshatriya.echo.ui.settings.AudioEffectsFragment.Companion.AUDIO_FX
import dev.brahmkshatriya.echo.utils.ContextUtils.SETTINGS_NAME
import dev.brahmkshatriya.echo.utils.ui.prefs.MaterialListPreference
import dev.brahmkshatriya.echo.utils.ui.prefs.MaterialSliderPreference
import dev.brahmkshatriya.echo.utils.ui.prefs.TransitionPreference
import dev.brahmkshatriya.echo.utils.SettingsKeys

class SettingsPlayerFragment : BaseSettingsFragment() {
    override val title get() = getString(R.string.player)
    override val icon get() = R.drawable.ic_play_circle.toResourceImageHolder()
    override val creator = { AudioPreference() }

    class AudioPreference : PreferenceFragmentCompat() {

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

            // ---- Now Playing styles (from the Muso player) ----

            PreferenceCategory(context).apply {
                title = getString(R.string.player_section)
                key = "player"
                isIconSpaceReserved = false
                layoutResource = R.layout.preference_category
                screen.addPreference(this)

                MaterialListPreference(context).apply {
                    key = SettingsKeys.PLAYER_STYLE
                    title = getString(R.string.player_style)
                    summary = getString(R.string.player_style_summary)
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    entries = arrayOf(
                        getString(R.string.style_material3),
                        getString(R.string.style_spotify),
                        getString(R.string.style_apple_music),
                        getString(R.string.style_m3_expressive)
                    )
                    entryValues = arrayOf(
                        SettingsKeys.PLAYER_STYLE_MATERIAL3,
                        SettingsKeys.PLAYER_STYLE_SPOTIFY,
                        SettingsKeys.PLAYER_STYLE_APPLE,
                        SettingsKeys.PLAYER_STYLE_M3E
                    )
                    setDefaultValue(SettingsKeys.PLAYER_STYLE_MATERIAL3)
                    addPreference(this)
                }

                MaterialListPreference(context).apply {
                    key = SettingsKeys.SLIDER_STYLE
                    title = getString(R.string.slider_style)
                    summary = getString(R.string.slider_style_summary)
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    entries = arrayOf(
                        getString(R.string.slider_standard),
                        getString(R.string.slider_wavy),
                        getString(R.string.slider_thick),
                        getString(R.string.slider_circular),
                        getString(R.string.slider_simple)
                    )
                    entryValues = arrayOf(
                        SettingsKeys.SLIDER_STYLE_STANDARD,
                        SettingsKeys.SLIDER_STYLE_WAVY,
                        SettingsKeys.SLIDER_STYLE_THICK,
                        SettingsKeys.SLIDER_STYLE_CIRCULAR,
                        SettingsKeys.SLIDER_STYLE_SIMPLE
                    )
                    setDefaultValue(SettingsKeys.SLIDER_STYLE_STANDARD)
                    addPreference(this)
                }
            }

            PreferenceCategory(context).apply {
                title = getString(R.string.playback)
                key = "playback"
                isIconSpaceReserved = false
                layoutResource = R.layout.preference_category
                screen.addPreference(this)

                TransitionPreference(context).apply {
                    key = AUDIO_FX
                    title = getString(R.string.audio_fx)
                    summary = getString(R.string.audio_fx_summary)
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    addPreference(this)
                }

                MaterialListPreference(context).apply {
                    key = STREAM_QUALITY
                    title = getString(R.string.stream_quality)
                    summary = getString(R.string.stream_quality_summary)
                    entries = context.resources.getStringArray(R.array.stream_qualities)
                    entryValues = streamQualities
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    setDefaultValue(streamQualities[1])
                    addPreference(this)
                }

                MaterialListPreference(context).apply {
                    key = UNMETERED_STREAM_QUALITY
                    title = getString(R.string.unmetered_stream_quality)
                    summary = getString(R.string.unmetered_stream_quality_summary)
                    entries =
                        context.resources.getStringArray(R.array.stream_qualities) + getString(R.string.off)
                    entryValues = streamQualities + "off"
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    setDefaultValue("off")
                    addPreference(this)
                }

                // ---- added from ArchiveTune ----

                SwitchPreferenceCompat(context).apply {
                    key = SettingsKeys.AUDIO_NORMALIZATION
                    title = getString(R.string.audio_normalization)
                    summary = getString(R.string.audio_normalization_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = SettingsKeys.LOW_DATA_MODE
                    title = getString(R.string.low_data_mode)
                    summary = getString(R.string.low_data_mode_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                MaterialListPreference(context).apply {
                    key = SettingsKeys.PRELOAD_MODE
                    title = getString(R.string.preload_mode)
                    summary = getString(R.string.preload_mode_summary)
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    entries = arrayOf(
                        getString(R.string.preload_off),
                        getString(R.string.preload_wifi),
                        getString(R.string.preload_always)
                    )
                    entryValues = arrayOf(
                        SettingsKeys.PRELOAD_OFF,
                        SettingsKeys.PRELOAD_WIFI,
                        SettingsKeys.PRELOAD_ALWAYS
                    )
                    setDefaultValue(SettingsKeys.PRELOAD_OFF)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = SettingsKeys.SHOW_VOLUME_BAR
                    title = getString(R.string.show_volume_bar)
                    summary = getString(R.string.show_volume_bar_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = SettingsKeys.NERD_STATS
                    title = getString(R.string.nerd_stats)
                    summary = getString(R.string.nerd_stats_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }
            }

            PreferenceCategory(context).apply {
                title = getString(R.string.behavior)
                key = "behavior"
                isIconSpaceReserved = false
                layoutResource = R.layout.preference_category
                screen.addPreference(this)

                SwitchPreferenceCompat(context).apply {
                    key = KEEP_QUEUE
                    title = getString(R.string.keep_queue)
                    summary = getString(R.string.keep_queue_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = CLOSE_PLAYER
                    title = getString(R.string.stop_player)
                    summary = getString(R.string.stop_player_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = SKIP_SILENCE
                    title = getString(R.string.skip_silence)
                    summary = getString(R.string.skip_silence_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = MORE_BRAIN_CAPACITY
                    title = getString(R.string.more_brain_capacity)
                    summary = getString(R.string.more_brain_capacity_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = AUTO_START_RADIO
                    title = getString(R.string.auto_start_radio)
                    summary = getString(R.string.auto_start_radio_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }

                MaterialSliderPreference(context, 200, 1000, allowOverride = true).apply {
                    key = CACHE_SIZE
                    title = getString(R.string.cache_size)
                    summary = getString(R.string.cache_size_summary)
                    isIconSpaceReserved = false
                    setDefaultValue(250)
                    addPreference(this)
                }

                // ---- added from ArchiveTune ----

                SwitchPreferenceCompat(context).apply {
                    key = SettingsKeys.KEEP_CPU_AWAKE
                    title = getString(R.string.keep_cpu_awake)
                    summary = getString(R.string.keep_cpu_awake_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = SettingsKeys.AUTO_SKIP_ERROR
                    title = getString(R.string.auto_skip_error)
                    summary = getString(R.string.auto_skip_error_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = SettingsKeys.PERMANENT_SHUFFLE
                    title = getString(R.string.permanent_shuffle)
                    summary = getString(R.string.permanent_shuffle_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = SettingsKeys.PAUSE_ON_ZERO_VOLUME
                    title = getString(R.string.pause_on_zero_volume)
                    summary = getString(R.string.pause_on_zero_volume_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = SettingsKeys.AUTO_START_BLUETOOTH
                    title = getString(R.string.auto_start_bluetooth)
                    summary = getString(R.string.auto_start_bluetooth_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }
            }
        }

        override fun onPreferenceTreeClick(preference: Preference): Boolean {
            val view = listView.findViewById<View>(preference.key.hashCode())
            return when (preference.key) {
                AUDIO_FX -> {
                    requireActivity().openFragment<AudioEffectsFragment>(view)
                    true
                }

                else -> false
            }
        }
    }
}
