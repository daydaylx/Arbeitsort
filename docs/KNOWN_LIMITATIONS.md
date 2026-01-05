# Bekannte Limitationen - MontageZeit

Dieses Dokument listet alle bekannten Limitationen der MontageZeit App auf. Diese Limitationen sind bewusst für das MVP gewählt und können in zukünftigen Versionen adressiert werden.

## Konfiguration

### Radius-Berechnung
- **Limitation:** Der Radius ist auf 30km um Leipzig Zentrum hardcoded.
- **Auswirkung:** Anpassung des Radius erfordert Code-Änderung.
- **Workaround:** Manuelles Setzen in `LocationCalculator.kt`.

### GPS Timeout
- **Limitation:** GPS Timeout ist auf 10 Sekunden fixiert.
- **Auswirkung:** Balance zwischen Battery-Verbrauch und Genauigkeit.
- **Workaround:** Manuelles Anpassen in `LocationProviderImpl.kt`.

### Accuracy Threshold
- **Limitation:** Accuracy Threshold ist auf 3000m fixiert.
- **Auswirkung:** Standorte mit schlechterer Genauigkeit werden als "Low Accuracy" markiert.
- **Workaround:** Manuelles Anpassen in `LocationProviderImpl.kt`.

## Daten & Export

### CSV Format
- **Limitation:** Keine Export-Formatierungsoptionen (nur Semikolon-separiert).
- **Auswirkung:** Benutzer können Trennzeichen nicht ändern.
- **Workaround:** Manuelles Umwandeln in Excel/Google Sheets.

### JSON Export
- **Limitation:** JSON Export ist nicht implementiert (nur CSV).
- **Auswirkung:** Nur ein Export-Format verfügbar.
- **Workaround:** Manuelles Konvertieren mit externen Tools.

### Cloud Sync
- **Limitation:** Keine Cloud-Synchronisation (nur lokales Speichern).
- **Auswirkung:** Daten sind nur auf einem Gerät verfügbar.
- **Workaround:** Manueller Export und Import.

### Log-Größe
- **Limitation:** Max. 2MB Ringbuffer für Debug-Logs.
- **Auswirkung:** Ältere Logs werden überschrieben.
- **Workaround:** Regelmäßiges Exportieren der Logs vor Rotation.

## UI & UX

### DayType OFF
- **Limitation:** DayType OFF verhindert keine manuellen Check-ins.
- **Auswirkung:** Benutzer können an OFF-Tagen trotzdem check-ins machen.
- **Workaround:** Manuelles Deaktivieren in der UI.

### Multi-User
- **Limitation:** Keine Multi-User Unterstützung.
- **Auswirkung:** Nur ein Benutzer pro Gerät möglich.
- **Workaround:** Separate Geräte für verschiedene Benutzer.

### Sprachen
- **Limitation:** Nur Deutsch verfügbar.
- **Auswirkung:** Keine Internationalisierung.
- **Workaround:** Manuelles Übersetzen der Strings.

## Performance & Ressourcen

### Battery Verbrauch
- **Limitation:** Location-Abfragen verbrauchen Batterie.
- **Auswirkung:** Häufige Check-ins reduzieren die Akku-Laufzeit.
- **Workaround:** Weniger häufige Check-ins oder GPS deaktivieren.

### Speicherplatz
- **Limitation:** Keine automatische Bereinigung alter Einträge.
- **Auswirkung:** Datenbank wächst mit der Zeit.
- **Workaround:** Manuelles Löschen alter Einträge.

## Kompatibilität

### Android Version
- **Limitation:** Mindestens Android 7.0 (API 24) erforderlich.
- **Auswirkung:** Ältere Android-Versionen werden nicht unterstützt.
- **Workaround:** Upgrade auf neuere Android-Version.

### Samsung Sleep-Modus
- **Limitation:** Samsung Sleep-Modus kann Reminder verhindern.
- **Auswirkung:** Notifications werden nicht angezeigt.
- **Workaround:** Manuelles Deaktivieren des Sleep-Modus für die App (siehe README).

## Sicherheit & Datenschutz

### Datenverschlüsselung
- **Limitation:** Datenbank ist nicht verschlüsselt.
- **Auswirkung:** Bei Root-Zugriff sind Daten lesbar.
- **Workaround:** Kein Workaround (Future Feature).

### Backup
- **Limitation:** `allowBackup=false` (kein System-Backup).
- **Auswirkung:** Daten gehen bei Deinstallation verloren.
- **Workaround:** Manueller Export vor Deinstallation.

## Testing

### Code Coverage
- **Limitation:** Code Coverage ist nicht 100%.
- **Auswirkung:** Manuelle Tests erforderlich.
- **Workaround:** Ausführliche QA-Checkliste befolgen.

## Future Features

Diese Limitationen können in zukünftigen Versionen adressiert werden:

1. **Konfigurierbarer Radius** (Settings UI)
2. **JSON Export** (zusätzlich zu CSV)
3. **Cloud Sync** (Firebase/Backend)
4. **Multi-User Unterstützung** (Login/Accounts)
5. **Internationalisierung** (Mehrere Sprachen)
6. **Datenbank-Verschlüsselung** (SQLCipher)
7. **Automatische Datenbereinigung** (Retention Policy)
8. **Statistiken & Dashboards** (Arbeitszeiten, Reisen)
9. **Offline-Sync** (Cache & Sync bei Verbindung)
10. **Push Notifications** (Cloud Messaging)

---

**Version:** 1.0.0  
**Letzte Aktualisierung:** 2026-01-05  
**Status:** Production-Ready (mit bekannten Limitationen)
