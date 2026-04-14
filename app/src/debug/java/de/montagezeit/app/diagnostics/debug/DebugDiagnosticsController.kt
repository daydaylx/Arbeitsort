package de.montagezeit.app.diagnostics.debug

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import de.montagezeit.app.BuildConfig
import de.montagezeit.app.diagnostics.AppDiagnostics
import de.montagezeit.app.diagnostics.DiagnosticCategory
import de.montagezeit.app.diagnostics.DiagnosticPhase
import de.montagezeit.app.diagnostics.DiagnosticSeverity
import de.montagezeit.app.diagnostics.DiagnosticStatus
import de.montagezeit.app.diagnostics.DiagnosticTrace
import de.montagezeit.app.diagnostics.DiagnosticTraceRequest
import de.montagezeit.app.diagnostics.toDiagnosticPayload
import de.montagezeit.app.diagnostics.debug.data.DebugDiagnosticsDao
import de.montagezeit.app.diagnostics.debug.data.DebugDiagnosticsDatabase
import de.montagezeit.app.diagnostics.debug.data.DiagnosticEventEntity
import de.montagezeit.app.diagnostics.debug.data.DiagnosticSessionEntity
import de.montagezeit.app.diagnostics.debug.data.DiagnosticTraceEntity
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

private const val PAYLOAD_VERSION = 1
private const val TRACE_STATUS_RUNNING = "RUNNING"
private const val RETENTION_DAYS = 30L
private const val MAX_TRACES = 2_000
private const val MAX_EVENTS = 50_000
private const val MAX_DB_BYTES = 100L * 1024L * 1024L
private const val PRUNE_AFTER_COMPLETED_TRACES = 25

data class DiagnosticSummary(
    val sessionId: String,
    val sessionStartedAtEpochMs: Long,
    val totalTraces: Int,
    val warningTraces: Int,
    val errorTraces: Int,
    val totalEvents: Int,
    val currentSessionTraces: Int
)

@Singleton
class DebugDiagnosticsController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val diagnosticsDao: DebugDiagnosticsDao
) : AppDiagnostics {

    private val writeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))
    private val currentSession = MutableStateFlow<DiagnosticSessionEntity?>(null)
    private val completedSincePrune = AtomicInteger(0)

    fun initialize(applicationScope: CoroutineScope) {
        if (currentSession.value != null) return

        val session = DiagnosticSessionEntity(
            sessionId = UUID.randomUUID().toString(),
            startedAtEpochMs = System.currentTimeMillis(),
            appVersion = BuildConfig.VERSION_NAME,
            buildType = BuildConfig.BUILD_TYPE,
            timezoneId = ZoneId.systemDefault().id
        )
        currentSession.value = session
        applicationScope.launch(Dispatchers.IO) {
            diagnosticsDao.upsertSession(session)
            pruneStorage()
        }
    }

    override fun startTrace(request: DiagnosticTraceRequest): DiagnosticTrace {
        val session = currentSession.value ?: DiagnosticSessionEntity(
            sessionId = UUID.randomUUID().toString(),
            startedAtEpochMs = System.currentTimeMillis(),
            appVersion = BuildConfig.VERSION_NAME,
            buildType = BuildConfig.BUILD_TYPE,
            timezoneId = ZoneId.systemDefault().id
        ).also { session ->
            currentSession.value = session
            writeScope.launch {
                diagnosticsDao.upsertSession(session)
            }
        }
        return StoredDiagnosticTrace(
            controller = this,
            session = session,
            request = request
        )
    }

    fun observeSummary(): Flow<DiagnosticSummary> {
        return combine(
            currentSession.filterNotNull(),
            diagnosticsDao.observeTraces(),
            diagnosticsDao.observeEventCount()
        ) { session, traces, eventCount ->
            DiagnosticSummary(
                sessionId = session.sessionId,
                sessionStartedAtEpochMs = session.startedAtEpochMs,
                totalTraces = traces.size,
                warningTraces = traces.count { it.warningCount > 0 },
                errorTraces = traces.count { it.errorCount > 0 || it.status == DiagnosticStatus.ERROR.name },
                totalEvents = eventCount,
                currentSessionTraces = traces.count { it.sessionId == session.sessionId }
            )
        }
    }

    fun observeTraces(): Flow<List<DiagnosticTraceEntity>> = diagnosticsDao.observeTraces()

    fun observeTrace(traceId: String): Flow<DiagnosticTraceEntity?> = diagnosticsDao.observeTrace(traceId)

    fun observeEvents(traceId: String): Flow<List<DiagnosticEventEntity>> = diagnosticsDao.observeEvents(traceId)

    fun currentSessionIdFlow(): Flow<String> = currentSession.filterNotNull().map { it.sessionId }

    suspend fun clearAll() {
        withContext(Dispatchers.IO) {
            diagnosticsDao.clearEvents()
            diagnosticsDao.clearTraces()
            diagnosticsDao.clearSessions()
            val freshSession = DiagnosticSessionEntity(
                sessionId = UUID.randomUUID().toString(),
                startedAtEpochMs = System.currentTimeMillis(),
                appVersion = BuildConfig.VERSION_NAME,
                buildType = BuildConfig.BUILD_TYPE,
                timezoneId = ZoneId.systemDefault().id
            )
            currentSession.value = freshSession
            diagnosticsDao.upsertSession(freshSession)
        }
    }

    suspend fun exportBundle(traceIds: List<String>, prefix: String = "diagnostics"): Uri? {
        if (traceIds.isEmpty()) return null
        return withContext(Dispatchers.IO) {
            val traces = diagnosticsDao.getAllTracesNewestFirst().filter { it.traceId in traceIds }
            if (traces.isEmpty()) {
                return@withContext null
            }
            val events = diagnosticsDao.getEventsForTraces(traces.map { it.traceId })
            val exportDir = File(context.cacheDir, "exports/diagnostics").apply { mkdirs() }
            val timestamp = Instant.ofEpochMilli(System.currentTimeMillis()).toString().replace(':', '-')
            val bundleFile = File(exportDir, "${prefix}_${timestamp}.zip")

            ZipOutputStream(bundleFile.outputStream().buffered()).use { zip ->
                writeZipEntry(
                    zip = zip,
                    name = "manifest.json",
                    content = JSONObject()
                        .put("exportedAtEpochMs", System.currentTimeMillis())
                        .put("buildType", BuildConfig.BUILD_TYPE)
                        .put("appVersion", BuildConfig.VERSION_NAME)
                        .put("traceCount", traces.size)
                        .put("eventCount", events.size)
                        .put("sessionIds", JSONArray(traces.map { it.sessionId }.distinct()))
                        .toString(2)
                )
                writeZipEntry(
                    zip = zip,
                    name = "traces.json",
                    content = JSONArray().apply {
                        traces.forEach { trace ->
                            put(
                                JSONObject()
                                    .put("traceId", trace.traceId)
                                    .put("sessionId", trace.sessionId)
                                    .put("rootSpanId", trace.rootSpanId)
                                    .put("category", trace.category)
                                    .put("name", trace.name)
                                    .put("status", trace.status)
                                    .put("severity", trace.severity)
                                    .put("startedAtEpochMs", trace.startedAtEpochMs)
                                    .put("completedAtEpochMs", trace.completedAtEpochMs)
                                    .put("durationMs", trace.durationMs)
                                    .put("sourceClass", trace.sourceClass)
                                    .put("screenOrWorker", trace.screenOrWorker)
                                    .put("entityDate", trace.entityDate)
                                    .put("rangeStart", trace.rangeStart)
                                    .put("rangeEnd", trace.rangeEnd)
                                    .put("payloadVersion", trace.payloadVersion)
                                    .put("rootPayloadJson", JSONObject(trace.rootPayloadJson))
                                    .put("resultPayloadJson", trace.resultPayloadJson?.let(::JSONObject))
                                    .put("warningCount", trace.warningCount)
                                    .put("errorCount", trace.errorCount)
                                    .put("eventCount", trace.eventCount)
                                    .put("firstWarningCode", trace.firstWarningCode)
                                    .put("warningFingerprint", trace.warningFingerprint)
                                    .put("appVersion", trace.appVersion)
                                    .put("buildType", trace.buildType)
                            )
                        }
                    }.toString(2)
                )
                writeZipEntry(
                    zip = zip,
                    name = "events.json",
                    content = JSONArray().apply {
                        events.forEach { event ->
                            put(
                                JSONObject()
                                    .put("eventId", event.eventId)
                                    .put("traceId", event.traceId)
                                    .put("sessionId", event.sessionId)
                                    .put("spanId", event.spanId)
                                    .put("parentSpanId", event.parentSpanId)
                                    .put("seq", event.seq)
                                    .put("category", event.category)
                                    .put("name", event.name)
                                    .put("phase", event.phase)
                                    .put("severity", event.severity)
                                    .put("createdAtEpochMs", event.createdAtEpochMs)
                                    .put("payloadVersion", event.payloadVersion)
                                    .put("warningCode", event.warningCode)
                                    .put("payloadJson", JSONObject(event.payloadJson))
                                    .put("sourceClass", event.sourceClass)
                                    .put("screenOrWorker", event.screenOrWorker)
                                    .put("entityDate", event.entityDate)
                                    .put("rangeStart", event.rangeStart)
                                    .put("rangeEnd", event.rangeEnd)
                            )
                        }
                    }.toString(2)
                )
            }

            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                bundleFile
            )
        }
    }

    internal fun enqueueTraceStart(entity: DiagnosticTraceEntity) {
        writeScope.launch {
            diagnosticsDao.upsertTrace(entity)
        }
    }

    internal fun enqueueTraceFinish(entity: DiagnosticTraceEntity) {
        writeScope.launch {
            diagnosticsDao.upsertTrace(entity)
            if (completedSincePrune.incrementAndGet() >= PRUNE_AFTER_COMPLETED_TRACES) {
                completedSincePrune.set(0)
                pruneStorage()
            }
        }
    }

    internal fun enqueueEvent(entity: DiagnosticEventEntity) {
        writeScope.launch {
            try {
                diagnosticsDao.insertEvent(entity)
            } catch (e: Exception) {
                android.util.Log.e("Diagnostics", "Failed to insert event ${entity.eventId}: ${e.message}")
            }
        }
    }

    private suspend fun pruneStorage() {
        val sessionId = currentSession.value?.sessionId
        val allTraces = diagnosticsDao.getAllTracesOldestFirst()
        if (allTraces.isEmpty()) return

        val deleteIds = linkedSetOf<String>()
        val candidates = allTraces.filterNot { it.sessionId == sessionId || it.status == TRACE_STATUS_RUNNING }
        val ageCutoff = System.currentTimeMillis() - RETENTION_DAYS * 24L * 60L * 60L * 1000L
        deleteIds += candidates
            .filter { (it.completedAtEpochMs ?: it.startedAtEpochMs) < ageCutoff }
            .map { it.traceId }

        val remainingAfterAge = allTraces.filterNot { it.traceId in deleteIds }
        if (remainingAfterAge.size > MAX_TRACES) {
            val overflow = remainingAfterAge.size - MAX_TRACES
            deleteIds += remainingAfterAge
                .filterNot { it.sessionId == sessionId || it.status == TRACE_STATUS_RUNNING }
                .take(overflow)
                .map { it.traceId }
        }

        val remainingAfterCount = allTraces.filterNot { it.traceId in deleteIds }
        val eventCount = remainingAfterCount.sumOf { it.eventCount }
        if (eventCount > MAX_EVENTS) {
            var overflow = eventCount - MAX_EVENTS
            remainingAfterCount
                .filterNot { it.sessionId == sessionId || it.status == TRACE_STATUS_RUNNING }
                .forEach { trace ->
                    if (overflow > 0) {
                        deleteIds += trace.traceId
                        overflow -= max(trace.eventCount, 1)
                    }
                }
        }

        val databaseFile = context.getDatabasePath(DebugDiagnosticsDatabase.DATABASE_NAME)
        if (databaseFile.exists() && databaseFile.length() > MAX_DB_BYTES) {
            deleteIds += allTraces
                .filterNot { it.traceId in deleteIds }
                .filterNot { it.sessionId == sessionId || it.status == TRACE_STATUS_RUNNING }
                .take((allTraces.size * 0.25f).toInt().coerceAtLeast(1))
                .map { it.traceId }
        }

        if (deleteIds.isNotEmpty()) {
            diagnosticsDao.deleteTraces(deleteIds.toList())
        }
    }

    private fun writeZipEntry(zip: ZipOutputStream, name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray())
        zip.closeEntry()
    }
}

private class StoredDiagnosticTrace(
    private val controller: DebugDiagnosticsController,
    private val session: DiagnosticSessionEntity,
    private val request: DiagnosticTraceRequest
) : DiagnosticTrace {

    override val traceId: String = UUID.randomUUID().toString()
    override val sessionId: String = session.sessionId

    private val rootSpanId: String = UUID.randomUUID().toString()
    private val startedAtEpochMs = System.currentTimeMillis()
    private val lock = Any()
    private var seq = 0
    private var warningCount = 0
    private var errorCount = 0
    private var eventCount = 0
    private var firstWarningCode: String? = null
    private val warningCodes = linkedSetOf<String>()
    private var finished = false

    init {
        controller.enqueueTraceStart(
            DiagnosticTraceEntity(
                traceId = traceId,
                sessionId = sessionId,
                rootSpanId = rootSpanId,
                category = request.category.name,
                name = request.name,
                status = TRACE_STATUS_RUNNING,
                severity = DiagnosticSeverity.INFO.name,
                startedAtEpochMs = startedAtEpochMs,
                completedAtEpochMs = null,
                durationMs = null,
                sourceClass = request.sourceClass,
                screenOrWorker = request.screenOrWorker,
                entityDate = request.entityDate?.toString(),
                rangeStart = request.dateRange?.startDate?.toString(),
                rangeEnd = request.dateRange?.endDate?.toString(),
                payloadVersion = PAYLOAD_VERSION,
                rootPayloadJson = DebugDiagnosticsJson.encode(request.payload),
                resultPayloadJson = null,
                warningCount = 0,
                errorCount = 0,
                eventCount = 0,
                firstWarningCode = null,
                warningFingerprint = null,
                appVersion = session.appVersion,
                buildType = session.buildType
            )
        )
    }

    override fun event(
        name: String,
        phase: DiagnosticPhase,
        severity: DiagnosticSeverity,
        payload: Map<String, Any?>
    ) {
        val eventEntity = synchronized(lock) {
            if (finished) return
            seq += 1
            eventCount += 1
            DiagnosticEventEntity(
                eventId = UUID.randomUUID().toString(),
                traceId = traceId,
                sessionId = sessionId,
                spanId = UUID.randomUUID().toString(),
                parentSpanId = rootSpanId,
                seq = seq,
                category = request.category.name,
                name = name,
                phase = phase.name,
                severity = severity.name,
                createdAtEpochMs = System.currentTimeMillis(),
                payloadVersion = PAYLOAD_VERSION,
                warningCode = null,
                payloadJson = DebugDiagnosticsJson.encode(payload),
                sourceClass = request.sourceClass,
                screenOrWorker = request.screenOrWorker,
                entityDate = request.entityDate?.toString(),
                rangeStart = request.dateRange?.startDate?.toString(),
                rangeEnd = request.dateRange?.endDate?.toString()
            )
        }
        controller.enqueueEvent(eventEntity)
    }

    override fun warning(code: String, payload: Map<String, Any?>, name: String) {
        val eventEntity = synchronized(lock) {
            if (finished) return
            seq += 1
            eventCount += 1
            warningCount += 1
            if (firstWarningCode == null) {
                firstWarningCode = code
            }
            warningCodes += code
            DiagnosticEventEntity(
                eventId = UUID.randomUUID().toString(),
                traceId = traceId,
                sessionId = sessionId,
                spanId = UUID.randomUUID().toString(),
                parentSpanId = rootSpanId,
                seq = seq,
                category = request.category.name,
                name = name,
                phase = DiagnosticPhase.ANOMALY.name,
                severity = DiagnosticSeverity.WARNING.name,
                createdAtEpochMs = System.currentTimeMillis(),
                payloadVersion = PAYLOAD_VERSION,
                warningCode = code,
                payloadJson = DebugDiagnosticsJson.encode(payload),
                sourceClass = request.sourceClass,
                screenOrWorker = request.screenOrWorker,
                entityDate = request.entityDate?.toString(),
                rangeStart = request.dateRange?.startDate?.toString(),
                rangeEnd = request.dateRange?.endDate?.toString()
            )
        }
        controller.enqueueEvent(eventEntity)
    }

    override fun error(name: String, throwable: Throwable?, payload: Map<String, Any?>) {
        val mergedPayload = buildMap {
            putAll(payload)
            throwable?.let { put("throwable", it.toDiagnosticPayload()) }
        }
        val eventEntity = synchronized(lock) {
            if (finished) return
            seq += 1
            eventCount += 1
            errorCount += 1
            DiagnosticEventEntity(
                eventId = UUID.randomUUID().toString(),
                traceId = traceId,
                sessionId = sessionId,
                spanId = UUID.randomUUID().toString(),
                parentSpanId = rootSpanId,
                seq = seq,
                category = request.category.name,
                name = name,
                phase = DiagnosticPhase.ANOMALY.name,
                severity = DiagnosticSeverity.ERROR.name,
                createdAtEpochMs = System.currentTimeMillis(),
                payloadVersion = PAYLOAD_VERSION,
                warningCode = null,
                payloadJson = DebugDiagnosticsJson.encode(mergedPayload),
                sourceClass = request.sourceClass,
                screenOrWorker = request.screenOrWorker,
                entityDate = request.entityDate?.toString(),
                rangeStart = request.dateRange?.startDate?.toString(),
                rangeEnd = request.dateRange?.endDate?.toString()
            )
        }
        controller.enqueueEvent(eventEntity)
    }

    override fun finish(status: DiagnosticStatus, payload: Map<String, Any?>) {
        val traceEntity = synchronized(lock) {
            if (finished) return
            finished = true
            val completedAt = System.currentTimeMillis()
            val resolvedStatus = when {
                status == DiagnosticStatus.CANCELLED -> DiagnosticStatus.CANCELLED
                errorCount > 0 || status == DiagnosticStatus.ERROR -> DiagnosticStatus.ERROR
                warningCount > 0 || status == DiagnosticStatus.WARNING -> DiagnosticStatus.WARNING
                else -> DiagnosticStatus.SUCCESS
            }
            val resolvedSeverity = when (resolvedStatus) {
                DiagnosticStatus.ERROR -> DiagnosticSeverity.ERROR
                DiagnosticStatus.WARNING -> DiagnosticSeverity.WARNING
                DiagnosticStatus.CANCELLED -> DiagnosticSeverity.WARNING
                DiagnosticStatus.SUCCESS -> DiagnosticSeverity.INFO
            }
            DiagnosticTraceEntity(
                traceId = traceId,
                sessionId = sessionId,
                rootSpanId = rootSpanId,
                category = request.category.name,
                name = request.name,
                status = resolvedStatus.name,
                severity = resolvedSeverity.name,
                startedAtEpochMs = startedAtEpochMs,
                completedAtEpochMs = completedAt,
                durationMs = completedAt - startedAtEpochMs,
                sourceClass = request.sourceClass,
                screenOrWorker = request.screenOrWorker,
                entityDate = request.entityDate?.toString(),
                rangeStart = request.dateRange?.startDate?.toString(),
                rangeEnd = request.dateRange?.endDate?.toString(),
                payloadVersion = PAYLOAD_VERSION,
                rootPayloadJson = DebugDiagnosticsJson.encode(request.payload),
                resultPayloadJson = DebugDiagnosticsJson.encode(payload),
                warningCount = warningCount,
                errorCount = errorCount,
                eventCount = eventCount,
                firstWarningCode = firstWarningCode,
                warningFingerprint = warningCodes.sorted().joinToString("|").ifBlank { null },
                appVersion = session.appVersion,
                buildType = session.buildType
            )
        }
        controller.enqueueTraceFinish(traceEntity)
    }
}
