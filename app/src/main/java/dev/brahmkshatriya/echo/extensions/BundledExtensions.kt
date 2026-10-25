package dev.brahmkshatriya.echo.extensions

import android.content.Context
import dev.brahmkshatriya.echo.extensions.repo.FileRepository.Companion.getExtensionsFileDir
import java.io.File

/**
 * Extensions that ship inside the app and are installed on first launch.
 *
 * Echo loads file-based extensions from any APK file in its `extensions` folder, so putting
 * the APK in `assets/extensions` and copying it there once is enough for it to be a normal,
 * really installed extension — no manual "Add extension" step, and it survives app updates
 * because an already-installed copy is never overwritten (so a newer install wins).
 *
 * Both bundled extensions are built with minSdk 24, so they work on Android 7.
 */
object BundledExtensions {

    private val FILES = listOf(
        "yt.apk",      // YouTube / YouTube Music
        "saavn.apk",   // JioSaavn
    )

    /** @return true when at least one extension was copied, so the loader can be told to reload. */
    fun install(context: Context): Boolean = runCatching {
        val dir = context.getExtensionsFileDir()
        // loadAllApks() makes the folder read-only, so open it up before writing.
        dir.setWritable(true)
        var copied = false
        FILES.forEach { name ->
            val target = File(dir, name)
            if (target.exists()) return@forEach
            runCatching {
                context.assets.open("extensions/$name").use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                }
                copied = true
            }
        }
        dir.setReadOnly()
        copied
    }.getOrDefault(false)
}
