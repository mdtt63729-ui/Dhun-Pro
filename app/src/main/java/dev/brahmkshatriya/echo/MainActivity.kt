package dev.brahmkshatriya.echo

import android.content.Context
import android.graphics.Color
import android.graphics.Color.TRANSPARENT
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.add
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.DynamicColorsOptions
import com.google.android.material.navigation.NavigationBarView
import dev.brahmkshatriya.echo.databinding.ActivityMainBinding
import dev.brahmkshatriya.echo.extensions.ExtensionLoader
import dev.brahmkshatriya.echo.ui.common.ExceptionUtils.setupExceptionHandler
import dev.brahmkshatriya.echo.ui.common.FragmentUtils.setupIntents
import dev.brahmkshatriya.echo.ui.common.SnackBarHandler.Companion.setupSnackBar
import dev.brahmkshatriya.echo.ui.common.UiViewModel
import dev.brahmkshatriya.echo.ui.common.UiViewModel.Companion.setupNavBarAndInsets
import dev.brahmkshatriya.echo.ui.common.UiViewModel.Companion.setupPlayerBehavior
import dev.brahmkshatriya.echo.ui.extensions.ExtensionsViewModel.Companion.configureExtensionsUpdater
import dev.brahmkshatriya.echo.ui.main.MainFragment
import dev.brahmkshatriya.echo.ui.player.PlayerFragment
import dev.brahmkshatriya.echo.ui.player.PlayerFragment.Companion.PLAYER_COLOR
import dev.brahmkshatriya.echo.utils.ContextUtils.getSettings
import dev.brahmkshatriya.echo.utils.SettingsKeys
import dev.brahmkshatriya.echo.utils.PermsUtils.checkAppPermissions
import dev.brahmkshatriya.echo.utils.ui.UiUtils.isNightMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import com.google.android.material.bottomsheet.BottomSheetBehavior
import android.content.res.ColorStateList
import androidx.core.graphics.ColorUtils
import android.graphics.drawable.Animatable
import androidx.core.view.isVisible

open class MainActivity : AppCompatActivity() {

    class Back : MainActivity()

    val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val uiViewModel by viewModel<UiViewModel>()
    private val extensionLoader by inject<ExtensionLoader>()

    private val splashFinished = AtomicBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        // Keep the system splash up until its icon animation has actually finished.
        // Without this the home screen appears mid-animation, because the first frame
        // is drawn long before the 900ms animation is over.
        //
        // Only on a cold start, though: holding the splash through a recreate (which is
        // what a theme/colour settings toggle does) made every toggle look like the app
        // was closing and restarting.
        val coldStart = savedInstanceState == null
        installSplashScreen().setKeepOnScreenCondition { coldStart && !splashFinished.get() }
        lifecycleScope.launch {
            if (coldStart) delay(SYSTEM_SPLASH_MS)
            splashFinished.set(true)
        }
        super.onCreate(savedInstanceState)

        // --- settings added from ArchiveTune; these must run before the theme is built ---
        val settings = getSettings()
        if (settings.getBoolean(SettingsKeys.RANDOM_THEME, false)) {
            SettingsKeys.randomPaletteColor()?.let { color ->
                settings.edit()
                    .putBoolean(CUSTOM_THEME_KEY, true)
                    .putInt(COLOR_KEY, color)
                    .apply()
            }
        }
        if (settings.getBoolean(SettingsKeys.DISABLE_SCREENSHOT, false)) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        }
        if (settings.getBoolean(SettingsKeys.FORCE_REFRESH_RATE, false)) {
            highestRefreshModeId()?.let { modeId ->
                window.attributes = window.attributes.apply { preferredDisplayModeId = modeId }
            }
        }

        setTheme(getAppTheme())
        DynamicColors.applyToActivityIfAvailable(
            this, applyUiChanges(this, uiViewModel)
        )

        setContentView(binding.root)

        playSplashAnimation(coldStart)

        enableEdgeToEdge(
            SystemBarStyle.auto(TRANSPARENT, TRANSPARENT),
            if (isNightMode()) SystemBarStyle.dark(TRANSPARENT)
            else SystemBarStyle.light(TRANSPARENT, TRANSPARENT)
        )

        setupNavBarAndInsets(uiViewModel, binding.root, binding.navView as NavigationBarView)
        setupPlayerBehavior(uiViewModel, binding.playerFragmentContainer)
        setupExceptionHandler(setupSnackBar(uiViewModel, binding.root))
        checkAppPermissions { extensionLoader.setPermGranted() }
        configureExtensionsUpdater()
        supportFragmentManager.commit {
            if (savedInstanceState != null) return@commit
            add<MainFragment>(R.id.navHostFragment, "main")
            add<PlayerFragment>(R.id.playerFragmentContainer, "player")
        }
        setupIntents(uiViewModel)
        observeDynamicColor()
    }

    private val handler = Handler(Looper.getMainLooper())

    /**
     * In this app a theme change means recreating the activity — that is the only way to
     * re-tint the whole UI. The rebuild is debounced so skipping through tracks does not
     * thrash it, and `setupTransition` skips the fragment animation while
     * `isChangingConfigurations` is set, so the screen does not flash blank or slide in.
     */
    /** The display mode with the highest refresh rate the panel supports, if any. */
    private fun highestRefreshModeId(): Int? {
        val display = display ?: return null
        return display.supportedModes.maxByOrNull { it.refreshRate }?.modeId
    }

    private var lastReTheme = 0L

    private val reThemeRunnable = object : Runnable {
        override fun run() {
            if (isChangingConfigurations || isFinishing || isDestroyed) return
            // The full player is where a rebuild hurts most: it re-inflates the player, the
            // queue and the lyrics while a song is playing, which is the freeze you see. The
            // artwork already tints the player live, so hold the app-wide rebuild back until
            // the player is closed and then apply it in one go.
            if (uiViewModel.playerSheetState.value != BottomSheetBehavior.STATE_HIDDEN &&
                uiViewModel.playerSheetState.value != BottomSheetBehavior.STATE_COLLAPSED
            ) {
                handler.postDelayed(this, RE_THEME_RETRY_MS)
                return
            }
            // Never rebuild more often than this, so a burst of colour changes cannot turn
            // into a recreate loop that leaves the app stuck.
            val now = SystemClock.elapsedRealtime()
            val since = now - lastReTheme
            if (since < RE_THEME_MIN_INTERVAL_MS) {
                handler.postDelayed(this, RE_THEME_MIN_INTERVAL_MS - since)
                return
            }
            lastReTheme = now
            appliedAccent = pendingAccent ?: appliedAccent
            recreate()
        }
    }

    /** The colour waiting to be applied once a rebuild is allowed to run. */
    private var pendingAccent: Int? = null

    /**
     * When "Dynamic Color" is on, rebuild the theme whenever the song's artwork colour has
     * moved far enough to be worth it — so the whole UI follows the song's wallpaper.
     */
    private fun observeDynamicColor() {
        if (!getSettings().getBoolean(PLAYER_COLOR, false)) return
        // Only seed when nothing has been applied yet — the rebuild keeps this value, so
        // overwriting it here would make the colour that was just applied look new again.
        if (appliedAccent == null) appliedAccent = uiViewModel.playerColors.value?.accent
        lifecycleScope.launch {
            uiViewModel.playerColors.collect { colors ->
                val accent = colors?.accent ?: return@collect
                // Added for the theme-change lag: tint the navigation bar immediately, without
                // rebuilding anything. That is what makes the colour change feel instant; the
                // full rebuild below is the expensive part and is deliberately rare.
                tintNavBar(accent)
                val applied = appliedAccent
                if (applied != null && accent == applied) return@collect
                if (applied != null && !isThemeColorDifferent(accent, applied)) return@collect
                pendingAccent = accent
                handler.removeCallbacks(reThemeRunnable)
                handler.postDelayed(reThemeRunnable, RE_THEME_DELAY_MS)
            }
        }
    }

    /**
     * Plays the splash animation in the app itself, then fades the UI in behind it.
     *
     * The system splash screen only runs one second of an icon animation — the platform caps it
     * — while this animation is 2.4s, so it was always being cut off. The system splash now
     * shows a still frame for a moment and this plays the whole thing.
     */
    private fun playSplashAnimation(coldStart: Boolean) {
        val binding = binding ?: return
        val overlay = binding.splashOverlay ?: return
        if (!coldStart) {
            overlay.isVisible = false
            return
        }
        // The overlay is a child of binding.root, so fading the root would fade the splash too.
        // The content is simply revealed by fading the overlay out at the end.
        overlay.isVisible = true
        overlay.alpha = 1f
        val image = binding.splashImage
        image?.setImageResource(R.drawable.art_splash_anim)
        (image?.drawable as? Animatable)?.start()
        overlay.postDelayed({
            if (isFinishing || isDestroyed) return@postDelayed
            overlay.animate().alpha(0f).setDuration(FADE_IN_MS).withEndAction {
                overlay.isVisible = false
            }.start()
        }, SPLASH_ANIM_DURATION_MS)
    }

    /**
     * Tints the bottom navigation bar with the artwork colour. No view is recreated, so this is
     * free compared to the activity rebuild, and it is the surface the user notices most.
     */
    private fun tintNavBar(accent: Int) {
        val nav = (binding.navView as? NavigationBarView) ?: return
        runCatching {
            val checked = intArrayOf(android.R.attr.state_checked)
            val unchecked = intArrayOf()
            val colors = intArrayOf(accent, ColorUtils.setAlphaComponent(accent, 170))
            nav.itemIconTintList = ColorStateList(arrayOf(checked, unchecked), colors)
            nav.itemTextColor = ColorStateList(arrayOf(checked, unchecked), colors)
            nav.itemActiveIndicatorColor =
                ColorStateList.valueOf(ColorUtils.setAlphaComponent(accent, 60))
        }
    }

    private fun isThemeColorDifferent(a: Int, b: Int): Boolean {
        val first = FloatArray(3)
        val second = FloatArray(3)
        Color.colorToHSV(a, first)
        Color.colorToHSV(b, second)
        val hue = abs(first[0] - second[0]).let { if (it > 180f) 360f - it else it }
        return hue > THEME_HUE_THRESHOLD || abs(first[1] - second[1]) * 100f > 25f
    }


    companion object {
        /** How long the splash animation actually runs (art_splash_anim's longest animator). */
        const val SPLASH_ANIM_DURATION_MS = 2400L

        /** How long the system splash is held before the app's own splash overlay takes over. */
        private const val SYSTEM_SPLASH_MS = 250L

        /** How long the cold-start fade-in of the whole UI takes. */
        private const val FADE_IN_MS = 260L

        /**
         * How different two artwork colours must be before the theme is worth a full rebuild.
         * Raised from 24 to 60: the navigation bar now re-tints live, so the rebuild only has
         * to catch up when the colour really jumps.
         */
        private const val THEME_HUE_THRESHOLD = 60f

        /** Debounce the rebuild so skipping through tracks does not thrash it. */
        private const val RE_THEME_DELAY_MS = 700L

        /** Hard floor between rebuilds, so colour changes can never stack up into a loop. */
        private const val RE_THEME_MIN_INTERVAL_MS = 8_000L

        /** How long to wait before re-checking whether the rebuild can run yet. */
        private const val RE_THEME_RETRY_MS = 1_500L

        /**
         * The artwork colour the theme on screen was built from.
         *
         * Deliberately in the companion: the rebuild throws the activity away, so a
         * per-instance field came back null every time and the colour that had just been
         * applied looked like a brand-new one.
         */
        private var appliedAccent: Int? = null

        const val THEME_KEY = "theme"
        const val AMOLED_KEY = "amoled"
        const val BIG_COVER = "big_cover"
        const val CUSTOM_THEME_KEY = "custom_theme"
        const val COLOR_KEY = "color"

        fun Context.getAppTheme(): Int {
            val settings = getSettings()
            val bigCover = settings.getBoolean(BIG_COVER, false)
            val amoled = settings.getBoolean(AMOLED_KEY, false)
            return when {
                amoled && bigCover -> R.style.AmoledBigCover
                amoled -> R.style.Amoled
                bigCover -> R.style.BigCover
                else -> R.style.Default
            }
        }

        fun Context.defaultColor() =
            ContextCompat.getColor(this, R.color.app_color)

        fun Context.isAmoled() = getSettings().getBoolean(AMOLED_KEY, false)

        fun applyUiChanges(context: Context, uiViewModel: UiViewModel): DynamicColorsOptions {
            val settings = context.getSettings()
            val mode = when (settings.getString(THEME_KEY, "system")) {
                "light" -> AppCompatDelegate.MODE_NIGHT_NO
                "dark" -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            AppCompatDelegate.setDefaultNightMode(mode)

            val custom = settings.getBoolean(CUSTOM_THEME_KEY, true)
            val color = if (custom) settings.getInt(COLOR_KEY, context.defaultColor()) else null
            // "Dynamic Color": tint the whole app from the current song's artwork.
            // Applied here, at activity creation, on purpose — re-theming a running
            // activity means recreating it, which flashed and lagged on every track
            // change. So the colour follows the song from the next app start instead.
            val playerColor = settings.getBoolean(PLAYER_COLOR, false)
            val customColor = uiViewModel.playerColors.value?.accent?.takeIf { playerColor }

            val builder = DynamicColorsOptions.Builder()
            (customColor ?: color)?.let { builder.setContentBasedSource(it) }
            return builder.build()
        }

        const val BACK_ANIM = "back_anim"
        fun Context.getMainActivity() = if (getSettings().getBoolean(BACK_ANIM, false))
            Back::class.java else MainActivity::class.java
    }
}