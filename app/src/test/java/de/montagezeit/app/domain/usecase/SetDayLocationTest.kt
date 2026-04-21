package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.preferences.ReminderSettings
import de.montagezeit.app.data.preferences.ReminderSettingsManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class SetDayLocationTest {

    private lateinit var workEntryDao: WorkEntryDao
    private lateinit var reminderSettingsManager: ReminderSettingsManager
    private lateinit var useCase: SetDayLocation

    @Before
    fun setup() {
        workEntryDao = mockk(relaxed = true)
        reminderSettingsManager = mockk()
        every { reminderSettingsManager.settings } returns flowOf(ReminderSettings())
        useCase = SetDayLocation(testRepository(workEntryDao), reminderSettingsManager)
    }

    @Test
    fun `invoke trimmt Arbeitsort beim Update`() = runTest {
        val date = LocalDate.of(2026, 3, 12)
        val existing = WorkEntry(date = date, dayType = DayType.WORK, dayLocationLabel = "Alt")
        mockReadModifyWrite(date, existing)

        val result = useCase(date, "  Neuer Ort  ")

        assertEquals("Neuer Ort", result.dayLocationLabel)
        assertEquals(existing.date, result.date)
        coVerify(exactly = 1) { workEntryDao.readModifyWrite(date, any()) }
    }

    @Test
    fun `invoke lehnt leeren Arbeitsort ab`() = runTest {
        val date = LocalDate.of(2026, 3, 12)

        val error = try {
            useCase(date, "   ")
            throw AssertionError("Expected IllegalArgumentException")
        } catch (exception: IllegalArgumentException) {
            exception
        }

        assertEquals("dayLocationLabel darf nicht leer sein", error.message)
        coVerify(exactly = 0) { workEntryDao.readModifyWrite(any(), any()) }
    }

    @Test
    fun `invoke normalisiert Verpflegungspauschale bei Standortwechsel nach Leipzig`() = runTest {
        val date = LocalDate.of(2026, 3, 12)
        val existing = WorkEntry(
            date = date,
            dayType = DayType.WORK,
            dayLocationLabel = "Dresden",
            workStart = java.time.LocalTime.of(8, 0),
            workEnd = java.time.LocalTime.of(17, 0),
            breakMinutes = 60,
            mealIsArrivalDeparture = true,
            mealBreakfastIncluded = true,
            mealAllowanceBaseCents = 1400,
            mealAllowanceAmountCents = 820
        )
        mockReadModifyWrite(date, existing)

        val result = useCase(date, "Leipzig")

        assertEquals("Leipzig", result.dayLocationLabel)
        assertEquals(0, result.mealAllowanceBaseCents)
        assertEquals(0, result.mealAllowanceAmountCents)
        assertEquals(false, result.mealIsArrivalDeparture)
        assertEquals(false, result.mealBreakfastIncluded)
        assertEquals(true, result.confirmedWorkDay)
    }

    private fun mockReadModifyWrite(date: LocalDate, existingEntry: WorkEntry?) {
        coEvery { workEntryDao.readModifyWrite(date, any()) } coAnswers {
            val modify = secondArg<(WorkEntry?) -> WorkEntry>()
            modify(existingEntry)
        }
    }
}
