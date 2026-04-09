package de.montagezeit.app.diagnostics.debug.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "diagnostic_sessions",
    indices = [
        Index(value = ["startedAtEpochMs"])
    ]
)
data class DiagnosticSessionEntity(
    @PrimaryKey val sessionId: String,
    val startedAtEpochMs: Long,
    val appVersion: String,
    val buildType: String,
    val timezoneId: String
)

@Entity(
    tableName = "diagnostic_traces",
    indices = [
        Index(value = ["startedAtEpochMs"]),
        Index(value = ["sessionId"]),
        Index(value = ["category"]),
        Index(value = ["severity"]),
        Index(value = ["status"]),
        Index(value = ["entityDate"]),
        Index(value = ["rangeStart", "rangeEnd"])
    ]
)
data class DiagnosticTraceEntity(
    @PrimaryKey val traceId: String,
    val sessionId: String,
    val rootSpanId: String,
    val category: String,
    val name: String,
    val status: String,
    val severity: String,
    val startedAtEpochMs: Long,
    val completedAtEpochMs: Long?,
    val durationMs: Long?,
    val sourceClass: String,
    val screenOrWorker: String?,
    val entityDate: String?,
    val rangeStart: String?,
    val rangeEnd: String?,
    val payloadVersion: Int,
    val rootPayloadJson: String,
    val resultPayloadJson: String?,
    val warningCount: Int,
    val errorCount: Int,
    val eventCount: Int,
    val firstWarningCode: String?,
    val warningFingerprint: String?,
    val appVersion: String,
    val buildType: String
)

@Entity(
    tableName = "diagnostic_events",
    foreignKeys = [
        ForeignKey(
            entity = DiagnosticTraceEntity::class,
            parentColumns = ["traceId"],
            childColumns = ["traceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["traceId"]),
        Index(value = ["sessionId"]),
        Index(value = ["createdAtEpochMs"]),
        Index(value = ["severity"]),
        Index(value = ["warningCode"])
    ]
)
data class DiagnosticEventEntity(
    @PrimaryKey val eventId: String,
    val traceId: String,
    val sessionId: String,
    val spanId: String,
    val parentSpanId: String?,
    val seq: Int,
    val category: String,
    val name: String,
    val phase: String,
    val severity: String,
    val createdAtEpochMs: Long,
    val payloadVersion: Int,
    val warningCode: String?,
    val payloadJson: String,
    val sourceClass: String,
    val screenOrWorker: String?,
    val entityDate: String?,
    val rangeStart: String?,
    val rangeEnd: String?
)
