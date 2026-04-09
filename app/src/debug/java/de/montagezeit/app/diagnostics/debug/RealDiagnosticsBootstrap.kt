package de.montagezeit.app.diagnostics.debug

import de.montagezeit.app.diagnostics.AppDiagnosticsRuntime
import de.montagezeit.app.diagnostics.DiagnosticsBootstrap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope

@Singleton
class RealDiagnosticsBootstrap @Inject constructor(
    private val diagnosticsController: DebugDiagnosticsController
) : DiagnosticsBootstrap {
    override fun initialize(applicationScope: CoroutineScope) {
        AppDiagnosticsRuntime.install(diagnosticsController)
        diagnosticsController.initialize(applicationScope)
    }
}
