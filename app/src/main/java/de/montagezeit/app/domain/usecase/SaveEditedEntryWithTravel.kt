package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.repository.WorkEntryRepository
import de.montagezeit.app.ui.screen.edit.EditEntryPendingSave
import javax.inject.Inject

class SaveEditedEntryWithTravel @Inject constructor(
    private val workEntryRepository: WorkEntryRepository
) {
    suspend operator fun invoke(pendingSave: EditEntryPendingSave): EditEntryPendingSave {
        workEntryRepository.replaceEntryWithTravelLegs(pendingSave.entry, pendingSave.legs)
        return pendingSave
    }
}
