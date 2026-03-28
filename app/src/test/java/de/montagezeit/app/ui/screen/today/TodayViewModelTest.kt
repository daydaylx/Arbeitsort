package de.montagezeit.app.ui.screen.today

import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.TravelLeg
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.local.entity.WorkEntryWithTravelLegs
import de.montagezeit.app.data.preferences.ReminderSettings
import de.montagezeit.app.data.preferences.ReminderSettingsManager
import de.montagezeit.app.domain.util.WeekCalculator
import de.montagezeit.app.domain.usecase.ConfirmOffDay
import de.montagezeit.app.domain.usecase.DeletedDaySnapshot
import de.montagezeit.app.domain.usecase.DeleteDayEntry
import de.montagezeit.app.domain.usecase.RecordDailyManualCheckIn
import de.montagezeit.app.domain.usecase.ResolveDayLocationPrefill
import de.montagezeit.app.domain.usecase.SetDayLocation
import de.montagezeit.app.domain.util.NonWorkingDayChecker
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

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
        val workEntryDao = mockk<WorkEntryDao>(relaxed = true)
        val settingsManager = mockk<ReminderSettingsManager>()

        every { workEntryDao.getByDateFlow(any()) } returns flowOf(null)
        coEvery { workEntryDao.getByDateRange(any(), any()) } returns emptyList()
        every { settingsManager.settings } returns flowOf(ReminderSettings())

        val viewModel = createViewModel(workEntryDao, settingsManager)
        val latch = CountDownLatch(1)

        viewModel.ensureTodayEntryThen { latch.countDown() }

        assertTrue(latch.await(2, TimeUnit.SECONDS))
        coVerify(exactly = 0) { workEntryDao.upsert(any()) }
    }

    @Test
    fun `ensureTodayEntryThen does not touch dao when entry exists`() {
        val workEntryDao = mockk<WorkEntryDao>(relaxed = true)
        val settingsManager = mockk<ReminderSettingsManager>()

        every { workEntryDao.getByDateFlow(any()) } returns flowOf(null)
        coEvery { workEntryDao.getByDateRange(any(), any()) } returns emptyList()
        every { settingsManager.settings } returns flowOf(ReminderSettings())

        val viewModel = createViewModel(workEntryDao, settingsManager)
        val latch = CountDownLatch(1)

        viewModel.ensureTodayEntryThen { latch.countDown() }

        assertTrue(latch.await(2, TimeUnit.SECONDS))
        coVerify(exactly = 0) { workEntryDao.upsert(any()) }
    }

    @Test
    fun `openDailyCheckInDialog uses todays label first`() {
        val today = LocalDate.now()
        val existing = WorkEntry(
            date = today,
            dayType = DayType.WORK,
            dayLocationLabel = "Baustelle Heute"
        )
        val workEntryDao = mockk<WorkEntryDao>(relaxed = true)
        val settingsManager = mockk<ReminderSettingsManager>()
        val settings = ReminderSettings()

        coEvery { workEntryDao.getByDate(any()) } returns existing
        every { workEntryDao.getByDateFlow(any()) } returns flowOf(null)
        coEvery { workEntryDao.getByDateRange(any(), any()) } returns emptyList()
        coEvery { workEntryDao.getLatestDayLocationLabelByDayType(any()) } returns null
        every { settingsManager.settings } returns flowOf(settings)

        val viewModel = createViewModel(workEntryDao, settingsManager)
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
        coVerify(exactly = 0) { workEntryDao.getLatestDayLocationLabelByDayType(any()) }
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
        val workEntryDao = mockk<WorkEntryDao>(relaxed = true)
        val settingsManager = mockk<ReminderSettingsManager>()
        val settings = ReminderSettings()

        coEvery { workEntryDao.getByDate(any()) } returns existing
        every { workEntryDao.getByDateFlow(any()) } returns flowOf(null)
        coEvery { workEntryDao.getByDateRange(any(), any()) } returns emptyList()
        coEvery { workEntryDao.getLatestDayLocationLabelByDayType(DayType.WORK) } returns "Letzte Baustelle"
        every { settingsManager.settings } returns flowOf(settings)

        val viewModel = createViewModel(workEntryDao, settingsManager)
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
        coVerify(exactly = 1) { workEntryDao.getLatestDayLocationLabelByDayType(DayType.WORK) }
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
        val workEntryDao = mockk<WorkEntryDao>(relaxed = true)
        val settingsManager = mockk<ReminderSettingsManager>()
        val settings = ReminderSettings()

        coEvery { workEntryDao.getByDate(any()) } returns existing
        every { workEntryDao.getByDateFlow(any()) } returns flowOf(null)
        coEvery { workEntryDao.getByDateRange(any(), any()) } returns emptyList()
        coEvery { workEntryDao.getLatestDayLocationLabelByDayType(DayType.WORK) } returns null
        every { settingsManager.settings } returns flowOf(settings)

        val viewModel = createViewModel(workEntryDao, settingsManager)
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
        val workEntryDao = mockk<WorkEntryDao>(relaxed = true)
        val settingsManager = mockk<ReminderSettingsManager>()
        var calls = 0

        coEvery { workEntryDao.getByDate(any()) } answers {
            calls += 1
            if (calls == 1) throw IllegalStateException("boom")
            recoveredEntry
        }
        every { workEntryDao.getByDateFlow(any()) } returns flowOf(null)
        every { workEntryDao.getByDateWithTravelFlow(any()) } returns flowOf(null)
        coEvery { workEntryDao.getByDateRange(any(), any()) } returns emptyList()
        coEvery { workEntryDao.getByDateRangeWithTravel(any(), any()) } returns emptyList()
        every { settingsManager.settings } returns flowOf(ReminderSettings())

        val viewModel = createViewModel(workEntryDao, settingsManager)

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
        val workEntryDao = mockk<WorkEntryDao>(relaxed = true)
        val settingsManager = mockk<ReminderSettingsManager>()
        val recordDailyManualCheckIn = mockk<RecordDailyManualCheckIn>()

        coEvery { workEntryDao.getByDate(any()) } returns null
        every { workEntryDao.getByDateFlow(any()) } returns flowOf(null)
        coEvery { workEntryDao.getByDateRange(any(), any()) } returns emptyList()
        every { settingsManager.settings } returns flowOf(ReminderSettings())
        coEvery { recordDailyManualCheckIn(any()) } returns savedEntry

        val viewModel = createViewModel(
            workEntryDao = workEntryDao,
            settingsManager = settingsManager,
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
        val workEntryDao = mockk<WorkEntryDao>(relaxed = true)
        val settingsManager = mockk<ReminderSettingsManager>()
        val recordDailyManualCheckIn = mockk<RecordDailyManualCheckIn>()

        coEvery { workEntryDao.getByDate(any()) } returns null
        every { workEntryDao.getByDateFlow(any()) } returns flowOf(null)
        coEvery { workEntryDao.getByDateRange(any(), any()) } returns emptyList()
        every { settingsManager.settings } returns flowOf(ReminderSettings())
        coEvery { recordDailyManualCheckIn(any()) } throws SecurityException("Location permission missing")

        val viewModel = createViewModel(
            workEntryDao = workEntryDao,
            settingsManager = settingsManager,
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
        val workEntryDao = mockk<WorkEntryDao>(relaxed = true)
        val settingsManager = mockk<ReminderSettingsManager>()

        coEvery { workEntryDao.getByDate(any()) } returns null
        every { workEntryDao.getByDateFlow(any()) } returns flowOf(null)
        coEvery { workEntryDao.getByDateRange(any(), any()) } returns emptyList()
        every { settingsManager.settings } returns flowOf(ReminderSettings())

        val viewModel = createViewModel(
            workEntryDao,
            settingsManager,
            entriesWithTravel = emptyList()
        )
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
        val workEntryDao = mockk<WorkEntryDao>(relaxed = true)
        val settingsManager = mockk<ReminderSettingsManager>()

        coEvery { workEntryDao.getByDate(any()) } returns null
        every { workEntryDao.getByDateFlow(any()) } returns flowOf(null)
        coEvery { workEntryDao.getByDateRange(any(), any()) } returns emptyList()
        every { settingsManager.settings } returns flowOf(ReminderSettings())

        val viewModel = createViewModel(
            workEntryDao,
            settingsManager,
            entriesWithTravel = emptyList()
        )

        // Initially today
        assertEquals(today, viewModel.selectedDate.value)

        // Switch to yesterday
        viewModel.selectDate(yesterday)
        assertEquals(yesterday, viewModel.selectedDate.value)

        // Switch back to today
        viewModel.selectDate(today)
        assertEquals(today, viewModel.selectedDate.value)

        // Verify DB was queried for both dates
        coVerify(atLeast = 1) { workEntryDao.getByDate(today) }
        coVerify(atLeast = 1) { workEntryDao.getByDate(yesterday) }
    }

    @Test
    fun `selectDate rebuilds week overview from selected date`() {
        val today = LocalDate.now()
        val otherWeekDate = today.plusWeeks(1)
        val workEntryDao = mockk<WorkEntryDao>(relaxed = true)
        val settingsManager = mockk<ReminderSettingsManager>()

        coEvery { workEntryDao.getByDate(any()) } returns null
        every { workEntryDao.getByDateFlow(any()) } returns flowOf(null)
        coEvery { workEntryDao.getByDateRange(any(), any()) } returns emptyList()
        every { settingsManager.settings } returns flowOf(ReminderSettings())

        val viewModel = createViewModel(
            workEntryDao,
            settingsManager,
            entriesWithTravel = emptyList()
        )
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
        val workEntryDao = mockk<WorkEntryDao>(relaxed = true)
        val settingsManager = mockk<ReminderSettingsManager>()

        coEvery { workEntryDao.getByDateWithTravel(today) } returns record(existingEntry)
        every { workEntryDao.getByDateFlow(any()) } returns flowOf(null)
        coEvery { workEntryDao.getByDateRange(any(), any()) } returns emptyList()
        coEvery { workEntryDao.deleteByDate(today) } returns Unit
        every { settingsManager.settings } returns flowOf(ReminderSettings())

        val viewModel = createViewModel(workEntryDao, settingsManager)

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
        coVerify(exactly = 1) { workEntryDao.deleteByDate(today) }
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
        val workEntryDao = mockk<WorkEntryDao>(relaxed = true)
        val settingsManager = mockk<ReminderSettingsManager>()

        coEvery { workEntryDao.getByDateWithTravel(today) } returns record(existingEntry, travelLegs)
        coEvery { workEntryDao.getByDate(today) } returns existingEntry
        every { workEntryDao.getByDateFlow(any()) } returns flowOf(null)
        coEvery { workEntryDao.getByDateRange(any(), any()) } returns emptyList()
        coEvery { workEntryDao.deleteByDate(today) } returns Unit
        coEvery { workEntryDao.replaceEntryWithTravelLegs(existingEntry, travelLegs) } returns Unit
        every { settingsManager.settings } returns flowOf(ReminderSettings())

        val viewModel = createViewModel(workEntryDao, settingsManager)

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
        coVerify(exactly = 1) { workEntryDao.replaceEntryWithTravelLegs(existingEntry, travelLegs) }
        assertTrue(viewModel.uiState.value is TodayUiState.Success)
        assertEquals(existingEntry, (viewModel.uiState.value as TodayUiState.Success).entry)
        collectJob.cancel()
    }

    @Test
    fun `confirmDeleteDay does nothing when no entry exists`() {
        val today = LocalDate.now()
        val workEntryDao = mockk<WorkEntryDao>(relaxed = true)
        val settingsManager = mockk<ReminderSettingsManager>()

        coEvery { workEntryDao.getByDateWithTravel(today) } returns null
        every { workEntryDao.getByDateFlow(any()) } returns flowOf(null)
        coEvery { workEntryDao.getByDateRange(any(), any()) } returns emptyList()
        every { settingsManager.settings } returns flowOf(ReminderSettings())

        val viewModel = createViewModel(workEntryDao, settingsManager)
        viewModel.confirmDeleteDay()

        // Short wait for coroutine to complete
        Thread.sleep(200)
        assertEquals(null, viewModel.deletedEntryForUndo.value)
        coVerify(exactly = 0) { workEntryDao.deleteByDate(any()) }
    }

    @Test
    fun `swipe navigation selectDate with plusDays and minusDays updates selectedDate and queries DB`() {
        val today = LocalDate.now()
        val workEntryDao = mockk<WorkEntryDao>(relaxed = true)
        val settingsManager = mockk<ReminderSettingsManager>()

        coEvery { workEntryDao.getByDate(any()) } returns null
        every { workEntryDao.getByDateFlow(any()) } returns flowOf(null)
        coEvery { workEntryDao.getByDateRange(any(), any()) } returns emptyList()
        every { settingsManager.settings } returns flowOf(ReminderSettings())

        val viewModel = createViewModel(workEntryDao, settingsManager)

        // Simulate swipe left (next day)
        viewModel.selectDate(today.plusDays(1))
        assertEquals(today.plusDays(1), viewModel.selectedDate.value)

        // Simulate swipe left again
        viewModel.selectDate(today.plusDays(2))
        assertEquals(today.plusDays(2), viewModel.selectedDate.value)

        // Simulate swipe right (previous day, back to +1)
        viewModel.selectDate(today.plusDays(1))
        assertEquals(today.plusDays(1), viewModel.selectedDate.value)

        // Swipe right past today into past
        viewModel.selectDate(today)
        viewModel.selectDate(today.minusDays(1))
        assertEquals(today.minusDays(1), viewModel.selectedDate.value)

        // Verify DB was queried for each navigated date
        coVerify { workEntryDao.getByDate(today.plusDays(1)) }
        coVerify { workEntryDao.getByDate(today.plusDays(2)) }
        coVerify { workEntryDao.getByDate(today.minusDays(1)) }
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
        val workEntryDao = mockk<WorkEntryDao>(relaxed = true)
        val settingsManager = mockk<ReminderSettingsManager>()

        coEvery { workEntryDao.getByDate(any()) } returns existing
        every { workEntryDao.getByDateFlow(any()) } returns flowOf(null)
        coEvery { workEntryDao.getByDateRange(any(), any()) } returns emptyList()
        every { settingsManager.settings } returns flowOf(ReminderSettings())

        val viewModel = createViewModel(workEntryDao, settingsManager)

        viewModel.openDailyCheckInDialog()

        assertTrue(viewModel.showDailyCheckInDialog.value)
        assertEquals(true, viewModel.dailyCheckInIsArrivalDeparture.value)
        assertEquals(true, viewModel.dailyCheckInBreakfastIncluded.value)
    }

    @Test
    fun `openDailyCheckInDialog defaults meal flags to false when no entry exists`() = runTest {
        val workEntryDao = mockk<WorkEntryDao>(relaxed = true)
        val settingsManager = mockk<ReminderSettingsManager>()

        coEvery { workEntryDao.getByDate(any()) } returns null
        every { workEntryDao.getByDateFlow(any()) } returns flowOf(null)
        coEvery { workEntryDao.getByDateRange(any(), any()) } returns emptyList()
        coEvery { workEntryDao.getLatestDayLocationLabelByDayType(any()) } returns null
        every { settingsManager.settings } returns flowOf(ReminderSettings())

        val viewModel = createViewModel(workEntryDao, settingsManager)

        viewModel.openDailyCheckInDialog()

        assertTrue(viewModel.showDailyCheckInDialog.value)
        assertEquals(false, viewModel.dailyCheckInIsArrivalDeparture.value)
        assertEquals(false, viewModel.dailyCheckInBreakfastIncluded.value)
    }

    private fun createViewModel(
        workEntryDao: WorkEntryDao,
        settingsManager: ReminderSettingsManager,
        recordDailyManualCheckIn: RecordDailyManualCheckIn = mockk(relaxed = true),
        resolveDayLocationPrefill: ResolveDayLocationPrefill = ResolveDayLocationPrefill(workEntryDao),
        entriesWithTravel: List<WorkEntryWithTravelLegs> = emptyList(),
        deleteDayEntry: DeleteDayEntry = DeleteDayEntry(workEntryDao)
    ): TodayViewModel {
        coEvery { workEntryDao.getByDateRangeWithTravel(any(), any()) } returns entriesWithTravel
        every { workEntryDao.getByDateWithTravelFlow(any()) } returns flowOf(null)
        return TodayViewModel(
            workEntryDao = workEntryDao,
            recordDailyManualCheckIn = recordDailyManualCheckIn,
            resolveDayLocationPrefill = resolveDayLocationPrefill,
            confirmOffDay = mockk<ConfirmOffDay>(relaxed = true),
            setDayLocation = mockk<SetDayLocation>(relaxed = true),
            reminderSettingsManager = settingsManager,
            deleteDayEntry = deleteDayEntry,
            nonWorkingDayChecker = mockk<NonWorkingDayChecker>(relaxed = true)
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
