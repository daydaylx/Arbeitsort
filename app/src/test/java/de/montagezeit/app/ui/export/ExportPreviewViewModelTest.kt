package de.montagezeit.app.ui.export

import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.local.entity.WorkEntryWithTravelLegs
import de.montagezeit.app.ui.screen.export.buildExportPreviewRow
import de.montagezeit.app.ui.screen.export.buildExportPreviewTotals
import de.montagezeit.app.ui.screen.export.calculatePreviewSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class ExportPreviewViewModelTest {

    private fun record(entry: WorkEntry) = WorkEntryWithTravelLegs(
        workEntry = entry,
        travelLegs = emptyList()
    )

    @Test
    fun `calculatePreviewSummary sums work travel and paid minutes`() {
        val entries = listOf(
            record(WorkEntry(
                date = LocalDate.of(2026, 1, 10),
                dayType = DayType.WORK,
                workStart = LocalTime.of(8, 0),
                workEnd = LocalTime.of(18, 0),
                breakMinutes = 60,
                travelPaidMinutes = 60,
                mealAllowanceAmountCents = 820,
                confirmedWorkDay = true,
                confirmationAt = System.currentTimeMillis()
            )),
            record(WorkEntry(
                date = LocalDate.of(2026, 1, 11),
                dayType = DayType.WORK,
                workStart = LocalTime.of(7, 30),
                workEnd = LocalTime.of(16, 0),
                breakMinutes = 30,
                travelPaidMinutes = 30,
                mealAllowanceAmountCents = 2220,
                confirmedWorkDay = true,
                confirmationAt = System.currentTimeMillis()
            ))
        )

        val summary = calculatePreviewSummary(entries)

        assertEquals(1020, summary.workMinutes)
        assertEquals(90, summary.travelMinutes)
        assertEquals(1110, summary.paidMinutes)
        assertEquals(3040, summary.mealAllowanceCents)
        assertEquals(summary.workMinutes + summary.travelMinutes, summary.paidMinutes)
    }

    @Test
    fun `buildExportPreviewTotals formats meal allowance total`() {
        val totals = buildExportPreviewTotals(
            calculatePreviewSummary(
                listOf(
                    record(WorkEntry(
                        date = LocalDate.of(2026, 1, 10),
                        dayType = DayType.WORK,
                        mealAllowanceAmountCents = 820
                    )),
                    record(WorkEntry(
                        date = LocalDate.of(2026, 1, 11),
                        dayType = DayType.WORK,
                        mealAllowanceAmountCents = 2220
                    ))
                )
            )
        )

        assertEquals("30,40 €", totals.mealAllowanceTotal)
    }

    @Test
    fun `buildExportPreviewRow exposes meal allowance label only when amount is positive`() {
        val entryWithMeal = WorkEntry(
            date = LocalDate.of(2026, 1, 10),
            dayType = DayType.WORK,
            mealAllowanceAmountCents = 820
        )
        val entryWithoutMeal = WorkEntry(
            date = LocalDate.of(2026, 1, 11),
            dayType = DayType.WORK,
            mealAllowanceAmountCents = 0
        )

        assertEquals("8,20 €", buildExportPreviewRow(record(entryWithMeal)).mealAllowanceLabel)
        assertNull(buildExportPreviewRow(record(entryWithoutMeal)).mealAllowanceLabel)
    }

    @Test
    fun `buildExportPreviewRow hides work schedule for off day`() {
        val offEntry = WorkEntry(
            date = LocalDate.of(2026, 1, 12),
            dayType = DayType.OFF
        )

        val row = buildExportPreviewRow(record(offEntry))

        assertEquals("–", row.startLabel)
        assertEquals("–", row.endLabel)
        assertEquals("–", row.breakLabel)
        assertEquals("0,00 h", row.workLabel)
    }
}
