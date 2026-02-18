package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntry
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ResolveDayLocationPrefillTest {

    private val workEntryDao = mockk<WorkEntryDao>()
    private val useCase = ResolveDayLocationPrefill(workEntryDao)

    @Test
    fun `uses today label first`() = runTest {
        val entry = WorkEntry(
            date = LocalDate.now(),
            dayType = DayType.WORK,
            dayLocationLabel = "Heute Ort"
        )

        val result = useCase(entry)

        assertEquals("Heute Ort", result)
        coVerify(exactly = 0) { workEntryDao.getLatestDayLocationLabelByDayType(any()) }
        coVerify(exactly = 0) { workEntryDao.getLatestDayLocationLabel() }
    }

    @Test
    fun `uses latest work label before any label`() = runTest {
        coEvery { workEntryDao.getLatestDayLocationLabelByDayType(DayType.WORK) } returns "Werk A"
        coEvery { workEntryDao.getLatestDayLocationLabel() } returns "Any B"

        val result = useCase(existingEntry = null)

        assertEquals("Werk A", result)
        coVerify(exactly = 1) { workEntryDao.getLatestDayLocationLabelByDayType(DayType.WORK) }
        coVerify(exactly = 0) { workEntryDao.getLatestDayLocationLabel() }
    }

    @Test
    fun `uses latest any label when no work label exists`() = runTest {
        coEvery { workEntryDao.getLatestDayLocationLabelByDayType(DayType.WORK) } returns null
        coEvery { workEntryDao.getLatestDayLocationLabel() } returns "Any B"

        val result = useCase(existingEntry = null)

        assertEquals("Any B", result)
    }

    @Test
    fun `falls back to default city when no history exists`() = runTest {
        coEvery { workEntryDao.getLatestDayLocationLabelByDayType(DayType.WORK) } returns null
        coEvery { workEntryDao.getLatestDayLocationLabel() } returns null

        val result = useCase(existingEntry = null)

        assertEquals("Leipzig", result)
    }
}
