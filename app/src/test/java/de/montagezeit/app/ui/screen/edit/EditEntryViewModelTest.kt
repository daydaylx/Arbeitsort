package de.montagezeit.app.ui.screen.edit

import androidx.lifecycle.SavedStateHandle
import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.TravelLeg
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.local.entity.WorkEntryWithTravelLegs
import de.montagezeit.app.data.preferences.ReminderSettings
import de.montagezeit.app.data.preferences.ReminderSettingsManager
import de.montagezeit.app.domain.usecase.testRepository
import io.mockk.coVerify
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class EditEntryViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val workEntryDao = mockk<WorkEntryDao>(relaxed = true)
    private val reminderSettingsManager = mockk<ReminderSettingsManager>()
    private val draftRules = EditEntryDraftRules()
    private val saveBuilder = EditEntrySaveBuilder(draftRules)
    private val diagnostics = EditEntryDiagnostics(draftRules)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadEntry uses configured overtime target hours`() = runTest {
        val date = LocalDate.of(2026, 3, 24)
        val settings = ReminderSettings(
            workStart = LocalTime.of(8, 0),
            workEnd = LocalTime.of(19, 0),
            breakMinutes = 60,
            dailyTargetHours = 7.5
        )
        every { reminderSettingsManager.settings } returns flowOf(settings)
        coEvery { workEntryDao.getByDateWithTravel(date) } returns null

        val repository = testRepository(workEntryDao)
        val viewModel = EditEntryViewModel(
            workEntryRepository = repository,
            reminderSettingsManager = reminderSettingsManager,
            draftRules = draftRules,
            saveBuilder = saveBuilder,
            editEntryDiagnostics = diagnostics,
            savedStateHandle = SavedStateHandle(mapOf("date" to date.toString()))
        )

        advanceUntilIdle()

        assertEquals(7.5, viewModel.screenState.value.dailyTargetHours, 0.001)
    }

    @Test
    fun `save persists overnight travel with next day arrival timestamp`() = runTest {
        val date = LocalDate.of(2026, 4, 1)
        val settings = ReminderSettings(
            workStart = LocalTime.of(8, 0),
            workEnd = LocalTime.of(17, 0),
            breakMinutes = 60
        )
        every { reminderSettingsManager.settings } returns flowOf(settings)
        coEvery { workEntryDao.getByDateWithTravel(date) } returns null
        coEvery { workEntryDao.getByDate(date) } returns null
        val capturedLegs = slot<List<TravelLeg>>()
        coEvery { workEntryDao.replaceEntryWithTravelLegs(any(), capture(capturedLegs)) } returns Unit

        val repository = testRepository(workEntryDao)
        val viewModel = EditEntryViewModel(
            workEntryRepository = repository,
            reminderSettingsManager = reminderSettingsManager,
            draftRules = draftRules,
            saveBuilder = saveBuilder,
            editEntryDiagnostics = diagnostics,
            savedStateHandle = SavedStateHandle(mapOf("date" to date.toString()))
        )

        advanceUntilIdle()

        viewModel.setFormData(
            viewModel.copyEntryData().copy(
                hasWorkTimes = false,
                dayLocationLabel = "Baustelle",
                travelLegs = listOf(
                    EditTravelLegForm(
                        startTime = LocalTime.of(23, 0),
                        arriveTime = LocalTime.of(1, 0)
                    )
                )
            )
        )

        viewModel.save()
        advanceUntilIdle()

        val zoneId = ZoneId.systemDefault()
        val savedLeg = capturedLegs.captured.single()
        assertEquals(
            date.atTime(LocalTime.of(23, 0)).atZone(zoneId).toInstant().toEpochMilli(),
            savedLeg.startAt
        )
        assertEquals(
            date.plusDays(1).atTime(LocalTime.of(1, 0)).atZone(zoneId).toInstant().toEpochMilli(),
            savedLeg.arriveAt
        )
        coVerify(exactly = 1) { workEntryDao.replaceEntryWithTravelLegs(any(), any()) }
    }

    @Test
    fun `validationErrors emits MissingDayLocation after debounce for WORK entry without location`() = runTest {
        val date = LocalDate.of(2026, 4, 3)
        val settings = ReminderSettings(
            workStart = LocalTime.of(8, 0),
            workEnd = LocalTime.of(17, 0),
            breakMinutes = 60
        )
        every { reminderSettingsManager.settings } returns flowOf(settings)
        coEvery { workEntryDao.getByDateWithTravel(date) } returns null

        val repository = testRepository(workEntryDao)
        val viewModel = EditEntryViewModel(
            workEntryRepository = repository,
            reminderSettingsManager = reminderSettingsManager,
            draftRules = draftRules,
            saveBuilder = saveBuilder,
            editEntryDiagnostics = diagnostics,
            savedStateHandle = SavedStateHandle(mapOf("date" to date.toString()))
        )

        val collector = launch { viewModel.validationErrors.collect {} }
        advanceUntilIdle()

        viewModel.setFormData(
            viewModel.copyEntryData().copy(dayLocationLabel = null)
        )
        advanceUntilIdle()

        assertTrue(
            viewModel.validationErrors.value.any { it is ValidationError.MissingDayLocation }
        )
        collector.cancel()
    }

    @Test
    fun `mealAllowancePreviewCents stays stable when only leg labels change`() = runTest {
        val date = LocalDate.of(2026, 4, 4)
        val settings = ReminderSettings(
            workStart = LocalTime.of(8, 0),
            workEnd = LocalTime.of(17, 0),
            breakMinutes = 60
        )
        every { reminderSettingsManager.settings } returns flowOf(settings)
        coEvery { workEntryDao.getByDateWithTravel(date) } returns null

        val repository = testRepository(workEntryDao)
        val viewModel = EditEntryViewModel(
            workEntryRepository = repository,
            reminderSettingsManager = reminderSettingsManager,
            draftRules = draftRules,
            saveBuilder = saveBuilder,
            editEntryDiagnostics = diagnostics,
            savedStateHandle = SavedStateHandle(mapOf("date" to date.toString()))
        )

        advanceUntilIdle()

        viewModel.setFormData(
            viewModel.copyEntryData().copy(
                dayLocationLabel = "Baustelle",
                travelLegs = listOf(
                    EditTravelLegForm(
                        startTime = LocalTime.of(6, 0),
                        arriveTime = LocalTime.of(7, 30)
                    )
                )
            )
        )
        advanceUntilIdle()
        val baselineMealCents = viewModel.screenState.value.mealAllowancePreviewCents

        viewModel.updateTravelLegStartLabel(0, "Depot")
        viewModel.updateTravelLegEndLabel(0, "Kunde")
        viewModel.updateNote("Nachtrag")
        advanceUntilIdle()

        assertEquals(baselineMealCents, viewModel.screenState.value.mealAllowancePreviewCents)
    }

    @Test
    fun `save refreshes existing entry before persisting to avoid overwriting newer fields`() = runTest {
        val date = LocalDate.of(2026, 4, 2)
        val settings = ReminderSettings(
            workStart = LocalTime.of(8, 0),
            workEnd = LocalTime.of(17, 0),
            breakMinutes = 60
        )
        val staleEntry = WorkEntry(
            date = date,
            dayType = DayType.WORK,
            workStart = LocalTime.of(8, 0),
            workEnd = LocalTime.of(17, 0),
            breakMinutes = 60,
            dayLocationLabel = "Berlin",
            confirmedWorkDay = false
        )
        val freshEntry = staleEntry.copy(
            confirmedWorkDay = true,
            confirmationAt = 1234L,
            confirmationSource = "SYNC"
        )
        val capturedEntry = slot<WorkEntry>()

        every { reminderSettingsManager.settings } returns flowOf(settings)
        coEvery {
            workEntryDao.getByDateWithTravel(date)
        } returnsMany listOf(
            WorkEntryWithTravelLegs(staleEntry, emptyList()),
            WorkEntryWithTravelLegs(freshEntry, emptyList())
        )
        coEvery {
            workEntryDao.replaceEntryWithTravelLegs(capture(capturedEntry), any())
        } returns Unit

        val repository = testRepository(workEntryDao)
        val viewModel = EditEntryViewModel(
            workEntryRepository = repository,
            reminderSettingsManager = reminderSettingsManager,
            draftRules = draftRules,
            saveBuilder = saveBuilder,
            editEntryDiagnostics = diagnostics,
            savedStateHandle = SavedStateHandle(mapOf("date" to date.toString()))
        )

        advanceUntilIdle()

        viewModel.updateNote("Nachtrag")
        viewModel.save()
        advanceUntilIdle()

        assertTrue(capturedEntry.isCaptured)
        assertEquals(true, capturedEntry.captured.confirmedWorkDay)
        assertEquals(1234L, capturedEntry.captured.confirmationAt)
        assertEquals("SYNC", capturedEntry.captured.confirmationSource)
        assertEquals("Nachtrag", capturedEntry.captured.note)
    }
}
