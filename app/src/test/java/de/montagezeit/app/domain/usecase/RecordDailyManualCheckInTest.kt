package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.DayLocationSource
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.LocationStatus
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.preferences.ReminderSettings
import de.montagezeit.app.data.preferences.ReminderSettingsManager
import de.montagezeit.app.domain.util.MealAllowanceCalculator
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class RecordDailyManualCheckInTest {

    private lateinit var workEntryDao: WorkEntryDao
    private lateinit var reminderSettingsManager: ReminderSettingsManager
    private lateinit var useCase: RecordDailyManualCheckIn

    @Before
    fun setup() {
        workEntryDao = mockk()
        reminderSettingsManager = mockk()

        every { reminderSettingsManager.settings } returns flowOf(
            ReminderSettings(
                workStart = LocalTime.of(8, 0),
                workEnd = LocalTime.of(17, 0),
                breakMinutes = 45
            )
        )

        useCase = RecordDailyManualCheckIn(
            workEntryDao = workEntryDao,
            reminderSettingsManager = reminderSettingsManager
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun mockReadModifyWrite(date: LocalDate, existingEntry: WorkEntry?) {
        coEvery { workEntryDao.readModifyWrite(date, any()) } coAnswers {
            val modify = secondArg<(WorkEntry?) -> WorkEntry>()
            modify(existingEntry)
        }
    }

    @Test
    fun `invoke confirms work day without synthesizing snapshots`() = runTest {
        val date = LocalDate.now()
        mockReadModifyWrite(date, null)

        val result = useCase(DailyManualCheckInInput(date, "  Baustelle XY  "))

        assertEquals(DayType.WORK, result.dayType)
        assertEquals("Baustelle XY", result.dayLocationLabel)
        assertEquals(DayLocationSource.MANUAL, result.dayLocationSource)
        assertNull(result.morningCapturedAt)
        assertNull(result.eveningCapturedAt)
        assertEquals(true, result.confirmedWorkDay)
        assertFalse(result.confirmationAt == null)
        assertEquals("UI", result.confirmationSource)
        assertFalse(result.needsReview)

        coVerify(exactly = 1) { workEntryDao.readModifyWrite(date, any()) }
    }

    @Test
    fun `invoke throws for blank input`() = runTest {
        val date = LocalDate.now()
        val error = try {
            useCase(DailyManualCheckInInput(date, "   "))
            fail("Expected IllegalArgumentException")
            null
        } catch (e: IllegalArgumentException) {
            e
        }
        val thrown = requireNotNull(error)
        assertEquals("dayLocationLabel darf nicht leer sein", thrown.message)
        coVerify(exactly = 0) { workEntryDao.readModifyWrite(any(), any()) }
    }

    @Test
    fun `invoke keeps existing snapshot statuses when already captured`() = runTest {
        val date = LocalDate.now()
        val existing = WorkEntry(
            date = date,
            dayType = DayType.WORK,
            dayLocationLabel = "Alt",
            morningCapturedAt = 1_000L,
            morningLocationStatus = LocationStatus.OK,
            eveningCapturedAt = 2_000L,
            eveningLocationStatus = LocationStatus.LOW_ACCURACY
        )
        mockReadModifyWrite(date, existing)

        val result = useCase(DailyManualCheckInInput(date, "Neuer Ort"))

        assertEquals(LocationStatus.OK, result.morningLocationStatus)
        assertEquals(LocationStatus.LOW_ACCURACY, result.eveningLocationStatus)
        assertEquals(1_000L, result.morningCapturedAt)
        assertEquals(2_000L, result.eveningCapturedAt)
        assertEquals("Neuer Ort", result.dayLocationLabel)
    }

    @Test
    fun `invoke does not create snapshots on existing entry without captures`() = runTest {
        val date = LocalDate.now()
        val existing = WorkEntry(
            date = date,
            dayType = DayType.WORK,
            dayLocationLabel = "Alt"
        )
        mockReadModifyWrite(date, existing)

        val result = useCase(DailyManualCheckInInput(date, "Neuer Ort"))

        assertNull(result.morningCapturedAt)
        assertNull(result.eveningCapturedAt)
        assertEquals("Neuer Ort", result.dayLocationLabel)
        assertEquals(true, result.confirmedWorkDay)
    }

    @Test
    fun `invoke stores meal allowance for normal day without breakfast`() = runTest {
        val date = LocalDate.now()
        mockReadModifyWrite(date, null)

        val result = useCase(
            DailyManualCheckInInput(
                date = date,
                dayLocationLabel = "Baustelle",
                isArrivalDeparture = false,
                breakfastIncluded = false
            )
        )

        assertFalse(result.mealIsArrivalDeparture)
        assertFalse(result.mealBreakfastIncluded)
        assertEquals(MealAllowanceCalculator.BASE_NORMAL_CENTS, result.mealAllowanceBaseCents)
        assertEquals(MealAllowanceCalculator.BASE_NORMAL_CENTS, result.mealAllowanceAmountCents)
    }

    @Test
    fun `invoke stores meal allowance for arrival departure with breakfast`() = runTest {
        val date = LocalDate.now()
        mockReadModifyWrite(date, null)

        val result = useCase(
            DailyManualCheckInInput(
                date = date,
                dayLocationLabel = "Baustelle",
                isArrivalDeparture = true,
                breakfastIncluded = true
            )
        )

        assertEquals(true, result.mealIsArrivalDeparture)
        assertEquals(true, result.mealBreakfastIncluded)
        assertEquals(MealAllowanceCalculator.BASE_ARRIVAL_DEPARTURE_CENTS, result.mealAllowanceBaseCents)
        assertEquals(820, result.mealAllowanceAmountCents)
    }

    @Test
    fun `invoke updates meal allowance on existing entry`() = runTest {
        val date = LocalDate.now()
        val existing = WorkEntry(
            date = date,
            dayType = DayType.WORK,
            dayLocationLabel = "Alt",
            mealIsArrivalDeparture = false,
            mealBreakfastIncluded = false,
            mealAllowanceBaseCents = 0,
            mealAllowanceAmountCents = 0
        )
        mockReadModifyWrite(date, existing)

        val result = useCase(
            DailyManualCheckInInput(
                date = date,
                dayLocationLabel = "Neu",
                isArrivalDeparture = false,
                breakfastIncluded = true
            )
        )

        assertFalse(result.mealIsArrivalDeparture)
        assertEquals(true, result.mealBreakfastIncluded)
        assertEquals(2220, result.mealAllowanceAmountCents)
    }

    @Test
    fun `invoke preserves existing work schedule for existing WORK entry`() = runTest {
        val date = LocalDate.now()
        val existing = WorkEntry(
            date = date,
            dayType = DayType.WORK,
            dayLocationLabel = "Alt",
            workStart = LocalTime.of(6, 15),
            workEnd = LocalTime.of(14, 45),
            breakMinutes = 30
        )
        mockReadModifyWrite(date, existing)

        val result = useCase(
            DailyManualCheckInInput(
                date = date,
                dayLocationLabel = "Neu",
                isArrivalDeparture = false,
                breakfastIncluded = false
            )
        )

        assertEquals(LocalTime.of(6, 15), result.workStart)
        assertEquals(LocalTime.of(14, 45), result.workEnd)
        assertEquals(30, result.breakMinutes)
        assertEquals("Neu", result.dayLocationLabel)
    }
}
