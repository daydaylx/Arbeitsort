# APK Tech Spec (MVP) – „MontageZeit“
**Privates Stunden- & Ortslog für Montage (Offline-first)**  
**Owner:** Du  
**Ziel:** Jeden Arbeitstag zuverlässig festhalten **wo** du morgens warst und **wo** du abends warst, plus optional **Anreise/Abreise**.  
**Wichtig:** Uhrzeit-Genauigkeit ist egal. Standort-Logik ist das Produkt.

---

## A) Produkt-Konzept

### A1) Problem → Lösung

**Problem**
- Montage = wechselnde Orte.
- Du brauchst später nachvollziehbar:
  - **Wo war ich morgens?**
  - **Wo war ich abends?**
  - **Wann Anreise / Abreise?** (optional, aber wichtig)
- Nachträglich rekonstruieren ist ungenau und nervt.

**Lösung**
- Ultra-simple Offline-App, die pro Tag einen Eintrag führt.
- **2 Check-ins pro Tag** (Morning/Evening) als **Location-Snapshots**.
- Arbeitszeit wird als Default gesetzt: **08:00–19:00**, Pause Default **60 min**.
- Leipzig-Radius (30 km) entscheidet: **Leipzig** vs **Außerhalb** (dann kurzer Ortsname).
- Reminder sind **Zeitfenster-basiert**, nicht exakt: “irgendwann am Vormittag/Abend reicht”.

**Realitätscheck**
- Vollautomatische Standortlogik ohne Interaktion ist auf Android unzuverlässig (Doze/OEM-Killer/Permissions).
- Deshalb: **Notification + 1 Tap** als Hauptweg, plus **Fallbacks**.

---

### A2) Zielgruppe
- Primär: du (1 Nutzer, 1 Gerät, privat).
- Sekundär (später, optional): Kollegen. Nicht MVP.

---

### A3) Konkurrenz/Alternativen (kurz)
1) Timesheet-Apps: Overkill, Cloud, zu viel UI, falscher Fokus.  
2) Excel/Sheets: mobil nervig, keine Reminder, keine klare Tageslogik.  
3) Notizen/WhatsApp: chaotisch, keine Auswertung, schlecht editierbar.

---

### A4) Erfolgskriterien (messbar)
- **Erfassungsquote:** ≥ 90% der *Arbeitstage* haben bis 23:00 beide Snapshots (Morning + Evening) ODER sind als “Frei” markiert.
- **Time-to-log:** Notification → gespeichert in ≤ 10 Sekunden.
- **Offline:** 100% Kernfunktionen ohne Internet.
- **Datenverlust:** 0 (lokal robust, Export möglich).
- **Reliability:** Reminder kommt in der Praxis “meistens” in den Fenstern; wenn nicht, muss die App trotzdem Daten sicher halten und Nachtragen extrem einfach machen.

---

## B) UX & Screens (max 3)

### B1) Navigation
Bottom Nav (3 Tabs), weil du schnell sein willst:
1) **Heute**
2) **Verlauf**
3) **Einstellungen**

Keine Untermenü-Orgie.

---

### B2) Screens

#### 1) Heute
**Zweck:** Tageslog in Sekunden, Fokus auf Ort-Snapshots.

**Oben: „Statuskarte Heute“**
- Datum
- Morning Snapshot: ✅/⚠️/— (inkl. Label, z. B. “Leipzig” oder “Dresden”)
- Evening Snapshot: ✅/⚠️/— (inkl. Label)
- Default Arbeitszeit: **08:00–19:00**
- Pause: **60 min**
- Flag: **Außerhalb Leipzig (30 km)** (pro Snapshot)

**Primäraktionen (groß)**
- **„Morgens check-in“**
- **„Abends check-in“**
- **„Manuell bearbeiten“**

**Optional (einklappbar)**
- **Anreise Start**
- **Anreise Ende / Ankunft**
- **Abreise Start**
- (oder minimal nur 2: “Anreise Start/Ende”, je nachdem was du wirklich brauchst)

**Notiz (kurz)**
- Freitext max. 200–300 Zeichen

**Kleines Footer-Info**
- „Zuletzt gespeichert: …“
- „Standortgenauigkeit: …“ (nur wenn schlecht, sonst versteckt)

---

#### 2) Verlauf
**Zweck:** Nachtragen, korrigieren, exportieren.

**Liste nach Wochen (KW)**
- Eintrag pro Tag: Datum + Status (M/E) + Orte + Warnsymbol wenn:
  - Snapshot fehlt
  - Standort ungenau / nicht verfügbar
  - außerhalb Leipzig ohne Ortstext
- Tap → Edit (Modal/BottomSheet, kein eigener Tab nötig)

**Export**
- CSV (default)
- optional JSON (für Backup/Restore später)

---

#### 3) Einstellungen
**Zweck:** Defaults, Reminder-Fenster, Leipzig-Radius, Standortmodus.

**Defaults**
- Arbeitsbeginn Default: 08:00
- Arbeitsende Default: 19:00
- Pause Default: 60 min
- Leipzig-Radius: 30 km

**Reminder**
- Morning Window: z. B. **06:00–13:00**
- Evening Window: z. B. **16:00–22:30**
- Toggle: Reminder an/aus
- Fallback-Reminder: z. B. **22:30** einmalig, falls Tag unvollständig

**Standortmodus**
- A) Nur beim Check-in (recommended)
- B) Experimentell (nicht MVP): Hintergrundversuch (mit Warnhinweis)

**Samsung/One UI Hardening (kurz, ehrlich)**
- Hinweis-Karte: “Damit Reminder nicht sterben: App aus Schlaflisten ausnehmen”
- Button “Einstellungen öffnen” (führt in passende System-Seiten, soweit möglich)

**Daten**
- Export/Backup
- Import (nicht MVP, optional später)

---

### B3) Zustände (Loading/Error/Empty/Success)

**Heute**
- Loading: Standort holen (max 10–15s), minimaler Spinner
- Error Standort: “Standort nicht verfügbar” → Button “Ohne Standort speichern” → `needsReview=true`
- Empty: noch kein Eintrag → große Buttons
- Success: Karte gefüllt, Bearbeiten sichtbar

**Verlauf**
- Empty: “Noch keine Einträge”
- Error DB: “Export retten” + “Log exportieren”

**Einstellungen**
- praktisch immer Success, bei Problemen Defaults fallback

---

## C) Kernlogik (die wirklich zählt)

### C1) Leipzig-Radius Check
- Fixer Referenzpunkt: Leipzig Zentrum (im Code fest)
- Radius Default: 30 km
- Distanzberechnung: Haversine

**Genauigkeits-Regel (wichtig)**
- Wenn Standort-Accuracy zu schlecht (z. B. > 3000 m):
  - `locationStatus=LOW_ACCURACY`
  - `needsReview=true`
  - outsideLeipzig wird **nicht geraten**, sondern “unknown/needsReview”

**Grenzzone**
- Wenn Distanz nahe Radius (z. B. 28–32 km): immer bestätigen lassen.

---

### C2) Morning Check-in (zeitflexibel)
**Trigger:** Notification irgendwann im Morning Window **oder** Button im Heute-Screen.

**Ablauf**
1) Standort best effort (Timeout 10–15s)
2) Entry upsert für heute:
   - `workStart = 08:00`
   - `workEnd = 19:00` (optional bereits setzen oder erst abends)
   - `breakMinutes = 60` (optional erst abends)
   - `morningCapturedAt = now()`
3) Wenn Standort da:
   - Distanz prüfen → `outsideLeipzigMorning`
   - Wenn innerhalb: `morningLocationLabel="Leipzig"` (oder leer + “Leipzig”-Toggle)
   - Wenn außerhalb: UI fragt kurz “Ort (Text)” (1 Feld)
4) Wenn Standort nicht da/zu ungenau:
   - speichern ohne Ort, `needsReview=true`

---

### C3) Evening Check-in (zeitflexibel)
Analog:
- `eveningCapturedAt = now()`
- optional Ort erneut setzen (falls du tagsüber umgezogen bist)
- Wenn morgens außerhalb ohne Ortstext: abends nochmal nachholen lassen.

---

### C4) Travel (Anreise/Abreise) – MVP-Realismus
**Wichtig:** Das wird nicht automatisch zuverlässig ohne Tracking.

**MVP-Lösung**
- 1-Tap Actions (in App oder Notification):
  - “Anreise Start”
  - “Anreise Ende/Ankunft”
  - “Abreise Start” (optional)
- Speichert Timestamps + optional Standortlabel.
- Wenn du’s nie nutzt: bleibt leer, kein Drama.

---

## D) Technik

### D1) Tech Stack (empfohlen)
- Kotlin + Jetpack Compose
- Room (SQLite)
- DataStore (Settings)
- Location: Fused Location Provider (Play Services) + Fallback auf Android APIs
- Reminder/Scheduling:
  - **Fenster-basiert**, nicht exakt
  - je nach Umsetzung: AlarmManager (inexact) **oder** WorkManager + Fallbacks
- Notifications mit Actions (Check-in, ohne Standort, bearbeiten)

**Warum so?**
- Offline-first, robust, minimal.
- Keine Cloud, kein Login, keine Sync-Hölle.

---

### D2) Architektur (clean-ish, nicht akademisch)
**UI**
- Screens + ViewModels
- StateFlow

**Domain**
- UseCases:
  - `RecordMorningCheckIn`
  - `RecordEveningCheckIn`
  - `SetTravelEvent`
  - `UpdateEntry`
  - `ExportCsv`

**Data**
- Room DAO + Entities
- LocationProvider (abstrahiert)
- SettingsRepository (DataStore)

Single-module reicht fürs MVP.

---

### D3) Datenmodell (MVP)

**work_entries**
- `date: LocalDate` (unique)
- `workStart: LocalTime` (default 08:00)
- `workEnd: LocalTime` (default 19:00)
- `breakMinutes: Int` (default 60)

**Morning Snapshot**
- `morningCapturedAt: Long` (epoch)
- `morningLocationLabel: String?`
- `morningLat: Double?`, `morningLon: Double?`
- `morningAccuracyMeters: Float?`
- `outsideLeipzigMorning: Boolean?` (nullable/unknown)
- `morningLocationStatus: OK/UNAVAILABLE/LOW_ACCURACY`

**Evening Snapshot**
- analog: `eveningCapturedAt`, etc.

**Travel (optional)**
- `travelStartAt: Long?`
- `travelArriveAt: Long?`
- optional: `travelLabelStart`, `travelLabelEnd`

**Meta**
- `dayType: WORK/OFF/SICK/HOLIDAY` (mindestens WORK/OFF!)
- `needsReview: Boolean`
- `createdAt`, `updatedAt`

---

### D4) Logging/Crash Handling
- Lokales Log (Ringbuffer Datei, 1–2 MB)
- Export-Log nur wenn nötig
- Crashlytics/Sentry optional (privat meist Overkill/Datenschutz)

---

## E) Security & Privacy

### E1) Permissions minimal
- POST_NOTIFICATIONS (Android 13+)
- ACCESS_COARSE_LOCATION (reicht meist)
- Optional FINE nur, wenn du wirklich willst

Kein Background Location im MVP.

### E2) Datenschutz-Minimierung
- Standort nur beim Check-in
- Koordinaten optional speicherbar (Setting: “nur Label” möglich)
- Kein Upload, kein Sync

### E3) Android Backup Entscheidung (klar festlegen)
- Entweder strikt: `allowBackup=false` (privacy) + Export wichtig
- Oder bequem: `allowBackup=true` (Google Backup kann dann mitspielen)

---

## F) Umsetzungsplan

### F1) MVP Backlog (P0)
1) Room DB + DAO + Entities (inkl. dayType, capturedAt, status)
2) Heute Screen (Statuskarte + Buttons)
3) Morning/Evening UseCases + Upsert-Logik
4) LocationProvider + Accuracy/Timeout + Radius-Check + Grenzzone
5) Reminder Scheduling (fenster-basiert) + Notifications + Actions
6) Verlauf Screen (KW-Gruppierung + Warnsymbole) + Edit Modal
7) Settings (DataStore) + Defaults + Reminder-Fenster
8) Export CSV (Share Intent)
9) Edge Cases: permission denied, GPS off, reboot, time change
10) Samsung/One UI Setup-Karte (Sleep-Ausnahme Hinweis + Test-Reminder)

### F2) P1 (sinnvoll)
- Reverse Geocoding (nur wenn online, fallback “Unbekannt”)
- “Needs Review” Workflow (Badge + Filter)
- Travel Actions auch direkt in Notification

### F3) P2 (nur wenn du wirklich willst)
- Import/Restore
- Auswertungen (Wochen/Monat)
- Hintergrund-Autocapture (experimentell, kann lügen)

---

## G) Testplan (Top 10)

**Unit**
1) Radius-Check: Leipzig vs >30 km
2) Grenzzone: 29–31 km → confirm required
3) Low accuracy → outside = unknown, needsReview=true
4) Morning Check-in upsert setzt capturedAt + defaults korrekt
5) Evening Check-in überschreibt nicht kaputt (idempotent)

**Instrumented/UI**
6) Notification Action speichert Morning/Evening
7) Permission denied → “Ohne Standort speichern” funktioniert
8) Reboot → Reminder neu geplant (BOOT_COMPLETED)
9) Offline → App startet, Verlauf ok, Export ok
10) DayType OFF verhindert Warnspam (keine “unvollständig” für freie Tage)

---

## H) Risikoanalyse (Top)
1) Samsung killt Reminder → Onboarding + Sleep-Ausnahme + Test-Reminder
2) Standort indoor schlecht → Timeout + LowAccuracy + needsReview statt raten
3) User ignoriert Reminder → 1 Fallback + Verlauf-Badge, dann Ruhe
4) Reinstall → Export/Backup prominent
5) Scope Creep → bewusst “nicht im MVP”

---

## I) Scope-Grenzen (NICHT im MVP)
- Keine Cloud, kein Login, kein Sync
- Kein permanentes Tracking
- Keine Kartenansicht
- Keine Spesen/Belege
- Keine “Arbeitszeitrecht”-Validierung
- Keine komplexen Reports (CSV reicht)

---

## Kurz gesagt
Das ist keine Zeiterfassungs-App. Das ist ein **Ortsprotokoll mit Defaults** und zwei täglichen Snapshots. Genau das, was du brauchst. Android darf dabei “ungefähr” sein, ohne dass dein Ergebnis leidet.
```0
