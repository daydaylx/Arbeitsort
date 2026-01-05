package de.montagezeit.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "work_entries")
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
