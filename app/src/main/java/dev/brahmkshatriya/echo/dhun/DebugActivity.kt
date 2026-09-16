/*
 * Dhun Project Original (2026)
 * Dhun
 * Licensed Under GPL-3.0 | see git history for contributors
 */


package dev.brahmkshatriya.echo.dhun

import android.app.Activity
import android.os.Bundle
import android.widget.ScrollView
import android.widget.TextView
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Simple crash/debug viewer launched from the global exception handler in [App]
 * when an unhandled exception is caught in a debug context.
 */
class DebugActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val stackTrace = intent.getStringExtra(EXTRA_STACK_TRACE) ?: "No stack trace provided."

        val sw = StringWriter()
        sw.write(stackTrace)
        sw.write("\n\n--- Device ---\n")
        sw.write("Android ${android.os.Build.VERSION.RELEASE} (${android.os.Build.VERSION.SDK_INT})\n")
        sw.write("${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}\n")
        sw.write("\n--- App ---\n")
        sw.write("${packageName}\n")
        PrintWriter(sw).use { it.flush() }

        val text = TextView(this).apply {
            setTextIsSelectable(true)
            text = sw.toString()
            textSize = 12f
            setPadding(32, 32, 32, 32)
        }
        setContentView(ScrollView(this).apply { addView(text) })
    }

    companion object {
        const val EXTRA_STACK_TRACE = "dev.brahmkshatriya.echo.dhun.EXTRA_STACK_TRACE"
    }
}
