# Umsetzungsplan (Implementation Plan)

Der Wechsel auf das Dark Control Dashboard erfordert einen radikalen visuellen Bruch, sollte aber die bestehende funktionale Logik nicht gefährden. Daher erfolgt die Umsetzung in klaren Phasen.

---

## Phase 1: Fundament (Tokens & Typografie)
Ziel: Das "Gefühl" der App auf "Dark Tech" trimmen, ohne Komponentenstrukturen zu zerstören.

1.  **Fonts integrieren:** Einen Monospace-Font (z.B. JetBrains Mono oder Roboto Mono) für Zahlen/Metriken hinzufügen und in `Type.kt` einbinden. Die restliche Font auf eine saubere, moderne Grotesk (z.B. Inter) umstellen.
2.  **Farben austauschen:** `Color.kt` komplett leeren und durch die Neon/DeepDark Palette ersetzen.
3.  **Theme anpassen:** `Theme.kt` umschreiben. Den LightMode entfernen oder "verstecken" (Dark Mode First). MaterialColors zwingen, die neuen dunklen Tokens zu nutzen, damit bestehende Komponenten nicht crashen, sondern sofort dunkel/neon werden.
4.  **Spacing-Tokens einführen:** Ein Objekt `MZSpacing` (z.B. in `MontageZeitComponents.kt` oder eigener Datei) anlegen für konsistente Abstände (4dp, 8dp, 16dp, 24dp).

*Risiko:* Hartcodierte Farben in den Screens (falls vorhanden) fallen auf.
*Aufwand:* Klein.

---

## Phase 2: Basis-Komponenten (Glass & Neon)
Ziel: Die zentralen Wrapper-Komponenten aus `MontageZeitComponents.kt` auf den neuen Look umschreiben. Ab hier sieht die App komplett neu aus.

1.  **Background-Upgrade:** `MZPageBackground` von einem linearen Gradienten zu einem "Mesh" (oder komplexeren dunklen Gradienten mit farbigen Glows in den Ecken) umbauen.
2.  **Card-Refactoring (`MZCard`, `MZHeroCard`):**
    *   Hintergrund auf `White` mit Alpha (z.B. `0.03f`) setzen.
    *   Border auf `1dp` Glossy (Gradient von hellem Weiß zu fast unsichtbar) setzen.
    *   Elevation auf `0.dp` setzen (keine falschen Material-Schatten).
3.  **Button-Refactoring:**
    *   `PrimaryActionButton`: Hintergrund Neon, Text Schwarz, Form auf Pille (`CircleShape`) oder Token-Radius anpassen.
    *   `SecondaryActionButton`: Background transluzent, Rand Glossy, Text Weiß.
4.  **Badges & Status:** `StatusType` Farben auf die neuen Neon-Signal-Farben (Cyber-Green, Alert-Red) mappen.

*Risiko:* Kontrastprobleme bei falscher Alpha-Gewichtung. Accessibility muss via Contrast-Checker geprüft werden.
*Aufwand:* Mittel.

---

## Phase 3: Layouts & Navigation (Die Hülle)
Ziel: Der äußere Rahmen der App soll schweben und premium wirken.

1.  **Floating Nav Bar (`MontageZeitNavGraph.kt`):**
    *   Den Hintergrund-Container (`Surface`) extrem verdunkeln oder blurren.
    *   Die aktiven Icons (Indicator) leuchten lassen (Neon-Underline oder Glow statt dicker Material-Pille).
2.  **TopAppBar (Header):**
    *   In allen Screens sicherstellen, dass die TopAppBar transparent ist und der Inhalt (Background Mesh) nahtlos darunter durchfließt.

*Risiko:* Blur-Modifier auf dem Navigation-Background kann auf älteren Android-Versionen laggen. Fallback auf Alpha-Opacity vorsehen.
*Aufwand:* Mittel.

---

## Phase 4: Screen-Architektur & UX-Brüche reparieren
Ziel: Altlasten (wie Dialoge) durch premium UI-Pattern (Bottom Sheets) ersetzen.

1.  **TodayScreen:**
    *   `DailyManualCheckInDialog` und `DayLocationDialog` ausbauen.
    *   Durch `ModalBottomSheet` mit Glassmorphism-Styling ersetzen.
    *   Die dicken, farbigen Statuskarten durch schlanke Glass-Panels mit Neon-Rändern ersetzen.
2.  **OverviewScreen:**
    *   Hero-Stats Typografie auf den neuen Monospace-Display-Font ziehen.
    *   KPI-Grid von plumpen M3-Cards auf feine Glass-Tiles umstellen.
3.  **HistoryScreen:**
    *   Status-Punkte im Kalender auf helle, kleine Neon-Dots umstellen (wirkt technischer).
    *   Listen-Elemente durch schlanke Rows mit dünnem Status-Border ersetzen.
4.  **EditEntrySheet:**
    *   Dieses Bottom Sheet ist bereits vorhanden, muss aber die "Sticky Save Bar" auf Glass-Look trimmen (Hintergrund-Unschärfe).

*Risiko:* Der Umbau von Dialogen zu BottomSheets im `TodayScreen` erfordert State-Management-Anpassungen im ViewModel/UI.
*Aufwand:* Groß.

---

## Phase 5: Polish & Mikrodynamik (Optional/Letzte 10%)
Ziel: Das "Control Dashboard" Gefühl durch Animationen und Details perfektionieren.

1.  Haptisches Feedback (Haptic Engine) bei jedem Neon-Button-Klick und Switch einbauen (ist teils schon in `SettingsScreen`, muss systemweit angewendet werden).
2.  Glow-Effekte animieren (z.B. pulsiert der Neon-Ring bei einem offenen Tag ganz subtil).
3.  Zahlen in der Overview-Ansicht hochzählen lassen (Count-Up Animation beim Laden der Überstunden).

*Aufwand:* Groß (da viel Fine-Tuning).
