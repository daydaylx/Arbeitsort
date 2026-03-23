package de.montagezeit.app.data.local.entity

import androidx.room.Entity
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
)

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
