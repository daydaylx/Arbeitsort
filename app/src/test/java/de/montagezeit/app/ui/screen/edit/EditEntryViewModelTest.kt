package de.montagezeit.app.ui.screen.edit

import androidx.lifecycle.SavedStateHandle
import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.TravelLeg
import de.montagezeit.app.data.preferences.ReminderSettings
import de.montagezeit.app.data.preferences.ReminderSettingsManager
import de.montagezeit.app.domain.usecase.DeleteWorkEntryByDate
import de.montagezeit.app.domain.usecase.GetWorkEntryByDate
import de.montagezeit.app.domain.usecase.GetWorkEntryWithTravelByDate
import de.montagezeit.app.domain.usecase.ReplaceWorkEntryWithTravelLegs
import de.montagezeit.app.domain.usecase.testRepository
import io.mockk.coVerify
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
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
            getWorkEntryByDate = GetWorkEntryByDate(repository),
            getWorkEntryWithTravelByDate = GetWorkEntryWithTravelByDate(repository),
            deleteWorkEntryByDate = DeleteWorkEntryByDate(repository),
            replaceWorkEntryWithTravelLegs = ReplaceWorkEntryWithTravelLegs(repository),
            reminderSettingsManager = reminderSettingsManager,
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
            getWorkEntryByDate = GetWorkEntryByDate(repository),
            getWorkEntryWithTravelByDate = GetWorkEntryWithTravelByDate(repository),
            deleteWorkEntryByDate = DeleteWorkEntryByDate(repository),
            replaceWorkEntryWithTravelLegs = ReplaceWorkEntryWithTravelLegs(repository),
            reminderSettingsManager = reminderSettingsManager,
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
}
