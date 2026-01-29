package de.montagezeit.app.ui.export

import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.ui.screen.export.calculatePreviewSummary
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class ExportPreviewViewModelTest {

    @Test
    fun `calculatePreviewSummary sums work travel and paid minutes`() {
        val entries = listOf(
            WorkEntry(
                date = LocalDate.of(2026, 1, 10),
                dayType = DayType.WORK,
                workStart = LocalTime.of(8, 0),
                workEnd = LocalTime.of(18, 0),
                breakMinutes = 60,
                travelPaidMinutes = 60,
                confirmedWorkDay = true,
                confirmationAt = System.currentTimeMillis()
            ),
            WorkEntry(
                date = LocalDate.of(2026, 1, 11),
                dayType = DayType.WORK,
                workStart = LocalTime.of(7, 30),
                workEnd = LocalTime.of(16, 0),
                breakMinutes = 30,
                travelPaidMinutes = 30,
                confirmedWorkDay = true,
                confirmationAt = System.currentTimeMillis()
            )
        )

        val summary = calculatePreviewSummary(entries)

        assertEquals(1020, summary.workMinutes)
        assertEquals(90, summary.travelMinutes)
        assertEquals(1110, summary.paidMinutes)
        assertEquals(summary.workMinutes + summary.travelMinutes, summary.paidMinutes)
    }
}
