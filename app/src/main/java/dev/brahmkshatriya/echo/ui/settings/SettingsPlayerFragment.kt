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
import dev.brahmkshatriya.echo.playback.listener.PlayerRadio.Companion.ENDLESS_QUEUE
import dev.brahmkshatriya.echo.playback.listener.PlayerRadio.Companion.RADIO_THRESHOLD
import dev.brahmkshatriya.echo.ui.common.FragmentUtils.openFragment
import dev.brahmkshatriya.echo.ui.player.PlayerViewModel.Companion.KEEP_QUEUE
import dev.brahmkshatriya.echo.ui.settings.AudioEffectsFragment.Companion.AUDIO_FX
import dev.brahmkshatriya.echo.utils.ContextUtils.SETTINGS_NAME
import dev.brahmkshatriya.echo.utils.ui.prefs.MaterialListPreference
import dev.brahmkshatriya.echo.utils.ui.prefs.MaterialSliderPreference
import dev.brahmkshatriya.echo.utils.ui.prefs.MaterialTextInputPreference
import dev.brahmkshatriya.echo.utils.ui.prefs.TransitionPreference

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

            // ========== PLAYBACK CATEGORY (existing + streaming quality kept) ==========
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

                // Stream client selection
                MaterialListPreference(context).apply {
                    key = "playerStreamClient"
                    title = getString(R.string.player_stream_client)
                    summary = getString(R.string.player_stream_client_desc)
                    entries = listOf(
                        getString(R.string.player_stream_client_android_vr),
                        getString(R.string.player_stream_client_web_remix),
                        getString(R.string.player_stream_client_ios),
                        getString(R.string.player_stream_client_tvhtml5),
                        getString(R.string.player_stream_client_android_music)
                    ).toTypedArray()
                    entryValues = listOf(
                        "ANDROID_VR", "WEB_REMIX", "IOS", "TVHTML5", "ANDROID_MUSIC"
                    ).toTypedArray()
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    setDefaultValue("ANDROID_VR")
                    addPreference(this)
                }

                // Audio normalization
                SwitchPreferenceCompat(context).apply {
                    key = "audioNormalization"
                    title = getString(R.string.audio_normalization)
                    summary = getString(R.string.audio_normalization_desc)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                // Audio offload
                SwitchPreferenceCompat(context).apply {
                    key = "audioOffload"
                    title = getString(R.string.audio_offload)
                    summary = getString(R.string.audio_offload_desc)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }
            }

            // ========== CROSSFADE CATEGORY ==========
            PreferenceCategory(context).apply {
                title = getString(R.string.audio_crossfade)
                key = "crossfade"
                isIconSpaceReserved = false
                layoutResource = R.layout.preference_category
                screen.addPreference(this)

                SwitchPreferenceCompat(context).apply {
                    key = "audio_crossfade_gapless"
                    title = getString(R.string.audio_crossfade_gapless)
                    summary = getString(R.string.audio_crossfade_gapless_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                MaterialSliderPreference(context, 0, 12, steps = 12).apply {
                    key = "audioCrossfadeDuration"
                    title = getString(R.string.audio_crossfade_duration)
                    summary = getString(R.string.audio_crossfade_duration_summary)
                    isIconSpaceReserved = false
                    setDefaultValue(0)
                    addPreference(this)
                }
            }

            // ========== BEHAVIOR CATEGORY (existing + new) ==========
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

                // Persistent Queue
                SwitchPreferenceCompat(context).apply {
                    key = "persistentQueue"
                    title = getString(R.string.persistent_queue)
                    summary = getString(R.string.persistent_queue_desc)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }

                // Permanent Shuffle
                SwitchPreferenceCompat(context).apply {
                    key = "permanentShuffle"
                    title = getString(R.string.permanent_shuffle)
                    summary = getString(R.string.permanent_shuffle_desc)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
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

                // ── Endless Queue (from SimpMusic queue algorithm) ──
                // When enabled, the queue automatically appends radio tracks
                // when it nears its end, providing infinite playback.
                SwitchPreferenceCompat(context).apply {
                    key = ENDLESS_QUEUE
                    title = getString(R.string.endless_queue)
                    summary = getString(R.string.endless_queue_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }

                // ── Radio Load Threshold (from SimpMusic queue algorithm) ──
                // Number of tracks remaining before radio starts loading more.
                // Higher = earlier loading (less gap), lower = less data usage.
                MaterialSliderPreference(context).apply {
                    key = RADIO_THRESHOLD
                    title = getString(R.string.radio_threshold)
                    summary = getString(R.string.radio_threshold_summary)
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    setDefaultValue(3)
                    min = 1f
                    max = 10f
                    step = 1f
                    addPreference(this)
                }

                // Auto Load More
                SwitchPreferenceCompat(context).apply {
                    key = "autoLoadMore"
                    title = getString(R.string.auto_load_more)
                    summary = getString(R.string.auto_load_more_desc)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }

                // Disable Load More when Repeat All
                SwitchPreferenceCompat(context).apply {
                    key = "disableLoadMoreWhenRepeatAll"
                    title = getString(R.string.disable_load_more_when_repeat_all)
                    summary = getString(R.string.disable_load_more_when_repeat_all_desc)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                // Auto Download on Like
                SwitchPreferenceCompat(context).apply {
                    key = "autoDownloadOnLike"
                    title = getString(R.string.auto_download_on_like)
                    summary = getString(R.string.auto_download_on_like_desc)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                // Auto Skip on Error
                SwitchPreferenceCompat(context).apply {
                    key = "autoSkipNextOnError"
                    title = getString(R.string.auto_skip_next_on_error)
                    summary = getString(R.string.auto_skip_next_on_error_desc)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }

                // Pause on Device Mute
                SwitchPreferenceCompat(context).apply {
                    key = "pauseOnDeviceMute"
                    title = getString(R.string.pause_on_device_mute)
                    summary = getString(R.string.pause_on_device_mute_desc)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                // Auto Start on Bluetooth
                SwitchPreferenceCompat(context).apply {
                    key = "autoStartOnBluetooth"
                    title = getString(R.string.auto_start_on_bluetooth)
                    summary = getString(R.string.auto_start_on_bluetooth_desc)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                // Stop on Task Clear
                SwitchPreferenceCompat(context).apply {
                    key = "stopMusicOnTaskClear"
                    title = getString(R.string.stop_music_on_task_clear)
                    summary = getString(R.string.stop_music_on_task_clear_desc)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                // Wakelock
                SwitchPreferenceCompat(context).apply {
                    key = "wakelock"
                    title = getString(R.string.wakelock)
                    summary = getString(R.string.wakelock_desc)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                // Play Explicit Content
                SwitchPreferenceCompat(context).apply {
                    key = "playExplicitContent"
                    title = getString(R.string.play_explicit_content)
                    summary = getString(R.string.play_explicit_content_description)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }

                // Play Video Instead of Audio
                SwitchPreferenceCompat(context).apply {
                    key = "playVideoInsteadOfAudio"
                    title = getString(R.string.play_video_instead_of_audio)
                    summary = getString(R.string.play_video_instead_of_audio_description)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                // Auto Play Similar Content
                SwitchPreferenceCompat(context).apply {
                    key = "enableSimilarContent"
                    title = getString(R.string.enable_similar_content)
                    summary = getString(R.string.enable_similar_content_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                // SponsorBlock
                SwitchPreferenceCompat(context).apply {
                    key = "enableSponsorBlock"
                    title = getString(R.string.enable_sponsor_block)
                    summary = getString(R.string.enable_sponsor_block_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                // Display Codec on Player
                SwitchPreferenceCompat(context).apply {
                    key = "displayCodecOnPlayer"
                    title = getString(R.string.display_codec_on_player)
                    summary = getString(R.string.description_display_codec_on_player)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                // Show Nerd Stats
                SwitchPreferenceCompat(context).apply {
                    key = "showNerdStats"
                    title = getString(R.string.show_nerd_stats)
                    summary = getString(R.string.description_show_nerd_stats)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                // Show Small Image
                SwitchPreferenceCompat(context).apply {
                    key = "showSmallImage"
                    title = getString(R.string.show_small_image)
                    summary = getString(R.string.description_show_small_image)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                // Show Canvas Debug
                SwitchPreferenceCompat(context).apply {
                    key = "showCanvasDebug"
                    title = getString(R.string.show_canvas_debug)
                    summary = getString(R.string.description_show_canvas_debug)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                // Queue Edit Lock
                SwitchPreferenceCompat(context).apply {
                    key = "queueEditLock"
                    title = getString(R.string.queue_edit_lock)
                    summary = getString(R.string.queue_edit_lock_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                // Playlist Edit Lock
                SwitchPreferenceCompat(context).apply {
                    key = "playlistEditLock"
                    title = getString(R.string.playlist_edit_lock)
                    summary = getString(R.string.playlist_edit_lock_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                // Speed Dial Song IDs
                MaterialTextInputPreference(context).apply {
                    key = "speedDialSongIds"
                    title = getString(R.string.speed_dial)
                    summary = getString(R.string.speed_dial_random)
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    addPreference(this)
                }

                // Artist Separators
                MaterialTextInputPreference(context).apply {
                    key = "artistSeparators"
                    title = getString(R.string.artist_separators)
                    summary = getString(R.string.artist_separators_desc)
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    setDefaultValue(",")
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
            }

            // ========== SWIPE CATEGORY ==========
            PreferenceCategory(context).apply {
                title = getString(R.string.swipe_gestures)
                key = "swipe_gestures"
                isIconSpaceReserved = false
                layoutResource = R.layout.preference_category
                screen.addPreference(this)

                SwitchPreferenceCompat(context).apply {
                    key = "swipeThumbnail"
                    title = getString(R.string.enable_swipe_thumbnail)
                    summary = getString(R.string.enable_swipe_thumbnail_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }

                MaterialSliderPreference(context, 1, 10, steps = 9).apply {
                    key = "swipeSensitivity"
                    title = getString(R.string.swipe_sensitivity)
                    summary = getString(R.string.swipe_sensitivity_desc)
                    isIconSpaceReserved = false
                    setDefaultValue(5)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = "SwipeToSong"
                    title = getString(R.string.swipe_to_song)
                    summary = getString(R.string.swipe_to_song_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }
            }

            // ========== SERVICE LIFECYCLE CATEGORY ==========
            PreferenceCategory(context).apply {
                title = getString(R.string.player)
                key = "service_lifecycle"
                isIconSpaceReserved = false
                layoutResource = R.layout.preference_category
                screen.addPreference(this)

                SwitchPreferenceCompat(context).apply {
                    key = "saveLastPlayed"
                    title = getString(R.string.save_last_played)
                    summary = getString(R.string.save_last_played_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = "killServiceOnExit"
                    title = getString(R.string.kill_service_on_exit)
                    summary = getString(R.string.kill_service_on_exit_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = "keepServiceAlive"
                    title = getString(R.string.keep_service_alive)
                    summary = getString(R.string.keep_service_alive_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = "savePlaybackState"
                    title = getString(R.string.save_playback_state)
                    summary = getString(R.string.save_playback_state_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = "loadLast"
                    title = getString(R.string.load_last)
                    summary = getString(R.string.load_last_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = "resetOnSkip"
                    title = getString(R.string.reset_on_skip)
                    summary = getString(R.string.reset_on_skip_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = "enforceRepeat"
                    title = getString(R.string.enforce_repeat)
                    summary = getString(R.string.enforce_repeat_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }
            }

            // ========== RADIO & AUTOPLAY CATEGORY ==========
            PreferenceCategory(context).apply {
                title = getString(R.string.auto_start_radio)
                key = "radio_autoplay"
                isIconSpaceReserved = false
                layoutResource = R.layout.preference_category
                screen.addPreference(this)

                SwitchPreferenceCompat(context).apply {
                    key = "autoplay"
                    title = getString(R.string.autoplay)
                    summary = getString(R.string.autoplay_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = "radioAudioOnly"
                    title = getString(R.string.radio_audio_only)
                    summary = getString(R.string.radio_audio_only_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = "combineLikedSongs"
                    title = getString(R.string.combine_liked_songs)
                    summary = getString(R.string.combine_liked_songs_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = "autoDownloadLikedSongs"
                    title = getString(R.string.auto_download_liked_songs)
                    summary = getString(R.string.auto_download_liked_songs_description)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }
            }

            // ========== ADVANCED PLAYBACK CATEGORY ==========
            PreferenceCategory(context).apply {
                title = getString(R.string.playback)
                key = "advanced_playback"
                isIconSpaceReserved = false
                layoutResource = R.layout.preference_category
                screen.addPreference(this)

                // Dark Mode
                SwitchPreferenceCompat(context).apply {
                    key = "darkMode"
                    title = getString(R.string.dark_mode_title)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                // Use Login for Browse
                SwitchPreferenceCompat(context).apply {
                    key = "useLoginForBrowse"
                    title = getString(R.string.use_login_for_browse)
                    summary = getString(R.string.use_login_for_browse_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }

                // Playback Speed
                MaterialSliderPreference(context, 25, 300, steps = 275).apply {
                    key = "playbackSpeed"
                    title = getString(R.string.playback_speed)
                    summary = getString(R.string.playback_speed_summary)
                    isIconSpaceReserved = false
                    setDefaultValue(100)
                    addPreference(this)
                }

                // Fade Out Duration
                MaterialSliderPreference(context, 0, 10, steps = 10).apply {
                    key = "fadeOutDuration"
                    title = getString(R.string.fade_out_duration)
                    summary = getString(R.string.fade_out_duration_summary)
                    isIconSpaceReserved = false
                    setDefaultValue(0)
                    addPreference(this)
                }

                // Min Duration
                MaterialSliderPreference(context, 0, 60, steps = 60).apply {
                    key = "minDuration"
                    title = getString(R.string.min_duration)
                    summary = getString(R.string.min_duration_summary)
                    isIconSpaceReserved = false
                    setDefaultValue(0)
                    addPreference(this)
                }

                // Sleep Timer Duration
                MaterialSliderPreference(context, 1, 120, steps = 119).apply {
                    key = "sleepTimerDuration"
                    title = getString(R.string.sleep_timer_duration)
                    summary = getString(R.string.sleep_timer_duration_summary)
                    isIconSpaceReserved = false
                    setDefaultValue(15)
                    addPreference(this)
                }

                // A-B Repeat
                SwitchPreferenceCompat(context).apply {
                    key = "abRepeat"
                    title = getString(R.string.ab_repeat)
                    summary = getString(R.string.ab_repeat_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                // Cache Song
                SwitchPreferenceCompat(context).apply {
                    key = "cacheSong"
                    title = getString(R.string.cache_song)
                    summary = getString(R.string.cache_song_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }

                // Player Cache Limit
                MaterialSliderPreference(context, 0, 4096, allowOverride = true).apply {
                    key = "playerCacheLimit"
                    title = getString(R.string.player_cache_limit)
                    summary = getString(R.string.player_cache_limit_summary)
                    isIconSpaceReserved = false
                    setDefaultValue(1024)
                    addPreference(this)
                }

                // Support EQ
                SwitchPreferenceCompat(context).apply {
                    key = "supportEq"
                    title = getString(R.string.support_eq)
                    summary = getString(R.string.support_eq_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }

                // Get Lyrics Online
                SwitchPreferenceCompat(context).apply {
                    key = "getLyricsOnline"
                    title = getString(R.string.get_lyrics_online)
                    summary = getString(R.string.get_lyrics_online_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }
            }

            // ========== GESTURES CATEGORY ==========
            PreferenceCategory(context).apply {
                title = getString(R.string.enable_gesture)
                key = "gestures"
                isIconSpaceReserved = false
                layoutResource = R.layout.preference_category
                screen.addPreference(this)

                SwitchPreferenceCompat(context).apply {
                    key = "enableGesture"
                    title = getString(R.string.enable_gesture)
                    summary = getString(R.string.enable_gesture_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = "volumeGestureEnabled"
                    title = getString(R.string.volume_gesture_enabled)
                    summary = getString(R.string.volume_gesture_enabled_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = "useLessDataImage"
                    title = getString(R.string.use_less_data_image)
                    summary = getString(R.string.use_less_data_image_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }
            }

            // ========== CROSSFADE ADDITIONAL ==========
            // Add to crossfade category
            PreferenceCategory(context).apply {
                title = getString(R.string.audio_crossfade)
                key = "crossfade_advanced"
                isIconSpaceReserved = false
                layoutResource = R.layout.preference_category
                screen.addPreference(this)

                SwitchPreferenceCompat(context).apply {
                    key = "crossfadeDjMode"
                    title = getString(R.string.crossfade_dj_mode)
                    summary = getString(R.string.crossfade_dj_mode_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = "crossfadeSkipAlbum"
                    title = getString(R.string.crossfade_skip_album)
                    summary = getString(R.string.crossfade_skip_album_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }
            }

            // ========== DATA & PRIVACY CATEGORY ==========
            PreferenceCategory(context).apply {
                title = getString(R.string.content_privacy)
                key = "data_privacy"
                isIconSpaceReserved = false
                layoutResource = R.layout.preference_category
                screen.addPreference(this)

                SwitchPreferenceCompat(context).apply {
                    key = "sendListeningDataToGoogle"
                    title = getString(R.string.send_listening_data_to_google)
                    summary = getString(R.string.send_listening_data_to_google_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = "syncFollowToYouTube"
                    title = getString(R.string.sync_follow_to_youtube)
                    summary = getString(R.string.sync_follow_to_youtube_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = "keepYouTubePlaylistOffline"
                    title = getString(R.string.keep_youtube_playlist_offline)
                    summary = getString(R.string.keep_youtube_playlist_offline_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = "localTrackingEnabled"
                    title = getString(R.string.local_tracking_enabled)
                    summary = getString(R.string.local_tracking_enabled_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }
            }

            // ========== NETWORK CATEGORY ==========
            PreferenceCategory(context).apply {
                title = getString(R.string.network)
                key = "network"
                isIconSpaceReserved = false
                layoutResource = R.layout.preference_category
                screen.addPreference(this)

                SwitchPreferenceCompat(context).apply {
                    key = "networkMetered"
                    title = getString(R.string.network_metered)
                    summary = getString(R.string.network_metered_desc)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                // Web Client PO Token
                SwitchPreferenceCompat(context).apply {
                    key = "webClientPoTokenEnabled"
                    title = getString(R.string.web_client_po_token)
                    summary = getString(R.string.web_client_po_token_desc)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                // Use Visitor Data
                SwitchPreferenceCompat(context).apply {
                    key = "useVisitorData"
                    title = getString(R.string.use_visitor_data)
                    summary = getString(R.string.use_visitor_data_desc)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
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
