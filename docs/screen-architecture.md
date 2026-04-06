# Screen-Architektur & Layout-Umbau

Dieser Bereich dokumentiert, wie die bestehenden Hauptscreens auf die neue "Dark Control Dashboard"-Architektur übersetzt werden sollen.

## Generelle Layout-Prinzipien für alle Screens
1.  **Immersive Background:** Der `MZPageBackground` (bald `GlassBackground`) muss über den gesamten Screen fließen. System-Bars (Status Bar, Navigation Bar) werden transparent.
2.  **Verzicht auf harte Trenner:** Keine opaken `HorizontalDivider` mehr. Trennung erfolgt durch Whitespace (Spacing Tokens) und leichte Glass-Container.
3.  **Floating Nav Bar:** Behält ihre abgerundete Form (`RoundedCornerShape(28.dp)`), wird aber zu einer `GlassSurface` (Transluzent + Blur) mit leuchtenden aktiven Icons (Neon-Cyan) aufleuchten.
4.  **Bottom Sheets statt Dialoge:** Für Eingaben und Bestätigungen (Check-In, Edit, Delete).

---

## 1. Today Screen (`app/src/main/java/de/montagezeit/app/ui/screen/today/TodayScreen.kt`)
*   **Zweck:** Das Cockpit für den aktuellen Tag. Schnelle Aktionen, klarer Status.
*   **Neue Struktur:**
    *   **Header:** `TopAppBar` entfällt optisch fast völlig in den Hintergrund (Transparent). Datum rückt als großes, technisches Display-Element nach oben.
    *   **Hero-Panel (Dashboard Card):** Die aktuelle `StatusCard` wird zur `GlassHeroCard`. Ein großer, leuchtender Indikator (Status-Ring oder Neon-Badge) signalisiert sofort: "Offen", "Abgeschlossen", "Pause".
    *   **Week Overview Row:** Diese bleibt oben, aber die Wochentage werden zu kleinen Glass-Pills statt flachen Kästchen.
    *   **Action Area:** Die Aktionen ("Offday", "Check-in") werden massiv aufgewertet. Ein großer `NeonButton` für den primären Workflow (Check-In).
    *   **Dialoge -> Sheets:** `DailyManualCheckInDialog` und `DayLocationDialog` MÜSSEN durch ein modernes Modal-Bottom-Sheet (Glassmorphism-Stil) ersetzt werden. Eine Tastatur über einem Standard-Dialog bricht das Premium-Erlebnis.

## 2. Overview Screen (`app/src/main/java/de/montagezeit/app/ui/screen/overview/OverviewScreen.kt`)
*   **Zweck:** Analytischer Blick auf Überstunden, Budgets und Zeiten.
*   **Neue Struktur:**
    *   **Period Selector (Top Range Bar):** Der Dropdown/Range-Switcher oben muss wie ein Segmented-Control in einer Glass-Pille wirken. Das Sheet zur Datumsauswahl `OverviewPeriodPickerSheet` wird mit dem neuen Glass-Theme ausgestattet.
    *   **Hero-Stats (Überstunden-Balance):** Dies ist das Herzstück. Die Zahl (z.B. "+12.5 Std.") wird rieeeeesig in Monospace-Typography (`TechType`) gesetzt, umgeben von einem subtilen Neon-Glow (Positiv: Cyan/Grün, Negativ: Rot).
    *   **KPI Grid:** Die `OverviewKpiGrid` wird von 4 klumpigen Material Cards zu einem modernen Dashboard-Gitter (2x2 Glass-Tiles). Die Icons bekommen einen leichten Glow.
    *   **Progress Bar:** Der `LinearProgressIndicator` in der Hero-Card bekommt stark abgerundete Ecken und einen Glow. Keine harten Kanten.

## 3. History Screen (`app/src/main/java/de/montagezeit/app/ui/screen/history/HistoryScreen.kt`)
*   **Zweck:** Listenansicht und Kalender für vergangene Tage.
*   **Neue Struktur:**
    *   **Mini Filter Bar:** Die Filter Chips ("Liste", "Kalender") wirken aktuell unruhig. Zusammenfassen in eine nahtlose, schwebende `GlassTabRow`.
    *   **Kalenderansicht:** Die `CalendarDayCell` ist aktuell sehr material-mäßig. Im neuen Look werden freie Tage als leere "Dots" und Arbeitstage als gefüllte Neon-Dots auf Glasflächen dargestellt. Das Grid selbst bekommt keine sichtbaren Linien mehr, nur Spacing.
    *   **List Items (`HistoryEntryItem`):** Die Listen-Elemente werden zu schlanken `GlassListItems`. Anstelle voller Statusfarben (M3 Container) nutzen wir einen dünnen, leuchtenden, farbigen linken Border-Stripe (z.B. 4dp breit), der den Status des Tages anzeigt.
    *   **Sticky Headers:** Die Month/Week Group Headers in der Liste verschmelzen fließend mit dem Hintergrund (Backdrop-Blur beim Scrollen).

## 4. Settings Screen (`app/src/main/java/de/montagezeit/app/ui/screen/settings/SettingsScreen.kt`)
*   **Zweck:** Konfiguration und Export.
*   **Neue Struktur:**
    *   **Dashboard-Header:** Der aktuelle Header mit "Aktive Reminder" wird beibehalten, bekommt aber den `GlassHeroCard` Look.
    *   **Einstellungs-Blöcke (`EditFormSectionCard` Äquivalente):** Anstatt vieler unzusammenhängender Cards werden Einstellungen logisch gruppiert in größere Glass-Container gepackt. Listen-Elemente innerhalb eines Containers bekommen Unterteilungen durch hauchdünne `Divider` (`White 5%`).
    *   **Switches/Toggles:** Standard Android-Switches werden umgestylt, um zur Neon-Farbpalette zu passen.

## 5. Edit Entry Sheet (`app/src/main/java/de/montagezeit/app/ui/screen/edit/EditEntrySheet.kt`)
*   **Zweck:** Detailbearbeitung eines Tages.
*   **Neue Struktur:**
    *   Der `ModalBottomSheet` Container selbst MUSS das Glass-Styling übernehmen.
    *   Die `EditStickySaveBar` wird extrem wichtig. Sie "schwebt" über dem Inhalt am unteren Rand und bekommt einen Background-Blur, damit die drunter durchscrollenden Elemente schemenhaft erkennbar bleiben.
    *   Die diversen `EditFormSectionCard` Elemente werden zu sauberen Glass-Blöcken.
    *   **Time-/Date-Picker:** Die Aufrufe zu System-Standard-Dialogen (`TimePickerDialog`, `DatePickerDialog`) wirken wie ein Bruch. Ideal wäre hier langfristig ein In-App-Scroll-Picker, kurzfristig müssen die Dialog-Surfaces strikt ins Dark Theme gepresst werden.
