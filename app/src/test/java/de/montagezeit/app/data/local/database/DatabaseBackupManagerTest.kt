package de.montagezeit.app.data.local.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DatabaseBackupManagerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        cleanup()
    }

    @After
    fun tearDown() {
        cleanup()
    }

    @Test
    fun `backupIfVersionMismatch copies pre-migration database when opened read-only`() {
        val dbName = "backup_manager_test.db"
        val dbFile = context.getDatabasePath(dbName)

        SQLiteDatabase.openOrCreateDatabase(dbFile, null).use {
            it.version = 16
        }

        val readOnlyDb = SQLiteDatabase.openDatabase(
            dbFile.absolutePath,
            null,
            SQLiteDatabase.OPEN_READONLY
        )
        try {
            DatabaseBackupManager.backupIfVersionMismatch(
                context = context,
                dbName = dbName,
                targetVersion = AppDatabase.DATABASE_VERSION
            )
        } finally {
            readOnlyDb.close()
        }

        val backups = backupFiles()
        assertEquals(1, backups.size)
        assertTrue(backups.single().name.startsWith("v16_"))
    }

    @Test
    fun `backupIfVersionMismatch skips database at target version`() {
        val dbName = "backup_manager_test.db"
        val dbFile = context.getDatabasePath(dbName)

        SQLiteDatabase.openOrCreateDatabase(dbFile, null).use {
            it.version = AppDatabase.DATABASE_VERSION
        }

        DatabaseBackupManager.backupIfVersionMismatch(
            context = context,
            dbName = dbName,
            targetVersion = AppDatabase.DATABASE_VERSION
        )

        assertTrue(backupFiles().isEmpty())
    }

    private fun backupFiles(): List<File> {
        return File(context.filesDir, "db_backups")
            .listFiles { file -> file.extension == "db" }
            ?.toList()
            ?: emptyList()
    }

    private fun cleanup() {
        context.deleteDatabase("backup_manager_test.db")
        File(context.filesDir, "db_backups").deleteRecursively()
    }
}
