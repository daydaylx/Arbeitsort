# Verifikation: `AUDIT_REPORT_2026-04-08`

**Datum der Verifikation:** 2026-04-08  
**Geprueft gegen:** aktueller Repo-Stand in `main`-Workspace  
**Methode:** Code- und Testabgleich, Reconciliation gegen `AUDIT_REPORT_2026-03-29.md`, Ausfuehrung der im Audit behaupteten Gradle-Checks

## Kurzfazit

Der Report ist in den **wichtigsten Top-Findings ueberwiegend korrekt**, vor allem bei:

- `C01` `RecordDailyManualCheckIn` ohne Guard fuer bestaetigte Eintraege
- `H01` kein direkter Test fuer `OverviewViewModel`
- `H03` PDF zeigt pro Zeile keinen Euro-Betrag fuer Verpflegungspauschale
- `H04` Preview zeigt alle Eintraege, PDF lehnt >180 Eintraege ab
- `M01` Inkonsistenz bei `COMP_TIME` mit Travel
- `M02` kein dedizierter 13->14-Migrationstest fuer migrierte Travel-Leg-Daten
- `M03` `exportSchema = false`

Der Report ist aber **nicht durchgehend sauber**. Es gibt mehrere **veraltete oder ueberzogene Aussagen**, und der Reconciliation-Anhang ist **numerisch inkonsistent**.

## Verifizierte Build-Claims

Die im Audit unter "Verifikationskommandos (alle bestanden)" genannten Commands wurden auf dem aktuellen Stand ausgefuehrt und sind **korrekt**:

- `./gradlew detekt` -> `BUILD SUCCESSFUL`
- `./gradlew lint :app:testDebugUnitTest assembleDebug` -> `BUILD SUCCESSFUL`

Damit sind die konkreten Build-/Lint-/Unit-Test-/Debug-Build-Behauptungen des Reports auf dem aktuellen Stand belegt.

## Korrekte Aussagen

| Audit-Aussage | Urteil | Beleg |
| --- | --- | --- |
| `C01`: bestaetigte Eintraege koennen in `RecordDailyManualCheckIn` ueberschrieben werden | korrekt | `RecordDailyManualCheckIn` kopiert `existingEntry` direkt weiter und setzt Felder neu, ohne Guard auf `confirmedWorkDay`: `app/src/main/java/de/montagezeit/app/domain/usecase/RecordDailyManualCheckIn.kt:48-89` |
| `H01`: `OverviewViewModel` hat keinen Test | korrekt | Repo-Suche findet keinen `OverviewViewModelTest`; vorhanden ist nur `OverviewCalculationsTest`: `app/src/test/java/de/montagezeit/app/ui/screen/overview/OverviewCalculationsTest.kt` |
| `H03`: PDF zeigt keinen taeglichen Euro-Betrag fuer Verpflegungspauschale | korrekt | Die PDF-Zeile zeichnet nur ein Fruehstueck-Flag, keinen Euro-Wert: `app/src/main/java/de/montagezeit/app/export/PdfExporter.kt:497-504`; die Preview hat dagegen `mealAllowanceLabel`: `app/src/main/java/de/montagezeit/app/ui/screen/export/ExportPreviewViewModel.kt:83-107` |
| `H04`: Preview/PDF-Mismatch bei >180 Eintraegen | korrekt | Preview laedt und rendert alle eligible Entries: `app/src/main/java/de/montagezeit/app/ui/screen/export/ExportPreviewViewModel.kt:328-349`; PDF blockiert ab `MAX_ENTRIES_PER_PDF = 180`: `app/src/main/java/de/montagezeit/app/export/PdfExporter.kt:50-53`, `:170-172` |
| `M01`: `COMP_TIME` mit Travel wird zwischen Statistik und Overtime inkonsistent behandelt | korrekt | `AggregateWorkStats` summiert blanketmaessig `totalTravelMinutes`: `app/src/main/java/de/montagezeit/app/domain/usecase/AggregateWorkStats.kt:73-74`; `CalculateOvertimeForRange` zaehlt bei `UEBERSTUNDEN_ABBAU` keine Ist-Zeit: `app/src/main/java/de/montagezeit/app/domain/usecase/CalculateOvertimeForRange.kt:68-74` |
| `M02`: kein eigener Test fuer migrierte Travel-Leg-Daten in `MIGRATION_13_14` | korrekt | Die Migration baut und befuellt `travel_legs`: `app/src/main/java/de/montagezeit/app/data/local/database/AppDatabase.kt:395-470`; der vorhandene Migrationstest prueft im 13->14-Kontext aber nur Tabellen-/Spaltenzustand, nicht migrierte Datensaetze: `app/src/test/java/de/montagezeit/app/data/local/database/AppDatabaseMigrationTest.kt:50-110` |
| `M03`: `exportSchema = false` verhindert Room-Schema-Export | korrekt | `app/src/main/java/de/montagezeit/app/data/local/database/AppDatabase.kt:14-18` |
| `M04`: `HistoryViewModel.applyBatchEdit` ist Read-then-Write ohne gemeinsame Transaction | korrekt | Erst `getWorkEntriesByDateRange`, danach `upsertWorkEntries`; keine umschliessende Transaction auf ViewModel-/UseCase-Ebene: `app/src/main/java/de/montagezeit/app/ui/screen/history/HistoryViewModel.kt:92-145` |
| `M05`: `currentRange` ist weiterhin mutable var, aber mit lokaler Snapshot-Mitigation | korrekt | `currentRange` ist `private var`; `refresh()` und `createPdf()` kopieren in lokales `range`: `app/src/main/java/de/montagezeit/app/ui/screen/export/ExportPreviewViewModel.kt:171-173`, `:180-218` |
| `M07`: `DefaultNonWorkingDayChecker` ist ungetestet | korrekt | Implementierung vorhanden: `app/src/main/java/de/montagezeit/app/work/DefaultNonWorkingDayChecker.kt`; Repo-Suche findet keinen `DefaultNonWorkingDayCheckerTest` |
| `T06`: es gibt doppelte/missplatzierte `ExportPreviewViewModelTest`-Dateien | korrekt | Zwei Testdateien mit gleichem Namen in unterschiedlichen Packages: `app/src/test/java/de/montagezeit/app/ui/export/ExportPreviewViewModelTest.kt`, `app/src/test/java/de/montagezeit/app/ui/screen/export/ExportPreviewViewModelTest.kt` |

## Teilweise korrekte oder ueberzogene Aussagen

| Audit-Aussage | Urteil | Einordnung |
| --- | --- | --- |
| `H02` / `T02`: `EditEntryViewModel` hat nur 2 Tests | teilweise korrekt / zu eng formuliert | Der direkte VM-Test hat tatsaechlich nur 2 `@Test`-Methoden: `app/src/test/java/de/montagezeit/app/ui/screen/edit/EditEntryViewModelTest.kt:51-130`. Als Abdeckungsurteil ist die Aussage aber zu stark, weil es zusaetzliche edit-nahe Tests fuer Validierung und Formularlogik im selben Feature gibt. |
| `T04`: DAO-Query-Flaeche unzureichend getestet | teilweise korrekt | Direkte DAO-Integrationstests sind duenn und decken `getByDateRangeWithTravel`/`replaceEntryWithTravelLegs` nicht sauber als echte DB-Tests ab. Es existiert aber indirekte Abdeckung ueber UseCase-/ViewModel-Tests mit Mocks, daher ist "unzureichend" vertretbar, aber nicht gleichbedeutend mit "gar nicht getestet". |
| `Scheinsicherheit: Keine truegerischen Tests gefunden` | zu stark formuliert | Das ist als harte Aussage nicht belastbar. Beispiel: `BootReceiverTest` testet nur eine nachgebildete Intent-Filter-Logik, nicht den echten Receiver-Pfad mit `scheduleAll()`: `app/src/test/java/de/montagezeit/app/receiver/BootReceiverTest.kt:8-40`. |
| `L10`: `contentDescription = null` in 30+ Composables | wahrscheinlich ueberzogen | Repo-Suche liefert aktuell 29 direkte Treffer fuer `contentDescription = null`. Das Problem ist real, die konkrete Zaehlung im Report passt auf dem aktuellen Stand aber nicht exakt. |

## Falsche oder veraltete Aussagen

| Audit-Aussage | Urteil | Beleg |
| --- | --- | --- |
| `W03` / `L11` / `L14`: Receiver `CoroutineScope` ohne Lifecycle / Leak | veraltet | Beide Receiver verwenden `goAsync()`, `SupervisorJob()`, `withTimeoutOrNull(...)` und `pendingResult.finish()`: `app/src/main/java/de/montagezeit/app/receiver/BootReceiver.kt:31-47`, `app/src/main/java/de/montagezeit/app/receiver/TimeChangeReceiver.kt:37-55` |
| Reconciliation: `A17` vollstaendig behoben | falsch / ueberzogen | Das Maerz-14-Problem war explizit "Kein Test fuer `scheduleAll()` nach Boot". Aktuell gibt es nur einen Spiegeltest fuer die Intent-Auswahl, keinen Test fuer echtes Rescheduling: `app/src/test/java/de/montagezeit/app/receiver/BootReceiverTest.kt:8-40` |
| Reconciliation-Zahlen `39 vollstaendig behoben`, `14 noch offen` | inkonsistent | Der Anhang listet sichtbar nur 33 vollstaendig behobene IDs (wenn `B20-B21` als zwei gezaehlt werden) und 10 offene IDs, nicht 39 bzw. 14: `docs/AUDITS/AUDIT_REPORT_2026-04-08.md`, Abschnitt "Anhang: Reconciliation vorheriger Befunde" |
| Testzaehlungen in Abschnitt 8 | teilweise falsch | Aktuell gezaehlt: `AggregateWorkStatsTest` 25 Tests, `ClassifyDayTest` 19, `TimeCalculatorTest` 13, `CalculateOvertimeForRangeTest` 6, `EditEntryViewModelTest` 2, `OverviewCalculationsTest` 4. Die Report-Angaben `24`, `18`, `20` sind auf dem aktuellen Stand nicht korrekt. |

## Reconciliation gegen `AUDIT_REPORT_2026-03-29.md`

Die inhaltliche Richtung stimmt nur teilweise:

- `H1` aus 2026-03-29 ist plausibel behoben.
- `H2` aus 2026-03-29 ist plausibel behoben.
- `A16` aus 2026-03-14 ist plausibel behoben, weil `ReminderNotificationManagerTest` heute echte Robolectric-Abdeckung hat: `app/src/test/java/de/montagezeit/app/notification/ReminderNotificationManagerTest.kt:16-112`.
- `A17` ist **nicht** vollstaendig belegt als behoben.

Damit ist die Reconciliation im Report **als Gesamtbild zu optimistisch**.

## Gesamtbewertung

Der Audit-Report ist **als technischer Problemanzeiger brauchbar**, aber **nicht als exakt verifizierter Statusbericht in allen Details**.

Pragmatisch bedeutet das:

- Die wichtigsten technischen Risiken im Report sind ernst zu nehmen.
- Die Build-/Lint-/Unit-Test-Aussagen sind auf dem aktuellen Stand korrekt.
- Die Receiver-bezogenen Leak-Aussagen sind veraltet.
- Die Testabdeckungs- und Reconciliation-Sektionen brauchen Nacharbeit, wenn der Report als belastbares Steuerungsdokument dienen soll.
