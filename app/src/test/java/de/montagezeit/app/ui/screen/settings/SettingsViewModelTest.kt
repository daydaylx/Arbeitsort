package de.montagezeit.app.ui.screen.settings

import de.montagezeit.app.R
import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.local.entity.WorkEntryWithTravelLegs
import de.montagezeit.app.data.preferences.ReminderSettings
import de.montagezeit.app.data.preferences.ReminderSettingsManager
import de.montagezeit.app.domain.usecase.GetWorkEntriesWithTravelByDateRange
import de.montagezeit.app.domain.usecase.testRepository
import de.montagezeit.app.export.CsvExporter
import de.montagezeit.app.export.PdfExporter
import de.montagezeit.app.notification.ReminderNotificationManager
import de.montagezeit.app.ui.util.UiText
import de.montagezeit.app.work.ReminderScheduler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
class SettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val reminderSettingsManager = mockk<ReminderSettingsManager>(relaxed = true)
    private val workEntryDao = mockk<WorkEntryDao>(relaxed = true)
    private val pdfExporter = mockk<PdfExporter>()
    private val csvExporter = mockk<CsvExporter>()
    private val reminderScheduler = mockk<ReminderScheduler>(relaxed = true)
    private val notificationManager = mockk<ReminderNotificationManager>(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { reminderSettingsManager.settings } returns flowOf(ReminderSettings(pdfEmployeeName = "Max Mustermann"))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `updateMorningWindow rejects invalid range`() = runTest {
        val viewModel = createViewModel()

        viewModel.updateMorningWindow(8, 0, 8, 0)
        advanceUntilIdle()

        assertEquals(
            SettingsUiState.ReminderError(UiText.StringResource(R.string.error_time_range_invalid)),
            viewModel.uiState.value
        )
        coVerify(exactly = 0) {
            reminderSettingsManager.updateSettings(
                morningWindowStart = any(),
                morningWindowEnd = any()
            )
        }
    }

    @Test
    fun `updateWorkStart rejects invalid range`() = runTest {
        every {
            reminderSettingsManager.settings
        } returns flowOf(
            ReminderSettings(
                workStart = LocalTime.of(8, 0),
                workEnd = LocalTime.of(17, 0)
            )
        )
        val viewModel = createViewModel()

        viewModel.updateWorkStart(LocalTime.of(18, 0))
        advanceUntilIdle()

        assertEquals(
            SettingsUiState.ReminderError(UiText.StringResource(R.string.error_time_range_invalid)),
            viewModel.uiState.value
        )
        coVerify(exactly = 0) {
            reminderSettingsManager.updateSettings(
                workStart = any(),
                workEnd = any()
            )
        }
    }

    @Test
    fun `updateDailyTargetHours persists normalized daily target only`() = runTest {
        val viewModel = createViewModel()

        viewModel.updateDailyTargetHours(7.5)
        advanceUntilIdle()

        coVerify(exactly = 1) {
            reminderSettingsManager.updateSettings(
                dailyTargetHours = 7.5
            )
        }
    }

    @Test
    fun `updateBreakMinutes rejects break that removes all net work time`() = runTest {
        every { reminderSettingsManager.settings } returns flowOf(
            ReminderSettings(
                workStart = LocalTime.of(8, 0),
                workEnd = LocalTime.of(8, 30),
                breakMinutes = 15
            )
        )
        val viewModel = createViewModel()

        viewModel.updateBreakMinutes(30)
        advanceUntilIdle()

        assertEquals(
            SettingsUiState.ReminderError(UiText.StringResource(R.string.error_break_must_be_shorter_than_shift)),
            viewModel.uiState.value
        )
        coVerify(exactly = 0) { reminderSettingsManager.updateSettings(breakMinutes = any()) }
    }

    @Test
    fun `exportPdfCurrentMonth maps storage failures to detailed error`() = runTest {
        val today = LocalDate.now()
        val entries = listOf(WorkEntryWithTravelLegs(workEntry = WorkEntry(date = today), travelLegs = emptyList()))
        coEvery { workEntryDao.getByDateRangeWithTravel(any(), any()) } returns entries
        every {
            pdfExporter.exportToPdf(
                entries = entries,
                employeeName = "Max Mustermann",
                company = null,
                project = null,
                personnelNumber = null,
                startDate = any(),
                endDate = any()
            )
        } returns PdfExporter.PdfExportResult.StorageError("Nicht genug Speicher")

        val viewModel = createViewModel()
        viewModel.exportPdfCurrentMonth()
        advanceUntilIdle()

        assertEquals(
            SettingsUiState.ExportError(UiText.DynamicString("Nicht genug Speicher")),
            viewModel.uiState.value
        )
    }

    @Test
    fun `exportCsvCurrentMonth exposes csv success state`() = runTest {
        val today = LocalDate.now()
        val entries = listOf(
            WorkEntryWithTravelLegs(
                workEntry = WorkEntry(
                    date = today,
                    confirmedWorkDay = true
                ),
                travelLegs = emptyList()
            )
        )
        val exportUri = mockk<android.net.Uri>()
        coEvery { workEntryDao.getByDateRangeWithTravel(any(), any()) } returns entries
        every { csvExporter.exportToCsv(entries) } returns exportUri

        val viewModel = createViewModel()
        viewModel.exportCsvCurrentMonth()
        advanceUntilIdle()

        assertEquals(
            SettingsUiState.ExportSuccess(
                fileUri = exportUri,
                format = ExportFormat.CSV
            ),
            viewModel.uiState.value
        )
    }

    @Test
    fun `exportCsvCurrentMonth maps exporter null to csv error`() = runTest {
        val today = LocalDate.now()
        val entries = listOf(
            WorkEntryWithTravelLegs(
                workEntry = WorkEntry(
                    date = today,
                    confirmedWorkDay = true
                ),
                travelLegs = emptyList()
            )
        )
        coEvery { workEntryDao.getByDateRangeWithTravel(any(), any()) } returns entries
        every { csvExporter.exportToCsv(entries) } returns null

        val viewModel = createViewModel()
        viewModel.exportCsvCurrentMonth()
        advanceUntilIdle()

        assertEquals(
            SettingsUiState.ExportError(UiText.StringResource(R.string.settings_error_csv_export_failed)),
            viewModel.uiState.value
        )
    }

    @Test
    fun `exportCsvCurrentMonth does not require employee name`() = runTest {
        val today = LocalDate.now()
        val entries = listOf(
            WorkEntryWithTravelLegs(
                workEntry = WorkEntry(
                    date = today,
                    confirmedWorkDay = true
                ),
                travelLegs = emptyList()
            )
        )
        val exportUri = mockk<android.net.Uri>()
        every { reminderSettingsManager.settings } returns flowOf(ReminderSettings(pdfEmployeeName = null))
        coEvery { workEntryDao.getByDateRangeWithTravel(any(), any()) } returns entries
        every { csvExporter.exportToCsv(entries) } returns exportUri

        val viewModel = createViewModel()
        viewModel.exportCsvCurrentMonth()
        advanceUntilIdle()

        assertEquals(
            SettingsUiState.ExportSuccess(
                fileUri = exportUri,
                format = ExportFormat.CSV
            ),
            viewModel.uiState.value
        )
    }

    @Test
    fun `exportCsvCurrentMonth returns empty range when only unconfirmed entries exist`() = runTest {
        val today = LocalDate.now()
        val entries = listOf(
            WorkEntryWithTravelLegs(
                workEntry = WorkEntry(
                    date = today,
                    confirmedWorkDay = false
                ),
                travelLegs = emptyList()
            )
        )
        coEvery { workEntryDao.getByDateRangeWithTravel(any(), any()) } returns entries

        val viewModel = createViewModel()
        viewModel.exportCsvCurrentMonth()
        advanceUntilIdle()

        assertEquals(
            SettingsUiState.ExportError(UiText.StringResource(R.string.settings_error_no_entries_current_month)),
            viewModel.uiState.value
        )
        verify(exactly = 0) { csvExporter.exportToCsv(any()) }
    }

    private fun createViewModel(): SettingsViewModel {
        val repository = testRepository(workEntryDao)
        return SettingsViewModel(
            reminderSettingsManager = reminderSettingsManager,
            getWorkEntriesWithTravelByDateRange = GetWorkEntriesWithTravelByDateRange(repository),
            pdfExporter = pdfExporter,
            csvExporter = csvExporter,
            reminderScheduler = reminderScheduler,
            notificationManager = notificationManager
        )
    }
}
