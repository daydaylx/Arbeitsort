package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.WorkEntry

/**
 * UseCase für manuelle Bearbeitung eines WorkEntry
 * 
 * Ermöglicht dem Benutzer, Einträge manuell zu korrigieren oder zu vervollständigen
 */
class UpdateEntry(
    private val workEntryDao: WorkEntryDao
) {
    
    /**
     * Aktualisiert einen WorkEntry mit den angegebenen Änderungen
     * 
     * @param entry Der zu aktualisierende WorkEntry
     * @return Der aktualisierte WorkEntry
     */
    suspend operator fun invoke(entry: WorkEntry): WorkEntry {
        val now = System.currentTimeMillis()
        
        // updatedAt immer aktualisieren
        val updatedEntry = entry.copy(
            updatedAt = now
        )
        
        workEntryDao.upsert(updatedEntry)
        return updatedEntry
    }
}
