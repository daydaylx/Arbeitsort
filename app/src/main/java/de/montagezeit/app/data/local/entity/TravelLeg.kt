package de.montagezeit.app.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.time.LocalDate

@Entity(
    tableName = "travel_legs",
    foreignKeys = [
        ForeignKey(
            entity = WorkEntry::class,
            parentColumns = ["date"],
            childColumns = ["workEntryDate"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["workEntryDate"]),
        Index(value = ["workEntryDate", "sortOrder"], unique = true)
    ]
)
data class TravelLeg(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val workEntryDate: LocalDate,
    val sortOrder: Int,
    val category: TravelLegCategory = TravelLegCategory.OTHER,
    val startAt: Long? = null,
    val arriveAt: Long? = null,
    val startLabel: String? = null,
    val endLabel: String? = null,
    val paidMinutesOverride: Int? = null,
    val source: TravelSource? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class TravelLegCategory {
    OUTBOUND,
    INTERSITE,
    RETURN,
    OTHER
}

data class WorkEntryWithTravelLegs(
    @Embedded
    val workEntry: WorkEntry,
    @Relation(
        parentColumn = "date",
        entityColumn = "workEntryDate"
    )
    val travelLegs: List<TravelLeg>
) {
    @Ignore
    val orderedTravelLegs: List<TravelLeg> = travelLegs.sortedBy { it.sortOrder }
}
