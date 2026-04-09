package de.montagezeit.app.diagnostics.release

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import de.montagezeit.app.diagnostics.DiagnosticsBootstrap
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DiagnosticsReleaseModule {
    @Binds
    @Singleton
    abstract fun bindDiagnosticsBootstrap(impl: ReleaseDiagnosticsBootstrap): DiagnosticsBootstrap
}
