package de.montagezeit.app.diagnostics.debug

import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import de.montagezeit.app.diagnostics.DiagnosticsBootstrap
import de.montagezeit.app.diagnostics.debug.data.DebugDiagnosticsDao
import de.montagezeit.app.diagnostics.debug.data.DebugDiagnosticsDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DiagnosticsDebugBindingsModule {
    @Binds
    @Singleton
    abstract fun bindDiagnosticsBootstrap(impl: RealDiagnosticsBootstrap): DiagnosticsBootstrap
}

@Module
@InstallIn(SingletonComponent::class)
object DiagnosticsDebugModule {
    @Provides
    @Singleton
    fun provideDebugDiagnosticsDatabase(
        @ApplicationContext context: Context
    ): DebugDiagnosticsDatabase {
        return Room.databaseBuilder(
            context,
            DebugDiagnosticsDatabase::class.java,
            DebugDiagnosticsDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration(true)
            .build()
    }

    @Provides
    fun provideDebugDiagnosticsDao(database: DebugDiagnosticsDatabase): DebugDiagnosticsDao {
        return database.diagnosticsDao()
    }
}
