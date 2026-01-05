# QA Checkliste - MontageZeit MVP

**Letztes Update:** 2026-01-05  
**Status:** Ready for Implementation

## Übersicht

Diese Checkliste deckt die **Top 10 Tests** aus dem Tech Spec ab und mappt sie auf:
- **Unit Tests** (automatisiert, schnell)
- **Instrumented Tests** (Android-Integration)
- **Manuelle Checks** (Echtzeit-Validation)

**Ziel:** ≥ 90% Erfüllung bevor Release.

---

## Top 10 Tests (gemapped)

### 1. Radius-Check: Leipzig vs >30 km

**Spec-Referenz:** C1) Leipzig-Radius Check  
**Priorität:** P0 (Kern-Logik)

#### Unit Test
```kotlin
@Test
fun `distanceToLeipzig - returns correct distance in meters`() {
    // Leipzig Zentrum: 51.340, 12.374
    // 30km nördlich: 51.610, 12.374
    val distance = calculateDistanceToLeipzig(51.610, 12.374)
    
    assertThat(distance).isGreaterThan(30_000)
    assertThat(distance).isLessThan(31_000) // ±1km Toleranz
}
```

#### Manueller Check
- [ ] Check-in in Leipzig (z.B. Hauptbahnhof) → Label="Leipzig", `outsideLeipzig=false`
- [ ] Check-in in Dresden (~100km) → UI fragt nach Ortsname, `outsideLeipzig=true`
- [ ] Export CSV enthält korrekte Labels

---

### 2. Grenzzone: 28-32 km → Confirm Required

**Spec-Referenz:** C1) Grenzzone  
**Priorität:** P0 (Edge-Case)

#### Unit Test
```kotlin
@Test
fun `isOutsideLeipzig - borderline zone requires confirmation`() {
    val radiusKm = 30.0
    
    // 29km (Grenzzone)
    val result1 = isOutsideLeipzig(51.410, 12.374, radiusKm)
    assertThat(result1.isBorderZone).isTrue()
    
    // 35km (klar außerhalb)
    val result2 = isOutsideLeipzig(51.660, 12.374, radiusKm)
    assertThat(result2.isOutside).isTrue()
    assertThat(result2.isBorderZone).isFalse()
}
```

#### Manueller Check
- [ ] Check-in bei 28-32km (z.B. Lutherstadt Wittenberg) → UI zeigt Bestätigungsdialog
- [ ] User klickt "Ja, außerhalb" → `outsideLeipzig=true` + Label gespeichert
- [ ] User klickt "Nein, in Leipzig" → `outsideLeipzig=false` + Label="Leipzig"
- [ ] Bestätigung wird nicht wiederholt für denselben Tag

---

### 3. Low Accuracy → Outside = Unknown, needsReview=true

**Spec-Referenz:** C1) Genauigkeits-Regel  
**Priorität:** P0 (Datenqualität)

#### Unit Test
```kotlin
@Test
fun `recordMorningCheckIn - low accuracy sets needsReview`() {
    val locationResult = LocationResult.LowAccuracy(3500f) // >3000m
    
    val entry = recordMorningCheckIn(
        date = LocalDate.now(),
        locationResult = locationResult
    )
    
    assertThat(entry.morningLocationStatus).isEqualTo(LocationStatus.LOW_ACCURACY)
    assertThat(entry.needsReview).isTrue()
    assertThat(entry.outsideLeipzigMorning).isNull() // unknown, not guessed
}
```

#### Manueller Check
- [ ] GPS deaktivieren → Check-in → `locationStatus=UNAVAILABLE` + `needsReview=true`
- [ ] GPS aktivieren, aber indoor (Acc. 5000m) → Check-in → `locationStatus=LOW_ACCURACY` + `needsReview=true`
- [ ] Verlauf-Screen zeigt ⚠️-Badge für diesen Tag
- [ ] Export CSV enthält Status-Feld (LOW_ACCURACY/UNAVAILABLE)

---

### 4. Morning Check-in upsert setzt capturedAt + Defaults korrekt

**Spec-Referenz:** C2) Morning Check-in  
**Priorität:** P0 (Daten-Integrität)

#### Unit Test
```kotlin
@Test
fun `recordMorningCheckIn - upsert sets defaults correctly`() {
    val date = LocalDate.now()
    
    // Erstes Check-in (Insert)
    val entry1 = recordMorningCheckIn(date, null, forceWithoutLocation = false)
    
    assertThat(entry1.date).isEqualTo(date)
    assertThat(entry1.morningCapturedAt).isNotNull()
    assertThat(entry1.workStart).isEqualTo(LocalTime.of(8, 0)) // Default
    assertThat(entry1.workEnd).isEqualTo(LocalTime.of(19, 0))  // Default
    assertThat(entry1.breakMinutes).isEqualTo(60)
    
    // Zweites Check-in (Update)
    Thread.sleep(1000)
    val entry2 = recordMorningCheckIn(date, null, forceWithoutLocation = false)
    
    assertThat(entry2.morningCapturedAt).isNotEqualTo(entry1.morningCapturedAt)
}
```

#### Instrumented Test
```kotlin
@Test
fun `workEntryDao - upsert replaces existing entry`() {
    val date = LocalDate.now()
    val entry1 = createTestEntry(date)
    dao.upsert(entry1)
    
    val entry2 = createTestEntry(date).copy(note = "Updated")
    dao.upsert(entry2)
    
    val result = dao.getByDate(date)
    assertThat(result.note).isEqualTo("Updated")
    assertThat(result.morningCapturedAt).isNotEqualTo(entry1.morningCapturedAt)
}
```

#### Manueller Check
- [ ] Morgen Check-in → Entry in DB mit `capturedAt` = jetzt
- [ ] Settings ändern (Arbeitsende auf 18:00) → Neuer Check-in → `workEnd` = 18:00
- [ ] Verlauf zeigt aktualisierte Werte
- [ ] Export CSV korrekt

---

### 5. Evening Check-in überschreibt nicht kaputt (idempotent)

**Spec-Referenz:** C3) Evening Check-in  
**Priorität:** P0 (Stabilität)

#### Unit Test
```kotlin
@Test
fun `recordEveningCheckIn - idempotent without losing morning data`() {
    val date = LocalDate.now()
    
    // Morning Check-in
    val morningEntry = recordMorningCheckIn(date, mockSuccessLocation(51.340, 12.374))
    
    // Evening Check-in (mehrfach)
    val eveningEntry1 = recordEveningCheckIn(date, null)
    val eveningEntry2 = recordEveningCheckIn(date, null)
    
    assertThat(eveningEntry2.morningCapturedAt).isEqualTo(morningEntry.morningCapturedAt)
    assertThat(eveningEntry2.eveningCapturedAt).isNotEqualTo(eveningEntry1.eveningCapturedAt)
}
```

#### Manueller Check
- [ ] Morgen Check-in → Daten speichern
- [ ] Abend Check-in → Morning-Daten bleiben erhalten
- [ ] Abend Check-in nochmal (2. Mal) → Keine Duplikate, nur `eveningCapturedAt` aktualisiert
- [ ] Verlauf zeigt korrekte Morning + Evening Werte

---

### 6. Notification Action speichert Morning/Evening

**Spec-Referenz:** D1) Notifications mit Actions  
**Priorität:** P0 (User-Flow)

#### Instrumented Test
```kotlin
@Test
fun `notificationActionReceiver - handles morning check-in action`() {
    val intent = Intent(context, NotificationReceiver::class.java).apply {
        action = "ACTION_MORNING_CHECK_IN"
        putExtra("date", LocalDate.now().toString())
    }
    
    receiver.onReceive(context, intent)
    
    val entry = dao.getByDate(LocalDate.now())
    assertThat(entry.morningCapturedAt).isNotNull()
}
```

#### Manueller Check
- [ ] Morning Notification erscheint (06:00-13:00 Fenster)
- [ ] Tap auf "Check-in" Button → Standort abrufen (≤15s) → Entry gespeichert
- [ ] Tap auf "Ohne Standort" → Entry ohne Location gespeichert, `needsReview=true`
- [ ] Notification verschwindet nach Aktion
- [ ] Heute-Screen zeigt aktualisierten Status

---

### 7. Permission Denied → "Ohne Standort speichern" funktioniert

**Spec-Referenz:** E1) Permissions minimal + C2) Fallback  
**Priorität:** P0 (Graceful Degradation)

#### Unit Test
```kotlin
@Test
fun `recordMorningCheckIn - permission denied saves without location`() {
    val entry = recordMorningCheckIn(
        date = LocalDate.now(),
        locationResult = LocationResult.Unavailable,
        forceWithoutLocation = true
    )
    
    assertThat(entry.morningLocationStatus).isEqualTo(LocationStatus.UNAVAILABLE)
    assertThat(entry.needsReview).isTrue()
    assertThat(entry.morningCapturedAt).isNotNull() // Still captured
}
```

#### Manueller Check
- [ ] Location Permission in Settings deaktivieren
- [ ] App öffnen → "Check-in" tap
- [ ] UI zeigt "Standort nicht verfügbar" + "Ohne Standort speichern" Button
- [ ] Tap auf "Ohne Standort" → Entry gespeichert, `locationStatus=UNAVAILABLE`
- [ ] Verlauf zeigt ⚠️-Badge, aber keine Exception
- [ ] Export CSV enthält UNAVAILABLE-Status

---

### 8. Reboot → Reminder neu geplant (BOOT_COMPLETED)

**Spec-Referenz:** F1) Edge Cases: reboot  
**Priorität:** P0 (Zuverlässigkeit)

#### Instrumented Test
```kotlin
@Test
fun `bootReceiver - reschedules reminders after reboot`() {
    // Simuliere BOOT_COMPLETED Intent
    val intent = Intent(Intent.ACTION_BOOT_COMPLETED)
    context.sendBroadcast(intent)
    
    // Prüfe ob WorkManager Jobs existieren
    val workInfos = workManager.getWorkInfosByTag("morning_reminder").get()
    assertThat(workInfos).isNotEmpty()
    assertThat(workInfos[0].state).isOneOf(
        WorkInfo.State.ENQUEUED,
        WorkInfo.State.RUNNING
    )
}
```

#### Manueller Check
- [ ] App installieren + Reminder aktivieren
- [ ] Gerät neu starten
- [ ] Nach Reboot → WorkManager Jobs im Log sichtbar (`adb shell dumpsys jobscheduler`)
- [ ] Morgen (06:00-13:00) → Notification erscheint
- [ ] Abend (16:00-22:30) → Notification erscheint
- [ ] Wenn Reminder fehlen → Verlauf zeigt Badge "Reminder prüfen"

---

### 9. Offline → App startet, Verlauf ok, Export ok

**Spec-Referenz:** A4) Erfolgskriterien: Offline 100%  
**Priorität:** P0 (Offline-First)

#### Unit Test
```kotlin
@Test
fun `exportCsv - works without network`() {
    val entries = listOf(createTestEntry(), createTestEntry())
    val csvFile = exportCsv(entries)
    
    assertThat(csvFile.exists()).isTrue()
    assertThat(csvFile.readText()).contains("date;workStart;workEnd;...")
}
```

#### Manueller Check
- [ ] Flugmodus aktivieren / WLAN/Deaktiviert
- [ ] App starten → Keine Fehler, Heute-Screen lädt
- [ ] Check-in → Speichert lokal, keine Exception
- [ ] Verlauf → Alle Einträge sichtbar
- [ ] Export CSV → Datei wird erstellt, Share Intent funktioniert
- [ ] App schließen + neu starten → Daten persistieren
- [ ] `adb logcat` zeigt keine Network-Exceptions

---

### 10. DayType OFF verhindert Warnspam (keine "unvollständig" für freie Tage)

**Spec-Referenz:** D3) Datenmodell + B3) Zustände  
**Priorität:** P1 (User-Experience)

#### Unit Test
```kotlin
@Test
fun `getTodayEntry - dayType OFF has no missing snapshot warnings`() {
    val date = LocalDate.now()
    val entry = createTestEntry(date).copy(
        dayType = DayType.OFF,
        morningCapturedAt = null,
        eveningCapturedAt = null
    )
    
    dao.upsert(entry)
    val result = getTodayEntry(date)
    
    assertThat(result.dayType).isEqualTo(DayType.OFF)
    assertThat(result.needsReview).isFalse() // No warning for OFF days
}
```

#### Manueller Check
- [ ] Heute-Screen → DayType auf "Frei" setzen
- [ ] Keine Morning/Evening Snapshots → Kein ⚠️-Badge
- [ ] Verlauf-Screen → Tag als "Frei" markiert, keine Warnung
- [ ] Export CSV zeigt `dayType=OFF`
- [ ] Settings → Tag wieder auf "Arbeit" → Check-in Button wird sichtbar

---

## Zusätzliche Tests (Optional aber empfohlen)

### 11. CSV Export Format-Korrektheit

**Spec-Referenz:** F1) P0: Export CSV  
**Priorität:** P1

```kotlin
@Test
fun `exportCsv - format matches spec (semicolon separated, UTF-8)`() {
    val entries = listOf(
        WorkEntry(
            date = LocalDate.of(2026, 1, 5),
            workStart = LocalTime.of(8, 0),
            workEnd = LocalTime.of(19, 0),
            morningLocationLabel = "Leipzig",
            eveningLocationLabel = "Dresden"
        )
    )
    
    val csv = exportCsv(entries)
    
    assertThat(csv).contains("date;workStart;workEnd;breakMinutes;...")
    assertThat(csv.contains(";")).isTrue() // Semicolon separator
    assertThat(csv.contains(",")).isFalse() // NOT comma
    assertThat(csv.toByteArray(Charsets.UTF_8)).isNotNull()
}
```

### 12. Settings Persistenz (DataStore)

**Priorität:** P1

```kotlin
@Test
fun `settingsRepository - persists and retrieves settings`() {
    repository.updateSettings { it.copy(defaultWorkStart = LocalTime.of(9, 0)) }
    
    val settings = repository.getSettings().first()
    assertThat(settings.defaultWorkStart).isEqualTo(LocalTime.of(9, 0))
}
```

### 13. Samsung/One UI Sleep-Ausnahme

**Spec-Referenz:** B2) Settings Screen  
**Priorität:** P1 (Samsung-Spezifisch)

#### Manueller Check
- [ ] Auf Samsung-Gerät (One UI 5+)
- [ ] App installieren
- [ ] Settings-Screen zeigt Hinweis-Karte "Reminder optimieren"
- [ ] Tap auf "Einstellungen öffnen" → Führt zu Samsung Battery Optimization
- [ ] App aus Schlafliste entfernen
- [ ] Reminder erscheint zuverlässig (mehrere Tage testen)

---

## Test-Coverage-Ziele

| Kategorie | Ziel |
|-----------|------|
| Unit Tests | ≥ 80% Code Coverage (UseCases, Helpers) |
| Instrumented Tests | ≥ 60% Code Coverage (DAO, LocationProvider, Receiver) |
| Manual Checks | 100% der Top 10 Tests |

---

## Automatisierung

### CI/CD (Optional später)
- GitHub Actions für Unit + Instrumented Tests
- Automatischer Build bei jedem Push
- Code Coverage Report (JaCoCo)

### Lokale Test-Suite
```bash
# Unit Tests
./gradlew testDebugUnitTest

# Instrumented Tests
./gradlew connectedDebugAndroidTest

# Alle Tests
./gradlew test
```

---

## Bug-Tracking

Wenn ein Test fehlschlägt:
1. **Issue anlegen** mit:
   - Test-Nr. (z.B. "QA-3: Low Accuracy")
   - Schritte zum Reproduzieren
   - Erwartetes vs. Tatsächliches Ergebnis
   - Logcat / Screenshots
   - Device + Android Version

2. **Fix implementieren**
3. **Test wiederholen**
4. **Regression Check:** Kein anderer Test fehlschlägt

---

## Release-Kriterien

**Alle** der folgenden müssen erfüllt sein:
- [ ] Alle 10 Top-Tests bestanden (Unit + Instrumented + Manual)
- [ ] Code Coverage ≥ 80% (Unit), ≥ 60% (Instrumented)
- [ ] Keine P0/P1 Bugs offen
- [ ] Samsung Sleep-Ausnahme getestet (min. 1x)
- [ ] Offline-Workflow getestet (min. 1x)
- [ ] Export CSV validiert (Format, Encoding)
- [ ] Reboot-Szenario getestet (min. 1x)

---

## Next Steps

1. **Implementiere Tests parallel zu Features** (TDD ideal, aber pragmatisch)
2. **Führe manuelle Checks nach jedem Feature** durch
3. **Automatisiere Tests früh** um Refactoring-Risiko zu minimieren
4. **Dokumentiere Edge-Cases** (z.B. Samsung-Specifika)
5. **Regression-Suite** vor jedem Release laufen lassen

---

**Letzte Änderung:** 2026-01-05  
**Owner:** Senior Android Lead Engineer
