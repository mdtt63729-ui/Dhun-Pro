package dev.brahmkshatriya.echo.utils

import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Saves the last crash so it can be read from inside the app.
 *
 * Without this there is no way to see why the app stopped on a phone where adb isn't
 * available — the system dialog only says "keeps stopping", which says nothing about the cause.
 * The trace is written to the app's private storage and shown on the next launch.
 */
object CrashHandler {

    private fun file(context: Context) = File(context.filesDir, "last_crash.txt")

    fun install(context: Context) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                val writer = StringWriter()
                throwable.printStackTrace(PrintWriter(writer))
                file(context).writeText(
                    buildString {
                        appendLine("Thread: ${thread.name}")
                        appendLine("Android: ${Build.VERSION.SDK_INT} (${Build.VERSION.RELEASE})")
                        appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
                        appendLine("ABI: ${Build.SUPPORTED_ABIS.joinToString()}")
                        appendLine()
                        append(writer.toString())
                    }
                )
            }
            previous?.uncaughtException(thread, throwable)
        }
    }

    fun last(context: Context): String? =
        file(context).takeIf { it.exists() }?.readText()?.takeIf { it.isNotBlank() }

    fun clear(context: Context) {
        runCatching { file(context).delete() }
    }
}
