package dev.brahmkshatriya.echo.utils

import android.content.Context
import android.os.Build
import android.os.Environment
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Records crashes to files that can be read on the phone itself, without adb.
 *
 * Written to three places so at least one is reachable:
 *   /storage/emulated/0/echo/echo_crash.txt                       (the folder the user asked for)
 *   /storage/emulated/0/Android/data/<pkg>/files/echo/echo_crash.txt   (no permission needed)
 *   the app's private storage, which is also shown in a dialog on the next launch
 *
 * A start marker is written on every launch too: if that file exists but no crash file does,
 * the process died somewhere the handler could not run.
 */
object CrashHandler {

    private const val CRASH_FILE = "echo_crash.txt"
    private const val START_FILE = "echo_last_start.txt"

    private fun privateFile(context: Context) = File(context.filesDir, CRASH_FILE)

    private fun writableDirs(context: Context): List<File> {
        val dirs = mutableListOf<File>()
        // The folder the user asked for. Only writable once storage permission has been granted.
        runCatching {
            @Suppress("DEPRECATION")
            val root: File? = Environment.getExternalStorageDirectory()
            if (root != null) dirs.add(File(root, "echo"))
        }
        // Always writable without any permission, and still visible in a file manager.
        runCatching {
            context.getExternalFilesDir(null)?.let { dirs.add(File(it, "echo")) }
        }
        return dirs
    }

    private fun writeEverywhere(context: Context, name: String, text: String) {
        runCatching {
            privateFile(context).writeText(text)
        }
        writableDirs(context).forEach { dir ->
            runCatching {
                if (!dir.exists()) dir.mkdirs()
                File(dir, name).writeText(text)
            }
        }
    }

    private fun header(context: Context): String = buildString {
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        appendLine("Time: $time")
        appendLine("Android: ${Build.VERSION.SDK_INT} (${Build.VERSION.RELEASE})")
        appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
        appendLine("ABI: ${Build.SUPPORTED_ABIS.joinToString()}")
        appendLine("App: ${context.packageName}")
    }

    /** Call first thing in Application.onCreate, so even a very early crash leaves a trace. */
    fun install(context: Context) {
        runCatching {
            writeEverywhere(context, START_FILE, header(context) + "\nThe app reached Application.onCreate.")
        }
        runCatching {
            val previous = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                runCatching {
                    val writer = StringWriter()
                    throwable.printStackTrace(PrintWriter(writer))
                    writeEverywhere(
                        context,
                        CRASH_FILE,
                        header(context) + "\nThread: ${thread.name}\n\n" + writer.toString()
                    )
                }
                previous?.uncaughtException(thread, throwable)
            }
        }
    }

    fun last(context: Context): String? =
        runCatching { privateFile(context).takeIf { it.exists() }?.readText() }
            .getOrNull()?.takeIf { it.isNotBlank() }

    fun clear(context: Context) {
        runCatching { privateFile(context).delete() }
        writableDirs(context).forEach { dir ->
            runCatching { File(dir, CRASH_FILE).delete() }
        }
    }
}
