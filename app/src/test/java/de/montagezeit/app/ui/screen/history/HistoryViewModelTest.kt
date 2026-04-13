package de.montagezeit.app.ui.screen.history

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import de.montagezeit.app.R
import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.preferences.ReminderSettings
import de.montagezeit.app.data.preferences.ReminderSettingsManager
import de.montagezeit.app.domain.usecase.testRepository
import de.montagezeit.app.ui.util.UiText
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher()
    private val workEntryDao = mockk<WorkEntryDao>(relaxed = true)
    private val reminderSettingsManager = mockk<ReminderSettingsManager>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { reminderSettingsManager.settings } returns flowOf(ReminderSettings())
        every { workEntryDao.getAllWithTravelFlow() } returns emptyFlow()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `applyBatchEdit rejects invalid range before DAO work`() = runTest {
        val viewModel = createViewModel()
        val request = BatchEditRequest(
            startDate = LocalDate.of(2026, 3, 15),
            endDate = LocalDate.of(2026, 3, 14),
            dayType = DayType.WORK,
            applyDefaultTimes = false,
            dayLocationLabel = null,
            applyDayLocation = false,
            note = null,
            applyNote = false
        )

        viewModel.applyBatchEdit(request)

        assertEquals(
            BatchEditState.Failure(UiText.StringResource(R.string.history_batch_invalid_range)),
            viewModel.batchEditState.value
        )
        coVerify(exactly = 0) { workEntryDao.getByDateRange(any(), any()) }
    }

    @Test
    fun `applyBatchEdit rejects no-op request before DAO work`() = runTest {
        val viewModel = createViewModel()
        val today = LocalDate.of(2026, 3, 15)

        viewModel.applyBatchEdit(
            BatchEditRequest(
                startDate = today,
                endDate = today,
                dayType = null,
                applyDefaultTimes = false,
                dayLocationLabel = null,
                applyDayLocation = false,
                note = null,
                applyNote = false
            )
        )

        assertEquals(
            BatchEditState.Failure(UiText.StringResource(R.string.history_batch_select_action)),
            viewModel.batchEditState.value
        )
        coVerify(exactly = 0) { workEntryDao.getByDateRange(any(), any()) }
    }

    @Test
    fun `applyBatchEdit reports no changes when request produces no updates`() = runTest {
        val date = LocalDate.of(2026, 3, 12)
        val existing = WorkEntry(
            date = date,
            dayType = DayType.WORK,
            dayLocationLabel = "Berlin",
            note = "Bereits vorhanden"
        )
        coEvery { workEntryDao.getByDateRange(date, date) } returns listOf(existing)

        val viewModel = createViewModel()
        viewModel.applyBatchEdit(
            BatchEditRequest(
                startDate = date,
                endDate = date,
                dayType = DayType.WORK,
                applyDefaultTimes = false,
                dayLocationLabel = null,
                applyDayLocation = false,
                note = null,
                applyNote = false
            )
        )
        waitUntil { viewModel.batchEditState.value is BatchEditState.Failure }

        assertEquals(
            BatchEditState.Failure(UiText.StringResource(R.string.history_batch_no_changes)),
            viewModel.batchEditState.value
        )
        coVerify(exactly = 0) { workEntryDao.upsertAllAndDeleteTravelLegs(any(), any()) }
    }

    @Test
    fun `applyBatchEdit clears meal allowance when switching existing work entry to off`() = runTest {
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
        val deletedTravelDates = mutableListOf<LocalDate>()

        coEvery { workEntryDao.getByDateRange(date, date) } returns listOf(existing)
        coEvery { workEntryDao.upsertAllAndDeleteTravelLegs(any(), any()) } answers {
            @Suppress("UNCHECKED_CAST")
            savedEntries += args[0] as List<WorkEntry>
            @Suppress("UNCHECKED_CAST")
            deletedTravelDates += args[1] as List<LocalDate>
            Unit
        }

        val viewModel = createViewModel()
        viewModel.applyBatchEdit(
            BatchEditRequest(
                startDate = date,
                endDate = date,
                dayType = DayType.OFF,
                applyDefaultTimes = false,
                dayLocationLabel = null,
                applyDayLocation = false,
                applyNote = false,
                note = null
            )
        )
        waitUntil { viewModel.batchEditState.value is BatchEditState.Success }

        assertTrue(viewModel.batchEditState.value is BatchEditState.Success)
        assertEquals(1, savedEntries.size)
        val saved = savedEntries.single()
        assertEquals(DayType.OFF, saved.dayType)
        assertEquals(true, saved.confirmedWorkDay)
        assertEquals(false, saved.mealIsArrivalDeparture)
        assertEquals(false, saved.mealBreakfastIncluded)
        assertEquals(0, saved.mealAllowanceBaseCents)
        assertEquals(0, saved.mealAllowanceAmountCents)
        assertTrue(deletedTravelDates.isEmpty())
        coVerify(atLeast = 1) { workEntryDao.upsertAllAndDeleteTravelLegs(any(), any()) }
    }

    @Test
    fun `applyBatchEdit rejects work transition without day location`() = runTest {
        val date = LocalDate.of(2026, 3, 12)
        val existing = WorkEntry(
            date = date,
            dayType = DayType.OFF,
            dayLocationLabel = ""
        )
        coEvery { workEntryDao.getByDateRange(date, date) } returns listOf(existing)

        val viewModel = createViewModel()
        viewModel.applyBatchEdit(
            BatchEditRequest(
                startDate = date,
                endDate = date,
                dayType = DayType.WORK,
                applyDefaultTimes = false,
                dayLocationLabel = null,
                applyDayLocation = false,
                note = null,
                applyNote = false
            )
        )
        waitUntil { viewModel.batchEditState.value is BatchEditState.Failure }

        assertEquals(
            BatchEditState.Failure(UiText.StringResource(R.string.history_batch_work_requires_location)),
            viewModel.batchEditState.value
        )
        coVerify(exactly = 0) { workEntryDao.upsertAllAndDeleteTravelLegs(any(), any()) }
    }

    @Test
    fun `applyBatchEdit rejects ranges with missing dates`() = runTest {
        val start = LocalDate.of(2026, 3, 12)
        val end = LocalDate.of(2026, 3, 14)
        coEvery {
            workEntryDao.getByDateRange(start, end)
        } returns listOf(WorkEntry(date = start, dayType = DayType.OFF))

        val viewModel = createViewModel()
        viewModel.applyBatchEdit(
            BatchEditRequest(
                startDate = start,
                endDate = end,
                dayType = null,
                applyDefaultTimes = false,
                dayLocationLabel = null,
                applyDayLocation = false,
                note = "Nur bestehende Einträge",
                applyNote = true
            )
        )
        waitUntil { viewModel.batchEditState.value is BatchEditState.Failure }

        assertEquals(
            BatchEditState.Failure(UiText.StringResource(R.string.history_batch_missing_entries)),
            viewModel.batchEditState.value
        )
        coVerify(exactly = 0) { workEntryDao.upsertAllAndDeleteTravelLegs(any(), any()) }
    }

    @Test
    fun `applyBatchEdit can set day location while switching to work`() = runTest {
        val date = LocalDate.of(2026, 3, 12)
        val existing = WorkEntry(date = date, dayType = DayType.OFF, dayLocationLabel = "")
        val savedEntries = mutableListOf<WorkEntry>()

        coEvery { workEntryDao.getByDateRange(date, date) } returns listOf(existing)
        coEvery { workEntryDao.upsertAllAndDeleteTravelLegs(any(), any()) } answers {
            @Suppress("UNCHECKED_CAST")
            savedEntries += args[0] as List<WorkEntry>
            Unit
        }

        val viewModel = createViewModel()
        viewModel.applyBatchEdit(
            BatchEditRequest(
                startDate = date,
                endDate = date,
                dayType = DayType.WORK,
                applyDefaultTimes = false,
                dayLocationLabel = "Berlin",
                applyDayLocation = true,
                note = null,
                applyNote = false
            )
        )
        waitUntil { viewModel.batchEditState.value is BatchEditState.Success }

        assertEquals(1, savedEntries.size)
        assertEquals("Berlin", savedEntries.single().dayLocationLabel)
        assertEquals(DayType.WORK, savedEntries.single().dayType)
    }

    @Test
    fun `applyBatchEdit deletes travel legs when switching range to comp time`() = runTest {
        val date = LocalDate.of(2026, 3, 12)
        val existing = WorkEntry(date = date, dayType = DayType.WORK, dayLocationLabel = "Berlin")
        val deletedTravelDates = mutableListOf<LocalDate>()

        coEvery { workEntryDao.getByDateRange(date, date) } returns listOf(existing)
        coEvery { workEntryDao.upsertAllAndDeleteTravelLegs(any(), any()) } answers {
            @Suppress("UNCHECKED_CAST")
            deletedTravelDates += args[1] as List<LocalDate>
            Unit
        }

        val viewModel = createViewModel()
        viewModel.applyBatchEdit(
            BatchEditRequest(
                startDate = date,
                endDate = date,
                dayType = DayType.COMP_TIME,
                applyDefaultTimes = false,
                dayLocationLabel = null,
                applyDayLocation = false,
                note = null,
                applyNote = false
            )
        )
        waitUntil { viewModel.batchEditState.value is BatchEditState.Success }

        assertEquals(listOf(date), deletedTravelDates)
    }

    @Test
    fun `uiState observes all entries without a rolling cutoff`() = runTest {
        every { workEntryDao.getAllWithTravelFlow() } returns flowOf(emptyList())

        val viewModel = createViewModel()
        val collectJob = backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        io.mockk.verify(exactly = 1) { workEntryDao.getAllWithTravelFlow() }
        collectJob.cancel()
    }

    private fun waitUntil(timeoutMs: Long = 2_000L, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!condition() && System.currentTimeMillis() < deadline) {
            Thread.sleep(25)
        }
        assertTrue(condition())
    }

    private fun createViewModel(): HistoryViewModel {
        val repository = testRepository(workEntryDao)
        return HistoryViewModel(
            workEntryRepository = repository,
            reminderSettingsManager = reminderSettingsManager
        )
    }
}
