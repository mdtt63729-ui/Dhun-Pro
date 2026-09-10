package dev.brahmkshatriya.echo.ui.settings

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import dev.brahmkshatriya.echo.MainActivity
import dev.brahmkshatriya.echo.MainActivity.Companion.AMOLED_KEY
import dev.brahmkshatriya.echo.MainActivity.Companion.BACK_ANIM
import dev.brahmkshatriya.echo.MainActivity.Companion.BIG_COVER
import dev.brahmkshatriya.echo.MainActivity.Companion.COLOR_KEY
import dev.brahmkshatriya.echo.MainActivity.Companion.CUSTOM_THEME_KEY
import dev.brahmkshatriya.echo.MainActivity.Companion.THEME_KEY
import dev.brahmkshatriya.echo.MainActivity.Companion.defaultColor
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toResourceImageHolder
import dev.brahmkshatriya.echo.playback.MediaItemUtils.SHOW_BACKGROUND
import dev.brahmkshatriya.echo.ui.common.UiViewModel.Companion.BACKGROUND_GRADIENT
import dev.brahmkshatriya.echo.ui.common.UiViewModel.Companion.NAVBAR_GRADIENT
import dev.brahmkshatriya.echo.ui.player.PlayerFragment.Companion.DYNAMIC_PLAYER
import dev.brahmkshatriya.echo.utils.ContextUtils.SETTINGS_NAME
import dev.brahmkshatriya.echo.utils.ui.AnimationUtils.ANIMATIONS_KEY
import dev.brahmkshatriya.echo.utils.ui.AnimationUtils.SCROLL_ANIMATIONS_KEY
import dev.brahmkshatriya.echo.utils.ui.FastScrollerHelper.SCROLL_BAR
import dev.brahmkshatriya.echo.utils.ui.prefs.ColorListPreference
import dev.brahmkshatriya.echo.utils.ui.prefs.MaterialListPreference
import dev.brahmkshatriya.echo.utils.ui.prefs.MaterialSliderPreference

class SettingsLookFragment : BaseSettingsFragment() {
    override val title get() = getString(R.string.look_and_feel)
    override val icon get() = R.drawable.ic_palette.toResourceImageHolder()
    override val creator = { LookPreference() }

    class LookPreference : PreferenceFragmentCompat() {
        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            configure()
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            val context = preferenceManager.context
            preferenceManager.sharedPreferencesName = SETTINGS_NAME
            preferenceManager.sharedPreferencesMode = Context.MODE_PRIVATE
            val preferences = preferenceManager.sharedPreferences ?: return

            val screen = preferenceManager.createPreferenceScreen(context)
            preferenceScreen = screen

            // ========== COLORS CATEGORY (existing + new) ==========
            PreferenceCategory(context).apply {
                title = getString(R.string.colors)
                key = "colors"
                isIconSpaceReserved = false
                layoutResource = R.layout.preference_category
                screen.addPreference(this)

                MaterialListPreference(context).apply {
                    key = THEME_KEY
                    title = getString(R.string.theme)
                    summary = getString(R.string.theme_summary)
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false

                    entries = context.resources.getStringArray(R.array.themes)
                    entryValues = arrayOf("light", "dark", "system")
                    value = preferences.getString(THEME_KEY, "system")
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = CUSTOM_THEME_KEY
                    title = getString(R.string.custom_theme_color)
                    summary = getString(R.string.custom_theme_color_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, it ->
                        screen.findPreference<Preference>(COLOR_KEY)?.isEnabled = it as Boolean
                        true
                    }
                    addPreference(this)
                }

                ColorListPreference(this@LookPreference).apply {
                    key = COLOR_KEY
                    setDefaultValue(context.defaultColor())
                    isEnabled = preferences.getBoolean(CUSTOM_THEME_KEY, true)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = AMOLED_KEY
                    title = getString(R.string.amoled)
                    summary = getString(R.string.amoled_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = NAVBAR_GRADIENT
                    title = getString(R.string.navbar_gradient)
                    summary = getString(R.string.navbar_gradient_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = BACKGROUND_GRADIENT
                    title = getString(R.string.background_gradient)
                    summary = getString(R.string.background_gradient_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = DYNAMIC_PLAYER
                    title = getString(R.string.dynamic_player)
                    summary = getString(R.string.dynamic_player_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }

                // Dynamic Theme
                SwitchPreferenceCompat(context).apply {
                    key = "dynamicTheme"
                    title = getString(R.string.enable_dynamic_theme)
                    summary = getString(R.string.enable_dynamic_theme_desc)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }

                // Random theme on startup
                SwitchPreferenceCompat(context).apply {
                    key = "randomThemeOnStartup"
                    title = getString(R.string.random_theme_on_startup)
                    summary = getString(R.string.random_theme_on_startup_desc)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                // Pure Black
                SwitchPreferenceCompat(context).apply {
                    key = "pureBlack"
                    title = getString(R.string.pure_black)
                    summary = getString(R.string.pure_black_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }
            }

            // ========== UI CATEGORY (existing + new) ==========
            PreferenceCategory(context).apply {
                title = getString(R.string.ui)
                key = "ui"
                isIconSpaceReserved = false
                layoutResource = R.layout.preference_category
                screen.addPreference(this)

                SwitchPreferenceCompat(context).apply {
                    key = BIG_COVER
                    title = getString(R.string.big_cover)
                    summary = getString(R.string.big_cover_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = SCROLL_BAR
                    title = getString(R.string.scroll_bar)
                    summary = getString(R.string.scroll_bar_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = SHOW_BACKGROUND
                    title = getString(R.string.show_background)
                    summary = getString(R.string.show_background_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }

                // Use System Font
                SwitchPreferenceCompat(context).apply {
                    key = "useSystemFont"
                    title = getString(R.string.use_system_font)
                    summary = getString(R.string.use_system_font_desc)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                // Slim Navbar
                SwitchPreferenceCompat(context).apply {
                    key = "slimNavBar"
                    title = getString(R.string.slim_navbar)
                    summary = getString(R.string.slim_navbar_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                // Grid Item Size
                MaterialListPreference(context).apply {
                    key = "gridItemSize"
                    title = getString(R.string.grid_cell_size)
                    summary = getString(R.string.grid_cell_size_summary)
                    entries = arrayOf(
                        getString(R.string.grid_size_big),
                        getString(R.string.grid_size_small)
                    )
                    entryValues = arrayOf("BIG", "SMALL")
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    setDefaultValue("BIG")
                    addPreference(this)
                }

                // Default Open Tab
                MaterialListPreference(context).apply {
                    key = "defaultOpenTab"
                    title = getString(R.string.default_open_tab)
                    summary = getString(R.string.default_open_tab_summary)
                    entries = arrayOf(
                        getString(R.string.tab_home),
                        getString(R.string.tab_search),
                        getString(R.string.tab_library),
                        getString(R.string.tab_feed)
                    )
                    entryValues = arrayOf("HOME", "SEARCH", "LIBRARY", "FEED")
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    setDefaultValue("HOME")
                    addPreference(this)
                }

                // New Library Design
                SwitchPreferenceCompat(context).apply {
                    key = "useNewLibraryDesign"
                    title = getString(R.string.new_library_design)
                    summary = getString(R.string.new_library_design_description)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                // New Mini Player Design
                SwitchPreferenceCompat(context).apply {
                    key = "useNewMiniPlayerDesign"
                    title = getString(R.string.new_mini_player_design)
                    summary = getString(R.string.new_mini_player_design_description)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                // Liquid Glass
                SwitchPreferenceCompat(context).apply {
                    key = "enableLiquidGlass"
                    title = getString(R.string.enable_liquid_glass)
                    summary = getString(R.string.enable_liquid_glass_desc)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                // Liquid Glass Navbar
                SwitchPreferenceCompat(context).apply {
                    key = "liquid_glass_nav_bar"
                    title = getString(R.string.liquid_glass_navbar)
                    summary = getString(R.string.liquid_glass_navbar_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                // Show Home Category Chips
                SwitchPreferenceCompat(context).apply {
                    key = "showHomeCategoryChips"
                    title = getString(R.string.show_home_category_chips)
                    summary = getString(R.string.show_home_category_chips_desc)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }

                // Show Tags in Library
                SwitchPreferenceCompat(context).apply {
                    key = "showTagsInLibrary"
                    title = getString(R.string.show_tags_in_library)
                    summary = getString(R.string.show_tags_in_library_desc)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                // Show Liked Playlist
                SwitchPreferenceCompat(context).apply {
                    key = "show_liked_playlist"
                    title = getString(R.string.show_liked_playlist)
                    summary = getString(R.string.show_liked_playlist_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }

                // Show Downloaded Playlist
                SwitchPreferenceCompat(context).apply {
                    key = "show_downloaded_playlist"
                    title = getString(R.string.show_downloaded_playlist)
                    summary = getString(R.string.show_downloaded_playlist_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }

                // Show Top Playlist
                SwitchPreferenceCompat(context).apply {
                    key = "show_top_playlist"
                    title = getString(R.string.show_top_playlist)
                    summary = getString(R.string.show_top_playlist_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }

                // Show Cached Playlist
                SwitchPreferenceCompat(context).apply {
                    key = "show_cached_playlist"
                    title = getString(R.string.show_cached_playlist)
                    summary = getString(R.string.show_cached_playlist_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }

                // Show Local Playlist
                SwitchPreferenceCompat(context).apply {
                    key = "show_local_playlist"
                    title = getString(R.string.show_local_playlist)
                    summary = getString(R.string.show_local_playlist_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }

                // Haptic Feedback
                SwitchPreferenceCompat(context).apply {
                    key = "enableHapticFeedback"
                    title = getString(R.string.enable_haptic_feedback)
                    summary = getString(R.string.enable_haptic_feedback_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }

                // Player Fullscreen
                SwitchPreferenceCompat(context).apply {
                    key = "player_fullscreen"
                    title = getString(R.string.player_fullscreen)
                    summary = getString(R.string.player_fullscreen_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }
            }

            // ========== PLAYER DESIGN CATEGORY ==========
            PreferenceCategory(context).apply {
                title = getString(R.string.player_design_style)
                key = "player_design"
                isIconSpaceReserved = false
                layoutResource = R.layout.preference_category
                screen.addPreference(this)

                // Player Design Style
                MaterialListPreference(context).apply {
                    key = "playerDesignStyle"
                    title = getString(R.string.player_design_style)
                    summary = getString(R.string.player_design_style_summary)
                    entries = arrayOf(
                        getString(R.string.player_design_default),
                        getString(R.string.player_design_v1),
                        getString(R.string.player_design_v2),
                        getString(R.string.player_design_v3),
                        getString(R.string.player_design_v4),
                        getString(R.string.player_design_v5),
                        getString(R.string.player_design_v6),
                        getString(R.string.player_design_v7),
                        getString(R.string.player_design_spotify)
                    )
                    entryValues = arrayOf(
                        "PIXEL", "V1", "V2", "V3", "V4", "V5", "V6", "V7", "SPOTIFY"
                    )
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    setDefaultValue("PIXEL")
                    addPreference(this)
                }

                // Player Background Style
                MaterialListPreference(context).apply {
                    key = "playerBackgroundStyle"
                    title = getString(R.string.player_background_style)
                    summary = getString(R.string.player_background_style_summary)
                    entries = arrayOf(
                        getString(R.string.player_background_default),
                        getString(R.string.player_background_gradient),
                        getString(R.string.player_background_custom),
                        getString(R.string.player_background_blur),
                        getString(R.string.player_background_coloring),
                        getString(R.string.player_background_blur_gradient),
                        getString(R.string.player_background_glow),
                        getString(R.string.player_background_glow_animated),
                        getString(R.string.player_background_spotify)
                    )
                    entryValues = arrayOf(
                        "DEFAULT", "GRADIENT", "CUSTOM", "BLUR", "COLORING",
                        "BLUR_GRADIENT", "GLOW", "GLOW_ANIMATED", "SPOTIFY"
                    )
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    setDefaultValue("DEFAULT")
                    addPreference(this)
                }

                // Player Buttons Style
                MaterialListPreference(context).apply {
                    key = "player_buttons_style"
                    title = getString(R.string.player_buttons_style)
                    summary = getString(R.string.player_buttons_style_summary)
                    entries = arrayOf(
                        getString(R.string.player_buttons_default),
                        getString(R.string.player_buttons_secondary)
                    )
                    entryValues = arrayOf("DEFAULT", "SECONDARY")
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    setDefaultValue("DEFAULT")
                    addPreference(this)
                }

                // Slider Style
                MaterialListPreference(context).apply {
                    key = "sliderStyle"
                    title = getString(R.string.player_slider_style)
                    summary = getString(R.string.player_slider_style_summary)
                    entries = arrayOf(
                        getString(R.string.slider_style_standard),
                        getString(R.string.slider_style_wavy),
                        getString(R.string.slider_style_thick),
                        getString(R.string.slider_style_circular),
                        getString(R.string.slider_style_simple)
                    )
                    entryValues = arrayOf("Standard", "Wavy", "Thick", "Circular", "Simple")
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    setDefaultValue("Standard")
                    addPreference(this)
                }

                // Squiggly Slider
                SwitchPreferenceCompat(context).apply {
                    key = "enableSquigglySlider"
                    title = getString(R.string.enable_squiggly_slider)
                    summary = getString(R.string.enable_squiggly_slider_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                // Hide Player Thumbnail
                SwitchPreferenceCompat(context).apply {
                    key = "hidePlayerThumbnail"
                    title = getString(R.string.hide_player_thumbnail)
                    summary = getString(R.string.hide_player_thumbnail_desc)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                // Thumbnail Corner Radius
                MaterialSliderPreference(context, 0, 32, steps = 32).apply {
                    key = "thumbnailCornerRadius"
                    title = getString(R.string.thumbnail_corner_radius)
                    summary = getString(R.string.thumbnail_corner_radius_summary)
                    isIconSpaceReserved = false
                    setDefaultValue(8)
                    addPreference(this)
                }

                // Crop Thumbnail to Square
                SwitchPreferenceCompat(context).apply {
                    key = "cropThumbnailToSquare"
                    title = getString(R.string.crop_thumbnail_to_square)
                    summary = getString(R.string.crop_thumbnail_to_square_desc)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }

                // Seek Extra Seconds
                SwitchPreferenceCompat(context).apply {
                    key = "seekExtraSeconds"
                    title = getString(R.string.seek_extra_seconds)
                    summary = getString(R.string.seek_extra_seconds_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                // Disable Blur
                SwitchPreferenceCompat(context).apply {
                    key = "disableBlur"
                    title = getString(R.string.disable_blur)
                    summary = getString(R.string.disable_blur_desc)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                // Blur Radius
                MaterialSliderPreference(context, 0, 50, steps = 50).apply {
                    key = "blurRadius"
                    title = getString(R.string.blur_radius)
                    summary = getString(R.string.blur_radius_summary)
                    isIconSpaceReserved = false
                    setDefaultValue(16)
                    addPreference(this)
                }
            }

            // ========== CANVAS CATEGORY ==========
            PreferenceCategory(context).apply {
                title = getString(R.string.dhun_canvas)
                key = "canvas"
                isIconSpaceReserved = false
                layoutResource = R.layout.preference_category
                screen.addPreference(this)

                SwitchPreferenceCompat(context).apply {
                    key = "DhunCanvas"
                    title = getString(R.string.dhun_canvas)
                    summary = getString(R.string.dhun_canvas_desc)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                MaterialListPreference(context).apply {
                    key = "canvasSource"
                    title = getString(R.string.canvas_source)
                    summary = getString(R.string.canvas_source_summary)
                    entries = arrayOf(
                        getString(R.string.canvas_source_auto),
                        getString(R.string.canvas_source_apple_music),
                        getString(R.string.canvas_source_dhun),
                        getString(R.string.canvas_source_tidal)
                    )
                    entryValues = arrayOf("AUTO", "APPLE_MUSIC", "DHUN", "TIDAL")
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    setDefaultValue("AUTO")
                    addPreference(this)
                }
            }

            // ========== GRADIENTS & COLORS CATEGORY ==========
            PreferenceCategory(context).apply {
                title = getString(R.string.colors)
                key = "gradients"
                isIconSpaceReserved = false
                layoutResource = R.layout.preference_category
                screen.addPreference(this)

                // Background Gradient Type
                MaterialListPreference(context).apply {
                    key = "playerGradientType"
                    title = getString(R.string.player_gradient_type)
                    summary = getString(R.string.player_gradient_type_summary)
                    entries = arrayOf(
                        getString(R.string.player_background_default),
                        getString(R.string.player_background_gradient),
                        getString(R.string.player_background_blur),
                        getString(R.string.player_background_blur_gradient)
                    )
                    entryValues = arrayOf("DEFAULT", "GRADIENT", "BLUR", "BLUR_GRADIENT")
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    setDefaultValue("GRADIENT")
                    addPreference(this)
                }

                // Back Gradient
                SwitchPreferenceCompat(context).apply {
                    key = "backGrad"
                    title = getString(R.string.back_grad)
                    summary = getString(R.string.back_grad_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }

                // Card Gradient
                SwitchPreferenceCompat(context).apply {
                    key = "cardGrad"
                    title = getString(R.string.card_grad)
                    summary = getString(R.string.card_grad_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                // Bottom Bar Gradient
                SwitchPreferenceCompat(context).apply {
                    key = "bottomGrad"
                    title = getString(R.string.bottom_grad)
                    summary = getString(R.string.bottom_grad_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }

                // Canvas Color
                MaterialSliderPreference(context, 0, 360, steps = 360).apply {
                    key = "canvasColor"
                    title = getString(R.string.canvas_color)
                    summary = getString(R.string.canvas_color_summary)
                    isIconSpaceReserved = false
                    setDefaultValue(0)
                    addPreference(this)
                }

                // Card Color
                MaterialSliderPreference(context, 0, 360, steps = 360).apply {
                    key = "cardColor"
                    title = getString(R.string.card_color)
                    summary = getString(R.string.card_color_summary)
                    isIconSpaceReserved = false
                    setDefaultValue(0)
                    addPreference(this)
                }

                // Color Hue
                MaterialSliderPreference(context, 0, 360, steps = 360).apply {
                    key = "colorHue"
                    title = getString(R.string.color_hue)
                    summary = getString(R.string.color_hue_summary)
                    isIconSpaceReserved = false
                    setDefaultValue(0)
                    addPreference(this)
                }

                // Translucent Bottom Bar
                SwitchPreferenceCompat(context).apply {
                    key = "translucentBottomBar"
                    title = getString(R.string.translucent_bottom_bar)
                    summary = getString(R.string.translucent_bottom_bar_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                // Dense Mini Player
                SwitchPreferenceCompat(context).apply {
                    key = "useDenseMini"
                    title = getString(R.string.use_dense_mini)
                    summary = getString(R.string.use_dense_mini_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }
            }

            // ========== PLAYER CUSTOM BACKGROUND CATEGORY ==========
            PreferenceCategory(context).apply {
                title = getString(R.string.customized_background)
                key = "player_custom_bg"
                isIconSpaceReserved = false
                layoutResource = R.layout.preference_category
                screen.addPreference(this)

                // Custom Image URI
                dev.brahmkshatriya.echo.utils.ui.prefs.MaterialTextInputPreference(context).apply {
                    key = "playerCustomImageUri"
                    title = getString(R.string.player_custom_image_uri)
                    summary = getString(R.string.player_custom_image_uri_summary)
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    addPreference(this)
                }

                // Custom Blur
                MaterialSliderPreference(context, 0, 50, steps = 50).apply {
                    key = "playerCustomBlur"
                    title = getString(R.string.player_custom_blur)
                    isIconSpaceReserved = false
                    setDefaultValue(16)
                    addPreference(this)
                }

                // Custom Contrast
                MaterialSliderPreference(context, 0, 200, steps = 200).apply {
                    key = "playerCustomContrast"
                    title = getString(R.string.player_custom_contrast)
                    isIconSpaceReserved = false
                    setDefaultValue(100)
                    addPreference(this)
                }

                // Custom Brightness
                MaterialSliderPreference(context, 0, 200, steps = 200).apply {
                    key = "playerCustomBrightness"
                    title = getString(R.string.player_custom_brightness)
                    isIconSpaceReserved = false
                    setDefaultValue(100)
                    addPreference(this)
                }
            }

            // ========== AOD (ALWAYS-ON DISPLAY) CATEGORY ==========
            PreferenceCategory(context).apply {
                title = getString(R.string.aod_settings)
                key = "aod"
                isIconSpaceReserved = false
                layoutResource = R.layout.preference_category
                screen.addPreference(this)

                MaterialListPreference(context).apply {
                    key = "aod_style"
                    title = getString(R.string.aod_style)
                    summary = getString(R.string.aod_style_summary)
                    entries = arrayOf(getString(R.string.aod_style), getString(R.string.follow_theme))
                    entryValues = arrayOf("background", "minimal")
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    setDefaultValue("background")
                    addPreference(this)
                }

                MaterialListPreference(context).apply {
                    key = "aod_art_shape"
                    title = getString(R.string.aod_art_shape)
                    summary = getString(R.string.aod_art_shape_summary)
                    entries = arrayOf("Circle", "Square", "Rounded")
                    entryValues = arrayOf("circle", "square", "rounded")
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    setDefaultValue("circle")
                    addPreference(this)
                }

                MaterialSliderPreference(context, 0, 100, steps = 100).apply {
                    key = "aod_darkness"
                    title = getString(R.string.aod_darkness)
                    summary = getString(R.string.aod_darkness_summary)
                    isIconSpaceReserved = false
                    setDefaultValue(80)
                    addPreference(this)
                }

                MaterialSliderPreference(context, 50, 200, steps = 150).apply {
                    key = "aod_art_size"
                    title = getString(R.string.aod_art_size)
                    summary = getString(R.string.aod_art_size_summary)
                    isIconSpaceReserved = false
                    setDefaultValue(100)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = "aod_show_title"
                    title = getString(R.string.aod_show_title)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = "aod_show_artist"
                    title = getString(R.string.aod_show_artist)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = "aod_show_time_labels"
                    title = getString(R.string.aod_show_time_labels)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = "aod_show_progress"
                    title = getString(R.string.aod_show_progress)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = "aod_show_controls"
                    title = getString(R.string.aod_show_controls)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                MaterialSliderPreference(context, 0, 120, steps = 120).apply {
                    key = "aod_auto_activation_seconds"
                    title = getString(R.string.aod_auto_activation)
                    summary = getString(R.string.aod_auto_activation_summary)
                    isIconSpaceReserved = false
                    setDefaultValue(15)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = "aod_fullscreen_mode"
                    title = getString(R.string.aod_fullscreen_mode)
                    summary = getString(R.string.aod_fullscreen_mode_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = "aod_show_clock"
                    title = getString(R.string.aod_show_clock)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = "aod_clock_24h"
                    title = getString(R.string.aod_clock_24h)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }
            }

            // ========== MINI PLAYER CATEGORY ==========
            PreferenceCategory(context).apply {
                title = getString(R.string.mini_player_settings)
                key = "mini_player"
                isIconSpaceReserved = false
                layoutResource = R.layout.preference_category
                screen.addPreference(this)

                // New Mini Player Design
                SwitchPreferenceCompat(context).apply {
                    key = "useNewMiniPlayerDesign"
                    title = getString(R.string.new_mini_player_design)
                    summary = getString(R.string.new_mini_player_design_description)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                // Dense Mini Player
                SwitchPreferenceCompat(context).apply {
                    key = "useDenseMini"
                    title = getString(R.string.use_dense_mini)
                    summary = getString(R.string.use_dense_mini_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                // Mini Player Button Order
                MaterialTextInputPreference(context).apply {
                    key = "miniButtonsOrder"
                    title = getString(R.string.mini_player_button_order)
                    summary = getString(R.string.mini_player_button_order_summary)
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    setDefaultValue("play_pause,skip_next,like")
                    addPreference(this)
                }

                // Preferred Mini Player Buttons
                dev.brahmkshatriya.echo.utils.ui.prefs.MaterialMultipleChoicePreference(context).apply {
                    key = "preferredMiniButtons"
                    title = getString(R.string.mini_player_buttons)
                    summary = getString(R.string.mini_player_buttons_summary)
                    entries = arrayOf(
                        getString(R.string.mini_button_play_pause),
                        getString(R.string.mini_button_skip_next),
                        getString(R.string.mini_button_skip_previous),
                        getString(R.string.mini_button_like),
                        getString(R.string.mini_button_queue),
                        getString(R.string.mini_button_more),
                        getString(R.string.mini_button_artist)
                    )
                    entryValues = arrayOf(
                        "play_pause", "skip_next", "skip_previous",
                        "like", "queue", "more", "artist"
                    )
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    addPreference(this)
                }

                // Preferred Compact Notification Buttons
                dev.brahmkshatriya.echo.utils.ui.prefs.MaterialMultipleChoicePreference(context).apply {
                    key = "preferredCompactNotificationButtons"
                    title = getString(R.string.preferred_compact_notification_buttons)
                    summary = getString(R.string.preferred_compact_notification_buttons_summary)
                    entries = arrayOf(
                        getString(R.string.mini_button_play_pause),
                        getString(R.string.mini_button_skip_next),
                        getString(R.string.mini_button_skip_previous),
                        getString(R.string.mini_button_like)
                    )
                    entryValues = arrayOf(
                        "play_pause", "skip_next", "skip_previous", "like"
                    )
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    addPreference(this)
                }

                // Mini Player Last Anchor
                MaterialSliderPreference(context, 0, 100, steps = 100).apply {
                    key = "miniPlayerLastAnchor"
                    title = getString(R.string.mini_player_anchor)
                    summary = getString(R.string.mini_player_anchor_summary)
                    isIconSpaceReserved = false
                    setDefaultValue(0)
                    addPreference(this)
                }

                // Liquid Glass for Mini Player
                SwitchPreferenceCompat(context).apply {
                    key = "enableLiquidGlass"
                    title = getString(R.string.enable_liquid_glass)
                    summary = getString(R.string.enable_liquid_glass_desc)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                // Hide Player Thumbnail
                SwitchPreferenceCompat(context).apply {
                    key = "hidePlayerThumbnail"
                    title = getString(R.string.hide_player_thumbnail)
                    summary = getString(R.string.hide_player_thumbnail_desc)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                // Player Fullscreen
                SwitchPreferenceCompat(context).apply {
                    key = "player_fullscreen"
                    title = getString(R.string.player_fullscreen)
                    summary = getString(R.string.player_fullscreen_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                // Minimum Volume
                MaterialSliderPreference(context, 0, 100, steps = 100).apply {
                    key = "minimumVolume"
                    title = getString(R.string.minimum_volume)
                    summary = getString(R.string.minimum_volume_summary)
                    isIconSpaceReserved = false
                    setDefaultValue(0)
                    addPreference(this)
                }

                // Maximum Volume
                MaterialSliderPreference(context, 0, 100, steps = 100).apply {
                    key = "maximumVolume"
                    title = getString(R.string.maximum_volume)
                    summary = getString(R.string.maximum_volume_summary)
                    isIconSpaceReserved = false
                    setDefaultValue(100)
                    addPreference(this)
                }
            }

            // ========== NAVIGATION CATEGORY ==========
            PreferenceCategory(context).apply {
                title = getString(R.string.navigation_settings)
                key = "navigation"
                isIconSpaceReserved = false
                layoutResource = R.layout.preference_category
                screen.addPreference(this)

                // Slim Navbar
                SwitchPreferenceCompat(context).apply {
                    key = "slimNavBar"
                    title = getString(R.string.slim_navbar)
                    summary = getString(R.string.slim_navbar_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                // Translucent Bottom Bar
                SwitchPreferenceCompat(context).apply {
                    key = "translucentBottomBar"
                    title = getString(R.string.translucent_bottom_bar)
                    summary = getString(R.string.translucent_bottom_bar_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                // Liquid Glass Navbar
                SwitchPreferenceCompat(context).apply {
                    key = "liquid_glass_nav_bar"
                    title = getString(R.string.liquid_glass_navbar)
                    summary = getString(R.string.liquid_glass_navbar_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                // Floating Navigation Bar
                SwitchPreferenceCompat(context).apply {
                    key = "floatingNavigation"
                    title = getString(R.string.floating_navigation)
                    summary = getString(R.string.floating_navigation_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                // Customize Navigation Tabs (button)
                TransitionPreference(context).apply {
                    key = "customizeNavigationTabs"
                    title = getString(R.string.customize_navigation_tabs)
                    summary = getString(R.string.customize_navigation_tabs_summary)
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    addPreference(this)
                }

                // Default Open Tab
                MaterialListPreference(context).apply {
                    key = "defaultOpenTab"
                    title = getString(R.string.default_open_tab)
                    summary = getString(R.string.default_open_tab_summary)
                    entries = arrayOf(
                        getString(R.string.nav_tab_home),
                        getString(R.string.nav_tab_search),
                        getString(R.string.nav_tab_library),
                        getString(R.string.nav_tab_feed)
                    )
                    entryValues = arrayOf("HOME", "SEARCH", "LIBRARY", "FEED")
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    setDefaultValue("HOME")
                    addPreference(this)
                }
            }

            // ========== ANIMATION CATEGORY (existing) ==========
            PreferenceCategory(context).apply {
                title = getString(R.string.animation)
                key = "animation"
                isIconSpaceReserved = false
                layoutResource = R.layout.preference_category
                screen.addPreference(this)

                SwitchPreferenceCompat(context).apply {
                    key = BACK_ANIM
                    title = getString(R.string.back_animations)
                    summary = getString(R.string.back_animations_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = ANIMATIONS_KEY
                    title = getString(R.string.animations)
                    summary = getString(R.string.animations_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = SCROLL_ANIMATIONS_KEY
                    title = getString(R.string.scroll_animations)
                    summary = getString(R.string.scroll_animations_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                // Sonar Swipe
                SwitchPreferenceCompat(context).apply {
                    key = "sonarSwipe"
                    title = getString(R.string.sonar_swipe)
                    summary = getString(R.string.sonar_swipe_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }
            }
        }

        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                THEME_KEY, CUSTOM_THEME_KEY, COLOR_KEY, AMOLED_KEY,
                BIG_COVER, NAVBAR_GRADIENT, BACKGROUND_GRADIENT,
                "pureBlack", "dynamicTheme", "randomThemeOnStartup",
                "useSystemFont", "slimNavBar", "useNewLibraryDesign",
                "useNewMiniPlayerDesign", "enableLiquidGlass", "liquid_glass_nav_bar",
                "playerDesignStyle", "playerBackgroundStyle", "hidePlayerThumbnail",
                "disableBlur", "DhunCanvas", "showHomeCategoryChips",
                "showTagsInLibrary", "gridItemSize",
                "darkMode", "translucentBottomBar", "useDenseMini",
                "backGrad", "cardGrad", "bottomGrad", "playerGradientType",
                "aod_style", "aod_art_shape", "aod_darkness", "aod_art_size",
                "aod_fullscreen_mode", "aod_show_clock", "aod_clock_24h",
                "useNewMiniPlayerDesign", "useDenseMini", "floatingNavigation",
                "translucentBottomBar", "miniPlayerLastAnchor", "minimumVolume",
                "maximumVolume",
                    -> {
                    requireActivity().recreate()
                }

                BACK_ANIM -> {
                    val pref = preferenceScreen.findPreference<SwitchPreferenceCompat>(key)
                    val enabled = pref?.isChecked == true
                    val backActivity = MainActivity.Back::class.java.name
                    val mainActivity = MainActivity::class.java.name
                    requireActivity().changeEnabledComponent(
                        if (enabled) backActivity else mainActivity,
                        if (enabled) mainActivity else backActivity
                    )
                }
            }
        }

        override fun onResume() {
            super.onResume()
            preferenceManager.sharedPreferences!!
                .registerOnSharedPreferenceChangeListener(listener)
        }

        override fun onPause() {
            super.onPause()
            preferenceManager.sharedPreferences!!
                .unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    companion object {

        fun Activity.changeEnabledComponent(enabled: String, disabled: String) {
            packageManager.setComponentEnabledSetting(
                ComponentName(this, enabled),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
            packageManager.setComponentEnabledSetting(
                ComponentName(this, disabled),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }
    }
}
