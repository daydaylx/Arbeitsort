package de.montagezeit.app.ui.screen.overview

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import de.montagezeit.app.data.preferences.ReminderSettings
import de.montagezeit.app.data.preferences.ReminderSettingsManager
import de.montagezeit.app.data.repository.WorkEntryRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class OverviewViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher()
    private val workEntryRepository = mockk<WorkEntryRepository>()
    private val reminderSettingsManager = mockk<ReminderSettingsManager>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { reminderSettingsManager.settings } returns flowOf(ReminderSettings())
        every { workEntryRepository.getByDateWithTravelFlow(any()) } returns flowOf(null)
        coEvery { workEntryRepository.getByDateRangeWithTravel(any(), any()) } returns emptyList()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        io.mockk.clearAllMocks()
    }

    private fun createViewModel() = OverviewViewModel(workEntryRepository, reminderSettingsManager)
        .also { it.calculationDispatcher = dispatcher }

    @Test
    fun `selectDate aktualisiert selectedDate StateFlow`() = runTest {
        val vm = createViewModel()
        val newDate = LocalDate.of(2026, 6, 15)

        vm.selectDate(newDate)

        assertEquals(newDate, vm.selectedDate.value)
    }

    @Test
    fun `selectPeriod aktualisiert selectedPeriod StateFlow`() = runTest {
        val vm = createViewModel()

        vm.selectPeriod(OverviewPeriod.MONTH)

        assertEquals(OverviewPeriod.MONTH, vm.selectedPeriod.value)
    }

    @Test
    fun `goToPreviousRange verschiebt selectedDate um eine Woche zurueck bei WEEK-Periode`() = runTest {
        val vm = createViewModel()
        val baseDate = LocalDate.of(2026, 6, 15)
        vm.selectDate(baseDate)

        vm.goToPreviousRange()

        assertEquals(baseDate.minusWeeks(1), vm.selectedDate.value)
    }

    @Test
    fun `goToNextRange verschiebt selectedDate um eine Woche vor bei WEEK-Periode`() = runTest {
        val vm = createViewModel()
        val baseDate = LocalDate.of(2026, 6, 15)
        vm.selectDate(baseDate)

        vm.goToNextRange()

        assertEquals(baseDate.plusWeeks(1), vm.selectedDate.value)
    }

    @Test
    fun `screenState spiegelt selectedPeriod Aenderung sofort wider`() = runTest {
        val vm = createViewModel()
        val collectJob = backgroundScope.launch { vm.screenState.collect { } }
        advanceUntilIdle()

        vm.selectPeriod(OverviewPeriod.YEAR)
        advanceUntilIdle()

        assertEquals(OverviewPeriod.YEAR, vm.screenState.value.selectedPeriod)
        collectJob.cancel()
    }
}
