package de.montagezeit.app.diagnostics

import java.time.LocalDate

enum class DiagnosticCategory {
    STATE_MUTATION,
    CALCULATION_RUN,
    VALIDATION,
    REMINDER_DECISION,
    REMINDER_SCHEDULE,
    EXPORT_OPERATION,
    INTEGRITY_SCAN,
    SYSTEM_EVENT
}

enum class DiagnosticPhase {
    START,
    STEP,
    RESULT,
    ANOMALY
}

enum class DiagnosticSeverity {
    DEBUG,
    INFO,
    WARNING,
    ERROR
}

enum class DiagnosticStatus {
    SUCCESS,
    WARNING,
    ERROR,
    CANCELLED
}

data class DiagnosticDateRange(
    val startDate: LocalDate,
    val endDate: LocalDate
)

data class DiagnosticTraceRequest(
    val category: DiagnosticCategory,
    val name: String,
    val sourceClass: String,
    val screenOrWorker: String? = null,
    val entityDate: LocalDate? = null,
    val dateRange: DiagnosticDateRange? = null,
    val payload: Map<String, Any?> = emptyMap()
)

interface DiagnosticTrace {
    val traceId: String
    val sessionId: String

    fun event(
        name: String,
        phase: DiagnosticPhase = DiagnosticPhase.STEP,
        severity: DiagnosticSeverity = DiagnosticSeverity.INFO,
        payload: Map<String, Any?> = emptyMap()
    )

    fun warning(
        code: String,
        payload: Map<String, Any?> = emptyMap(),
        name: String = code
    )

    fun error(
        name: String,
        throwable: Throwable? = null,
        payload: Map<String, Any?> = emptyMap()
    )

    fun finish(
        status: DiagnosticStatus = DiagnosticStatus.SUCCESS,
        payload: Map<String, Any?> = emptyMap()
    )
}

interface AppDiagnostics {
    fun startTrace(request: DiagnosticTraceRequest): DiagnosticTrace
}

object AppDiagnosticsRuntime {
    @Volatile
    private var delegate: AppDiagnostics = NoOpAppDiagnostics

    fun install(appDiagnostics: AppDiagnostics) {
        delegate = appDiagnostics
    }

    fun startTrace(request: DiagnosticTraceRequest): DiagnosticTrace = delegate.startTrace(request)
}

private object NoOpAppDiagnostics : AppDiagnostics {
    override fun startTrace(request: DiagnosticTraceRequest): DiagnosticTrace = NoOpDiagnosticTrace
}

private object NoOpDiagnosticTrace : DiagnosticTrace {
    override val traceId: String = "noop"
    override val sessionId: String = "noop"

    override fun event(
        name: String,
        phase: DiagnosticPhase,
        severity: DiagnosticSeverity,
        payload: Map<String, Any?>
    ) = Unit

    override fun warning(code: String, payload: Map<String, Any?>, name: String) = Unit

    override fun error(name: String, throwable: Throwable?, payload: Map<String, Any?>) = Unit

    override fun finish(status: DiagnosticStatus, payload: Map<String, Any?>) = Unit
}
