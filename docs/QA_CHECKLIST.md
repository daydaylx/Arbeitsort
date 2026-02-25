# QA Checkliste - MontageZeit

**Letztes Update:** 2026-02-25
**Status:** Ready for Testing

## Übersicht

Diese Checkliste deckt die **Top 10 Tests** für das manuelle Check-in System ab und mappt sie auf:
- **Unit Tests** (automatisiert, schnell)
- **Manuelle Checks** (Echtzeit-Validation)

**Ziel:** ≥ 90% Erfüllung bevor Release.

---

## Top 10 Tests

### 1. Daily Check-in speichert Tagesort

**Priorität:** P0 (Kern-Logik)

#### Unit Test
```kotlin
@Test
fun `recordDailyManualCheckIn - saves day location and confirms work day`() {
    val date = LocalDate.now()
    val locationLabel = "Baustelle Mitte"

    val entry = recordDailyManualCheckIn(
        date = date,
        dayLocationLabel = locationLabel
    )

    assertThat(entry.dayLocationLabel).isEqualTo(locationLabel)
    assertThat(entry.confirmedWorkDay).isTrue()
    assertThat(entry.dayType).isEqualTo(DayType.WORK)
}
```

#### Manueller Check
- [ ] Today Screen → "Einchecken (Arbeit)" tap
- [ ] Dialog mit Tagesort-Input erscheint
- [ ] Ort eingeben oder Prefill wählen → Speichern
- [ ] Tag wird als abgeschlossen markiert (`confirmedWorkDay = true`)
- [ ] Export CSV enthält den Tagesort

---

### 2. Tagesort Prefill funktioniert korrekt

**Priorität:** P0 (User-Experience)

#### Unit Test
```kotlin
@Test
fun `resolveDayLocationPrefill - returns last work day location first`() {
    // Einträge mit verschiedenen Tagesorten erstellen
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)

    insertEntry(today.minusDays(5), "Ort A", DayType.WORK)
    insertEntry(yesterday, "Ort B", DayType.WORK)
    insertEntry(today.minusDays(2), "Ort C", DayType.OFF)

    val prefill = resolveDayLocationPrefill(today)

    assertThat(prefill).isEqualTo("Ort B") // Letzter Work-Tag
}
```

#### Manueller Check
- [ ] Vorherige Arbeitstage mit verschiedenen Ortslabels erstellen
- [ ] Today Screen öffnen → Prefill zeigt letzten Arbeitsort
- [ ] Wenn kein Work-Tag existiert → Settings-Default wird verwendet
- [ ] Prefill kann überschrieben werden

---

### 3. DayType OFF verhindert Warnspam

**Priorität:** P1 (User-Experience)

#### Unit Test
```kotlin
@Test
fun `confirmOffDay - creates off day without review warnings`() {
    val date = LocalDate.now()

    val entry = confirmOffDay(date)

    assertThat(entry.dayType).isEqualTo(DayType.OFF)
    assertThat(entry.needsReview).isFalse()
}
```

#### Manueller Check
- [ ] Today Screen → "Heute frei" tap
- [ ] Tag wird als OFF markiert
- [ ] Keine Morning/Evening Snapshots erforderlich
- [ ] Keine ⚠️-Badge im Verlauf
- [ ] Export CSV zeigt `dayType=OFF`

---

### 4. COMP_TIME reduziert Überstundenkonto

**Priorität:** P1 (Feature)

#### Unit Test
```kotlin
@Test
fun `recordDailyManualCheckIn - COMP_TIME day type handled correctly`() {
    val date = LocalDate.now()

    val entry = recordDailyManualCheckIn(
        date = date,
        dayType = DayType.COMP_TIME,
        dayLocationLabel = "Homeoffice"
    )

    assertThat(entry.dayType).isEqualTo(DayType.COMP_TIME)
    assertThat(entry.confirmedWorkDay).isTrue()
}
```

#### Manueller Check
- [ ] Edit-Screen für Tag öffnen
- [ ] DayType auf "Überstundenabbau" ändern
- [ ] Tag wird mit COMP_TIME gespeichert
- [ ] Export CSV zeigt `dayType=COMP_TIME`

---

### 5. Morning Check-in via Notification

**Priorität:** P0 (Reminder-Flow)

#### Manueller Check
- [ ] Morning Reminder erscheint (06:00-13:00 Fenster)
- [ ] Tap auf "Check-in" Button → Morning Snapshot erfasst
- [ ] Notification verschwindet nach Aktion
- [ ] Today Screen zeigt aktualisierten Status

---

### 6. Evening Check-in via Notification

**Priorität:** P0 (Reminder-Flow)

#### Manueller Check
- [ ] Evening Reminder erscheint (16:00-22:30 Fenster)
- [ ] Tap auf "Check-in" Button → Evening Snapshot erfasst
- [ ] Morning-Daten bleiben erhalten
- [ ] Today Screen zeigt beide Snapshots als erfasst

---

### 7. Reboot → Reminder neu geplant

**Priorität:** P0 (Zuverlässigkeit)

#### Manueller Check
- [ ] App installieren + Reminder aktivieren
- [ ] Gerät neu starten
- [ ] Nach Reboot → WorkManager Jobs im Log sichtbar (`adb shell dumpsys jobscheduler`)
- [ ] Morgen (06:00-13:00) → Notification erscheint
- [ ] Abend (16:00-22:30) → Notification erscheint

---

### 8. Offline → App startet, Verlauf ok, Export ok

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
- [ ] Flugmodus aktivieren / WLAN deaktivieren
- [ ] App starten → Keine Fehler, Today-Screen lädt
- [ ] Check-in → Speichert lokal, keine Exception
- [ ] Verlauf → Alle Einträge sichtbar
- [ ] Export CSV → Datei wird erstellt
- [ ] App schließen + neu starten → Daten persistieren

---

### 9. Travel-Events werden gespeichert

**Priorität:** P1 (Feature)

#### Unit Test
```kotlin
@Test
fun `setTravelEvent - saves travel data correctly`() {
    val date = LocalDate.now()

    val entry = setTravelEvent(
        date = date,
        startLabel = "Zuhause",
        endLabel = "Baustelle"
    )

    assertThat(entry.travelFromLabel).isEqualTo("Zuhause")
    assertThat(entry.travelToLabel).isEqualTo("Baustelle")
}
```

#### Manueller Check
- [ ] Edit-Screen für Tag öffnen
- [ ] Travel-Daten eingeben (Start/Ende, Labels)
- [ ] Speichern → Travel-Daten in Verlauf sichtbar
- [ ] Export CSV enthält Travel-Felder

---

### 10. DayType WORK vs OFF vs COMP_TIME

**Priorität:** P0 (Daten-Integrität)

#### Unit Test
```kotlin
@Test
fun `getTodayEntry - day types are preserved correctly`() {
    val workEntry = createTestEntry(dayType = DayType.WORK)
    val offEntry = createTestEntry(dayType = DayType.OFF)
    val compEntry = createTestEntry(dayType = DayType.COMP_TIME)

    dao.upsert(workEntry)
    dao.upsert(offEntry)
    dao.upsert(compEntry)

    assertThat(dao.getByDate(workEntry.date).dayType).isEqualTo(DayType.WORK)
    assertThat(dao.getByDate(offEntry.date).dayType).isEqualTo(DayType.OFF)
    assertThat(dao.getByDate(compEntry.date).dayType).isEqualTo(DayType.COMP_TIME)
}
```

#### Manueller Check
- [ ] Mehrere Tage mit verschiedenen DayTypes anlegen
- [ ] Verlauf zeigt korrekte Icons/Labels für jeden Typ
- [ ] Export CSV enthält korrekte `dayType` Werte
- [ ] Statistik berücksichtigt alle Typen korrekt

---

## Zusätzliche Tests (Optional aber empfohlen)

### 11. CSV Export Format-Korrektheit

**Priorität:** P1

```kotlin
@Test
fun `exportCsv - format matches spec (semicolon separated, UTF-8)`() {
    val entries = listOf(
        WorkEntry(
            date = LocalDate.of(2026, 2, 25),
            workStart = LocalTime.of(8, 0),
            workEnd = LocalTime.of(19, 0),
            dayLocationLabel = "Baustelle"
        )
    )

    val csv = exportCsv(entries)

    assertThat(csv).contains("date;dayType;workStart;...")
    assertThat(csv.contains(";")).isTrue() // Semicolon separator
    assertThat(csv.contains(",")).isFalse() // NOT comma
}
```

### 12. Settings Persistenz (DataStore)

**Priorität:** P1

```kotlin
@Test
fun `settingsRepository - persists and retrieves settings`() {
    repository.updateDefaultWorkStart(LocalTime.of(9, 0))

    val settings = repository.getSettings().first()
    assertThat(settings.defaultWorkStart).isEqualTo(LocalTime.of(9, 0))
}
```

### 13. Samsung/One UI Sleep-Ausnahme

**Priorität:** P1 (Samsung-Spezifisch)

#### Manueller Check
- [ ] Auf Samsung-Gerät (One UI 5+)
- [ ] Settings-Screen zeigt Hinweis-Karte "Reminder optimieren"
- [ ] Tap auf "Einstellungen öffnen" → Führt zu Samsung Battery Optimization
- [ ] App aus Schlafliste entfernen
- [ ] Reminder erscheint zuverlässig

---

## Test-Coverage-Ziele

| Kategorie | Ziel |
|-----------|------|
| Unit Tests | ≥ 80% Code Coverage (UseCases, Helpers) |
| Manual Checks | 100% der Top 10 Tests |

---

## Bug-Tracking

Wenn ein Test fehlschlägt:
1. **Issue anlegen** mit:
   - Test-Nr. (z.B. "QA-3: DayType OFF")
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
- [ ] Alle 10 Top-Tests bestanden (Unit + Manual)
- [ ] Code Coverage ≥ 80% (Unit)
- [ ] Keine P0/P1 Bugs offen
- [ ] Samsung Sleep-Ausnahme getestet (min. 1x)
- [ ] Offline-Workflow getestet (min. 1x)
- [ ] Export CSV validiert (Format, Encoding)
- [ ] Reboot-Szenario getestet (min. 1x)

---

**Letzte Änderung:** 2026-02-25
**Version:** 1.0.1
