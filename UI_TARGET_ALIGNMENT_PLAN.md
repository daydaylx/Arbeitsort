# UI Target Alignment Plan – NACHTZUG 19

**Plan Date**: 2026-01-31
**Plan Version**: 1.0
**Status**: Draft

---

## Executive Summary

Dieser Plan definiert die **konkreten Schritte** zur Ausrichtung der aktuellen UI-Implementierung mit dem Zielbild aus den Docs. Der Fokus liegt auf:
1. **Kritische P0-Blocker lösen** (Phase 3 Kompilierungsfehler)
2. **Beat-Chunking implementieren** (Mobile Pacing Regel "Spiel statt Roman")
3. **Status Visualization vervollständigen** (Tooltips, Warning Colors, Glow)
4. **Accessibility verbessern** (Icon Buttons, Screen Reader)
5. **Performance optimieren** (memo, remember, State collocation)

**Architekturprinzip**: UI hat keine Storylogik, Domain entscheidet, UI rendert.

---

## Leitprinzipien (aus /docs)

### Reader Noir Ästhetik
- Lesen fühlt sich wie E-Reader, nicht wie Chat
- Entscheidungen sind große, klare Tickets, nicht kleine Buttons
- Subtile Drift-Effekte, keine aggressiven Glitches im Text
- "Offiziell vs. falsch": Alles sieht ordentlich aus, aber Details kippen subtil

### Mobile-First: "Spiel statt Roman"
- **Interaktion schlägt Text** – Der Spieler ist kein Leser, er ist ein Teilnehmer
- **Beat-Regel**: Eine Szene = ein Beat = 1 Gefühl + 1 konkrete Aktion
- **Max 6–10 Sätze pro Beat**, dann MUSS eine Aktion kommen
- **Kein Scrollen** (außer bei Überlauf): Der Beat muss auf einen Bildschirm passen

### Canon Rules (R1-R4)
- **R1**: Drift After Stations – Jedes Kapitel-Ende erhöht `memory_drift` automatisch
- **R2**: Controls at Chapters 2, 3, 5 – Feste Kontrollpunkte (Schaffner)
- **R3**: Every Choice Has Callback – Keine Choice ohne sichtbare Konsequenz später
- **R4**: Train Never Lies Directly – Der Zug lügt nicht plump, sondern verschiebt Bedeutung

### Accessibility & Performance
- Icon Buttons haben `contentDescription`
- Reduce Motion & Immersion FX sind abschaltbar
- Performance-Optimierung (memo, remember, State collocation)
- Screen Reader-Unterstützung (semantic roles, Labels)

---

## Architekturregeln

### Separation of Concerns

| Layer | Verantwortung | Nicht-Erlaubt |
|--------|----------------|---------------|
| **UI Layer** | Rendering, User Interaction, State Management | Storylogik, Conditions, Effects |
| **Domain Layer** | GameEngine (Logik), Models (Datenstrukturen) | UI-Rendering, Animationen |
| **Content Layer** | Story-Definition (Szenen, Choices) | UI-Code, Logik-Code |

**Regel**: UI hat keine Storylogik. UI rendert nur, was Domain/Engine entscheidet.

### State Management

- **Single Source of Truth**: `GameState` aus GameEngine.kt
- **UI State**: `UiState` in GameViewModel.kt (Wrapper für GameEngine State + UI-spezifische Flags)
- **State Updates**: Immer via GameViewModel → GameEngine.kt → UiState → UI Re-Compose
- **Keine UI-State-Mutation direkt**: UI liest nur aus UiState, ändert niemals direkt

---

## Konkrete Umbauschritte

### Step 1: Phase 3 Fix – Choice Tray & Ticket Interaction

**Ziel**: Kompilierungsfehler in `TicketChoiceEnhanced.kt` beheben und Ticket-Choice-System funktionsfähig machen.

**Betroffene Dateien/Module**:
- `android-native/app/src/main/java/de/daydaylx/nachtzug19/ui/components/TicketChoiceEnhanced.kt`
- `android-native/app/src/main/java/de/daydaylx/nachtzug19/ui/components/ChoiceFeedback.kt`
- `android-native/app/src/main/java/de/daydaylx/nachtzug19/ui/PlayerScreen.kt` (Integration)

**Aufgaben**:
1. `TicketChoiceEnhanced.kt` Kompilierungsfehler beheben (Import-Konflikte: `background`, `scale`)
2. `ChoiceFeedback.kt` kompilieren und testen
3. TicketChoiceEnhanced in PlayerScreen integrieren (ersetzt TicketChoice)
4. Tone-Choice-Indikator (★) implementieren
5. Punch-Animation (scale 0.98f) testen
6. Border-Color-Animation (pressed → StationNeon, punched → ControlOrange) testen

**Acceptance Criteria**:
- [ ] TicketChoiceEnhanced kompiliert ohne Fehler
- [ ] ChoiceFeedback kompiliert ohne Fehler
- [ ] PlayerScreen kompiliert ohne Fehler
- [ ] Punch-Animation funktioniert (150ms timing)
- [ ] Border-Color-Change funktioniert
- [ ] Tone-Indikator (★) wird angezeigt
- [ ] `npm run build` erfolgreich

**Risiken**:
- Import-Konflikte können komplex sein → Lösung in Android Studio (IDE-Support nutzen)
- Performance-Impact durch komplexe Animationen → Benchmark auf Zielgeräten

**How to verify**:
- Build APK: `cd android-native && ./gradlew assembleDebug`
- Install auf Emulator/Gerät
- Teste Choice-Interaktion (Press, Punch, Border-Color)

---

### Step 2: Beat-Chunking Implementierung

**Ziel**: Narrative-Text in Beats chunken (Beat Cards, Continue Button nach jedem Beat) – Mobile Pacing Regel "Spiel statt Roman".

**Betroffene Dateien/Module**:
- `android-native/app/src/main/java/de/daydaylx/nachtzug19/ui/components/ReaderCard.kt`
- `android-native/app/src/main/java/de/daydaylx/nachtzug19/ui/components/BeatCard.kt` (Neu)
- `src/domain/types/index.ts` (Content-Format erweitern für Beat-Markup)

**Aufgaben**:
1. Content-Format erweitern: Beat-Markup in `narrative` Feld (z.B. `||` oder `//` als Trenner für Beats)
2. `BeatCard` Komponente erstellen (Reader-Card für einzelnen Beat)
3. `BeatContainer` Komponente erstellen (Stack von BeatCards)
4. `ContinueButton` Komponente erstellen (nach jedem Beat, außer letztem)
5. Auto-Höhe für BeatContainer (nicht scrollen, außer bei Überlauf)
6. Typewriter-Animation für jeden Beat (nicht für gesamten Text)

**Acceptance Criteria**:
- [ ] Beat-Markup in Content-Format dokumentiert
- [ ] BeatCard Komponente erstellt
- [ ] BeatContainer Komponente erstellt
- [ ] ContinueButton Komponente erstellt
- [ ] Narrative-Text wird in Beats aufgeteilt (nicht als Block)
- [ ] Continue-Button erscheint nach jedem Beat (außer letztem)
- [ ] Max 6–10 Sätze pro Beat werden erzwungen (Content-Level)
- [ ] Auto-Höhe für BeatContainer (kein unnötiges Scroll)
- [ ] `npm test` erfolgreich

**Risiken**:
- Content-Format-Änderung kann Breaking Changes für bestehende Szenen sein → Migrationsskript schreiben
- Performance-Impact durch viele BeatCards → memo implementieren

**How to verify**:
- `npm run export:story` (Story mit Beat-Markup)
- Build APK
- Teste Beat-Chunking auf Emulator (Beats erscheinen einzeln mit Continue-Button)

---

### Step 3: Phase 4 – Status Visualization vervollständigen

**Ziel**: Status-Visualisierung mit Tooltips, Warning Colors, Glow für Tickets und Items.

**Betroffene Dateien/Module**:
- `android-native/app/src/main/java/de/daydaylx/nachtzug19/ui/components/StatusVisuals.kt`
- `android-native/app/src/main/java/de/daydaylx/nachtzug19/ui/components/StatusSheet.kt`
- `android-native/app/src/main/java/de/daydaylx/nachtzug19/ui/theme/ColorPalette.kt` (Warning Colors)

**Aufgaben**:
1. `TICKET_TOOLTIPS` Map erstellen (Beschreibungen für Tickets)
2. `TicketStamp` mit Tooltip-Popup erweitern
3. `PressureBar` mit Warning Colors erweitern:
   - Attention: Orange bei >=3, Red bei >=5
   - Drift: Teal bei >=3, Purple bei >=5
   - Wobble-Animation für warning segments
4. `ItemIcon` mit Glow-Effekt erweitern:
   - Glow wenn `hasItem == true`
   - Dimmed wenn `hasItem == false`
5. `Relationships` Visualisierung erstellen (negative/neutral/positive tint, dots -2 bis +4)
6. `StatusSheet` mit erweiterten Komponenten aktualisieren

**Acceptance Criteria**:
- [ ] TICKET_TOOLTIPS Map erstellt
- [ ] TicketStamp zeigt Tooltip auf Tap
- [ ] Attention-Bar zeigt Orange bei >=3, Red bei >=5
- [ ] Drift-Bar zeigt Teal bei >=3, Purple bei >=5
- [ ] Warning segments haben Wobble-Animation
- [ ] ItemIcon hat Glow-Effekt bei `hasItem == true`
- [ ] ItemIcon ist dimmed bei `hasItem == false`
- [ ] Relationships werden visualisiert (tint, dots)
- [ ] StatusSheet zeigt alle erweiterten Komponenten
- [ ] `npm run build` erfolgreich

**Risiken**:
- Tooltip-UX muss unobtrusiv sein → nicht ablenken
- Glow-Effekt kann Performance-Impact haben → benchmarken
- Relationships-Visualisierung kann unübersichtlich werden → Clean Design

**How to verify**:
- Build APK
- Teste Ticket-Stempels mit Tap (Tooltip erscheint)
- Teste Attention/Drift-Bars mit verschiedenen Werten (Warning Colors, Wobble)
- Teste Item-Icons mit `hasItem == true/false` (Glow, Dimmed)
- Teste Relationships-Visualisierung (tint, dots)

---

### Step 4: Micro-Actions Implementierung

**Ziel**: Micro-Action-Komponente (Hotspots, Mini-Overlays) für kurze Interaktionen statt langer Szenenübergänge.

**Betroffene Dateien/Module**:
- `android-native/app/src/main/java/de/daydaylx/nachtzug19/ui/components/MicroAction.kt` (Neu)
- `src/domain/types/index.ts` (Content-Format erweitern für Micro-Action-Tags)
- `android-native/app/src/main/java/de/daydaylx/nachtzug19/ui/PlayerScreen.kt` (Integration)

**Aufgaben**:
1. Content-Format erweitern: Micro-Action-Tags (z.B. `microaction_horchen`, `microaction_ticket_pruefen`)
2. `MicroAction` Komponente erstellen (Tap-Hotspot mit kurzer Text-Reaktion)
3. Micro-Action-Integration in PlayerScreen (wenn Tag `microaction_*` vorhanden)
4. Mini-Szene-Flow: Micro-Action → kurze Text-Reaktion → neue Choice

**Acceptance Criteria**:
- [ ] Micro-Action-Tags dokumentiert
- [ ] MicroAction Komponente erstellt
- [ ] Micro-Action wird gerendert wenn Tag `microaction_*` vorhanden
- [ ] Tap auf Micro-Action zeigt kurze Text-Reaktion
- [ ] Nach Micro-Action erscheinen Choices
- [ ] `npm test` erfolgreich

**Risiken**:
- Content-Format-Änderung kann Breaking Changes sein → Migrationsskript schreiben
- Micro-Action-UX muss intuitiv sein → Clear Visual Feedback

**How to verify**:
- `npm run export:story` (Story mit Micro-Action-Tags)
- Build APK
- Teste Micro-Action auf Emulator (Tap → Text-Reaktion → Choices)

---

### Step 5: Tone Choices Implementierung

**Ziel**: Tone-Choice-Indikator (★) für reine Stimmungs-Entscheidungen (keine Effekte, keine Verzweigung).

**Betroffene Dateien/Module**:
- `android-native/app/src/main/java/de/daydaylx/nachtzug19/ui/components/TicketChoiceEnhanced.kt`
- `android-native/app/src/main/java/de/daydaylx/nachtzug19/ui/components/ChoiceTray.kt` (Logic für Tone-Detection)

**Aufgaben**:
1. Tone-Detection-Logik implementieren (isWeighted, isTone)
2. TicketChoiceEnhanced mit Tone-Indikator (★) erweitern
3. Tone-Choices visuell unterscheiden (farblich subtil, oder Icon)
4. Content-Migration: Einige Choices zu Tone machen (Effects entfernen)

**Acceptance Criteria**:
- [ ] Tone-Detection-Logik implementiert
- [ ] Tone-Choices zeigen ★-Indikator
- [ ] Tone-Choices sind visuell unterscheidbar
- [ ] Mindestens 1 Tone-Choice pro Szene (wo sinnvoll)
- [ ] `npm run build` erfolgreich

**Risiken**:
- Content-Migration kann zeitaufwendig sein → Priorisiere wichtige Szenen
- Spieler könnten Tone-Choices mit gewichteten verwechseln → Clear Visual Distinction

**How to verify**:
- Build APK
- Teste Tone-Choices (★-Indikator erscheint)
- Teste Visuelle Unterscheidung (gewichtet vs. Tone)

---

### Step 6: Accessibility Verbesserung

**Ziel**: Icon Buttons mit `contentDescription`, Screen Reader-Unterstützung (semantic roles, Labels).

**Betroffene Dateien/Module**:
- `android-native/app/src/main/java/de/daydaylx/nachtzug19/ui/PlayerScreen.kt`
- `android-native/app/src/main/java/de/daydaylx/nachtzug19/ui/components/*` (Alle Icon Buttons)
- `android-native/app/src/main/java/de/daydaylx/nachtzug19/ui/theme/Theme.kt` (Semantic roles)

**Aufgaben**:
1. Alle Icon Buttons mit `contentDescription` versehen
2. Screen Reader Labels für wichtige UI-Elemente (ReaderCard, Choice Tray, StatusSheet)
3. Semantic roles implementieren (z.B. `role = Role.Button`, `role = Role.List`)
4. Focus states für Keyboard-Navigation (falls relevant)
5. TalkBack-Test auf Android-Gerät

**Acceptance Criteria**:
- [ ] Alle Icon Buttons haben `contentDescription`
- [ ] Screen Reader Labels existieren für UI-Elemente
- [ ] Semantic roles implementiert
- [ ] Focus states sichtbar
- [ ] TalkBack-Test erfolgreich (Screen Reader liest UI korrekt)

**Risiken**:
- `contentDescription` Overload → Keep descriptions concise
- Semantic roles können komplex sein → Einfache Implementierung

**How to verify**:
- Build APK
- Install auf Android-Gerät mit TalkBack aktiviert
- Teste Screen Reader-Navigation (alle UI-Elemente werden korrekt vorgelesen)

---

### Step 7: Performance Optimierung

**Ziel**: Unnötige Re-renders eliminieren, memo, remember, State collocation implementieren.

**Betroffene Dateien/Module**:
- `android-native/app/src/main/java/de/daydaylx/nachtzug19/ui/PlayerScreen.kt`
- `android-native/app/src/main/java/de/daydaylx/nachtzug19/ui/components/*` (Alle Komponenten)
- `android-native/app/src/main/java/de/daydaylx/nachtzug19/ui/GameViewModel.kt` (State collocation)

**Aufgaben**:
1. `remember` für stable Werte implementieren (z.B. settings, driftLevel)
2. `memo` für komplexe Komponenten implementieren (z.B. BeatCard, TicketChoice)
3. State collocation in GameViewModel (State lokal zu Komponente, nicht globale Re-renders)
4. Performance-Benchmark auf Zielgeräten (Profile rendering time)
5. Unnötige Berechnungen eliminieren (z.B. Drift-Tinting nur wenn Drift sich ändert)

**Acceptance Criteria**:
- [ ] `remember` für stable Werte implementiert
- [ ] `memo` für komplexe Komponenten implementiert
- [ ] State collocation implementiert
- [ ] Rendering time < 16ms (60fps) auf Zielgeräten
- [ ] Keine unnötigen Re-renders (Composable-Inspector)

**Risiken**:
- `memo` Overuse → kann Performance verschlechtern, wenn Dependencies zu groß
- State collocation kann komplex werden → Balance zwischen Lokalität und Lesbarkeit

**How to verify**:
- Build APK
- Profile rendering time auf Zielgeräten (Android Studio Profiler)
- Teste Re-renders (Composable-Inspector zeigt keine unnötigen Updates)

---

## Risiken & Abhängigkeiten

### Risiken

| Risiko | Auswirkung | Mitigation |
|--------|--------------|------------|
| Import-Konflikte in TicketChoiceEnhanced.kt | Phase 3 nicht funktional | Lösung in Android Studio (IDE-Support) |
| Content-Format-Änderungen (Beat-Markup, Micro-Actions) | Breaking Changes für bestehende Szenen | Migrationsskript schreiben, schrittweise Migration |
| Performance-Impact durch komplexe Animationen | Lag auf Zielgeräten | Benchmark, Optimierung, reduceMotion Toggle |
| Accessibility-Implementierung kann zeitaufwendig sein | Verzögerung des MVP | Priorisiere kritische UI-Elemente |

### Abhängigkeiten

| Step | Abhängig von | Grund |
|-------|----------------|-------|
| Step 2 (Beat-Chunking) | Step 1 (Phase 3 Fix) | Beat-Chunking braucht funktionierende TicketChoices |
| Step 3 (Status Visualization) | Step 1 (Phase 3 Fix) | Status Visualization braucht TicketChoices mit Tone-Indikator |
| Step 5 (Tone Choices) | Step 1 (Phase 3 Fix) | Tone Choices brauchen TicketChoiceEnhanced |
| Step 6 (Accessibility) | Alle vorherigen Schritte | Accessibility-Verbesserung ist finale Polish |
| Step 7 (Performance) | Alle vorherigen Schritte | Performance-Optimierung nach Abschluss der Features |

---

## Empfohlene Reihenfolge

1. **Step 1**: Phase 3 Fix (P0-Blocker lösen)
2. **Step 2**: Beat-Chunking Implementierung (P0 für Mobile Pacing)
3. **Step 3**: Phase 4 – Status Visualization vervollständigen (P1)
4. **Step 4**: Micro-Actions Implementierung (P1)
5. **Step 5**: Tone Choices Implementierung (P1)
6. **Step 6**: Accessibility Verbesserung (P2)
7. **Step 7**: Performance Optimierung (P2)

---

## Next Actions

### Immediately

1. **TicketChoiceEnhanced.kt** in Android Studio öffnen und Kompilierungsfehler behehen
2. `ChoiceFeedback.kt` kompilieren und testen
3. TicketChoiceEnhanced in PlayerScreen integrieren

### Short-term

1. Beat-Chunking Design (Content-Format für Beat-Markup)
2. `BeatCard` Komponente erstellen
3. Micro-Action Design (Tags, Komponente)

### Medium-term

1. Status Visualization erweitern (Tooltips, Warning Colors, Glow)
2. Tone Choices implementieren
3. Accessibility Verbesserung
4. Performance Optimierung

---

**Plan Version**: 1.0
**Last Updated**: 2026-01-31
**Next Review**: Nach Abschluss von Step 1 (Phase 3 Fix)

---

**End of Plan**
