package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.repository.RoomWorkEntryRepository
import io.mockk.coEvery

internal fun testRepository(workEntryDao: WorkEntryDao) = RoomWorkEntryRepository(workEntryDao)

internal fun stubReadModifyWrite(
    workEntryDao: WorkEntryDao,
    existingEntry: WorkEntry?
) {
    coEvery { workEntryDao.readModifyWrite(any(), any()) } coAnswers {
        val modify = secondArg<(WorkEntry?) -> WorkEntry>()
        val updatedEntry = modify(existingEntry)
        workEntryDao.upsert(updatedEntry)
    }
}
