package de.montagezeit.app.data.local.entity

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalTime

@Entity(
    tableName = "work_entries",
    indices = [
        Index(value = ["date"], unique = true),
        Index(value = ["createdAt"]),
        Index(value = ["dayType", "date"]) // Composite index für Work-Entry-Queries
    ]
)
data class WorkEntry(
    @PrimaryKey
    val date: LocalDate,

    // Core Fields (Arbeitszeit Defaults)
    val workStart: LocalTime? = null,
    val workEnd: LocalTime? = null,
    val breakMinutes: Int = 0,
    val dayType: DayType = DayType.WORK,

    // Daily Location (Pflicht)
    val dayLocationLabel: String = "",

    // Morning Snapshot
    val morningCapturedAt: Long? = null,

    // Evening Snapshot
    val eveningCapturedAt: Long? = null,

    // Daily Confirmation
    val confirmedWorkDay: Boolean = false,
    val confirmationAt: Long? = null,
    val confirmationSource: String? = null,
    
    // Verpflegungspauschale
    val mealIsArrivalDeparture: Boolean = false,
    val mealBreakfastIncluded: Boolean = false,
    val mealAllowanceBaseCents: Int = 0,
    val mealAllowanceAmountCents: Int = 0,

    // Meta & Flags
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    @Ignore var travelStartAt: Long? = null
    @Ignore var travelArriveAt: Long? = null
    @Ignore var travelLabelStart: String? = null
    @Ignore var travelLabelEnd: String? = null
    @Ignore var travelFromLabel: String? = null
    @Ignore var travelToLabel: String? = null
    @Ignore var travelDistanceKm: Double? = null
    @Ignore var travelPaidMinutes: Int? = null
    @Ignore var travelSource: TravelSource? = null
    @Ignore var travelUpdatedAt: Long? = null
    @Ignore var returnStartAt: Long? = null
    @Ignore var returnArriveAt: Long? = null

    constructor(
        date: LocalDate,
        workStart: LocalTime? = null,
        workEnd: LocalTime? = null,
        breakMinutes: Int = 0,
        dayType: DayType = DayType.WORK,
        dayLocationLabel: String = "",
        morningCapturedAt: Long? = null,
        eveningCapturedAt: Long? = null,
        confirmedWorkDay: Boolean = false,
        confirmationAt: Long? = null,
        confirmationSource: String? = null,
        mealIsArrivalDeparture: Boolean = false,
        mealBreakfastIncluded: Boolean = false,
        mealAllowanceBaseCents: Int = 0,
        mealAllowanceAmountCents: Int = 0,
        travelStartAt: Long? = null,
        travelArriveAt: Long? = null,
        travelLabelStart: String? = null,
        travelLabelEnd: String? = null,
        travelFromLabel: String? = null,
        travelToLabel: String? = null,
        travelDistanceKm: Double? = null,
        travelPaidMinutes: Int? = null,
        travelSource: TravelSource? = null,
        travelUpdatedAt: Long? = null,
        returnStartAt: Long? = null,
        returnArriveAt: Long? = null,
        note: String? = null,
        createdAt: Long = System.currentTimeMillis(),
        updatedAt: Long = System.currentTimeMillis()
    ) : this(
        date = date,
        workStart = workStart,
        workEnd = workEnd,
        breakMinutes = breakMinutes,
        dayType = dayType,
        dayLocationLabel = dayLocationLabel,
        morningCapturedAt = morningCapturedAt,
        eveningCapturedAt = eveningCapturedAt,
        confirmedWorkDay = confirmedWorkDay,
        confirmationAt = confirmationAt,
        confirmationSource = confirmationSource,
        mealIsArrivalDeparture = mealIsArrivalDeparture,
        mealBreakfastIncluded = mealBreakfastIncluded,
        mealAllowanceBaseCents = mealAllowanceBaseCents,
        mealAllowanceAmountCents = mealAllowanceAmountCents,
        note = note,
        createdAt = createdAt,
        updatedAt = updatedAt
    ) {
        this.travelStartAt = travelStartAt
        this.travelArriveAt = travelArriveAt
        this.travelLabelStart = travelLabelStart
        this.travelLabelEnd = travelLabelEnd
        this.travelFromLabel = travelFromLabel
        this.travelToLabel = travelToLabel
        this.travelDistanceKm = travelDistanceKm
        this.travelPaidMinutes = travelPaidMinutes
        this.travelSource = travelSource
        this.travelUpdatedAt = travelUpdatedAt
        this.returnStartAt = returnStartAt
        this.returnArriveAt = returnArriveAt
    }
}

enum class DayType {
    WORK,
    OFF,
    COMP_TIME // Überstundenabbau (ganzer Tag) – reduziert Überstundenkonto um targetMinutes
}

enum class TravelSource {
    MANUAL,
    ROUTED,
    ESTIMATED
}
