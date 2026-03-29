package de.montagezeit.app.domain.model

/**
 * Klassifikation eines Tages basierend auf DayType, Arbeitszeit und Reisezeit.
 * 
 * Diese Unterscheidung ist wichtig für:
 * - Korrekte Zählung von Arbeitstagen
 * - Differenzierte Statistiken (Arbeit vs. Reise)
 * - Verpflegungspauschalen-Berechtigung
 * - UI-Darstellung und Exporte
 */
enum class DayClassification {
    /**
     * Freier Tag ohne Reisezeit (DayType.OFF oder COMP_TIME, keine Reise).
     * - Kein gezählter Tag
     * - Keine Ist-Stunden
     * - Keine Sollstunden
     * - Keine Verpflegungspauschale
     */
    FREI,
    
    /**
     * Freier Tag mit Reisezeit (z.B. Heimreise am Freitag).
     * - Kein gezählter Arbeitstag
     * - Reisezeit wird separat als "Fahrstunden an freien Tagen" ausgewiesen
     * - Keine Sollstunden
     * - Verpflegungspauschale möglich (fachlich zu klären)
     */
    FREI_MIT_REISE,
    
    /**
     * Arbeitstag mit Arbeitszeit (> 0 Minuten).
     * Kann zusätzlich Reisezeit haben.
     * - Gezählter Arbeitstag
     * - Ist-Stunden = Arbeitszeit + Reisezeit
     * - Sollstunden werden belastet
     * - Verpflegungspauschale möglich
     */
    ARBEITSTAG_MIT_ARBEIT,
    
    /**
     * Arbeitstag ohne Arbeitszeit, aber mit Reisezeit.
     * (z.B. Anreise zum Einsatzort, erster Tag)
     * - Gezählter Arbeitstag
     * - Ist-Stunden = Reisezeit (keine Arbeitszeit)
     * - Sollstunden werden belastet
     * - Verpflegungspauschale möglich
     */
    ARBEITSTAG_NUR_REISE,
    
    /**
     * Arbeitstag ohne Arbeitszeit und ohne Reisezeit.
     * (z.B. bestätigter Tag ohne erfasste Zeiten)
     * - Gezählter Arbeitstag
     * - Keine Ist-Stunden
     * - Sollstunden werden belastet
     * - Keine Verpflegungspauschale
     * 
     * WARNUNG: Dies sollte in der Regel durch Validierung verhindert werden,
     * da es auf vergessene Zeiterfassung hindeuten könnte.
     */
    ARBEITSTAG_LEER,
    
    /**
     * Tag zum Abbau von Überstunden (COMP_TIME).
     * - Zählt als Arbeitstag (Sollstunden werden abgezogen)
     * - Keine Ist-Stunden
     * - Keine Verpflegungspauschale
     */
    UEBERSTUNDEN_ABBAU;
    
    /**
     * true, wenn dieser Tag als Arbeitstag gezählt wird (für Sollstunden-Berechnung).
     */
    val isCountedWorkDay: Boolean
        get() = when (this) {
            ARBEITSTAG_MIT_ARBEIT, ARBEITSTAG_NUR_REISE, ARBEITSTAG_LEER, UEBERSTUNDEN_ABBAU -> true
            FREI, FREI_MIT_REISE -> false
        }

    /**
     * true, wenn dieser Tag Arbeitszeit enthält.
     */
    val hasWorkTime: Boolean
        get() = this == ARBEITSTAG_MIT_ARBEIT

    /**
     * true, wenn dieser Tag Reisezeit enthalten kann.
     *
     * Hinweis: ARBEITSTAG_MIT_ARBEIT kann mit oder ohne Reisezeit auftreten.
     * Diese Property gibt true zurück, weil dieser Tagtyp Reisezeit haben KANN.
     */
    val canHaveTravelTime: Boolean
        get() = when (this) {
            FREI_MIT_REISE, ARBEITSTAG_MIT_ARBEIT, ARBEITSTAG_NUR_REISE -> true
            FREI, ARBEITSTAG_LEER, UEBERSTUNDEN_ABBAU -> false
        }

    /**
     * true, wenn dieser Tag IMMER Reisezeit enthält.
     * (ARBEITSTAG_MIT_ARBEIT wird nicht zurückgegeben, da er optional Reisezeit haben kann)
     */
    val hasTravelTime: Boolean
        get() = when (this) {
            FREI_MIT_REISE, ARBEITSTAG_NUR_REISE -> true
            FREI, ARBEITSTAG_MIT_ARBEIT, ARBEITSTAG_LEER, UEBERSTUNDEN_ABBAU -> false
        }

    /**
     * true, wenn dieser Tag für Verpflegungspauschale berechtigt sein kann.
     */
    val isMealAllowanceEligible: Boolean
        get() = when (this) {
            FREI_MIT_REISE, ARBEITSTAG_MIT_ARBEIT, ARBEITSTAG_NUR_REISE -> true
            FREI, ARBEITSTAG_LEER, UEBERSTUNDEN_ABBAU -> false
        }
}