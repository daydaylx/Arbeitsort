package de.montagezeit.app.ui.screen.today

import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.preferences.ReminderSettings
import de.montagezeit.app.data.preferences.ReminderSettingsManager
import de.montagezeit.app.domain.usecase.CalculateTravelCompensation
import de.montagezeit.app.domain.usecase.ConfirmOffDay
import de.montagezeit.app.domain.usecase.ConfirmWorkDay
import de.montagezeit.app.domain.usecase.FetchRouteDistance
import de.montagezeit.app.domain.usecase.RecordEveningCheckIn
import de.montagezeit.app.domain.usecase.RecordMorningCheckIn
import de.montagezeit.app.domain.usecase.ResolveReview
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
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

    private fun createViewModel(
        workEntryDao: WorkEntryDao,
        settingsManager: ReminderSettingsManager
    ): TodayViewModel {
        return TodayViewModel(
            workEntryDao = workEntryDao,
            recordMorningCheckIn = mockk<RecordMorningCheckIn>(relaxed = true),
            recordEveningCheckIn = mockk<RecordEveningCheckIn>(relaxed = true),
            confirmWorkDay = mockk<ConfirmWorkDay>(relaxed = true),
            confirmOffDay = mockk<ConfirmOffDay>(relaxed = true),
            fetchRouteDistance = mockk<FetchRouteDistance>(relaxed = true),
            calculateTravelCompensation = mockk<CalculateTravelCompensation>(relaxed = true),
            resolveReview = mockk<ResolveReview>(relaxed = true),
            reminderSettingsManager = settingsManager
        )
    }
}
