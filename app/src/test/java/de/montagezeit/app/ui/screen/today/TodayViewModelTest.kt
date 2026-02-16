package de.montagezeit.app.ui.screen.today

import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.preferences.ReminderSettings
import de.montagezeit.app.data.preferences.ReminderSettingsManager
import de.montagezeit.app.domain.usecase.ConfirmOffDay
import de.montagezeit.app.domain.usecase.RecordDailyManualCheckIn
import de.montagezeit.app.domain.usecase.ResolveDayLocationPrefill
import de.montagezeit.app.domain.usecase.ResolveReview
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
        val settings = ReminderSettings(defaultDayLocationLabel = "Default Ort")

        coEvery { workEntryDao.getByDate(any()) } returns existing
        every { workEntryDao.getByDateFlow(any()) } returns flowOf(null)
        coEvery { workEntryDao.getByDateRange(any(), any()) } returns emptyList()
        coEvery { workEntryDao.getLatestDayLocationLabelByDayType(any()) } returns null
        coEvery { workEntryDao.getLatestDayLocationLabel() } returns null
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
        coVerify(exactly = 0) { workEntryDao.getLatestDayLocationLabel() }
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
        val settings = ReminderSettings(defaultDayLocationLabel = "Default Ort")

        coEvery { workEntryDao.getByDate(any()) } returns existing
        every { workEntryDao.getByDateFlow(any()) } returns flowOf(null)
        coEvery { workEntryDao.getByDateRange(any(), any()) } returns emptyList()
        coEvery { workEntryDao.getLatestDayLocationLabelByDayType(DayType.WORK) } returns "Werk 9"
        coEvery { workEntryDao.getLatestDayLocationLabel() } returns "Any Ort"
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
        coVerify(exactly = 0) { workEntryDao.getLatestDayLocationLabel() }
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
        coEvery { workEntryDao.getLatestDayLocationLabel() } returns "Irgendein Ort"
        every { settingsManager.settings } returns flowOf(ReminderSettings(defaultDayLocationLabel = "Standard-Ort"))

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
        coVerify(exactly = 0) { workEntryDao.getLatestDayLocationLabel() }
        collectJob.cancel()
    }

    @Test
    fun `openDailyCheckInDialog falls back to any label`() {
        val today = LocalDate.now()
        val existing = WorkEntry(
            date = today,
            dayType = DayType.WORK,
            dayLocationLabel = ""
        )
        val workEntryDao = mockk<WorkEntryDao>()
        val settingsManager = mockk<ReminderSettingsManager>()
        val settings = ReminderSettings(defaultDayLocationLabel = "Standard-Ort")

        coEvery { workEntryDao.getByDate(any()) } returns existing
        every { workEntryDao.getByDateFlow(any()) } returns flowOf(null)
        coEvery { workEntryDao.getByDateRange(any(), any()) } returns emptyList()
        coEvery { workEntryDao.getLatestDayLocationLabelByDayType(DayType.WORK) } returns null
        coEvery { workEntryDao.getLatestDayLocationLabel() } returns "Any Ort"
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
        assertEquals("Any Ort", viewModel.dailyCheckInLocationInput.value)
        collectJob.cancel()
    }

    @Test
    fun `openDailyCheckInDialog falls back to settings label`() {
        val today = LocalDate.now()
        val existing = WorkEntry(
            date = today,
            dayType = DayType.WORK,
            dayLocationLabel = ""
        )
        val workEntryDao = mockk<WorkEntryDao>()
        val settingsManager = mockk<ReminderSettingsManager>()
        val settings = ReminderSettings(defaultDayLocationLabel = "Standard-Ort")

        coEvery { workEntryDao.getByDate(any()) } returns existing
        every { workEntryDao.getByDateFlow(any()) } returns flowOf(null)
        coEvery { workEntryDao.getByDateRange(any(), any()) } returns emptyList()
        coEvery { workEntryDao.getLatestDayLocationLabelByDayType(DayType.WORK) } returns null
        coEvery { workEntryDao.getLatestDayLocationLabel() } returns null
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
        assertEquals("Standard-Ort", viewModel.dailyCheckInLocationInput.value)
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

    private fun createViewModel(
        workEntryDao: WorkEntryDao,
        settingsManager: ReminderSettingsManager,
        recordDailyManualCheckIn: RecordDailyManualCheckIn = mockk(relaxed = true),
        resolveDayLocationPrefill: ResolveDayLocationPrefill = ResolveDayLocationPrefill(workEntryDao, settingsManager)
    ): TodayViewModel {
        return TodayViewModel(
            workEntryDao = workEntryDao,
            recordDailyManualCheckIn = recordDailyManualCheckIn,
            resolveDayLocationPrefill = resolveDayLocationPrefill,
            confirmOffDay = mockk<ConfirmOffDay>(relaxed = true),
            resolveReview = mockk<ResolveReview>(relaxed = true),
            setDayLocation = mockk<SetDayLocation>(relaxed = true),
            reminderSettingsManager = settingsManager
        )
    }
}
