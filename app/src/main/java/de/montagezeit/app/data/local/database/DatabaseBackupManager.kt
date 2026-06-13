package de.montagezeit.app.data.local.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DatabaseBackupManager {

    private const val BACKUP_DIR = "db_backups"
    private const val MAX_BACKUPS = 3

    fun backupIfVersionMismatch(context: Context, dbName: String, targetVersion: Int) {
        val dbFile = context.getDatabasePath(dbName)
        val currentVersion = dbFile.takeIf { it.exists() }?.let { readVersion(it) }
        if (currentVersion == null || currentVersion >= targetVersion) return

        val backupDir = File(context.filesDir, BACKUP_DIR).also { it.mkdirs() }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val dest = File(backupDir, "v${currentVersion}_${timestamp}.db")

        runCatching { dbFile.copyTo(dest, overwrite = false) }
        listOf("-wal", "-shm").forEach { ext ->
            val companion = File("${dbFile.path}$ext")
            if (companion.exists()) {
                runCatching { companion.copyTo(File("${dest.path}$ext"), overwrite = false) }
            }
        }

        pruneOldBackups(backupDir)
    }

    private fun readVersion(dbFile: File): Int? = runCatching {
        @Suppress("DEPRECATION")
        SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
            .use { it.version }
    }.getOrNull()
    private fun pruneOldBackups(backupDir: File) {
        backupDir.listFiles { f -> f.extension == "db" }
            ?.sortedBy { it.lastModified() }
            ?.dropLast(MAX_BACKUPS)
            ?.forEach { backup ->
                backup.delete()
                File("${backup.path}-wal").delete()
                File("${backup.path}-shm").delete()
            }
    }
}
