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
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "montagezeit_database"
        )
            .addMigrations(*AppDatabase.MIGRATIONS)
            .build()
    }
    
    @Provides
    fun provideWorkEntryDao(database: AppDatabase): WorkEntryDao {
        return database.workEntryDao()
    }
}
