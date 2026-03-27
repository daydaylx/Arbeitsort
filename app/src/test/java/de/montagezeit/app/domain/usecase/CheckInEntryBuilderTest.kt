package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test
import java.time.LocalDate

class CheckInEntryBuilderTest {

    private val date = LocalDate.of(2024, 6, 10)

    @Test
    fun `build with MORNING snapshot creates new WORK entry with morningCapturedAt set when no existing entry`() {
        val result = CheckInEntryBuilder.build(date, existingEntry = null, CheckInEntryBuilder.Snapshot.MORNING)

        assertEquals(date, result.date)
        assertEquals(DayType.WORK, result.dayType)
        assertNotNull(result.morningCapturedAt)
        assertNull(result.eveningCapturedAt)
        assertEquals("", result.dayLocationLabel)
    }

    @Test
    fun `build with EVENING snapshot creates new WORK entry with eveningCapturedAt set when no existing entry`() {
        val result = CheckInEntryBuilder.build(date, existingEntry = null, CheckInEntryBuilder.Snapshot.EVENING)

        assertEquals(date, result.date)
        assertEquals(DayType.WORK, result.dayType)
        assertNull(result.morningCapturedAt)
        assertNotNull(result.eveningCapturedAt)
        assertEquals("", result.dayLocationLabel)
    }

    @Test
    fun `build with MORNING snapshot updates existing WORK entry morningCapturedAt and preserves eveningCapturedAt`() {
        val existing = WorkEntry(
            date = date,
            dayType = DayType.WORK,
            eveningCapturedAt = 12345L,
            dayLocationLabel = "München"
        )

        val result = CheckInEntryBuilder.build(date, existing, CheckInEntryBuilder.Snapshot.MORNING)

        assertEquals(DayType.WORK, result.dayType)
        assertNotNull(result.morningCapturedAt)
        assertEquals(12345L, result.eveningCapturedAt)
        assertEquals("München", result.dayLocationLabel)
    }

    @Test
    fun `build with EVENING snapshot updates existing WORK entry eveningCapturedAt and preserves morningCapturedAt`() {
        val existing = WorkEntry(
            date = date,
            dayType = DayType.WORK,
            morningCapturedAt = 99999L,
            dayLocationLabel = "Hamburg"
        )

        val result = CheckInEntryBuilder.build(date, existing, CheckInEntryBuilder.Snapshot.EVENING)

        assertEquals(DayType.WORK, result.dayType)
        assertEquals(99999L, result.morningCapturedAt)
        assertNotNull(result.eveningCapturedAt)
        assertEquals("Hamburg", result.dayLocationLabel)
    }

    @Test
    fun `build throws IllegalStateException for existing OFF day entry`() {
        val existing = WorkEntry(date = date, dayType = DayType.OFF)

        try {
            CheckInEntryBuilder.build(date, existing, CheckInEntryBuilder.Snapshot.MORNING)
            fail("Expected IllegalStateException")
        } catch (e: IllegalStateException) {
            // expected
        }
    }

    @Test
    fun `build throws IllegalStateException for existing COMP_TIME day entry`() {
        val existing = WorkEntry(date = date, dayType = DayType.COMP_TIME)

        try {
            CheckInEntryBuilder.build(date, existing, CheckInEntryBuilder.Snapshot.EVENING)
            fail("Expected IllegalStateException")
        } catch (e: IllegalStateException) {
            // expected
        }
    }

    @Test
    fun `build preserves non-blank dayLocationLabel from existing entry`() {
        val existing = WorkEntry(date = date, dayType = DayType.WORK, dayLocationLabel = "Berlin")

        val result = CheckInEntryBuilder.build(date, existing, CheckInEntryBuilder.Snapshot.MORNING)

        assertEquals("Berlin", result.dayLocationLabel)
    }

    @Test
    fun `build uses empty dayLocationLabel when existing entry has blank label`() {
        val existing = WorkEntry(date = date, dayType = DayType.WORK, dayLocationLabel = "   ")

        val result = CheckInEntryBuilder.build(date, existing, CheckInEntryBuilder.Snapshot.MORNING)

        assertEquals("", result.dayLocationLabel)
    }

    @Test
    fun `build with no existing entry uses empty dayLocationLabel`() {
        val result = CheckInEntryBuilder.build(date, existingEntry = null, CheckInEntryBuilder.Snapshot.EVENING)

        assertEquals("", result.dayLocationLabel)
    }
}
