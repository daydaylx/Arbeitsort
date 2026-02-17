package de.montagezeit.app.export

import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntry
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Ergänzende Unit-Tests für PdfUtilities.
 *
 * Abgedeckt werden Lücken aus PdfUtilitiesTest:
 *   - formatTravelWindow (komplett fehlend)
 *   - Edge Cases: leere Listen, 0-Werte
 *   - filterWorkDays Grenzfälle
 */
class PdfUtilitiesAdditionalTest {

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Erstellt epoch-Millis für eine Uhrzeit am 2026-01-15 in der System-Zeitzone. */
    private fun epochMillisAt(hour: Int, minute: Int): Long {
        return LocalDateTime.of(2026, 1, 15, hour, minute)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    private fun workEntry(
        date: LocalDate = LocalDate.of(2026, 1, 15),
        dayType: DayType = DayType.WORK,
        morningCapturedAt: Long? = null,
        travelStartAt: Long? = null,
        travelArriveAt: Long? = null,
        travelPaidMinutes: Int? = null,
        confirmedWorkDay: Boolean = false
    ) = WorkEntry(
        date = date,
        dayType = dayType,
        workStart = LocalTime.of(8, 0),
        workEnd = LocalTime.of(19, 0),
        breakMinutes = 60,
        morningCapturedAt = morningCapturedAt,
        travelStartAt = travelStartAt,
        travelArriveAt = travelArriveAt,
        travelPaidMinutes = travelPaidMinutes,
        confirmedWorkDay = confirmedWorkDay
    )

    // -------------------------------------------------------------------------
    // formatTravelWindow
    // -------------------------------------------------------------------------

    @Test
    fun `formatTravelWindow - beide null ergibt leeren String`() {
        assertEquals("", PdfUtilities.formatTravelWindow(null, null))
    }

    @Test
    fun `formatTravelWindow - nur startAt null ergibt leeren String`() {
        assertEquals("", PdfUtilities.formatTravelWindow(null, epochMillisAt(14, 0)))
    }

    @Test
    fun `formatTravelWindow - nur arriveAt null ergibt leeren String`() {
        assertEquals("", PdfUtilities.formatTravelWindow(epochMillisAt(8, 0), null))
    }

    @Test
    fun `formatTravelWindow - beide gesetzt ergibt HHmm Trennstrich HHmm`() {
        val start  = epochMillisAt(8, 0)
        val arrive = epochMillisAt(14, 30)
        val result = PdfUtilities.formatTravelWindow(start, arrive)
        // Format: "HH:mm–HH:mm" (Gedankenstrich U+2013)
        assertEquals("08:00–14:30", result)
    }

    @Test
    fun `formatTravelWindow - Mitternacht bis frueh morgens korrekt formatiert`() {
        val start  = epochMillisAt(0, 0)
        val arrive = epochMillisAt(6, 45)
        val result = PdfUtilities.formatTravelWindow(start, arrive)
        assertEquals("00:00–06:45", result)
    }

    @Test
    fun `formatTravelWindow - Start gleich Ankunft ergibt gleiche Zeitangabe`() {
        val ts = epochMillisAt(12, 0)
        val result = PdfUtilities.formatTravelWindow(ts, ts)
        assertEquals("12:00–12:00", result)
    }

    // -------------------------------------------------------------------------
    // formatWorkHours – Grenzfälle
    // -------------------------------------------------------------------------

    @Test
    fun `formatWorkHours - 0 Stunden ergibt 0-Komma-00`() {
        assertEquals("0,00", PdfUtilities.formatWorkHours(0.0))
    }

    @Test
    fun `formatWorkHours - negative Stunden wird formatiert (kein Crash)`() {
        // Negative Werte sollten nicht vorkommen, aber defensiv testen
        val result = PdfUtilities.formatWorkHours(-1.5)
        assertEquals("-1,50", result)
    }

    // -------------------------------------------------------------------------
    // sumWorkHours – Grenzfälle
    // -------------------------------------------------------------------------

    @Test
    fun `sumWorkHours - leere Liste ergibt 0 Stunden`() {
        assertEquals(0.0, PdfUtilities.sumWorkHours(emptyList()), 0.001)
    }

    @Test
    fun `sumWorkHours - Liste mit OFF-Tags ergibt 0 Stunden`() {
        val offEntry = workEntry(dayType = DayType.OFF)
        assertEquals(0.0, PdfUtilities.sumWorkHours(listOf(offEntry)), 0.001)
    }

    @Test
    fun `sumWorkHours - gemischte WORK und OFF Tags zaehlt nur WORK`() {
        val workE = workEntry(dayType = DayType.WORK, confirmedWorkDay = true)
        val offE  = workEntry(dayType = DayType.OFF,  date = LocalDate.of(2026, 1, 16))
        // workE: 8:00–19:00 – 60 min Pause = 10.0 h; offE: 0 h
        val sum = PdfUtilities.sumWorkHours(listOf(workE, offE))
        assertEquals(10.0, sum, 0.01)
    }

    // -------------------------------------------------------------------------
    // sumTravelMinutes – Grenzfälle
    // -------------------------------------------------------------------------

    @Test
    fun `sumTravelMinutes - leere Liste ergibt 0`() {
        assertEquals(0, PdfUtilities.sumTravelMinutes(emptyList()))
    }

    @Test
    fun `sumTravelMinutes - Entry ohne travelPaidMinutes ergibt 0`() {
        val entry = workEntry(travelPaidMinutes = null)
        assertEquals(0, PdfUtilities.sumTravelMinutes(listOf(entry)))
    }

    // -------------------------------------------------------------------------
    // filterWorkDays – Grenzfälle
    // -------------------------------------------------------------------------

    @Test
    fun `filterWorkDays - leere Liste ergibt leere Liste`() {
        assertTrue(PdfUtilities.filterWorkDays(emptyList()).isEmpty())
    }

    @Test
    fun `filterWorkDays - nur OFF-Tags ergibt leere Liste`() {
        val entries = listOf(
            workEntry(dayType = DayType.OFF, date = LocalDate.of(2026, 1, 15)),
            workEntry(dayType = DayType.OFF, date = LocalDate.of(2026, 1, 16))
        )
        assertTrue(PdfUtilities.filterWorkDays(entries).isEmpty())
    }

    @Test
    fun `filterWorkDays - alle WORK-Tags bleiben erhalten`() {
        val entries = listOf(
            workEntry(date = LocalDate.of(2026, 1, 15)),
            workEntry(date = LocalDate.of(2026, 1, 16)),
            workEntry(date = LocalDate.of(2026, 1, 17))
        )
        assertEquals(3, PdfUtilities.filterWorkDays(entries).size)
    }

    // -------------------------------------------------------------------------
    // formatDate + formatTime – weitere Formate
    // -------------------------------------------------------------------------

    @Test
    fun `formatDate - einstellige Tag- und Monatszahlen werden mit fuehrender Null formatiert`() {
        assertEquals("01.01.2026", PdfUtilities.formatDate(LocalDate.of(2026, 1, 1)))
    }

    @Test
    fun `formatTime - Mitternacht wird als 00-00 formatiert`() {
        assertEquals("00:00", PdfUtilities.formatTime(LocalTime.MIDNIGHT))
    }

    @Test
    fun `formatTime - 23-59 korrekt formatiert`() {
        assertEquals("23:59", PdfUtilities.formatTime(LocalTime.of(23, 59)))
    }
}
