package de.montagezeit.app.ui.screen.today

import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.TravelLeg
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.local.entity.WorkEntryWithTravelLegs
import de.montagezeit.app.data.repository.WorkEntryRepository
import de.montagezeit.app.domain.usecase.DeletedDaySnapshot
import de.montagezeit.app.domain.usecase.DeleteDayEntry
import de.montagezeit.app.domain.usecase.GetWorkEntriesByDateRange
import de.montagezeit.app.domain.usecase.GetWorkEntryByDate
import de.montagezeit.app.domain.usecase.ObserveWorkEntryByDate
import de.montagezeit.app.domain.usecase.ObserveWorkEntryWithTravelByDate
import de.montagezeit.app.domain.usecase.RecordDailyManualCheckIn
import de.montagezeit.app.domain.usecase.ReplaceWorkEntryWithTravelLegs
import de.montagezeit.app.domain.usecase.ResolveDayLocationPrefill
import de.montagezeit.app.domain.util.WeekCalculator
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModelTest {

    private val testScheduler = TestCoroutineScheduler()
    private val mainDispatcher = UnconfinedTestDispatcher(testScheduler)
    private val createdViewModels = mutableListOf<TodayViewModel>()

    @Before
    fun setup() {
        Dispatchers.setMain(mainDispatcher)
    }

    @After
    fun tearDown() {
        createdViewModels.forEach { it.viewModelScope.cancel() }
        createdViewModels.clear()
        Dispatchers.resetMain()
        io.mockk.clearAllMocks()
    }

    @Test
    fun `ensureTodayEntryThen opens callback without creating entry when missing`() {
        val repository = mockk<WorkEntryRepository>(relaxed = true)

        val viewModel = createViewModel(repository)
        val latch = CountDownLatch(1)

        viewModel.ensureTodayEntryThen { latch.countDown() }

        assertTrue(latch.await(2, TimeUnit.SECONDS))
        coVerify(exactly = 0) { repository.upsert(any()) }
    }

    @Test
    fun `ensureTodayEntryThen does not touch dao when entry exists`() {
        val repository = mockk<WorkEntryRepository>(relaxed = true)

        val viewModel = createViewModel(repository)
        val latch = CountDownLatch(1)

        viewModel.ensureTodayEntryThen { latch.countDown() }

        assertTrue(latch.await(2, TimeUnit.SECONDS))
        coVerify(exactly = 0) { repository.upsert(any()) }
    }

    @Test
    fun `openDailyCheckInDialog uses todays label first`() {
        val today = LocalDate.now()
        val existing = WorkEntry(
            date = today,
            dayType = DayType.WORK,
            dayLocationLabel = "Baustelle Heute"
        )
        val repository = mockk<WorkEntryRepository>(relaxed = true)

        coEvery { repository.getByDate(any()) } returns existing
        coEvery { repository.getLatestDayLocationLabelByDayType(any()) } returns null

        val viewModel = createViewModel(repository)
        val shownLatch = CountDownLatch(1)
        val collectJob = CoroutineScope(Dispatchers.Main).launch {
            viewModel.showDailyCheckInDialog.collect { shown ->
                if (shown) {
                    shownLatch.countDown()
                }
            }
        }

        viewModel.openDailyCheckInDialog()

        assertTrue(shownLatch.await(2, TimeUnit.SECONDS))
        assertEquals("Baustelle Heute", viewModel.dailyCheckInLocationInput.value)
        coVerify(exactly = 0) { repository.getLatestDayLocationLabelByDayType(any()) }
        collectJob.cancel()
    }

    @Test
    fun `openDailyCheckInDialog falls back to last used label when empty`() {
        val today = LocalDate.now()
        val existing = WorkEntry(
            date = today,
            dayType = DayType.WORK,
            dayLocationLabel = ""
        )
        val repository = mockk<WorkEntryRepository>(relaxed = true)

        coEvery { repository.getByDate(any()) } returns existing
        coEvery { repository.getLatestDayLocationLabelByDayType(DayType.WORK) } returns "Letzte Baustelle"

        val viewModel = createViewModel(repository)
        val shownLatch = CountDownLatch(1)
        val collectJob = CoroutineScope(Dispatchers.Main).launch {
            viewModel.showDailyCheckInDialog.collect { shown ->
                if (shown) {
                    shownLatch.countDown()
                }
            }
        }

        viewModel.openDailyCheckInDialog()

        assertTrue(shownLatch.await(2, TimeUnit.SECONDS))
        assertEquals("Letzte Baustelle", viewModel.dailyCheckInLocationInput.value)
        coVerify(exactly = 1) { repository.getLatestDayLocationLabelByDayType(DayType.WORK) }
        collectJob.cancel()
    }

    @Test
    fun `openDailyCheckInDialog falls back to empty string when no location known`() {
        val today = LocalDate.now()
        val existing = WorkEntry(
            date = today,
            dayType = DayType.WORK,
            dayLocationLabel = ""
        )
        val repository = mockk<WorkEntryRepository>(relaxed = true)

        coEvery { repository.getByDate(any()) } returns existing
        coEvery { repository.getLatestDayLocationLabelByDayType(DayType.WORK) } returns null

        val viewModel = createViewModel(repository)
        val shownLatch = CountDownLatch(1)
        val collectJob = CoroutineScope(Dispatchers.Main).launch {
            viewModel.showDailyCheckInDialog.collect { shown ->
                if (shown) {
                    shownLatch.countDown()
                }
            }
        }

        viewModel.openDailyCheckInDialog()

        assertTrue(shownLatch.await(2, TimeUnit.SECONDS))
        assertEquals("", viewModel.dailyCheckInLocationInput.value)
        collectJob.cancel()
    }

    @Test
    fun `onResetError retries loading selected date`() = runTest {
        val today = LocalDate.now()
        val recoveredEntry = WorkEntry(
            date = today,
            dayType = DayType.WORK,
            dayLocationLabel = "Wieder geladen"
        )
        val repository = mockk<WorkEntryRepository>(relaxed = true)
        var calls = 0

        coEvery { repository.getByDate(any()) } answers {
            calls += 1
            if (calls == 1) throw IllegalStateException("boom")
            recoveredEntry
        }

        val viewModel = createViewModel(repository)

        waitUntil { viewModel.uiState.value is TodayUiState.Error }

        viewModel.onResetError()

        waitUntil { viewModel.uiState.value == TodayUiState.Success(recoveredEntry) }
        assertEquals(TodayUiState.Success(recoveredEntry), viewModel.uiState.value)
    }

    @Test
    fun `submitDailyManualCheckIn calls usecase and closes dialog`() {
        val today = LocalDate.now()
        val savedEntry = WorkEntry(
            date = today,
            dayType = DayType.WORK,
            dayLocationLabel = "Gespeichert"
        )
        val repository = mockk<WorkEntryRepository>(relaxed = true)
        val recordDailyManualCheckIn = mockk<RecordDailyManualCheckIn>()

        coEvery { repository.getByDate(any()) } returns null
        coEvery { recordDailyManualCheckIn(any()) } returns savedEntry

        val viewModel = createViewModel(
            repository = repository,
            recordDailyManualCheckIn = recordDailyManualCheckIn
        )

        val successLatch = CountDownLatch(1)
        val collectJob = CoroutineScope(Dispatchers.Main).launch {
            viewModel.uiState.collect { state ->
                if (state is TodayUiState.Success && state.entry == savedEntry) {
                    successLatch.countDown()
                }
            }
        }

        viewModel.onDailyCheckInLocationChanged("Baustelle A")
        viewModel.submitDailyManualCheckIn()

        assertTrue(successLatch.await(2, TimeUnit.SECONDS))
        assertEquals(false, viewModel.showDailyCheckInDialog.value)
        assertEquals("Gespeichert", viewModel.dailyCheckInLocationInput.value)
        coVerify(exactly = 1) { recordDailyManualCheckIn(match { it.date == today && it.dayLocationLabel == "Baustelle A" }) }
        collectJob.cancel()
    }

    @Test
    fun `submitDailyManualCheckIn surfaces security errors as snackbar without location flow`() {
        val today = LocalDate.now()
        val repository = mockk<WorkEntryRepository>(relaxed = true)
        val recordDailyManualCheckIn = mockk<RecordDailyManualCheckIn>()

        coEvery { repository.getByDate(any()) } returns null
        coEvery { recordDailyManualCheckIn(any()) } throws SecurityException("Location permission missing")

        val viewModel = createViewModel(
            repository = repository,
            recordDailyManualCheckIn = recordDailyManualCheckIn
        )
        val snackbarLatch = CountDownLatch(1)
        val collectJob = CoroutineScope(Dispatchers.Main).launch {
            viewModel.snackbarMessage.collect { message ->
                if (message != null) {
                    snackbarLatch.countDown()
                }
            }
        }

        viewModel.onDailyCheckInLocationChanged("Baustelle B")
        viewModel.submitDailyManualCheckIn()

        coVerify(exactly = 1) { recordDailyManualCheckIn(match { it.date == today && it.dayLocationLabel == "Baustelle B" }) }
        assertTrue(snackbarLatch.await(2, TimeUnit.SECONDS))
        assertTrue(viewModel.uiState.value !is TodayUiState.Error)
        collectJob.cancel()
    }

    @Test
    fun `selectDate updates selectedDate even when same date is selected`() {
        val today = LocalDate.now()
        val repository = mockk<WorkEntryRepository>(relaxed = true)

        coEvery { repository.getByDate(any()) } returns null

        val viewModel = createViewModel(repository)
        val successLatch = CountDownLatch(1)
        val collectJob = CoroutineScope(Dispatchers.Main).launch {
            var loadingSeen = false
            viewModel.uiState.collect { state ->
                if (state is TodayUiState.Loading) {
                    loadingSeen = true
                }
                if (loadingSeen && state is TodayUiState.Success) {
                    successLatch.countDown()
                }
            }
        }

        // Initially selectedDate should be today
        assertEquals(today, viewModel.selectedDate.value)

        // Select today again (should not fail or return early without updating UI)
        viewModel.selectDate(today)

        // selectedDate should still be today
        assertEquals(today, viewModel.selectedDate.value)
        assertTrue(successLatch.await(2, TimeUnit.SECONDS))

        // Week days UI should reflect selection
        val weekDays = viewModel.weekDaysUi.value
        if (weekDays.isNotEmpty()) {
            val todayChip = weekDays.find { it.date == today }
            assertTrue("Today should be marked as selected", todayChip?.isSelected == true)
        }
        collectJob.cancel()
    }

    @Test
    fun `selectDate switches between different dates correctly`() {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        val todayEntry = WorkEntry(date = today, dayType = DayType.WORK, dayLocationLabel = "Heute")
        val yesterdayEntry = WorkEntry(date = yesterday, dayType = DayType.WORK, dayLocationLabel = "Gestern")
        val repository = mockk<WorkEntryRepository>(relaxed = true)

        coEvery { repository.getByDate(today) } returns todayEntry
        coEvery { repository.getByDate(yesterday) } returns yesterdayEntry

        val viewModel = createViewModel(repository)

        assertEquals(today, viewModel.selectedDate.value)
        waitUntil { viewModel.uiState.value == TodayUiState.Success(todayEntry) }

        viewModel.selectDate(yesterday)
        assertEquals(yesterday, viewModel.selectedDate.value)
        waitUntil { viewModel.uiState.value == TodayUiState.Success(yesterdayEntry) }

        viewModel.selectDate(today)
        assertEquals(today, viewModel.selectedDate.value)
        waitUntil { viewModel.uiState.value == TodayUiState.Success(todayEntry) }
    }

    @Test
    fun `stale initial load does not overwrite a newer selected date`() {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        val todayEntry = WorkEntry(date = today, dayType = DayType.WORK, dayLocationLabel = "Heute")
        val yesterdayEntry = WorkEntry(date = yesterday, dayType = DayType.WORK, dayLocationLabel = "Gestern")
        val repository = mockk<WorkEntryRepository>(relaxed = true)
        val initialLoadStarted = CountDownLatch(1)
        val releaseInitialLoad = CountDownLatch(1)
        val todayCalls = AtomicInteger(0)

        coEvery { repository.getByDate(today) } coAnswers {
            if (todayCalls.incrementAndGet() == 1) {
                initialLoadStarted.countDown()
                assertTrue(releaseInitialLoad.await(2, TimeUnit.SECONDS))
            }
            todayEntry
        }
        coEvery { repository.getByDate(yesterday) } returns yesterdayEntry

        val viewModel = createViewModel(repository)

        assertTrue(initialLoadStarted.await(2, TimeUnit.SECONDS))

        viewModel.selectDate(yesterday)
        waitUntil { viewModel.uiState.value == TodayUiState.Success(yesterdayEntry) }

        releaseInitialLoad.countDown()
        Thread.sleep(200)

        assertEquals(yesterday, viewModel.selectedDate.value)
        assertEquals(TodayUiState.Success(yesterdayEntry), viewModel.uiState.value)
    }

    @Test
    fun `selectDate rebuilds week overview from selected date`() {
        val today = LocalDate.now()
        val otherWeekDate = today.plusWeeks(1)
        val repository = mockk<WorkEntryRepository>(relaxed = true)

        coEvery { repository.getByDate(any()) } returns null

        val viewModel = createViewModel(repository)
        val weekLatch = CountDownLatch(1)
        val collectJob = CoroutineScope(Dispatchers.Main).launch {
            viewModel.weekDaysUi.collect { days ->
                if (
                    days.firstOrNull()?.date == WeekCalculator.weekStart(otherWeekDate) &&
                    days.any { it.date == otherWeekDate && it.isSelected }
                ) {
                    weekLatch.countDown()
                }
            }
        }

        viewModel.selectDate(otherWeekDate)

        assertTrue(weekLatch.await(2, TimeUnit.SECONDS))
        val weekDays = viewModel.weekDaysUi.value
        assertEquals(WeekCalculator.weekStart(otherWeekDate), weekDays.first().date)
        assertTrue(weekDays.any { it.date == otherWeekDate && it.isSelected })
        assertTrue(weekDays.none { it.isToday })
        collectJob.cancel()
    }

    @Test
    fun `confirmDeleteDay removes entry and exposes it for undo`() {
        val today = LocalDate.now()
        val existingEntry = WorkEntry(date = today, dayType = DayType.WORK)
        val existingSnapshot = DeletedDaySnapshot(existingEntry, emptyList())
        val repository = mockk<WorkEntryRepository>(relaxed = true)

        coEvery { repository.getByDateWithTravel(today) } returns record(existingEntry)

        val viewModel = createViewModel(repository)

        val deletedLatch = CountDownLatch(1)
        val collectJob = CoroutineScope(Dispatchers.Main).launch {
            viewModel.deletedEntryForUndo.collect { entry ->
                if (entry != null) deletedLatch.countDown()
            }
        }

        viewModel.confirmDeleteDay()

        assertTrue(deletedLatch.await(2, TimeUnit.SECONDS))
        assertEquals(existingSnapshot, viewModel.deletedEntryForUndo.value)
        assertTrue(viewModel.uiState.value is TodayUiState.Success)
        assertEquals(null, (viewModel.uiState.value as TodayUiState.Success).entry)
        coVerify(exactly = 1) { repository.deleteByDate(today) }
        collectJob.cancel()
    }

    @Test
    fun `undoDeleteDay re-inserts the deleted entry`() {
        val today = LocalDate.now()
        val existingEntry = WorkEntry(date = today, dayType = DayType.WORK)
        val travelLegs = listOf(
            TravelLeg(
                workEntryDate = today,
                sortOrder = 0,
                startLabel = "A",
                endLabel = "B"
            )
        )
        val repository = mockk<WorkEntryRepository>(relaxed = true)

        coEvery { repository.getByDateWithTravel(today) } returns record(existingEntry, travelLegs)
        coEvery { repository.getByDate(today) } returns existingEntry

        val viewModel = createViewModel(repository)

        // Delete
        val deletedLatch = CountDownLatch(1)
        val collectJob = CoroutineScope(Dispatchers.Main).launch {
            viewModel.deletedEntryForUndo.collect { if (it != null) deletedLatch.countDown() }
        }
        viewModel.confirmDeleteDay()
        assertTrue(deletedLatch.await(2, TimeUnit.SECONDS))

        // Undo
        viewModel.undoDeleteDay()

        assertEquals(null, viewModel.deletedEntryForUndo.value)
        coVerify(exactly = 1) { repository.replaceEntryWithTravelLegs(existingEntry, travelLegs) }
        assertTrue(viewModel.uiState.value is TodayUiState.Success)
        assertEquals(existingEntry, (viewModel.uiState.value as TodayUiState.Success).entry)
        collectJob.cancel()
    }

    @Test
    fun `confirmDeleteDay does nothing when no entry exists`() {
        val today = LocalDate.now()
        val repository = mockk<WorkEntryRepository>(relaxed = true)

        coEvery { repository.getByDateWithTravel(today) } returns null

        val viewModel = createViewModel(repository)
        viewModel.confirmDeleteDay()

        // Short wait for coroutine to complete
        Thread.sleep(200)
        assertEquals(null, viewModel.deletedEntryForUndo.value)
        coVerify(exactly = 0) { repository.deleteByDate(any()) }
    }

    @Test
    fun `swipe navigation selectDate with plusDays and minusDays keeps final selection and queries final date`() {
        val today = LocalDate.now()
        val plusOneEntry = WorkEntry(date = today.plusDays(1), dayType = DayType.WORK, dayLocationLabel = "+1")
        val plusTwoEntry = WorkEntry(date = today.plusDays(2), dayType = DayType.WORK, dayLocationLabel = "+2")
        val yesterdayEntry = WorkEntry(date = today.minusDays(1), dayType = DayType.WORK, dayLocationLabel = "-1")
        val repository = mockk<WorkEntryRepository>(relaxed = true)

        coEvery { repository.getByDate(today) } returns WorkEntry(date = today, dayType = DayType.WORK, dayLocationLabel = "Heute")
        coEvery { repository.getByDate(today.plusDays(1)) } returns plusOneEntry
        coEvery { repository.getByDate(today.plusDays(2)) } returns plusTwoEntry
        coEvery { repository.getByDate(today.minusDays(1)) } returns yesterdayEntry

        val viewModel = createViewModel(repository)

        viewModel.selectDate(today.plusDays(1))
        assertEquals(today.plusDays(1), viewModel.selectedDate.value)

        viewModel.selectDate(today.plusDays(2))
        assertEquals(today.plusDays(2), viewModel.selectedDate.value)

        viewModel.selectDate(today.plusDays(1))
        assertEquals(today.plusDays(1), viewModel.selectedDate.value)

        viewModel.selectDate(today)
        viewModel.selectDate(today.minusDays(1))
        assertEquals(today.minusDays(1), viewModel.selectedDate.value)

        waitUntil { viewModel.uiState.value == TodayUiState.Success(yesterdayEntry) }
        coVerify(timeout = 2_000) { repository.getByDate(today.minusDays(1)) }
    }

    @Test
    fun `openDailyCheckInDialog prefills meal flags from existing entry`() = runTest {
        val today = LocalDate.now()
        val existing = WorkEntry(
            date = today,
            dayType = DayType.WORK,
            dayLocationLabel = "Baustelle",
            mealIsArrivalDeparture = true,
            mealBreakfastIncluded = true,
            mealAllowanceBaseCents = 1400,
            mealAllowanceAmountCents = 820
        )
        val repository = mockk<WorkEntryRepository>(relaxed = true)

        coEvery { repository.getByDate(any()) } returns existing

        val viewModel = createViewModel(repository)

        viewModel.openDailyCheckInDialog()

        assertTrue(viewModel.showDailyCheckInDialog.value)
        assertEquals(true, viewModel.dailyCheckInIsArrivalDeparture.value)
        assertEquals(true, viewModel.dailyCheckInBreakfastIncluded.value)
    }

    @Test
    fun `openDailyCheckInDialog defaults meal flags to false when no entry exists`() = runTest {
        val repository = mockk<WorkEntryRepository>(relaxed = true)

        coEvery { repository.getByDate(any()) } returns null
        coEvery { repository.getLatestDayLocationLabelByDayType(any()) } returns null

        val viewModel = createViewModel(repository)

        viewModel.openDailyCheckInDialog()

        assertTrue(viewModel.showDailyCheckInDialog.value)
        assertEquals(false, viewModel.dailyCheckInIsArrivalDeparture.value)
        assertEquals(false, viewModel.dailyCheckInBreakfastIncluded.value)
    }

    private fun createViewModel(
        repository: WorkEntryRepository = mockk(relaxed = true),
        recordDailyManualCheckIn: RecordDailyManualCheckIn = mockk(relaxed = true)
    ): TodayViewModel {
        every { repository.getByDateFlow(any()) } returns flowOf(null)
        every { repository.getByDateWithTravelFlow(any()) } returns flowOf(null)
        coEvery { repository.getByDateRange(any(), any()) } returns emptyList()

        val actionsHandler = TodayActionsHandler(
            recordDailyManualCheckIn = recordDailyManualCheckIn,
            confirmOffDay = mockk(relaxed = true),
            setDayLocation = mockk(relaxed = true),
            deleteDayEntry = DeleteDayEntry(repository),
            replaceWorkEntryWithTravelLegs = ReplaceWorkEntryWithTravelLegs(repository)
        )
        return TodayViewModel(
            observeWorkEntryByDate = ObserveWorkEntryByDate(repository),
            observeWorkEntryWithTravelByDate = ObserveWorkEntryWithTravelByDate(repository),
            getWorkEntryByDate = GetWorkEntryByDate(repository),
            getWorkEntriesByDateRange = GetWorkEntriesByDateRange(repository),
            resolveDayLocationPrefill = ResolveDayLocationPrefill(repository),
            dateCoordinator = TodayDateCoordinator(),
            weekOverviewUseCase = TodayWeekOverviewUseCase(),
            dialogsStateHolder = TodayDialogsStateHolder(),
            actionsHandler = actionsHandler
        ).also {
            createdViewModels.add(it)
            it.viewModelScope.launch { it.screenState.collect {} }
            it.viewModelScope.launch { it.dialogState.collect {} }
        }
    }

    private fun record(
        entry: WorkEntry,
        travelLegs: List<TravelLeg> = emptyList()
    ) = WorkEntryWithTravelLegs(
        workEntry = entry,
        travelLegs = travelLegs
    )

    private fun waitUntil(timeoutMs: Long = 2_000L, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!condition() && System.currentTimeMillis() < deadline) {
            Thread.sleep(25)
        }
        assertTrue(condition())
    }
}
