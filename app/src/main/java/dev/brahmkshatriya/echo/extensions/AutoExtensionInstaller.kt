package dev.brahmkshatriya.echo.extensions

import android.content.Context
import android.content.SharedPreferences
import dev.brahmkshatriya.echo.extensions.repo.ExtensionParser
import dev.brahmkshatriya.echo.extensions.repo.FileRepository.Companion.getExtensionsFileDir
import dev.brahmkshatriya.echo.extensions.InstallationUtils.installFile
import dev.brahmkshatriya.echo.utils.AppUpdater.downloadUpdate
import dev.brahmkshatriya.echo.utils.AppUpdater.getUpdateFileUrl
import dev.brahmkshatriya.echo.utils.Serializer.toData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

/**
 * First-launch extension bootstrapper.
 *
 * Dhun ships only YouTube Music and Saavn inside the APK. The remaining
 * extensions come from the public Echo extension catalog and are downloaded
 * one-by-one after the first splash screen. The user sees one progress popup
 * for the complete operation.
 */
object AutoExtensionInstaller {

    const val PREF_KEY = "auto_extensions_installed"
    const val ASSETS_DIR = "bundled-extensions"
    const val SETUP_COMPLETE_KEY = "auto_extensions_setup_complete_v2"

    private const val REMOTE_CATALOG_URL =
        "https://raw.githubusercontent.com/itsmechinmoy/echo-extensions/main/echo_extensions.json"

    /** Only these two are physically shipped with Dhun. */
    val bundledExtensions = listOf(
        "Youtube_music-057f37c.eapk",
        "yt-c2c45d2.eapk",
    )

    /** Never download another copy of the two bundled music sources. */
    private val bundledIds = setOf("Youtube_music", "saavn_music")

    @Serializable
    data class RemoteExtension(
        val id: String,
        val name: String = id,
        val updateUrl: String,
    )

    data class InstallProgress(
        val current: Int,
        val total: Int,
        val name: String,
        val isDone: Boolean,
        val error: String? = null,
    )

    private sealed interface InstallTask {
        data class Bundled(val assetName: String) : InstallTask
        data class Remote(val extension: RemoteExtension) : InstallTask
    }

    fun isAlreadyInstalled(settings: SharedPreferences): Boolean =
        settings.getBoolean(PREF_KEY, false)

    fun isSetupComplete(settings: SharedPreferences): Boolean =
        settings.getBoolean(SETUP_COMPLETE_KEY, false)

    fun markComplete(settings: SharedPreferences) {
        settings.edit()
            .putBoolean(PREF_KEY, true)
            .putBoolean(SETUP_COMPLETE_KEY, true)
            .apply()
    }

    fun clearSetupComplete(settings: SharedPreferences) {
        settings.edit().putBoolean(SETUP_COMPLETE_KEY, false).apply()
    }

    private suspend fun fetchRemoteCatalog(): List<RemoteExtension> = withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(REMOTE_CATALOG_URL)
            .header("Accept", "application/json")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("Extension catalog HTTP ${response.code}")
            }
            response.body?.string().orEmpty()
                .toData<List<RemoteExtension>>()
                .getOrThrow()
        }
    }

    private fun getBundledExtensionList(context: Context): List<String> {
        if (bundledExtensions.isNotEmpty()) return bundledExtensions
        return runCatching {
            context.assets.list(ASSETS_DIR)
                ?.filter { it.endsWith(".apk") || it.endsWith(".eapk") }
                .orEmpty()
        }.getOrDefault(emptyList())
    }

    private suspend fun copyAssetToTemp(context: Context, assetName: String): File =
        withContext(Dispatchers.IO) {
            val tempFile = File(context.cacheDir, "ext_temp_${System.currentTimeMillis()}.apk")
            tempFile.delete()
            context.assets.open("$ASSETS_DIR/$assetName").use { input ->
                FileOutputStream(tempFile).use { output -> input.copyTo(output) }
            }
            tempFile
        }

    private fun parseExtensionId(context: Context, apkFile: File): String? = runCatching {
        context.packageManager.getPackageArchiveInfo(
            apkFile.absolutePath,
            ExtensionParser.PACKAGE_FLAGS
        )?.packageName
    }.getOrNull()

    private fun isExtensionInstalled(context: Context, extensionId: String): Boolean {
        val dir = context.getExtensionsFileDir()
        return File(dir, "$extensionId.apk").exists() ||
            File(dir, "$extensionId.eapk").exists()
    }

    private suspend fun buildInstallQueue(context: Context): List<InstallTask> {
        val tasks = mutableListOf<InstallTask>()

        // Bundled sources are always first so the core music sources become
        // available immediately, before the larger remote catalog downloads.
        for (asset in getBundledExtensionList(context)) {
            val temp = runCatching { copyAssetToTemp(context, asset) }.getOrNull() ?: continue
            val id = parseExtensionId(context, temp)
            temp.delete()
            if (id != null && !isExtensionInstalled(context, id)) {
                tasks += InstallTask.Bundled(asset)
            }
        }

        // Everything else comes from the remote catalog. YouTube Music and
        // Saavn are skipped here because their .eapk files are already bundled.
        val catalog = fetchRemoteCatalog()
        catalog
            .filter { it.id !in bundledIds }
            .distinctBy { it.id }
            .forEach { item ->
                if (!isExtensionInstalled(context, item.id)) {
                    tasks += InstallTask.Remote(item)
                }
            }

        return tasks
    }

    /**
     * Installs the complete catalog sequentially and reports every step to the
     * single first-launch popup. Errors are isolated so one bad extension does
     * not prevent the remaining extensions from being attempted.
     */
    suspend fun installAll(
        context: Context,
        settings: SharedPreferences,
        fileIgnoreFlow: MutableSharedFlow<File?>,
        onProgress: (InstallProgress) -> Unit,
    ): Boolean {
        val tasks = try {
            buildInstallQueue(context)
        } catch (e: Exception) {
            onProgress(
                InstallProgress(
                    current = 0,
                    total = 0,
                    name = "Could not load extension catalog",
                    isDone = true,
                    error = e.message ?: "Network error",
                )
            )
            return false
        }

        if (tasks.isEmpty()) {
            onProgress(InstallProgress(0, 0, "All extensions are already installed", true))
            markComplete(settings)
            return true
        }

        val client = OkHttpClient()
        val total = tasks.size
        var successCount = 0
        val errors = mutableListOf<String>()

        tasks.forEachIndexed { index, task ->
            val current = index + 1
            val displayName = when (task) {
                is InstallTask.Bundled -> task.assetName
                    .removeSuffix(".eapk")
                    .removeSuffix(".apk")
                    .replace('_', ' ')
                is InstallTask.Remote -> task.extension.name
            }
            onProgress(InstallProgress(current, total, displayName, false))

            runCatching {
                when (task) {
                    is InstallTask.Bundled -> {
                        val tempFile = copyAssetToTemp(context, task.assetName)
                        try {
                            val id = parseExtensionId(context, tempFile)
                                ?: error("Could not read extension ID")
                            if (!isExtensionInstalled(context, id)) {
                                installFile(context, fileIgnoreFlow, id, tempFile)
                            }
                        } finally {
                            tempFile.delete()
                        }
                    }

                    is InstallTask.Remote -> {
                        val downloadUrl = getUpdateFileUrl("", task.extension.updateUrl, client)
                            .getOrThrow()
                            ?: error("No downloadable release found")
                        val file = downloadUpdate(context, downloadUrl, client)
                            .getOrThrow()
                            ?: error("Download returned no file")
                        try {
                            installFile(context, fileIgnoreFlow, task.extension.id, file)
                        } finally {
                            file.delete()
                        }
                    }
                }
            }.onSuccess {
                successCount++
                onProgress(InstallProgress(current, total, "$displayName ✓", false))
            }.onFailure { error ->
                errors += "$displayName: ${error.message ?: "Installation failed"}"
                onProgress(
                    InstallProgress(
                        current,
                        total,
                        displayName,
                        false,
                        error = error.message ?: "Installation failed",
                    )
                )
            }
        }

        val errorText = errors.takeIf { it.isNotEmpty() }?.joinToString("\n")
        onProgress(
            InstallProgress(
                current = total,
                total = total,
                name = "$successCount/$total extensions installed",
                isDone = true,
                error = errorText,
            )
        )

        // Only suppress the first-launch installer after the complete queue has
        // succeeded. A failed network/download can therefore retry on next launch.
        if (errors.isEmpty()) {
            markComplete(settings)
        }
        return errors.isEmpty()
    }

    /** Compatibility helper used by older startup code. */
    suspend fun copyBundledToTempFiles(context: Context): List<File> {
        val files = mutableListOf<File>()
        for (assetName in getBundledExtensionList(context)) {
            runCatching {
                val file = copyAssetToTemp(context, assetName)
                val id = parseExtensionId(context, file)
                if (id != null && !isExtensionInstalled(context, id)) files += file
                else file.delete()
            }
        }
        return files
    }
}
