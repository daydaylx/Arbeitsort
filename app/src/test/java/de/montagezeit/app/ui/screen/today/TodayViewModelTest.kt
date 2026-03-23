package de.montagezeit.app.ui.screen.today

import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.local.entity.WorkEntryWithTravelLegs
import de.montagezeit.app.data.preferences.ReminderSettings
import de.montagezeit.app.data.preferences.ReminderSettingsManager
import de.montagezeit.app.domain.util.WeekCalculator
import de.montagezeit.app.domain.usecase.ConfirmOffDay
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
    fun `ensureTodayEntryThen creates entry when missing`() {
        val workEntryDao = mockk<WorkEntryDao>(relaxed = true)
        val settingsManager = mockk<ReminderSettingsManager>()
        val settings = ReminderSettings()

        coEvery { workEntryDao.getByDate(any()) } returns null
        every { workEntryDao.getByDateFlow(any()) } returns flowOf(null)
        coEvery { workEntryDao.getByDateRange(any(), any()) } returns emptyList()
        coEvery { workEntryDao.upsert(any()) } returns Unit
        every { settingsManager.settings } returns flowOf(settings)

        val viewModel = createViewModel(workEntryDao, settingsManager)
        val latch = CountDownLatch(1)

        viewModel.ensureTodayEntryThen { latch.countDown() }

        assertTrue(latch.await(2, TimeUnit.SECONDS))
        coVerify(exactly = 1) {
            workEntryDao.upsert(match {
                it.date == LocalDate.now() &&
                    it.workStart == settings.workStart &&
                    it.workEnd == settings.workEnd &&
                    it.breakMinutes == settings.breakMinutes
            })
        }
    }

    @Test
    fun `ensureTodayEntryThen skips upsert when entry exists`() {
        val today = LocalDate.now()
        val existing = WorkEntry(date = today)
        val workEntryDao = mockk<WorkEntryDao>(relaxed = true)
        val settingsManager = mockk<ReminderSettingsManager>()

        coEvery { workEntryDao.getByDate(any()) } returns existing
        every { workEntryDao.getByDateFlow(any()) } returns flowOf(existing)
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
    fun `openDailyCheckInDialog uses latest work label before any label`() {
        val today = LocalDate.now()
        val existing = WorkEntry(
            date = today,
            dayType = DayType.WORK,
            dayLocationLabel = "   "
        )
        val workEntryDao = mockk<WorkEntryDao>(relaxed = true)
        val settingsManager = mockk<ReminderSettingsManager>()
        val settings = ReminderSettings()

        coEvery { workEntryDao.getByDate(any()) } returns existing
        every { workEntryDao.getByDateFlow(any()) } returns flowOf(null)
        coEvery { workEntryDao.getByDateRange(any(), any()) } returns emptyList()
        coEvery { workEntryDao.getLatestDayLocationLabelByDayType(DayType.WORK) } returns "Werk 9"
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
        assertEquals("Werk 9", viewModel.dailyCheckInLocationInput.value)
        coVerify(exactly = 1) { workEntryDao.getLatestDayLocationLabelByDayType(DayType.WORK) }
        collectJob.cancel()
    }

    @Test
    fun `openDailyCheckInDialog uses latest work label when today entry is missing`() {
        val workEntryDao = mockk<WorkEntryDao>(relaxed = true)
        val settingsManager = mockk<ReminderSettingsManager>()

        coEvery { workEntryDao.getByDate(any()) } returns null
        every { workEntryDao.getByDateFlow(any()) } returns flowOf(null)
        coEvery { workEntryDao.getByDateRange(any(), any()) } returns emptyList()
        coEvery { workEntryDao.getLatestDayLocationLabelByDayType(DayType.WORK) } returns "Letzte Baustelle"
        every { settingsManager.settings } returns flowOf(ReminderSettings())

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
    fun `overtime uses configurable target from settings`() {
        val today = LocalDate.now()
        val workEntryDao = mockk<WorkEntryDao>(relaxed = true)
        val settingsManager = mockk<ReminderSettingsManager>()
        val settings = ReminderSettings(
            dailyTargetHours = 10.0,
            weeklyTargetHours = 50.0,
            monthlyTargetHours = 200.0
        )
        val overtimeEntry = WorkEntryWithTravelLegs(
            workEntry = WorkEntry(
                date = today,
                workStart = LocalTime.of(8, 0),
                workEnd = LocalTime.of(17, 0),
                breakMinutes = 0,  // 9h actual
                dayType = DayType.WORK,
                confirmedWorkDay = true
            ),
            travelLegs = emptyList()
        )

        coEvery { workEntryDao.getByDate(any()) } returns null
        every { workEntryDao.getByDateFlow(any()) } returns flowOf(null)
        coEvery { workEntryDao.getByDateRange(any(), any()) } returns emptyList()
        every { settingsManager.settings } returns flowOf(settings)

        val viewModel = createViewModel(
            workEntryDao = workEntryDao,
            settingsManager = settingsManager,
            entriesWithTravel = listOf(overtimeEntry)
        )

        val latch = CountDownLatch(1)
        val collectJob = CoroutineScope(Dispatchers.Main).launch {
            viewModel.screenState.collect { state ->
                // 9h actual - 10h target (from settings) = -1h overtime
                if (state.overtimeYearDisplay == "-1,00h") {
                    latch.countDown()
                }
            }
        }

        assertTrue(latch.await(2, TimeUnit.SECONDS))
        collectJob.cancel()
    }

    @Test
    fun `week stats uses weekly target from settings`() {
        val today = LocalDate.now()
        val workEntryDao = mockk<WorkEntryDao>(relaxed = true)
        val settingsManager = mockk<ReminderSettingsManager>()
        val settings = ReminderSettings(
            dailyTargetHours = 7.5,
            weeklyTargetHours = 37.5,
            monthlyTargetHours = 150.0
        )

        coEvery { workEntryDao.getByDate(any()) } returns null
        every { workEntryDao.getByDateFlow(any()) } returns flowOf(null)
        coEvery { workEntryDao.getByDateRange(any(), any()) } returns emptyList()
        every { settingsManager.settings } returns flowOf(settings)

        val viewModel = createViewModel(
            workEntryDao,
            settingsManager,
            entriesWithTravel = emptyList()
        )

        val latch = CountDownLatch(1)
        val collectJob = CoroutineScope(Dispatchers.Main).launch {
            viewModel.weekStats.collect { stats ->
                // Verify weekStats uses the custom 37.5h target from settings
                if (stats?.targetHours == 37.5) {
                    latch.countDown()
                }
            }
        }

        assertTrue(latch.await(2, TimeUnit.SECONDS))
        collectJob.cancel()
    }

    @Test
    fun `month stats uses monthly target from settings`() {
        val today = LocalDate.now()
        val workEntryDao = mockk<WorkEntryDao>(relaxed = true)
        val settingsManager = mockk<ReminderSettingsManager>()
        val settings = ReminderSettings(
            dailyTargetHours = 7.5,
            weeklyTargetHours = 37.5,
            monthlyTargetHours = 150.0
        )

        coEvery { workEntryDao.getByDate(any()) } returns null
        every { workEntryDao.getByDateFlow(any()) } returns flowOf(null)
        coEvery { workEntryDao.getByDateRange(any(), any()) } returns emptyList()
        every { settingsManager.settings } returns flowOf(settings)

        val viewModel = createViewModel(
            workEntryDao,
            settingsManager,
            entriesWithTravel = emptyList()
        )

        val latch = CountDownLatch(1)
        val collectJob = CoroutineScope(Dispatchers.Main).launch {
            viewModel.monthStats.collect { stats ->
                // Verify monthStats uses the custom 150.0h target from settings
                if (stats?.targetHours == 150.0) {
                    latch.countDown()
                }
            }
        }

        assertTrue(latch.await(2, TimeUnit.SECONDS))
        collectJob.cancel()
    }

    @Test
    fun `overtime targets default to 8-40-160 when not configured`() {
        val today = LocalDate.now()
        val workEntryDao = mockk<WorkEntryDao>(relaxed = true)
        val settingsManager = mockk<ReminderSettingsManager>()
        val settings = ReminderSettings()  // No explicit target hours, should use defaults

        coEvery { workEntryDao.getByDate(any()) } returns null
        every { workEntryDao.getByDateFlow(any()) } returns flowOf(null)
        coEvery { workEntryDao.getByDateRange(any(), any()) } returns emptyList()
        every { settingsManager.settings } returns flowOf(settings)

        val viewModel = createViewModel(
            workEntryDao,
            settingsManager,
            entriesWithTravel = emptyList()
        )

        // Verify defaults are used
        assertEquals(8.0, settings.dailyTargetHours, 0.001)
        assertEquals(40.0, settings.weeklyTargetHours, 0.001)
        assertEquals(160.0, settings.monthlyTargetHours, 0.001)

        val weekLatch = CountDownLatch(1)
        val monthLatch = CountDownLatch(1)

        val weekJob = CoroutineScope(Dispatchers.Main).launch {
            viewModel.weekStats.collect { stats ->
                if (stats?.targetHours == 40.0) {
                    weekLatch.countDown()
                }
            }
        }

        val monthJob = CoroutineScope(Dispatchers.Main).launch {
            viewModel.monthStats.collect { stats ->
                if (stats?.targetHours == 160.0) {
                    monthLatch.countDown()
                }
            }
        }

        assertTrue(weekLatch.await(2, TimeUnit.SECONDS))
        assertTrue(monthLatch.await(2, TimeUnit.SECONDS))
        weekJob.cancel()
        monthJob.cancel()
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
        val workEntryDao = mockk<WorkEntryDao>(relaxed = true)
        val settingsManager = mockk<ReminderSettingsManager>()

        coEvery { workEntryDao.getByDate(today) } returns existingEntry
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
        assertEquals(existingEntry, viewModel.deletedEntryForUndo.value)
        assertTrue(viewModel.uiState.value is TodayUiState.Success)
        assertEquals(null, (viewModel.uiState.value as TodayUiState.Success).entry)
        coVerify(exactly = 1) { workEntryDao.deleteByDate(today) }
        collectJob.cancel()
    }

    @Test
    fun `undoDeleteDay re-inserts the deleted entry`() {
        val today = LocalDate.now()
        val existingEntry = WorkEntry(date = today, dayType = DayType.WORK)
        val workEntryDao = mockk<WorkEntryDao>(relaxed = true)
        val settingsManager = mockk<ReminderSettingsManager>()

        coEvery { workEntryDao.getByDate(today) } returns existingEntry
        every { workEntryDao.getByDateFlow(any()) } returns flowOf(null)
        coEvery { workEntryDao.getByDateRange(any(), any()) } returns emptyList()
        coEvery { workEntryDao.deleteByDate(today) } returns Unit
        coEvery { workEntryDao.upsert(existingEntry) } returns Unit
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
        coVerify(exactly = 1) { workEntryDao.upsert(existingEntry) }
        assertTrue(viewModel.uiState.value is TodayUiState.Success)
        assertEquals(existingEntry, (viewModel.uiState.value as TodayUiState.Success).entry)
        collectJob.cancel()
    }

    @Test
    fun `confirmDeleteDay does nothing when no entry exists`() {
        val today = LocalDate.now()
        val workEntryDao = mockk<WorkEntryDao>(relaxed = true)
        val settingsManager = mockk<ReminderSettingsManager>()

        coEvery { workEntryDao.getByDate(today) } returns null
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
    fun `unbestaetigter WORK-Tag zaehlt nicht in Wochenwerte`() {
        val today = LocalDate.now()
        val confirmedEntry = WorkEntry(
            date = today,
            dayType = DayType.WORK,
            dayLocationLabel = "Baustelle",
            confirmedWorkDay = true,
            workStart = LocalTime.of(8, 0),
            workEnd = LocalTime.of(17, 0),
            breakMinutes = 60
        )
        val unconfirmedEntry = WorkEntry(
            date = today.minusDays(1),
            dayType = DayType.WORK,
            dayLocationLabel = "Baustelle",
            confirmedWorkDay = false,
            workStart = LocalTime.of(8, 0),
            workEnd = LocalTime.of(17, 0),
            breakMinutes = 60
        )
        val workEntryDao = mockk<WorkEntryDao>(relaxed = true)
        val settingsManager = mockk<ReminderSettingsManager>()

        coEvery { workEntryDao.getByDate(any()) } returns null
        every { workEntryDao.getByDateFlow(any()) } returns flowOf(null)
        coEvery { workEntryDao.getByDateRange(any(), any()) } returns listOf(confirmedEntry, unconfirmedEntry)
        every { settingsManager.settings } returns flowOf(ReminderSettings())

        val viewModel = createViewModel(
            workEntryDao,
            settingsManager,
            entriesWithTravel = listOf(record(confirmedEntry), record(unconfirmedEntry))
        )

        Thread.sleep(300)

        val stats = viewModel.weekStats.value
        assertEquals(
            "Nur bestätigte WORK-Tage dürfen in Wochenwerte einfließen",
            1,
            stats?.workDaysCount
        )
        assertEquals(8.0, stats?.totalHours ?: 0.0, 0.01)
    }

    @Test
    fun `bestaetigter WORK-Tag zaehlt in Wochenwerte`() {
        val today = LocalDate.now()
        val confirmedEntry = WorkEntry(
            date = today,
            dayType = DayType.WORK,
            dayLocationLabel = "Baustelle",
            confirmedWorkDay = true,
            workStart = LocalTime.of(8, 0),
            workEnd = LocalTime.of(17, 0),
            breakMinutes = 60
        )
        val workEntryDao = mockk<WorkEntryDao>(relaxed = true)
        val settingsManager = mockk<ReminderSettingsManager>()

        coEvery { workEntryDao.getByDate(any()) } returns null
        every { workEntryDao.getByDateFlow(any()) } returns flowOf(null)
        coEvery { workEntryDao.getByDateRange(any(), any()) } returns listOf(confirmedEntry)
        every { settingsManager.settings } returns flowOf(ReminderSettings())

        val viewModel = createViewModel(
            workEntryDao,
            settingsManager,
            entriesWithTravel = listOf(record(confirmedEntry))
        )
        Thread.sleep(300)

        val stats = viewModel.weekStats.value
        assertEquals(1, stats?.workDaysCount)
        assertEquals(8.0, stats?.totalHours ?: 0.0, 0.01)
    }

    @Test
    fun `monthStats mealAllowanceTotalCents sums confirmed work entries`() {
        val now = LocalDate.now()
        val workEntryDao = mockk<WorkEntryDao>(relaxed = true)
        val settingsManager = mockk<ReminderSettingsManager>()

        val entries = listOf(
            WorkEntry(date = now, dayType = DayType.WORK, confirmedWorkDay = true, mealAllowanceAmountCents = 2800),
            WorkEntry(date = now.minusDays(1), dayType = DayType.WORK, confirmedWorkDay = true, mealAllowanceAmountCents = 2220),
            WorkEntry(date = now.minusDays(2), dayType = DayType.OFF, mealAllowanceAmountCents = 0)
        )

        coEvery { workEntryDao.getByDate(any()) } returns null
        every { workEntryDao.getByDateFlow(any()) } returns flowOf(null)
        coEvery { workEntryDao.getByDateRange(any(), any()) } returns entries
        every { settingsManager.settings } returns flowOf(ReminderSettings())

        val viewModel = createViewModel(
            workEntryDao,
            settingsManager,
            entriesWithTravel = entries.map(::record)
        )

        Thread.sleep(300)

        assertEquals(5020, viewModel.monthStats.value?.mealAllowanceTotalCents)
    }

    @Test
    fun `monthStats mealAllowanceTotalCents ignores non-work entries with stale allowance`() {
        val now = LocalDate.now()
        val workEntryDao = mockk<WorkEntryDao>(relaxed = true)
        val settingsManager = mockk<ReminderSettingsManager>()

        // Simulate a corrupt OFF entry that still has a non-zero meal allowance
        val entries = listOf(
            WorkEntry(date = now, dayType = DayType.WORK, confirmedWorkDay = true, mealAllowanceAmountCents = 2800),
            WorkEntry(date = now.minusDays(1), dayType = DayType.OFF, mealAllowanceAmountCents = 1400)
        )

        coEvery { workEntryDao.getByDate(any()) } returns null
        every { workEntryDao.getByDateFlow(any()) } returns flowOf(null)
        coEvery { workEntryDao.getByDateRange(any(), any()) } returns entries
        every { settingsManager.settings } returns flowOf(ReminderSettings())

        val viewModel = createViewModel(
            workEntryDao,
            settingsManager,
            entriesWithTravel = entries.map(::record)
        )

        Thread.sleep(300)

        // Only the WORK entry's allowance should be counted
        assertEquals(2800, viewModel.monthStats.value?.mealAllowanceTotalCents)
    }

    @Test
    fun `monthStats mealAllowanceTotalCents ignores unconfirmed work entries`() {
        val now = LocalDate.now()
        val workEntryDao = mockk<WorkEntryDao>(relaxed = true)
        val settingsManager = mockk<ReminderSettingsManager>()

        val entries = listOf(
            WorkEntry(date = now, dayType = DayType.WORK, confirmedWorkDay = true, mealAllowanceAmountCents = 2800),
            WorkEntry(date = now.minusDays(1), dayType = DayType.WORK, confirmedWorkDay = false, mealAllowanceAmountCents = 2220)
        )

        coEvery { workEntryDao.getByDate(any()) } returns null
        every { workEntryDao.getByDateFlow(any()) } returns flowOf(null)
        coEvery { workEntryDao.getByDateRange(any(), any()) } returns entries
        every { settingsManager.settings } returns flowOf(ReminderSettings())

        val viewModel = createViewModel(
            workEntryDao,
            settingsManager,
            entriesWithTravel = entries.map(::record)
        )

        Thread.sleep(300)

        assertEquals(2800, viewModel.monthStats.value?.mealAllowanceTotalCents)
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

    @Test
    fun `monthStats mealAllowanceTotalCents is zero when all entries have no allowance`() {
        val now = LocalDate.now()
        val workEntryDao = mockk<WorkEntryDao>(relaxed = true)
        val settingsManager = mockk<ReminderSettingsManager>()

        val entries = listOf(
            WorkEntry(date = now, dayType = DayType.OFF, mealAllowanceAmountCents = 0),
            WorkEntry(date = now.minusDays(1), dayType = DayType.COMP_TIME, mealAllowanceAmountCents = 0)
        )

        coEvery { workEntryDao.getByDate(any()) } returns null
        every { workEntryDao.getByDateFlow(any()) } returns flowOf(null)
        coEvery { workEntryDao.getByDateRange(any(), any()) } returns entries
        every { settingsManager.settings } returns flowOf(ReminderSettings())

        val viewModel = createViewModel(workEntryDao, settingsManager)

        Thread.sleep(300)

        assertEquals(0, viewModel.monthStats.value?.mealAllowanceTotalCents)
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
        ).also { createdViewModels.add(it) }
    }

    private fun record(entry: WorkEntry) = WorkEntryWithTravelLegs(
        workEntry = entry,
        travelLegs = emptyList()
    )
}
