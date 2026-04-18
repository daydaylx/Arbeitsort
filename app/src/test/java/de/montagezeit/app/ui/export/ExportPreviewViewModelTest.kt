package de.montagezeit.app.ui.export

import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.TravelLeg
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

    private fun record(entry: WorkEntry, travelMinutes: Int = 0): WorkEntryWithTravelLegs {
        val legs = if (travelMinutes > 0) listOf(
            TravelLeg(workEntryDate = entry.date, sortOrder = 0, paidMinutesOverride = travelMinutes)
        ) else emptyList()
        return WorkEntryWithTravelLegs(workEntry = entry, travelLegs = legs)
    }

    @Test
    fun `calculatePreviewSummary sums work travel and paid minutes`() {
        val entries = listOf(
            record(WorkEntry(
                date = LocalDate.of(2026, 1, 10),
                dayType = DayType.WORK,
                workStart = LocalTime.of(8, 0),
                workEnd = LocalTime.of(18, 0),
                breakMinutes = 60,
                mealAllowanceAmountCents = 820,
                confirmedWorkDay = true,
                confirmationAt = System.currentTimeMillis()
            ), travelMinutes = 60),
            record(WorkEntry(
                date = LocalDate.of(2026, 1, 11),
                dayType = DayType.WORK,
                workStart = LocalTime.of(7, 30),
                workEnd = LocalTime.of(16, 0),
                breakMinutes = 30,
                mealAllowanceAmountCents = 2220,
                confirmedWorkDay = true,
                confirmationAt = System.currentTimeMillis()
            ), travelMinutes = 30)
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
                        workStart = LocalTime.of(8, 0),
                        workEnd = LocalTime.of(17, 0),
                        breakMinutes = 60,
                        mealAllowanceAmountCents = 820,
                        confirmedWorkDay = true
                    )),
                    record(WorkEntry(
                        date = LocalDate.of(2026, 1, 11),
                        dayType = DayType.WORK,
                        workStart = LocalTime.of(8, 0),
                        workEnd = LocalTime.of(17, 0),
                        breakMinutes = 60,
                        mealAllowanceAmountCents = 2220,
                        confirmedWorkDay = true
                    ))
                )
            )
        )

        assertEquals("30,40 €", totals.mealAllowanceTotal)
    }

    @Test
    fun `calculatePreviewSummary excludes unconfirmed work entries despite work block`() {
        val entries = listOf(
            record(
                WorkEntry(
                    date = LocalDate.of(2026, 1, 10),
                    dayType = DayType.WORK,
                    workStart = LocalTime.of(8, 0),
                    workEnd = LocalTime.of(18, 0),
                    breakMinutes = 60,
                    mealAllowanceAmountCents = 820,
                    confirmedWorkDay = true
                ),
                travelMinutes = 60
            ),
            record(
                WorkEntry(
                    date = LocalDate.of(2026, 1, 11),
                    dayType = DayType.WORK,
                    workStart = LocalTime.of(7, 30),
                    workEnd = LocalTime.of(16, 0),
                    breakMinutes = 30,
                    mealAllowanceAmountCents = 2220,
                    confirmedWorkDay = false
                ),
                travelMinutes = 30
            )
        )

        val summary = calculatePreviewSummary(entries)

        assertEquals(540, summary.workMinutes)
        assertEquals(60, summary.travelMinutes)
        assertEquals(600, summary.paidMinutes)
        assertEquals(820, summary.mealAllowanceCents)
    }

    @Test
    fun `calculatePreviewSummary excludes WORK entries without data`() {
        val entries = listOf(
            record(
                WorkEntry(
                    date = LocalDate.of(2026, 1, 10),
                    dayType = DayType.WORK,
                    workStart = LocalTime.of(8, 0),
                    workEnd = LocalTime.of(18, 0),
                    breakMinutes = 60,
                    mealAllowanceAmountCents = 820,
                    confirmedWorkDay = true
                ),
                travelMinutes = 60
            ),
            record(
                WorkEntry(
                    date = LocalDate.of(2026, 1, 11),
                    dayType = DayType.WORK,
                    workStart = null,
                    workEnd = null,
                    breakMinutes = 0,
                    mealAllowanceAmountCents = 0,
                    confirmedWorkDay = false
                ),
                travelMinutes = 0
            )
        )

        val summary = calculatePreviewSummary(entries)

        assertEquals(540, summary.workMinutes)
        assertEquals(60, summary.travelMinutes)
        assertEquals(600, summary.paidMinutes)
        assertEquals(820, summary.mealAllowanceCents)
    }

    @Test
    fun `buildExportPreviewRow exposes meal allowance label only when amount is positive`() {
        val entryWithMeal = WorkEntry(
            date = LocalDate.of(2026, 1, 10),
            dayType = DayType.WORK,
            workStart = LocalTime.of(8, 0),
            workEnd = LocalTime.of(17, 0),
            breakMinutes = 60,
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
    fun `buildExportPreviewRow hides stored meal allowance when work day has zero activity`() {
        val zeroNetEntry = WorkEntry(
            date = LocalDate.of(2026, 1, 13),
            dayType = DayType.WORK,
            workStart = LocalTime.of(8, 0),
            workEnd = LocalTime.of(8, 30),
            breakMinutes = 30,
            mealAllowanceAmountCents = 1400
        )

        assertNull(buildExportPreviewRow(record(zeroNetEntry)).mealAllowanceLabel)
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
