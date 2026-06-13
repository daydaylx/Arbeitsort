package de.montagezeit.app.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.database.AppDatabase
import de.montagezeit.app.data.local.database.DatabaseBackupManager
import de.montagezeit.app.data.repository.RoomWorkEntryRepository
import de.montagezeit.app.data.repository.WorkEntryRepository
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val backupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        val database = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .addMigrations(*AppDatabase.MIGRATIONS)
            .build()

        backupScope.launch {
            DatabaseBackupManager.backupIfVersionMismatch(
                context,
                AppDatabase.DATABASE_NAME,
                AppDatabase.DATABASE_VERSION
            )
        }

        return database
    }

    @Provides
    fun provideWorkEntryDao(database: AppDatabase): WorkEntryDao {
        return database.workEntryDao()
    }

    @Provides
    @Singleton
    fun provideWorkEntryRepository(workEntryDao: WorkEntryDao): WorkEntryRepository {
        return RoomWorkEntryRepository(workEntryDao)
    }
}
