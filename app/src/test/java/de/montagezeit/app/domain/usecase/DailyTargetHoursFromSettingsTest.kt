package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.preferences.ReminderSettings
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

class DailyTargetHoursFromSettingsTest {

    @Test
    fun `berechnet taegliches soll aus start ende und pause`() {
        val settings = ReminderSettings(
            workStart = LocalTime.of(8, 0),
            workEnd = LocalTime.of(17, 0),
            breakMinutes = 60
        )

        val result = dailyTargetHoursFromSettings(settings)

        assertEquals(8.0, result, 0.0001)
    }

    @Test
    fun `ungueltige zeitspanne liefert nullziel`() {
        val settings = ReminderSettings(
            workStart = LocalTime.of(18, 0),
            workEnd = LocalTime.of(17, 0),
            breakMinutes = 60
        )

        val result = dailyTargetHoursFromSettings(settings)

        assertEquals(0.0, result, 0.0001)
    }

    @Test
    fun `pause groesser gleich dauer liefert nullziel`() {
        val settings = ReminderSettings(
            workStart = LocalTime.of(8, 0),
            workEnd = LocalTime.of(9, 0),
            breakMinutes = 60
        )

        val result = dailyTargetHoursFromSettings(settings)

        assertEquals(0.0, result, 0.0001)
    }
}
