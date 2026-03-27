# MontageZeit Audit Report

Erstellt: 2026-03-14

---

## BEREICH A – HAUPTLISTE DER FUNDE

### Durchgang 1: Basis-Audit (A01–A20)

| ID | Schweregrad | Kategorie | Titel | Status | Fundort | Beleg | Auswirkung | Reproduzierbarkeit | Empfohlene Priorität |
|----|-------------|-----------|-------|--------|---------|-------|------------|-------------------|---------------------|
| A01 | hoch | Zustand/Daten | ReminderFlagsStore: StringSets wachsen unbegrenzt | bestätigt | `data/preferences/ReminderFlagsStore.kt:110-161` | `setMorningReminded()` etc. fügen Dates zum StringSet hinzu (`+ date.toString()`), aber es gibt keinen Cleanup-Mechanismus. Nach 1 Jahr: 365+ Einträge × 4 Sets. | DataStore-Reads werden langsamer, Speicherverbrauch steigt stetig, potenzielle ANR bei alten Geräten | klar reproduzierbar | sofort |
| A02 | hoch | Logik | CheckInActionService: ACTION_REMIND_LATER ohne try-catch und stopSelf()-Garantie | bestätigt | `handler/CheckInActionService.kt:178-185` | `serviceScope.launch { scheduleReminderLater(...); stopSelf() }` – kein try-catch. Wenn `scheduleReminderLater()` eine Exception wirft, wird `stopSelf()` nie aufgerufen. | Service-Leak: Service bleibt aktiv bis System-Kill | bedingt reproduzierbar | sofort |
| A03 | hoch | Zustand | TodayViewModel: todayEntry trackt immer nur das Datum der Erstellung | bestätigt | `ui/screen/today/TodayViewModel.kt:114` | `workEntryDao.getByDateFlow(LocalDate.now())` – `LocalDate.now()` wird einmal beim ViewModel-Init evaluiert. | Wenn App über Mitternacht offen bleibt: todayEntry zeigt gestern, Stats falsch | klar reproduzierbar | sofort |
| A04 | hoch | Performance | TodayViewModel: observeEntryUpdates ohne Debouncing | bestätigt | `ui/screen/today/TodayViewModel.kt:228-234` | `selectedEntry.collect { loadStatistics(); loadWeekOverview() }` – jede Änderung startet 2 DB-Coroutines ohne Debounce. | UI-Ruckeln, unnötige DB-Last bei schnellem Swipen | klar reproduzierbar | kurzfristig |
| A05 | mittel | Dokumentation | CsvExporter: Docstring verspricht Exception, Code gibt null zurück | bestätigt | `export/CsvExporter.kt:33,37-40` | KDoc: `@throws IllegalArgumentException`. Code: `return null`. | Stiller Fehler, kein Feedback an User | klar reproduzierbar | kurzfristig |
| A06 | mittel | Race Condition | ReminderFlagsStore: @Volatile check-then-set nicht threadsafe | wahrscheinlich | `data/preferences/ReminderFlagsStore.kt:54-59` | Zwei Coroutines können gleichzeitig passieren. Idempotent → praktisch harmlos. | Theoretisch doppelte Migration | nur potenziell | später |
| A07 | mittel | Sicherheit | CheckInActionService: Keine Validierung Intent-Extras | bestätigt | `handler/CheckInActionService.kt:90-91,113,135,153,190,211,232,259` | `LocalDate.parse(dateStr)` ohne try-catch in allen 8 Action-Handlern. | Service-Crash bei korruptem Extra | bedingt reproduzierbar | kurzfristig |
| A08 | mittel | Build | ProGuard RingBufferLogger-Regel wirkungslos | bestätigt | `proguard-rules.pro:126-130` | Regel zielt auf `void d(...)`, Methoden sind `suspend fun`-Extensions → Signatur-Mismatch. | Debug-Logging in Release aktiv | klar reproduzierbar | kurzfristig |
| A09 | mittel | Architektur | Cross-Layer-Dependency domain → work | bestätigt | `domain/usecase/SetDayLocation.kt:8,34` + `ui/screen/today/TodayViewModel.kt:479` | Domain/UI importiert direkt aus Work-Package. | Erhöhte Kopplung, erschwerte Tests | klar reproduzierbar | später |
| A10 | ~~mittel~~ | ~~Zustand~~ | ~~EditEntryViewModel DB auf Main~~ | **verworfen** | — | Room-suspend-DAO wechselt automatisch Thread. | Kein Problem. | — | — |
| A11 | mittel | Robustheit | RingBufferLogger: Rotation löscht bei Fehler alles | bestätigt | `logging/RingBufferLogger.kt:107-109` | `catch (e: Exception) { logFile.delete() }` | Totaler Logverlust bei transientem Fehler | bedingt reproduzierbar | kurzfristig |
| A12 | niedrig | UI | Rapid Delete Undo-Snackbar | abgeschwächt | `ui/screen/today/TodayScreen.kt:105-117` | LaunchedEffect-Cancellation handhabt dies korrekt. | Kein echtes Problem. | — | — |
| A13 | niedrig | Logik | SetDayType: shouldClearMealAllowance unnötig komplex | bestätigt | `domain/usecase/SetDayType.kt:39` | Logik korrekt, aber verwirrend. | Wartungskomplexität | n/a | später |
| A14 | niedrig | Robustheit | PdfExporter: Generischer Exception-Catch → null | bestätigt | `export/PdfExporter.kt:188-190` | Alle Fehler geschluckt. | Stiller Export-Fehler | klar reproduzierbar | später |
| A15 | niedrig | Tests | CheckInActionServiceTest: Service gemockt | bestätigt | `test/.../CheckInActionServiceTest.kt:41` | `mockk(relaxed = true)` – kein echter Test. | Bugs A02/A07 unentdeckt | n/a | kurzfristig |
| A16 | niedrig | Tests | Keine Tests für ReminderNotificationManager | bestätigt | `notification/ReminderNotificationManager.kt` | Komplett ungetestet. | Regressions unbemerkt | n/a | später |
| A17 | niedrig | Tests | Keine Tests für Receiver | bestätigt | `receiver/BootReceiver.kt`, `TimeChangeReceiver.kt` | Kein Test für scheduleAll() nach Boot. | Scheduling-Fehler unbemerkt | n/a | später |
| A18 | niedrig | Build | Lint NewApi pauschal deaktiviert | bestätigt | `app/build.gradle.kts:59-68` | Echte API-Probleme maskiert. | Crashes auf alten Geräten | n/a | später |
| A19 | niedrig | Daten | Migration-Sentinel StringSet statt Boolean | Verdacht | `ReminderFlagsStore.kt:42,62` | Ungewöhnlich, kein Laufzeitproblem. | Code-Clarity | n/a | später |
| A20 | niedrig | Architektur | Duplizierte Entry-Konstruktion | bestätigt | `RecordDailyManualCheckIn.kt:48-128` | 2× ~40 Zeilen dupliziert. | Wartungsrisiko | n/a | später |

---

### Durchgang 2: Deep-Dive Funde (B01–B25)

| ID | Schweregrad | Kategorie | Titel | Status | Fundort | Beleg | Auswirkung | Reproduzierbarkeit | Empfohlene Priorität |
|----|-------------|-----------|-------|--------|---------|-------|------------|-------------------|---------------------|
| B01 | hoch | Recomposition | TodayScreen: 25+ separate StateFlow-Collectionen | bestätigt | `ui/screen/today/TodayScreen.kt:56-80` | 25 einzelne `collectAsStateWithLifecycle()`-Aufrufe. Jede einzelne Änderung triggert Recomposition des gesamten Screens. | Full-Screen-Recomposition bei jedem StateFlow-Update, Performance-Degradation | klar reproduzierbar | sofort |
| B02 | hoch | Race Condition | Alle UseCases: Read-then-Write ohne Transaktion | bestätigt | `SetDayType.kt:35-77`, `RecordDailyManualCheckIn.kt:36-131`, `EditEntryViewModel.kt:293-426` | Pattern: `val entry = dao.getByDate(date); ...; dao.upsert(modified)` – kein Room-@Transaction. Concurrent Writes überschreiben sich gegenseitig. | Datenverlust bei gleichzeitigen Operationen (Worker + UI, oder schnelle Doppel-Taps) | bedingt reproduzierbar | sofort |
| B03 | hoch | Datenfluss | RecordDailyManualCheckIn: Überschreibt bestätigte Einträge | bestätigt | `domain/usecase/RecordDailyManualCheckIn.kt:48-99` | Kein Guard-Check für `confirmedWorkDay==true`. Ein bereits bestätigter Tag wird bei erneutem Check-In komplett überschrieben. | Bestätigte Daten (inkl. Meal Allowance, Travel) können stillschweigend verloren gehen | klar reproduzierbar | sofort |
| B04 | hoch | Datenfluss | Notification Check-In: Stale Date nicht validiert | bestätigt | `handler/CheckInActionService.kt:89-91,111-113` | Notification enthält `EXTRA_DATE` vom Erstellungszeitpunkt. Keine Prüfung ob `date == LocalDate.now()`. User kann eine gestrige Notification antippen und Check-In für falsches Datum auslösen. | Check-In für falsches Datum ohne Warnung | klar reproduzierbar | kurzfristig |
| B05 | hoch | Race Condition | CheckInActionService: Concurrent serviceScope.launch | bestätigt | `handler/CheckInActionService.kt:72,95-108,117-130` | `CoroutineScope(Dispatchers.IO + SupervisorJob())` – mehrere Intents können gleichzeitige DB-Writes auslösen ohne Synchronisierung (kein Mutex wie in WindowCheckWorker). | Race bei simultanem Morning + Evening Check-In → ein Write überschreibt den anderen | bedingt reproduzierbar | kurzfristig |
| B06 | hoch | Datenfluss | Undo nach Datumswechsel: Entry für falsches Datum wiederhergestellt | bestätigt | `ui/screen/today/TodayViewModel.kt:546-575` | Delete setzt `_deletedEntryForUndo`. User wechselt Datum. User tappt Undo. Entry wird in DB upserted (korrekt für ursprüngliches Datum), aber UI zeigt neues Datum → User sieht "kein Eintrag" statt restored Entry. | Verwirrende UX: Undo scheint nicht zu funktionieren, obwohl Entry korrekt wiederhergestellt wurde | klar reproduzierbar | kurzfristig |
| B07 | hoch | Datenfluss | ExportPreviewViewModel: Range-Wechsel während Export | bestätigt | `ui/screen/export/ExportPreviewViewModel.kt:162,170-230` | `currentRange` ist mutable var. Wenn User Range ändert während Coroutine läuft: Entries von alter Range, aber Header/Filename von neuer Range. | PDF/CSV mit inkonsistentem Inhalt vs. Header | bedingt reproduzierbar | kurzfristig |
| B08 | mittel | Race Condition | TodayViewModel.selectDate: Non-atomic Read-Modify | bestätigt | `ui/screen/today/TodayViewModel.kt:362-387` | `wasAlreadySelected` und `isDateInCurrentWeek` lesen State, dann startet async DB-Query. Bei schnellem Doppel-Click: zweiter Click liest stale State. | UI zeigt kurzzeitig Daten für falsches Datum | bedingt reproduzierbar | kurzfristig |
| B09 | mittel | Race Condition | HistoryViewModel.applyBatchEdit: Nicht-atomare Batch-Writes | bestätigt | `ui/screen/history/HistoryViewModel.kt:79-147` | `for (date in dates) { dao.upsert(entry) }` – einzelne Writes in Schleife statt @Transaction. | Inkonsistenter Zustand bei App-Crash während Batch | bedingt reproduzierbar | kurzfristig |
| B10 | mittel | Datenfluss | DailyCheckIn: selectedDate kann sich während Dialog ändern | bestätigt | `ui/screen/today/TodayViewModel.kt:389-465` | Dialog wird für `_selectedDate.value` geöffnet (Zeile 392), Submit nutzt erneut `_selectedDate.value` (Zeile 450). Dazwischen kann User Datum wechseln. | Check-In für falsches Datum | bedingt reproduzierbar | kurzfristig |
| B11 | mittel | Datenfluss | CSV: Semicolon-Escaping unvollständig | bestätigt | `export/CsvExporter.kt:98,124` | Semicolons → Komma, aber Kommas im Original bleiben. Bei `"Ort A,B;C"` → `"Ort A,B,C"`. | CSV-Import kann Spalten falsch zuordnen | klar reproduzierbar | kurzfristig |
| B12 | mittel | Datenfluss | Edit: Travel-Timestamps timezone-abhängig | bestätigt | `ui/screen/edit/EditEntryViewModel.kt:464-468` | `date.atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()` – systemDefault kann sich ändern. | Falsche Travel-Zeiten bei Timezone-Wechsel | bedingt reproduzierbar | später |
| B13 | mittel | UI/Compose | TextFields ohne IME-Action-Konfiguration | bestätigt | `ui/screen/today/TodayScreen.kt:765-771,849-855` | Kein `keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)`. | Kein "Fertig"-Button auf Tastatur, User muss außerhalb tippen | klar reproduzierbar | kurzfristig |
| B14 | mittel | UI/Compose | HistoryScreen: forEach + key() statt LazyColumn items() | bestätigt | `ui/screen/history/HistoryScreen.kt:1473-1480` | `entriesToShow.forEach { entry -> key(entry.date) { ... } }` – umgeht LazyColumn-Recycling. | Unnötige Recompositions, Performance bei langen Listen | klar reproduzierbar | kurzfristig |
| B15 | mittel | Accessibility | Checkbox-Rows ohne Role.Checkbox Semantik | bestätigt | `ui/screen/today/TodayScreen.kt:778-809` | Row ist clickable, aber ohne `semantics { role = Role.Checkbox }`. Screen-Reader können Zusammenhang nicht erkennen. | Accessibility-Problem für Screenreader-User | klar reproduzierbar | kurzfristig |
| B16 | mittel | Accessibility | WeekOverviewRow: Touch-Target < 48dp | bestätigt | `ui/screen/today/WeekOverviewRow.kt:91-107` | Width 44dp minus Padding → effektiv ~36-40dp. WCAG 2.1 AA fordert min. 48dp. | Schwer zu treffen für motorisch eingeschränkte User | klar reproduzierbar | kurzfristig |
| B17 | mittel | Concurrency | BootReceiver/TimeChangeReceiver: CoroutineScope ohne Lifecycle-Management | bestätigt | `receiver/BootReceiver.kt:33-40`, `receiver/TimeChangeReceiver.kt:40-51` | `CoroutineScope(Dispatchers.IO)` ohne SupervisorJob, scope kann garbage-collected werden bevor `pendingResult.finish()` läuft. | Receiver-Callback nicht abgeschlossen → System warnt/killt | nur potenziell | später |
| B18 | mittel | UI/Compose | EditEntrySheet: LaunchedEffect(uiState) zu breit | bestätigt | `ui/screen/edit/EditEntrySheet.kt:70-74` | Watched den gesamten uiState. Re-runs auch bei Success→Success-Wechsel (z.B. validationErrors geändert). | Unnötige Effect-Ausführung, potenzielle Doppel-Dismissals | bedingt reproduzierbar | später |
| B19 | mittel | UI/Compose | TodayScreen: Lambda-Captures nicht memoized | bestätigt | `ui/screen/today/TodayScreen.kt:90-97` | Lambdas mit `viewModel`-Capture werden bei jeder Recomposition neu erstellt → bricht Memoization in Child-Composables. | Performance-Regression durch fehlende Referenzstabilität | klar reproduzierbar | später |
| B20 | niedrig | Datenfluss | Undo-Entry nur in RAM, kein Crash-Recovery | bestätigt | `ui/screen/today/TodayViewModel.kt:206-207,554` | `_deletedEntryForUndo` ist StateFlow in ViewModel. Bei App-Crash → Entry verloren. | Datenverlust bei Crash während Undo-Fenster | bedingt reproduzierbar | später |
| B21 | niedrig | Build | ProGuard: Overly-broad Keep-Rules | bestätigt | `proguard-rules.pro:27,50-59` | `-keep class dagger.hilt.** { *; }`, `-keep class androidx.work.** { *; }` – hält zu viel, mindert Minification. | Größere APK als nötig | klar reproduzierbar | später |
| B22 | niedrig | Build | ProGuard: Moshi-Regeln aber Moshi nicht im Projekt | bestätigt | `proguard-rules.pro:77-81` | `-keep class com.squareup.moshi.**` – Moshi nicht in build.gradle.kts. Toter Code. | Kein Laufzeitproblem, nur Noise | klar reproduzierbar | später |
| B23 | niedrig | UI/Compose | HistoryScreen: 7 separate mutableStateOf statt Datenklasse | bestätigt | `ui/screen/history/HistoryScreen.kt:157-163` | 7 einzelne State-Variablen → 7 Recomposition-Scopes. | Unnötige Recompositions | klar reproduzierbar | später |
| B24 | niedrig | Datenfluss | DayType OFF→WORK: Meal Allowance bleibt 0 | bestätigt | `domain/usecase/SetDayType.kt:39,51-62` | Wechsel OFF→WORK: shouldClearMealAllowance=true (OFF hatte 0, bleibt 0). Kein Auto-Recalculate. | Neuer WORK-Tag hat 0 Meal Allowance bis manuelles Check-In | klar reproduzierbar | später |
| B25 | niedrig | Datenfluss | COMP_TIME→WORK: Travel unwiederbringlich verloren | bestätigt | `domain/usecase/SetDayType.kt:42,51-62` | WORK→COMP_TIME: `withTravelCleared()`. COMP_TIME→WORK: Travel nicht wiederhergestellt. | User muss Travel-Daten erneut eingeben | klar reproduzierbar | später |

---

## BEREICH B – VALIDIERUNG NACH 2. DURCHGANG

### Basis-Audit Funde (A-Serie)

| ID | Ergebnis | Begründung |
|----|----------|------------|
| A01 | **bestätigt + erweitert** | Deep-Dive bestätigt: Auch `setAllReminded()` erweitert 4 Sets gleichzeitig. Kein Cleanup in gesamter Codebasis. |
| A02 | **bestätigt + erweitert** | `ACTION_REMIND_LATER_CONFIRMATION` (Zeile 250-254) hat identisches Problem. Zusätzlich: concurrent serviceScope.launch (→ B05). |
| A03 | **bestätigt + erweitert** | `loadStatistics()` nutzt ebenfalls `LocalDate.now()` (Zeile 240) → auch Wochen-/Monats-/Jahresstatistiken betroffen. |
| A04 | **bestätigt** | Deep-Dive bestätigt: flatMapLatest emittiert initial null bei jedem Datumswechsel → collect feuert. |
| A05 | **bestätigt** | Keine Änderung. |
| A06 | **bestätigt, abgeschwächt** | DataStore.edit() intern synchronisiert. Migration idempotent. Risiko theoretisch. |
| A07 | **bestätigt + erweitert** | Betrifft alle 8 Action-Handler (Zeilen 91, 113, 135, 153, 190, 211, 232, 259). |
| A08 | **bestätigt** | Deep-Dive bestätigt: Kotlin suspend-Extension → JVM-Signatur `Object d(... Continuation)`, matched nicht auf `void d(...)`. |
| A09 | **bestätigt + erweitert** | Auch `TodayViewModel.kt:479` importiert direkt aus Work-Package. |
| A10 | **verworfen** | Room-suspend-DAO wechselt automatisch auf Background-Thread. |
| A11 | **bestätigt** | Keine Änderung. |
| A12 | **abgeschwächt → niedrig** | LaunchedEffect-Cancellation handhabt korrekt. |
| A13–A20 | **bestätigt** | Keine wesentlichen Änderungen. |

### Deep-Dive Funde (B-Serie)

| ID | Ergebnis | Begründung |
|----|----------|------------|
| B01 | **bestätigt** | Zählung verifiziert: exakt 25 collectAsStateWithLifecycle()-Aufrufe in TodayScreen. |
| B02 | **bestätigt** | Kein einziger UseCase nutzt @Transaction. Pattern überall identisch. |
| B03 | **bestätigt** | RecordDailyManualCheckIn Zeile 48-52: `existingEntry != null` → Modify ohne Guard. |
| B04 | **bestätigt** | Datum aus Notification-Extra wird nie gegen LocalDate.now() validiert. |
| B05 | **bestätigt** | WindowCheckWorker nutzt Mutex, CheckInActionService nicht – Inkonsistenz. |
| B06 | **bestätigt** | Entry wird korrekt in DB upserted, aber UI-selectedDate stimmt nicht → UX-Confusion. |
| B07 | **bestätigt** | `currentRange` ist plain `var`, nicht StateFlow. Keine Synchronisierung. |
| B08 | **bestätigt, leicht abgeschwächt** | In der Praxis selten: User müsste innerhalb von Millisekunden doppelt klicken. |
| B09 | **bestätigt** | Batch-Edit iteriert und schreibt einzeln ohne @Transaction. |
| B10 | **bestätigt** | Datum wird bei Dialog-Open UND Submit von _selectedDate.value gelesen – dazwischen änderbar. |
| B11 | **bestätigt** | Kein RFC-4180-konformes CSV-Escaping (keine Quoting-Logik). |
| B12 | **bestätigt** | Betrifft nur User die Timezone wechseln. Für Zielgruppe (deutsche Monteure) selten. |
| B13–B25 | **bestätigt** | Keine Änderungen nach Gegenprüfung. |

---

## BEREICH C – ANHANG MIT KURZERKLÄRUNGEN

### A01 – ReminderFlagsStore: Unbegrenztes StringSet-Wachstum
**Was?** Die DataStore-StringSets akkumulieren Datumseinträge ohne Cleanup.
**Warum problematisch?** DataStore serialisiert bei jedem Read/Write. 365+ Einträge × 4 Sets = zunehmend langsam.
**Wodurch?** Fehlender Cleanup (z.B. Einträge älter als 7 Tage entfernen).
**Bedingung:** Verschlechtert sich kontinuierlich mit jeder Nutzung.

### A02 – CheckInActionService: stopSelf() nicht garantiert
**Was?** REMIND_LATER und REMIND_LATER_CONFIRMATION ohne try-finally.
**Warum problematisch?** Service-Leak bei Exception.
**Wodurch?** stopSelf() steht nach der fehlbaren Operation, nicht in finally.

### A03 – TodayViewModel: Stale LocalDate.now()
**Was?** todayEntry, loadStatistics(), loadWeekOverview() nutzen einmalig evaluiertes LocalDate.now().
**Warum problematisch?** Nach Mitternacht: alle Anzeigen beziehen sich auf gestern.
**Wodurch?** Kein Refresh-Mechanismus bei Datumswechsel.

### A04 – TodayViewModel: Fehlende Debounce
**Was?** selectedEntry.collect triggert sofort 2 DB-Queries.
**Warum problematisch?** Bei schnellem Swipen: parallele Queries, Interleaving.
**Wodurch?** Kein debounce()/conflate() auf dem Collector.

### A05 – CsvExporter: Docstring/Code-Mismatch
**Was?** KDoc: throws Exception. Code: return null.
**Wodurch?** Inkonsistenz bei Refactoring nicht korrigiert.

### A07 – CheckInActionService: DateTimeParseException
**Was?** LocalDate.parse() in 8 Handlern ohne try-catch.
**Wodurch?** Fehlende Input-Validierung am System-Boundary.

### A08 – ProGuard: Suspend-Extension-Mismatch
**Was?** ProGuard-Regel matched nicht auf Kotlin suspend-Extension-Functions.
**Wodurch?** suspend-Functions kompilieren zu `Object method(..., Continuation)`, nicht `void method(...)`.

### B01 – TodayScreen: 25+ StateFlow-Collectionen
**Was?** Jeder einzelne StateFlow wird separat collected. Ein Update an irgendeinem Flow triggert Full-Screen-Recomposition.
**Warum problematisch?** Compose kann Recomposition-Scopes nur auf Ebene der State-Reads eingrenzen. 25 Reads am Screen-Root = kein Scoping möglich.
**Wodurch?** ViewModel exponiert 25+ individuelle StateFlows statt konsolidierten UI-State.

### B02 – UseCases: Read-then-Write ohne @Transaction
**Was?** Pattern `getByDate() → modify → upsert()` in allen schreibenden UseCases.
**Warum problematisch?** Zwischen Read und Write kann ein anderer Prozess (Worker, zweiter Intent) denselben Eintrag ändern. Last-Write-Wins → Datenverlust.
**Wodurch?** Fehlende Room-@Transaction-Annotationen, kein Optimistic Locking.

### B03 – RecordDailyManualCheckIn: Überschreibt bestätigte Einträge
**Was?** Kein Guard für `confirmedWorkDay==true`.
**Warum problematisch?** User bestätigt Tag, öffnet Dialog erneut, submitted → Bestätigung überschrieben.
**Wodurch?** Fehlender Schutzmechanismus im UseCase.

### B04 – Stale Notification Date
**Was?** Notification enthält Erstellungsdatum. Keine Validierung gegen aktuelles Datum.
**Warum problematisch?** Alte Notification → Check-In für falsches Datum.
**Wodurch?** Kein Date-Guard im Service.

### B05 – CheckInActionService: Concurrent Launches
**Was?** Mehrere Intents können gleichzeitige serviceScope.launch starten.
**Warum problematisch?** Parallele DB-Writes ohne Mutex (Inkonsistenz zu WindowCheckWorker).
**Wodurch?** Fehlende Synchronisierung im Service.

### B06 – Undo nach Datumswechsel
**Was?** Entry wird korrekt für ursprüngliches Datum wiederhergestellt, aber UI zeigt anderes Datum.
**Warum problematisch?** User denkt Undo hat nicht funktioniert.
**Wodurch?** Undo aktualisiert nicht die UI-Ansicht auf das wiederhergestellte Datum.

### B07 – ExportPreviewViewModel: Range-Race
**Was?** currentRange ist mutable var, änderbar während Export-Coroutine läuft.
**Wodurch?** Kein StateFlow/Mutex für currentRange.

### B10 – DailyCheckIn: Date-Drift zwischen Dialog-Open und Submit
**Was?** _selectedDate.value wird zweimal gelesen: bei Open und bei Submit.
**Wodurch?** Kein Capture des Datums beim Dialog-Öffnen.

### B11 – CSV: Unvollständiges Escaping
**Was?** Semicolons ersetzt, aber kein RFC-4180-Quoting.
**Wodurch?** Kommas, Zeilenumbrüche, Anführungszeichen nicht korrekt escaped.

### B13 – TextFields ohne IME-Action
**Was?** Kein `ImeAction.Done` → kein "Fertig"-Button auf Tastatur.
**Wodurch?** Fehlende `keyboardOptions`/`keyboardActions`-Parameter.

### B14 – HistoryScreen: forEach statt items()
**Was?** Manuelles `forEach + key()` statt LazyColumn `items()`.
**Warum problematisch?** Umgeht Compose-Recycling-Mechanismus.

### B15/B16 – Accessibility: Checkbox-Semantik und Touch-Targets
**Was?** Fehlende Role.Checkbox, Touch-Targets unter 48dp.
**Wodurch?** Fehlende semantics-Modifier und zu kleine Dimensions.

---

## ZUSAMMENFASSUNG (nach Schweregrad sortiert)

### Top 15 Probleme

1. **B02** (hoch) – Alle UseCases: Read-then-Write ohne @Transaction → Datenverlust-Risiko
2. **B01** (hoch) – TodayScreen: 25+ StateFlow-Collections → Full-Screen-Recomposition
3. **B03** (hoch) – RecordDailyManualCheckIn überschreibt bestätigte Einträge
4. **A01** (hoch) – ReminderFlagsStore StringSets wachsen unbegrenzt
5. **A02** (hoch) – CheckInActionService REMIND_LATER ohne try-finally → Service-Leak
6. **A03** (hoch) – TodayViewModel stale nach Mitternacht
7. **A04** (hoch) – TodayViewModel keine Debounce bei Entry-Updates
8. **B05** (hoch) – CheckInActionService concurrent Launches ohne Mutex
9. **B04** (hoch) – Notification Check-In: Stale Date nicht validiert
10. **B06** (hoch) – Undo nach Datumswechsel: UI zeigt falsches Datum
11. **B07** (hoch) – ExportPreviewViewModel: Range-Wechsel während Export
12. **A07** (mittel) – Intent-Extras ohne Validierung → Crash
13. **A08** (mittel) – ProGuard RingBufferLogger-Regel wirkungslos
14. **B10** (mittel) – DailyCheckIn: Date-Drift zwischen Dialog-Open und Submit
15. **B11** (mittel) – CSV: Unvollständiges Escaping

### Blind Spots / nicht vollständig verifizierbar

- **Notification-Delivery auf Samsung-Geräten**: Sleep Mode / Battery Optimization – ohne reales Gerät nicht testbar
- **WorkManager Exact-Timing**: `SCHEDULE_EXACT_ALARM` nicht im Manifest – Android 12+ Verhalten unklar ohne Device-Test
- **ProGuard Release-Build**: Ob R8 den Code korrekt minifiziert – nur mit tatsächlichem Release-Build verifizierbar
- **Room-Migrationen**: Migrationskette 1→11 analysiert, aber ohne vollständigen Integrationstest nicht 100% sicher
- **Compose Recomposition-Performance**: Ohne Layout Inspector / Profiling nicht messbar
- **Concurrent WorkManager-Worker**: Runtime-abhängig, ob zwei Worker gleichzeitig laufen
- **Memory-Verhalten bei großen Datenmengen**: Kein Profiling möglich → Export mit 1000+ Entries ungetestet
- **Timezone-Edge-Cases**: Travel-Timestamps bei DST-Wechsel – ohne Device in verschiedenen Zeitzonen nicht prüfbar
