package dev.brahmkshatriya.echo.utils

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.io.File

/**
 * Added from ArchiveTune: writes a settings backup on a schedule.
 *
 * The file lands in the app's own external files directory so no storage permission or
 * document picker is needed — `Android/data/<package>/files/backups/echo-settings.json`.
 */
class SettingsBackupWorker(
    context: Context,
    params: WorkerParameters,
) : Worker(context, params) {

    override fun doWork(): Result = runCatching {
        val dir = File(applicationContext.getExternalFilesDir(null), "backups")
        dir.mkdirs()
        File(dir, BACKUP_FILE_NAME).writeText(applicationContext.settingsBackupJson())
        Result.success()
    }.getOrElse { Result.failure() }

    companion object {
        const val BACKUP_FILE_NAME = "echo-settings.json"
    }
}
