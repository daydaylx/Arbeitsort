package de.montagezeit.app.ui.screen.edit

import androidx.lifecycle.SavedStateHandle
import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.preferences.ReminderSettings
import de.montagezeit.app.data.preferences.ReminderSettingsManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
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

        val viewModel = EditEntryViewModel(
            workEntryDao = workEntryDao,
            reminderSettingsManager = reminderSettingsManager,
            savedStateHandle = SavedStateHandle(mapOf("date" to date.toString()))
        )

        advanceUntilIdle()

        assertEquals(7.5, viewModel.screenState.value.dailyTargetHours, 0.001)
    }
}
