package de.montagezeit.app.data.preferences

/**
 * Data class für PDF-Export Settings
 * Enthält Informationen für den PDF-Header (Name, Firma, etc.)
 */
data class PdfSettings(
    val employeeName: String? = null,
    val company: String? = null,
    val project: String? = null,
    val personnelNumber: String? = null
)