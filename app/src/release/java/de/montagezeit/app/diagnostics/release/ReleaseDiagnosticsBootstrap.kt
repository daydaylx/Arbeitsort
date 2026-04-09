package de.montagezeit.app.diagnostics.release

import de.montagezeit.app.diagnostics.DiagnosticsBootstrap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope

@Singleton
class ReleaseDiagnosticsBootstrap @Inject constructor() : DiagnosticsBootstrap {
    override fun initialize(applicationScope: CoroutineScope) = Unit
}
