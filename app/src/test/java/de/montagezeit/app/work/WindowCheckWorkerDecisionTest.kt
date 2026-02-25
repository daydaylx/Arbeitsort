package de.montagezeit.app.work

import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Unit-Tests für die statischen Entscheidungsmethoden von WindowCheckWorker.
 *
 * Diese Methoden bestimmen, ob ein Reminder gezeigt werden soll:
 *   - shouldShowMorningReminder(entry)
 *   - shouldShowEveningReminder(entry)
 *   - shouldShowFallbackReminder(entry)
 *   - shouldShowDailyReminder(entry)
 *
 * Alle vier sind `internal` static members und können direkt aus dem gleichen
 * Paket getestet werden (kein Instrumented Test nötig).
 */
class WindowCheckWorkerDecisionTest {

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private fun workEntry(
        morningCapturedAt: Long? = null,
        eveningCapturedAt: Long? = null,
        confirmedWorkDay: Boolean = false,
        dayType: DayType = DayType.WORK
    ) = WorkEntry(
        date = LocalDate.of(2026, 2, 17),
        dayType = dayType,
        morningCapturedAt = morningCapturedAt,
        eveningCapturedAt = eveningCapturedAt,
        confirmedWorkDay = confirmedWorkDay
    )

    // -------------------------------------------------------------------------
    // Kein Eintrag für diesen Tag (null)
    // -------------------------------------------------------------------------

    @Test
    fun `null entry - alle Reminder sollen angezeigt werden`() {
        assertFalse(WindowCheckWorker.shouldShowMorningReminder(null).let { !it }) // true
        assertTrue(WindowCheckWorker.shouldShowMorningReminder(null))
        assertTrue(WindowCheckWorker.shouldShowEveningReminder(null))
        assertTrue(WindowCheckWorker.shouldShowFallbackReminder(null))
        assertTrue(WindowCheckWorker.shouldShowDailyReminder(null))
    }

    // -------------------------------------------------------------------------
    // WORK-Tag – alle Snapshots und Bestätigung vorhanden
    // -------------------------------------------------------------------------

    @Test
    fun `vollstaendig bestaetiger WORK-Tag unterdrückt alle Reminder`() {
        val entry = workEntry(
            morningCapturedAt = 1_000L,
            eveningCapturedAt = 2_000L,
            confirmedWorkDay = true
        )

        assertFalse(WindowCheckWorker.shouldShowMorningReminder(entry))
        assertFalse(WindowCheckWorker.shouldShowEveningReminder(entry))
        assertFalse(WindowCheckWorker.shouldShowFallbackReminder(entry))
        assertFalse(WindowCheckWorker.shouldShowDailyReminder(entry))
    }

    // -------------------------------------------------------------------------
    // WORK-Tag – kein Snapshot, nicht bestätigt
    // -------------------------------------------------------------------------

    @Test
    fun `unvollstaendiger WORK-Tag aktiviert alle Reminder`() {
        val entry = workEntry(
            morningCapturedAt = null,
            eveningCapturedAt = null,
            confirmedWorkDay = false
        )

        assertTrue(WindowCheckWorker.shouldShowMorningReminder(entry))
        assertTrue(WindowCheckWorker.shouldShowEveningReminder(entry))
        assertTrue(WindowCheckWorker.shouldShowFallbackReminder(entry))
        assertTrue(WindowCheckWorker.shouldShowDailyReminder(entry))
    }

    // -------------------------------------------------------------------------
    // WORK-Tag – nur Morgen-Snapshot vorhanden
    // -------------------------------------------------------------------------

    @Test
    fun `WORK-Tag mit nur Morgen-Snapshot - MorningReminder inaktiv, Evening und Fallback aktiv`() {
        val entry = workEntry(
            morningCapturedAt = 1_000L,
            eveningCapturedAt = null,
            confirmedWorkDay = false
        )

        assertFalse(WindowCheckWorker.shouldShowMorningReminder(entry))   // morning gesetzt → kein Reminder
        assertTrue(WindowCheckWorker.shouldShowEveningReminder(entry))    // evening fehlt
        assertTrue(WindowCheckWorker.shouldShowFallbackReminder(entry))   // evening fehlt → Fallback aktiv
        assertTrue(WindowCheckWorker.shouldShowDailyReminder(entry))      // nicht bestätigt
    }

    // -------------------------------------------------------------------------
    // WORK-Tag – nur Abend-Snapshot vorhanden
    // -------------------------------------------------------------------------

    @Test
    fun `WORK-Tag mit nur Abend-Snapshot - EveningReminder inaktiv, Morning und Fallback aktiv`() {
        val entry = workEntry(
            morningCapturedAt = null,
            eveningCapturedAt = 2_000L,
            confirmedWorkDay = false
        )

        assertTrue(WindowCheckWorker.shouldShowMorningReminder(entry))    // morning fehlt
        assertFalse(WindowCheckWorker.shouldShowEveningReminder(entry))   // evening gesetzt → kein Reminder
        assertTrue(WindowCheckWorker.shouldShowFallbackReminder(entry))   // morning fehlt → Fallback aktiv
        assertTrue(WindowCheckWorker.shouldShowDailyReminder(entry))      // nicht bestätigt
    }

    // -------------------------------------------------------------------------
    // WORK-Tag – beide Snapshots, aber NICHT bestätigt
    // -------------------------------------------------------------------------

    @Test
    fun `WORK-Tag mit beiden Snapshots aber nicht bestaetigt - nur Daily aktiv`() {
        val entry = workEntry(
            morningCapturedAt = 1_000L,
            eveningCapturedAt = 2_000L,
            confirmedWorkDay = false
        )

        assertFalse(WindowCheckWorker.shouldShowMorningReminder(entry))   // morning gesetzt
        assertFalse(WindowCheckWorker.shouldShowEveningReminder(entry))   // evening gesetzt
        assertFalse(WindowCheckWorker.shouldShowFallbackReminder(entry))  // beide gesetzt → kein Fallback
        assertTrue(WindowCheckWorker.shouldShowDailyReminder(entry))      // nicht bestätigt → Daily aktiv
    }

    // -------------------------------------------------------------------------
    // OFF-Tag
    // -------------------------------------------------------------------------

    @Test
    fun `OFF-Tag ohne Bestaetigung - keine Check-In-Reminder, aber Daily aktiv`() {
        val entry = workEntry(
            dayType = DayType.OFF,
            morningCapturedAt = null,
            eveningCapturedAt = null,
            confirmedWorkDay = false
        )

        // WORK-Condition schlägt fehl → keine Check-In-Reminder
        assertFalse(WindowCheckWorker.shouldShowMorningReminder(entry))
        assertFalse(WindowCheckWorker.shouldShowEveningReminder(entry))
        assertFalse(WindowCheckWorker.shouldShowFallbackReminder(entry))
        // Daily: confirmedWorkDay != true → Daily Reminder würde anklopfen
        assertTrue(WindowCheckWorker.shouldShowDailyReminder(entry))
    }

    @Test
    fun `OFF-Tag bestaetigt - alle Reminder inaktiv`() {
        val entry = workEntry(
            dayType = DayType.OFF,
            morningCapturedAt = null,
            eveningCapturedAt = null,
            confirmedWorkDay = true  // manuell bestätigt (z.B. via ConfirmOffDay)
        )

        assertFalse(WindowCheckWorker.shouldShowMorningReminder(entry))
        assertFalse(WindowCheckWorker.shouldShowEveningReminder(entry))
        assertFalse(WindowCheckWorker.shouldShowFallbackReminder(entry))
        assertFalse(WindowCheckWorker.shouldShowDailyReminder(entry))    // bestätigt → Daily inaktiv
    }

    // -------------------------------------------------------------------------
    // shouldShowDailyReminder – isoliert
    // -------------------------------------------------------------------------

    @Test
    fun `shouldShowDailyReminder - bestaetigt ergibt false`() {
        assertFalse(WindowCheckWorker.shouldShowDailyReminder(workEntry(confirmedWorkDay = true)))
    }

    @Test
    fun `shouldShowDailyReminder - nicht bestaetigt ergibt true`() {
        assertTrue(WindowCheckWorker.shouldShowDailyReminder(workEntry(confirmedWorkDay = false)))
    }

    // -------------------------------------------------------------------------
    // shouldShowFallbackReminder – Grenzfälle
    // -------------------------------------------------------------------------

    @Test
    fun `shouldShowFallbackReminder - beide Snapshots vorhanden ergibt false`() {
        val entry = workEntry(morningCapturedAt = 1L, eveningCapturedAt = 2L)
        assertFalse(WindowCheckWorker.shouldShowFallbackReminder(entry))
    }

    @Test
    fun `shouldShowFallbackReminder - kein morgen aber abend vorhanden ergibt true`() {
        val entry = workEntry(morningCapturedAt = null, eveningCapturedAt = 2L)
        assertTrue(WindowCheckWorker.shouldShowFallbackReminder(entry))
    }

    // -------------------------------------------------------------------------
    // COMP_TIME
    // -------------------------------------------------------------------------

    @Test
    fun `COMP_TIME tag mit confirmedWorkDay - alle Reminder unterdrückt`() {
        val entry = workEntry(
            dayType = DayType.COMP_TIME,
            confirmedWorkDay = true
        )

        // Morning/Evening/Fallback: DayType.WORK check fails → no reminder
        assertFalse(WindowCheckWorker.shouldShowMorningReminder(entry))
        assertFalse(WindowCheckWorker.shouldShowEveningReminder(entry))
        assertFalse(WindowCheckWorker.shouldShowFallbackReminder(entry))
        // Daily: explicit COMP_TIME guard returns false
        assertFalse(WindowCheckWorker.shouldShowDailyReminder(entry))
    }

    @Test
    fun `COMP_TIME tag ohne confirmedWorkDay - Daily Reminder trotzdem unterdrückt`() {
        // COMP_TIME is always auto-confirmed by SetDayType, but even without the
        // confirmedWorkDay flag the explicit COMP_TIME guard must suppress the reminder.
        val entry = workEntry(
            dayType = DayType.COMP_TIME,
            confirmedWorkDay = false
        )

        assertFalse(WindowCheckWorker.shouldShowMorningReminder(entry))
        assertFalse(WindowCheckWorker.shouldShowEveningReminder(entry))
        assertFalse(WindowCheckWorker.shouldShowFallbackReminder(entry))
        // shouldShowDailyReminder has an explicit COMP_TIME guard before the confirmedWorkDay check
        assertFalse(WindowCheckWorker.shouldShowDailyReminder(entry))
    }
}
