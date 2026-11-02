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
import kotlin.math.abs
import com.google.android.material.bottomsheet.BottomSheetBehavior
import android.content.res.ColorStateList
import androidx.core.graphics.ColorUtils
import android.graphics.drawable.Animatable
import androidx.core.view.isVisible
import android.content.ClipData
import android.content.ClipboardManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.brahmkshatriya.echo.utils.CrashHandler
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build

open class MainActivity : AppCompatActivity() {

    class Back : MainActivity()

    val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val uiViewModel by viewModel<UiViewModel>()
    private val extensionLoader by inject<ExtensionLoader>()

    override fun onCreate(savedInstanceState: Bundle?) {
        // The compat splash screen is deliberately NOT installed.
        //
        // It loads the theme's windowSplashScreenAnimatedIcon itself, and on Android 7/8 that
        // load threw Resources$NotFoundException ("no path defined") and killed the app before
        // anything could draw. The app plays the splash animation in its own overlay instead,
        // so nothing here needs the library.
        val coldStart = savedInstanceState == null
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
        showLastCrashIfAny()
        requestCrashLogPermission()

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
     * Asks for storage access so the crash log can be written to /storage/emulated/0/echo.
     * Without it the log still lands in Android/data/<package>/files/echo, which needs no
     * permission at all.
     */
    private fun requestCrashLogPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) return
        val granted = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) return
        runCatching {
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1001)
        }
    }

    /**
     * Shows the trace of the previous crash, if there was one.
     *
     * The system "keeps stopping" dialog says nothing useful; this puts the actual exception and
     * the device details on screen so it can be copied and reported.
     */
    private fun showLastCrashIfAny() {
        val text = CrashHandler.last(this) ?: return
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.last_crash)
            .setMessage(text)
            .setPositiveButton(R.string.copy) { _, _ ->
                runCatching {
                    getSystemService(ClipboardManager::class.java)
                        ?.setPrimaryClip(ClipData.newPlainText("crash", text))
                }
                CrashHandler.clear(this)
            }
            .setNegativeButton(R.string.dismiss) { _, _ -> CrashHandler.clear(this) }
            .show()
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
        // Safety net: whatever happens, the splash never stays on screen forever.
        overlay.postDelayed({
            if (!isFinishing && !isDestroyed) overlay.isVisible = false
        }, SPLASH_ANIM_DURATION_MS + SPLASH_SAFETY_MS)
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

        /** How long the cold-start fade-in of the whole UI takes. */
        private const val FADE_IN_MS = 260L

        /** Extra time before the splash overlay is force-hidden, whatever else happened. */
        private const val SPLASH_SAFETY_MS = 4_000L

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