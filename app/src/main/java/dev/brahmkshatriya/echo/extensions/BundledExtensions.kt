package dev.brahmkshatriya.echo.extensions

import android.content.Context
import dev.brahmkshatriya.echo.extensions.repo.FileRepository.Companion.getExtensionsFileDir
import java.io.File

/**
 * Extensions that ship inside the app and are installed on first launch.
 *
 * Echo loads file-based extensions from `filesDir/extensions/*.apk`, so dropping the APK into
 * `assets/extensions/` and copying it there once is enough for it to be a normal, really
 * installed extension — no manual "Add extension" step, and it survives app updates because an
 * already-installed copy is never overwritten (so a newer version the user installed wins).
 *
 * To bundle another extension (for example JioSaavn), just put its APK in
 * `app/src/main/assets/extensions/` and add its file name to [FILES].
 */
object BundledExtensions {

    private val FILES = listOf(
        "yt.apk",
    )

    fun install(context: Context) = runCatching {
        val dir = context.getExtensionsFileDir()
        // loadAllApks() makes the folder read-only, so open it up before writing.
        dir.setWritable(true)
        FILES.forEach { name ->
            val target = File(dir, name)
            if (target.exists()) return@forEach
            runCatching {
                context.assets.open("extensions/$name").use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                }
            }
        }
        dir.setReadOnly()
    }
}
