# Tagesklassifikation

## Überblick

Die Tagesklassifikation steuert in MontageZeit drei fachlich getrennte Dinge:

1. sichtbare Arbeitstage in Verlauf/PDF
2. Sollstunden-/Überstunden-Zähltage
3. Verpflegungspauschalen für Statistik und Export

Die Klassifikation wird nur für fachlich bestätigte Tage verwendet. Unbestätigte Tage gehen nicht in Statistik, Export-Summen oder Überstundenlogik ein.

## Kategorien

| Klassifikation | Beschreibung | Sichtbarer Arbeitstag | Sollstundenrelevant | Verpflegungspauschale |
| --- | --- | --- | --- | --- |
| `FREI` | `OFF` ohne Reisezeit | Nein | Nein | Nein |
| `FREI_MIT_REISE` | `OFF` mit Reisezeit | Nein | Nein | Nein |
| `ARBEITSTAG_MIT_ARBEIT` | `WORK` mit positiver Arbeitszeit, optional zusätzlich Reisezeit | Ja | Ja | Ja |
| `ARBEITSTAG_NUR_REISE` | `WORK` ohne Arbeitszeit, aber mit Reisezeit | Ja | Ja | Ja |
| `ARBEITSTAG_LEER` | `WORK` ohne Arbeits- und Reisezeit | Ja | Ja | Nein |
| `UEBERSTUNDEN_ABBAU` | `COMP_TIME` | Nein | Ja | Nein |

Wichtig:
- Es gibt keine eigene Kategorie `ARBEITSTAG_REISE_UND_ARBEIT`. Ein `WORK`-Tag mit Arbeitszeit und Reisezeit bleibt `ARBEITSTAG_MIT_ARBEIT`.
- `UEBERSTUNDEN_ABBAU` zählt für Sollstunden/Overtime, wird aber nicht als sichtbarer Arbeitstag angezeigt.

## Entscheidungslogik

Für bestätigte Tage gilt vereinfacht:

```kotlin
when (dayType) {
    OFF -> if (travelMinutes > 0) FREI_MIT_REISE else FREI
    COMP_TIME -> UEBERSTUNDEN_ABBAU
    WORK -> when {
        workMinutes > 0 -> ARBEITSTAG_MIT_ARBEIT
        travelMinutes > 0 -> ARBEITSTAG_NUR_REISE
        else -> ARBEITSTAG_LEER
    }
}
```

`workMinutes` sind Netto-Arbeitsminuten nach Pausenabzug. Ein `WORK`-Tag mit `0` Netto-Minuten gilt daher nicht als Arbeitstag mit Aktivität.

## Verwendung in der Statistik

### Sichtbare Arbeitstage (`workDays`)

`workDays` meint in UI- und Export-Zusammenfassungen nur bestätigte `WORK`-Tage:

- `ARBEITSTAG_MIT_ARBEIT`
- `ARBEITSTAG_NUR_REISE`
- `ARBEITSTAG_LEER`

`COMP_TIME` gehört ausdrücklich nicht in diesen sichtbaren Zähler.

### Sollstundenrelevante Tage (`targetCountedDays`)

Für die Sollstunden- und Overtime-Logik werden zusätzlich `COMP_TIME`-Tage gezählt:

- alle sichtbaren Arbeitstage
- plus `UEBERSTUNDEN_ABBAU`

Damit bleibt der fachliche Unterschied zwischen „Arbeitstage“ und „Solltage“ erhalten.

### Stundenwerte

- `totalWorkMinutes`: Summe der Netto-Arbeitszeit bestätigter Tage
- `totalTravelMinutes`: Summe der Reisezeit bestätigter Tage
- `totalPaidMinutes`: `totalWorkMinutes + totalTravelMinutes`

Reisezeit an freien Tagen (`FREI_MIT_REISE`) zählt in bezahlte Zeit und Überstunden-Istzeit, aber nicht als Arbeitstag.

### Verpflegungspauschale

Verpflegungspauschale ist nur für bestätigte `WORK`-Tage mit tatsächlicher Aktivität zulässig:

- positive Arbeitszeit oder
- positive Reisezeit

Keinen Anspruch haben:

- `ARBEITSTAG_LEER`
- `FREI`
- `FREI_MIT_REISE`
- `UEBERSTUNDEN_ABBAU`
- `WORK`-Tage mit `0` Netto-Arbeitszeit und ohne Reisezeit

Statistik, Export-Preview, CSV und PDF verwenden dieselbe Eligibility-Regel. Gespeicherte Altwerte ohne Aktivität werden in Summen nicht mehr mitgezählt.

## Beispielszenarien

### Reiner Reisetag

```text
Tagtyp: WORK
Bestätigt: Ja
Arbeitszeit: 0h
Reisezeit: 3h

Klassifikation: ARBEITSTAG_NUR_REISE
Sichtbarer Arbeitstag: Ja
Sollstundenrelevant: Ja
Verpflegungspauschale: Ja
```

### Arbeitstag mit Arbeit und Reise

```text
Tagtyp: WORK
Bestätigt: Ja
Arbeitszeit: 8h
Reisezeit: 2h

Klassifikation: ARBEITSTAG_MIT_ARBEIT
Sichtbarer Arbeitstag: Ja
Sollstundenrelevant: Ja
Verpflegungspauschale: Ja
```

### Leerer WORK-Tag

```text
Tagtyp: WORK
Bestätigt: Ja
Arbeitszeit: 0h
Reisezeit: 0h

Klassifikation: ARBEITSTAG_LEER
Sichtbarer Arbeitstag: Ja
Sollstundenrelevant: Ja
Verpflegungspauschale: Nein
```

### Überstundenabbau

```text
Tagtyp: COMP_TIME
Bestätigt: Ja
Arbeitszeit: 0h
Reisezeit: 0h

Klassifikation: UEBERSTUNDEN_ABBAU
Sichtbarer Arbeitstag: Nein
Sollstundenrelevant: Ja
Verpflegungspauschale: Nein
```

## Hinweise für Implementierung und Tests

- Nur bestätigte Tage dürfen in Statistik- und Export-Summen auftauchen.
- `workDays` und `targetCountedDays` sind fachlich nicht austauschbar.
- Verpflegungspauschale muss immer aus derselben Eligibility-Regel abgeleitet werden; rohe gespeicherte Centwerte allein sind nicht ausreichend.
- Default-Arbeitszeiten müssen positive Netto-Arbeitszeit ergeben, sonst wird auf App-Defaults zurückgefallen.
