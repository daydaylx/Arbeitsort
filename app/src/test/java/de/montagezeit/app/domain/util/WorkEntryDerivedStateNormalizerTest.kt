package de.montagezeit.app.domain.util

import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.TravelLeg
import de.montagezeit.app.data.local.entity.WorkEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class WorkEntryDerivedStateNormalizerTest {

    private val date = LocalDate.of(2026, 5, 1)
    private val fixedNow = 1_746_057_600_000L

    // ── WORK-like days ──────────────────────────────────────────────────────

    @Test
    fun `work day with work times is confirmed and gets DERIVED_STATE source`() {
        val entry = workEntry()
        val result = WorkEntryDerivedStateNormalizer.normalize(entry, emptyList(), fixedNow)

        assertTrue(result.confirmedWorkDay)
        assertEquals(ConfirmationSources.DERIVED_STATE, result.confirmationSource)
        assertEquals(fixedNow, result.confirmationAt)
    }

    @Test
    fun `work day without activity is not confirmed`() {
        val entry = WorkEntry(date = date, dayType = DayType.WORK, dayLocationLabel = "Berlin")
        val result = WorkEntryDerivedStateNormalizer.normalize(entry, emptyList(), fixedNow)

        assertFalse(result.confirmedWorkDay)
        assertNull(result.confirmationSource)
        assertNull(result.confirmationAt)
    }

    @Test
    fun `work day with only travel is confirmed via travel activity`() {
        val entry = WorkEntry(date = date, dayType = DayType.WORK, dayLocationLabel = "Berlin")
        val result = WorkEntryDerivedStateNormalizer.normalize(entry, listOf(travelLeg()), fixedNow)

        assertTrue(result.confirmedWorkDay)
        assertEquals(ConfirmationSources.DERIVED_STATE, result.confirmationSource)
    }

    @Test
    fun `work day with existing non-blank source keeps source on confirmation`() {
        val entry = workEntry(confirmationSource = ConfirmationSources.UI)
        val result = WorkEntryDerivedStateNormalizer.normalize(entry, emptyList(), fixedNow)

        assertTrue(result.confirmedWorkDay)
        assertEquals(ConfirmationSources.UI, result.confirmationSource)
    }

    @Test
    fun `work day with blank source falls back to DERIVED_STATE`() {
        val entry = workEntry(confirmationSource = "")
        val result = WorkEntryDerivedStateNormalizer.normalize(entry, emptyList(), fixedNow)

        assertTrue(result.confirmedWorkDay)
        assertEquals(ConfirmationSources.DERIVED_STATE, result.confirmationSource)
    }

    @Test
    fun `existing confirmationAt is preserved when already set`() {
        val existing = 1_000_000L
        val entry = workEntry(confirmationAt = existing)
        val result = WorkEntryDerivedStateNormalizer.normalize(entry, emptyList(), fixedNow)

        assertEquals(existing, result.confirmationAt)
    }

    // ── Non-work-like days ──────────────────────────────────────────────────

    @Test
    fun `OFF day without source gets DERIVED_STATE confirmation source`() {
        val entry = WorkEntry(date = date, dayType = DayType.OFF)
        val result = WorkEntryDerivedStateNormalizer.normalize(entry, emptyList(), fixedNow)

        assertTrue(result.confirmedWorkDay)
        assertEquals(ConfirmationSources.DERIVED_STATE, result.confirmationSource)
        assertEquals(fixedNow, result.confirmationAt)
    }

    @Test
    fun `COMP_TIME day without source gets DERIVED_STATE confirmation source`() {
        val entry = WorkEntry(date = date, dayType = DayType.COMP_TIME)
        val result = WorkEntryDerivedStateNormalizer.normalize(entry, emptyList(), fixedNow)

        assertTrue(result.confirmedWorkDay)
        assertEquals(ConfirmationSources.DERIVED_STATE, result.confirmationSource)
    }

    @Test
    fun `VACATION day without source gets DERIVED_STATE confirmation source`() {
        val entry = WorkEntry(date = date, dayType = DayType.VACATION)
        val result = WorkEntryDerivedStateNormalizer.normalize(entry, emptyList(), fixedNow)

        assertTrue(result.confirmedWorkDay)
        assertEquals(ConfirmationSources.DERIVED_STATE, result.confirmationSource)
        assertEquals(0, result.mealAllowanceAmountCents)
    }

    @Test
    fun `OFF day with existing source keeps that source`() {
        val entry = WorkEntry(
            date = date,
            dayType = DayType.OFF,
            confirmationSource = ConfirmationSources.NOTIFICATION,
            confirmationAt = 999L
        )
        val result = WorkEntryDerivedStateNormalizer.normalize(entry, emptyList(), fixedNow)

        assertEquals(ConfirmationSources.NOTIFICATION, result.confirmationSource)
        assertEquals(999L, result.confirmationAt)
    }

    // ── Meal allowance ──────────────────────────────────────────────────────

    @Test
    fun `work day in Leipzig gets zero meal allowance`() {
        val entry = workEntry(dayLocationLabel = "Leipzig")
        val result = WorkEntryDerivedStateNormalizer.normalize(entry, emptyList(), fixedNow)

        assertEquals(0, result.mealAllowanceBaseCents)
        assertEquals(0, result.mealAllowanceAmountCents)
    }

    @Test
    fun `work day outside Leipzig with activity gets non-zero meal allowance`() {
        val entry = workEntry(dayLocationLabel = "Berlin")
        val result = WorkEntryDerivedStateNormalizer.normalize(entry, emptyList(), fixedNow)

        assertTrue(result.mealAllowanceBaseCents > 0)
        assertTrue(result.mealAllowanceAmountCents > 0)
    }

    @Test
    fun `work day without activity gets zero meal allowance regardless of location`() {
        val entry = WorkEntry(date = date, dayType = DayType.WORK, dayLocationLabel = "Berlin")
        val result = WorkEntryDerivedStateNormalizer.normalize(entry, emptyList(), fixedNow)

        assertEquals(0, result.mealAllowanceBaseCents)
        assertEquals(0, result.mealAllowanceAmountCents)
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private fun workEntry(
        dayLocationLabel: String = "Berlin",
        confirmationSource: String? = null,
        confirmationAt: Long? = null
    ) = WorkEntry(
        date = date,
        dayType = DayType.WORK,
        dayLocationLabel = dayLocationLabel,
        workStart = LocalTime.of(8, 0),
        workEnd = LocalTime.of(16, 0),
        breakMinutes = 30,
        confirmationSource = confirmationSource,
        confirmationAt = confirmationAt
    )

    private fun travelLeg() = TravelLeg(
        workEntryDate = date,
        sortOrder = 0,
        startAt = 1_700_000_000_000L,
        arriveAt = 1_700_003_600_000L
    )
}
