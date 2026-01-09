package de.montagezeit.app.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import de.montagezeit.app.data.local.dao.RouteCacheDao
import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.database.AppDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        // TODO: VOR RELEASE entfernen - fallbackToDestructiveMigration() löscht ALLE Nutzerdaten!
        //
        // PROBLEM: Diese Konfiguration löscht die komplette Datenbank bei Schema-Updates.
        // Akzeptabel für MVP-Entwicklung, aber KATASTROPHAL für Produktivnutzer.
        //
        // LÖSUNG VOR RELEASE:
        // 1. Entferne .fallbackToDestructiveMigration()
        // 2. Implementiere echte Migrationen mit .addMigrations(MIGRATION_X_Y)
        // 3. Siehe AppDatabase.kt für detaillierte Migrations-Anleitung
        //
        // REF: https://developer.android.com/training/data-storage/room/migrating-db-versions
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "montagezeit_database"
        )
            .addMigrations(AppDatabase.MIGRATION_1_2)
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
