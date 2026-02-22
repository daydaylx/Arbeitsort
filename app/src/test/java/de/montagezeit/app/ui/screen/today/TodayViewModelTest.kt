package de.montagezeit.app.ui.screen.today

import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.dao.OvertimeEntryRow
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.preferences.ReminderSettings
import de.montagezeit.app.data.preferences.ReminderSettingsManager
import de.montagezeit.app.domain.usecase.ConfirmOffDay
import de.montagezeit.app.domain.usecase.RecordDailyManualCheckIn
import de.montagezeit.app.domain.usecase.ResolveDayLocationPrefill
import de.montagezeit.app.domain.usecase.SetDayLocation
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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

    private val mainDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(mainDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `ensureTodayEntryThen creates entry when missing`() {
        val workEntryDao = mockk<WorkEntryDao>()
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
        val workEntryDao = mockk<WorkEntryDao>()
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
        val workEntryDao = mockk<WorkEntryDao>()
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
        val workEntryDao = mockk<WorkEntryDao>()
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
        val workEntryDao = mockk<WorkEntryDao>()
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
        val workEntryDao = mockk<WorkEntryDao>()
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
        val workEntryDao = mockk<WorkEntryDao>()
        val settingsManager = mockk<ReminderSettingsManager>()
        val recordDailyManualCheckIn = mockk<RecordDailyManualCheckIn>()

        coEvery { workEntryDao.getByDate(any()) } returns null
        every { workEntryDao.getByDateFlow(any()) } returns flowOf(null)
        coEvery { workEntryDao.getByDateRange(any(), any()) } returns emptyList()
        every { settingsManager.settings } returns flowOf(ReminderSettings())
        coEvery { recordDailyManualCheckIn(any(), any()) } returns savedEntry

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
        coVerify(exactly = 1) { recordDailyManualCheckIn(today, "Baustelle A") }
        collectJob.cancel()
    }

    @Test
    fun `submitDailyManualCheckIn surfaces security errors as snackbar without location flow`() {
        val today = LocalDate.now()
        val workEntryDao = mockk<WorkEntryDao>()
        val settingsManager = mockk<ReminderSettingsManager>()
        val recordDailyManualCheckIn = mockk<RecordDailyManualCheckIn>()

        coEvery { workEntryDao.getByDate(any()) } returns null
        every { workEntryDao.getByDateFlow(any()) } returns flowOf(null)
        coEvery { workEntryDao.getByDateRange(any(), any()) } returns emptyList()
        every { settingsManager.settings } returns flowOf(ReminderSettings())
        coEvery { recordDailyManualCheckIn(any(), any()) } throws SecurityException("Location permission missing")

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

        coVerify(exactly = 1) { recordDailyManualCheckIn(today, "Baustelle B") }
        assertTrue(snackbarLatch.await(2, TimeUnit.SECONDS))
        assertTrue(viewModel.uiState.value !is TodayUiState.Error)
        collectJob.cancel()
    }

    @Test
    fun `overtime uses configurable target from settings`() {
        val today = LocalDate.now()
        val workEntryDao = mockk<WorkEntryDao>()
        val settingsManager = mockk<ReminderSettingsManager>()
        val settings = ReminderSettings(
            dailyTargetHours = 10.0,
            weeklyTargetHours = 50.0,
            monthlyTargetHours = 200.0
        )
        val overtimeEntry = OvertimeEntryRow(
            date = today,
            workStart = LocalTime.of(8, 0),
            workEnd = LocalTime.of(17, 0),
            breakMinutes = 0,  // 9h actual
            dayType = DayType.WORK,
            confirmedWorkDay = true,
            travelStartAt = null,
            travelArriveAt = null,
            travelPaidMinutes = 0
        )

        coEvery { workEntryDao.getByDate(any()) } returns null
        every { workEntryDao.getByDateFlow(any()) } returns flowOf(null)
        coEvery { workEntryDao.getByDateRange(any(), any()) } returns emptyList()
        every { settingsManager.settings } returns flowOf(settings)

        val viewModel = createViewModel(
            workEntryDao = workEntryDao,
            settingsManager = settingsManager,
            entriesBetween = listOf(overtimeEntry)
        )

        val latch = CountDownLatch(1)
        val collectJob = CoroutineScope(Dispatchers.Main).launch {
            viewModel.overtimeYearDisplay.collect { display ->
                // 9h actual - 10h target (from settings) = -1h overtime
                if (display == "-1,00h") {
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
        val workEntryDao = mockk<WorkEntryDao>()
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

        val viewModel = createViewModel(workEntryDao, settingsManager)

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
        val workEntryDao = mockk<WorkEntryDao>()
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

        val viewModel = createViewModel(workEntryDao, settingsManager)

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
        val workEntryDao = mockk<WorkEntryDao>()
        val settingsManager = mockk<ReminderSettingsManager>()
        val settings = ReminderSettings()  // No explicit target hours, should use defaults

        coEvery { workEntryDao.getByDate(any()) } returns null
        every { workEntryDao.getByDateFlow(any()) } returns flowOf(null)
        coEvery { workEntryDao.getByDateRange(any(), any()) } returns emptyList()
        every { settingsManager.settings } returns flowOf(settings)

        val viewModel = createViewModel(workEntryDao, settingsManager)

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
        val workEntryDao = mockk<WorkEntryDao>()
        val settingsManager = mockk<ReminderSettingsManager>()

        coEvery { workEntryDao.getByDate(any()) } returns null
        every { workEntryDao.getByDateFlow(any()) } returns flowOf(null)
        coEvery { workEntryDao.getByDateRange(any(), any()) } returns emptyList()
        every { settingsManager.settings } returns flowOf(ReminderSettings())

        val viewModel = createViewModel(workEntryDao, settingsManager)

        // Initially selectedDate should be today
        assertEquals(today, viewModel.selectedDate.value)

        // Select today again (should not fail or return early without updating UI)
        viewModel.selectDate(today)

        // selectedDate should still be today
        assertEquals(today, viewModel.selectedDate.value)

        // Week days UI should reflect selection
        val weekDays = viewModel.weekDaysUi.value
        if (weekDays.isNotEmpty()) {
            val todayChip = weekDays.find { it.date == today }
            assertTrue("Today should be marked as selected", todayChip?.isSelected == true)
        }
    }

    @Test
    fun `selectDate switches between different dates correctly`() {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        val workEntryDao = mockk<WorkEntryDao>()
        val settingsManager = mockk<ReminderSettingsManager>()

        coEvery { workEntryDao.getByDate(any()) } returns null
        every { workEntryDao.getByDateFlow(any()) } returns flowOf(null)
        coEvery { workEntryDao.getByDateRange(any(), any()) } returns emptyList()
        every { settingsManager.settings } returns flowOf(ReminderSettings())

        val viewModel = createViewModel(workEntryDao, settingsManager)

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

    private fun createViewModel(
        workEntryDao: WorkEntryDao,
        settingsManager: ReminderSettingsManager,
        recordDailyManualCheckIn: RecordDailyManualCheckIn = mockk(relaxed = true),
        resolveDayLocationPrefill: ResolveDayLocationPrefill = ResolveDayLocationPrefill(workEntryDao),
        entriesBetween: List<OvertimeEntryRow> = emptyList()
    ): TodayViewModel {
        coEvery { workEntryDao.getEntriesBetween(any(), any()) } returns entriesBetween
        return TodayViewModel(
            workEntryDao = workEntryDao,
            recordDailyManualCheckIn = recordDailyManualCheckIn,
            resolveDayLocationPrefill = resolveDayLocationPrefill,
            confirmOffDay = mockk<ConfirmOffDay>(relaxed = true),
            setDayLocation = mockk<SetDayLocation>(relaxed = true),
            reminderSettingsManager = settingsManager
        )
    }
}
