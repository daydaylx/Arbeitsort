# Reminder & Notification System

## Übersicht

Das Reminder & Notification System implementiert fensterbasierte Erinnerungen für morgendlichen und abendlichen Check-in mit Notification Actions. Das System nutzt WorkManager für zuverlässiges, deferrable Scheduling und ist reboot-resilient.

## Architektur

### Komponenten

#### 1. DataStore Settings (`data/preferences/`)
- **ReminderSettings.kt**: Data class mit allen Konfigurationen
- **ReminderSettingsManager.kt**: DataStore Wrapper für Persistenz

**Konfigurationen:**
- Arbeitszeit Defaults (workStart, workEnd, breakMinutes, radius)
- Morning Window (06:00-13:00, Intervall 120min)
- Evening Window (16:00-22:30, Intervall 180min)
- Fallback (22:30, einmal täglich)

#### 2. WindowCheckWorker (`work/`)
Prüft regelmäßig ob Reminder notwendig sind:
- Läuft mehrfach im Fenster (deferrable scheduling)
- Prüft WorkEntry DB ob Snapshot fehlt
- Prüft ob dayType == WORK
- Vermeidet mehrfache Erinnerungen pro Tag (SharedPreferences Flag)
- **Wichtig**: Pro Worker-Typ nur im eigenen Fenster aktiv, sonst sofort `Result.success()`

**Logik:**
```kotlin
if (inMorningWindow && !alreadyReminded && (entry == null || entry.dayType == WORK && entry.morningCapturedAt == null)) {
    showMorningReminder()
    setReminderFlag()
}
```

#### 3. ReminderScheduler (`work/`)
Plant WindowCheckWorker mit UniqueWork:
- **Morning Worker**: Startet 06:00 (oder sofort im Fenster), wiederholt alle 2 Stunden
- **Evening Worker**: Startet 16:00 (oder sofort im Fenster), wiederholt alle 3 Stunden
- **Fallback Worker**: Startet 22:30, wiederholt täglich

**Constraints:**
- NetworkType.NOT_REQUIRED (Offline OK)
- RequiresBatteryNotLow = false

#### 4. ReminderNotificationManager (`notification/`)
Erstellt Notifications mit Actions:
- **Morning Actions**: "Mit Standort check-in", "Ohne Standort speichern"
- **Evening Actions**: Analog
- **Fallback Action**: "Eintrag bearbeiten"

**Notification Channel:**
- ID: `reminder_notifications`
- Importance: HIGH (Sound + Vibration)

#### 5. CheckInActionService (`handler/`)
ForegroundService für Check-In Actions:
- Verwendet ForegroundService statt BroadcastReceiver (Location-Timeout 15s+)
- Ruft RecordMorningCheckIn/RecordEveningCheckIn UseCases auf
- Zeigt Toast bei Erfolg/Fehler
- Cancelt Reminder-Notification nach Check-in

**Warum ForegroundService?**
- Location-Requests mit Timeout können länger dauern als BroadcastReceiver lebt
- ForegroundService hat höhere Priorität und wird vom System nicht abgebrochen

#### 6. BootReceiver (`receiver/`)
Reboot-Resilienz:
- Empfängt BOOT_COMPLETED, QUICKBOOT_POWERON, MY_PACKAGE_REPLACED
- Plant alle Reminder-Worker neu

#### 7. Settings UI (`ui/screen/`)
**ReminderSettingsScreen.kt** mit:
- Toggle für Morning/Evening/Fallback
- Time Picker für Fenster-Start/Ende
- Slider für Intervalle
- Samsung Hardening Info-Karte
- Buttons zu Battery Optimization / App Details

## Funktionsweise

### Morning Reminder Flow

1. **06:00**: WindowCheckWorker startet
2. **Prüfung**:
   - Ist 06:00-13:00? Ja
   - Morning-Captured-At == null? Ja
   - dayType == WORK? Ja
   - Schon erinnert? Nein
3. **Action**: Zeige Notification mit Actions
4. **User Klick**: Startet CheckInActionService
5. **Service**:
   - Location-Request mit 15s Timeout
   - Ruft RecordMorningCheckIn auf
   - Speichert in DB
   - Zeigt Toast
   - Cancelt Notification

### Evening Reminder Flow

Analog zu Morning, aber:
- Fenster: 16:00-22:30
- Ruft RecordEveningCheckIn auf

### Fallback Reminder Flow

1. **22:30**: Fallback Worker startet
2. **Prüfung**:
   - Ist nach 22:30? Ja
   - Tag unvollständig? Ja
   - Schon erinnert? Nein
3. **Action**: Zeige Fallback-Notification
4. **User Klick**: Öffnet Edit Screen (TODO)

## Bekannte Limits & Edge Cases

### OS-Restrictions

**Samsung One UI:**
- Killt Hintergrund-Prozesse aggressiv
- **Lösung**: Hardening Screen mit Battery Optimization / App Details Buttons
- User muss "Unrestricted" setzen

**Battery Optimization:**
- Android 6.0+ kann WorkManager blockieren
- **Lösung**: User wird aufgefordert, App zu whitelisten

**Doze Mode:**
- Verzögert WorkManager Ausführung
- **Lösung**: WorkManager ist resilient, führt Worker später aus

### Location Issues

**GPS indoors:**
- GPS kann indoors nicht verfügbar sein
- **Lösung**: "Ohne Standort speichern" Action → needsReview=true

**Low Accuracy:**
- Accuracy zu niedrig (< 50m)
- **Lösung**: LocationStatus.LOW_ACCURACY → needsReview=true

**Timeout:**
- Location-Request timeout nach 15s
- **Lösung**: LocationStatus.UNAVAILABLE → needsReview=true

### User Experience

**Mehrfache Erinnerungen:**
- Können nerven wenn User schon gecheck-in hat
- **Lösung**: SharedPreferences Flag "remindedToday" (pro Tag und Typ)

**DayType=OFF:**
- Sollte keine Reminder auslösen
- **Lösung**: Worker prüft entry.dayType == WORK

**Weekend/Feiertage:**
- Derzeit nicht implementiert
- **TODO**: Settings für Weekend/Feiertage hinzufügen

## Scheduling-Strategie

### Keine exakten Alarme

**Warum nicht AlarmManager.setExact()?**
- Permission-Hölle (SCHEDULE_EXACT_ALARM)
- Battery Optimization killt Alarms
- Samsung/MIUI Blockierungen
- Doze Mode verzögert

**Lösung: WorkManager deferrable scheduling**
```kotlin
PeriodicWorkRequestBuilder<WindowCheckWorker>(2, TimeUnit.HOURS)
    .setInitialDelay(delayTo06:00)
    .setConstraints(noNetworkRequired)
    .setInputData(workDataOf("reminder_type" to "MORNING"))
    .build()
```

### Window Check Strategie

**Strategie**: Worker läuft mehrfach im Fenster
- Morning: alle 2 Stunden im Morning Window
- Evening: alle 3 Stunden im Evening Window
- Fallback: 1x nach 22:30

**Implementierung**:
```kotlin
// Morning: Alle 2 Stunden im Fenster
PeriodicWorkRequestBuilder<WindowCheckWorker>(2, TimeUnit.HOURS)
    .setInitialDelay(delayTo06:00)
    .setInputData(workDataOf("reminder_type" to "MORNING"))
    .addTag("morning_reminder")
    .build()
// Worker prüft nur im Fenster und beendet sich sonst sofort
```

## Testing

### Unit Tests
- `ReminderWindowEvaluatorTest`: Prüft Fensterlogik (ohne Android Framework)
- `ReminderSettingsManagerTest`: Persistenz

### Instrumented Tests
- `CheckInActionServiceTest`: Testet Actions
  - Morning Check-in mit Location speichert DB-Entry
  - Morning Check-in ohne Location setzt needsReview=true
  - Evening Check-in analog

### Manuelle Tests
1. Reminder Scheduler starten
2. Time manipulieren (adb shell date)
3. Notifications prüfen
4. Actions auslösen
5. DB-Einträge verifizieren

## AndroidManifest Permissions

```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
```

## Initialisierung

In `MontageZeitApplication.onCreate()`:
```kotlin
lifecycleScope.launch {
    reminderScheduler.scheduleAll()
}
```

## Troubleshooting

### Reminder erscheinen nicht

1. **Permissions prüfen**:
   ```bash
   adb shell pm grant de.montagezeit.app android.permission.POST_NOTIFICATIONS
   ```

2. **Battery Optimization**:
   - Settings → Apps → MontageZeit → Battery → Unrestricted

3. **WorkManager Status**:
   ```bash
   adb shell dumpsys jobscheduler | grep de.montagezeit.app
   ```

### Notifications erscheinen aber Actions funktionieren nicht

1. **Foreground Service prüfen**:
   ```bash
   adb shell dumpsys activity services de.montagezeit.app
   ```

2. **Location Permission prüfen**:
   ```bash
   adb shell pm grant de.montagezeit.app android.permission.ACCESS_FINE_LOCATION
   ```

3. **Logs prüfen**:
   ```bash
   adb logcat | grep -E "CheckInActionService|WindowCheckWorker"
   ```

## TODOs

- [ ] TimePickerDialog implementieren (aktuell Placeholder)
- [ ] Edit Action öffnet MainActivity mit Edit-Route
- [ ] Weekend/Feiertage Settings
- [ ] Reminder History Screen
- [ ] Snooze Action (erinnere in 10 Minuten)
- [ ] Custom Reminder Times (pro Tag)

## Referenzen

- [WorkManager Documentation](https://developer.android.com/topic/libraries/architecture/workmanager)
- [Notifications Guide](https://developer.android.com/develop/ui/views/notifications)
- [Foreground Service Guide](https://developer.android.com/guide/components/foreground-services)
- [Battery Optimization Guide](https://developer.android.com/topic/performance/power/background-restrictions)
