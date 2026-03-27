package de.montagezeit.app.ui.screen.export

import android.net.Uri
import androidx.lifecycle.viewModelScope
import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.local.entity.WorkEntryWithTravelLegs
import de.montagezeit.app.data.preferences.ReminderSettings
import de.montagezeit.app.data.preferences.ReminderSettingsManager
import de.montagezeit.app.export.PdfExporter
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ExportPreviewViewModelTest {

    private val testScheduler = TestCoroutineScheduler()
    private val mainDispatcher = UnconfinedTestDispatcher(testScheduler)

    private val workEntryDao = mockk<WorkEntryDao>(relaxed = true)
    private val reminderSettingsManager = mockk<ReminderSettingsManager>()
    private val pdfExporter = mockk<PdfExporter>()

    private lateinit var viewModel: ExportPreviewViewModel

    private val startDate = LocalDate.of(2024, 6, 1)
    private val endDate = LocalDate.of(2024, 6, 30)

    @Before
    fun setup() {
        Dispatchers.setMain(mainDispatcher)
        every { reminderSettingsManager.settings } returns flowOf(ReminderSettings())
        viewModel = ExportPreviewViewModel(workEntryDao, reminderSettingsManager, pdfExporter)
    }

    @After
    fun tearDown() {
        viewModel.viewModelScope.cancel()
        Dispatchers.resetMain()
        io.mockk.clearAllMocks()
    }

    @Test
    fun `loadRange transitions to Empty Loading state immediately`() = runTest {
        coEvery { workEntryDao.getByDateRangeWithTravel(any(), any()) } returns emptyList()

        viewModel.loadRange(startDate, endDate)

        // After coroutine completes the state will have advanced, but the initial
        // synchronous transition sets an Empty/loading state before the coroutine runs.
        // With UnconfinedTestDispatcher the coroutine executes eagerly, so we assert
        // the final state is Empty (no entries) rather than List.
        assertTrue(viewModel.uiState.value is PreviewState.Empty)
    }

    @Test
    fun `refresh transitions to List state when entries exist`() = runTest {
        val entry = WorkEntry(date = startDate, dayType = DayType.WORK)
        coEvery { workEntryDao.getByDateRangeWithTravel(startDate, endDate) } returns listOf(
            WorkEntryWithTravelLegs(workEntry = entry, travelLegs = emptyList())
        )

        viewModel.loadRange(startDate, endDate)

        assertTrue("Expected List state but got: ${viewModel.uiState.value}",
            viewModel.uiState.value is PreviewState.List)
    }

    @Test
    fun `refresh transitions to Empty state when no entries exist`() = runTest {
        coEvery { workEntryDao.getByDateRangeWithTravel(startDate, endDate) } returns emptyList()

        viewModel.loadRange(startDate, endDate)

        assertTrue("Expected Empty state but got: ${viewModel.uiState.value}",
            viewModel.uiState.value is PreviewState.Empty)
    }

    @Test
    fun `refresh transitions to Error state when DAO throws`() = runTest {
        coEvery { workEntryDao.getByDateRangeWithTravel(any(), any()) } throws RuntimeException("DB failure")

        viewModel.loadRange(startDate, endDate)

        assertTrue("Expected Error state but got: ${viewModel.uiState.value}",
            viewModel.uiState.value is PreviewState.Error)
    }

    @Test
    fun `createPdf transitions to Error when employeeName is missing`() = runTest {
        every { reminderSettingsManager.settings } returns flowOf(
            ReminderSettings(pdfEmployeeName = null)
        )
        val vm = ExportPreviewViewModel(workEntryDao, reminderSettingsManager, pdfExporter)
        coEvery { workEntryDao.getByDateRangeWithTravel(any(), any()) } returns emptyList()

        vm.loadRange(startDate, endDate)
        vm.createPdf()

        val state = vm.uiState.value
        assertTrue("Expected Error state but got: $state", state is PreviewState.Error)
        vm.viewModelScope.cancel()
    }

    @Test
    fun `createPdf transitions to PdfReady on export success`() = runTest {
        val settings = ReminderSettings(pdfEmployeeName = "Max Mustermann")
        every { reminderSettingsManager.settings } returns flowOf(settings)
        val vm = ExportPreviewViewModel(workEntryDao, reminderSettingsManager, pdfExporter)
        val entry = WorkEntry(date = startDate, dayType = DayType.WORK)
        coEvery { workEntryDao.getByDateRangeWithTravel(startDate, endDate) } returns listOf(
            WorkEntryWithTravelLegs(workEntry = entry, travelLegs = emptyList())
        )
        val fakeUri = mockk<Uri>()
        every { fakeUri.lastPathSegment } returns "export.pdf"
        every {
            pdfExporter.exportToPdf(
                entries = any(),
                employeeName = any(),
                company = any(),
                project = any(),
                personnelNumber = any(),
                startDate = any(),
                endDate = any()
            )
        } returns PdfExporter.PdfExportResult.Success(fakeUri)

        vm.loadRange(startDate, endDate)
        vm.createPdf()

        assertTrue("Expected PdfReady state but got: ${vm.uiState.value}",
            vm.uiState.value is PreviewState.PdfReady)
        vm.viewModelScope.cancel()
    }

    @Test
    fun `returnToPreview restores cached List state without re-querying DAO`() = runTest {
        val entry = WorkEntry(date = startDate, dayType = DayType.WORK)
        coEvery { workEntryDao.getByDateRangeWithTravel(startDate, endDate) } returns listOf(
            WorkEntryWithTravelLegs(workEntry = entry, travelLegs = emptyList())
        )

        viewModel.loadRange(startDate, endDate)
        val listState = viewModel.uiState.value
        assertTrue(listState is PreviewState.List)

        viewModel.returnToPreview()

        assertTrue("Expected restored List state but got: ${viewModel.uiState.value}",
            viewModel.uiState.value is PreviewState.List)
        // Should not re-query DAO (only 1 call from loadRange)
        coVerify(exactly = 1) { workEntryDao.getByDateRangeWithTravel(startDate, endDate) }
    }
}
