# UI-Audit: Aktueller Zustand vs. Premium Dark Control Dashboard

## 1. Aktueller UI-Zustand (Status Quo)
Die App basiert aktuell auf einem soliden, aber sehr standardisierten **Material Design 3** Ansatz. Es gibt eine gut gepflegte `MontageZeitComponents.kt`-Datei, die zentrale Bausteine bündelt, sowie eine grundlegende Token-Struktur in `Theme.kt`, `Color.kt` und `Type.kt`. Der Look wirkt stark nach "Android Standard" und weniger nach einem hochwertigen, technischen Spezialtool.

Der Dark Mode ist zwar vorhanden (`isSystemInDarkTheme()`), wirkt aber aufgesetzt und nicht wie das primäre, touch-optimierte Premium-Interface, das als "Dark Control Dashboard" gewünscht wird.

## 2. Stärken (Was gut funktioniert und bleiben sollte)
*   **Accessibility & Touch-Targets:** In `MontageZeitComponents.kt` ist ein `AccessibilityDefaults`-Objekt definiert, das explizit Größen wie `MinTouchTargetSize = 48`, `ButtonHeight = 54.dp` und `CardCornerRadius = 24.dp` vorgibt. Das ist hervorragend für ein mobiles Dashboard.
*   **Zentrale Komponenten:** Es gibt bereits Wrapper wie `MZCard`, `PrimaryActionButton` und `SecondaryActionButton`. Das erleichtert ein Refactoring extrem.
*   **Floating Navigation:** Die Bottom Navigation (`MontageZeitNavGraph.kt`) ist als Floating-Pille mit abgerundeten Ecken (`RoundedCornerShape(28.dp)`) umgesetzt, was modern wirkt und sich gut ins Zielbild einfügt.
*   **Strukturierte Layouts:** Die Screens (`TodayScreen`, `OverviewScreen`, etc.) nutzen `MZPageBackground`, was eine zentrale Steuerung des Hintergrunds ermöglicht.

## 3. Schwächen & UX-Probleme (Was uns vom Premium-Look abhält)
*   **Gradients & Backgrounds:** `MZPageBackground` nutzt einen sehr simplen `Brush.verticalGradient`, was eher wie ein billiger Farbverlauf aussieht als nach Glassmorphism oder Ambient Glow.
*   **Material 3 "Plastik-Look":** Die Farben (`Color.kt`) nutzen typische Material 3 Töne (z.B. Teal, Amber, Blue). Sie wirken bunt und unruhig, anstatt einer tiefen, dunklen, technischen Palette (wie Vanta Black, Dark Neon, Glass).
*   **Schattierungen (Elevation):** Die `MZCard` nutzt Standard `CardDefaults.cardElevation(2.dp)` und einen simplen Outline-Border. Das wirkt flach. Für ein "Dark Control Dashboard" brauchen wir tiefe Schatten und leuchtende Glow-Borders.
*   **AlertDialogs statt Modal/BottomSheets:** Viele Bestätigungen (z.B. `DailyManualCheckInDialog`, `DayLocationDialog` in `TodayScreen.kt`) nutzen standard `AlertDialog`. Auf mobilen Geräten wirken Bottom Sheets flüssiger, einhändig bedienbarer und hochwertiger.
*   **Fehlende Blur-Effekte (Glassmorphism):** Es gibt aktuell keinen Einsatz von Blur (`Modifier.blur`) oder halbtransparenten Layern über Glow-Hintergründen. Alles ist opakes Material 3.
*   **Zu viele Card-Varianten:** `MZCard`, `MZStatusCard`, `MZHeroCard`. Diese unterscheiden sich stark in der Umsetzung (teilweise `Color.Transparent`, teilweise `SurfaceVariant`). Die visuelle Hierarchie bricht oft auf.

## 4. Inkonsistenzen & Konkrete technische Hürden
*   **Typography:** In `Type.kt` wird lediglich `FontFamily.SansSerif` genutzt. Das wirkt lieblos und nicht nach "technischem Dashboard".
*   **Spacings & Padding:** Viele Paddings sind fest verdrahtet in den Komponenten (z.B. `padding(32.dp)` in `MZEmptyState`, `padding(horizontal = 16.dp, vertical = 8.dp)` in den Screens). Es gibt kein echtes Design-Token-System für Spacings in `MontageZeitTheme`.
*   **Modale vs NavGraph:** Der `EditEntrySheet` wird manuell im `MontageZeitNavGraph.kt` via `if (showEditSheet)` gerendert. Das ist fehleranfällig. Neuere Compose Navigation Versionen unterstützen native BottomSheets in der Navigation.
*   **Status-Farben:** Die Status (SUCCESS, WARNING, ERROR in `StatusType`) nutzen Standard Material `Container`-Colors. In einem Dark Dashboard führt das oft zu Matschfarben. Hier fehlen klare, neon-artige Signalfarben.

## 5. Konkrete Stellen im Code (Dateipfade)
*   **`app/src/main/java/de/montagezeit/app/ui/theme/Color.kt`**: Farben sind auf Material 3 getrimmt. Keine echten Premium Dark-Tokens.
*   **`app/src/main/java/de/montagezeit/app/ui/theme/Type.kt`**: Standard SansSerif statt technischer/geometrischer Font (z.B. Inter, Roboto Mono, JetBrains Mono für Stats).
*   **`app/src/main/java/de/montagezeit/app/ui/components/MontageZeitComponents.kt`**:
    *   `MZPageBackground` -> Simpler Gradient, kein Mesh/Glow.
    *   `MZCard` -> Standard Elevation, flache Border.
    *   `MZStatusCard` -> Setzt auf M3 Container Colors, die im Dark Mode stumpf wirken.
*   **`app/src/main/java/de/montagezeit/app/ui/screen/today/TodayScreen.kt`**: Nutzt `AlertDialog` für `DailyManualCheckInDialog` und `DayLocationDialog`. Besser: Hochwertige Bottom Sheets.
*   **`app/src/main/java/de/montagezeit/app/ui/navigation/MontageZeitNavGraph.kt`**: Das Floating Bottom Navigation Panel hat eine gute Form, aber der Background des `Scaffold` und die `Surface` verschmelzen oft ungünstig durch fehlenden Blur/Glow-Contrast.
