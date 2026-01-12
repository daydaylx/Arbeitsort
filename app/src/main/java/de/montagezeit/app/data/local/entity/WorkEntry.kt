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
        Index(value = ["createdAt"])
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
    
    // Morning Snapshot
    val morningCapturedAt: Long? = null,
    val morningLocationLabel: String? = null,
    val morningLat: Double? = null,
    val morningLon: Double? = null,
    val morningAccuracyMeters: Float? = null,
    val outsideLeipzigMorning: Boolean? = null,
    val morningLocationStatus: LocationStatus = LocationStatus.UNAVAILABLE,
    
    // Evening Snapshot
    val eveningCapturedAt: Long? = null,
    val eveningLocationLabel: String? = null,
    val eveningLat: Double? = null,
    val eveningLon: Double? = null,
    val eveningAccuracyMeters: Float? = null,
    val outsideLeipzigEvening: Boolean? = null,
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
    
    // Meta & Flags
    val needsReview: Boolean = false,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class DayType {
    WORK,
    OFF
}

enum class LocationStatus {
    OK,
    UNAVAILABLE,
    LOW_ACCURACY
}

enum class TravelSource {
    MANUAL,
    ROUTED,
    ESTIMATED
}
