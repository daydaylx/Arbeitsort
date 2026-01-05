package de.montagezeit.app.domain.usecase

import android.net.Uri
import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.export.CsvExporter
import java.time.LocalDate
import javax.inject.Inject

/**
 * UseCase für das Exportieren von WorkEntries als CSV
 */
class ExportDataUseCase @Inject constructor(
    private val workEntryDao: WorkEntryDao,
    private val csvExporter: CsvExporter
) {
    
    /**
     * Exportiert alle WorkEntries aus einem bestimmten Zeitraum
     * 
     * @param startDate Startdatum (inklusive)
     * @param endDate Enddatum (inklusive)
     * @return Uri der exportierten CSV-Datei oder null bei Fehler
     */
    suspend operator fun invoke(startDate: LocalDate, endDate: LocalDate): Uri? {
        val entries = workEntryDao.getByDateRange(startDate, endDate)
        return csvExporter.exportToCsv(entries)
    }
    
    /**
     * Exportiert alle WorkEntries (ohne Zeitbegrenzung)
     * 
     * @return Uri der exportierten CSV-Datei oder null bei Fehler
     */
    suspend operator fun invoke(): Uri? {
        // Exportiere alle Einträge von Anfang bis heute
        val startDate = LocalDate.of(2020, 1, 1)
        val endDate = LocalDate.now()
        return invoke(startDate, endDate)
    }
}
