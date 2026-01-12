package de.montagezeit.app.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.dao.RouteCacheDao
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
            AppDatabase.DATABASE_NAME
        )
            .addMigrations(*AppDatabase.MIGRATIONS)
            .build()
    }
    
    @Provides
    fun provideWorkEntryDao(database: AppDatabase): WorkEntryDao {
        return database.workEntryDao()
    }
    
    @Provides
    fun provideRouteCacheDao(database: AppDatabase): RouteCacheDao {
        return database.routeCacheDao()
    }
}
