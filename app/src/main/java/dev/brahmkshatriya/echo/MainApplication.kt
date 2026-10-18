package dev.brahmkshatriya.echo

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Looper
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.core.os.LocaleListCompat
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.request.allowHardware
import coil3.request.crossfade
import dev.brahmkshatriya.echo.di.DI
import dev.brahmkshatriya.echo.extensions.ExtensionLoader
import dev.brahmkshatriya.echo.utils.AppShortcuts.configureAppShortcuts
import dev.brahmkshatriya.echo.utils.CoroutineUtils
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.androix.startup.KoinStartup
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.koinConfiguration
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import dev.brahmkshatriya.echo.utils.net.HttpClient
import dev.brahmkshatriya.echo.utils.ContextUtils.getSettings
import dev.brahmkshatriya.echo.utils.SettingsKeys
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import dev.brahmkshatriya.echo.utils.SettingsBackupWorker

@OptIn(KoinExperimentalAPI::class)
class MainApplication : Application(), KoinStartup, SingletonImageLoader.Factory {

    override fun onKoinStartup() = koinConfiguration {
        androidContext(this@MainApplication)
        modules(DI.appModule)
        workManagerFactory()
    }

    private val settings by inject<SharedPreferences>()
    private val extensionLoader by inject<ExtensionLoader>()

    override fun onCreate() {
        super.onCreate()
        CoroutineUtils.setDebug()
        applyLocale(settings)
        configureAppShortcuts(extensionLoader)
        scheduleSettingsBackup()
    }

    /**
     * Added from ArchiveTune: "Scheduled Backup". Enqueues (or cancels) the periodic
     * settings-backup worker to match the current preference.
     */
    private fun scheduleSettingsBackup() {
        val settings = getSettings()
        val workManager = WorkManager.getInstance(this)
        if (!settings.getBoolean(SettingsKeys.SCHEDULED_BACKUP, false)) {
            workManager.cancelUniqueWork(BACKUP_WORK_NAME)
            return
        }
        val days = when (
            settings.getString(SettingsKeys.BACKUP_SCHEDULE, SettingsKeys.BACKUP_SCHEDULE_DAILY)
        ) {
            BACKUP_SCHEDULE_WEEKLY -> 7L
            BACKUP_SCHEDULE_MONTHLY -> 30L
            else -> 1L
        }
        val request = PeriodicWorkRequestBuilder<SettingsBackupWorker>(days, TimeUnit.DAYS)
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.NOT_REQUIRED).build()
            )
            .build()
        workManager.enqueueUniquePeriodicWork(
            BACKUP_WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request
        )
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            // Added from ArchiveTune: images go through the shared client so the
            // DNS-over-HTTPS and proxy settings apply to them too.
            .components {
                add(
                    OkHttpNetworkFetcherFactory(
                        callFactory = { HttpClient.client(this@MainApplication) }
                    )
                )
            }
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.25)
                    .build()
            }
            .diskCache {
                // Added from ArchiveTune: "Image Cache Size".
                val mb = getSettings().getInt(
                    SettingsKeys.IMAGE_CACHE_SIZE, SettingsKeys.IMAGE_CACHE_SIZE_DEFAULT
                )
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image-cache"))
                    .maxSizeBytes(mb * 1024L * 1024L)
                    .build()
            }
            .allowHardware(false)
            .crossfade(true)
            .build()
    }

    override fun getPackageName(): String {
        // The stack walk below is expensive and only meaningful on the main thread,
        // so skip it entirely for background callers.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            Looper.myLooper() === Looper.getMainLooper()
        ) runCatching {
            val stackTrace = Looper.getMainLooper().thread.stackTrace
            val isChromiumCall = stackTrace.any { trace ->
                trace.className.equals(CLASS_NAME, ignoreCase = true)
                        && FUNCTION_SET.any { trace.methodName.equals(it, ignoreCase = true) }
            }
            if (isChromiumCall) return spoofedPackageName(applicationContext)
        }
        return super.getPackageName()
    }

    private fun spoofedPackageName(context: Context): String {
        return runCatching {
            context.packageManager.getPackageInfo(CHROME_PACKAGE, PackageManager.GET_META_DATA)
            CHROME_PACKAGE
        }.getOrElse {
            SYSTEM_SETTINGS_PACKAGE
        }
    }

    companion object {
        private const val BACKUP_WORK_NAME = "echo_settings_backup"
        private const val BACKUP_SCHEDULE_WEEKLY = "weekly"
        private const val BACKUP_SCHEDULE_MONTHLY = "monthly"

        private const val CHROME_PACKAGE = "com.android.chrome"
        private const val SYSTEM_SETTINGS_PACKAGE = "com.android.settings"
        private const val CLASS_NAME = "org.chromium.base.BuildInfo"
        private val FUNCTION_SET = setOf("getAll", "getPackageName", "<init>")

        fun getCurrentLanguage(sharedPref: SharedPreferences) =
            sharedPref.getString("language", null) ?: "system"

        fun setCurrentLanguage(sharedPref: SharedPreferences, locale: String?) {
            sharedPref.edit { putString("language", locale) }
            applyLocale(sharedPref)
        }

        fun applyLocale(sharedPref: SharedPreferences) {
            val value = sharedPref.getString("language", null) ?: "system"
            val locale = if (value == "system") LocaleListCompat.getEmptyLocaleList()
            else LocaleListCompat.forLanguageTags(value)
            AppCompatDelegate.setApplicationLocales(locale)
        }

        val languages = mapOf(
            "ar" to "العربية",
            "as" to "Assamese",
            "be" to "Беларуская",
            "bn" to "বাংলা",
            "ca" to "Català",
            "de" to "Deutsch",
            "es" to "Español",
            "fa" to "فارسی",
            "fr" to "Français",
            "en" to "English",
            "hi" to "हिन्दी",
            "hng" to "Hinglish",
            "hu" to "Magyar",
            "in" to "Bahasa Indonesia",
            "it" to "Italiano",
            "iw" to "עברית",
            "ja" to "日本語",
            "ko" to "한국어",
            "lv" to "Latviski",
            "ms" to "Bahasa Melayu",
            "pl" to "Polski",
            "pt" to "Português",
            "pt-rBR" to "Português (Brasil)",
            "ru" to "Русский",
            "sa" to "संस्कृतम्",
            "si" to "සිංහල",
            "sk" to "Slovenčina",
            "sr" to "Српски",
            "ta" to "தமிழ்",
            "th" to "ไทย",
            "tl" to "Filipino",
            "tr" to "Türkçe",
            "uk" to "Українська",
            "vi" to "Tiếng Việt",
            "zh-rCN" to "中文 (简体)",
            "zh-rTW" to "中文 (繁體)"
        )
    }
}