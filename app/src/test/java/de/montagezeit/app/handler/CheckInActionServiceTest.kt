package de.montagezeit.app.handler

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CheckInActionServiceTest {

    private val today = LocalDate.of(2026, 3, 15)

    @Test
    fun `parseDateFromExtra returns now for null input`() {
        val result = CheckInActionService.parseDateFromExtra(null, now = today)
        assertEquals(today, result)
    }

    @Test
    fun `parseDateFromExtra returns parsed date for today`() {
        val result = CheckInActionService.parseDateFromExtra("2026-03-15", now = today)
        assertEquals(today, result)
    }

    @Test
    fun `parseDateFromExtra returns parsed date for yesterday (within 1 day tolerance)`() {
        val yesterday = today.minusDays(1)
        val result = CheckInActionService.parseDateFromExtra(yesterday.toString(), now = today)
        assertEquals(yesterday, result)
    }

    @Test
    fun `parseDateFromExtra returns parsed date for tomorrow (within 1 day tolerance)`() {
        val tomorrow = today.plusDays(1)
        val result = CheckInActionService.parseDateFromExtra(tomorrow.toString(), now = today)
        assertEquals(tomorrow, result)
    }

    @Test
    fun `parseDateFromExtra returns now for date 2 days ago (exceeds tolerance)`() {
        val twoDaysAgo = today.minusDays(2)
        val result = CheckInActionService.parseDateFromExtra(twoDaysAgo.toString(), now = today)
        assertEquals(today, result)
    }

    @Test
    fun `parseDateFromExtra returns now for date 2 days ahead (exceeds tolerance)`() {
        val twoDaysAhead = today.plusDays(2)
        val result = CheckInActionService.parseDateFromExtra(twoDaysAhead.toString(), now = today)
        assertEquals(today, result)
    }

    @Test
    fun `parseDateFromExtra returns now for malformed string`() {
        val result = CheckInActionService.parseDateFromExtra("not-a-date", now = today)
        assertEquals(today, result)
    }

    @Test
    fun `parseDateFromExtra returns now for empty string`() {
        val result = CheckInActionService.parseDateFromExtra("", now = today)
        assertEquals(today, result)
    }
}
