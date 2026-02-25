package de.montagezeit.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalTime

@Entity(
    tableName = "work_entries",
    indices = [
        Index(value = ["needsReview"]),
        Index(value = ["date"], unique = true),
        Index(value = ["createdAt"]),
        Index(value = ["dayType", "date"]) // Composite index für Work-Entry-Queries
    ]
)
data class WorkEntry(
    @PrimaryKey
    val date: LocalDate,
    
    // Core Fields (Arbeitszeit Defaults)
    val workStart: LocalTime = LocalTime.of(8, 0),
    val workEnd: LocalTime = LocalTime.of(19, 0),
    val breakMinutes: Int = 60,
    val dayType: DayType = DayType.WORK,

    // Daily Location (Pflicht)
    val dayLocationLabel: String = "",
    val dayLocationSource: DayLocationSource = DayLocationSource.FALLBACK,
    val dayLocationLat: Double? = null,
    val dayLocationLon: Double? = null,
    val dayLocationAccuracyMeters: Float? = null,
    
    // Morning Snapshot
    val morningCapturedAt: Long? = null,
    val morningLocationLabel: String? = null,
    val morningLat: Double? = null,
    val morningLon: Double? = null,
    val morningAccuracyMeters: Float? = null,
    val morningLocationStatus: LocationStatus = LocationStatus.UNAVAILABLE,
    
    // Evening Snapshot
    val eveningCapturedAt: Long? = null,
    val eveningLocationLabel: String? = null,
    val eveningLat: Double? = null,
    val eveningLon: Double? = null,
    val eveningAccuracyMeters: Float? = null,
    val eveningLocationStatus: LocationStatus = LocationStatus.UNAVAILABLE,
    
    // Travel (optional)
    val travelStartAt: Long? = null,
    val travelArriveAt: Long? = null,
    val travelLabelStart: String? = null,
    val travelLabelEnd: String? = null,
    val travelFromLabel: String? = null,
    val travelToLabel: String? = null,
    val travelDistanceKm: Double? = null,
    val travelPaidMinutes: Int? = null,
    val travelSource: TravelSource? = null,
    val travelUpdatedAt: Long? = null,
    
    // Daily Confirmation
    val confirmedWorkDay: Boolean = false,
    val confirmationAt: Long? = null,
    val confirmationSource: String? = null,
    
    // Meta & Flags
    val needsReview: Boolean = false,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class DayType {
    WORK,
    OFF,
    COMP_TIME // Überstundenabbau (ganzer Tag) – reduziert Überstundenkonto um targetMinutes
}

enum class LocationStatus {
    OK,
    UNAVAILABLE,
    LOW_ACCURACY
}

enum class DayLocationSource {
    GPS,
    MANUAL,
    FALLBACK
}

enum class TravelSource {
    MANUAL,
    ROUTED,
    ESTIMATED
}
