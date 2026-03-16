package de.montagezeit.app.ui.screen.history

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.preferences.ReminderSettings
import de.montagezeit.app.data.preferences.ReminderSettingsManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testScheduler = TestCoroutineScheduler()
    private val dispatcher = UnconfinedTestDispatcher(testScheduler)
    private val workEntryDao = mockk<WorkEntryDao>(relaxed = true)
    private val reminderSettingsManager = mockk<ReminderSettingsManager>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { reminderSettingsManager.settings } returns flowOf(ReminderSettings())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `applyBatchEdit clears meal allowance when switching existing work entry to off`() {
        val date = LocalDate.of(2026, 3, 12)
        val existing = WorkEntry(
            date = date,
            dayType = DayType.WORK,
            dayLocationLabel = "Berlin",
            mealIsArrivalDeparture = true,
            mealBreakfastIncluded = true,
            mealAllowanceBaseCents = 1400,
            mealAllowanceAmountCents = 820
        )
        val savedEntries = mutableListOf<WorkEntry>()
        val resultLatch = CountDownLatch(1)

        coEvery { workEntryDao.getByDateRange(any(), any()) } answers {
            val start = firstArg<LocalDate>()
            val end = secondArg<LocalDate>()
            if (start == date && end == date) listOf(existing) else emptyList()
        }
        coEvery { workEntryDao.upsertAll(any()) } answers {
            savedEntries += firstArg<List<WorkEntry>>()
            Unit
        }

        val viewModel = HistoryViewModel(workEntryDao, reminderSettingsManager)

        var success: Boolean? = null
        viewModel.applyBatchEdit(
            request = BatchEditRequest(
                startDate = date,
                endDate = date,
                dayType = DayType.OFF,
                applyDefaultTimes = false,
                applyNote = false,
                note = null
            ),
            onResult = {
                success = it
                resultLatch.countDown()
            }
        )

        assertTrue(resultLatch.await(2, TimeUnit.SECONDS))
        assertEquals(true, success)
        assertEquals(1, savedEntries.size)
        val saved = savedEntries.single()
        assertEquals(DayType.OFF, saved.dayType)
        assertFalse(saved.mealIsArrivalDeparture)
        assertFalse(saved.mealBreakfastIncluded)
        assertEquals(0, saved.mealAllowanceBaseCents)
        assertEquals(0, saved.mealAllowanceAmountCents)
        coVerify(atLeast = 1) { workEntryDao.upsertAll(any()) }
    }
}
