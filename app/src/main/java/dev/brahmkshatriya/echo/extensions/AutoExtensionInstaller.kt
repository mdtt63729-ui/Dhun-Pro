package dev.brahmkshatriya.echo.extensions

import android.content.Context
import android.content.SharedPreferences
import dev.brahmkshatriya.echo.extensions.repo.ExtensionParser
import dev.brahmkshatriya.echo.extensions.repo.FileRepository.Companion.getExtensionsFileDir
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object AutoExtensionInstaller {

    const val PREF_KEY = "auto_extensions_installed"
    const val ASSETS_DIR = "bundled-extensions"

    val bundledExtensions = listOf(
        "Youtube_music-057f37c.eapk",
        "sp-b4c9ec7.eapk",
        "yt-c2c45d2.eapk",
        "Groove_music-fec592e_1.eapk",
        "anikoto-9f8c9eb.eapk",
        "echo_combine-18b5da3.eapk",
        "applemusic_lyrics-v1.3.0_1.eapk",
        "khinsider-v1.5.4-dev.3.eapk",
        "echodown-arm64-v8a-release.eapk",
        "soundcloud-c01dabe.eapk",
        "kisskh-66f64b0.eapk",
        "ex-31a7490.eapk",
        "lrclib_lyrics-c11ffb5_1.eapk",
        "anidb-8e3f0a7.eapk",
        "artistgrid-251873c.eapk",
        "musixmatch_lyrics-a9523dc.eapk",
    )

    data class InstallProgress(
        val current: Int,
        val total: Int,
        val name: String,
        val isDone: Boolean,
        val error: String? = null,
    )

    fun isAlreadyInstalled(settings: SharedPreferences): Boolean {
        return settings.getBoolean(PREF_KEY, false)
    }

    fun markComplete(settings: SharedPreferences) {
        settings.edit().putBoolean(PREF_KEY, true).apply()
    }

    private fun getBundledExtensionList(context: Context): List<String> {
        if (bundledExtensions.isNotEmpty()) return bundledExtensions
        return try {
            val files = context.assets.list(ASSETS_DIR) ?: emptyArray()
            files.filter { it.endsWith(".apk") || it.endsWith(".eapk") }.toList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun copyAssetToTemp(
        context: Context,
        assetName: String,
    ): File = withContext(Dispatchers.IO) {
        val tempFile = File(context.cacheDir, "ext_temp_${System.currentTimeMillis()}.apk")
        tempFile.delete()
        context.assets.open("$ASSETS_DIR/$assetName").use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }
        tempFile
    }

    private fun isExtensionInstalled(context: Context, extensionId: String): Boolean {
        val dir = context.getExtensionsFileDir()
        return File(dir, "$extensionId.apk").exists() ||
               File(dir, "$extensionId.eapk").exists()
    }

    private fun parseExtensionId(context: Context, apkFile: File): String? {
        return try {
            val pkgInfo = context.packageManager.getPackageArchiveInfo(
                apkFile.absolutePath,
                ExtensionParser.PACKAGE_FLAGS
            )
            pkgInfo?.packageName
        } catch (e: Exception) {
            null
        }
    }

    suspend fun copyBundledToTempFiles(context: Context): List<File> {
        val assetList = getBundledExtensionList(context)
        if (assetList.isEmpty()) return emptyList()

        val files = mutableListOf<File>()
        for (assetName in assetList) {
            try {
                val tempFile = copyAssetToTemp(context, assetName)
                val extensionId = parseExtensionId(context, tempFile)
                if (extensionId == null) {
                    tempFile.delete()
                    continue
                }
                if (isExtensionInstalled(context, extensionId)) {
                    tempFile.delete()
                    continue
                }
                files.add(tempFile)
            } catch (e: Exception) {
                // Skip this extension on error
            }
        }
        return files
    }

    suspend fun installAll(
        context: Context,
        settings: SharedPreferences,
        fileIgnoreFlow: MutableSharedFlow<File?>,
        onProgress: (InstallProgress) -> Unit,
    ): List<String> {
        val assetList = getBundledExtensionList(context)
        if (assetList.isEmpty()) {
            onProgress(InstallProgress(0, 0, "No bundled extensions found", true))
            markComplete(settings)
            return emptyList()
        }

        val installed = mutableListOf<String>()
        val total = assetList.size
        var errors: String? = null

        for ((index, assetName) in assetList.withIndex()) {
            val displayNum = index + 1
            onProgress(InstallProgress(displayNum, total, assetName, false))

            try {
                val tempFile = copyAssetToTemp(context, assetName)
                val extensionId = parseExtensionId(context, tempFile)
                if (extensionId == null) {
                    onProgress(InstallProgress(displayNum, total, assetName, false,
                        error = "Failed to parse extension ID"))
                    tempFile.delete()
                    continue
                }

                if (isExtensionInstalled(context, extensionId)) {
                    onProgress(InstallProgress(displayNum, total, "$assetName (already installed)", false))
                    tempFile.delete()
                    continue
                }

                InstallationUtils.installFile(context, fileIgnoreFlow, extensionId, tempFile)
                installed.add(assetName)

                onProgress(InstallProgress(displayNum, total, "$assetName \u2713", false))
            } catch (e: Exception) {
                errors = errors?.let { "$it\n" } ?: "" + "${assetName}: ${e.message}\n"
                onProgress(InstallProgress(displayNum, total, assetName, false, error = e.message))
            }
        }

        onProgress(InstallProgress(total, total, "Complete", true, error = errors))
        markComplete(settings)
        return installed
    }
}
