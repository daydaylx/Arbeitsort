package de.montagezeit.app.diagnostics

import kotlinx.coroutines.CoroutineScope

interface DiagnosticsBootstrap {
    fun initialize(applicationScope: CoroutineScope)
}
