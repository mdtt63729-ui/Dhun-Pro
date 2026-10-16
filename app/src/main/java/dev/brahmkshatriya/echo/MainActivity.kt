package dev.brahmkshatriya.echo

import android.content.Context
import android.graphics.Color
import android.graphics.Color.TRANSPARENT
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.add
import androidx.fragment.app.commit
import androidx.lifecycle.Lifecycle
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
import dev.brahmkshatriya.echo.utils.PermsUtils.checkAppPermissions
import dev.brahmkshatriya.echo.utils.ui.UiUtils.isNightMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs

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
            if (coldStart) delay(SPLASH_ANIM_DURATION_MS)
            splashFinished.set(true)
        }
        super.onCreate(savedInstanceState)
        setTheme(getAppTheme())
        DynamicColors.applyToActivityIfAvailable(
            this, applyUiChanges(this, uiViewModel)
        )

        setContentView(binding.root)

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
        // Remember which artwork colour the theme was actually built from (may be none).
        if (getSettings().getBoolean(PLAYER_COLOR, false)) {
            appliedThemeColor = uiViewModel.playerColors.value?.accent
        }
        observeDynamicColor()
    }

    /**
     * When "Dynamic Color" is on, the whole UI is tinted from the current song's
     * artwork. The theme can only be rebuilt by recreating the activity, so this does
     * that — but only when the artwork colour actually changed enough to notice, at
     * most once every [THEME_RECREATE_INTERVAL_MS], and only while the UI is resumed.
     */
    private fun observeDynamicColor() {
        if (!getSettings().getBoolean(PLAYER_COLOR, false)) return
        lifecycleScope.launch {
            uiViewModel.playerColors.collect { colors ->
                val accent = colors?.accent ?: return@collect
                val applied = appliedThemeColor
                if (applied == accent) return@collect
                if (applied != null && !isThemeColorDifferent(accent, applied)) return@collect
                val now = SystemClock.elapsedRealtime()
                if (now - lastThemeRecreate < THEME_RECREATE_INTERVAL_MS) return@collect
                if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return@collect
                appliedThemeColor = accent
                lastThemeRecreate = now
                recreate()
            }
        }
    }

    private fun isThemeColorDifferent(a: Int, b: Int): Boolean {
        val first = FloatArray(3)
        val second = FloatArray(3)
        Color.colorToHSV(a, first)
        Color.colorToHSV(b, second)
        val hue = abs(first[0] - second[0]).let { if (it > 180f) 360f - it else it }
        val saturation = abs(first[1] - second[1]) * 100f
        return hue > THEME_HUE_THRESHOLD || saturation > 25f
    }

    companion object {
        /** Matches android:windowSplashScreenAnimationDuration in themes.xml. */
        const val SPLASH_ANIM_DURATION_MS = 900L

        /** How different two artwork colours must be before the theme is rebuilt. */
        private const val THEME_HUE_THRESHOLD = 24f

        /** Never rebuild the theme more often than this. */
        private const val THEME_RECREATE_INTERVAL_MS = 8_000L

        /** The artwork colour the current theme was built from; survives a recreate. */
        private var appliedThemeColor: Int? = null
        private var lastThemeRecreate = 0L

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