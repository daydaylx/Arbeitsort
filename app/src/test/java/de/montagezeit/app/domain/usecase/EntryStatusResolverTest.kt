package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.TravelLeg
import de.montagezeit.app.data.local.entity.WorkEntry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class EntryStatusResolverTest {

    private val date = LocalDate.of(2026, 4, 20)

    @Test
    fun `confirmed work with positive work minutes is confirmed and eligible`() {
        val entry = WorkEntry(
            date = date,
            dayType = DayType.WORK,
            workStart = LocalTime.of(8, 0),
            workEnd = LocalTime.of(17, 0),
            breakMinutes = 60,
            confirmedWorkDay = true
        )

        val status = EntryStatusResolver.resolve(entry)

        assertTrue(status.hasActivity)
        assertTrue(status.isConfirmed)
        assertTrue(status.isStatisticsEligible)
        assertTrue(status.isReminderTerminal)
    }

    @Test
    fun `unconfirmed work with activity stays pending`() {
        val entry = WorkEntry(
            date = date,
            dayType = DayType.WORK,
            workStart = LocalTime.of(8, 0),
            workEnd = LocalTime.of(17, 0),
            breakMinutes = 60,
            confirmedWorkDay = false
        )

        val status = EntryStatusResolver.resolve(entry)

        assertTrue(status.hasActivity)
        assertFalse(status.isConfirmed)
        assertFalse(status.isStatisticsEligible)
        assertFalse(status.isReminderTerminal)
    }

    @Test
    fun `confirmed empty work day is not terminal`() {
        val entry = WorkEntry(
            date = date,
            dayType = DayType.WORK,
            confirmedWorkDay = true
        )

        val status = EntryStatusResolver.resolve(entry)

        assertFalse(status.hasActivity)
        assertFalse(status.isConfirmed)
        assertFalse(status.isStatisticsEligible)
        assertFalse(status.isReminderTerminal)
    }

    @Test
    fun `travel only work day auto confirmation rule uses positive travel`() {
        val entry = WorkEntry(
            date = date,
            dayType = DayType.WORK,
            confirmedWorkDay = false
        )
        val travel = listOf(
            TravelLeg(
                workEntryDate = date,
                sortOrder = 0,
                paidMinutesOverride = 90
            )
        )

        assertTrue(EntryStatusResolver.shouldAutoConfirmWorkDay(entry, travel))
    }

    @Test
    fun `off and comp time are always terminal`() {
        assertTrue(
            EntryStatusResolver.resolve(
                WorkEntry(date = date, dayType = DayType.OFF, confirmedWorkDay = false)
            ).isReminderTerminal
        )
        assertTrue(
            EntryStatusResolver.resolve(
                WorkEntry(date = date, dayType = DayType.COMP_TIME, confirmedWorkDay = false)
            ).isReminderTerminal
        )
    }
}
