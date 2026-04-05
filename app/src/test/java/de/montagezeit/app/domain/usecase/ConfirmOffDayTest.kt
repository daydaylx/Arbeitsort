package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntry
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ConfirmOffDayTest {

    private val testScheduler = TestCoroutineScheduler()
    private val mainDispatcher = UnconfinedTestDispatcher(testScheduler)
    private val workEntryDao = mockk<WorkEntryDao>(relaxed = true)
    private val useCase = ConfirmOffDay(testRepository(workEntryDao))

    @Before
    fun setup() {
        Dispatchers.setMain(mainDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `invoke creates OFF entry with confirmation data when no entry exists`() = runTest {
        val date = LocalDate.now()
        coEvery { workEntryDao.upsert(any()) } returns Unit
        stubReadModifyWrite(workEntryDao, existingEntry = null)

        val result = useCase(date, source = "TEST")

        assertEquals(DayType.OFF, result.dayType)
        assertEquals("", result.dayLocationLabel)
        assertEquals(true, result.confirmedWorkDay)
        assertNotNull(result.confirmationAt)
        assertEquals("TEST", result.confirmationSource)

        coVerify(exactly = 1) { workEntryDao.upsert(result) }
    }

    @Test
    fun `invoke converts existing work entry to OFF and keeps manual day location`() = runTest {
        val date = LocalDate.now()
        val existing = WorkEntry(
            date = date,
            dayType = DayType.WORK,
            dayLocationLabel = "Berlin"
        )

        coEvery { workEntryDao.upsert(any()) } returns Unit
        stubReadModifyWrite(workEntryDao, existing)

        val result = useCase(date, source = "UI")

        assertEquals(DayType.OFF, result.dayType)
        assertEquals("Berlin", result.dayLocationLabel)
        assertFalse(result.mealIsArrivalDeparture)
        assertFalse(result.mealBreakfastIncluded)
        assertEquals(0, result.mealAllowanceBaseCents)
        assertEquals(0, result.mealAllowanceAmountCents)
        assertTrue(result.confirmedWorkDay)
        assertEquals("UI", result.confirmationSource)
    }

    @Test
    fun `invoke keeps day location blank when existing day location label is blank`() = runTest {
        val date = LocalDate.now()
        val existing = WorkEntry(
            date = date,
            dayType = DayType.WORK,
            dayLocationLabel = ""
        )

        coEvery { workEntryDao.upsert(any()) } returns Unit
        stubReadModifyWrite(workEntryDao, existing)

        val result = useCase(date)

        assertEquals(DayType.OFF, result.dayType)
        assertEquals("", result.dayLocationLabel)
    }
}
