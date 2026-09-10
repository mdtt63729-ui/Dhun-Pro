package dev.brahmkshatriya.echo.ui.settings

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toResourceImageHolder
import dev.brahmkshatriya.echo.utils.ContextUtils.SETTINGS_NAME
import dev.brahmkshatriya.echo.utils.ui.prefs.MaterialListPreference
import dev.brahmkshatriya.echo.utils.ui.prefs.MaterialSliderPreference
import dev.brahmkshatriya.echo.utils.ui.prefs.MaterialTextInputPreference

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

            // ========== PROXY CATEGORY ==========
            PreferenceCategory(context).apply {
                title = getString(R.string.proxy_settings)
                key = "proxy"
                isIconSpaceReserved = false
                layoutResource = R.layout.preference_category
                screen.addPreference(this)

                SwitchPreferenceCompat(context).apply {
                    key = "proxyEnabled"
                    title = getString(R.string.enable_proxy)
                    summary = getString(R.string.enable_proxy_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                MaterialTextInputPreference(context).apply {
                    key = "proxyUrl"
                    title = getString(R.string.proxy_url)
                    summary = getString(R.string.proxy_url_summary)
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    addPreference(this)
                }

                MaterialListPreference(context).apply {
                    key = "proxyType"
                    title = getString(R.string.proxy_type)
                    summary = getString(R.string.proxy_type_summary)
                    entries = arrayOf(
                        getString(R.string.proxy_type_http),
                        getString(R.string.proxy_type_socks),
                        getString(R.string.proxy_type_direct)
                    )
                    entryValues = arrayOf("HTTP", "SOCKS", "DIRECT")
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    setDefaultValue("HTTP")
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = "streamBypassProxy"
                    title = getString(R.string.stream_bypass_proxy)
                    summary = getString(R.string.stream_bypass_proxy_desc)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }
            }

            // ========== SCROBBLING CATEGORY ==========
            PreferenceCategory(context).apply {
                title = getString(R.string.scrobbling)
                key = "scrobbling"
                isIconSpaceReserved = false
                layoutResource = R.layout.preference_category
                screen.addPreference(this)

                // Last.fm
                SwitchPreferenceCompat(context).apply {
                    key = "lastfmScrobblingEnable"
                    title = getString(R.string.enable_scrobbling)
                    summary = getString(R.string.enable_scrobbling_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = "lastfmUseNowPlaying"
                    title = getString(R.string.lastfm_now_playing)
                    summary = getString(R.string.lastfm_now_playing_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }

                MaterialSliderPreference(context, 0, 100, steps = 100).apply {
                    key = "scrobbleDelayPercent"
                    title = getString(R.string.scrobble_delay_percent)
                    summary = getString(R.string.scrobble_delay_percent_summary)
                    isIconSpaceReserved = false
                    setDefaultValue(50)
                    addPreference(this)
                }

                MaterialSliderPreference(context, 0, 600, steps = 600).apply {
                    key = "scrobbleMinSongDuration"
                    title = getString(R.string.scrobble_min_track_duration)
                    summary = getString(R.string.scrobble_min_track_duration_summary)
                    isIconSpaceReserved = false
                    setDefaultValue(30)
                    addPreference(this)
                }

                MaterialSliderPreference(context, 0, 60, steps = 60).apply {
                    key = "scrobbleDelaySeconds"
                    title = getString(R.string.scrobble_delay_minutes)
                    summary = getString(R.string.scrobble_delay_minutes_summary)
                    isIconSpaceReserved = false
                    setDefaultValue(0)
                    addPreference(this)
                }

                // ListenBrainz
                SwitchPreferenceCompat(context).apply {
                    key = "listenbrainz_enabled"
                    title = getString(R.string.listenbrainz_scrobbling)
                    summary = getString(R.string.listenbrainz_scrobbling_description)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                MaterialTextInputPreference(context).apply {
                    key = "listenbrainz_token"
                    title = getString(R.string.edit_listenbrainz_token)
                    summary = getString(R.string.set_listenbrainz_token)
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    addPreference(this)
                }
            }

            // ========== DISCORD RPC CATEGORY ==========
            PreferenceCategory(context).apply {
                title = getString(R.string.discord_integration)
                key = "discord"
                isIconSpaceReserved = false
                layoutResource = R.layout.preference_category
                screen.addPreference(this)

                SwitchPreferenceCompat(context).apply {
                    key = "discordRPCEnable"
                    title = getString(R.string.enable_discord_rpc)
                    summary = getString(R.string.enable_discord_rpc_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = "discordShowWhenPaused"
                    title = getString(R.string.discord_show_when_paused)
                    summary = getString(R.string.discord_show_when_paused_desc)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }

                MaterialListPreference(context).apply {
                    key = "discordActivityType"
                    title = getString(R.string.discord_activity_type)
                    entries = arrayOf(
                        getString(R.string.discord_type_playing),
                        getString(R.string.discord_type_streaming),
                        getString(R.string.discord_type_listening),
                        getString(R.string.discord_type_watching),
                        getString(R.string.discord_type_competing)
                    )
                    entryValues = arrayOf("PLAYING", "STREAMING", "LISTENING", "WATCHING", "COMPETING")
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    setDefaultValue("LISTENING")
                    addPreference(this)
                }

                MaterialListPreference(context).apply {
                    key = "discordPresenceStatus"
                    title = getString(R.string.discord_presence_status)
                    entries = arrayOf(
                        getString(R.string.discord_status_online),
                        getString(R.string.discord_status_idle),
                        getString(R.string.discord_status_dnd),
                        getString(R.string.discord_status_invisible)
                    )
                    entryValues = arrayOf("ONLINE", "IDLE", "DND", "INVISIBLE")
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    setDefaultValue("ONLINE")
                    addPreference(this)
                }

                MaterialListPreference(context).apply {
                    key = "discordLargeImageType"
                    title = getString(R.string.discord_large_image_type)
                    entries = arrayOf(
                        getString(R.string.discord_image_thumbnail),
                        getString(R.string.discord_image_artist),
                        getString(R.string.discord_image_appicon),
                        getString(R.string.discord_image_custom)
                    )
                    entryValues = arrayOf("thumbnail", "artist", "appicon", "custom")
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    setDefaultValue("thumbnail")
                    addPreference(this)
                }

                MaterialListPreference(context).apply {
                    key = "discordSmallImageType"
                    title = getString(R.string.discord_small_image_type)
                    entries = arrayOf(
                        getString(R.string.discord_image_thumbnail),
                        getString(R.string.discord_image_artist),
                        getString(R.string.discord_image_appicon),
                        getString(R.string.discord_image_custom)
                    )
                    entryValues = arrayOf("thumbnail", "artist", "appicon", "custom")
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    setDefaultValue("appicon")
                    addPreference(this)
                }

                // Activity Name
                MaterialTextInputPreference(context).apply {
                    key = "discordActivityName"
                    title = getString(R.string.discord_activity_name)
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    addPreference(this)
                }

                // Activity Details
                MaterialTextInputPreference(context).apply {
                    key = "discordActivityDetails"
                    title = getString(R.string.discord_activity_details)
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    addPreference(this)
                }

                // Activity State
                MaterialTextInputPreference(context).apply {
                    key = "discordActivityState"
                    title = getString(R.string.discord_activity_state)
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    addPreference(this)
                }

                // Button 1
                SwitchPreferenceCompat(context).apply {
                    key = "discordActivityButton1Enabled"
                    title = getString(R.string.discord_activity_button1_label)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                MaterialTextInputPreference(context).apply {
                    key = "discordActivityButton1Label"
                    title = getString(R.string.discord_activity_button1_label)
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    addPreference(this)
                }

                MaterialListPreference(context).apply {
                    key = "discordActivityButton1UrlSource"
                    title = getString(R.string.discord_activity_button1_url)
                    entries = arrayOf(
                        getString(R.string.discord_button_url_song),
                        getString(R.string.discord_button_url_artist),
                        getString(R.string.discord_button_url_album),
                        getString(R.string.discord_button_url_custom)
                    )
                    entryValues = arrayOf("song", "artist", "album", "custom")
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    setDefaultValue("song")
                    addPreference(this)
                }

                MaterialTextInputPreference(context).apply {
                    key = "discordActivityButton1CustomUrl"
                    title = getString(R.string.discord_activity_button1_url)
                    summary = getString(R.string.discord_button_url_custom)
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    addPreference(this)
                }

                // Button 2
                SwitchPreferenceCompat(context).apply {
                    key = "discordActivityButton2Enabled"
                    title = getString(R.string.discord_activity_button2_label)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                MaterialTextInputPreference(context).apply {
                    key = "discordActivityButton2Label"
                    title = getString(R.string.discord_activity_button2_label)
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    addPreference(this)
                }

                MaterialListPreference(context).apply {
                    key = "discordActivityButton2UrlSource"
                    title = getString(R.string.discord_activity_button2_url)
                    entries = arrayOf(
                        getString(R.string.discord_button_url_song),
                        getString(R.string.discord_button_url_artist),
                        getString(R.string.discord_button_url_album),
                        getString(R.string.discord_button_url_custom)
                    )
                    entryValues = arrayOf("song", "artist", "album", "custom")
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    setDefaultValue("artist")
                    addPreference(this)
                }

                MaterialTextInputPreference(context).apply {
                    key = "discordActivityButton2CustomUrl"
                    title = getString(R.string.discord_activity_button2_url)
                    summary = getString(R.string.discord_button_url_custom)
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    addPreference(this)
                }

                // Presence update interval
                MaterialSliderPreference(context, 5, 300, steps = 295).apply {
                    key = "discordPresenceIntervalValue"
                    title = getString(R.string.discord_presence_interval)
                    isIconSpaceReserved = false
                    setDefaultValue(30)
                    addPreference(this)
                }

                // Activity platform
                MaterialTextInputPreference(context).apply {
                    key = "discordActivityPlatform"
                    title = getString(R.string.discord_activity_platform)
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    addPreference(this)
                }
            }

            // ========== CONTENT & PRIVACY CATEGORY ==========
            PreferenceCategory(context).apply {
                title = getString(R.string.content_privacy)
                key = "content_privacy"
                isIconSpaceReserved = false
                layoutResource = R.layout.preference_category
                screen.addPreference(this)

                // Hide Explicit
                SwitchPreferenceCompat(context).apply {
                    key = "hideExplicit"
                    title = getString(R.string.hide_explicit)
                    summary = getString(R.string.hide_explicit_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                // Hide Video
                SwitchPreferenceCompat(context).apply {
                    key = "hideVideo"
                    title = getString(R.string.hide_video)
                    summary = getString(R.string.hide_video_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                // Pause Listen History
                SwitchPreferenceCompat(context).apply {
                    key = "pauseListenHistory"
                    title = getString(R.string.pause_listen_history)
                    summary = getString(R.string.pause_listen_history_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                // Pause Search History
                SwitchPreferenceCompat(context).apply {
                    key = "pauseSearchHistory"
                    title = getString(R.string.pause_search_history)
                    summary = getString(R.string.pause_search_history_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                // Pause Remote Listen History
                SwitchPreferenceCompat(context).apply {
                    key = "pauseRemoteListenHistory"
                    title = getString(R.string.pause_remote_listen_history)
                    summary = getString(R.string.pause_remote_listen_history_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                // Disable Screenshot
                SwitchPreferenceCompat(context).apply {
                    key = "disableScreenshot"
                    title = getString(R.string.disable_screenshot)
                    summary = getString(R.string.disable_screenshot_desc)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }
            }

            // ========== CONTENT LANGUAGE CATEGORY ==========
            PreferenceCategory(context).apply {
                title = getString(R.string.app_language)
                key = "content_language"
                isIconSpaceReserved = false
                layoutResource = R.layout.preference_category
                screen.addPreference(this)

                MaterialTextInputPreference(context).apply {
                    key = "contentLanguage"
                    title = getString(R.string.content_language)
                    summary = getString(R.string.content_language_summary)
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    setDefaultValue("SYSTEM_DEFAULT")
                    addPreference(this)
                }

                MaterialTextInputPreference(context).apply {
                    key = "contentCountry"
                    title = getString(R.string.content_country)
                    summary = getString(R.string.content_country_summary)
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    setDefaultValue("SYSTEM_DEFAULT")
                    addPreference(this)
                }
            }

            // ========== YTM SYNC ==========
            PreferenceCategory(context).apply {
                title = getString(R.string.ytm_sync)
                key = "ytm_sync"
                isIconSpaceReserved = false
                layoutResource = R.layout.preference_category
                screen.addPreference(this)

                SwitchPreferenceCompat(context).apply {
                    key = "ytmSync"
                    title = getString(R.string.ytm_sync)
                    summary = getString(R.string.ytm_sync_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }
            }

            // ========== MUSIC TOGETHER ==========
            PreferenceCategory(context).apply {
                title = getString(R.string.music_together)
                key = "together"
                isIconSpaceReserved = false
                layoutResource = R.layout.preference_category
                screen.addPreference(this)

                MaterialTextInputPreference(context).apply {
                    key = "together_display_name"
                    title = getString(R.string.together_display_name)
                    summary = getString(R.string.together_display_name_placeholder)
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    addPreference(this)
                }

                MaterialSliderPreference(context, 1024, 65535, allowOverride = true).apply {
                    key = "together_default_port"
                    title = getString(R.string.together_port)
                    isIconSpaceReserved = false
                    setDefaultValue(8080)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = "together_allow_guests_add_tracks"
                    title = getString(R.string.together_allow_guests_add)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = "together_allow_guests_control_playback"
                    title = getString(R.string.together_allow_guests_control)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = "together_require_host_approval_to_join"
                    title = getString(R.string.together_require_approval)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }
            }

            // ========== STORAGE & CACHE CATEGORY ==========
            PreferenceCategory(context).apply {
                title = getString(R.string.storage_settings)
                key = "storage"
                isIconSpaceReserved = false
                layoutResource = R.layout.preference_category
                screen.addPreference(this)

                // Max Image Cache
                MaterialSliderPreference(context, 0, 2048, allowOverride = true).apply {
                    key = "maxImageCacheSize"
                    title = getString(R.string.max_image_cache_size)
                    summary = getString(R.string.max_image_cache_size_summary)
                    isIconSpaceReserved = false
                    setDefaultValue(512)
                    addPreference(this)
                }

                // Max Song Cache
                MaterialSliderPreference(context, 0, 4096, allowOverride = true).apply {
                    key = "maxSongCacheSize"
                    title = getString(R.string.max_song_cache_size)
                    summary = getString(R.string.max_song_cache_size_summary)
                    isIconSpaceReserved = false
                    setDefaultValue(1024)
                    addPreference(this)
                }

                // Max Canvas Cache
                MaterialSliderPreference(context, 0, 1024, allowOverride = true).apply {
                    key = "maxCanvasCacheSize"
                    title = getString(R.string.max_canvas_cache_size)
                    summary = getString(R.string.max_canvas_cache_size_summary)
                    isIconSpaceReserved = false
                    setDefaultValue(256)
                    addPreference(this)
                }

                // Smart Trimmer
                SwitchPreferenceCompat(context).apply {
                    key = "smartTrimmer"
                    title = getString(R.string.smart_trimmer)
                    summary = getString(R.string.smart_trimmer_description)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                // External Downloader
                SwitchPreferenceCompat(context).apply {
                    key = "externalDownloaderEnabled"
                    title = getString(R.string.external_downloader)
                    summary = getString(R.string.external_downloader_desc)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                MaterialTextInputPreference(context).apply {
                    key = "externalDownloaderPackage"
                    title = getString(R.string.external_downloader_package)
                    summary = getString(R.string.external_downloader_package_desc)
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    addPreference(this)
                }
            }

            // ========== EQUALIZER CATEGORY ==========
            PreferenceCategory(context).apply {
                title = getString(R.string.equalizer)
                key = "equalizer_settings"
                isIconSpaceReserved = false
                layoutResource = R.layout.preference_category
                screen.addPreference(this)

                SwitchPreferenceCompat(context).apply {
                    key = "equalizerEnabled"
                    title = getString(R.string.eq_enabled)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = "equalizerOutputGainEnabled"
                    title = getString(R.string.eq_output_gain_enabled)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                MaterialSliderPreference(context, -12, 12, steps = 24).apply {
                    key = "equalizerOutputGainMb"
                    title = getString(R.string.eq_output_gain_mb)
                    isIconSpaceReserved = false
                    setDefaultValue(0)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = "equalizerBassBoostEnabled"
                    title = getString(R.string.eq_bass_boost_enabled)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                MaterialSliderPreference(context, 0, 1000, steps = 1000).apply {
                    key = "equalizerBassBoostStrength"
                    title = getString(R.string.eq_bass_boost_strength)
                    isIconSpaceReserved = false
                    setDefaultValue(0)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = "equalizerVirtualizerEnabled"
                    title = getString(R.string.eq_virtualizer_enabled)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                MaterialSliderPreference(context, 0, 1000, steps = 1000).apply {
                    key = "equalizerVirtualizerStrength"
                    title = getString(R.string.eq_virtualizer_strength)
                    isIconSpaceReserved = false
                    setDefaultValue(0)
                    addPreference(this)
                }

                // Equalizer Selected Profile
                MaterialTextInputPreference(context).apply {
                    key = "equalizerSelectedProfileId"
                    title = getString(R.string.equalizer_selected_profile)
                    summary = getString(R.string.equalizer_selected_profile_summary)
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    addPreference(this)
                }

                // Equalizer Custom Profiles JSON
                MaterialTextInputPreference(context).apply {
                    key = "equalizerCustomProfilesJson"
                    title = getString(R.string.equalizer_custom_profiles)
                    summary = getString(R.string.equalizer_custom_profiles_summary)
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    addPreference(this)
                }
            }

            // ========== DISCORD SOCIAL SDK CATEGORY ==========
            PreferenceCategory(context).apply {
                title = getString(R.string.discord_integration)
                key = "discord_sdk"
                isIconSpaceReserved = false
                layoutResource = R.layout.preference_category
                screen.addPreference(this)

                SwitchPreferenceCompat(context).apply {
                    key = "discordSocialSdkEnabled"
                    title = getString(R.string.discord_social_sdk_enabled)
                    summary = getString(R.string.discord_social_sdk_enabled_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }
            }

            // ========== SPOTIFY CATEGORY ==========
            PreferenceCategory(context).apply {
                title = getString(R.string.spotify)
                key = "spotify_settings"
                isIconSpaceReserved = false
                layoutResource = R.layout.preference_category
                screen.addPreference(this)

                SwitchPreferenceCompat(context).apply {
                    key = "show_spotify_playlists"
                    title = getString(R.string.show_spotify_playlists)
                    summary = getString(R.string.show_spotify_playlists_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }
            }

            // ========== JOSSRED CATEGORY ==========
            PreferenceCategory(context).apply {
                title = "JossRed"
                key = "jossred_settings"
                isIconSpaceReserved = false
                layoutResource = R.layout.preference_category
                screen.addPreference(this)

                SwitchPreferenceCompat(context).apply {
                    key = "JossRedMultimedia"
                    title = getString(R.string.jossred_multimedia)
                    summary = getString(R.string.jossred_multimedia_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }
            }

            // ========== WIDGET CATEGORY ==========
            PreferenceCategory(context).apply {
                title = getString(R.string.widget_settings)
                key = "widget_settings"
                isIconSpaceReserved = false
                layoutResource = R.layout.preference_category
                screen.addPreference(this)

                MaterialListPreference(context).apply {
                    key = "widget_background_mode"
                    title = getString(R.string.widget_background_mode)
                    summary = getString(R.string.widget_background_mode_summary)
                    entries = arrayOf(
                        getString(R.string.widget_background_solid),
                        getString(R.string.widget_background_blur),
                        getString(R.string.widget_background_dominant_color)
                    )
                    entryValues = arrayOf("solid", "blur", "dominant_color")
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    setDefaultValue("blur")
                    addPreference(this)
                }

                MaterialSliderPreference(context, 0, 100, steps = 100).apply {
                    key = "widget_scrim_opacity"
                    title = getString(R.string.widget_scrim_opacity)
                    summary = getString(R.string.widget_scrim_opacity_summary)
                    isIconSpaceReserved = false
                    setDefaultValue(50)
                    addPreference(this)
                }

                MaterialSliderPreference(context, 0, 48, steps = 48).apply {
                    key = "widget_corner_radius"
                    title = getString(R.string.widget_corner_radius)
                    summary = getString(R.string.widget_corner_radius_summary)
                    isIconSpaceReserved = false
                    setDefaultValue(16)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = "widget_show_progress_bar"
                    title = getString(R.string.widget_show_progress_bar)
                    summary = getString(R.string.widget_show_progress_bar_desc)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }
            }

            // ========== DOWNLOAD SETTINGS CATEGORY ==========
            PreferenceCategory(context).apply {
                title = getString(R.string.download)
                key = "download_settings"
                isIconSpaceReserved = false
                layoutResource = R.layout.preference_category
                screen.addPreference(this)

                MaterialTextInputPreference(context).apply {
                    key = "downloadFilename"
                    title = getString(R.string.download_filename)
                    summary = getString(R.string.download_filename_summary)
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    setDefaultValue("%title% - %artist%")
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = "createAlbumFolder"
                    title = getString(R.string.create_album_folder)
                    summary = getString(R.string.create_album_folder_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = "createYoutubeFolder"
                    title = getString(R.string.create_youtube_folder)
                    summary = getString(R.string.create_youtube_folder_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = "downloadLyrics"
                    title = getString(R.string.download_lyrics)
                    summary = getString(R.string.download_lyrics_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = "useDown"
                    title = getString(R.string.use_down)
                    summary = getString(R.string.use_down_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = "liveSearch"
                    title = getString(R.string.live_search)
                    summary = getString(R.string.live_search_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }

                MaterialListPreference(context).apply {
                    key = "includeOrExclude"
                    title = getString(R.string.include_or_exclude)
                    summary = getString(R.string.include_or_exclude_summary)
                    entries = arrayOf("Include", "Exclude")
                    entryValues = arrayOf("include", "exclude")
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    setDefaultValue("exclude")
                    addPreference(this)
                }

                MaterialTextInputPreference(context).apply {
                    key = "includedExcludedPaths"
                    title = getString(R.string.included_excluded_paths)
                    summary = getString(R.string.included_excluded_paths_summary)
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    addPreference(this)
                }

                MaterialSliderPreference(context, 0, 300, steps = 300).apply {
                    key = "minAudioLength"
                    title = getString(R.string.min_audio_length)
                    summary = getString(R.string.min_audio_length_summary)
                    isIconSpaceReserved = false
                    setDefaultValue(30)
                    addPreference(this)
                }
            }

            // ========== AUTO BACKUP CATEGORY ==========
            PreferenceCategory(context).apply {
                title = getString(R.string.auto_backup_setting)
                key = "auto_backup"
                isIconSpaceReserved = false
                layoutResource = R.layout.preference_category
                screen.addPreference(this)

                SwitchPreferenceCompat(context).apply {
                    key = "autoBackupEnabled"
                    title = getString(R.string.auto_backup_enabled)
                    summary = getString(R.string.auto_backup_enabled_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                MaterialSliderPreference(context, 1, 30, steps = 29).apply {
                    key = "autoBackupFrequency"
                    title = getString(R.string.auto_backup_frequency)
                    summary = getString(R.string.auto_backup_frequency_summary)
                    isIconSpaceReserved = false
                    setDefaultValue(7)
                    addPreference(this)
                }

                MaterialSliderPreference(context, 1, 20, steps = 19).apply {
                    key = "autoBackupMaxFiles"
                    title = getString(R.string.auto_backup_max_files)
                    summary = getString(R.string.auto_backup_max_files_summary)
                    isIconSpaceReserved = false
                    setDefaultValue(5)
                    addPreference(this)
                }

                MaterialTextInputPreference(context).apply {
                    key = "autoBackupLocation"
                    title = getString(R.string.auto_backup_location)
                    summary = getString(R.string.auto_backup_location_summary)
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    addPreference(this)
                }
            }

            // ========== MUSIC & CONTENT CATEGORY ==========
            PreferenceCategory(context).apply {
                title = getString(R.string.music_language)
                key = "music_content"
                isIconSpaceReserved = false
                layoutResource = R.layout.preference_category
                screen.addPreference(this)

                MaterialTextInputPreference(context).apply {
                    key = "musicLanguage"
                    title = getString(R.string.music_language)
                    summary = getString(R.string.music_language_summary)
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    setDefaultValue("en")
                    addPreference(this)
                }

                MaterialTextInputPreference(context).apply {
                    key = "chartLocation"
                    title = getString(R.string.chart_location)
                    summary = getString(R.string.chart_location_summary)
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    setDefaultValue("US")
                    addPreference(this)
                }
            }

            // ========== UPDATES CATEGORY ==========
            PreferenceCategory(context).apply {
                title = getString(R.string.check_for_updates)
                key = "updates"
                isIconSpaceReserved = false
                layoutResource = R.layout.preference_category
                screen.addPreference(this)

                SwitchPreferenceCompat(context).apply {
                    key = "check_for_updates"
                    title = getString(R.string.check_for_updates)
                    summary = getString(R.string.check_for_updates_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = "enableUpdateNotification"
                    title = getString(R.string.enable_update_notification)
                    summary = getString(R.string.enable_update_notification_desc)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = "autoCheckForUpdate"
                    title = getString(R.string.auto_check_for_update)
                    summary = getString(R.string.auto_check_for_update_desc)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = "blogNotificationEnabled"
                    title = getString(R.string.blog_notification_enabled)
                    summary = getString(R.string.blog_notification_enabled_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }
            }
        }
    }
}
