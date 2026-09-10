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

class SettingsLyricsFragment : BaseSettingsFragment() {
    override val title get() = getString(R.string.lyrics_settings)
    override val icon get() = R.drawable.ic_article.toResourceImageHolder()
    override val creator = { LyricsPreference() }

    class LyricsPreference : PreferenceFragmentCompat() {

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

            // ========== LYRICS DISPLAY CATEGORY ==========
            PreferenceCategory(context).apply {
                title = getString(R.string.lyrics)
                key = "lyrics_display"
                isIconSpaceReserved = false
                layoutResource = R.layout.preference_category
                screen.addPreference(this)

                // Show Lyrics
                SwitchPreferenceCompat(context).apply {
                    key = "showLyrics"
                    title = getString(R.string.show_lyrics)
                    summary = getString(R.string.show_lyrics_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }

                // Use Lyrics V2
                SwitchPreferenceCompat(context).apply {
                    key = "useLyricsV2"
                    title = getString(R.string.use_lyrics_v2)
                    summary = getString(R.string.use_lyrics_v2_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }

                // Lyrics Animation Style
                MaterialListPreference(context).apply {
                    key = "lyricsAnimationStyle"
                    title = getString(R.string.lyrics_animation_style)
                    summary = getString(R.string.lyrics_animation_style_summary)
                    entries = arrayOf(
                        getString(R.string.lyrics_animation_none),
                        getString(R.string.lyrics_animation_fade),
                        getString(R.string.lyrics_animation_glow),
                        getString(R.string.lyrics_animation_slide),
                        getString(R.string.lyrics_animation_karaoke),
                        getString(R.string.lyrics_animation_apple)
                    )
                    entryValues = arrayOf("NONE", "FADE", "GLOW", "SLIDE", "KARAOKE", "APPLE")
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    setDefaultValue("FADE")
                    addPreference(this)
                }

                // Lyrics Text Size
                MaterialSliderPreference(context, 10, 30, steps = 20).apply {
                    key = "lyricsTextSize"
                    title = getString(R.string.lyrics_text_size)
                    summary = getString(R.string.lyrics_text_size_summary)
                    isIconSpaceReserved = false
                    setDefaultValue(16)
                    addPreference(this)
                }

                // Lyrics Line Spacing
                MaterialSliderPreference(context, 0, 20, steps = 20).apply {
                    key = "lyricsLineSpacing"
                    title = getString(R.string.lyrics_line_spacing)
                    summary = getString(R.string.lyrics_line_spacing_summary)
                    isIconSpaceReserved = false
                    setDefaultValue(4)
                    addPreference(this)
                }

                // Lyrics Text Position
                MaterialListPreference(context).apply {
                    key = "lyricsTextPosition"
                    title = getString(R.string.lyrics_text_position)
                    summary = getString(R.string.lyrics_text_position_summary)
                    entries = arrayOf(
                        getString(R.string.lyrics_position_left),
                        getString(R.string.lyrics_position_center),
                        getString(R.string.lyrics_position_right)
                    )
                    entryValues = arrayOf("LEFT", "CENTER", "RIGHT")
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    setDefaultValue("CENTER")
                    addPreference(this)
                }

                // Click to Change Line
                SwitchPreferenceCompat(context).apply {
                    key = "lyricsClick"
                    title = getString(R.string.lyrics_click_change)
                    summary = getString(R.string.lyrics_click_change_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }

                // Auto Scroll
                SwitchPreferenceCompat(context).apply {
                    key = "lyricsScrollKey"
                    title = getString(R.string.lyrics_auto_scroll)
                    summary = getString(R.string.lyrics_auto_scroll_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }
            }

            // ========== LYRICS PROVIDERS CATEGORY ==========
            PreferenceCategory(context).apply {
                title = getString(R.string.lyrics_providers)
                key = "lyrics_providers"
                isIconSpaceReserved = false
                layoutResource = R.layout.preference_category
                screen.addPreference(this)

                // Primary Lyrics Provider
                MaterialListPreference(context).apply {
                    key = "lyricsProvider"
                    title = getString(R.string.main_lyrics_provider)
                    summary = getString(R.string.main_lyrics_provider_summary)
                    entries = arrayOf(
                        getString(R.string.lyrics_provider_lrclib),
                        getString(R.string.lyrics_provider_kugou),
                        getString(R.string.lyrics_provider_better_lyrics),
                        getString(R.string.lyrics_provider_simpmusic)
                    )
                    entryValues = arrayOf("LRCLIB", "KUGOU", "BETTER_LYRICS", "SIMPMUSIC")
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    setDefaultValue("LRCLIB")
                    addPreference(this)
                }

                // Enable LrcLib
                SwitchPreferenceCompat(context).apply {
                    key = "enableLrclib"
                    title = getString(R.string.enable_lrclib)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }

                // Enable KuGou
                SwitchPreferenceCompat(context).apply {
                    key = "enableKugou"
                    title = getString(R.string.enable_kugou)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }

                // Enable Better Lyrics
                SwitchPreferenceCompat(context).apply {
                    key = "enableBetterLyrics"
                    title = getString(R.string.enable_betterlyrics)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }

                // Enable SimpMusic
                SwitchPreferenceCompat(context).apply {
                    key = "enableSimpMusicLyrics"
                    title = getString(R.string.enable_simpmusic_lyrics)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }

                // Enable Spotify Lyrics
                SwitchPreferenceCompat(context).apply {
                    key = "enableSpotifyLyrics"
                    title = getString(R.string.enable_spotify_lyrics)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }
            }

            // ========== ROMANIZATION & TRANSLATION CATEGORY ==========
            PreferenceCategory(context).apply {
                title = getString(R.string.lyrics_romanization)
                key = "lyrics_romanization"
                isIconSpaceReserved = false
                layoutResource = R.layout.preference_category
                screen.addPreference(this)

                // Romanize Japanese
                SwitchPreferenceCompat(context).apply {
                    key = "lyricsRomanizeJapanese"
                    title = getString(R.string.lyrics_romanize_japanese)
                    summary = getString(R.string.lyrics_romanize_japanese_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                // Romanize Korean
                SwitchPreferenceCompat(context).apply {
                    key = "lyricsRomanizeKorean"
                    title = getString(R.string.lyrics_romanize_korean)
                    summary = getString(R.string.lyrics_romanize_korean_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                // Translate Lyrics
                SwitchPreferenceCompat(context).apply {
                    key = "translateLyrics"
                    title = getString(R.string.translate_lyrics)
                    summary = getString(R.string.translate_lyrics_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                // Enable Translator
                SwitchPreferenceCompat(context).apply {
                    key = "enableTranslator"
                    title = getString(R.string.enable_translator)
                    summary = getString(R.string.enable_translator_desc)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                // Translator Target Language
                MaterialTextInputPreference(context).apply {
                    key = "translatorTargetLang"
                    title = getString(R.string.translator_target_lang)
                    summary = getString(R.string.translator_target_lang_summary)
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    setDefaultValue("en")
                    addPreference(this)
                }
            }

            // ========== LYRICS ADVANCED CATEGORY ==========
            PreferenceCategory(context).apply {
                title = getString(R.string.lyrics)
                key = "lyrics_advanced"
                isIconSpaceReserved = false
                layoutResource = R.layout.preference_category
                screen.addPreference(this)

                // Lyrics Romanization
                SwitchPreferenceCompat(context).apply {
                    key = "lyricsRomanization"
                    title = getString(R.string.lyrics_romanization_key)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                // Main Lyrics Provider
                MaterialListPreference(context).apply {
                    key = "mainLyricsProvider"
                    title = getString(R.string.main_lyrics_provider_key)
                    entries = arrayOf(
                        getString(R.string.lyrics_provider_lrclib),
                        getString(R.string.lyrics_provider_kugou),
                        getString(R.string.lyrics_provider_better_lyrics),
                        getString(R.string.lyrics_provider_simpmusic)
                    )
                    entryValues = arrayOf("LRCLIB", "KUGOU", "BETTER_LYRICS", "SIMPMUSIC")
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    setDefaultValue("LRCLIB")
                    addPreference(this)
                }

                // Translation Language
                MaterialTextInputPreference(context).apply {
                    key = "translationLanguage"
                    title = getString(R.string.translation_language)
                    summary = getString(R.string.translation_language_summary)
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    setDefaultValue("en")
                    addPreference(this)
                }

                // YouTube Subtitle Language
                MaterialTextInputPreference(context).apply {
                    key = "youtubeSubtitleLanguage"
                    title = getString(R.string.youtube_subtitle_language)
                    summary = getString(R.string.youtube_subtitle_language_summary)
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    setDefaultValue("en")
                    addPreference(this)
                }

                // Help Build Lyrics Database
                SwitchPreferenceCompat(context).apply {
                    key = "helpBuildLyricsDatabase"
                    title = getString(R.string.help_build_lyrics_database)
                    summary = getString(R.string.help_build_lyrics_database_description)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                // Lyrics Line Blur
                SwitchPreferenceCompat(context).apply {
                    key = "lyricsLineBlur"
                    title = getString(R.string.lyrics_line_blur)
                    summary = getString(R.string.lyrics_line_blur_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                // Lyrics Sync Offset
                MaterialSliderPreference(context, -500, 500, steps = 1000).apply {
                    key = "lyrics_sync_offset"
                    title = getString(R.string.lyrics_sync_offset)
                    summary = getString(R.string.lyrics_sync_offset_summary)
                    isIconSpaceReserved = false
                    setDefaultValue(0)
                    addPreference(this)
                }

                // Lyrics Sync Offset Ms (alternative key)
                MaterialSliderPreference(context, -500, 500, steps = 1000).apply {
                    key = "lyricsSyncOffsetMs"
                    title = getString(R.string.lyrics_sync_offset)
                    summary = getString(R.string.lyrics_sync_offset_summary)
                    isIconSpaceReserved = false
                    setDefaultValue(0)
                    addPreference(this)
                }
            }

            // ========== PRELOAD CATEGORY ==========
            PreferenceCategory(context).apply {
                title = getString(R.string.preload_queue_lyrics)
                key = "lyrics_preload"
                isIconSpaceReserved = false
                layoutResource = R.layout.preference_category
                screen.addPreference(this)

                // Preload Queue Lyrics
                SwitchPreferenceCompat(context).apply {
                    key = "preload_queue_lyrics_enabled"
                    title = getString(R.string.preload_queue_lyrics)
                    summary = getString(R.string.preload_queue_lyrics_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                // Queue Lyrics Preload Count
                MaterialSliderPreference(context, 1, 10, steps = 9).apply {
                    key = "queue_lyrics_preload_count"
                    title = getString(R.string.queue_lyrics_preload_count)
                    summary = getString(R.string.queue_lyrics_preload_count_summary)
                    isIconSpaceReserved = false
                    setDefaultValue(3)
                    addPreference(this)
                }
            }
        }
    }
}
