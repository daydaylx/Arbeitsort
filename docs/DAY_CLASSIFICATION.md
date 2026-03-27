# Tagesklassifikation (Day Classification)

## Überblick

Die Tagesklassifikation ist ein zentraler Bestandteil der Statistik-Berechnung in MontageZeit. Sie sorgt dafür, dass Tage korrekt in der Tageszählung, Stundenberechnung und Verpflegungspauschalen berücksichtigt werden.

## Problemstellung

### Ursprüngliches Problem

Ein Arbeitstag konnte Reisezeit enthalten, aber keine Arbeitszeit. Beispiel:
- Tagtyp: Arbeitstag (WORK)
- Arbeitszeiten: 0h / deaktiviert
- Reisezeit: 3h Anreise vorhanden
- Optional: Verpflegungspauschale vorhanden

Ursprünglich wurde dieser Tag nicht korrekt in der Statistik erkannt oder inkonsistent berücksichtigt.

## Lösung: Tagesklassifikation

### Kategorien

Die Tagesklassifikation unterscheidet folgende Kategorien:

| Klassifikation | Beschreibung | Zählt als Arbeitstag? | Berücksichtigt für Verpflegungspauschale? |
|---------------|--------------|----------------------|-----------------------------------------|
| `FREI` | OFF oder COMP_TIME Tage | Nein | Nein |
| `UEBERSTUNDEN_ABBAU` | COMP_TIME Tage | Nein | Nein |
| `ARBEITSTAG_MIT_ARBEIT` | WORK-Tag mit Arbeitszeit (>0h) | Ja | Ja |
| `ARBEITSTAG_NUR_REISE` | WORK-Tag ohne Arbeitszeit, aber mit Reisezeit (>0h) | Ja | Ja |
| `ARBEITSTAG_REISE_UND_ARBEIT` | WORK-Tag mit Arbeitszeit (>0h) UND Reisezeit (>0h) | Ja | Ja |
| `ARBEITSTAG_LEER` | WORK-Tag ohne Arbeitszeit und ohne Reisezeit | Ja | Nein |

### Entscheidungslogik

Die Klassifikation erfolgt basierend auf folgenden Kriterien:

1. **Nicht-WORK-Tage**: OFF oder COMP_TIME → `FREI` oder `UEBERSTUNDEN_ABBAU`
2. **Unbestätigte Tage**: Nicht bestätigt → nicht berücksichtigt (keine Klassifikation)
3. **WORK-Tage**: Bestätigt → weiter in detaillierte Klassifikation

Für WORK-Tage gilt:

```kotlin
val hasWorkTime = workHours > 0
val hasTravelTime = travelMinutes > 0

when {
    hasWorkTime && hasTravelTime -> ARBEITSTAG_REISE_UND_ARBEIT
    hasWorkTime && !hasTravelTime -> ARBEITSTAG_MIT_ARBEIT
    !hasWorkTime && hasTravelTime -> ARBEITSTAG_NUR_REISE
    !hasWorkTime && !hasTravelTime -> ARBEITSTAG_LEER
}
```

### Implementation

Die Implementierung erfolgt durch zwei Hauptkomponenten:

1. **`DayClassification` Enum**: Definiert die Kategorien
2. **`ClassifyDay` Use Case**: Kategorisiert einen einzelnen Tag basierend auf seinen Daten

## Verwendung in der Statistik

### AggregateWorkStats

Der `AggregateWorkStats` Use Case verwendet die Tagesklassifikation für folgende Berechnungen:

#### Gezählte Tage (`workDays`)

Alle bestätigten Arbeitstage außer `FREI` und `UEBERSTUNDEN_ABBAU`:

```kotlin
val workDays = entries
    .filter { it.workEntry.confirmedWorkDay }
    .map { classify(it) }
    .count { it != FREI && it != UEBERSTUNDEN_ABBAU }
```

Dazu gehören:
- `ARBEITSTAG_MIT_ARBEIT`
- `ARBEITSTAG_NUR_REISE` ← **NEU: Reisetage werden jetzt korrekt gezählt!**
- `ARBEITSTAG_REISE_UND_ARBEIT`
- `ARBEITSTAG_LEER`

#### Ist-Stunden (`totalWorkMinutes`)

Summe aller Arbeitsstunden aller klassifizierten Tage:

```kotlin
val totalWorkMinutes = entries
    .filter { it.workEntry.confirmedWorkDay }
    .sumOf { calculateWorkMinutes(it) }
```

#### Fahrstunden (`totalTravelMinutes`)

Summe aller Reisezeit aller klassifizierten Tage:

```kotlin
val totalTravelMinutes = entries
    .filter { it.workEntry.confirmedWorkDay }
    .sumOf { calculateTravelMinutes(it) }
```

#### Verpflegungspauschale (`mealAllowanceCents`)

Nur für Tage mit Aktivität:

```kotlin
val mealAllowanceCents = entries
    .filter { it.workEntry.confirmedWorkDay }
    .map { classify(it) }
    .filter { it == ARBEITSTAG_MIT_ARBEIT || it == ARBEITSTAG_NUR_REISE || it == ARBEITSTAG_REISE_UND_ARBEIT }
    .sumOf { it.workEntry.mealAllowanceAmountCents }
}
```

**Wichtig**: `ARBEITSTAG_LEER` (ohne Aktivität) bekommt keine Verpflegungspauschale!

### Sollstunden

Die Sollstunden werden basierend auf der Anzahl der gezählten Arbeitstage berechnet:

```kotlin
val targetMinutes = workDays * dailyTargetHours * 60
```

## Beispielszenarien

### Szenario 1: Nur Reise (Anreisetag)

```
Tagtyp: WORK
Bestätigt: Ja
Arbeitszeit: 0h
Reisezeit: 3h (Anreise)
Verpflegungspauschale: €28,00

Klassifikation: ARBEITSTAG_NUR_REISE
Gezählter Tag: ✓
Ist-Stunden: 0h
Fahrstunden: 3h
Verpflegungspauschale: €28,00 ✓
```

### Szenario 2: Nur Arbeit

```
Tagtyp: WORK
Bestätigt: Ja
Arbeitszeit: 8h
Reisezeit: 0h
Verpflegungspauschale: €28,00

Klassifikation: ARBEITSTAG_MIT_ARBEIT
Gezählter Tag: ✓
Ist-Stunden: 8h
Fahrstunden: 0h
Verpflegungspauschale: €28,00 ✓
```

### Szenario 3: Reise und Arbeit

```
Tagtyp: WORK
Bestätigt: Ja
Arbeitszeit: 8h
Reisezeit: 3h (Anreise)
Verpflegungspauschale: €28,00

Klassifikation: ARBEITSTAG_REISE_UND_ARBEIT
Gezählter Tag: ✓
Ist-Stunden: 8h
Fahrstunden: 3h
Verpflegungspauschale: €28,00 ✓
```

### Szenario 4: Frei

```
Tagtyp: OFF
Bestätigt: Ja
Arbeitszeit: 0h
Reisezeit: 0h
Verpflegungspauschale: €0,00

Klassifikation: FREI
Gezählter Tag: ✗
Ist-Stunden: 0h
Fahrstunden: 0h
Verpflegungspauschale: €0,00 ✗
```

### Szenario 5: Urlaub/Überstundenabbau

```
Tagtyp: COMP_TIME
Bestätigt: Ja
Arbeitszeit: 0h
Reisezeit: 0h
Verpflegungspauschale: €0,00

Klassifikation: UEBERSTUNDEN_ABBAU
Gezählter Tag: ✗
Ist-Stunden: 0h
Fahrstunden: 0h
Verpflegungspauschale: €0,00 ✗
```

### Szenario 6: Leerer Arbeitstag (kranker Tag)

```
Tagtyp: WORK
Bestätigt: Ja
Arbeitszeit: 0h
Reisezeit: 0h
Verpflegungspauschale: €0,00

Klassifikation: ARBEITSTAG_LEER
Gezählter Tag: ✓ (aber ohne Aktivität)
Ist-Stunden: 0h
Fahrstunden: 0h
Verpflegungspauschale: €0,00 ✗
```

## Testabdeckung

Die Unit Tests in `ClassifyDayTest.kt` decken alle Klassifikationsfälle ab:

- ✓ WORK mit Arbeit und Reise
- ✓ WORK mit nur Arbeit
- ✓ WORK mit nur Reise
- ✓ WORK leer
- ✓ OFF-Tage
- ✓ COMP_TIME-Tage
- ✓ Unbestätigte WORK-Tage

## Wichtige Hinweise

1. **Nur bestätigte Tage**: Unbestätigte Tage werden in der Statistik ignoriert
2. **Reisetage werden gezählt**: `ARBEITSTAG_NUR_REISE` zählt als Arbeitstag und bekommt Verpflegungspauschale
3. **Leere Arbeitstage**: `ARBEITSTAG_LEER` wird als Tag gezählt, bekommt aber keine Verpflegungspauschale
4. **Konsistenz**: Die gleiche Klassifikationslogik wird in allen Statistikberechnungen verwendet

## Migration von alter Logik

Die alte Logik hat nur WORK-Tage gezählt und alle in die Verpflegungspauschale einbezogen. Die neue Logik unterscheidet detaillierter und berücksichtigt Reisetage explizit.

**Vorher:**
- Gezählte Tage: Nur bestätigte WORK-Tage
- Verpflegungspauschale: Alle bestätigten WORK-Tage

**Jetzt:**
- Gezählte Tage: Alle bestätigten Tage außer FREI und UEBERSTUNDEN_ABBAU
- Verpflegungspauschale: Nur Tage mit Aktivität (Arbeit oder Reise)