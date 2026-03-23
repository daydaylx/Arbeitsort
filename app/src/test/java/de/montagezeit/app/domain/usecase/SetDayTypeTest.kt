package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntry
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import org.junit.Assert.*

class SetDayTypeTest {

    private lateinit var workEntryDao: WorkEntryDao
    private lateinit var setDayType: SetDayType

    @Before
    fun setup() {
        workEntryDao = mockk()
        setDayType = SetDayType(workEntryDao)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    /**
     * Helper: mocks readModifyWrite to call the lambda with the given existing entry,
     * then capture the upserted result.
     */
    private fun mockReadModifyWrite(date: LocalDate, existingEntry: WorkEntry?) {
        coEvery { workEntryDao.readModifyWrite(date, any()) } coAnswers {
            val modify = secondArg<(WorkEntry?) -> WorkEntry>()
            val result = modify(existingEntry)
            // The real readModifyWrite calls upsert internally, so we just verify the lambda output
        }
    }

    @Test
    fun `invoke - Neuer Eintrag - Erstellt WorkEntry mit DayType`() = runTest {
        val date = LocalDate.now()
        val dayType = DayType.WORK

        mockReadModifyWrite(date, null)

        val result = setDayType.invoke(date, dayType)

        assertEquals(date, result.date)
        assertEquals(dayType, result.dayType)
        assertNotNull(result.createdAt)
        assertNotNull(result.updatedAt)
        assertEquals(result.createdAt, result.updatedAt)

        coVerify { workEntryDao.readModifyWrite(date, any()) }
    }

    @Test
    fun `invoke - Existierender Eintrag - Aktualisiert DayType`() = runTest {
        val date = LocalDate.now()
        val existingEntry = WorkEntry(
            date = date,
            dayType = DayType.WORK,
            createdAt = 1000000L,
            updatedAt = 1000000L
        )
        val newDayType = DayType.OFF

        mockReadModifyWrite(date, existingEntry)

        val result = setDayType.invoke(date, newDayType)

        assertEquals(date, result.date)
        assertEquals(newDayType, result.dayType)
        assertEquals(existingEntry.createdAt, result.createdAt)
        assertTrue(result.updatedAt > existingEntry.updatedAt)

        coVerify { workEntryDao.readModifyWrite(date, any()) }
    }

    @Test
    fun `invoke - WORK zu OFF - Aktualisiert korrekt`() = runTest {
        val date = LocalDate.now()
        val existingEntry = WorkEntry(
            date = date,
            dayType = DayType.WORK,
            mealIsArrivalDeparture = true,
            mealBreakfastIncluded = true,
            mealAllowanceBaseCents = 1400,
            mealAllowanceAmountCents = 820,
            createdAt = 1000000L,
            updatedAt = 1000000L
        )

        mockReadModifyWrite(date, existingEntry)

        val result = setDayType.invoke(date, DayType.OFF)

        assertEquals(DayType.OFF, result.dayType)
        assertFalse(result.mealIsArrivalDeparture)
        assertFalse(result.mealBreakfastIncluded)
        assertEquals(0, result.mealAllowanceBaseCents)
        assertEquals(0, result.mealAllowanceAmountCents)

        coVerify { workEntryDao.readModifyWrite(date, any()) }
    }

    @Test
    fun `invoke - OFF zu WORK - Aktualisiert korrekt`() = runTest {
        val date = LocalDate.now()
        val existingEntry = WorkEntry(
            date = date,
            dayType = DayType.OFF,
            mealIsArrivalDeparture = true,
            mealBreakfastIncluded = true,
            mealAllowanceBaseCents = 1400,
            mealAllowanceAmountCents = 820,
            createdAt = 1000000L,
            updatedAt = 1000000L
        )

        mockReadModifyWrite(date, existingEntry)

        val result = setDayType.invoke(date, DayType.WORK)

        assertEquals(DayType.WORK, result.dayType)
        assertFalse(result.mealIsArrivalDeparture)
        assertFalse(result.mealBreakfastIncluded)
        assertEquals(0, result.mealAllowanceBaseCents)
        assertEquals(0, result.mealAllowanceAmountCents)

        coVerify { workEntryDao.readModifyWrite(date, any()) }
    }

    @Test
    fun `invoke - WORK zu COMP_TIME - Setzt confirmedWorkDay und confirmationSource`() = runTest {
        val date = LocalDate.now()
        val existingEntry = WorkEntry(
            date = date,
            dayType = DayType.WORK,
            confirmedWorkDay = false,
            mealIsArrivalDeparture = true,
            mealBreakfastIncluded = true,
            mealAllowanceBaseCents = 1400,
            mealAllowanceAmountCents = 820,
            createdAt = 1000000L,
            updatedAt = 1000000L
        )
        mockReadModifyWrite(date, existingEntry)

        val result = setDayType.invoke(date, DayType.COMP_TIME)

        assertEquals(DayType.COMP_TIME, result.dayType)
        assertTrue(result.confirmedWorkDay)
        assertEquals(DayType.COMP_TIME.name, result.confirmationSource)
        assertNotNull(result.confirmationAt)
        assertFalse(result.mealIsArrivalDeparture)
        assertFalse(result.mealBreakfastIncluded)
        assertEquals(0, result.mealAllowanceBaseCents)
        assertEquals(0, result.mealAllowanceAmountCents)
        coVerify { workEntryDao.readModifyWrite(date, any()) }
    }

    @Test
    fun `invoke - OFF zu COMP_TIME - Setzt confirmedWorkDay und confirmationSource`() = runTest {
        val date = LocalDate.now()
        val existingEntry = WorkEntry(
            date = date,
            dayType = DayType.OFF,
            confirmedWorkDay = false,
            createdAt = 1000000L,
            updatedAt = 1000000L
        )
        mockReadModifyWrite(date, existingEntry)

        val result = setDayType.invoke(date, DayType.COMP_TIME)

        assertEquals(DayType.COMP_TIME, result.dayType)
        assertTrue(result.confirmedWorkDay)
        assertEquals(DayType.COMP_TIME.name, result.confirmationSource)
        assertNotNull(result.confirmationAt)
        coVerify { workEntryDao.readModifyWrite(date, any()) }
    }

    @Test
    fun `invoke - COMP_TIME zu WORK - Loescht confirmedWorkDay und confirmationSource`() = runTest {
        val date = LocalDate.now()
        val existingEntry = WorkEntry(
            date = date,
            dayType = DayType.COMP_TIME,
            confirmedWorkDay = true,
            confirmationAt = 9000000L,
            confirmationSource = DayType.COMP_TIME.name,
            createdAt = 1000000L,
            updatedAt = 1000000L
        )
        mockReadModifyWrite(date, existingEntry)

        val result = setDayType.invoke(date, DayType.WORK)

        assertEquals(DayType.WORK, result.dayType)
        assertFalse(result.confirmedWorkDay)
        assertNull(result.confirmationAt)
        assertNull(result.confirmationSource)
        coVerify { workEntryDao.readModifyWrite(date, any()) }
    }

    @Test
    fun `invoke - COMP_TIME zu OFF - Loescht confirmedWorkDay und confirmationSource`() = runTest {
        val date = LocalDate.now()
        val existingEntry = WorkEntry(
            date = date,
            dayType = DayType.COMP_TIME,
            confirmedWorkDay = true,
            confirmationAt = 9000000L,
            confirmationSource = DayType.COMP_TIME.name,
            createdAt = 1000000L,
            updatedAt = 1000000L
        )
        mockReadModifyWrite(date, existingEntry)

        val result = setDayType.invoke(date, DayType.OFF)

        assertEquals(DayType.OFF, result.dayType)
        assertFalse(result.confirmedWorkDay)
        assertNull(result.confirmationAt)
        assertNull(result.confirmationSource)
        coVerify { workEntryDao.readModifyWrite(date, any()) }
    }
}
