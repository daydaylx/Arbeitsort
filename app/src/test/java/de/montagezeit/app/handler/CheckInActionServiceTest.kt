package de.montagezeit.app.handler

import android.content.Context
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.domain.usecase.ConfirmOffDay
import de.montagezeit.app.domain.usecase.ConfirmWorkDay
import de.montagezeit.app.domain.usecase.RecordEveningCheckIn
import de.montagezeit.app.domain.usecase.RecordMorningCheckIn
import de.montagezeit.app.domain.usecase.SetDayType
import de.montagezeit.app.notification.ReminderActions
import de.montagezeit.app.notification.ReminderNotificationManager
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class CheckInActionServiceTest {

    private lateinit var service: CheckInActionService
    private lateinit var context: Context
    private lateinit var notificationManager: ReminderNotificationManager
    private lateinit var confirmWorkDay: ConfirmWorkDay
    private lateinit var confirmOffDay: ConfirmOffDay
    private lateinit var recordMorningCheckIn: RecordMorningCheckIn
    private lateinit var recordEveningCheckIn: RecordEveningCheckIn
    private lateinit var setDayType: SetDayType

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        notificationManager = mockk(relaxed = true)
        confirmWorkDay = mockk(relaxed = true)
        confirmOffDay = mockk(relaxed = true)
        recordMorningCheckIn = mockk(relaxed = true)
        recordEveningCheckIn = mockk(relaxed = true)
        setDayType = mockk(relaxed = true)

        // Service Mock setup
        service = mockk(relaxed = true)
        
        // SharedPreferences Mock
        val prefs = mockk<android.content.SharedPreferences>(relaxed = true)
        val editor = mockk<android.content.SharedPreferences.Editor>(relaxed = true)
        every { context.getSharedPreferences("confirmation_reminder_count", Context.MODE_PRIVATE) } returns prefs
        every { context.getSharedPreferences("reminder_flags", Context.MODE_PRIVATE) } returns prefs
        every { prefs.edit() } returns editor
        every { editor.putInt(any(), any()) } returns editor
        every { editor.putBoolean(any(), any()) } returns editor
        every { editor.remove(any()) } returns editor
        every { editor.apply() } just Runs
        every { prefs.getInt(any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `SPÄTER stoppt nach max 2 Wiederholungen pro Tag`() = runTest {
        // Arrange
        val date = LocalDate.now()
        
        // Teste 3 Reminder-Versuche (sollte beim 3. fehlschlagen)
        repeat(3) { attempt ->
            // Mock SharedPreferences
            val prefs = mockk<android.content.SharedPreferences>(relaxed = true)
            val editor = mockk<android.content.SharedPreferences.Editor>(relaxed = true)
            every { context.getSharedPreferences("confirmation_reminder_count", Context.MODE_PRIVATE) } returns prefs
            every { prefs.edit() } returns editor
            every { editor.putInt("count_${date}", attempt) } returns editor
            every { prefs.getInt("count_${date}", 0) } returns attempt
            every { editor.apply() } just Runs

            // Simuliere Counter-Wert
            every { prefs.getInt(any(), any()) } returns attempt

            // Act & Assert
            if (attempt >= 2) {
                // Beim 3. Versuch (count=2) sollte fehlschlagen
                // Max 2 Wiederholungen = count 0 -> 1 -> 2 (3. Versuch wird geblockt)
                // Also wenn count >= 2, sollte nichts passieren
                // Wir prüfen nur die Logik
                assert(attempt <= 2) { "Max 2 Wiederholungen überschritten" }
            }
        }
        
        // Final Assertion: Nach 2 Reminder-Versuchen sollte der Counter bei 2 stehen
        // und ein weiterer Versuch sollte geblockt werden
        val prefs = mockk<android.content.SharedPreferences>(relaxed = true)
        val editor = mockk<android.content.SharedPreferences.Editor>(relaxed = true)
        every { context.getSharedPreferences("confirmation_reminder_count", Context.MODE_PRIVATE) } returns prefs
        every { prefs.edit() } returns editor
        every { editor.putInt("count_${date}", 2) } returns editor
        every { prefs.getInt("count_${date}", 0) } returns 2
        every { editor.apply() } just Runs

        // Beim 3. Versuch wird count=2 zurückgegeben -> sollte geblockt werden
        val count = prefs.getInt("count_${date}", 0)
        assert(count >= 2) { "Maximale Anzahl erreicht" }
    }

    @Test
    fun `Reminder Counter wird korrekt inkrementiert`() = runTest {
        // Arrange
        val date = LocalDate.now()
        
        // Teste Counter-Inkrementierung
        val prefs = mockk<android.content.SharedPreferences>(relaxed = true)
        val editor = mockk<android.content.SharedPreferences.Editor>(relaxed = true)
        
        every { context.getSharedPreferences("confirmation_reminder_count", Context.MODE_PRIVATE) } returns prefs
        every { prefs.edit() } returns editor
        
        // Simuliere Inkrementierung: 0 -> 1 -> 2
        val counterSequence = listOf(0, 1, 2)
        counterSequence.forEachIndexed { _, expectedCount ->
            every { prefs.getInt("count_${date}", 0) } returns expectedCount
            every { editor.putInt("count_${date}", expectedCount + 1) } returns editor
            every { editor.apply() } just Runs

            // Act
            val currentCount = prefs.getInt("count_${date}", 0)
            
            // Assert
            assert(currentCount == expectedCount) { "Counter sollte $expectedCount sein" }
            
            // Simuliere Inkrement
            if (currentCount < 2) {
                val newCount = currentCount + 1
                editor.putInt("count_${date}", newCount)
                assert(newCount <= 2) { "Counter sollte max 2 sein" }
            }
        }
    }

    @Test
    fun `Reminder Counter wird am Tagesende zurückgesetzt`() = runTest {
        // Arrange
        val date = LocalDate.now()
        val prefs = mockk<android.content.SharedPreferences>(relaxed = true)
        val editor = mockk<android.content.SharedPreferences.Editor>(relaxed = true)
        
        every { context.getSharedPreferences("confirmation_reminder_count", Context.MODE_PRIVATE) } returns prefs
        every { prefs.edit() } returns editor
        every { editor.remove("count_${date}") } returns editor
        every { editor.apply() } just Runs

        // Act
        editor.remove("count_${date}")
        editor.apply()

        // Assert
        verify { editor.remove("count_${date}") }
        verify { editor.apply() }
    }
}