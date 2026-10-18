package dev.brahmkshatriya.echo.ui.settings

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import androidx.recyclerview.widget.LinearLayoutManager
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
import dev.brahmkshatriya.echo.ui.player.PlayerFragment.Companion.PLAYER_COLOR
import dev.brahmkshatriya.echo.utils.ContextUtils.SETTINGS_NAME
import dev.brahmkshatriya.echo.utils.SettingsKeys
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

                SwitchPreferenceCompat(context).apply {
                    key = PLAYER_COLOR
                    title = getString(R.string.dynamic_color)
                    summary = getString(R.string.dynamic_color_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                // ---- added from ArchiveTune ----

                MaterialListPreference(context).apply {
                    key = SettingsKeys.PALETTE
                    title = getString(R.string.palette)
                    summary = getString(R.string.palette_summary)
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    entries = SettingsKeys.paletteNames
                    entryValues = SettingsKeys.paletteValues
                    setDefaultValue(SettingsKeys.PALETTE_DEFAULT)
                    onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, v ->
                        val value = v as? String ?: return@OnPreferenceChangeListener true
                        val editor = preferences.edit()
                        if (value == SettingsKeys.PALETTE_DEFAULT) {
                            editor.putBoolean(CUSTOM_THEME_KEY, false)
                        } else {
                            runCatching { Color.parseColor(value) }.getOrNull()?.let { color ->
                                editor.putBoolean(CUSTOM_THEME_KEY, true)
                                editor.putInt(COLOR_KEY, color)
                            }
                        }
                        editor.apply()
                        recreateSafely()
                        true
                    }
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = SettingsKeys.RANDOM_THEME
                    title = getString(R.string.random_theme)
                    summary = getString(R.string.random_theme_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = SettingsKeys.DISABLE_BLUR
                    title = getString(R.string.disable_blur)
                    summary = getString(R.string.disable_blur_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                MaterialSliderPreference(context, 0, 100, allowOverride = true).apply {
                    key = SettingsKeys.BLUR_AMOUNT
                    title = getString(R.string.blur_amount)
                    summary = getString(R.string.blur_amount_summary)
                    isIconSpaceReserved = false
                    setDefaultValue(SettingsKeys.BLUR_AMOUNT_DEFAULT)
                    addPreference(this)
                }
            }

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

                // ---- added from ArchiveTune ----

                SwitchPreferenceCompat(context).apply {
                    key = SettingsKeys.FORCE_REFRESH_RATE
                    title = getString(R.string.force_refresh_rate)
                    summary = getString(R.string.force_refresh_rate_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, _ ->
                        recreateSafely()
                        true
                    }
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = SettingsKeys.HIDE_PLAYER_THUMBNAIL
                    title = getString(R.string.hide_player_thumbnail)
                    summary = getString(R.string.hide_player_thumbnail_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                MaterialSliderPreference(context, 0, 32, allowOverride = true).apply {
                    key = SettingsKeys.COVER_RADIUS
                    title = getString(R.string.cover_radius)
                    summary = getString(R.string.cover_radius_summary)
                    isIconSpaceReserved = false
                    setDefaultValue(SettingsKeys.COVER_RADIUS_DEFAULT)
                    addPreference(this)
                }

                MaterialListPreference(context).apply {
                    key = SettingsKeys.DEFAULT_TAB
                    title = getString(R.string.default_tab)
                    summary = getString(R.string.default_tab_summary)
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    entries = arrayOf(
                        getString(R.string.home_tab),
                        getString(R.string.search_tab),
                        getString(R.string.library_tab)
                    )
                    entryValues = arrayOf("0", "1", "2")
                    setDefaultValue(SettingsKeys.DEFAULT_TAB_HOME)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = SettingsKeys.HAPTICS
                    title = getString(R.string.haptics)
                    summary = getString(R.string.haptics_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = SettingsKeys.DISABLE_SCREENSHOT
                    title = getString(R.string.disable_screenshot)
                    summary = getString(R.string.disable_screenshot_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, _ ->
                        recreateSafely()
                        true
                    }
                    addPreference(this)
                }

                MaterialSliderPreference(context, 0, 24, allowOverride = true).apply {
                    key = SettingsKeys.LYRICS_LINE_SPACING
                    title = getString(R.string.lyrics_line_spacing)
                    summary = getString(R.string.lyrics_line_spacing_summary)
                    isIconSpaceReserved = false
                    setDefaultValue(SettingsKeys.LYRICS_LINE_SPACING_DEFAULT)
                    addPreference(this)
                }
            }

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
            }
        }

        private var recreatePosted = false

        /**
         * Recreating straight out of the preference callback — or twice in a row — is what
         * made toggling a setting crash and stutter. Defer it to the next frame, and never
         * schedule more than one at a time.
         */
        private fun recreateSafely() {
            if (recreatePosted) return
            recreatePosted = true
            // Remember where the list was, so the rebuild doesn't jump back to the top.
            pendingScroll =
                (listView?.layoutManager as? LinearLayoutManager)?.findFirstVisibleItemPosition()
                    ?: 0
            view?.post {
                recreatePosted = false
                val activity = activity ?: return@post
                if (activity.isFinishing || activity.isDestroyed) return@post
                activity.recreate()
            }
        }

        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                THEME_KEY, CUSTOM_THEME_KEY, COLOR_KEY, AMOLED_KEY,
                BIG_COVER, NAVBAR_GRADIENT, BACKGROUND_GRADIENT,
                PLAYER_COLOR,
                    -> recreateSafely()

                BACK_ANIM -> {
                    val pref = preferenceScreen.findPreference<SwitchPreferenceCompat>(key)
                    val enabled = pref?.isChecked == true
                    // Swapping the enabled activity component can take the running one
                    // down, so do it off the preference callback too.
                    view?.post {
                        val activity = activity ?: return@post
                        if (activity.isFinishing || activity.isDestroyed) return@post
                        val backActivity = MainActivity.Back::class.java.name
                        val mainActivity = MainActivity::class.java.name
                        activity.changeEnabledComponent(
                            if (enabled) backActivity else mainActivity,
                            if (enabled) mainActivity else backActivity
                        )
                    }
                }
            }
        }

        override fun onResume() {
            super.onResume()
            preferenceManager.sharedPreferences!!
                .registerOnSharedPreferenceChangeListener(listener)
            // Put the list back where it was before the recreate, instead of jumping to the top.
            val scroll = pendingScroll
            if (scroll > 0) {
                pendingScroll = 0
                listView?.post { listView?.scrollToPosition(scroll) }
            }
        }

        override fun onPause() {
            super.onPause()
            preferenceManager.sharedPreferences!!
                .unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    companion object {

        /** Preference-list position to restore after a theme rebuild. */
        private var pendingScroll = 0

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